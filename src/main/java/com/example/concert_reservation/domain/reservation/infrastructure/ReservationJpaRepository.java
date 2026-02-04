package com.example.concert_reservation.domain.reservation.infrastructure;

import com.example.concert_reservation.domain.reservation.infrastructure.entity.ReservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 예약 JPA Repository
 */
public interface ReservationJpaRepository extends JpaRepository<ReservationEntity, Long> {
    
    /**
     * 사용자 ID로 예약 목록 조회
     * @param userId 사용자 ID
     * @return 예약 리스트
     */
    List<ReservationEntity> findByUserId(String userId);
    
    /**
     * 좌석 ID로 활성 예약 조회 (PENDING 또는 CONFIRMED)
     * @param seatId 좌석 ID
     * @return 활성 예약 Optional
     */
    @Query("SELECT r FROM ReservationEntity r WHERE r.seatId = :seatId AND r.status IN ('PENDING', 'CONFIRMED')")
    Optional<ReservationEntity> findActiveBySeatId(@Param("seatId") Long seatId);
    
    /**
     * 만료된 PENDING 예약 조회
     * @param now 현재 시간
     * @return 만료된 예약 리스트
     */
    @Query("SELECT r FROM ReservationEntity r WHERE r.status = 'PENDING' AND r.expiresAt < :now")
    List<ReservationEntity> findExpiredReservations(@Param("now") LocalDateTime now);
}
