package com.example.concert_reservation.support.exception;

/**
 * 토큰이 활성(ACTIVE) 상태가 아닌 경우 발생하는 예외
 * WAITING 또는 EXPIRED 상태일 때 발생
 * HTTP 403 Forbidden으로 매핑
 */
public class TokenNotActiveException extends BaseException {
    
    public TokenNotActiveException() {
        super(ErrorCode.TOKEN_NOT_ACTIVE, LogLevel.WARN);
    }
    
    public TokenNotActiveException(String message, String currentStatus) {
        super(ErrorCode.TOKEN_NOT_ACTIVE, LogLevel.WARN, message, currentStatus);
    }
}
