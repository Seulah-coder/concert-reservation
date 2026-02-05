package com.example.concert_reservation.support.exception;

/**
 * 로그 레벨 정의
 * - ERROR: 시스템 오류, 즉각 대응 필요
 * - WARN: 잠재적 문제, 모니터링 필요  
 * - INFO: 정상 흐름의 주요 이벤트
 */
public enum LogLevel {
    /**
     * 시스템 장애, 데이터 손실, 복구 불가능한 오류
     * 즉각적인 대응이 필요한 상황
     */
    ERROR,
    
    /**
     * 비정상 흐름이지만 처리 가능한 상황
     * 지속적인 모니터링이 필요함
     */
    WARN,
    
    /**
     * 정상적인 비즈니스 흐름의 주요 이벤트
     * 예: 예약 생성, 결제 완료, 환불 처리 등
     */
    INFO
}
