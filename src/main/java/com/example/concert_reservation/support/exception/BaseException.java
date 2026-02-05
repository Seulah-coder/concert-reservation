package com.example.concert_reservation.support.exception;

/**
 * 모든 커스텀 예외의 기본 클래스
 * ErrorCode, LogLevel, 추가 데이터를 포함하여 일관된 예외 처리 제공
 */
public abstract class BaseException extends RuntimeException {
    private final ErrorCode errorCode;
    private final LogLevel logLevel;
    private final Object data;
    
    protected BaseException(ErrorCode errorCode, LogLevel logLevel) {
        this(errorCode, logLevel, null);
    }
    
    protected BaseException(ErrorCode errorCode, LogLevel logLevel, Object data) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.logLevel = logLevel;
        this.data = data;
    }
    
    protected BaseException(ErrorCode errorCode, LogLevel logLevel, String customMessage) {
        this(errorCode, logLevel, customMessage, null);
    }
    
    protected BaseException(ErrorCode errorCode, LogLevel logLevel, String customMessage, Object data) {
        super(customMessage);
        this.errorCode = errorCode;
        this.logLevel = logLevel;
        this.data = data;
    }
    
    public ErrorCode getErrorCode() {
        return errorCode;
    }
    
    public LogLevel getLogLevel() {
        return logLevel;
    }
    
    public Object getData() {
        return data;
    }
}
