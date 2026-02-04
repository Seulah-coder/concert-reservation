package com.example.concert_reservation.domain.concert.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Seat 도메인 모델 테스트")
class SeatTest {
    
    @Test
    @DisplayName("새로운 좌석을 생성할 수 있다")
    void create_newSeat() {
        // given
        Long concertDateId = 1L;
        Integer seatNumber = 10;
        BigDecimal price = new BigDecimal("50000");
        
        // when
        Seat seat = Seat.create(concertDateId, seatNumber, price);
        
        // then
        assertThat(seat).isNotNull();
        assertThat(seat.getConcertDateId()).isEqualTo(concertDateId);
        assertThat(seat.getSeatNumber()).isEqualTo(seatNumber);
        assertThat(seat.getPrice()).isEqualTo(price);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }
    
    @Test
    @DisplayName("콘서트 날짜 ID가 null이면 예외가 발생한다")
    void create_withNullConcertDateId_throwsException() {
        // given
        Long concertDateId = null;
        Integer seatNumber = 10;
        BigDecimal price = new BigDecimal("50000");
        
        // when & then
        assertThatThrownBy(() -> Seat.create(concertDateId, seatNumber, price))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("콘서트 날짜 ID는 필수입니다");
    }
    
    @Test
    @DisplayName("좌석 번호가 0 이하면 예외가 발생한다")
    void create_withInvalidSeatNumber_throwsException() {
        // given
        Long concertDateId = 1L;
        Integer seatNumber = 0;
        BigDecimal price = new BigDecimal("50000");
        
        // when & then
        assertThatThrownBy(() -> Seat.create(concertDateId, seatNumber, price))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("좌석 번호는 1 이상이어야 합니다");
    }
    
    @Test
    @DisplayName("가격이 0 이하면 예외가 발생한다")
    void create_withInvalidPrice_throwsException() {
        // given
        Long concertDateId = 1L;
        Integer seatNumber = 10;
        BigDecimal price = BigDecimal.ZERO;
        
        // when & then
        assertThatThrownBy(() -> Seat.create(concertDateId, seatNumber, price))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("가격은 0보다 커야 합니다");
    }
    
    @Test
    @DisplayName("AVAILABLE 상태의 좌석을 예약할 수 있다")
    void reserve_fromAvailable_success() {
        // given
        Seat seat = Seat.create(1L, 10, new BigDecimal("50000"));
        
        // when
        seat.reserve();
        
        // then
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.RESERVED);
    }
    
    @Test
    @DisplayName("AVAILABLE이 아닌 좌석은 예약할 수 없다")
    void reserve_fromNonAvailable_throwsException() {
        // given
        Seat seat = Seat.of(1L, 1L, 10, SeatStatus.RESERVED, new BigDecimal("50000"));
        
        // when & then
        assertThatThrownBy(() -> seat.reserve())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("예약 가능한 좌석만 예약할 수 있습니다");
    }
    
    @Test
    @DisplayName("RESERVED 상태의 좌석을 판매할 수 있다")
    void sell_fromReserved_success() {
        // given
        Seat seat = Seat.of(1L, 1L, 10, SeatStatus.RESERVED, new BigDecimal("50000"));
        
        // when
        seat.sell();
        
        // then
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.SOLD);
    }
    
    @Test
    @DisplayName("RESERVED가 아닌 좌석은 판매할 수 없다")
    void sell_fromNonReserved_throwsException() {
        // given
        Seat seat = Seat.create(1L, 10, new BigDecimal("50000"));
        
        // when & then
        assertThatThrownBy(() -> seat.sell())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("임시 예약된 좌석만 판매할 수 있습니다");
    }
    
    @Test
    @DisplayName("RESERVED 상태의 좌석을 해제할 수 있다")
    void release_fromReserved_success() {
        // given
        Seat seat = Seat.of(1L, 1L, 10, SeatStatus.RESERVED, new BigDecimal("50000"));
        
        // when
        seat.release();
        
        // then
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }
    
    @Test
    @DisplayName("RESERVED가 아닌 좌석은 해제할 수 없다")
    void release_fromNonReserved_throwsException() {
        // given
        Seat seat = Seat.create(1L, 10, new BigDecimal("50000"));
        
        // when & then
        assertThatThrownBy(() -> seat.release())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("임시 예약된 좌석만 해제할 수 있습니다");
    }
    
    @Test
    @DisplayName("좌석 상태를 확인할 수 있다")
    void checkStatus() {
        // given
        Seat availableSeat = Seat.create(1L, 10, new BigDecimal("50000"));
        Seat reservedSeat = Seat.of(1L, 1L, 11, SeatStatus.RESERVED, new BigDecimal("50000"));
        Seat soldSeat = Seat.of(2L, 1L, 12, SeatStatus.SOLD, new BigDecimal("50000"));
        
        // when & then
        assertThat(availableSeat.isAvailable()).isTrue();
        assertThat(availableSeat.isReserved()).isFalse();
        assertThat(availableSeat.isSold()).isFalse();
        
        assertThat(reservedSeat.isAvailable()).isFalse();
        assertThat(reservedSeat.isReserved()).isTrue();
        assertThat(reservedSeat.isSold()).isFalse();
        
        assertThat(soldSeat.isAvailable()).isFalse();
        assertThat(soldSeat.isReserved()).isFalse();
        assertThat(soldSeat.isSold()).isTrue();
    }
}
