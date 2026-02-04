package com.example.concert_reservation.domain.concert.infrastructure;

import com.example.concert_reservation.domain.concert.infrastructure.entity.SeatEntity;
import com.example.concert_reservation.domain.concert.models.Seat;
import com.example.concert_reservation.domain.concert.models.SeatStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(SeatCoreStoreRepository.class)
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("SeatCoreStoreRepository 통합 테스트")
class SeatCoreStoreRepositoryTest {
    
    @Autowired
    private SeatCoreStoreRepository seatCoreStoreRepository;
    
    @Autowired
    private SeatJpaRepository seatJpaRepository;
    
    @Test
    @DisplayName("좌석을 저장할 수 있다")
    void save_success() {
        // given
        Seat seat = Seat.create(1L, 10, new BigDecimal("50000"));
        
        // when
        Seat saved = seatCoreStoreRepository.save(seat);
        
        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getSeatNumber()).isEqualTo(10);
        assertThat(saved.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }
    
    @Test
    @DisplayName("특정 콘서트의 좌석 목록을 조회할 수 있다")
    void findByConcertDateId_success() {
        // given
        Long concertDateId = 1L;
        seatJpaRepository.save(new SeatEntity(null, concertDateId, 1, "AVAILABLE", new BigDecimal("50000")));
        seatJpaRepository.save(new SeatEntity(null, concertDateId, 2, "RESERVED", new BigDecimal("50000")));
        seatJpaRepository.save(new SeatEntity(null, 2L, 1, "AVAILABLE", new BigDecimal("60000"))); // 다른 콘서트
        
        // when
        List<Seat> result = seatCoreStoreRepository.findByConcertDateId(concertDateId);
        
        // then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(seat -> seat.getConcertDateId().equals(concertDateId));
    }
    
    @Test
    @DisplayName("콘서트 날짜 ID와 좌석 번호로 좌석을 조회할 수 있다")
    void findByConcertDateIdAndSeatNumber_success() {
        // given
        Long concertDateId = 1L;
        Integer seatNumber = 10;
        seatJpaRepository.save(new SeatEntity(null, concertDateId, seatNumber, "AVAILABLE", new BigDecimal("50000")));
        
        // when
        Optional<Seat> result = seatCoreStoreRepository.findByConcertDateIdAndSeatNumber(concertDateId, seatNumber);
        
        // then
        assertThat(result).isPresent();
        assertThat(result.get().getSeatNumber()).isEqualTo(seatNumber);
        assertThat(result.get().getConcertDateId()).isEqualTo(concertDateId);
    }
    
    @Test
    @DisplayName("비관적 락으로 좌석을 조회할 수 있다")
    void findByIdWithLock_success() {
        // given
        SeatEntity saved = seatJpaRepository.save(
            new SeatEntity(null, 1L, 10, "AVAILABLE", new BigDecimal("50000"))
        );
        
        // when
        Optional<Seat> result = seatCoreStoreRepository.findByIdWithLock(saved.getId());
        
        // then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(saved.getId());
    }
    
    @Test
    @DisplayName("좌석 상태를 업데이트할 수 있다")
    void update_status_success() {
        // given
        Seat seat = Seat.create(1L, 10, new BigDecimal("50000"));
        Seat saved = seatCoreStoreRepository.save(seat);
        
        // when
        saved.reserve();
        Seat updated = seatCoreStoreRepository.save(saved);
        
        // then
        assertThat(updated.getStatus()).isEqualTo(SeatStatus.RESERVED);
    }
}
