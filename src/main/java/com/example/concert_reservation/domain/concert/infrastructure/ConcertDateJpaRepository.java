package com.example.concert_reservation.domain.concert.infrastructure;

import com.example.concert_reservation.domain.concert.infrastructure.entity.ConcertDateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 콘서트 날짜 JPA Repository
 */
public interface ConcertDateJpaRepository extends JpaRepository<ConcertDateEntity, Long> {
    
    /**
     * 예약 가능한 콘서트 날짜 조회 (가용 좌석 > 0)
     * @return 예약 가능한 콘서트 날짜 리스트
     */
    @Query("SELECT c FROM ConcertDateEntity c WHERE c.availableSeats > 0 ORDER BY c.concertDate")
    List<ConcertDateEntity> findAvailableDates();
    
    /**
     * 날짜로 콘서트 조회
     * @param concertDate 조회할 날짜
     * @return 콘서트 날짜 Optional
     */
    Optional<ConcertDateEntity> findByConcertDate(LocalDate concertDate);
}
