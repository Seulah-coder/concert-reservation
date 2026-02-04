package com.example.concert_reservation.domain.queue.models;

import java.util.Objects;
import java.util.UUID;

/**
 * 대기열 토큰을 나타내는 값 객체 (Value Object)
 * UUID 기반으로 고유한 토큰을 생성하여 사용자를 식별
 */
public class QueueToken {
    
    private final String value;
    
    private QueueToken(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("토큰 값은 비어있을 수 없습니다");
        }
        this.value = value;
    }
    
    /**
     * 새로운 토큰 생성 (UUID 기반)
     * @return 새로 생성된 QueueToken
     */
    public static QueueToken generate() {
        return new QueueToken(UUID.randomUUID().toString());
    }
    
    /**
     * 기존 토큰 값으로 QueueToken 생성
     * @param value 토큰 문자열
     * @return QueueToken 인스턴스
     */
    public static QueueToken of(String value) {
        return new QueueToken(value);
    }
    
    /**
     * 토큰 값 반환
     * @return 토큰 문자열
     */
    public String getValue() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QueueToken that = (QueueToken) o;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
