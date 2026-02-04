package com.example.concert_reservation.domain.balance.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 잔액 도메인 모델 (순수 자바 - JPA 의존 없음)
 * 사용자의 잔액 관리 및 충전/사용/환불 로직
 */
public class Balance {
    
    private final Long id;
    private final String userId;
    private BigDecimal amount;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final Long version;
    
    // 생성자 (불변 필드만)
    private Balance(Long id, String userId, BigDecimal amount, 
                    LocalDateTime createdAt, LocalDateTime updatedAt, Long version) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }
    
    /**
     * 새로운 잔액 생성 (Static Factory Method)
     * @param userId 사용자 ID
     * @return 생성된 Balance (초기 잔액 0원)
     */
    public static Balance create(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
        
        LocalDateTime now = LocalDateTime.now();
        return new Balance(null, userId, BigDecimal.ZERO, now, now, null);
    }
    
    /**
     * 기존 잔액 재구성 (Repository에서 조회 시)
     * @param id 잔액 ID
     * @param userId 사용자 ID
     * @param amount 잔액
     * @param createdAt 생성 시간
     * @param updatedAt 수정 시간
     * @param version 버전 (낙관적 잠금용)
     * @return 재구성된 Balance
     */
    public static Balance of(Long id, String userId, BigDecimal amount,
                             LocalDateTime createdAt, LocalDateTime updatedAt, Long version) {
        return new Balance(id, userId, amount, createdAt, updatedAt, version);
    }
    
    /**
     * 잔액 충전
     * @param chargeAmount 충전 금액
     * @throws IllegalArgumentException 충전 금액이 0보다 작거나 같은 경우
     */
    public void charge(BigDecimal chargeAmount) {
        if (chargeAmount == null || chargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다");
        }
        
        this.amount = this.amount.add(chargeAmount);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 잔액 사용
     * @param useAmount 사용 금액
     * @throws IllegalArgumentException 사용 금액이 0보다 작거나 같은 경우
     * @throws IllegalStateException 잔액이 부족한 경우
     */
    public void use(BigDecimal useAmount) {
        if (useAmount == null || useAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다");
        }
        
        if (this.amount.compareTo(useAmount) < 0) {
            throw new IllegalStateException(
                String.format("잔액이 부족합니다. 현재 잔액: %s, 사용 금액: %s", 
                    this.amount, useAmount)
            );
        }
        
        this.amount = this.amount.subtract(useAmount);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 잔액 환불
     * @param refundAmount 환불 금액
     * @throws IllegalArgumentException 환불 금액이 0보다 작거나 같은 경우
     */
    public void refund(BigDecimal refundAmount) {
        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다");
        }
        
        this.amount = this.amount.add(refundAmount);
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * 잔액이 충분한지 확인
     * @param requiredAmount 필요한 금액
     * @return 잔액이 충분하면 true
     */
    public boolean hasSufficientBalance(BigDecimal requiredAmount) {
        if (requiredAmount == null) {
            throw new IllegalArgumentException("필요 금액은 필수입니다");
        }
        return this.amount.compareTo(requiredAmount) >= 0;
    }
    
    // Getters
    public Long getId() {
        return id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public Long getVersion() {
        return version;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Balance balance = (Balance) o;
        return Objects.equals(id, balance.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
