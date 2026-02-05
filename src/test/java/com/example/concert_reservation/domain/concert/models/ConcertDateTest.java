package com.example.concert_reservation.domain.concert.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ConcertDate 도메인 모델 테스트")
class ConcertDateTest {
    
    @Test
    @DisplayName("새로운 콘서트 날짜를 생성할 수 있다")
    void create_newConcertDate() {
        // given
        String name = "아이유 콘서트";
        LocalDate date = LocalDate.of(2024, 12, 31);
        Integer totalSeats = 50;
        
        // when
        ConcertDate concertDate = ConcertDate.create(name, date, totalSeats);
        
        // then
        assertThat(concertDate).isNotNull();
        assertThat(concertDate.getConcertName()).isEqualTo(name);
        assertThat(concertDate.getConcertDate()).isEqualTo(date);
        assertThat(concertDate.getTotalSeats()).isEqualTo(totalSeats);
        assertThat(concertDate.getAvailableSeats()).isEqualTo(totalSeats);
    }
    
    @Test
    @DisplayName("콘서트 이름이 null이면 예외가 발생한다")
    void create_withNullName_throwsException() {
        // given
        String name = null;
        LocalDate date = LocalDate.of(2024, 12, 31);
        Integer totalSeats = 50;
        
        // when & then
        assertThatThrownBy(() -> ConcertDate.create(name, date, totalSeats))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("콘서트 이름은 필수입니다");
    }
    
    @Test
    @DisplayName("콘서트 날짜가 null이면 예외가 발생한다")
    void create_withNullDate_throwsException() {
        // given
        String name = "아이유 콘서트";
        LocalDate date = null;
        Integer totalSeats = 50;
        
        // when & then
        assertThatThrownBy(() -> ConcertDate.create(name, date, totalSeats))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("콘서트 날짜는 필수입니다");
    }
    
    @Test
    @DisplayName("총 좌석 수가 0 이하면 예외가 발생한다")
    void create_withInvalidTotalSeats_throwsException() {
        // given
        String name = "아이유 콘서트";
        LocalDate date = LocalDate.of(2024, 12, 31);
        Integer totalSeats = 0;
        
        // when & then
        assertThatThrownBy(() -> ConcertDate.create(name, date, totalSeats))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("총 좌석 수는 1 이상이어야 합니다");
    }
    
    @Test
    @DisplayName("가용 좌석을 감소시킬 수 있다")
    void decreaseAvailableSeats_success() {
        // given
        ConcertDate concertDate = ConcertDate.create("아이유 콘서트", LocalDate.of(2024, 12, 31), 50);
        Integer initialAvailable = concertDate.getAvailableSeats();
        
        // when
        concertDate.decreaseAvailableSeats();
        
        // then
        assertThat(concertDate.getAvailableSeats()).isEqualTo(initialAvailable - 1);
    }
    
    @Test
    @DisplayName("가용 좌석이 0일 때 감소시키면 예외가 발생한다")
    void decreaseAvailableSeats_whenZero_throwsException() {
        // given
        ConcertDate concertDate = ConcertDate.of(1L, "아이유 콘서트", 
            LocalDate.of(2024, 12, 31), 50, 0);
        
        // when & then
        assertThatThrownBy(() -> concertDate.decreaseAvailableSeats())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("예약 가능한 좌석이 없습니다");
    }
    
    @Test
    @DisplayName("가용 좌석을 증가시킬 수 있다")
    void increaseAvailableSeats_success() {
        // given
        ConcertDate concertDate = ConcertDate.of(1L, "아이유 콘서트", 
            LocalDate.of(2024, 12, 31), 50, 40);
        
        // when
        concertDate.increaseAvailableSeats();
        
        // then
        assertThat(concertDate.getAvailableSeats()).isEqualTo(41);
    }
    
    @Test
    @DisplayName("가용 좌석이 총 좌석과 같을 때 증가시키면 예외가 발생한다")
    void increaseAvailableSeats_whenFull_throwsException() {
        // given
        ConcertDate concertDate = ConcertDate.create("아이유 콘서트", 
            LocalDate.of(2024, 12, 31), 50);
        
        // when & then
        assertThatThrownBy(() -> concertDate.increaseAvailableSeats())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("가용 좌석 수가 총 좌석 수를 초과할 수 없습니다");
    }
    
    @Test
    @DisplayName("예약 가능한 좌석이 있는지 확인할 수 있다")
    void hasAvailableSeats_returnsTrue() {
        // given
        ConcertDate concertDate = ConcertDate.create("아이유 콘서트", 
            LocalDate.of(2024, 12, 31), 50);
        
        // when
        boolean result = concertDate.hasAvailableSeats();
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("좌석이 매진되었는지 확인할 수 있다")
    void isSoldOut_returnsTrue() {
        // given
        ConcertDate concertDate = ConcertDate.of(1L, "아이유 콘서트", 
            LocalDate.of(2024, 12, 31), 50, 0);
        
        // when
        boolean result = concertDate.isSoldOut();
        
        // then
        assertThat(result).isTrue();
        assertThat(concertDate.hasAvailableSeats()).isFalse();
    }
}
