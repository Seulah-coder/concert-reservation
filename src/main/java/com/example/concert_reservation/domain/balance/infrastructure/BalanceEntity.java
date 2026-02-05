package com.example.concert_reservation.domain.balance.infrastructure;

import com.example.concert_reservation.domain.balance.models.Balance;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "balance")
public class BalanceEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String userId;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    protected BalanceEntity() {
    }
    
    public BalanceEntity(String userId, BigDecimal amount, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.amount = amount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    public static BalanceEntity from(Balance balance) {
        BalanceEntity entity = new BalanceEntity(
            balance.getUserId(),
            balance.getAmount(),
            balance.getCreatedAt(),
            balance.getUpdatedAt()
        );
        entity.id = balance.getId();
        return entity;
    }
    
    public Balance toDomain() {
        return Balance.of(id, userId, amount, createdAt, updatedAt);
    }
    
    public void updateFrom(Balance balance) {
        this.amount = balance.getAmount();
        this.updatedAt = balance.getUpdatedAt();
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
}
