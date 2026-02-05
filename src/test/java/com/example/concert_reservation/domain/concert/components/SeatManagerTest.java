package com.example.concert_reservation.domain.concert.components;

import com.example.concert_reservation.domain.concert.models.Seat;
import com.example.concert_reservation.domain.concert.models.SeatStatus;
import com.example.concert_reservation.domain.concert.repositories.SeatStoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeatManager 컴포넌트 테스트")
class SeatManagerTest {
    
    @Mock
    private SeatStoreRepository seatStoreRepository;
    
    @InjectMocks
    private SeatManager seatManager;
    
    @Test
    @DisplayName("특정 콘서트의 좌석 목록을 조회할 수 있다")
    void getSeatsByConcert_success() {
        // given
        Long concertDateId = 1L;
        List<Seat> expectedSeats = List.of(
            Seat.of(1L, concertDateId, 1, SeatStatus.AVAILABLE, new BigDecimal("50000")),
            Seat.of(2L, concertDateId, 2, SeatStatus.RESERVED, new BigDecimal("50000"))
        );
        given(seatStoreRepository.findByConcertDateId(concertDateId)).willReturn(expectedSeats);
        
        // when
        List<Seat> result = seatManager.getSeatsByConcert(concertDateId);
        
        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(expectedSeats);
        verify(seatStoreRepository).findByConcertDateId(concertDateId);
    }
    
    @Test
    @DisplayName("비관적 락으로 좌석을 조회할 수 있다")
    void getSeatByIdWithLock_success() {
        // given
        Long seatId = 1L;
        Seat expectedSeat = Seat.of(seatId, 1L, 10, SeatStatus.AVAILABLE, new BigDecimal("50000"));
        given(seatStoreRepository.findByIdWithLock(seatId)).willReturn(Optional.of(expectedSeat));
        
        // when
        Seat result = seatManager.getSeatByIdWithLock(seatId);
        
        // then
        assertThat(result).isEqualTo(expectedSeat);
        verify(seatStoreRepository).findByIdWithLock(seatId);
    }
    
    @Test
    @DisplayName("존재하지 않는 좌석을 조회하면 예외가 발생한다")
    void getSeatByIdWithLock_notFound_throwsException() {
        // given
        Long seatId = 999L;
        given(seatStoreRepository.findByIdWithLock(seatId)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> seatManager.getSeatByIdWithLock(seatId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("존재하지 않는 좌석입니다");
        verify(seatStoreRepository).findByIdWithLock(seatId);
    }
    
    @Test
    @DisplayName("예약 가능한 좌석을 검증할 수 있다")
    void validateAvailableForReservation_success() {
        // given
        Seat seat = Seat.of(1L, 1L, 10, SeatStatus.AVAILABLE, new BigDecimal("50000"));
        
        // when & then
        assertThatCode(() -> seatManager.validateAvailableForReservation(seat))
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("예약 불가능한 좌석을 검증하면 예외가 발생한다")
    void validateAvailableForReservation_notAvailable_throwsException() {
        // given
        Seat seat = Seat.of(1L, 1L, 10, SeatStatus.RESERVED, new BigDecimal("50000"));
        
        // when & then
        assertThatThrownBy(() -> seatManager.validateAvailableForReservation(seat))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("예약 가능한 좌석이 아닙니다");
    }
    
    @Test
    @DisplayName("좌석을 예약할 수 있다")
    void reserveSeat_success() {
        // given
        Seat seat = Seat.of(1L, 1L, 10, SeatStatus.AVAILABLE, new BigDecimal("50000"));
        Seat reservedSeat = Seat.of(1L, 1L, 10, SeatStatus.RESERVED, new BigDecimal("50000"));
        given(seatStoreRepository.save(any(Seat.class))).willReturn(reservedSeat);
        
        // when
        Seat result = seatManager.reserveSeat(seat);
        
        // then
        assertThat(result.getStatus()).isEqualTo(SeatStatus.RESERVED);
        verify(seatStoreRepository).save(any(Seat.class));
    }
    
    @Test
    @DisplayName("예약 불가능한 좌석을 예약하면 예외가 발생한다")
    void reserveSeat_notAvailable_throwsException() {
        // given
        Seat seat = Seat.of(1L, 1L, 10, SeatStatus.RESERVED, new BigDecimal("50000"));
        
        // when & then
        assertThatThrownBy(() -> seatManager.reserveSeat(seat))
            .isInstanceOf(IllegalStateException.class);
        verify(seatStoreRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("좌석을 판매할 수 있다")
    void sellSeat_success() {
        // given
        Seat seat = Seat.of(1L, 1L, 10, SeatStatus.RESERVED, new BigDecimal("50000"));
        Seat soldSeat = Seat.of(1L, 1L, 10, SeatStatus.SOLD, new BigDecimal("50000"));
        given(seatStoreRepository.save(any(Seat.class))).willReturn(soldSeat);
        
        // when
        Seat result = seatManager.sellSeat(seat);
        
        // then
        assertThat(result.getStatus()).isEqualTo(SeatStatus.SOLD);
        verify(seatStoreRepository).save(any(Seat.class));
    }
    
    @Test
    @DisplayName("예약되지 않은 좌석을 판매하면 예외가 발생한다")
    void sellSeat_notReserved_throwsException() {
        // given
        Seat seat = Seat.of(1L, 1L, 10, SeatStatus.AVAILABLE, new BigDecimal("50000"));
        
        // when & then
        assertThatThrownBy(() -> seatManager.sellSeat(seat))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("임시 예약된 좌석만 판매할 수 있습니다");
        verify(seatStoreRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("좌석을 해제할 수 있다")
    void releaseSeat_success() {
        // given
        Seat seat = Seat.of(1L, 1L, 10, SeatStatus.RESERVED, new BigDecimal("50000"));
        Seat releasedSeat = Seat.of(1L, 1L, 10, SeatStatus.AVAILABLE, new BigDecimal("50000"));
        given(seatStoreRepository.save(any(Seat.class))).willReturn(releasedSeat);
        
        // when
        Seat result = seatManager.releaseSeat(seat);
        
        // then
        assertThat(result.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        verify(seatStoreRepository).save(any(Seat.class));
    }
    
    @Test
    @DisplayName("예약되지 않은 좌석을 해제하면 예외가 발생한다")
    void releaseSeat_notReserved_throwsException() {
        // given
        Seat seat = Seat.of(1L, 1L, 10, SeatStatus.AVAILABLE, new BigDecimal("50000"));
        
        // when & then
        assertThatThrownBy(() -> seatManager.releaseSeat(seat))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("임시 예약된 좌석만 해제할 수 있습니다");
        verify(seatStoreRepository, never()).save(any());
    }
}
