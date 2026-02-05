package com.example.concert_reservation.support.exception;

/**
 * 도메인 리소스 미존재 예외
 * HTTP 404 Not Found로 매핑
 */
public class DomainNotFoundException extends BaseException {
    
    public DomainNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, LogLevel.WARN, message);
    }
    
    public DomainNotFoundException(ErrorCode errorCode) {
        super(errorCode, LogLevel.WARN);
    }
    
    public DomainNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, LogLevel.WARN, message);
    }
}
