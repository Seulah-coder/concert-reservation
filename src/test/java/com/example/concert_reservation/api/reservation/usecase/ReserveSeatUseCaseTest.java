package com.example.concert_reservation.api.reservation.usecase;

import com.example.concert_reservation.api.reservation.dto.ReserveSeatRequest;
import com.example.concert_reservation.api.reservation.dto.ReservationResponse;
import com.example.concert_reservation.domain.concert.components.SeatManager;
import com.example.concert_reservation.domain.concert.models.Seat;
import com.example.concert_reservation.domain.concert.models.SeatStatus;
import com.example.concert_reservation.domain.reservation.components.ReservationManager;
import com.example.concert_reservation.domain.reservation.models.Reservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReserveSeatUseCase 테스트")
class ReserveSeatUseCaseTest {
    
    @Mock
    private ReservationManager reservationManager;
    
    @Mock
    private SeatManager seatManager;
    
    @InjectMocks
    private ReserveSeatUseCase reserveSeatUseCase;
    
    @Test
    @DisplayName("좌석을 예약할 수 있다")
    void execute_success() {
        // given
        ReserveSeatRequest request = new ReserveSeatRequest("user123", 1L);
        Seat seat = Seat.of(1L, 1L, 10, SeatStatus.AVAILABLE, new BigDecimal("50000"));
        Seat reservedSeat = Seat.of(1L, 1L, 10, SeatStatus.RESERVED, new BigDecimal("50000"));
        Reservation reservation = Reservation.create("user123", 1L, 1L, new BigDecimal("50000"));
        
        given(seatManager.getSeatByIdWithLock(1L)).willReturn(seat);
        given(reservationManager.hasActiveReservation(1L)).willReturn(false);
        given(seatManager.reserveSeat(seat)).willReturn(reservedSeat);
        given(reservationManager.saveReservation(any(Reservation.class))).willReturn(reservation);
        
        // when
        ReservationResponse result = reserveSeatUseCase.execute(request);
        
        // then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo("user123");
        assertThat(result.getSeatId()).isEqualTo(1L);
        assertThat(result.getStatus()).isEqualTo("PENDING");
        
        verify(seatManager).getSeatByIdWithLock(1L);
        verify(reservationManager).hasActiveReservation(1L);
        verify(seatManager).reserveSeat(seat);
        verify(reservationManager).saveReservation(any(Reservation.class));
    }
    
    @Test
    @DisplayName("이미 예약된 좌석은 예약할 수 없다")
    void execute_alreadyReserved_throwsException() {
        // given
        ReserveSeatRequest request = new ReserveSeatRequest("user123", 1L);
        Seat seat = Seat.of(1L, 1L, 10, SeatStatus.AVAILABLE, new BigDecimal("50000"));
        
        given(seatManager.getSeatByIdWithLock(1L)).willReturn(seat);
        given(reservationManager.hasActiveReservation(1L)).willReturn(true);
        
        // when & then
        assertThatThrownBy(() -> reserveSeatUseCase.execute(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("이미 예약된 좌석입니다");
        
        verify(seatManager).getSeatByIdWithLock(1L);
        verify(reservationManager).hasActiveReservation(1L);
        verify(seatManager, never()).reserveSeat(any());
    }
    
    @Test
    @DisplayName("존재하지 않는 좌석은 예약할 수 없다")
    void execute_seatNotFound_throwsException() {
        // given
        ReserveSeatRequest request = new ReserveSeatRequest("user123", 999L);
        given(seatManager.getSeatByIdWithLock(999L))
            .willThrow(new IllegalArgumentException("존재하지 않는 좌석입니다"));
        
        // when & then
        assertThatThrownBy(() -> reserveSeatUseCase.execute(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("존재하지 않는 좌석입니다");
        
        verify(seatManager).getSeatByIdWithLock(999L);
    }
}
