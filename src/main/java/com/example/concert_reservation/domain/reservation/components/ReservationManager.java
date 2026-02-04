package com.example.concert_reservation.domain.reservation.components;

import com.example.concert_reservation.domain.concert.components.SeatManager;
import com.example.concert_reservation.domain.concert.models.Seat;
import com.example.concert_reservation.domain.reservation.models.Reservation;
import com.example.concert_reservation.domain.reservation.repositories.ReservationStoreRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 조회 및 관리 비즈니스 로직
 * 도메인 레이어의 컴포넌트
 */
@Component
public class ReservationManager {
    
    private final ReservationStoreRepository reservationStoreRepository;
    private final SeatManager seatManager;
    
    public ReservationManager(ReservationStoreRepository reservationStoreRepository,
                              SeatManager seatManager) {
        this.reservationStoreRepository = reservationStoreRepository;
        this.seatManager = seatManager;
    }
    
    /**
     * ID로 예약 조회
     * @param id 예약 ID
     * @return 예약
     * @throws IllegalArgumentException 존재하지 않는 예약
     */
    public Reservation getReservationById(Long id) {
        return reservationStoreRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다. ID: " + id));
    }
    
    /**
     * 사용자의 예약 목록 조회
     * @param userId 사용자 ID
     * @return 예약 리스트
     */
    public List<Reservation> getReservationsByUser(String userId) {
        return reservationStoreRepository.findByUserId(userId);
    }
    
    /**
     * 좌석에 활성 예약이 있는지 확인
     * @param seatId 좌석 ID
     * @return 활성 예약이 있으면 true
     */
    public boolean hasActiveReservation(Long seatId) {
        return reservationStoreRepository.findActiveBySeatId(seatId).isPresent();
    }
    
    /**
     * 예약 저장
     * @param reservation 저장할 예약
     * @return 저장된 예약
     */
    public Reservation saveReservation(Reservation reservation) {
        return reservationStoreRepository.save(reservation);
    }
    
    /**
     * 예약 확정 (결제 완료)
     * @param reservation 확정할 예약
     * @return 확정된 예약
     */
    public Reservation confirmReservation(Reservation reservation) {
        reservation.confirm();
        return reservationStoreRepository.save(reservation);
    }
    
    /**
     * 예약 취소
     * @param reservation 취소할 예약
     * @return 취소된 예약
     */
    public Reservation cancelReservation(Reservation reservation) {
        reservation.cancel();
        return reservationStoreRepository.save(reservation);
    }
    
    /**
     * 만료된 예약 처리
     * 스케줄러에서 주기적으로 호출
     * 1. 만료된 예약 조회
     * 2. 예약 상태를 EXPIRED로 변경
     * 3. 좌석 해제 (RESERVED → AVAILABLE)
     * @return 만료 처리된 예약 수
     */
    public int expireReservations() {
        LocalDateTime now = LocalDateTime.now();
        List<Reservation> expiredReservations = reservationStoreRepository.findExpiredReservations(now);
        
        int count = 0;
        for (Reservation reservation : expiredReservations) {
            try {
                // 1. 예약 만료 처리
                reservation.expire();
                reservationStoreRepository.save(reservation);
                
                // 2. 좌석 해제 (RESERVED → AVAILABLE)
                Seat seat = seatManager.getSeatByIdWithLock(reservation.getSeatId());
                seatManager.releaseSeat(seat);
                
                count++;
            } catch (Exception e) {
                // 로깅 후 계속 진행
            }
        }
        
        return count;
    }
}
