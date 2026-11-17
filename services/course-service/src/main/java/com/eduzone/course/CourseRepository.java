package com.eduzone.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByCode(String code);

    boolean existsByCode(String code);

    /** Trừ 1 suất một cách nguyên tử; trả về 0 nếu không còn suất hoặc không có khoá học. */
    @Modifying(clearAutomatically = true)
    @Query("update Course c set c.seatsAvailable = c.seatsAvailable - 1 where c.code = :code and c.seatsAvailable > 0")
    int reserveSeat(@Param("code") String code);

    @Modifying(clearAutomatically = true)
    @Query("update Course c set c.seatsAvailable = c.seatsAvailable + 1 where c.code = :code")
    int releaseSeat(@Param("code") String code);
}
