package com.eduzone.enrollment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/** Một lần ghi danh của học viên vào khoá học. */
@Entity
@Table(name = "enrollments")
public class Enrollment {

    public enum Status { PENDING, CONFIRMED, CANCELLED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "course_code", nullable = false, length = 32)
    private String courseCode;

    /** Học phí đã trừ (VND); null nếu chưa tới bước trừ tiền. */
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    /** Lý do huỷ, nếu có. */
    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Enrollment() {
    }

    public static Enrollment pending(Long studentId, String courseCode) {
        Enrollment e = new Enrollment();
        e.studentId = studentId;
        e.courseCode = courseCode;
        e.status = Status.PENDING;
        return e;
    }

    public void setAmount(Long amount) { this.amount = amount; }

    public void confirm() {
        this.status = Status.CONFIRMED;
    }

    public void cancel(String reason) {
        this.status = Status.CANCELLED;
        this.reason = reason;
    }

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getStudentId() { return studentId; }
    public String getCourseCode() { return courseCode; }
    public Long getAmount() { return amount; }
    public Status getStatus() { return status; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
