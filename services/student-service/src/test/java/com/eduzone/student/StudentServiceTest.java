package com.eduzone.student;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentServiceTest {

    private final StudentRepository repo = mock(StudentRepository.class);
    private final StudentService service = new StudentService(repo);

    @Test
    void debitReturnsNewBalance() {
        when(repo.debit(1L, 1_500_000L)).thenReturn(1);
        when(repo.findById(1L)).thenReturn(Optional.of(new Student("An", "an@eduzone.local", 8_500_000L)));

        Student s = service.debit(1L, new WalletRequest(10L, 1_500_000L));

        assertThat(s.getWalletBalance()).isEqualTo(8_500_000L);
    }

    @Test
    void debitFailsWith409WhenBalanceInsufficient() {
        when(repo.debit(2L, 5_000_000L)).thenReturn(0);
        when(repo.findById(2L)).thenReturn(Optional.of(new Student("Bình", "binh@eduzone.local", 3_000_000L)));

        assertThatThrownBy(() -> service.debit(2L, new WalletRequest(11L, 5_000_000L)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void refundFailsWith404WhenStudentUnknown() {
        when(repo.credit(99L, 100L)).thenReturn(0);

        assertThatThrownBy(() -> service.refund(99L, new WalletRequest(12L, 100L)))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
