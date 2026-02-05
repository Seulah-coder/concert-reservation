package com.example.concert_reservation.api.concert.usecase;

import com.example.concert_reservation.api.concert.dto.AvailableDateResponse;
import com.example.concert_reservation.domain.concert.components.ConcertReader;
import com.example.concert_reservation.domain.concert.models.ConcertDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 예약 가능한 콘서트 날짜 조회 UseCase
 * 비즈니스 트랜잭션 경계
 */
@Service
public class GetAvailableDatesUseCase {
    
    private final ConcertReader concertReader;
    
    public GetAvailableDatesUseCase(ConcertReader concertReader) {
        this.concertReader = concertReader;
    }
    
    /**
     * 예약 가능한 콘서트 날짜 목록 조회
     * @return 예약 가능한 날짜 리스트
     */
    @Transactional(readOnly = true)
    public List<AvailableDateResponse> execute() {
        List<ConcertDate> availableDates = concertReader.getAvailableDates();
        
        return availableDates.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Domain 모델을 Response DTO로 변환
     */
    private AvailableDateResponse toResponse(ConcertDate concertDate) {
        return new AvailableDateResponse(
            concertDate.getId(),
            concertDate.getConcertName(),
            concertDate.getConcertDate(),
            concertDate.getTotalSeats(),
            concertDate.getAvailableSeats()
        );
    }
}
