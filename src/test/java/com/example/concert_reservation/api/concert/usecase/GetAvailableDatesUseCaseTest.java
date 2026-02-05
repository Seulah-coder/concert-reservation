package com.example.concert_reservation.api.concert.usecase;

import com.example.concert_reservation.api.concert.dto.AvailableDateResponse;
import com.example.concert_reservation.domain.concert.components.ConcertReader;
import com.example.concert_reservation.domain.concert.models.ConcertDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetAvailableDatesUseCase 테스트")
class GetAvailableDatesUseCaseTest {
    
    @Mock
    private ConcertReader concertReader;
    
    @InjectMocks
    private GetAvailableDatesUseCase getAvailableDatesUseCase;
    
    @Test
    @DisplayName("예약 가능한 콘서트 날짜 목록을 조회할 수 있다")
    void execute_success() {
        // given
        List<ConcertDate> concertDates = List.of(
            ConcertDate.of(1L, "아이유 콘서트", LocalDate.of(2024, 12, 31), 50, 30),
            ConcertDate.of(2L, "BTS 콘서트", LocalDate.of(2024, 12, 25), 50, 20)
        );
        given(concertReader.getAvailableDates()).willReturn(concertDates);
        
        // when
        List<AvailableDateResponse> result = getAvailableDatesUseCase.execute();
        
        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getConcertDateId()).isEqualTo(1L);
        assertThat(result.get(0).getConcertName()).isEqualTo("아이유 콘서트");
        assertThat(result.get(0).getAvailableSeats()).isEqualTo(30);
        
        assertThat(result.get(1).getConcertDateId()).isEqualTo(2L);
        assertThat(result.get(1).getConcertName()).isEqualTo("BTS 콘서트");
        
        verify(concertReader).getAvailableDates();
    }
    
    @Test
    @DisplayName("예약 가능한 콘서트가 없으면 빈 리스트를 반환한다")
    void execute_emptyList() {
        // given
        given(concertReader.getAvailableDates()).willReturn(List.of());
        
        // when
        List<AvailableDateResponse> result = getAvailableDatesUseCase.execute();
        
        // then
        assertThat(result).isEmpty();
        verify(concertReader).getAvailableDates();
    }
}
