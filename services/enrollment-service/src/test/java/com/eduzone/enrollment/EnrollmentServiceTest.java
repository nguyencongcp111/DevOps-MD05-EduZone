package com.eduzone.enrollment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Kiểm tra luồng điều phối 6 bước, giả lập 3 service bên kia bằng MockRestServiceServer. */
class EnrollmentServiceTest {

    private static final String COURSE_JSON =
            "{\"code\":\"DEVOPS-101\",\"title\":\"DevOps\",\"fee\":1500000,\"seatsAvailable\":29}";

    private MockRestServiceServer server;
    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        EnrollmentRepository repo = mock(EnrollmentRepository.class);
        when(repo.save(any(Enrollment.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new EnrollmentService(repo, builder, "http://course", "http://student", "http://access");
    }

    @Test
    void confirmsWhenAllStepsSucceed() {
        server.expect(requestTo("http://course/internal/courses/DEVOPS-101/reserve"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(COURSE_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://student/internal/students/1/debit"))
                .andExpect(jsonPath("$.amount").value(1_500_000))
                .andRespond(withSuccess());
        server.expect(requestTo("http://access/internal/access-grants"))
                .andRespond(withStatus(HttpStatus.CREATED));

        Enrollment e = service.enroll(new EnrollRequest(1L, "DEVOPS-101"));

        assertThat(e.getStatus()).isEqualTo(Enrollment.Status.CONFIRMED);
        assertThat(e.getAmount()).isEqualTo(1_500_000L);
        server.verify();
    }

    @Test
    void cancelsAndCompensatesWhenClassIsFull() {
        server.expect(requestTo("http://course/internal/courses/DEVOPS-101/reserve"))
                .andRespond(withSuccess(COURSE_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://student/internal/students/1/debit"))
                .andRespond(withSuccess());
        server.expect(requestTo("http://access/internal/access-grants"))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"title\":\"CLASS_FULL\",\"status\":409,\"detail\":\"Lớp DEVOPS-101 đã đủ sĩ số\"}"));
        // Bước 5: hoàn tiền rồi trả suất
        server.expect(requestTo("http://student/internal/students/1/refund"))
                .andExpect(jsonPath("$.amount").value(1_500_000))
                .andRespond(withSuccess());
        server.expect(requestTo("http://course/internal/courses/DEVOPS-101/release"))
                .andRespond(withSuccess());

        Enrollment e = service.enroll(new EnrollRequest(1L, "DEVOPS-101"));

        assertThat(e.getStatus()).isEqualTo(Enrollment.Status.CANCELLED);
        assertThat(e.getReason()).isEqualTo("Lớp DEVOPS-101 đã đủ sĩ số");
        server.verify();
    }

    @Test
    void cancelsWithoutCompensationWhenNoSeatLeft() {
        server.expect(requestTo("http://course/internal/courses/K8S-301/reserve"))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"status\":409,\"detail\":\"Khoá học K8S-301 đã hết suất ghi danh\"}"));

        Enrollment e = service.enroll(new EnrollRequest(2L, "K8S-301"));

        assertThat(e.getStatus()).isEqualTo(Enrollment.Status.CANCELLED);
        assertThat(e.getReason()).contains("hết suất");
        server.verify(); // không có lời gọi trừ tiền / hoàn tiền nào
    }

    @Test
    void releasesSeatWhenWalletIsInsufficient() {
        server.expect(requestTo("http://course/internal/courses/DEVOPS-101/reserve"))
                .andRespond(withSuccess(COURSE_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://student/internal/students/2/debit"))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"status\":409,\"detail\":\"Số dư ví không đủ\"}"));
        server.expect(requestTo("http://course/internal/courses/DEVOPS-101/release"))
                .andRespond(withSuccess());

        Enrollment e = service.enroll(new EnrollRequest(2L, "DEVOPS-101"));

        assertThat(e.getStatus()).isEqualTo(Enrollment.Status.CANCELLED);
        server.verify();
    }
}
