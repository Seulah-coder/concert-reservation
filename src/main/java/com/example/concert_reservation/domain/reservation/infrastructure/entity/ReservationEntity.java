package com.example.concert_reservation.domain.reservation.infrastructure.entity;

import com.example.concert_reservation.support.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 예약 엔티티
 * JPA 엔티티로 infrastructure 레이어에 위치
 */
@Entity
@Table(name = "reservations")
public class ReservationEntity extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private Long seatId;
    
    @Column(nullable = false)
    private Long concertDateId;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    @Column(nullable = false, length = 20)
    private String status; // ReservationStatus enum의 name을 String으로 저장
    
    @Column(nullable = false)
    private LocalDateTime reservedAt;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    // JPA 기본 생성자
    protected ReservationEntity() {
    }
    
    // 생성자
    public ReservationEntity(Long id, String userId, Long seatId, Long concertDateId,
                             BigDecimal price, String status,
                             LocalDateTime reservedAt, LocalDateTime expiresAt) {
        this.id = id;
        this.userId = userId;
        this.seatId = seatId;
        this.concertDateId = concertDateId;
        this.price = price;
        this.status = status;
        this.reservedAt = reservedAt;
        this.expiresAt = expiresAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Long getSeatId() {
        return seatId;
    }
    
    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }
    
    public Long getConcertDateId() {
        return concertDateId;
    }
    
    public void setConcertDateId(Long concertDateId) {
        this.concertDateId = concertDateId;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getReservedAt() {
        return reservedAt;
    }
    
    public void setReservedAt(LocalDateTime reservedAt) {
        this.reservedAt = reservedAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
