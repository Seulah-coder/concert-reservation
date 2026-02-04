package com.example.concert_reservation.api.concert.usecase;

import com.example.concert_reservation.api.concert.dto.SeatResponse;
import com.example.concert_reservation.domain.concert.components.ConcertReader;
import com.example.concert_reservation.domain.concert.components.SeatManager;
import com.example.concert_reservation.domain.concert.models.ConcertDate;
import com.example.concert_reservation.domain.concert.models.Seat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 특정 콘서트의 좌석 조회 UseCase
 * 비즈니스 트랜잭션 경계
 */
@Service
public class GetSeatsUseCase {
    
    private final ConcertReader concertReader;
    private final SeatManager seatManager;
    
    public GetSeatsUseCase(ConcertReader concertReader, SeatManager seatManager) {
        this.concertReader = concertReader;
        this.seatManager = seatManager;
    }
    
    /**
     * 특정 콘서트 날짜의 좌석 목록 조회
     * @param concertDateId 콘서트 날짜 ID
     * @return 좌석 리스트
     */
    @Transactional(readOnly = true)
    public List<SeatResponse> execute(Long concertDateId) {
        // 콘서트 날짜 존재 여부 검증
        ConcertDate concertDate = concertReader.getConcertDateById(concertDateId);
        
        // 좌석 목록 조회
        List<Seat> seats = seatManager.getSeatsByConcert(concertDateId);
        
        return seats.stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Domain 모델을 Response DTO로 변환
     */
    private SeatResponse toResponse(Seat seat) {
        return new SeatResponse(
            seat.getId(),
            seat.getSeatNumber(),
            seat.getStatus().name(),
            seat.getPrice()
        );
    }
}
