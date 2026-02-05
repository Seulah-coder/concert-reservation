package com.example.concert_reservation.api.concert.usecase;

import com.example.concert_reservation.api.concert.dto.SeatResponse;
import com.example.concert_reservation.domain.concert.components.ConcertReader;
import com.example.concert_reservation.domain.concert.components.SeatManager;
import com.example.concert_reservation.domain.concert.models.ConcertDate;
import com.example.concert_reservation.domain.concert.models.Seat;
import com.example.concert_reservation.domain.concert.models.SeatStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetSeatsUseCase 테스트")
class GetSeatsUseCaseTest {
    
    @Mock
    private ConcertReader concertReader;
    
    @Mock
    private SeatManager seatManager;
    
    @InjectMocks
    private GetSeatsUseCase getSeatsUseCase;
    
    @Test
    @DisplayName("특정 콘서트의 좌석 목록을 조회할 수 있다")
    void execute_success() {
        // given
        Long concertDateId = 1L;
        ConcertDate concertDate = ConcertDate.of(concertDateId, "아이유 콘서트", 
            LocalDate.of(2024, 12, 31), 50, 30);
        List<Seat> seats = List.of(
            Seat.of(1L, concertDateId, 1, SeatStatus.AVAILABLE, new BigDecimal("50000")),
            Seat.of(2L, concertDateId, 2, SeatStatus.RESERVED, new BigDecimal("50000")),
            Seat.of(3L, concertDateId, 3, SeatStatus.SOLD, new BigDecimal("50000"))
        );
        
        given(concertReader.getConcertDateById(concertDateId)).willReturn(concertDate);
        given(seatManager.getSeatsByConcert(concertDateId)).willReturn(seats);
        
        // when
        List<SeatResponse> result = getSeatsUseCase.execute(concertDateId);
        
        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getSeatId()).isEqualTo(1L);
        assertThat(result.get(0).getSeatNumber()).isEqualTo(1);
        assertThat(result.get(0).getStatus()).isEqualTo("AVAILABLE");
        assertThat(result.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("50000"));
        
        assertThat(result.get(1).getStatus()).isEqualTo("RESERVED");
        assertThat(result.get(2).getStatus()).isEqualTo("SOLD");
        
        verify(concertReader).getConcertDateById(concertDateId);
        verify(seatManager).getSeatsByConcert(concertDateId);
    }
    
    @Test
    @DisplayName("존재하지 않는 콘서트 ID로 조회하면 예외가 발생한다")
    void execute_concertNotFound_throwsException() {
        // given
        Long concertDateId = 999L;
        given(concertReader.getConcertDateById(concertDateId))
            .willThrow(new IllegalArgumentException("존재하지 않는 콘서트입니다"));
        
        // when & then
        assertThatThrownBy(() -> getSeatsUseCase.execute(concertDateId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("존재하지 않는 콘서트입니다");
        
        verify(concertReader).getConcertDateById(concertDateId);
    }
    
    @Test
    @DisplayName("좌석이 없는 콘서트는 빈 리스트를 반환한다")
    void execute_noSeats_returnsEmptyList() {
        // given
        Long concertDateId = 1L;
        ConcertDate concertDate = ConcertDate.of(concertDateId, "아이유 콘서트", 
            LocalDate.of(2024, 12, 31), 50, 50);
        
        given(concertReader.getConcertDateById(concertDateId)).willReturn(concertDate);
        given(seatManager.getSeatsByConcert(concertDateId)).willReturn(List.of());
        
        // when
        List<SeatResponse> result = getSeatsUseCase.execute(concertDateId);
        
        // then
        assertThat(result).isEmpty();
        verify(concertReader).getConcertDateById(concertDateId);
        verify(seatManager).getSeatsByConcert(concertDateId);
    }
}
