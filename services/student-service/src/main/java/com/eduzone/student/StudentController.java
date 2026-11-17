package com.eduzone.student;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StudentController {

    private final StudentService service;

    public StudentController(StudentService service) {
        this.service = service;
    }

    // ---- Endpoint phụ trợ (đi qua gateway) ----

    @PostMapping("/api/students")
    @ResponseStatus(HttpStatus.CREATED)
    public Student create(@Valid @RequestBody CreateStudentRequest req) {
        return service.create(req);
    }

    @GetMapping("/api/students/{id}")
    public Student get(@PathVariable Long id) {
        return service.get(id);
    }

    // ---- Endpoint nội bộ (chỉ enrollment-service gọi) ----

    @PostMapping("/internal/students/{id}/debit")
    public Student debit(@PathVariable Long id, @Valid @RequestBody WalletRequest req) {
        return service.debit(id, req);
    }

    @PostMapping("/internal/students/{id}/refund")
    public Student refund(@PathVariable Long id, @Valid @RequestBody WalletRequest req) {
        return service.refund(id, req);
    }
}

record CreateStudentRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotNull @PositiveOrZero Long walletBalance) {
}

record WalletRequest(
        @NotNull Long enrollmentId,
        @NotNull @Positive Long amount) {
}
