package com.example.concert_reservation.api.balance.dto;

import com.example.concert_reservation.domain.balance.models.Balance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BalanceResponse(
    Long id,
    String userId,
    BigDecimal amount,
    LocalDateTime createdAt,
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
