package com.eduzone.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

/**
 * Cổng vào duy nhất của EduZone. Chỉ định tuyến các đường dẫn /api/**;
 * các đường dẫn /internal/** dùng cho lời gọi giữa service không được mở ra ngoài.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    RouteLocator routes(RouteLocatorBuilder builder,
                        @Value("${eduzone.routes.course-url}") String courseUrl,
                        @Value("${eduzone.routes.student-url}") String studentUrl,
                        @Value("${eduzone.routes.enrollment-url}") String enrollmentUrl,
                        @Value("${eduzone.routes.access-url}") String accessUrl) {
        return builder.routes()
                .route("course-service", r -> r.path("/api/courses/**").uri(courseUrl))
                .route("student-service", r -> r.path("/api/students/**").uri(studentUrl))
                .route("enrollment-service", r -> r.path("/api/enrollments/**").uri(enrollmentUrl))
                .route("access-service", r -> r.path("/api/access/**").uri(accessUrl))
                .build();
    }
}
