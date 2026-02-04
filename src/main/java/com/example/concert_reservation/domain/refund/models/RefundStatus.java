package com.example.concert_reservation.domain.refund.models;

/**
 * 환불 상태
 */
public enum RefundStatus {
    PENDING("대기중"),
    APPROVED("승인됨"),
    REJECTED("거부됨"),
    FAILED("실패");

    private final String description;

    RefundStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
