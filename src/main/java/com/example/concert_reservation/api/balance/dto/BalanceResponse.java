package com.example.concert_reservation.api.balance.dto;

import com.example.concert_reservation.domain.balance.models.Balance;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "잔액 정보 응답")
public record BalanceResponse(
    @Schema(description = "잔액 ID", example = "1")
    Long id,
    
    @Schema(description = "사용자 ID", example = "user123")
    String userId,
    
    @Schema(description = "현재 잔액 (KRW)", example = "100000")
    BigDecimal amount,
    
    @Schema(description = "생성 시각", example = "2026-02-04T12:00:00")
    LocalDateTime createdAt,
    
    @Schema(description = "마지막 업데이트 시각", example = "2026-02-04T13:30:00")
    LocalDateTime updatedAt
) {
    public static BalanceResponse from(Balance balance) {
        return new BalanceResponse(
            balance.getId(),
            balance.getUserId(),
            balance.getAmount(),
            balance.getCreatedAt(),
            balance.getUpdatedAt()
        );
    }
}
