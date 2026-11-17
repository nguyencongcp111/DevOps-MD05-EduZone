package com.eduzone.access;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class AccessController {

    private static final Logger log = LoggerFactory.getLogger(AccessController.class);

    private final AccessGrantRepository grants;
    private final ClassLockRepository locks;

    public AccessController(AccessGrantRepository grants, ClassLockRepository locks) {
        this.grants = grants;
        this.locks = locks;
    }

    // ---- Endpoint nội bộ: bước 4 của luồng ghi danh ----

    @PostMapping("/internal/access-grants")
    @Transactional
    public ResponseEntity<?> grant(@Valid @RequestBody GrantRequest req) {
        Optional<ClassLock> lock = locks.findById(req.courseCode());
        if (lock.isPresent()) {
            // NGUYÊN NHÂN GỐC của sự cố: từ chối có kiểm soát, ghi WARN và KHÔNG kèm ngoại lệ.
            log.warn("Từ chối cấp quyền cho ghi danh {}: lớp {} đã đủ sĩ số (khoá lớp lúc {})",
                    req.enrollmentId(), req.courseCode(), lock.get().getLockedAt());
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                    "Lớp " + req.courseCode() + " đã đủ sĩ số");
            problem.setTitle("CLASS_FULL");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
        }
        AccessGrant grant = grants.save(new AccessGrant(req.enrollmentId(), req.studentId(), req.courseCode()));
        log.info("Đã cấp quyền vào lớp {} cho học viên {} (ghi danh {})",
                req.courseCode(), req.studentId(), req.enrollmentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(grant);
    }

    // ---- Endpoint phụ trợ (đi qua gateway): bật/tắt điều kiện từ chối, tra cứu quyền ----

    @PutMapping("/api/access/class-locks/{courseCode}")
    @Transactional
    public Map<String, Object> lockClass(@PathVariable String courseCode) {
        ClassLock lock = locks.findById(courseCode).orElseGet(() -> locks.save(new ClassLock(courseCode)));
        log.info("Đánh dấu lớp {} đã đủ sĩ số", courseCode);
        return Map.of("courseCode", courseCode, "locked", true, "lockedAt", lock.getLockedAt());
    }

    @DeleteMapping("/api/access/class-locks/{courseCode}")
    @Transactional
    public Map<String, Object> unlockClass(@PathVariable String courseCode) {
        locks.deleteById(courseCode);
        log.info("Mở lại lớp {} để nhận ghi danh", courseCode);
        return Map.of("courseCode", courseCode, "locked", false);
    }

    @GetMapping("/api/access/grants")
    @Transactional(readOnly = true)
    public List<AccessGrant> listGrants(@RequestParam(required = false) Long studentId) {
        return studentId == null
                ? grants.findAllByOrderByGrantedAtDesc()
                : grants.findByStudentIdOrderByGrantedAtDesc(studentId);
    }
}

record GrantRequest(
        @NotNull Long enrollmentId,
        @NotNull Long studentId,
        @NotBlank String courseCode) {
}
