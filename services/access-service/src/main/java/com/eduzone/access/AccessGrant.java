package com.eduzone.access;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Quyền vào học của một học viên với một khoá học, sinh ra từ một ghi danh. */
@Entity
@Table(name = "access_grants")
public class AccessGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enrollment_id", nullable = false, unique = true)
    private Long enrollmentId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "course_code", nullable = false, length = 32)
    private String courseCode;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    protected AccessGrant() {
    }

    public AccessGrant(Long enrollmentId, Long studentId, String courseCode) {
        this.enrollmentId = enrollmentId;
        this.studentId = studentId;
        this.courseCode = courseCode;
        this.grantedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getEnrollmentId() { return enrollmentId; }
    public Long getStudentId() { return studentId; }
    public String getCourseCode() { return courseCode; }
    public Instant getGrantedAt() { return grantedAt; }
}
