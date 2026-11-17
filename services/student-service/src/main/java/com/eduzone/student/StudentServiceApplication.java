package com.eduzone.student;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class StudentServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(StudentServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(StudentServiceApplication.class, args);
    }

    /** Dữ liệu mẫu: chỉ chèn khi bảng còn trống. */
    @Bean
    CommandLineRunner seedStudents(StudentRepository repo) {
        return args -> {
            if (repo.count() > 0) {
                return;
            }
            repo.saveAll(List.of(
                    new Student("Nguyễn Văn An", "an.nguyen@eduzone.local", 10_000_000L),
                    new Student("Trần Thị Bình", "binh.tran@eduzone.local", 3_000_000L)));
            log.info("Đã tạo dữ liệu mẫu: {} học viên", repo.count());
        };
    }
}
