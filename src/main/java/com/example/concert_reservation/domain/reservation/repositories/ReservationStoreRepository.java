package com.example.concert_reservation.domain.reservation.repositories;

import com.example.concert_reservation.domain.reservation.models.Reservation;
import com.example.concert_reservation.domain.reservation.models.ReservationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 예약 저장 및 조회 Repository
 * 도메인 레이어의 인터페이스 (순수 자바)
 */
public interface ReservationStoreRepository {
    
    /**
     * 예약 저장 (생성 또는 업데이트)
     * @param reservation 저장할 예약
     * @return 저장된 예약
     */
    Reservation save(Reservation reservation);
    
    /**
     * ID로 예약 조회
     * @param id 예약 ID
     * @return 예약 Optional
     */
    Optional<Reservation> findById(Long id);
    
    /**
     * 사용자 ID로 예약 목록 조회
     * @param userId 사용자 ID
     * @return 예약 리스트
     */
    List<Reservation> findByUserId(String userId);
    
    /**
     * 좌석 ID로 활성 예약 조회
     * @param seatId 좌석 ID
     * @return 활성 예약 Optional (PENDING 또는 CONFIRMED)
     */
    Optional<Reservation> findActiveBySeatId(Long seatId);
    
    /**
     * 만료 시간이 지난 PENDING 상태의 예약 목록 조회
     * @param now 현재 시간
     * @return 만료 대상 예약 리스트
     */
    List<Reservation> findExpiredReservations(LocalDateTime now);
}
