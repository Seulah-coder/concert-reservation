package com.example.concert_reservation.domain.concert.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SeatStatus 테스트")
class SeatStatusTest {
    
    @Test
    @DisplayName("AVAILABLE 상태는 예약 가능하다")
    void available_isAvailable() {
        // when
        boolean result = SeatStatus.AVAILABLE.isAvailable();
        
        // then
        assertThat(result).isTrue();
        assertThat(SeatStatus.AVAILABLE.isReserved()).isFalse();
        assertThat(SeatStatus.AVAILABLE.isSold()).isFalse();
    }
    
    @Test
    @DisplayName("RESERVED 상태는 임시 예약 상태다")
    void reserved_isReserved() {
        // when
        boolean result = SeatStatus.RESERVED.isReserved();
        
        // then
        assertThat(result).isTrue();
        assertThat(SeatStatus.RESERVED.isAvailable()).isFalse();
        assertThat(SeatStatus.RESERVED.isSold()).isFalse();
    }
    
    @Test
    @DisplayName("SOLD 상태는 판매 완료 상태다")
    void sold_isSold() {
        // when
        boolean result = SeatStatus.SOLD.isSold();
        
        // then
        assertThat(result).isTrue();
        assertThat(SeatStatus.SOLD.isAvailable()).isFalse();
        assertThat(SeatStatus.SOLD.isReserved()).isFalse();
    }
}
