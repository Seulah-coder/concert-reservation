package com.example.concert_reservation.domain.payment.components;

import com.example.concert_reservation.domain.balance.components.BalanceManager;
import com.example.concert_reservation.domain.payment.models.Payment;
import com.example.concert_reservation.domain.payment.repositories.PaymentRepository;
import com.example.concert_reservation.domain.reservation.components.ReservationManager;
import com.example.concert_reservation.domain.reservation.models.Reservation;
import com.example.concert_reservation.domain.reservation.models.ReservationStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 결제 처리 컴포넌트
 * - 예약자만 결제 가능하도록 검증
 * - RESERVED 상태의 예약만 결제 가능
 * - 잔액 차감 및 예약 확정 처리
 */
@Component
public class PaymentProcessor {
    
    private final PaymentRepository paymentRepository;
    private final ReservationManager reservationManager;
    private final BalanceManager balanceManager;
    
    public PaymentProcessor(
        PaymentRepository paymentRepository,
        ReservationManager reservationManager,
        BalanceManager balanceManager
    ) {
        this.paymentRepository = paymentRepository;
        this.reservationManager = reservationManager;
        this.balanceManager = balanceManager;
    }
    
    /**
     * 예약에 대한 결제 처리
     * 
     * @param reservationId 예약 ID
     * @param userId 결제 요청 사용자 ID
     * @return 생성된 결제 정보
     * @throws IllegalArgumentException 예약이 존재하지 않거나 결제 조건을 만족하지 않는 경우
     * @throws IllegalStateException 잔액이 부족한 경우
     */
    public Payment processPayment(Long reservationId, String userId) {
        // 1. 예약 조회
        Reservation reservation = reservationManager.getReservation(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("예약을 찾을 수 없습니다: " + reservationId));
        
        // 2. 예약자 본인 확인
        if (!reservation.getUserId().equals(userId)) {
            throw new IllegalArgumentException("본인의 예약만 결제할 수 있습니다");
        }
        
        // 3. 예약 상태 확인 (PENDING만 결제 가능)
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("예약 상태가 올바르지 않습니다. 현재 상태: " + reservation.getStatus());
        }
        
        // 4. 중복 결제 방지
        paymentRepository.findByReservationId(reservationId).ifPresent(payment -> {
            throw new IllegalArgumentException("이미 결제된 예약입니다");
        });
        
        // 5. 잔액 차감
        BigDecimal amount = reservation.getPrice();
        balanceManager.useBalance(userId, amount);
        
        // 6. 예약 확정
        reservationManager.confirmReservation(reservationId);
        
        // 7. 결제 정보 저장
        Payment payment = Payment.create(reservationId, userId, amount);
        return paymentRepository.save(payment);
    }
}
