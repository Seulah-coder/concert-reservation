package com.example.concert_reservation.support.exception;

/**
 * 유효하지 않은 토큰으로 요청한 경우 발생하는 예외
 * HTTP 401 Unauthorized로 매핑
 */
public class TokenNotFoundException extends BaseException {
    
    public TokenNotFoundException() {
        super(ErrorCode.TOKEN_INVALID, LogLevel.WARN);
    }
    
    public TokenNotFoundException(String tokenValue) {
        super(ErrorCode.TOKEN_INVALID, LogLevel.WARN, "유효하지 않은 토큰입니다: " + tokenValue);
    }
}
