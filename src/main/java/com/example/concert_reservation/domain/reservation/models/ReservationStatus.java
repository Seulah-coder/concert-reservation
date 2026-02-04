package com.example.concert_reservation.domain.reservation.models;

/**
 * 예약 상태를 나타내는 열거형
 * - PENDING: 임시 예약 상태 (5분간 유효)
 * - CONFIRMED: 결제 완료된 예약
 * - CANCELLED: 사용자가 취소한 예약
 * - EXPIRED: 시간 초과로 만료된 예약
 */
public enum ReservationStatus {
    /**
     * 임시 예약 상태 (5분간 유효)
     */
    PENDING,
    
    /**
     * 결제 완료된 예약
     */
    CONFIRMED,
    
    /**
     * 사용자가 취소한 예약
     */
    CANCELLED,
    
    /**
     * 시간 초과로 만료된 예약
     */
    EXPIRED;
    
    /**
     * 임시 예약 상태인지 확인
     * @return 임시 예약 상태이면 true
     */
    public boolean isPending() {
        return this == PENDING;
    }
    
    /**
     * 확정된 예약인지 확인
     * @return 결제 완료 상태이면 true
     */
    public boolean isConfirmed() {
        return this == CONFIRMED;
    }
    
    /**
     * 취소된 예약인지 확인
     * @return 취소 상태이면 true
     */
    public boolean isCancelled() {
        return this == CANCELLED;
    }
    
    /**
     * 만료된 예약인지 확인
     * @return 만료 상태이면 true
     */
    public boolean isExpired() {
        return this == EXPIRED;
    }
    
    /**
     * 활성 상태인지 확인 (PENDING 또는 CONFIRMED)
     * @return 활성 상태이면 true
     */
    public boolean isActive() {
        return this == PENDING || this == CONFIRMED;
    }
}
