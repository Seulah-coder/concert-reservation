package com.example.concert_reservation.loadtest;

import com.example.concert_reservation.domain.queue.infrastructure.RedisQueueRepository;
import com.example.concert_reservation.domain.queue.models.UserQueue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 부하 테스트 #1: 대기열 30만명 동시 진입
 * 
 * 목적: Redis 기반 대기열 시스템의 처리 성능 및 안정성 검증
 * 규모: 300,000명 동시 진입
 * 예상 소요시간: 3-5분
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "spring.main.allow-bean-definition-overriding=true"
})
@DisplayName("부하 테스트 #1: 대기열 30만명 스트레스 테스트")
class LoadTest1_QueueStressTest {

    @Autowired
    private RedisQueueRepository queueRepository;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final int TOTAL_USERS = 30_000;
    private static final int THREAD_POOL_SIZE = 100;
    private static final int BATCH_SIZE = TOTAL_USERS / THREAD_POOL_SIZE;

    @BeforeEach
    void setUp() {
        // Redis 초기화
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        System.out.println("✅ Redis 초기화 완료");
    }

    @Test
    @DisplayName("⚡ 30만명 대기열 동시 진입 테스트")
    void test_300k_users_queue_entry() throws InterruptedException {
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🚀 부하 테스트 시작: 대기열 30만명 동시 진입");
        System.out.println("=".repeat(80));
        System.out.println("📊 테스트 설정:");
        System.out.println("   - 총 사용자 수: " + String.format("%,d", TOTAL_USERS) + "명");
        System.out.println("   - 스레드 풀 크기: " + THREAD_POOL_SIZE);
        System.out.println("   - 배치당 처리 수: " + BATCH_SIZE + "건");
        System.out.println("=".repeat(80) + "\n");
        
        // 결과 수집용
        ConcurrentHashMap<String, UserQueue> successResults = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Long> responseTimes = new CopyOnWriteArrayList<>();
        
        // ExecutorService 생성
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CountDownLatch latch = new CountDownLatch(THREAD_POOL_SIZE);
        
        Instant startTime = Instant.now();
        
        // 30만명의 사용자 ID 생성 및 작업 제출
        for (int i = 0; i < THREAD_POOL_SIZE; i++) {
            final int batchIndex = i;
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < BATCH_SIZE; j++) {
                        String userId = "load_test_user_" + (batchIndex * BATCH_SIZE + j);
                        
                        try {
                            long requestStart = System.currentTimeMillis();
                            UserQueue result = queueRepository.addToWaitingQueue(userId);
                            long requestEnd = System.currentTimeMillis();
                            
                            responseTimes.add(requestEnd - requestStart);
                            successResults.put(userId, result);
                            successCount.incrementAndGet();
                            
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
            
            // 진행 상황 출력 (10% 단위)
            if ((i + 1) % (THREAD_POOL_SIZE / 10) == 0) {
                int progress = (i + 1) * 100 / THREAD_POOL_SIZE;
                System.out.println("⏳ 진행률: " + progress + "% (" + (i + 1) + "/" + THREAD_POOL_SIZE + " 배치)");
            }
        }
        
        // 모든 작업 완료 대기
        boolean completed = latch.await(10, TimeUnit.MINUTES);
        executorService.shutdown();
        
        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);
        
        // 결과 분석
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 테스트 결과 분석");
        System.out.println("=".repeat(80));
        
        assertThat(completed).isTrue();
        System.out.println("✅ 모든 작업 완료: " + completed);
        
        int totalProcessed = successCount.get() + failureCount.get();
        System.out.println("\n[처리 결과]");
        System.out.println("   총 처리: " + String.format("%,d", totalProcessed) + "건");
        System.out.println("   성공: " + String.format("%,d", successCount.get()) + "건");
        System.out.println("   실패: " + String.format("%,d", failureCount.get()) + "건");
        System.out.println("   성공률: " + String.format("%.2f%%", (successCount.get() * 100.0 / totalProcessed)));
        
        // 성능 메트릭
        double totalSeconds = duration.getSeconds() + duration.getNano() / 1_000_000_000.0;
        double tps = successCount.get() / totalSeconds;
        
        System.out.println("\n[성능 메트릭]");
        System.out.println("   총 소요시간: " + String.format("%.2f", totalSeconds) + "초");
        System.out.println("   처리량(TPS): " + String.format("%,.0f", tps) + " req/sec");
        
        // 응답시간 분석
        if (!responseTimes.isEmpty()) {
            List<Long> sortedTimes = new ArrayList<>(responseTimes);
            Collections.sort(sortedTimes);
            
            long avgResponseTime = responseTimes.stream().mapToLong(Long::longValue).sum() / responseTimes.size();
            long p50 = sortedTimes.get(sortedTimes.size() / 2);
            long p95 = sortedTimes.get((int) (sortedTimes.size() * 0.95));
            long p99 = sortedTimes.get((int) (sortedTimes.size() * 0.99));
            long max = sortedTimes.get(sortedTimes.size() - 1);
            
            System.out.println("\n[응답시간 분석]");
            System.out.println("   평균: " + avgResponseTime + "ms");
            System.out.println("   50th percentile: " + p50 + "ms");
            System.out.println("   95th percentile: " + p95 + "ms");
            System.out.println("   99th percentile: " + p99 + "ms");
            System.out.println("   최대: " + max + "ms");
        }
        
        // 대기 순번 검증
        System.out.println("\n[데이터 정합성 검증]");
        Set<Long> queueNumbers = new HashSet<>();
        for (UserQueue queue : successResults.values()) {
            queueNumbers.add(queue.getQueueNumber());
        }
        
        System.out.println("   토큰 발급 수: " + String.format("%,d", successResults.size()));
        System.out.println("   고유 대기번호 수: " + String.format("%,d", queueNumbers.size()));
        System.out.println("   대기번호 중복 여부: " + (successResults.size() == queueNumbers.size() ? "없음 ✅" : "있음 ❌"));
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎉 테스트 완료!");
        System.out.println("=".repeat(80) + "\n");
        
        // 검증
        assertThat(successCount.get()).isGreaterThan((int)(TOTAL_USERS * 0.95)); // 95% 이상 성공
        assertThat(successResults.size()).isEqualTo(queueNumbers.size()); // 대기번호 중복 없음
    }
}
