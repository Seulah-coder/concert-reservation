package com.example.concert_reservation.api.concert.dto;

import java.math.BigDecimal;

/**
 * 좌석 정보 응답 DTO
 */
public class SeatResponse {
    
    private Long seatId;
    private Integer seatNumber;
    private String status;
    private BigDecimal price;
    
    // 기본 생성자
    public SeatResponse() {
    }
    
    // 전체 필드 생성자
    public SeatResponse(Long seatId, Integer seatNumber, String status, BigDecimal price) {
        this.seatId = seatId;
        this.seatNumber = seatNumber;
        this.status = status;
        this.price = price;
    }
    
    // Getters and Setters
    public Long getSeatId() {
        return seatId;
    }
    
    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }
    
    public Integer getSeatNumber() {
        return seatNumber;
    }
    
    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
