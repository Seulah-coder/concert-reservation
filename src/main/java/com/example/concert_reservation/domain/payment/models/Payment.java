package com.example.concert_reservation.domain.payment.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 도메인 모델
 * 예약된 좌석에 대한 결제 정보 관리
 */
public class Payment {
    
    private final Long id;
    private final Long reservationId;
    private final String userId;
    private final BigDecimal amount;
    private final PaymentStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime paidAt;
    
    private Payment(Long id, Long reservationId, String userId, BigDecimal amount,
                    PaymentStatus status, LocalDateTime createdAt, LocalDateTime paidAt) {
        this.id = id;
        this.reservationId = reservationId;
        this.userId = userId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.paidAt = paidAt;
    }
    
    /**
     * 새로운 결제 생성
     */
    public static Payment create(Long reservationId, String userId, BigDecimal amount) {
        if (reservationId == null) {
            throw new IllegalArgumentException("예약 ID는 필수입니다");
        }
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다");
        }
        
        LocalDateTime now = LocalDateTime.now();
        return new Payment(null, reservationId, userId, amount, 
                          PaymentStatus.COMPLETED, now, now);
    }
    
    /**
     * 기존 결제 재구성
     */
    public static Payment of(Long id, Long reservationId, String userId, BigDecimal amount,
                            PaymentStatus status, LocalDateTime createdAt, LocalDateTime paidAt) {
        return new Payment(id, reservationId, userId, amount, status, createdAt, paidAt);
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
