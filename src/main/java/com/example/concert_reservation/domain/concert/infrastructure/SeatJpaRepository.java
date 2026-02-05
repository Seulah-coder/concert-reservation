package com.example.concert_reservation.domain.concert.infrastructure;

import com.example.concert_reservation.domain.concert.infrastructure.entity.SeatEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 좌석 JPA Repository
 */
public interface SeatJpaRepository extends JpaRepository<SeatEntity, Long> {
    
    /**
     * 특정 콘서트 날짜의 모든 좌석 조회
     * @param concertDateId 콘서트 날짜 ID
     * @return 좌석 리스트
     */
    List<SeatEntity> findByConcertDateId(Long concertDateId);
    
    /**
     * 특정 콘서트 날짜의 특정 좌석 조회
     * @param concertDateId 콘서트 날짜 ID
     * @param seatNumber 좌석 번호
     * @return 좌석 Optional
     */
    Optional<SeatEntity> findByConcertDateIdAndSeatNumber(Long concertDateId, Integer seatNumber);
    
    /**
     * ID로 좌석 조회 (비관적 락 적용)
     * @param id 좌석 ID
     * @return 좌석 Optional
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SeatEntity s WHERE s.id = :id")
    Optional<SeatEntity> findByIdWithLock(@Param("id") Long id);
}
