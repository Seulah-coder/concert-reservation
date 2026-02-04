package com.example.concert_reservation.domain.concert.repositories;

import com.example.concert_reservation.domain.concert.models.Seat;

import java.util.List;
import java.util.Optional;

/**
 * 좌석 조회 및 저장 Repository
 * 도메인 레이어의 인터페이스 (순수 자바)
 */
public interface SeatStoreRepository {
    
    /**
     * 좌석 저장 (생성 또는 업데이트)
     * @param seat 저장할 좌석
     * @return 저장된 좌석
     */
    Seat save(Seat seat);
    
    /**
     * 특정 콘서트 날짜의 모든 좌석 조회
     * @param concertDateId 콘서트 날짜 ID
     * @return 좌석 리스트
     */
    List<Seat> findByConcertDateId(Long concertDateId);
    
    /**
     * 특정 콘서트 날짜의 특정 좌석 조회
     * @param concertDateId 콘서트 날짜 ID
     * @param seatNumber 좌석 번호
     * @return 좌석 Optional
     */
    Optional<Seat> findByConcertDateIdAndSeatNumber(Long concertDateId, Integer seatNumber);
    
    /**
     * ID로 좌석 조회 (비관적 락 적용)
     * @param id 좌석 ID
     * @return 좌석 Optional
     */
    Optional<Seat> findByIdWithLock(Long id);
}
