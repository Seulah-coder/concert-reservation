package com.example.concert_reservation.api.reservation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 예약 응답 DTO
 */
public class ReservationResponse {
    
    private Long reservationId;
    private String userId;
    private Long seatId;
    private Long concertDateId;
    private BigDecimal price;
    private String status;
    private LocalDateTime reservedAt;
    private LocalDateTime expiresAt;
    private Long remainingSeconds;
    
    // 기본 생성자
    public ReservationResponse() {
    }
    
    // 전체 필드 생성자
    public ReservationResponse(Long reservationId, String userId, Long seatId, Long concertDateId,
                               BigDecimal price, String status, LocalDateTime reservedAt,
                               LocalDateTime expiresAt, Long remainingSeconds) {
        this.reservationId = reservationId;
        this.userId = userId;
        this.seatId = seatId;
        this.concertDateId = concertDateId;
        this.price = price;
        this.status = status;
        this.reservedAt = reservedAt;
        this.expiresAt = expiresAt;
        this.remainingSeconds = remainingSeconds;
    }
    
    // Getters and Setters
    public Long getReservationId() {
        return reservationId;
    }
    
    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
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
    
    public Long getRemainingSeconds() {
        return remainingSeconds;
    }
    
    public void setRemainingSeconds(Long remainingSeconds) {
        this.remainingSeconds = remainingSeconds;
    }
}
