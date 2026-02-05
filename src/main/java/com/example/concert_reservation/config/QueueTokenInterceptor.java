package com.example.concert_reservation.config;

import com.example.concert_reservation.domain.queue.models.QueueToken;
import com.example.concert_reservation.domain.queue.models.UserQueue;
import com.example.concert_reservation.domain.queue.repositories.QueueStoreRepository;
import com.example.concert_reservation.support.exception.TokenMissingException;
import com.example.concert_reservation.support.exception.TokenNotActiveException;
import com.example.concert_reservation.support.exception.TokenNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 대기열 토큰 검증 Interceptor
 * 예약/결제/환불 API 호출 전에 토큰의 ACTIVE 상태를 검증
 */
@Component
public class QueueTokenInterceptor implements HandlerInterceptor {
    
    private static final String TOKEN_HEADER = "X-Queue-Token";
    
    private final QueueStoreRepository queueStoreRepository;
    
    public QueueTokenInterceptor(QueueStoreRepository queueStoreRepository) {
        this.queueStoreRepository = queueStoreRepository;
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, 
                            HttpServletResponse response, 
                            Object handler) {
        
        // 1. Header에서 X-Queue-Token 추출
        String tokenValue = request.getHeader(TOKEN_HEADER);
        
        // 2. 토큰 존재 여부 확인
        if (tokenValue == null || tokenValue.trim().isEmpty()) {
            throw new TokenMissingException("대기열 토큰이 필요합니다. Header에 'X-Queue-Token'을 포함해주세요.");
        }
        
        // 3. 토큰으로 UserQueue 조회
        QueueToken token = QueueToken.of(tokenValue);
        UserQueue userQueue = queueStoreRepository.findByToken(token)
            .orElseThrow(() -> new TokenNotFoundException("유효하지 않은 토큰입니다: " + tokenValue));
        
        // 4. ACTIVE 상태 확인
        if (!userQueue.isActive()) {
            String statusMessage = String.format(
                "대기열 토큰이 활성 상태가 아닙니다. 현재 상태: %s", 
                userQueue.getStatus()
            );
            
            if (userQueue.isWaiting()) {
                statusMessage += String.format(" (대기 순서: %d번)", userQueue.getQueueNumber());
            }
            
            throw new TokenNotActiveException(statusMessage, userQueue.getStatus().name());
        }
        
        // 5. userId를 request attribute에 저장 (컨트롤러에서 사용 가능)
        request.setAttribute("userId", userQueue.getUserId());
        request.setAttribute("queueToken", tokenValue);
        
        return true; // 검증 통과
    }
}
