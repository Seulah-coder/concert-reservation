package com.example.concert_reservation.domain.refund.infrastructure.entity;

import com.example.concert_reservation.domain.refund.models.RefundStatus;
import com.example.concert_reservation.support.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * 환불 JPA 엔티티
 */
@Entity
@Table(name = "refunds", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_refund_payment_id", columnNames = "payment_id")
    },
    indexes = {
        @Index(name = "idx_refund_payment_id", columnList = "payment_id"),
        @Index(name = "idx_refund_reservation_id", columnList = "reservation_id"),
        @Index(name = "idx_refund_user_id", columnList = "user_id")
    })
public class RefundEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "reservation_id", nullable = false)
    private Long reservationId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RefundStatus status;

    protected RefundEntity() {}

    public RefundEntity(Long paymentId, Long reservationId, String userId, BigDecimal amount, String reason, RefundStatus status) {
        this.paymentId = paymentId;
        this.reservationId = reservationId;
        this.userId = userId;
        this.amount = amount;
        this.reason = reason;
        this.status = status;
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

    public void setStatus(RefundStatus status) {
        this.status = status;
    }
}
