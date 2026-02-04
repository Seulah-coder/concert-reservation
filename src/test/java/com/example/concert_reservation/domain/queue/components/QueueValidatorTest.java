package com.example.concert_reservation.domain.queue.components;

import com.example.concert_reservation.domain.queue.models.QueueStatus;
import com.example.concert_reservation.domain.queue.models.QueueToken;
import com.example.concert_reservation.domain.queue.models.UserQueue;
import com.example.concert_reservation.domain.queue.repositories.QueueStoreRepository;
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
    private QueueStoreRepository queueStoreRepository;
    
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
        when(queueStoreRepository.findByToken(activeToken))
            .thenReturn(Optional.of(activeQueue));
        
        // when
        boolean result = queueValidator.isActiveToken(activeToken);
        
        // then
        assertThat(result).isTrue();
        verify(queueStoreRepository).findByToken(activeToken);
    }
    
    @Test
    @DisplayName("대기 중인 토큰은 isActiveToken()이 false를 반환한다")
    void isActiveToken_waitingQueue_returnsFalse() {
        // given
        when(queueStoreRepository.findByToken(waitingToken))
            .thenReturn(Optional.of(waitingQueue));
        
        // when
        boolean result = queueValidator.isActiveToken(waitingToken);
        
        // then
        assertThat(result).isFalse();
    }
    
    @Test
    @DisplayName("존재하지 않는 토큰은 isActiveToken()이 false를 반환한다")
    void isActiveToken_notFound_returnsFalse() {
        // given
        when(queueStoreRepository.findByToken(invalidToken))
            .thenReturn(Optional.empty());
        
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
        when(queueStoreRepository.existsByUserIdAndStatus(userId, QueueStatus.ACTIVE))
            .thenReturn(true);
        
        // when
        boolean result = queueValidator.hasActiveQueue(userId);
        
        // then
        assertThat(result).isTrue();
        verify(queueStoreRepository).existsByUserIdAndStatus(userId, QueueStatus.ACTIVE);
    }
    
    @Test
    @DisplayName("사용자가 활성 대기열을 보유하지 않으면 hasActiveQueue()가 false를 반환한다")
    void hasActiveQueue_notExists_returnsFalse() {
        // given
        String userId = "user1";
        when(queueStoreRepository.existsByUserIdAndStatus(userId, QueueStatus.ACTIVE))
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
        when(queueStoreRepository.existsByUserIdAndStatus(userId, QueueStatus.WAITING))
            .thenReturn(true);
        
        // when
        boolean result = queueValidator.hasWaitingQueue(userId);
        
        // then
        assertThat(result).isTrue();
        verify(queueStoreRepository).existsByUserIdAndStatus(userId, QueueStatus.WAITING);
    }
    
    @Test
    @DisplayName("유효한 토큰으로 대기열을 조회할 수 있다")
    void validateAndGetQueue_validToken_returnsQueue() {
        // given
        when(queueStoreRepository.findByToken(activeToken))
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
        when(queueStoreRepository.findByToken(invalidToken))
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
        when(queueStoreRepository.findByToken(activeToken))
            .thenReturn(Optional.of(activeQueue));
        
        // when & then
        assertThatCode(() -> queueValidator.validateActiveToken(activeToken))
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("비활성 토큰 검증 시 예외가 발생한다")
    void validateActiveToken_inactiveToken_throwsException() {
        // given
        when(queueStoreRepository.findByToken(waitingToken))
            .thenReturn(Optional.of(waitingQueue));
        
        // when & then
        assertThatThrownBy(() -> queueValidator.validateActiveToken(waitingToken))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("토큰이 활성 상태가 아닙니다");
    }
    
    @Test
    @DisplayName("대기 번호로 앞에 대기 중인 인원을 계산할 수 있다")
    void countWaitingAhead_returnsCorrectCount() {
        // given
        Long queueNumber = 10L;
        when(queueStoreRepository.countByStatusAndQueueNumberLessThan(
            QueueStatus.WAITING, queueNumber))
            .thenReturn(5L);  // 실제로 queueNumber < 10인 WAITING 사용자 5명
        
        // when
        long result = queueValidator.countWaitingAhead(queueNumber);
        
        // then
        assertThat(result).isEqualTo(5L);
        verify(queueStoreRepository).countByStatusAndQueueNumberLessThan(
            QueueStatus.WAITING, queueNumber);
    }
    
    @Test
    @DisplayName("대기 번호가 1이면 앞에 대기자가 0명이다")
    void countWaitingAhead_firstInLine_returnsZero() {
        // given
        Long queueNumber = 1L;
        when(queueStoreRepository.countByStatusAndQueueNumberLessThan(
            QueueStatus.WAITING, queueNumber))
            .thenReturn(0L);
        
        // when
        long result = queueValidator.countWaitingAhead(queueNumber);
        
        // then
        assertThat(result).isEqualTo(0L);
    }
    
    @Test
    @DisplayName("대기 번호에 갭이 있어도 정확한 대기 인원을 계산한다")
    void countWaitingAhead_withGapsInQueueNumbers_returnsCorrectCount() {
        // given
        // 대기 번호: 1, 5, 10, 15 (갭이 있는 경우)
        // 현재 사용자: 15번
        Long queueNumber = 15L;
        when(queueStoreRepository.countByStatusAndQueueNumberLessThan(
            QueueStatus.WAITING, queueNumber))
            .thenReturn(3L);  // 1, 5, 10번 사용자 = 3명
        
        // when
        long result = queueValidator.countWaitingAhead(queueNumber);
        
        // then
        assertThat(result).isEqualTo(3L);  // 14명이 아닌 3명!
    }
}
