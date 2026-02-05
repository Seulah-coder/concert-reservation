package com.example.concert_reservation.support.exception;

/**
 * 도메인 접근 금지 예외
 * HTTP 403 Forbidden으로 매핑
 */
public class DomainForbiddenException extends BaseException {
    
    public DomainForbiddenException(String message) {
        super(ErrorCode.UNAUTHORIZED_ACCESS, LogLevel.WARN, message);
    }
    
    public DomainForbiddenException(ErrorCode errorCode) {
        super(errorCode, LogLevel.WARN);
    }
    
    public DomainForbiddenException(ErrorCode errorCode, String message) {
        super(errorCode, LogLevel.WARN, message);
    }
}
