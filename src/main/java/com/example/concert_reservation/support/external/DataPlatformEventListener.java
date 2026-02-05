package com.example.concert_reservation.support.external;

import com.example.concert_reservation.domain.payment.events.PaymentCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 데이터 플랫폼 이벤트 리스너
 * 
 * 핵심 설계:
 * 1. @TransactionalEventListener(AFTER_COMMIT)
 *    - 결제 트랜잭션 커밋 후 실행
 *    - 외부 API 실패가 결제 롤백 안 함
 * 
 * 2. @Async
 *    - 비동기 처리로 응답 속도 개선
 *    - 스레드 풀에서 실행
 * 
 * 3. @Retryable
 *    - 외부 API 일시적 장애 대응
 *    - 최대 3회 재시도, 지수 백오프
 * 
 * 4. 실패 시 전략
 *    - 로그 기록
 *    - 별도 실패 큐에 적재 (옵션)
 *    - 배치로 재전송 (옵션)
 */
@Component
public class DataPlatformEventListener {
    
    private static final Logger log = LoggerFactory.getLogger(DataPlatformEventListener.class);
    
    private final DataPlatformClient dataPlatformClient;
    
    public DataPlatformEventListener(DataPlatformClient dataPlatformClient) {
        this.dataPlatformClient = dataPlatformClient;
    }
    
    /**
     * 결제 완료 이벤트 처리
     * 
     * 실행 시점: 결제 트랜잭션 커밋 직후
     * 실행 방식: 비동기 (별도 스레드)
     * 재시도: 최대 3회, 2초 간격, 지수 백오프
     * 
     * 장점:
     * - 외부 API 장애가 결제 성공에 영향 없음
     * - 사용자 응답 속도 개선 (외부 API 대기 안 함)
     * - 자동 재시도로 일시적 장애 복구
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Retryable(
        maxAttempts = 3,
        backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        try {
            log.info("데이터 플랫폼 전송 시작: paymentId={}", event.paymentId());
            
            // 외부 API 호출
            dataPlatformClient.sendOrderData(
                event.paymentId(),
                event.reservationId(),
                event.userId(),
                event.amount(),
                event.paidAt(),
                event.concertTitle(),
                event.seatNumber()
            );
            
            log.info("데이터 플랫폼 전송 성공: paymentId={}", event.paymentId());
            
        } catch (Exception e) {
            log.error("데이터 플랫폼 전송 실패: paymentId={}, error={}", 
                event.paymentId(), e.getMessage(), e);
            
            // 실패 처리 전략 (선택)
            // 1. 실패 큐에 적재 → 배치로 재전송
            // 2. 알람 발송 (Slack, PagerDuty 등)
            // 3. 메트릭 증가 (실패율 모니터링)
            
            throw e;  // 재시도 위해 예외 전파
        }
    }
    
    /**
     * 알림 발송 (선택 사항)
     * 
     * 결제 완료 시 사용자에게 알림 발송
     * - 카카오톡, SMS, 이메일 등
     * - 외부 알림 서비스 호출
     * - 실패해도 결제에 영향 없음
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendNotification(PaymentCompletedEvent event) {
        try {
            log.info("알림 발송 시작: userId={}, paymentId={}", 
                event.userId(), event.paymentId());
            
            // 알림 서비스 호출
            // notificationService.send(event.userId(), "결제가 완료되었습니다");
            
            log.info("알림 발송 완료");
            
        } catch (Exception e) {
            // 알림 실패는 로그만 기록 (재시도 안 함)
            log.warn("알림 발송 실패: userId={}, error={}", 
                event.userId(), e.getMessage());
        }
    }
    
    /**
     * 통계 집계 (선택 사항)
     * 
     * 실시간 매출, 예약 통계 업데이트
     * - Redis 카운터 증가
     * - 통계 DB 업데이트
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void updateStatistics(PaymentCompletedEvent event) {
        try {
            log.debug("통계 업데이트: amount={}", event.amount());
            
            // Redis 카운터 증가
            // redisTemplate.opsForValue().increment("stats:revenue:daily", event.amount());
            
        } catch (Exception e) {
            log.warn("통계 업데이트 실패: {}", e.getMessage());
        }
    }
}
