package com.example.concert_reservation.config;

import com.example.concert_reservation.domain.queue.models.QueueStatus;
import com.example.concert_reservation.domain.queue.models.QueueToken;
import com.example.concert_reservation.domain.queue.models.UserQueue;
import com.example.concert_reservation.domain.queue.repositories.QueueStoreRepository;
import com.example.concert_reservation.support.exception.TokenMissingException;
import com.example.concert_reservation.support.exception.TokenNotActiveException;
import com.example.concert_reservation.support.exception.TokenNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueueTokenInterceptor 테스트")
class QueueTokenInterceptorTest {
    
    @Mock
    private QueueStoreRepository queueStoreRepository;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @InjectMocks
    private QueueTokenInterceptor interceptor;
    
    private QueueToken validToken;
    private UserQueue activeQueue;
    private UserQueue waitingQueue;
    private UserQueue expiredQueue;
    
    @BeforeEach
    void setUp() {
        validToken = QueueToken.generate();
        
        // ACTIVE 상태 대기열
        activeQueue = UserQueue.of(
            1L,
            validToken,
            "user123",
            100L,
            QueueStatus.ACTIVE,
            LocalDateTime.now().minusMinutes(5),
            LocalDateTime.now().plusMinutes(25)
        );
        
        // WAITING 상태 대기열
        waitingQueue = UserQueue.of(
            2L,
            QueueToken.generate(),
            "user456",
            200L,
            QueueStatus.WAITING,
            LocalDateTime.now().minusMinutes(10),
            null
        );
        
        // EXPIRED 상태 대기열
        expiredQueue = UserQueue.of(
            3L,
            QueueToken.generate(),
            "user789",
            300L,
            QueueStatus.EXPIRED,
            LocalDateTime.now().minusMinutes(40),
            LocalDateTime.now().minusMinutes(10)
        );
    }
    
    @Test
    @DisplayName("유효한 ACTIVE 토큰이면 true를 반환한다")
    void preHandle_validActiveToken_returnsTrue() {
        // given
        given(request.getHeader("X-Queue-Token")).willReturn(validToken.getValue());
        given(queueStoreRepository.findByToken(validToken)).willReturn(Optional.of(activeQueue));
        
        // when
        boolean result = interceptor.preHandle(request, response, new Object());
        
        // then
        assertThat(result).isTrue();
        verify(request).setAttribute("userId", "user123");
        verify(request).setAttribute("queueToken", validToken.getValue());
    }
    
    @Test
    @DisplayName("토큰이 없으면 TokenMissingException을 던진다")
    void preHandle_missingToken_throwsException() {
        // given
        given(request.getHeader("X-Queue-Token")).willReturn(null);
        
        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
            .isInstanceOf(TokenMissingException.class)
            .hasMessageContaining("대기열 토큰이 필요합니다");
    }
    
    @Test
    @DisplayName("빈 문자열 토큰이면 TokenMissingException을 던진다")
    void preHandle_emptyToken_throwsException() {
        // given
        given(request.getHeader("X-Queue-Token")).willReturn("");
        
        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
            .isInstanceOf(TokenMissingException.class)
            .hasMessageContaining("대기열 토큰이 필요합니다");
    }
    
    @Test
    @DisplayName("존재하지 않는 토큰이면 TokenNotFoundException을 던진다")
    void preHandle_invalidToken_throwsException() {
        // given
        String invalidTokenValue = "invalid-token";
        QueueToken invalidToken = QueueToken.of(invalidTokenValue);
        given(request.getHeader("X-Queue-Token")).willReturn(invalidTokenValue);
        given(queueStoreRepository.findByToken(invalidToken)).willReturn(Optional.empty());
        
        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
            .isInstanceOf(TokenNotFoundException.class)
            .hasMessageContaining("유효하지 않은 토큰");
    }
    
    @Test
    @DisplayName("WAITING 상태 토큰이면 TokenNotActiveException을 던진다")
    void preHandle_waitingToken_throwsException() {
        // given
        QueueToken waitingToken = waitingQueue.getToken();
        given(request.getHeader("X-Queue-Token")).willReturn(waitingToken.getValue());
        given(queueStoreRepository.findByToken(waitingToken)).willReturn(Optional.of(waitingQueue));
        
        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
            .isInstanceOf(TokenNotActiveException.class)
            .hasMessageContaining("활성 상태가 아닙니다")
            .hasMessageContaining("WAITING");
    }
    
    @Test
    @DisplayName("EXPIRED 상태 토큰이면 TokenNotActiveException을 던진다")
    void preHandle_expiredToken_throwsException() {
        // given
        QueueToken expiredToken = expiredQueue.getToken();
        given(request.getHeader("X-Queue-Token")).willReturn(expiredToken.getValue());
        given(queueStoreRepository.findByToken(expiredToken)).willReturn(Optional.of(expiredQueue));
        
        // when & then
        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
            .isInstanceOf(TokenNotActiveException.class)
            .hasMessageContaining("활성 상태가 아닙니다")
            .hasMessageContaining("EXPIRED");
    }
}
