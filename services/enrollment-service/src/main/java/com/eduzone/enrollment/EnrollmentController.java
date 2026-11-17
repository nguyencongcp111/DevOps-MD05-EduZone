package com.eduzone.enrollment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnrollmentController {

    private final EnrollmentService service;

    public EnrollmentController(EnrollmentService service) {
        this.service = service;
    }

    /** Luôn trả HTTP 200; kết quả nghiệp vụ nằm ở trường status (CONFIRMED / CANCELLED). */
    @PostMapping("/api/enrollments")
    public Enrollment enroll(@Valid @RequestBody EnrollRequest req) {
        return service.enroll(req);
    }

    @GetMapping("/api/enrollments/{id}")
    public Enrollment get(@PathVariable Long id) {
        return service.get(id);
    }
}

record EnrollRequest(@NotNull Long studentId, @NotBlank String courseCode) {
}
