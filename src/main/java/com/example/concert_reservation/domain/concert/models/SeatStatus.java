package com.example.concert_reservation.domain.concert.models;

/**
 * 좌석 상태를 나타내는 열거형
 * - AVAILABLE: 예약 가능
 * - RESERVED: 임시 예약됨 (5분간 유효)
 * - SOLD: 결제 완료 (판매됨)
 */
public enum SeatStatus {
    /**
     * 예약 가능 상태
     */
    AVAILABLE,
    
    /**
     * 임시 예약 상태 (5분 유효)
     */
    RESERVED,
    
    /**
     * 판매 완료 상태
     */
    SOLD;
    
    /**
     * 예약 가능한 상태인지 확인
     * @return 예약 가능하면 true
     */
    public boolean isAvailable() {
        return this == AVAILABLE;
    }
    
    /**
     * 임시 예약 상태인지 확인
     * @return 임시 예약 상태이면 true
     */
    public boolean isReserved() {
        return this == RESERVED;
    }
    
    /**
     * 판매 완료 상태인지 확인
     * @return 판매 완료 상태이면 true
     */
    public boolean isSold() {
        return this == SOLD;
    }
}
