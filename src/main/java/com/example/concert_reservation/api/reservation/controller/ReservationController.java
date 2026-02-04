package com.example.concert_reservation.api.reservation.controller;

import com.example.concert_reservation.api.reservation.dto.ReserveSeatRequest;
import com.example.concert_reservation.api.reservation.dto.ReservationResponse;
import com.example.concert_reservation.api.reservation.usecase.CancelReservationUseCase;
import com.example.concert_reservation.api.reservation.usecase.ReserveSeatUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 예약 관리 REST API Controller
 */
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {
    
    private final ReserveSeatUseCase reserveSeatUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;
    
    public ReservationController(ReserveSeatUseCase reserveSeatUseCase,
                                 CancelReservationUseCase cancelReservationUseCase) {
        this.reserveSeatUseCase = reserveSeatUseCase;
        this.cancelReservationUseCase = cancelReservationUseCase;
    }
    
    /**
     * 좌석 예약
     * POST /api/v1/reservations
     * 
     * @param request 예약 요청 (userId, seatId)
     * @return 예약 응답 (201 CREATED)
     */
    @PostMapping
    public ResponseEntity<ReservationResponse> reserveSeat(@RequestBody ReserveSeatRequest request) {
        ReservationResponse response = reserveSeatUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 예약 취소
     * DELETE /api/v1/reservations/{reservationId}
     * 
     * @param reservationId 예약 ID
     * @return 취소된 예약 응답
     */
    @DeleteMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> cancelReservation(@PathVariable Long reservationId) {
        ReservationResponse response = cancelReservationUseCase.execute(reservationId);
        return ResponseEntity.ok(response);
    }
}
