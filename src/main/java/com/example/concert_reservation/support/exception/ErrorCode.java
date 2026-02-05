package com.example.concert_reservation.support.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 * 모든 비즈니스 예외에 대한 표준화된 에러 코드 체계
 */
public enum ErrorCode {
    // 400 Bad Request - 클라이언트 입력 오류
    INVALID_INPUT("E001", "입력값이 올바르지 않습니다", HttpStatus.BAD_REQUEST),
    INVALID_AMOUNT("E002", "유효하지 않은 금액입니다", HttpStatus.BAD_REQUEST),
    
    // 401 Unauthorized - 인증 실패
    TOKEN_MISSING("E101", "인증 토큰이 없습니다", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("E102", "유효하지 않은 토큰입니다", HttpStatus.UNAUTHORIZED),
    
    // 403 Forbidden - 권한 없음
    TOKEN_NOT_ACTIVE("E201", "토큰이 활성 상태가 아닙니다", HttpStatus.FORBIDDEN),
    UNAUTHORIZED_ACCESS("E202", "권한이 없습니다", HttpStatus.FORBIDDEN),
    PAYMENT_UNAUTHORIZED("E203", "결제 권한이 없습니다", HttpStatus.FORBIDDEN),
    REFUND_UNAUTHORIZED("E204", "환불 권한이 없습니다", HttpStatus.FORBIDDEN),
    
    // 404 Not Found - 리소스 없음
    RESOURCE_NOT_FOUND("E301", "리소스를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND("E302", "사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    BALANCE_NOT_FOUND("E303", "잔액 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    CONCERT_NOT_FOUND("E304", "콘서트를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    SEAT_NOT_FOUND("E305", "좌석을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    RESERVATION_NOT_FOUND("E306", "예약을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    PAYMENT_NOT_FOUND("E307", "결제 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    
    // 409 Conflict - 상태 충돌
    ALREADY_RESERVED("E401", "이미 예약된 좌석입니다", HttpStatus.CONFLICT),
    INSUFFICIENT_BALANCE("E402", "잔액이 부족합니다", HttpStatus.CONFLICT),
    PAYMENT_ALREADY_PROCESSED("E403", "이미 처리된 결제입니다", HttpStatus.CONFLICT),
    RESERVATION_ALREADY_PAID("E404", "이미 결제된 예약입니다", HttpStatus.CONFLICT),
    RESERVATION_EXPIRED("E405", "만료된 예약입니다", HttpStatus.CONFLICT),
    SEAT_NOT_AVAILABLE("E406", "예약 가능한 좌석이 아닙니다", HttpStatus.CONFLICT),
    ALREADY_REFUNDED("E407", "이미 환불된 결제입니다", HttpStatus.CONFLICT),
    INVALID_RESERVATION_STATUS("E408", "예약 상태가 올바르지 않습니다", HttpStatus.CONFLICT),
    INVALID_PAYMENT_STATUS("E409", "결제 상태가 올바르지 않습니다", HttpStatus.CONFLICT),
    
    // 500 Internal Server Error - 서버 오류
    INTERNAL_ERROR("E999", "서버 내부 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);
    
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
    
    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
