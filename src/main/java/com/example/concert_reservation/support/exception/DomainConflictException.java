package com.example.concert_reservation.support.exception;

/**
 * 도메인 상태 충돌 예외
 */
public class DomainConflictException extends RuntimeException {
    public DomainConflictException(String message) {
        super(message);
    }
}
