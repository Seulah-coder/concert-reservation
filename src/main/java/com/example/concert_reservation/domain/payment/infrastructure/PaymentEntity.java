package com.example.concert_reservation.domain.payment.infrastructure;

import com.example.concert_reservation.domain.payment.models.Payment;
import com.example.concert_reservation.domain.payment.models.PaymentStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class PaymentEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private Long reservationId;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime paidAt;
    
    protected PaymentEntity() {
    }
    
    public PaymentEntity(Long reservationId, String userId, BigDecimal amount,
                        PaymentStatus status, LocalDateTime createdAt, LocalDateTime paidAt) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.paidAt = paidAt;
    }
    
    public static PaymentEntity from(Payment payment) {
        PaymentEntity entity = new PaymentEntity(
            payment.getReservationId(),
            payment.getUserId(),
            payment.getAmount(),
            payment.getStatus(),
            payment.getCreatedAt(),
            payment.getPaidAt()
        );
        entity.id = payment.getId();
        return entity;
    }
    
    public Payment toDomain() {
        return Payment.of(id, reservationId, userId, amount, status, createdAt, paidAt);
    }
    
    // Getters
    public Long getId() {
        return id;
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
    
    public PaymentStatus getStatus() {
        return status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}
