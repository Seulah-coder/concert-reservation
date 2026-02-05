package com.example.concert_reservation.domain.payment.components;

import com.example.concert_reservation.domain.balance.components.BalanceManager;
import com.example.concert_reservation.domain.payment.models.Payment;
import com.example.concert_reservation.domain.payment.repositories.PaymentRepository;
import com.example.concert_reservation.domain.reservation.components.ReservationManager;
import com.example.concert_reservation.domain.reservation.models.Reservation;
import com.example.concert_reservation.domain.reservation.models.ReservationStatus;
import com.example.concert_reservation.support.exception.DomainConflictException;
import com.example.concert_reservation.support.exception.DomainForbiddenException;
import com.example.concert_reservation.support.exception.DomainNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 결제 처리 컴포넌트
 * - 예약자만 결제 가능하도록 검증
 * - PENDING 상태의 예약만 결제 가능
 * - 잔액 차감 및 예약 확정 처리
 */
@Component
public class PaymentProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(PaymentProcessor.class);
    
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
     * @throws DomainNotFoundException 예약이 존재하지 않는 경우
     * @throws DomainForbiddenException 예약자 본인이 아닌 경우
     * @throws DomainConflictException 예약 상태 불일치/중복 결제/잔액 부족
     */
    public Payment processPayment(Long reservationId, String userId) {
        log.info("결제 처리 시작 - reservationId: {}, userId: {}", reservationId, userId);
        
        // 1. 예약 조회
        Reservation reservation = reservationManager.getReservation(reservationId)
            .orElseThrow(() -> new DomainNotFoundException("예약을 찾을 수 없습니다: " + reservationId));
        
        // 2. 예약자 본인 확인
        if (!reservation.getUserId().equals(userId)) {
            log.warn("결제 권한 없음 - reservationId: {}, requestUserId: {}, reservationUserId: {}", 
                reservationId, userId, reservation.getUserId());
            throw new DomainForbiddenException("본인의 예약만 결제할 수 있습니다");
        }
        
        // 3. 예약 상태 확인 (PENDING만 결제 가능)
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            log.warn("예약 상태 불일치 - reservationId: {}, status: {}", reservationId, reservation.getStatus());
            throw new DomainConflictException("예약 상태가 올바르지 않습니다. 현재 상태: " + reservation.getStatus());
        }
        
        // 4. 중복 결제 방지
        paymentRepository.findByReservationId(reservationId).ifPresent(payment -> {
            log.warn("중복 결제 시도 - reservationId: {}, existingPaymentId: {}", reservationId, payment.getId());
            throw new DomainConflictException("이미 결제된 예약입니다");
        });
        
        // 5. 잔액 차감
        try {
            balanceManager.useBalance(userId, reservation.getPrice());
        } catch (IllegalStateException ex) {
            log.warn("잔액 부족 - userId: {}, requiredAmount: {}", userId, reservation.getPrice());
            throw new DomainConflictException(ex.getMessage());
        }
        
        // 6. 예약 확정
        reservationManager.confirmReservation(reservationId);
        
        // 7. 결제 정보 저장
        Payment payment = Payment.create(reservationId, userId, reservation.getPrice());
        Payment saved = paymentRepository.save(payment);
        
        log.info("결제 처리 완료 - paymentId: {}, reservationId: {}, userId: {}, amount: {}", 
            saved.getId(), reservationId, userId, reservation.getPrice());
        
        return saved;
    }
}
