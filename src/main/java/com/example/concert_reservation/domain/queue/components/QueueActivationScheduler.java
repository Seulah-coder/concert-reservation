package com.example.concert_reservation.domain.queue.components;

import com.example.concert_reservation.domain.queue.infrastructure.RedisQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 대기열 토큰 활성화 스케줄러
 * 
 * 설계 방식:
 * - 10초마다 3,000명씩 Active Tokens으로 전환
 * - 동시 접속자 수: 분당 20,000명 처리 가능
 *   (10초 * 6회 = 1분, 3,000 * 6 = 18,000명)
 * 
 * 대기 시간 계산:
 * - 대기 순번이 93,283이면 약 5분 대기
 *   (93,283 / 18,000 = 5.18분)
 */
@Component
@EnableScheduling
public class QueueActivationScheduler {
    
    private static final Logger log = LoggerFactory.getLogger(QueueActivationScheduler.class);
    
    // 활성화 설정
    private static final int ACTIVATION_COUNT = 3000; // 10초마다 활성화할 토큰 수
    private static final int ACTIVATION_INTERVAL = 10000; // 10초 (밀리초)
    
    private final RedisQueueRepository redisQueueRepository;
    
    public QueueActivationScheduler(RedisQueueRepository redisQueueRepository) {
        this.redisQueueRepository = redisQueueRepository;
    }
    
    /**
     * 10초마다 대기열 토큰 활성화
     * - 만료된 Active 토큰 제거
     * - Waiting Queue에서 3,000명 Active로 전환
     */
    @Scheduled(fixedRate = ACTIVATION_INTERVAL)
    public void activateWaitingTokens() {
        try {
            // 1. 만료된 Active 토큰 제거
            int removedCount = redisQueueRepository.removeExpiredActiveTokens();
            if (removedCount > 0) {
                log.info("만료된 Active 토큰 제거: {}개", removedCount);
            }
            
            // 2. 현재 Active/Waiting 상태 조회
            long activeCount = redisQueueRepository.getActiveQueueSize();
            long waitingCount = redisQueueRepository.getWaitingQueueSize();
            
            log.debug("대기열 현황 - Active: {}명, Waiting: {}명", activeCount, waitingCount);
            
            // 3. Waiting → Active 전환
            if (waitingCount > 0) {
                int countToActivate = Math.min(ACTIVATION_COUNT, (int) waitingCount);
                List<String> activatedTokens = redisQueueRepository.activateTokens(countToActivate);
                
                if (!activatedTokens.isEmpty()) {
                    log.info("대기열 토큰 활성화: {}명 (Waiting: {} → Active: {})", 
                        activatedTokens.size(), 
                        waitingCount, 
                        activeCount + activatedTokens.size()
                    );
                }
            }
            
        } catch (Exception e) {
            log.error("대기열 활성화 스케줄러 오류", e);
        }
    }
    
    /**
     * 예상 대기 시간 계산 (분)
     * 
     * @param queueNumber 대기 순번
     * @return 예상 대기 시간 (분)
     */
    public static double calculateEstimatedWaitTime(long queueNumber) {
        // 10초마다 3,000명 → 1분당 18,000명 처리
        // 예: 93,283번 → 93,283 / 18,000 = 5.18분
        double tokensPerMinute = (60.0 / (ACTIVATION_INTERVAL / 1000.0)) * ACTIVATION_COUNT;
        return queueNumber / tokensPerMinute;
    }
    
    /**
     * 예상 대기 시간을 분:초 형식으로 반환
     * 
     * @param queueNumber 대기 순번
     * @return "X분 Y초" 형식의 문자열
     */
    public static String getEstimatedWaitTimeString(long queueNumber) {
        double minutes = calculateEstimatedWaitTime(queueNumber);
        int totalSeconds = (int) (minutes * 60);
        int min = totalSeconds / 60;
        int sec = totalSeconds % 60;
        return String.format("%d분 %d초", min, sec);
    }
}
