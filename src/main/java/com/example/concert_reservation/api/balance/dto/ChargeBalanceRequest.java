package com.example.concert_reservation.api.balance.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "잔액 충전 요청")
public record ChargeBalanceRequest(
    @Schema(description = "사용자 ID", example = "user123", required = true)
    String userId,
    
    @Schema(description = "충전 금액 (KRW)", example = "50000", required = true)
    BigDecimal amount
) {
    public ChargeBalanceRequest {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다");
        }
    }
}
