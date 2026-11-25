package com.eduzone.enrollment;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Điều phối luồng ghi danh 6 bước:
 * 1. tạo ghi danh PENDING → 2. giữ suất (course) → 3. trừ học phí (student)
 * → 4. cấp quyền vào lớp (access, có thể từ chối) → 5. bù trừ nếu bị từ chối
 * → 6. kết thúc CONFIRMED / CANCELLED.
 *
 * Mỗi bước lưu trạng thái riêng, không bọc trong một transaction vì có lời gọi HTTP ở giữa.
 */
@Service
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final EnrollmentRepository repo;
    private final MeterRegistry meters;
    private final RestClient courseClient;
    private final RestClient studentClient;
    private final RestClient accessClient;

    public EnrollmentService(EnrollmentRepository repo,
                             MeterRegistry meters,
                             RestClient.Builder builder,
                             @Value("${eduzone.services.course-url}") String courseUrl,
                             @Value("${eduzone.services.student-url}") String studentUrl,
                             @Value("${eduzone.services.access-url}") String accessUrl) {
        this.repo = repo;
        this.meters = meters;
        // Đăng ký sẵn mọi tổ hợp nhãn với giá trị 0: nếu counter chỉ xuất hiện ở lần tăng đầu tiên,
        // Prometheus không có mẫu 0 ban đầu và increase()/rate() sẽ bỏ sót các ghi danh đầu tiên.
        count0("confirmed", "none");
        for (String reason : new String[] {"no_seat", "payment_failed", "class_full", "access_error"}) {
            count0("cancelled", reason);
        }
        this.courseClient = builder.clone().baseUrl(courseUrl).build();
        this.studentClient = builder.clone().baseUrl(studentUrl).build();
        this.accessClient = builder.clone().baseUrl(accessUrl).build();
    }

    public Enrollment get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy ghi danh " + id));
    }

    public Enrollment enroll(EnrollRequest req) {
        // Bước 1: tạo ghi danh PENDING
        Enrollment e = repo.save(Enrollment.pending(req.studentId(), req.courseCode()));
        log.info("Bước 1: tạo ghi danh {} (PENDING) cho học viên {}, khoá {}",
                e.getId(), e.getStudentId(), e.getCourseCode());

        // Bước 2: giữ 1 suất ở course-service
        CourseInfo course;
        try {
            course = courseClient.post()
                    .uri("/internal/courses/{code}/reserve", e.getCourseCode())
                    .retrieve()
                    .body(CourseInfo.class);
        } catch (RestClientException ex) {
            log.warn("Bước 2: không giữ được suất khoá {} cho ghi danh {}: {}",
                    e.getCourseCode(), e.getId(), detail(ex), ex);
            return cancel(e, "Không giữ được suất: " + detail(ex), "no_seat");
        }
        e.setAmount(course.fee());
        e = repo.save(e);
        log.info("Bước 2: đã giữ suất khoá {}, học phí {} VND", course.code(), course.fee());

        // Bước 3: trừ học phí ở student-service
        try {
            studentClient.post()
                    .uri("/internal/students/{id}/debit", e.getStudentId())
                    .body(new WalletRequest(e.getId(), e.getAmount()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Bước 3: không trừ được học phí của học viên {} cho ghi danh {}: {}",
                    e.getStudentId(), e.getId(), detail(ex), ex);
            releaseSeat(e);
            return cancel(e, "Không trừ được học phí: " + detail(ex), "payment_failed");
        }
        log.info("Bước 3: đã trừ {} VND của học viên {}", e.getAmount(), e.getStudentId());

        // Bước 4: xin cấp quyền vào lớp ở access-service (có thể bị từ chối)
        try {
            accessClient.post()
                    .uri("/internal/access-grants")
                    .body(new GrantRequest(e.getId(), e.getStudentId(), e.getCourseCode()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict ex) {
            // HẬU QUẢ: access-service đã từ chối; ghi WARN kèm ngoại lệ của lời gọi liên service.
            log.warn("Bước 4: access-service từ chối cấp quyền cho ghi danh {}: {}", e.getId(), detail(ex), ex);
            compensate(e);
            return cancel(e, detail(ex), "class_full");
        } catch (RestClientException ex) {
            log.error("Bước 4: lỗi khi gọi access-service cho ghi danh {}: {}", e.getId(), detail(ex), ex);
            compensate(e);
            return cancel(e, "Không cấp được quyền vào lớp: " + detail(ex), "access_error");
        }

        // Bước 6: kết thúc thành công
        e.confirm();
        e = repo.save(e);
        count("confirmed", "none");
        log.info("Bước 6: ghi danh {} hoàn tất, trạng thái CONFIRMED", e.getId());
        return e;
    }

    /** Số đo nghiệp vụ: eduzone_enrollments_total{status, reason} — loại đếm luỹ kế (counter). */
    private void count(String status, String reason) {
        count0(status, reason).increment();
    }

    private Counter count0(String status, String reason) {
        return meters.counter("eduzone.enrollments", "status", status, "reason", reason);
    }

    /** Bước 5: hoàn tiền rồi trả lại suất (ngược thứ tự bước 3, 2). */
    private void compensate(Enrollment e) {
        try {
            studentClient.post()
                    .uri("/internal/students/{id}/refund", e.getStudentId())
                    .body(new WalletRequest(e.getId(), e.getAmount()))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Bước 5: đã hoàn {} VND cho học viên {} (ghi danh {})",
                    e.getAmount(), e.getStudentId(), e.getId());
        } catch (RestClientException ex) {
            log.error("Bước 5: HOÀN TIỀN THẤT BẠI cho học viên {} (ghi danh {}, {} VND)",
                    e.getStudentId(), e.getId(), e.getAmount(), ex);
        }
        releaseSeat(e);
    }

    private void releaseSeat(Enrollment e) {
        try {
            courseClient.post()
                    .uri("/internal/courses/{code}/release", e.getCourseCode())
                    .retrieve()
                    .toBodilessEntity();
            log.info("Bước 5: đã trả lại suất khoá {} (ghi danh {})", e.getCourseCode(), e.getId());
        } catch (RestClientException ex) {
            log.error("Bước 5: TRẢ SUẤT THẤT BẠI cho khoá {} (ghi danh {})", e.getCourseCode(), e.getId(), ex);
        }
    }

    private Enrollment cancel(Enrollment e, String reason, String reasonCode) {
        e.cancel(reason);
        Enrollment saved = repo.save(e);
        count("cancelled", reasonCode);
        log.info("Bước 6: ghi danh {} kết thúc, trạng thái CANCELLED: {}", saved.getId(), reason);
        return saved;
    }

    /** Lấy thông điệp lỗi từ ProblemDetail mà service bên kia trả về. */
    private static String detail(RestClientException ex) {
        if (ex instanceof RestClientResponseException r) {
            try {
                ProblemDetail p = r.getResponseBodyAs(ProblemDetail.class);
                if (p != null && p.getDetail() != null) {
                    return p.getDetail();
                }
            } catch (RuntimeException ignored) {
                // thân phản hồi không phải ProblemDetail
            }
            return r.getStatusCode() + " " + r.getStatusText();
        }
        return ex.getMessage();
    }

    record CourseInfo(String code, String title, Long fee, Integer seatsAvailable) {
    }

    record WalletRequest(Long enrollmentId, Long amount) {
    }

    record GrantRequest(Long enrollmentId, Long studentId, String courseCode) {
    }
}
