package com.example.concert_reservation.domain.refund.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 환불 정보
 * - 결제된 예약에 대한 환불 처리
 * - 결제자 본인만 환불 가능
 * - 환불 금액 및 상태 관리
 */
public class Refund {
    private Long id;
    private Long paymentId;
    private Long reservationId;
    private String userId;
    private BigDecimal amount;
    private String reason;
    private RefundStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Refund(
        Long paymentId,
        Long reservationId,
        String userId,
        BigDecimal amount,
        String reason
    ) {
        this.paymentId = paymentId;
        this.reservationId = reservationId;
        this.userId = userId;
        this.amount = amount;
        this.reason = reason;
        this.status = RefundStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 환불 생성
     */
    public static Refund create(
        Long paymentId,
        Long reservationId,
        String userId,
        BigDecimal amount,
        String reason
    ) {
        return new Refund(paymentId, reservationId, userId, amount, reason);
    }

    /**
     * 데이터베이스에서 조회한 환불 복원
     */
    public static Refund of(
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
        Refund refund = new Refund(paymentId, reservationId, userId, amount, reason);
        refund.id = id;
        refund.status = status;
        refund.createdAt = createdAt;
        refund.updatedAt = updatedAt;
        return refund;
    }

    /**
     * 환불 승인
     */
    public void approve() {
        this.status = RefundStatus.APPROVED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 환불 거부
     */
    public void reject() {
        this.status = RefundStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 환불 실패
     */
    public void fail() {
        this.status = RefundStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
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

    public boolean isPending() {
        return status == RefundStatus.PENDING;
    }

    public boolean isApproved() {
        return status == RefundStatus.APPROVED;
    }
}
