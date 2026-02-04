package com.example.concert_reservation.support.exception;

/**
 * 도메인 리소스 미존재 예외
 */
public class DomainNotFoundException extends RuntimeException {
    public DomainNotFoundException(String message) {
        super(message);
    }
}
