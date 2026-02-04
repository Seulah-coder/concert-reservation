package com.example.concert_reservation.domain.concert.repositories;

import com.example.concert_reservation.domain.concert.models.ConcertDate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 콘서트 날짜 조회 전용 Repository
 * 도메인 레이어의 인터페이스 (순수 자바)
 */
public interface ConcertReaderRepository {
    
    /**
     * 예약 가능한 콘서트 날짜 목록 조회
     * @return 예약 가능한 콘서트 날짜 리스트
     */
    List<ConcertDate> findAvailableDates();
    
    /**
     * ID로 콘서트 날짜 조회
     * @param id 콘서트 날짜 ID
     * @return 콘서트 날짜 Optional
     */
    Optional<ConcertDate> findById(Long id);
    
    /**
     * 날짜로 콘서트 조회
     * @param date 조회할 날짜
     * @return 콘서트 날짜 Optional
     */
    Optional<ConcertDate> findByDate(LocalDate date);
}
