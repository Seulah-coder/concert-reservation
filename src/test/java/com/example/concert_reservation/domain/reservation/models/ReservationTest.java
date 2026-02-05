package com.example.concert_reservation.domain.reservation.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Reservation 도메인 모델 테스트")
class ReservationTest {
    
    @Test
    @DisplayName("새로운 예약을 생성할 수 있다")
    void create_newReservation() {
        // given
        String userId = "user123";
        Long seatId = 1L;
        Long concertDateId = 1L;
        BigDecimal price = new BigDecimal("50000");
        
        // when
        Reservation reservation = Reservation.create(userId, seatId, concertDateId, price);
        
        // then
        assertThat(reservation).isNotNull();
        assertThat(reservation.getUserId()).isEqualTo(userId);
        assertThat(reservation.getSeatId()).isEqualTo(seatId);
        assertThat(reservation.getConcertDateId()).isEqualTo(concertDateId);
        assertThat(reservation.getPrice()).isEqualTo(price);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(reservation.getReservedAt()).isNotNull();
        assertThat(reservation.getExpiresAt()).isAfter(reservation.getReservedAt());
    }
    
    @Test
    @DisplayName("사용자 ID가 null이면 예외가 발생한다")
    void create_withNullUserId_throwsException() {
        assertThatThrownBy(() -> Reservation.create(null, 1L, 1L, new BigDecimal("50000")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("사용자 ID는 필수입니다");
    }
    
    @Test
    @DisplayName("좌석 ID가 null이면 예외가 발생한다")
    void create_withNullSeatId_throwsException() {
        assertThatThrownBy(() -> Reservation.create("user123", null, 1L, new BigDecimal("50000")))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("좌석 ID는 필수입니다");
    }
    
    @Test
    @DisplayName("PENDING 예약을 확정할 수 있다")
    void confirm_fromPending_success() {
        // given
        Reservation reservation = Reservation.create("user123", 1L, 1L, new BigDecimal("50000"));
        
        // when
        reservation.confirm();
        
        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }
    
    @Test
    @DisplayName("PENDING이 아닌 예약은 확정할 수 없다")
    void confirm_fromNonPending_throwsException() {
        // given
        Reservation reservation = Reservation.of(1L, "user123", 1L, 1L, new BigDecimal("50000"),
            ReservationStatus.CONFIRMED, LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
        
        // when & then
        assertThatThrownBy(() -> reservation.confirm())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("임시 예약 상태만 확정할 수 있습니다");
    }
    
    @Test
    @DisplayName("PENDING 예약을 취소할 수 있다")
    void cancel_fromPending_success() {
        // given
        Reservation reservation = Reservation.create("user123", 1L, 1L, new BigDecimal("50000"));
        
        // when
        reservation.cancel();
        
        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }
    
    @Test
    @DisplayName("CONFIRMED 예약도 취소할 수 있다 (환불 시나리오)")
    void cancel_fromConfirmed_success() {
        // given
        Reservation reservation = Reservation.of(1L, "user123", 1L, 1L, new BigDecimal("50000"),
            ReservationStatus.CONFIRMED, LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
        
        // when
        reservation.cancel();
        
        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }
    
    @Test
    @DisplayName("CANCELLED 또는 EXPIRED 예약은 취소할 수 없다")
    void cancel_fromCancelledOrExpired_throwsException() {
        // given - CANCELLED 예약
        Reservation cancelledReservation = Reservation.of(1L, "user123", 1L, 1L, new BigDecimal("50000"),
            ReservationStatus.CANCELLED, LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
        
        // when & then
        assertThatThrownBy(() -> cancelledReservation.cancel())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("임시 예약 또는 확정된 예약만 취소할 수 있습니다");
        
        // given - EXPIRED 예약
        Reservation expiredReservation = Reservation.of(2L, "user456", 2L, 2L, new BigDecimal("50000"),
            ReservationStatus.EXPIRED, LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
        
        // when & then
        assertThatThrownBy(() -> expiredReservation.cancel())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("임시 예약 또는 확정된 예약만 취소할 수 있습니다");
    }
    
    @Test
    @DisplayName("PENDING 예약을 만료 처리할 수 있다")
    void expire_fromPending_success() {
        // given
        Reservation reservation = Reservation.create("user123", 1L, 1L, new BigDecimal("50000"));
        
        // when
        reservation.expire();
        
        // then
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
    }
    
    @Test
    @DisplayName("만료 시간이 지났는지 확인할 수 있다")
    void isExpired_returnsTrue() {
        // given
        Reservation reservation = Reservation.of(1L, "user123", 1L, 1L, new BigDecimal("50000"),
            ReservationStatus.PENDING, LocalDateTime.now().minusMinutes(10),
            LocalDateTime.now().minusMinutes(5));
        
        // when
        boolean result = reservation.isExpired();
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("만료되지 않은 예약은 false를 반환한다")
    void isExpired_returnsFalse() {
        // given
        Reservation reservation = Reservation.create("user123", 1L, 1L, new BigDecimal("50000"));
        
        // when
        boolean result = reservation.isExpired();
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("남은 시간을 초 단위로 계산할 수 있다")
    void getRemainingSeconds_success() {
        // given
        Reservation reservation = Reservation.create("user123", 1L, 1L, new BigDecimal("50000"));
        
        // when
        long remainingSeconds = reservation.getRemainingSeconds();
        
        // then
        assertThat(remainingSeconds).isGreaterThan(0);
        assertThat(remainingSeconds).isLessThanOrEqualTo(300); // 5분 = 300초
    }
    
    @Test
    @DisplayName("만료된 예약의 남은 시간은 0이다")
    void getRemainingSeconds_expired_returnsZero() {
        // given
        Reservation reservation = Reservation.of(1L, "user123", 1L, 1L, new BigDecimal("50000"),
            ReservationStatus.PENDING, LocalDateTime.now().minusMinutes(10),
            LocalDateTime.now().minusMinutes(5));
        
        // when
        long remainingSeconds = reservation.getRemainingSeconds();
        
        // then
        assertThat(remainingSeconds).isEqualTo(0);
    }
}
