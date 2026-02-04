package com.example.concert_reservation.domain.queue.models;

/**
 * 대기열 상태를 나타내는 열거형
 * - WAITING: 대기 중 (활성화 대기)
 * - ACTIVE: 활성 상태 (서비스 이용 가능)
 * - EXPIRED: 만료됨 (더 이상 사용 불가)
 */
public enum QueueStatus {
    /**
     * 대기 중 상태: 사용자가 대기열에 등록되었지만 아직 서비스 이용 불가
     */
    WAITING,
    
    /**
     * 활성 상태: 사용자가 서비스를 이용할 수 있는 상태
     */
    ACTIVE,
    
    /**
     * 만료 상태: 토큰이 만료되어 더 이상 사용할 수 없는 상태
     */
    EXPIRED;
    
    /**
     * 토큰이 활성 상태인지 확인
     * @return 활성 상태이면 true
     */
    public boolean isActive() {
        return this == ACTIVE;
    }
    
    /**
     * 토큰이 대기 중인지 확인
     * @return 대기 중이면 true
     */
    public boolean isWaiting() {
        return this == WAITING;
    }
    
    /**
     * 토큰이 만료되었는지 확인
     * @return 만료되었으면 true
     */
    public boolean isExpired() {
        return this == EXPIRED;
    }
}
