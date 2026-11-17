package com.eduzone.student;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByEmail(String email);

    /** Trừ tiền nguyên tử; trả về 0 nếu không đủ số dư hoặc không có học viên. */
    @Modifying(clearAutomatically = true)
    @Query("update Student s set s.walletBalance = s.walletBalance - :amount where s.id = :id and s.walletBalance >= :amount")
    int debit(@Param("id") Long id, @Param("amount") Long amount);

    @Modifying(clearAutomatically = true)
    @Query("update Student s set s.walletBalance = s.walletBalance + :amount where s.id = :id")
    int credit(@Param("id") Long id, @Param("amount") Long amount);
}
