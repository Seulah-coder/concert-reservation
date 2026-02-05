package com.example.concert_reservation.domain.reservation.components;

import com.example.concert_reservation.domain.concert.components.SeatManager;
import com.example.concert_reservation.domain.concert.models.Seat;
import com.example.concert_reservation.domain.concert.models.SeatStatus;
import com.example.concert_reservation.domain.reservation.models.Reservation;
import com.example.concert_reservation.domain.reservation.models.ReservationStatus;
import com.example.concert_reservation.domain.reservation.repositories.ReservationStoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationManager 컴포넌트 테스트")
class ReservationManagerTest {
    
    @Mock
    private ReservationStoreRepository reservationStoreRepository;
    
    @Mock
    private SeatManager seatManager;
    
    @InjectMocks
    private ReservationManager reservationManager;
    
    @Test
    @DisplayName("ID로 예약을 조회할 수 있다")
    void getReservationById_success() {
        // given
        Long id = 1L;
        Reservation expectedReservation = Reservation.create("user123", 1L, 1L, new BigDecimal("50000"));
        given(reservationStoreRepository.findById(id)).willReturn(Optional.of(expectedReservation));
        
        // when
        Reservation result = reservationManager.getReservationById(id);
        
        // then
        assertThat(result).isEqualTo(expectedReservation);
        verify(reservationStoreRepository).findById(id);
    }
    
    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 예외가 발생한다")
    void getReservationById_notFound_throwsException() {
        // given
        Long id = 999L;
        given(reservationStoreRepository.findById(id)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> reservationManager.getReservationById(id))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("존재하지 않는 예약입니다");
        verify(reservationStoreRepository).findById(id);
    }
    
    @Test
    @DisplayName("사용자의 예약 목록을 조회할 수 있다")
    void getReservationsByUser_success() {
        // given
        String userId = "user123";
        List<Reservation> expectedReservations = List.of(
            Reservation.create(userId, 1L, 1L, new BigDecimal("50000")),
            Reservation.create(userId, 2L, 1L, new BigDecimal("50000"))
        );
        given(reservationStoreRepository.findByUserId(userId)).willReturn(expectedReservations);
        
        // when
        List<Reservation> result = reservationManager.getReservationsByUser(userId);
        
        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(expectedReservations);
        verify(reservationStoreRepository).findByUserId(userId);
    }
    
    @Test
    @DisplayName("좌석에 활성 예약이 있는지 확인할 수 있다")
    void hasActiveReservation_returnsTrue() {
        // given
        Long seatId = 1L;
        Reservation reservation = Reservation.create("user123", seatId, 1L, new BigDecimal("50000"));
        given(reservationStoreRepository.findActiveBySeatId(seatId)).willReturn(Optional.of(reservation));
        
        // when
        boolean result = reservationManager.hasActiveReservation(seatId);
        
        // then
        assertThat(result).isTrue();
        verify(reservationStoreRepository).findActiveBySeatId(seatId);
    }
    
    @Test
    @DisplayName("예약을 확정할 수 있다")
    void confirmReservation_success() {
        // given
        Reservation reservation = Reservation.create("user123", 1L, 1L, new BigDecimal("50000"));
        Reservation confirmedReservation = Reservation.of(1L, "user123", 1L, 1L, new BigDecimal("50000"),
            ReservationStatus.CONFIRMED, LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
        given(reservationStoreRepository.save(any(Reservation.class))).willReturn(confirmedReservation);
        
        // when
        Reservation result = reservationManager.confirmReservation(reservation);
        
        // then
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        verify(reservationStoreRepository).save(any(Reservation.class));
    }
    
    @Test
    @DisplayName("예약을 취소할 수 있다")
    void cancelReservation_success() {
        // given
        Reservation reservation = Reservation.create("user123", 1L, 1L, new BigDecimal("50000"));
        Reservation cancelledReservation = Reservation.of(1L, "user123", 1L, 1L, new BigDecimal("50000"),
            ReservationStatus.CANCELLED, LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
        given(reservationStoreRepository.save(any(Reservation.class))).willReturn(cancelledReservation);
        
        // when
        Reservation result = reservationManager.cancelReservation(reservation);
        
        // then
        assertThat(result.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(reservationStoreRepository).save(any(Reservation.class));
    }
    
    @Test
    @DisplayName("만료된 예약을 처리할 수 있다")
    void expireReservations_success() {
        // given
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expiredReservations = List.of(
            Reservation.of(1L, "user1", 1L, 1L, new BigDecimal("50000"),
                ReservationStatus.PENDING, now.minusMinutes(10), now.minusMinutes(5)),
            Reservation.of(2L, "user2", 2L, 1L, new BigDecimal("50000"),
                ReservationStatus.PENDING, now.minusMinutes(10), now.minusMinutes(5))
        );
        given(reservationStoreRepository.findExpiredReservations(any(LocalDateTime.class)))
            .willReturn(expiredReservations);
        given(reservationStoreRepository.save(any(Reservation.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        
        // Mock SeatManager - 각 예약에 대한 좌석 조회 및 해제
        Seat seat1 = Seat.of(1L, 1L, 1, SeatStatus.RESERVED, new BigDecimal("50000"));
        Seat seat2 = Seat.of(2L, 1L, 2, SeatStatus.RESERVED, new BigDecimal("50000"));
        given(seatManager.getSeatByIdWithLock(1L)).willReturn(seat1);
        given(seatManager.getSeatByIdWithLock(2L)).willReturn(seat2);
        given(seatManager.releaseSeat(any(Seat.class)))
            .willAnswer(invocation -> invocation.getArgument(0));
        
        // when
        int count = reservationManager.expireReservations();
        
        // then
        assertThat(count).isEqualTo(2);
        verify(reservationStoreRepository).findExpiredReservations(any(LocalDateTime.class));
        verify(reservationStoreRepository, times(2)).save(any(Reservation.class));
        verify(seatManager, times(2)).getSeatByIdWithLock(anyLong());
        verify(seatManager, times(2)).releaseSeat(any(Seat.class));
    }
}
