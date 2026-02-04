package com.example.concert_reservation.support.exception;

/**
 * 도메인 접근 금지 예외
 */
public class DomainForbiddenException extends RuntimeException {
    public DomainForbiddenException(String message) {
        super(message);
    }
}
