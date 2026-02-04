package com.example.concert_reservation.api.payment.usecase;

import com.example.concert_reservation.api.payment.dto.PaymentResponse;
import com.example.concert_reservation.domain.payment.components.PaymentProcessor;
import com.example.concert_reservation.domain.payment.models.Payment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 처리 UseCase
 * 비즈니스 로직: 예약자 본인 확인, 잔액 차감, 예약 확정
 */
@Service
public class ProcessPaymentUseCase {
    
    private final PaymentProcessor paymentProcessor;
    
    public ProcessPaymentUseCase(PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }
    
    /**
     * 결제 처리
     * @param reservationId 예약 ID
     * @param userId 결제 요청 사용자 ID (예약자)
     * @return 결제 정보
     */
    @Transactional
    public PaymentResponse execute(Long reservationId, String userId) {
        Payment payment = paymentProcessor.processPayment(reservationId, userId);
        return PaymentResponse.from(payment);
    }
}
