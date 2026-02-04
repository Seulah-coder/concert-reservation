package com.example.concert_reservation.api.concert.controller;

import com.example.concert_reservation.api.concert.dto.AvailableDateResponse;
import com.example.concert_reservation.api.concert.dto.SeatResponse;
import com.example.concert_reservation.api.concert.usecase.GetAvailableDatesUseCase;
import com.example.concert_reservation.api.concert.usecase.GetSeatsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 콘서트 조회 REST API Controller
 */
@RestController
@RequestMapping("/api/v1/concerts")
public class ConcertController {
    
    private final GetAvailableDatesUseCase getAvailableDatesUseCase;
    private final GetSeatsUseCase getSeatsUseCase;
    
    public ConcertController(GetAvailableDatesUseCase getAvailableDatesUseCase,
                             GetSeatsUseCase getSeatsUseCase) {
        this.getAvailableDatesUseCase = getAvailableDatesUseCase;
        this.getSeatsUseCase = getSeatsUseCase;
    }
    
    /**
     * 예약 가능한 콘서트 날짜 조회
     * GET /api/v1/concerts/dates
     * 
     * @return 예약 가능한 날짜 리스트
     */
    @GetMapping("/dates")
    public ResponseEntity<List<AvailableDateResponse>> getAvailableDates() {
        List<AvailableDateResponse> dates = getAvailableDatesUseCase.execute();
        return ResponseEntity.ok(dates);
    }
    
    /**
     * 특정 콘서트 날짜의 좌석 조회
     * GET /api/v1/concerts/{concertDateId}/seats
     * 
     * @param concertDateId 콘서트 날짜 ID
     * @return 좌석 리스트
     */
    @GetMapping("/{concertDateId}/seats")
    public ResponseEntity<List<SeatResponse>> getSeats(@PathVariable Long concertDateId) {
        List<SeatResponse> seats = getSeatsUseCase.execute(concertDateId);
        return ResponseEntity.ok(seats);
    }
}
