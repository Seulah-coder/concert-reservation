package com.example.concert_reservation.domain.payment.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 완료 이벤트
 * 
 * 목적: 핵심 결제 로직과 부가 기능(외부 API 전송) 분리
 * - 결제 트랜잭션 커밋 후 발행
 * - 외부 시스템 장애가 결제 성공에 영향 없음
 */
public record PaymentCompletedEvent(
    Long paymentId,
    Long reservationId,
    String userId,
    BigDecimal amount,
    LocalDateTime paidAt,
    String concertTitle,
    String seatNumber
) {
    
    public static PaymentCompletedEvent of(
        Long paymentId,
        Long reservationId,
        String userId,
        BigDecimal amount,
        LocalDateTime paidAt,
        String concertTitle,
        String seatNumber
    ) {
        return new PaymentCompletedEvent(
            paymentId,
            reservationId,
            userId,
            amount,
            paidAt,
            concertTitle,
            seatNumber
        );
    }
}
