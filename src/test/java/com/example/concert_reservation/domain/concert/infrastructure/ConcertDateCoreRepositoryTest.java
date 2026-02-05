package com.example.concert_reservation.domain.concert.infrastructure;

import com.example.concert_reservation.domain.concert.infrastructure.entity.ConcertDateEntity;
import com.example.concert_reservation.domain.concert.models.ConcertDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(ConcertDateCoreRepository.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("ConcertDateCoreRepository 통합 테스트")
class ConcertDateCoreRepositoryTest {
    
    @Autowired
    private ConcertDateCoreRepository concertDateCoreRepository;
    
    @Autowired
    private ConcertDateJpaRepository concertDateJpaRepository;
    
    @Test
    @DisplayName("예약 가능한 콘서트 날짜 목록을 조회할 수 있다")
    void findAvailableDates_success() {
        // given
        concertDateJpaRepository.save(new ConcertDateEntity(null, "아이유 콘서트", 
            LocalDate.of(2024, 12, 31), 50, 30));
        concertDateJpaRepository.save(new ConcertDateEntity(null, "BTS 콘서트", 
            LocalDate.of(2024, 12, 25), 50, 0)); // 매진
        concertDateJpaRepository.save(new ConcertDateEntity(null, "블랙핑크 콘서트", 
            LocalDate.of(2024, 12, 20), 50, 10));
        
        // when
        List<ConcertDate> result = concertDateCoreRepository.findAvailableDates();
        
        // then
        assertThat(result).hasSize(2); // 매진된 것 제외
        assertThat(result).allMatch(ConcertDate::hasAvailableSeats);
    }
    
    @Test
    @DisplayName("ID로 콘서트 날짜를 조회할 수 있다")
    void findById_success() {
        // given
        ConcertDateEntity saved = concertDateJpaRepository.save(
            new ConcertDateEntity(null, "아이유 콘서트", LocalDate.of(2024, 12, 31), 50, 30)
        );
        
        // when
        Optional<ConcertDate> result = concertDateCoreRepository.findById(saved.getId());
        
        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
        assertThat(result.get().getConcertName()).isEqualTo("아이유 콘서트");
    }
    
    @Test
    @DisplayName("날짜로 콘서트를 조회할 수 있다")
    void findByDate_success() {
        // given
        LocalDate date = LocalDate.of(2024, 12, 31);
        concertDateJpaRepository.save(
            new ConcertDateEntity(null, "아이유 콘서트", date, 50, 30)
        );
        
        // when
        Optional<ConcertDate> result = concertDateCoreRepository.findByDate(date);
        
        // then
        assertThat(result).isPresent();
        assertThat(result.get().getConcertDate()).isEqualTo(date);
    }
    
    @Test
    @DisplayName("존재하지 않는 날짜로 조회하면 빈 Optional을 반환한다")
    void findByDate_notFound_returnsEmpty() {
        // given
        LocalDate date = LocalDate.of(2025, 1, 1);
        
        // when
        Optional<ConcertDate> result = concertDateCoreRepository.findByDate(date);
        
        // then
        assertThat(result).isEmpty();
    }
}
