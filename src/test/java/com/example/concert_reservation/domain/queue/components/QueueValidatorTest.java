package com.example.concert_reservation.domain.queue.components;

import com.example.concert_reservation.domain.queue.infrastructure.RedisQueueRepository;
import com.example.concert_reservation.domain.queue.models.QueueStatus;
import com.example.concert_reservation.domain.queue.models.QueueToken;
import com.example.concert_reservation.domain.queue.models.UserQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * QueueValidator 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("QueueValidator 컴포넌트 테스트")
class QueueValidatorTest {
    
    @Mock
    private RedisQueueRepository redisQueueRepository;
    
    @InjectMocks
    private QueueValidator queueValidator;
    
    private QueueToken activeToken;
    private QueueToken waitingToken;
    private QueueToken expiredToken;
    private QueueToken invalidToken;
    
    private UserQueue activeQueue;
    private UserQueue waitingQueue;
    private UserQueue expiredQueue;
    
    @BeforeEach
    void setUp() {
        activeToken = QueueToken.generate();
        waitingToken = QueueToken.generate();
        expiredToken = QueueToken.generate();
        invalidToken = QueueToken.generate();
        
        activeQueue = UserQueue.create("user1", 1L);
        activeQueue.activate(30);
        
        waitingQueue = UserQueue.create("user2", 2L);
        
        expiredQueue = UserQueue.create("user3", 3L);
        expiredQueue.expire();
    }
    
    @Test
    @DisplayName("활성 토큰은 isActiveToken()이 true를 반환한다")
    void isActiveToken_activeQueue_returnsTrue() {
        // given
        when(redisQueueRepository.isActiveToken(activeToken.getValue()))
            .thenReturn(true);
        
        // when
        boolean result = queueValidator.isActiveToken(activeToken);
        
        // then
        assertThat(result).isTrue();
        verify(redisQueueRepository).isActiveToken(activeToken.getValue());
    }
    
    @Test
    @DisplayName("대기 중인 토큰은 isActiveToken()이 false를 반환한다")
    void isActiveToken_waitingQueue_returnsFalse() {
        // given
        when(redisQueueRepository.isActiveToken(waitingToken.getValue()))
            .thenReturn(false);
        
        // when
        boolean result = queueValidator.isActiveToken(waitingToken);
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("존재하지 않는 토큰은 isActiveToken()이 false를 반환한다")
    void isActiveToken_notFound_returnsFalse() {
        // given
        when(redisQueueRepository.isActiveToken(invalidToken.getValue()))
            .thenReturn(false);
        
        // when
        boolean result = queueValidator.isActiveToken(invalidToken);
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("사용자가 활성 대기열을 보유하고 있으면 hasActiveQueue()가 true를 반환한다")
    void hasActiveQueue_existsActiveQueue_returnsTrue() {
        // given
        String userId = "user1";
        when(redisQueueRepository.hasActiveQueue(userId))
            .thenReturn(true);
        
        // when
        boolean result = queueValidator.hasActiveQueue(userId);
        
        // then
        assertThat(result).isTrue();
        verify(redisQueueRepository).hasActiveQueue(userId);
    }
    
    @Test
    @DisplayName("사용자가 활성 대기열을 보유하지 않으면 hasActiveQueue()가 false를 반환한다")
    void hasActiveQueue_notExists_returnsFalse() {
        // given
        String userId = "user1";
        when(redisQueueRepository.hasActiveQueue(userId))
            .thenReturn(false);
        
        // when
        boolean result = queueValidator.hasActiveQueue(userId);
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("사용자가 대기 중인 대기열을 보유하고 있으면 hasWaitingQueue()가 true를 반환한다")
    void hasWaitingQueue_existsWaitingQueue_returnsTrue() {
        // given
        String userId = "user2";
        when(redisQueueRepository.hasWaitingQueue(userId))
            .thenReturn(true);
        
        // when
        boolean result = queueValidator.hasWaitingQueue(userId);
        
        // then
        assertThat(result).isTrue();
        verify(redisQueueRepository).hasWaitingQueue(userId);
    }
    
    @Test
    @DisplayName("유효한 토큰으로 대기열을 조회할 수 있다")
    void validateAndGetQueue_validToken_returnsQueue() {
        // given
        when(redisQueueRepository.findByToken(activeToken))
            .thenReturn(Optional.of(activeQueue));
        
        // when
        UserQueue result = queueValidator.validateAndGetQueue(activeToken);
        
        // then
        assertThat(result).isEqualTo(activeQueue);
    }
    
    @Test
    @DisplayName("존재하지 않는 토큰으로 조회 시 예외가 발생한다")
    void validateAndGetQueue_invalidToken_throwsException() {
        // given
        when(redisQueueRepository.findByToken(invalidToken))
            .thenReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> queueValidator.validateAndGetQueue(invalidToken))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("유효하지 않은 토큰입니다");
    }
    
    @Test
    @DisplayName("활성 토큰 검증이 성공한다")
    void validateActiveToken_activeToken_success() {
        // given
        when(redisQueueRepository.isActiveToken(activeToken.getValue()))
            .thenReturn(true);
        
        // when & then
        assertThatCode(() -> queueValidator.validateActiveToken(activeToken))
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("비활성 토큰 검증 시 예외가 발생한다")
    void validateActiveToken_inactiveToken_throwsException() {
        // given
        when(redisQueueRepository.isActiveToken(waitingToken.getValue()))
            .thenReturn(false);
        
        // when & then
        assertThatThrownBy(() -> queueValidator.validateActiveToken(waitingToken))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("토큰이 활성 상태가 아닙니다");
    }
    
    @Test
    @DisplayName("토큰으로 앞에 대기 중인 인원을 계산할 수 있다 (ZRANK 기반)")
    void countWaitingAheadByToken_returnsCorrectCount() {
        // given
        String tokenValue = "test-token-abc";
        when(redisQueueRepository.countWaitingAheadByToken(tokenValue))
            .thenReturn(9L);
        
        // when
        long result = queueValidator.countWaitingAheadByToken(tokenValue);
        
        // then
        assertThat(result).isEqualTo(9L);
        verify(redisQueueRepository).countWaitingAheadByToken(tokenValue);
    }
    
    @Test
    @DisplayName("첫 번째 대기자의 토큰은 앞에 대기자가 0명이다 (ZRANK=0)")
    void countWaitingAheadByToken_firstInLine_returnsZero() {
        // given
        String tokenValue = "first-token";
        when(redisQueueRepository.countWaitingAheadByToken(tokenValue))
            .thenReturn(0L);
        
        // when
        long result = queueValidator.countWaitingAheadByToken(tokenValue);
        
        // then
        assertThat(result).isEqualTo(0L);
    }
    
    @Test
    @DisplayName("중간 토큰이 제거되어도 ZRANK로 정확한 대기 인원을 계산한다")
    void countWaitingAheadByToken_withGaps_returnsCorrectRank() {
        // given
        // 원래 5명(rank 0~4)이 있었으나 중간 2명이 활성화되어 제거됨
        // → 현재 사용자가 실제로는 3번째(rank 2)에 위치
        String tokenValue = "token-after-gaps";
        when(redisQueueRepository.countWaitingAheadByToken(tokenValue))
            .thenReturn(2L);  // ZRANK = 2 → 앞에 2명
        
        // when
        long result = queueValidator.countWaitingAheadByToken(tokenValue);
        
        // then
        assertThat(result).isEqualTo(2L);
    }
}
