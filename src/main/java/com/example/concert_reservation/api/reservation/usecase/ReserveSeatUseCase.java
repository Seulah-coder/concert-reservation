package com.example.concert_reservation.api.reservation.usecase;

import com.example.concert_reservation.api.reservation.dto.ReserveSeatRequest;
import com.example.concert_reservation.api.reservation.dto.ReservationResponse;
import com.example.concert_reservation.domain.concert.components.SeatManager;
import com.example.concert_reservation.domain.concert.models.Seat;
import com.example.concert_reservation.domain.reservation.components.ReservationManager;
import com.example.concert_reservation.domain.reservation.models.Reservation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 좌석 예약 UseCase
 * 비즈니스 트랜잭션 경계
 */
@Service
public class ReserveSeatUseCase {
    
    private final ReservationManager reservationManager;
    private final SeatManager seatManager;
    
    public ReserveSeatUseCase(ReservationManager reservationManager, SeatManager seatManager) {
        this.reservationManager = reservationManager;
        this.seatManager = seatManager;
    }
    
    /**
     * 좌석 예약 실행
     * 1. 좌석 조회 (비관적 락)
     * 2. 예약 가능 여부 검증
     * 3. 좌석 임시 예약 상태로 변경
     * 4. 예약 정보 저장
     * 
     * @param request 예약 요청
     * @return 예약 응답
     */
    @Transactional
    public ReservationResponse execute(ReserveSeatRequest request) {
        // 1. 좌석 조회 (비관적 락)
        Seat seat = seatManager.getSeatByIdWithLock(request.getSeatId());
        
        // 2. 좌석에 이미 활성 예약이 있는지 확인
        if (reservationManager.hasActiveReservation(seat.getId())) {
            throw new IllegalStateException("이미 예약된 좌석입니다. 좌석 ID: " + seat.getId());
        }
        
        // 3. 좌석 예약 (AVAILABLE → RESERVED)
        Seat reservedSeat = seatManager.reserveSeat(seat);
        
        // 4. 예약 정보 생성 및 저장
        Reservation reservation = Reservation.create(
            request.getUserId(),
            reservedSeat.getId(),
            reservedSeat.getConcertDateId(),
            reservedSeat.getPrice()
        );
        
        Reservation savedReservation = reservationManager.saveReservation(reservation);
        
        return toResponse(savedReservation);
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
