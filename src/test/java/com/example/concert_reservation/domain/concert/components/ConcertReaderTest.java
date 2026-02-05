package com.example.concert_reservation.domain.concert.components;

import com.example.concert_reservation.domain.concert.models.ConcertDate;
import com.example.concert_reservation.domain.concert.repositories.ConcertReaderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConcertReader 컴포넌트 테스트")
class ConcertReaderTest {
    
    @Mock
    private ConcertReaderRepository concertReaderRepository;
    
    @InjectMocks
    private ConcertReader concertReader;
    
    @Test
    @DisplayName("예약 가능한 콘서트 날짜 목록을 조회할 수 있다")
    void getAvailableDates_success() {
        // given
        List<ConcertDate> expectedDates = List.of(
            ConcertDate.of(1L, "아이유 콘서트", LocalDate.of(2024, 12, 31), 50, 30),
            ConcertDate.of(2L, "BTS 콘서트", LocalDate.of(2024, 12, 25), 50, 20)
        );
        given(concertReaderRepository.findAvailableDates()).willReturn(expectedDates);
        
        // when
        List<ConcertDate> result = concertReader.getAvailableDates();
        
        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(expectedDates);
        verify(concertReaderRepository).findAvailableDates();
    }
    
    @Test
    @DisplayName("ID로 콘서트를 조회할 수 있다")
    void getConcertDateById_success() {
        // given
        Long id = 1L;
        ConcertDate expectedConcert = ConcertDate.of(id, "아이유 콘서트", 
            LocalDate.of(2024, 12, 31), 50, 30);
        given(concertReaderRepository.findById(id)).willReturn(Optional.of(expectedConcert));
        
        // when
        ConcertDate result = concertReader.getConcertDateById(id);
        
        // then
        assertThat(result).isEqualTo(expectedConcert);
        verify(concertReaderRepository).findById(id);
    }
    
    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 예외가 발생한다")
    void getConcertDateById_notFound_throwsException() {
        // given
        Long id = 999L;
        given(concertReaderRepository.findById(id)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> concertReader.getConcertDateById(id))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("존재하지 않는 콘서트입니다");
        verify(concertReaderRepository).findById(id);
    }
    
    @Test
    @DisplayName("날짜로 콘서트를 조회할 수 있다")
    void getConcertDateByDate_success() {
        // given
        LocalDate date = LocalDate.of(2024, 12, 31);
        ConcertDate expectedConcert = ConcertDate.of(1L, "아이유 콘서트", date, 50, 30);
        given(concertReaderRepository.findByDate(date)).willReturn(Optional.of(expectedConcert));
        
        // when
        ConcertDate result = concertReader.getConcertDateByDate(date);
        
        // then
        assertThat(result).isEqualTo(expectedConcert);
        verify(concertReaderRepository).findByDate(date);
    }
    
    @Test
    @DisplayName("존재하지 않는 날짜로 조회하면 예외가 발생한다")
    void getConcertDateByDate_notFound_throwsException() {
        // given
        LocalDate date = LocalDate.of(2025, 1, 1);
        given(concertReaderRepository.findByDate(date)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> concertReader.getConcertDateByDate(date))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("해당 날짜에 콘서트가 없습니다");
        verify(concertReaderRepository).findByDate(date);
    }
    
    @Test
    @DisplayName("예약 가능한 콘서트를 검증할 수 있다")
    void validateAvailable_success() {
        // given
        ConcertDate concertDate = ConcertDate.of(1L, "아이유 콘서트", 
            LocalDate.of(2024, 12, 31), 50, 30);
        
        // when & then
        assertThatCode(() -> concertReader.validateAvailable(concertDate))
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("매진된 콘서트를 검증하면 예외가 발생한다")
    void validateAvailable_soldOut_throwsException() {
        // given
        ConcertDate soldOutConcert = ConcertDate.of(1L, "아이유 콘서트", 
            LocalDate.of(2024, 12, 31), 50, 0);
        
        // when & then
        assertThatThrownBy(() -> concertReader.validateAvailable(soldOutConcert))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("예약 가능한 좌석이 없습니다");
    }
}
