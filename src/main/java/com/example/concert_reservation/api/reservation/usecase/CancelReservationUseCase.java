package com.example.concert_reservation.api.reservation.usecase;

import com.example.concert_reservation.api.reservation.dto.ReservationResponse;
import com.example.concert_reservation.domain.concert.components.SeatManager;
import com.example.concert_reservation.domain.concert.models.Seat;
import com.example.concert_reservation.domain.reservation.components.ReservationManager;
import com.example.concert_reservation.domain.reservation.models.Reservation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 취소 UseCase
 * 비즈니스 트랜잭션 경계
 */
@Service
public class CancelReservationUseCase {
    
    private final ReservationManager reservationManager;
    private final SeatManager seatManager;
    
    public CancelReservationUseCase(ReservationManager reservationManager, SeatManager seatManager) {
        this.reservationManager = reservationManager;
        this.seatManager = seatManager;
    }
    
    /**
     * 예약 취소 실행
     * 1. 예약 조회
     * 2. 예약 취소 (PENDING → CANCELLED)
     * 3. 좌석 해제 (RESERVED → AVAILABLE)
     * 
     * @param reservationId 예약 ID
     * @return 취소된 예약 응답
     */
    @Transactional
    public ReservationResponse execute(Long reservationId) {
        // 1. 예약 조회
        Reservation reservation = reservationManager.getReservationById(reservationId);
        
        // 2. 좌석 조회 (비관적 락)
        Seat seat = seatManager.getSeatByIdWithLock(reservation.getSeatId());
        
        // 3. 예약 취소
        Reservation cancelledReservation = reservationManager.cancelReservation(reservation);
        
        // 4. 좌석 해제
        seatManager.releaseSeat(seat);
        
        return toResponse(cancelledReservation);
    }
    
    /**
     * Domain 모델을 Response DTO로 변환
     */
    private ReservationResponse toResponse(Reservation reservation) {
        return new ReservationResponse(
            reservation.getId(),
            reservation.getUserId(),
            reservation.getSeatId(),
            reservation.getConcertDateId(),
            reservation.getPrice(),
            reservation.getStatus().name(),
            reservation.getReservedAt(),
            reservation.getExpiresAt(),
            reservation.getRemainingSeconds()
        );
    }
}
