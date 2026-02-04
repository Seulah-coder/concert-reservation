package com.example.concert_reservation.api.queue.usecase;

import com.example.concert_reservation.api.queue.dto.IssueTokenRequest;
import com.example.concert_reservation.api.queue.dto.IssueTokenResponse;
import com.example.concert_reservation.domain.queue.components.QueueValidator;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * IssueQueueTokenUseCase 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("대기열 토큰 발급 UseCase 테스트")
class IssueQueueTokenUseCaseTest {
    
    @Mock
    private QueueStoreRepository queueStoreRepository;
    
    @Mock
    private QueueValidator queueValidator;
    
    @InjectMocks
    private IssueQueueTokenUseCase useCase;
    
    private IssueTokenRequest request;
    
    @BeforeEach
    void setUp() {
        request = new IssueTokenRequest("user123");
    }
    
    @Test
    @DisplayName("새로운 사용자는 토큰을 정상적으로 발급받을 수 있다")
    void execute_newUser_success() {
        // given
        when(queueValidator.hasActiveQueue("user123")).thenReturn(false);
        when(queueValidator.hasWaitingQueue("user123")).thenReturn(false);
        when(queueStoreRepository.getNextQueueNumber()).thenReturn(1L);
        
        UserQueue savedQueue = UserQueue.create("user123", 1L);
        when(queueStoreRepository.save(any(UserQueue.class))).thenReturn(savedQueue);
        
        // when
        IssueTokenResponse response = useCase.execute(request);
        
        // then
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotNull();
        assertThat(response.getQueueNumber()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(QueueStatus.WAITING.name());
        assertThat(response.getEnteredAt()).isNotNull();
        
        verify(queueValidator).hasActiveQueue("user123");
        verify(queueValidator).hasWaitingQueue("user123");
        verify(queueStoreRepository).getNextQueueNumber();
        verify(queueStoreRepository).save(any(UserQueue.class));
    }
    
    @Test
    @DisplayName("이미 활성 토큰이 있는 사용자는 예외가 발생한다")
    void execute_hasActiveToken_throwsException() {
        // given
        when(queueValidator.hasActiveQueue("user123")).thenReturn(true);
        
        // when & then
        assertThatThrownBy(() -> useCase.execute(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("이미 활성 상태의 토큰이 존재합니다");
        
        verify(queueValidator).hasActiveQueue("user123");
        verify(queueStoreRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("이미 대기 중인 토큰이 있는 사용자는 예외가 발생한다")
    void execute_hasWaitingToken_throwsException() {
        // given
        when(queueValidator.hasActiveQueue("user123")).thenReturn(false);
        when(queueValidator.hasWaitingQueue("user123")).thenReturn(true);
        
        // when & then
        assertThatThrownBy(() -> useCase.execute(request))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("이미 대기 중인 토큰이 존재합니다");
        
        verify(queueValidator).hasActiveQueue("user123");
        verify(queueValidator).hasWaitingQueue("user123");
        verify(queueStoreRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("대기 번호는 순차적으로 증가한다")
    void execute_queueNumberIncreases() {
        // given
        when(queueValidator.hasActiveQueue(anyString())).thenReturn(false);
        when(queueValidator.hasWaitingQueue(anyString())).thenReturn(false);
        when(queueStoreRepository.getNextQueueNumber()).thenReturn(10L);
        
        UserQueue savedQueue = UserQueue.create("user123", 10L);
        when(queueStoreRepository.save(any(UserQueue.class))).thenReturn(savedQueue);
        
        // when
        IssueTokenResponse response = useCase.execute(request);
        
        // then
        assertThat(response.getQueueNumber()).isEqualTo(10L);
        verify(queueStoreRepository).getNextQueueNumber();
    }
    
    @Test
    @DisplayName("여러 사용자가 동시에 토큰을 발급받을 수 있다")
    void execute_multipleUsers_success() {
        // given
        IssueTokenRequest request1 = new IssueTokenRequest("user1");
        IssueTokenRequest request2 = new IssueTokenRequest("user2");
        
        when(queueValidator.hasActiveQueue(anyString())).thenReturn(false);
        when(queueValidator.hasWaitingQueue(anyString())).thenReturn(false);
        when(queueStoreRepository.getNextQueueNumber()).thenReturn(1L, 2L);
        
        UserQueue queue1 = UserQueue.create("user1", 1L);
        UserQueue queue2 = UserQueue.create("user2", 2L);
        when(queueStoreRepository.save(any(UserQueue.class)))
            .thenReturn(queue1)
            .thenReturn(queue2);
        
        // when
        IssueTokenResponse response1 = useCase.execute(request1);
        IssueTokenResponse response2 = useCase.execute(request2);
        
        // then
        assertThat(response1.getQueueNumber()).isEqualTo(1L);
        assertThat(response2.getQueueNumber()).isEqualTo(2L);
        assertThat(response1.getToken()).isNotEqualTo(response2.getToken());
        
        verify(queueStoreRepository, times(2)).save(any(UserQueue.class));
    }
}
