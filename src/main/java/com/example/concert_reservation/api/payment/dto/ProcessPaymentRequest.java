package com.example.concert_reservation.api.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 결제 요청 DTO
 */
public record ProcessPaymentRequest(
    @NotNull(message = "예약 ID는 필수입니다")
    Long reservationId,
    
    @NotBlank(message = "사용자 ID는 필수입니다")
    String userId
) {
}
