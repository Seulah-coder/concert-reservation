package com.example.concert_reservation.support.exception;

/**
 * 대기열 토큰이 요청에 포함되지 않은 경우 발생하는 예외
 * HTTP 401 Unauthorized로 매핑
 */
public class TokenMissingException extends BaseException {
    
    public TokenMissingException() {
        super(ErrorCode.TOKEN_MISSING, LogLevel.WARN);
    }
    
    public TokenMissingException(String message) {
        super(ErrorCode.TOKEN_MISSING, LogLevel.WARN, message);
    }
}
