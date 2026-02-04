package com.example.concert_reservation.domain.concert.components;

import com.example.concert_reservation.domain.concert.models.ConcertDate;
import com.example.concert_reservation.domain.concert.repositories.ConcertReaderRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 콘서트 조회 비즈니스 로직
 * 도메인 레이어의 컴포넌트
 */
@Component
public class ConcertReader {
    
    private final ConcertReaderRepository concertReaderRepository;
    
    public ConcertReader(ConcertReaderRepository concertReaderRepository) {
        this.concertReaderRepository = concertReaderRepository;
    }
    
    /**
     * 예약 가능한 콘서트 날짜 목록 조회
     * @return 예약 가능한 콘서트 날짜 리스트
     */
    public List<ConcertDate> getAvailableDates() {
        return concertReaderRepository.findAvailableDates();
    }
    
    /**
     * ID로 콘서트 조회 및 검증
     * @param id 콘서트 날짜 ID
     * @return 콘서트 날짜
     * @throws IllegalArgumentException 존재하지 않는 콘서트
     */
    public ConcertDate getConcertDateById(Long id) {
        return concertReaderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 콘서트입니다. ID: " + id));
    }
    
    /**
     * 날짜로 콘서트 조회 및 검증
     * @param date 조회할 날짜
     * @return 콘서트 날짜
     * @throws IllegalArgumentException 존재하지 않는 콘서트
     */
    public ConcertDate getConcertDateByDate(LocalDate date) {
        return concertReaderRepository.findByDate(date)
            .orElseThrow(() -> new IllegalArgumentException("해당 날짜에 콘서트가 없습니다. Date: " + date));
    }
    
    /**
     * 콘서트 날짜의 예약 가능 여부 검증
     * @param concertDate 콘서트 날짜
     * @throws IllegalStateException 예약 불가능한 경우
     */
    public void validateAvailable(ConcertDate concertDate) {
        if (concertDate.isSoldOut()) {
            throw new IllegalStateException("예약 가능한 좌석이 없습니다. 콘서트: " + concertDate.getConcertName());
        }
    }
}
