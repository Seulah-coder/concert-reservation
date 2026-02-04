package com.example.concert_reservation.api.reservation.dto;

/**
 * 좌석 예약 요청 DTO
 */
public class ReserveSeatRequest {
    
    private String userId;
    private Long seatId;
    
    // 기본 생성자
    public ReserveSeatRequest() {
    }
    
    // 전체 필드 생성자
    public ReserveSeatRequest(String userId, Long seatId) {
        this.userId = userId;
        this.seatId = seatId;
    }
    
    // Getters and Setters
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
}
