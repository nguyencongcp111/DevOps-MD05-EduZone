package com.eduzone.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GatewayApplicationTest {

    @Autowired
    RouteLocator routeLocator;

    @Test
    void definesOneRoutePerService() {
        assertThat(routeLocator.getRoutes().map(Route::getId).collectList().block())
                .containsExactlyInAnyOrder("course-service", "student-service", "enrollment-service", "access-service");
    }
}
