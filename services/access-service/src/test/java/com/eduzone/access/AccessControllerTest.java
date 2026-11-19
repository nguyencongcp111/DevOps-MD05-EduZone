package com.eduzone.access;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccessControllerTest {

    private final AccessGrantRepository grants = mock(AccessGrantRepository.class);
    private final ClassLockRepository locks = mock(ClassLockRepository.class);
    private final AccessController controller = new AccessController(grants, locks);

    @Test
    void grantsAccessWhenClassIsOpen() {
        when(locks.findById("DEVOPS-101")).thenReturn(Optional.empty());
        when(grants.save(any(AccessGrant.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> res = controller.grant(new GrantRequest(1L, 1L, "DEVOPS-101"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(grants).save(any(AccessGrant.class));
    }

    @Test
    void rejectsWith409WhenClassIsFull() {
        when(locks.findById("SPRING-201")).thenReturn(Optional.of(new ClassLock("SPRING-201")));

        ResponseEntity<?> res = controller.grant(new GrantRequest(2L, 1L, "SPRING-201"));

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody()).isInstanceOfSatisfying(ProblemDetail.class, p -> {
            assertThat(p.getTitle()).isEqualTo("CLASS_FULL");
            assertThat(p.getDetail()).contains("SPRING-201");
        });
        verify(grants, never()).save(any());
    }
}
