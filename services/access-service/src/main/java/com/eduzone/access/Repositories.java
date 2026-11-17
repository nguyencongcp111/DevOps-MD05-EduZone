package com.eduzone.access;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface AccessGrantRepository extends JpaRepository<AccessGrant, Long> {

    List<AccessGrant> findByStudentIdOrderByGrantedAtDesc(Long studentId);

    List<AccessGrant> findAllByOrderByGrantedAtDesc();
}

interface ClassLockRepository extends JpaRepository<ClassLock, String> {
}
