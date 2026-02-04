package com.example.concert_reservation.domain.reservation.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ReservationStatus 테스트")
class ReservationStatusTest {
    
    @Test
    @DisplayName("PENDING 상태는 임시 예약 상태다")
    void pending_isPending() {
        assertThat(ReservationStatus.PENDING.isPending()).isTrue();
        assertThat(ReservationStatus.PENDING.isActive()).isTrue();
        assertThat(ReservationStatus.PENDING.isConfirmed()).isFalse();
    }
    
    @Test
    @DisplayName("CONFIRMED 상태는 확정된 예약이다")
    void confirmed_isConfirmed() {
        assertThat(ReservationStatus.CONFIRMED.isConfirmed()).isTrue();
        assertThat(ReservationStatus.CONFIRMED.isActive()).isTrue();
        assertThat(ReservationStatus.CONFIRMED.isPending()).isFalse();
    }
    
    @Test
    @DisplayName("CANCELLED 상태는 취소된 예약이다")
    void cancelled_isCancelled() {
        assertThat(ReservationStatus.CANCELLED.isCancelled()).isTrue();
        assertThat(ReservationStatus.CANCELLED.isActive()).isFalse();
    }
    
    @Test
    @DisplayName("EXPIRED 상태는 만료된 예약이다")
    void expired_isExpired() {
        assertThat(ReservationStatus.EXPIRED.isExpired()).isTrue();
        assertThat(ReservationStatus.EXPIRED.isActive()).isFalse();
    }
}
