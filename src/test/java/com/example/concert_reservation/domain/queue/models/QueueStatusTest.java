package com.example.concert_reservation.domain.queue.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * QueueStatus 단위 테스트
 */
@DisplayName("QueueStatus 테스트")
class QueueStatusTest {
    
    @Test
    @DisplayName("ACTIVE 상태는 isActive()가 true를 반환한다")
    void isActive_whenActive_returnsTrue() {
        // given
        QueueStatus status = QueueStatus.ACTIVE;
        
        // when
        boolean result = status.isActive();
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("WAITING 상태는 isActive()가 false를 반환한다")
    void isActive_whenWaiting_returnsFalse() {
        // given
        QueueStatus status = QueueStatus.WAITING;
        
        // when
        boolean result = status.isActive();
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("WAITING 상태는 isWaiting()이 true를 반환한다")
    void isWaiting_whenWaiting_returnsTrue() {
        // given
        QueueStatus status = QueueStatus.WAITING;
        
        // when
        boolean result = status.isWaiting();
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("EXPIRED 상태는 isExpired()가 true를 반환한다")
    void isExpired_whenExpired_returnsTrue() {
        // given
        QueueStatus status = QueueStatus.EXPIRED;
        
        // when
        boolean result = status.isExpired();
        
        // then
        assertThat(result).isTrue();
    }
}
