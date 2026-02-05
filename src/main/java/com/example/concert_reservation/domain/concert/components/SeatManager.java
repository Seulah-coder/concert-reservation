package com.example.concert_reservation.domain.concert.components;

import com.example.concert_reservation.domain.concert.models.Seat;
import com.example.concert_reservation.domain.concert.repositories.SeatStoreRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 좌석 조회 및 관리 비즈니스 로직
 * 도메인 레이어의 컴포넌트
 */
@Component
public class SeatManager {
    
    private final SeatStoreRepository seatStoreRepository;
    
    public SeatManager(SeatStoreRepository seatStoreRepository) {
        this.seatStoreRepository = seatStoreRepository;
    }
    
    /**
     * 특정 콘서트 날짜의 모든 좌석 조회
     * @param concertDateId 콘서트 날짜 ID
     * @return 좌석 리스트
     */
    public List<Seat> getSeatsByConcert(Long concertDateId) {
        return seatStoreRepository.findByConcertDateId(concertDateId);
    }
    
    /**
     * ID로 좌석 조회 (비관적 락)
     * @param id 좌석 ID
     * @return 좌석
     * @throws IllegalArgumentException 존재하지 않는 좌석
     */
    public Seat getSeatByIdWithLock(Long id) {
        return seatStoreRepository.findByIdWithLock(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다. ID: " + id));
    }
    
    /**
     * 좌석 예약 가능 여부 검증
     * @param seat 좌석
     * @throws IllegalStateException 예약 불가능한 좌석
     */
    public void validateAvailableForReservation(Seat seat) {
        if (!seat.isAvailable()) {
            throw new IllegalStateException(
                "예약 가능한 좌석이 아닙니다. 좌석번호: " + seat.getSeatNumber() + 
                ", 현재상태: " + seat.getStatus()
            );
        }
    }
    
    /**
     * 좌석 임시 예약
     * @param seat 예약할 좌석
     * @return 예약된 좌석
     */
    public Seat reserveSeat(Seat seat) {
        validateAvailableForReservation(seat);
        seat.reserve();
        return seatStoreRepository.save(seat);
    }
    
    /**
     * 좌석 판매 (결제 완료)
     * @param seat 판매할 좌석
     * @return 판매된 좌석
     */
    public Seat sellSeat(Seat seat) {
        if (!seat.isReserved()) {
            throw new IllegalStateException(
                "임시 예약된 좌석만 판매할 수 있습니다. 좌석번호: " + seat.getSeatNumber()
            );
        }
        seat.sell();
        return seatStoreRepository.save(seat);
    }
    
    /**
     * 좌석 해제 (예약 취소)
     * @param seat 해제할 좌석
     * @return 해제된 좌석
     */
    public Seat releaseSeat(Seat seat) {
        if (!seat.isReserved()) {
            throw new IllegalStateException(
                "임시 예약된 좌석만 해제할 수 있습니다. 좌석번호: " + seat.getSeatNumber()
            );
        }
        seat.release();
        return seatStoreRepository.save(seat);
    }
}
