package com.eduzone.access;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Công tắc của kịch bản từ chối có kiểm soát: lớp có bản ghi ở đây được coi là "đã đủ sĩ số",
 * access-service sẽ từ chối cấp quyền cho mọi ghi danh mới vào lớp đó.
 */
@Entity
@Table(name = "class_locks")
public class ClassLock {

    @Id
    @Column(name = "course_code", length = 32)
    private String courseCode;

    @Column(name = "locked_at", nullable = false)
    private Instant lockedAt;

    protected ClassLock() {
    }

    public ClassLock(String courseCode) {
        this.courseCode = courseCode;
        this.lockedAt = Instant.now();
    }

    public String getCourseCode() { return courseCode; }
    public Instant getLockedAt() { return lockedAt; }
}
