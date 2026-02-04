package com.example.concert_reservation.domain.queue.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * UserQueue 단위 테스트
 */
@DisplayName("UserQueue 도메인 모델 테스트")
class UserQueueTest {
    
    @Test
    @DisplayName("create()로 새로운 대기열을 생성할 수 있다")
    void create_validInput_createsUserQueue() {
        // given
        String userId = "user123";
        Long queueNumber = 10L;
        
        // when
        UserQueue queue = UserQueue.create(userId, queueNumber);
        
        // then
        assertThat(queue).isNotNull();
        assertThat(queue.getUserId()).isEqualTo(userId);
        assertThat(queue.getQueueNumber()).isEqualTo(queueNumber);
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.WAITING);
        assertThat(queue.getToken()).isNotNull();
        assertThat(queue.getEnteredAt()).isNotNull();
        assertThat(queue.getExpiredAt()).isNull();
    }
    
    @Test
    @DisplayName("null userId로 생성 시 예외가 발생한다")
    void create_nullUserId_throwsException() {
        // when & then
        assertThatThrownBy(() -> UserQueue.create(null, 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("사용자 ID는 필수입니다");
    }
    
    @Test
    @DisplayName("빈 userId로 생성 시 예외가 발생한다")
    void create_emptyUserId_throwsException() {
        // when & then
        assertThatThrownBy(() -> UserQueue.create("", 1L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("사용자 ID는 필수입니다");
    }
    
    @Test
    @DisplayName("null queueNumber로 생성 시 예외가 발생한다")
    void create_nullQueueNumber_throwsException() {
        // when & then
        assertThatThrownBy(() -> UserQueue.create("user123", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("대기 번호는 1 이상이어야 합니다");
    }
    
    @Test
    @DisplayName("0 이하의 queueNumber로 생성 시 예외가 발생한다")
    void create_invalidQueueNumber_throwsException() {
        // when & then
        assertThatThrownBy(() -> UserQueue.create("user123", 0L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("대기 번호는 1 이상이어야 합니다");
    }
    
    @Test
    @DisplayName("WAITING 상태의 대기열을 활성화할 수 있다")
    void activate_waitingQueue_changesStatusToActive() {
        // given
        UserQueue queue = UserQueue.create("user123", 1L);
        int validMinutes = 30;
        
        // when
        queue.activate(validMinutes);
        
        // then
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.ACTIVE);
        assertThat(queue.getExpiredAt()).isNotNull();
        assertThat(queue.getExpiredAt()).isAfter(LocalDateTime.now());
    }
    
    @Test
    @DisplayName("WAITING이 아닌 상태의 대기열을 활성화하면 예외가 발생한다")
    void activate_nonWaitingQueue_throwsException() {
        // given
        UserQueue queue = UserQueue.create("user123", 1L);
        queue.activate(30);
        
        // when & then
        assertThatThrownBy(() -> queue.activate(30))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("WAITING 상태만 활성화할 수 있습니다");
    }
    
    @Test
    @DisplayName("0 이하의 validMinutes로 활성화 시 예외가 발생한다")
    void activate_invalidMinutes_throwsException() {
        // given
        UserQueue queue = UserQueue.create("user123", 1L);
        
        // when & then
        assertThatThrownBy(() -> queue.activate(0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("유효 시간은 0보다 커야 합니다");
    }
    
    @Test
    @DisplayName("expire()로 대기열을 만료시킬 수 있다")
    void expire_changesStatusToExpired() {
        // given
        UserQueue queue = UserQueue.create("user123", 1L);
        
        // when
        queue.expire();
        
        // then
        assertThat(queue.getStatus()).isEqualTo(QueueStatus.EXPIRED);
        assertThat(queue.getExpiredAt()).isNotNull();
    }
    
    @Test
    @DisplayName("ACTIVE 상태의 대기열은 isActive()가 true를 반환한다")
    void isActive_activeQueue_returnsTrue() {
        // given
        UserQueue queue = UserQueue.create("user123", 1L);
        queue.activate(30);
        
        // when
        boolean result = queue.isActive();
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("WAITING 상태의 대기열은 isActive()가 false를 반환한다")
    void isActive_waitingQueue_returnsFalse() {
        // given
        UserQueue queue = UserQueue.create("user123", 1L);
        
        // when
        boolean result = queue.isActive();
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("만료 시간이 지난 대기열은 isActive()가 false를 반환한다")
    void isActive_expiredQueue_returnsFalse() {
        // given
        QueueToken token = QueueToken.generate();
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(1);
        UserQueue queue = UserQueue.of(
            1L, token, "user123", 1L, QueueStatus.ACTIVE, 
            LocalDateTime.now().minusHours(1), pastTime
        );
        
        // when
        boolean result = queue.isActive();
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("WAITING 상태의 대기열은 isWaiting()이 true를 반환한다")
    void isWaiting_waitingQueue_returnsTrue() {
        // given
        UserQueue queue = UserQueue.create("user123", 1L);
        
        // when
        boolean result = queue.isWaiting();
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("EXPIRED 상태의 대기열은 isExpired()가 true를 반환한다")
    void isExpired_expiredQueue_returnsTrue() {
        // given
        UserQueue queue = UserQueue.create("user123", 1L);
        queue.expire();
        
        // when
        boolean result = queue.isExpired();
        
        // then
        assertThat(result).isTrue();
    }
    
    @Test
    @DisplayName("of()로 기존 데이터로부터 UserQueue를 재구성할 수 있다")
    void of_reconstructsUserQueue() {
        // given
        Long id = 1L;
        QueueToken token = QueueToken.generate();
        String userId = "user123";
        Long queueNumber = 5L;
        QueueStatus status = QueueStatus.ACTIVE;
        LocalDateTime enteredAt = LocalDateTime.now();
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(30);
        
        // when
        UserQueue queue = UserQueue.of(id, token, userId, queueNumber, status, enteredAt, expiredAt);
        
        // then
        assertThat(queue.getId()).isEqualTo(id);
        assertThat(queue.getToken()).isEqualTo(token);
        assertThat(queue.getUserId()).isEqualTo(userId);
        assertThat(queue.getQueueNumber()).isEqualTo(queueNumber);
        assertThat(queue.getStatus()).isEqualTo(status);
        assertThat(queue.getEnteredAt()).isEqualTo(enteredAt);
        assertThat(queue.getExpiredAt()).isEqualTo(expiredAt);
    }
}
