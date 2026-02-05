package com.example.concert_reservation.support.exception;

/**
 * 도메인 상태 충돌 예외
 * HTTP 409 Conflict로 매핑
 */
public class DomainConflictException extends BaseException {
    
    public DomainConflictException(String message) {
        super(ErrorCode.INVALID_RESERVATION_STATUS, LogLevel.WARN, message);
    }
    
    public DomainConflictException(ErrorCode errorCode) {
        super(errorCode, LogLevel.WARN);
    }
    
    public DomainConflictException(ErrorCode errorCode, String message) {
        super(errorCode, LogLevel.WARN, message);
    }
    
    public DomainConflictException(ErrorCode errorCode, Object data) {
        super(errorCode, LogLevel.WARN, data);
    }
}
