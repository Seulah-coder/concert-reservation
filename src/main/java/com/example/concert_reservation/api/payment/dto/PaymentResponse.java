package com.example.concert_reservation.api.payment.dto;

import com.example.concert_reservation.domain.payment.models.Payment;
import com.example.concert_reservation.domain.payment.models.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 응답 DTO
 */
public record PaymentResponse(
    Long id,
    Long reservationId,
    String userId,
    BigDecimal amount,
    PaymentStatus status,
    LocalDateTime createdAt
) {
    
    /**
     * Payment 도메인 모델을 DTO로 변환
     */
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getReservationId(),
            payment.getUserId(),
            payment.getAmount(),
            payment.getStatus(),
            payment.getCreatedAt()
        );
    }
}
