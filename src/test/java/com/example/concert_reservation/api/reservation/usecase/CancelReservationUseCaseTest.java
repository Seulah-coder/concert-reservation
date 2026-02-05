package com.example.concert_reservation.api.reservation.usecase;

import com.example.concert_reservation.api.reservation.dto.ReservationResponse;
import com.example.concert_reservation.domain.concert.components.SeatManager;
import com.example.concert_reservation.domain.concert.models.Seat;
import com.example.concert_reservation.domain.concert.models.SeatStatus;
import com.example.concert_reservation.domain.reservation.components.ReservationManager;
import com.example.concert_reservation.domain.reservation.models.Reservation;
import com.example.concert_reservation.domain.reservation.models.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CancelReservationUseCase 테스트")
class CancelReservationUseCaseTest {
    
    @Mock
    private ReservationManager reservationManager;
    
    @Mock
    private SeatManager seatManager;
    
    @InjectMocks
    private CancelReservationUseCase cancelReservationUseCase;
    
    @Test
    @DisplayName("예약을 취소할 수 있다")
    void execute_success() {
        // given
        Long reservationId = 1L;
        Reservation reservation = Reservation.create("user123", 1L, 1L, new BigDecimal("50000"));
        Seat seat = Seat.of(1L, 1L, 10, SeatStatus.RESERVED, new BigDecimal("50000"));
        Reservation cancelledReservation = Reservation.of(1L, "user123", 1L, 1L, new BigDecimal("50000"),
            ReservationStatus.CANCELLED, LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
        Seat releasedSeat = Seat.of(1L, 1L, 10, SeatStatus.AVAILABLE, new BigDecimal("50000"));
        
        given(reservationManager.getReservationById(reservationId)).willReturn(reservation);
        given(seatManager.getSeatByIdWithLock(1L)).willReturn(seat);
        given(reservationManager.cancelReservation(reservation)).willReturn(cancelledReservation);
        given(seatManager.releaseSeat(seat)).willReturn(releasedSeat);
        
        // when
        ReservationResponse result = cancelReservationUseCase.execute(reservationId);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("CANCELLED");
        
        verify(reservationManager).getReservationById(reservationId);
        verify(seatManager).getSeatByIdWithLock(1L);
        verify(reservationManager).cancelReservation(reservation);
        verify(seatManager).releaseSeat(seat);
    }
    
    @Test
    @DisplayName("존재하지 않는 예약은 취소할 수 없다")
    void execute_reservationNotFound_throwsException() {
        // given
        Long reservationId = 999L;
        given(reservationManager.getReservationById(reservationId))
            .willThrow(new IllegalArgumentException("존재하지 않는 예약입니다"));
        
        // when & then
        assertThatThrownBy(() -> cancelReservationUseCase.execute(reservationId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("존재하지 않는 예약입니다");
        
        verify(reservationManager).getReservationById(reservationId);
        verify(seatManager, never()).getSeatByIdWithLock(any());
    }
    
    @Test
    @DisplayName("PENDING이 아닌 예약은 취소할 수 없다")
    void execute_notPending_throwsException() {
        // given
        Long reservationId = 1L;
        Reservation confirmedReservation = Reservation.of(1L, "user123", 1L, 1L, new BigDecimal("50000"),
            ReservationStatus.CONFIRMED, LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
        Seat seat = Seat.of(1L, 1L, 10, SeatStatus.SOLD, new BigDecimal("50000"));
        
        given(reservationManager.getReservationById(reservationId)).willReturn(confirmedReservation);
        given(seatManager.getSeatByIdWithLock(1L)).willReturn(seat);
        given(reservationManager.cancelReservation(confirmedReservation))
            .willThrow(new IllegalStateException("임시 예약 상태만 취소할 수 있습니다"));
        
        // when & then
        assertThatThrownBy(() -> cancelReservationUseCase.execute(reservationId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("임시 예약 상태만 취소할 수 있습니다");
        
        verify(reservationManager).getReservationById(reservationId);
        verify(reservationManager).cancelReservation(confirmedReservation);
    }
}
