package com.example.concert_reservation.api.refund.dto;

import com.example.concert_reservation.domain.refund.models.Refund;
import com.example.concert_reservation.domain.refund.models.RefundStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 환불 응답 DTO
 */
public class RefundResponse {
    private Long id;
    private Long paymentId;
    private Long reservationId;
    private String userId;
    private BigDecimal amount;
    private String reason;
    private RefundStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RefundResponse() {}

    public RefundResponse(
        Long id,
        Long paymentId,
        Long reservationId,
        String userId,
        BigDecimal amount,
        String reason,
        RefundStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
        this.id = id;
        this.paymentId = paymentId;
        this.reservationId = reservationId;
        this.userId = userId;
        this.amount = amount;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Refund 모델을 DTO로 변환
     */
    public static RefundResponse from(Refund refund) {
        return new RefundResponse(
            refund.getId(),
            refund.getPaymentId(),
            refund.getReservationId(),
            refund.getUserId(),
            refund.getAmount(),
            refund.getReason(),
            refund.getStatus(),
            refund.getCreatedAt(),
            refund.getUpdatedAt()
        );
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public String getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public RefundStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
