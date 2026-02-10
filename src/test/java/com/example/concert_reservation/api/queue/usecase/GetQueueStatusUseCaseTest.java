package com.example.concert_reservation.api.queue.usecase;

import com.example.concert_reservation.api.queue.dto.QueueStatusResponse;
import com.example.concert_reservation.domain.queue.components.QueueValidator;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GetQueueStatusUseCase 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("대기열 상태 조회 UseCase 테스트")
class GetQueueStatusUseCaseTest {
    
    @Mock
    private QueueValidator queueValidator;
    
    @InjectMocks
    private GetQueueStatusUseCase useCase;
    
    private QueueToken token;
    private UserQueue waitingQueue;
    private UserQueue activeQueue;
    
    @BeforeEach
    void setUp() {
        token = QueueToken.generate();
        waitingQueue = UserQueue.create("user123", 5L);
        activeQueue = UserQueue.create("user456", 1L);
        activeQueue.activate(30);
    }
    
    @Test
    @DisplayName("WAITING 상태의 대기열은 앞에 대기 인원을 포함하여 조회된다")
    void execute_waitingQueue_includesWaitingAhead() {
        // given
        when(queueValidator.validateAndGetQueue(any(QueueToken.class))).thenReturn(waitingQueue);
        when(queueValidator.countWaitingAhead(waitingQueue.getToken())).thenReturn(3L);
        
        // when
        QueueStatusResponse response = useCase.execute(token.getValue());
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo(waitingQueue.getToken().getValue());
        assertThat(response.getUserId()).isEqualTo("user123");
        assertThat(response.getQueueNumber()).isEqualTo(5L);
        assertThat(response.getStatus()).isEqualTo(QueueStatus.WAITING.name());
        assertThat(response.getWaitingAhead()).isEqualTo(3L);
        assertThat(response.getEnteredAt()).isNotNull();
        assertThat(response.getExpiredAt()).isNull();
        
        verify(queueValidator).validateAndGetQueue(any(QueueToken.class));
        verify(queueValidator).countWaitingAhead(waitingQueue.getToken());
    }
    
    @Test
    @DisplayName("ACTIVE 상태의 대기열은 대기 인원이 0이다")
    void execute_activeQueue_waitingAheadIsZero() {
        // given
        when(queueValidator.validateAndGetQueue(any(QueueToken.class))).thenReturn(activeQueue);
        
        // when
        QueueStatusResponse response = useCase.execute(token.getValue());
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(QueueStatus.ACTIVE.name());
        assertThat(response.getWaitingAhead()).isEqualTo(0L);
        assertThat(response.getExpiredAt()).isNotNull();
        
        verify(queueValidator).validateAndGetQueue(any(QueueToken.class));
        verify(queueValidator, never()).countWaitingAhead(any(QueueToken.class));
    }
    
    @Test
    @DisplayName("유효하지 않은 토큰은 예외가 발생한다")
    void execute_invalidToken_throwsException() {
        // given
        QueueToken invalidToken = QueueToken.generate();
        when(queueValidator.validateAndGetQueue(any(QueueToken.class)))
            .thenThrow(new IllegalArgumentException("유효하지 않은 토큰입니다"));
        
        // when & then
        assertThatThrownBy(() -> useCase.execute(invalidToken.getValue()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("유효하지 않은 토큰입니다");
        
        verify(queueValidator).validateAndGetQueue(any(QueueToken.class));
    }
    
    @Test
    @DisplayName("만료된 토큰도 조회할 수 있다")
    void execute_expiredQueue_success() {
        // given
        UserQueue expiredQueue = UserQueue.create("user789", 1L);
        expiredQueue.expire();
        
        when(queueValidator.validateAndGetQueue(any(QueueToken.class))).thenReturn(expiredQueue);
        
        // when
        QueueStatusResponse response = useCase.execute(token.getValue());
        
        // then
        assertThat(response.getStatus()).isEqualTo(QueueStatus.EXPIRED.name());
        assertThat(response.getWaitingAhead()).isEqualTo(0L);
        assertThat(response.getExpiredAt()).isNotNull();
    }
    
    @Test
    @DisplayName("대기 번호가 1번인 사용자는 앞에 대기자가 0명이다")
    void execute_firstInLine_waitingAheadIsZero() {
        // given
        UserQueue firstQueue = UserQueue.create("user111", 1L);
        when(queueValidator.validateAndGetQueue(any(QueueToken.class))).thenReturn(firstQueue);
        when(queueValidator.countWaitingAhead(firstQueue.getToken())).thenReturn(0L);
        
        // when
        QueueStatusResponse response = useCase.execute(token.getValue());
        
        // then
        assertThat(response.getQueueNumber()).isEqualTo(1L);
        assertThat(response.getWaitingAhead()).isEqualTo(0L);
    }
}
