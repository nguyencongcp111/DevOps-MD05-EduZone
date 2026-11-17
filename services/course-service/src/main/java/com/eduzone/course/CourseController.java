package com.eduzone.course;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CourseController {

    private final CourseService service;

    public CourseController(CourseService service) {
        this.service = service;
    }

    // ---- Endpoint phụ trợ (đi qua gateway) ----

    @GetMapping("/api/courses")
    public List<Course> list() {
        return service.findAll();
    }

    @PostMapping("/api/courses")
    @ResponseStatus(HttpStatus.CREATED)
    public Course create(@Valid @RequestBody CreateCourseRequest req) {
        return service.create(req);
    }

    // ---- Endpoint nội bộ (chỉ enrollment-service gọi) ----

    @PostMapping("/internal/courses/{code}/reserve")
    public Course reserve(@PathVariable String code) {
        return service.reserveSeat(code);
    }

    @PostMapping("/internal/courses/{code}/release")
    public Course release(@PathVariable String code) {
        return service.releaseSeat(code);
    }
}

record CreateCourseRequest(
        @NotBlank String code,
        @NotBlank String title,
        @NotNull @PositiveOrZero Long fee,
        @NotNull @PositiveOrZero Integer seatsAvailable) {
}
