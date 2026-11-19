package com.eduzone.course;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CourseServiceTest {

    private final CourseRepository repo = mock(CourseRepository.class);
    private final CourseService service = new CourseService(repo);

    @Test
    void reserveReturnsCourseWithRemainingSeats() {
        when(repo.reserveSeat("DEVOPS-101")).thenReturn(1);
        when(repo.findByCode("DEVOPS-101"))
                .thenReturn(Optional.of(new Course("DEVOPS-101", "DevOps", 1_500_000L, 29)));

        assertThat(service.reserveSeat("DEVOPS-101").getSeatsAvailable()).isEqualTo(29);
    }

    @Test
    void reserveFailsWith409WhenNoSeatLeft() {
        when(repo.reserveSeat("K8S-301")).thenReturn(0);
        when(repo.existsByCode("K8S-301")).thenReturn(true);

        assertThatThrownBy(() -> service.reserveSeat("K8S-301"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void reserveFailsWith404WhenCourseUnknown() {
        when(repo.reserveSeat("NOPE")).thenReturn(0);
        when(repo.existsByCode("NOPE")).thenReturn(false);

        assertThatThrownBy(() -> service.reserveSeat("NOPE"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
