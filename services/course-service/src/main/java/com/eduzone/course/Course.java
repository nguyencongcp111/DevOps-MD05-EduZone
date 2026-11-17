package com.eduzone.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Khoá học. {@code seatsAvailable} là số suất ghi danh còn mở bán ("tồn kho"). */
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false)
    private String title;

    /** Học phí, đơn vị VND. */
    @Column(nullable = false)
    private Long fee;

    @Column(name = "seats_available", nullable = false)
    private Integer seatsAvailable;

    protected Course() {
    }

    public Course(String code, String title, Long fee, Integer seatsAvailable) {
        this.code = code;
        this.title = title;
        this.fee = fee;
        this.seatsAvailable = seatsAvailable;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getTitle() { return title; }
    public Long getFee() { return fee; }
    public Integer getSeatsAvailable() { return seatsAvailable; }
}
