package com.eduzone.course;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class CourseServiceApplication {

    private static final Logger log = LoggerFactory.getLogger(CourseServiceApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CourseServiceApplication.class, args);
    }

    /** Dữ liệu mẫu: chỉ chèn khi bảng còn trống. */
    @Bean
    CommandLineRunner seedCourses(CourseRepository repo) {
        return args -> {
            if (repo.count() > 0) {
                return;
            }
            repo.saveAll(List.of(
                    new Course("DEVOPS-101", "DevOps thực chiến", 1_500_000L, 30),
                    new Course("SPRING-201", "Spring Boot Microservices", 2_000_000L, 20),
                    new Course("K8S-301", "Kubernetes căn bản", 2_500_000L, 1)));
            log.info("Đã tạo dữ liệu mẫu: {} khoá học", repo.count());
        };
    }
}
