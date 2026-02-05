package com.example.concert_reservation.domain.refund.components;

import com.example.concert_reservation.domain.balance.components.BalanceManager;
import com.example.concert_reservation.domain.payment.models.Payment;
import com.example.concert_reservation.domain.payment.repositories.PaymentRepository;
import com.example.concert_reservation.domain.refund.models.Refund;
import com.example.concert_reservation.domain.refund.repositories.RefundRepository;
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
 * 환불 처리 컴포넌트
 * - 결제자만 환불 가능하도록 검증
 * - 이미 환불된 예약은 환불 불가
 * - 잔액 복구 및 예약 상태 변경 처리
 */
@Component
public class RefundProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(RefundProcessor.class);

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final ReservationManager reservationManager;
    private final BalanceManager balanceManager;

    public RefundProcessor(
        RefundRepository refundRepository,
        PaymentRepository paymentRepository,
        ReservationManager reservationManager,
        BalanceManager balanceManager
    ) {
        this.refundRepository = refundRepository;
        this.paymentRepository = paymentRepository;
        this.reservationManager = reservationManager;
        this.balanceManager = balanceManager;
    }

    /**
     * 결제에 대한 환불 처리
     *
     * @param paymentId 결제 ID
     * @param userId 환불 요청 사용자 ID
     * @param reason 환불 사유
     * @return 생성된 환불 정보
     * @throws DomainNotFoundException 결제가 존재하지 않는 경우
     * @throws DomainForbiddenException 결제자 본인이 아닌 경우
     * @throws DomainConflictException 이미 환불됨/예약 상태 불일치
     */
    public Refund processRefund(Long paymentId, String userId, String reason) {
        log.info("환불 처리 시작 - paymentId: {}, userId: {}, reason: {}", paymentId, userId, reason);
        
        // 1. 결제 정보 조회
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new DomainNotFoundException("결제를 찾을 수 없습니다: " + paymentId));

        // 2. 결제자 본인 확인
        if (!payment.getUserId().equals(userId)) {
            log.warn("환불 권한 없음 - paymentId: {}, requestUserId: {}, paymentUserId: {}", 
                paymentId, userId, payment.getUserId());
            throw new DomainForbiddenException("본인의 결제만 환불할 수 있습니다");
        }

        // 3. 이미 환불된 예약 확인
        refundRepository.findByPaymentId(paymentId).ifPresent(refund -> {
            log.warn("중복 환불 시도 - paymentId: {}, existingRefundId: {}", paymentId, refund.getId());
            throw new DomainConflictException("이미 환불된 결제입니다");
        });

        // 4. 예약 정보 조회
        Reservation reservation = reservationManager.getReservation(payment.getReservationId())
            .orElseThrow(() -> new DomainNotFoundException("예약을 찾을 수 없습니다: " + payment.getReservationId()));

        // 5. 예약 상태 확인 (CONFIRMED만 환불 가능)
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            log.warn("예약 상태 불일치 - reservationId: {}, status: {}", 
                payment.getReservationId(), reservation.getStatus());
            throw new DomainConflictException("확정된 예약만 환불할 수 있습니다. 현재 상태: " + reservation.getStatus());
        }

        // 6. 잔액 복구
        try {
            balanceManager.chargeBalance(userId, payment.getAmount());
        } catch (IllegalStateException ex) {
            log.error("잔액 복구 실패 - userId: {}, amount: {}", userId, payment.getAmount(), ex);
            throw new DomainConflictException("잔액 복구에 실패했습니다: " + ex.getMessage());
        }

        // 7. 예약 상태 변경 (CANCELLED)
        reservationManager.cancelReservation(reservation);

        // 8. 환불 정보 저장
        Refund refund = Refund.create(
            paymentId,
            payment.getReservationId(),
            userId,
            payment.getAmount(),
            reason
        );
        refund.approve();
        Refund saved = refundRepository.save(refund);
        
        log.info("환불 처리 완료 - refundId: {}, paymentId: {}, userId: {}, amount: {}", 
            saved.getId(), paymentId, userId, payment.getAmount());
        
        return saved;
    }
}
