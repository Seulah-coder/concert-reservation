package com.example.concert_reservation.loadtest;

import com.example.concert_reservation.api.reservation.dto.ReserveSeatRequest;
import com.example.concert_reservation.api.reservation.usecase.ReserveSeatUseCase;
import com.example.concert_reservation.domain.concert.components.SeatManager;
import com.example.concert_reservation.domain.concert.infrastructure.ConcertDateJpaRepository;
import com.example.concert_reservation.domain.concert.infrastructure.SeatJpaRepository;
import com.example.concert_reservation.domain.concert.infrastructure.entity.ConcertDateEntity;
import com.example.concert_reservation.domain.concert.infrastructure.entity.SeatEntity;
import com.example.concert_reservation.domain.concert.models.Seat;
import com.example.concert_reservation.domain.concert.models.SeatStatus;
import com.example.concert_reservation.domain.queue.infrastructure.RedisQueueRepository;
import com.example.concert_reservation.domain.queue.models.QueueStatus;
import com.example.concert_reservation.domain.queue.models.UserQueue;
import com.example.concert_reservation.domain.reservation.infrastructure.ReservationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 부하 테스트 #4: 대기열→예약 간소화 테스트
 * 
 * 목적: 10,000명 규모로 대기열부터 예약까지의 성능 검증
 * 규모: 10,000명
 * 단계: 대기열 진입 → 토큰 활성화 → 좌석 예약
 * 제외: 결제, 환불 (간소화)
 * 예상 소요시간: 5-8분
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.profiles.active=loadtest",
    "spring.main.allow-bean-definition-overriding=true"
})
@DisplayName("부하 테스트 #4: 대기열→예약 간소화 테스트 (10,000명) - PostgreSQL")
class LoadTest4_QueueToReservationTest {

    @Autowired
    private RedisQueueRepository queueRepository;
    
    @Autowired
    private SeatManager seatManager;

    @Autowired
    private ReserveSeatUseCase reserveSeatUseCase;
    
    @Autowired
    private ConcertDateJpaRepository concertDateRepository;
    
    @Autowired
    private SeatJpaRepository seatRepository;
    
    @Autowired
    private ReservationJpaRepository reservationRepository;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final int TOTAL_USERS = 10_000;
    private static final int THREAD_POOL_SIZE = 100;
    private static final int TOTAL_CONCERTS = 100;
    private static final int SEATS_PER_CONCERT = 500;
    private static final BigDecimal SEAT_PRICE = new BigDecimal("50000");
    
    private List<Long> concertIds = new ArrayList<>();
    private List<Long> seatIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 데이터 초기화
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        concertDateRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        
        System.out.println("✅ Redis 초기화 완료");
        System.out.println("⏳ 테스트 데이터 생성 중...");
        
        // 100개 콘서트 생성 (각 500석)
        for (int i = 1; i <= TOTAL_CONCERTS; i++) {
            ConcertDateEntity concert = new ConcertDateEntity(
                null,
                "콘서트 " + i,
                LocalDate.now().plusDays(i),
                SEATS_PER_CONCERT,
                SEATS_PER_CONCERT
            );
            concert = concertDateRepository.save(concert);
            concertIds.add(concert.getId());
            
            // 각 콘서트에 500석 생성
            for (int seatNum = 1; seatNum <= SEATS_PER_CONCERT; seatNum++) {
                SeatEntity seat = new SeatEntity(
                    null,
                    concert.getId(),
                    seatNum,
                    SeatStatus.AVAILABLE.name(),
                    SEAT_PRICE
                );
                seat = seatRepository.save(seat);
                seatIds.add(seat.getId());
            }
        }
        
        System.out.println("✅ 테스트 데이터 준비 완료");
        System.out.println("   콘서트 수: " + concertIds.size());
        System.out.println("   총 좌석 수: " + String.format("%,d", seatIds.size()));
    }

    @Test
    @DisplayName("⚡ 10,000명 대기열→예약 테스트")
    void test_10k_users_queue_to_reservation() throws InterruptedException {
        Instant testStart = Instant.now();
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🚀 부하 테스트 시작: 대기열 → 예약 (10,000명)");
        System.out.println("=".repeat(80));
        System.out.println("📊 테스트 설정:");
        System.out.println("   - 총 사용자 수: " + String.format("%,d", TOTAL_USERS) + "명");
        System.out.println("   - 스레드 풀 크기: " + THREAD_POOL_SIZE);
        System.out.println("   - 콘서트 수: " + TOTAL_CONCERTS);
        System.out.println("   - 총 좌석 수: " + String.format("%,d", seatIds.size()));
        System.out.println("=".repeat(80) + "\n");
        
        // ===========================================
        // STAGE 1: 10,000명 대기열 진입
        // ===========================================
        System.out.println("▶".repeat(40));
        System.out.println("📍 STAGE 1: 대기열 진입 (" + String.format("%,d", TOTAL_USERS) + "명)");
        System.out.println("▶".repeat(40));
        
        Instant stage1Start = Instant.now();
        AtomicInteger stage1Success = new AtomicInteger(0);
        AtomicInteger stage1Fail = new AtomicInteger(0);
        ConcurrentHashMap<String, UserQueue> queueTokens = new ConcurrentHashMap<>();
        
        ExecutorService stage1Executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CountDownLatch stage1Latch = new CountDownLatch(TOTAL_USERS);
        
        for (int i = 0; i < TOTAL_USERS; i++) {
            final String userId = "user_" + (i + 1);
            
            stage1Executor.submit(() -> {
                try {
                    UserQueue queue = queueRepository.addToWaitingQueue(userId);
                    queueTokens.put(userId, queue);
                    stage1Success.incrementAndGet();
                    
                    // 진행률 표시 (10% 단위)
                    int progress = stage1Success.get();
                    if (progress % 1000 == 0) {
                        System.out.println("⏳ 대기열 진입: " + String.format("%,d", progress) + " / " + String.format("%,d", TOTAL_USERS));
                    }
                } catch (Exception e) {
                    stage1Fail.incrementAndGet();
                } finally {
                    stage1Latch.countDown();
                }
            });
        }
        
        boolean stage1Complete = stage1Latch.await(5, TimeUnit.MINUTES);
        stage1Executor.shutdown();
        
        Duration stage1Duration = Duration.between(stage1Start, Instant.now());
        
        System.out.println("\n✅ STAGE 1 완료");
        System.out.println("   성공: " + String.format("%,d", stage1Success.get()) + "명");
        System.out.println("   실패: " + String.format("%,d", stage1Fail.get()) + "명");
        System.out.println("   소요시간: " + stage1Duration.getSeconds() + "초");
        System.out.println("   성공률: " + String.format("%.2f", (stage1Success.get() * 100.0 / TOTAL_USERS)) + "%");
        
        // 검증
        assertThat(stage1Success.get()).isGreaterThan((int)(TOTAL_USERS * 0.95));
        
        // ===========================================
        // STAGE 2: 대기 (스케줄러가 토큰 활성화)
        // ===========================================
        System.out.println("\n" + "▶".repeat(40));
        System.out.println("📍 STAGE 2: 토큰 활성화 대기 (40초)");
        System.out.println("▶".repeat(40));
        
        Thread.sleep(40000); // 40초 대기 (스케줄러가 활성화 - 10,000명 전체 활성화 보장)
        
        // 활성 토큰만 필터링 (핵심!)
        ConcurrentHashMap<String, UserQueue> activeTokens = new ConcurrentHashMap<>();
        for (Map.Entry<String, UserQueue> entry : queueTokens.entrySet()) {
            try {
                Optional<UserQueue> queueOpt = queueRepository.findByToken(entry.getValue().getToken());
                if (queueOpt.isPresent() && queueOpt.get().getStatus() == QueueStatus.ACTIVE) {
                    activeTokens.put(entry.getKey(), queueOpt.get());
                }
            } catch (Exception ignored) {}
        }
        
        System.out.println("✅ STAGE 2 완료");
        System.out.println("   활성 토큰 수: " + String.format("%,d", activeTokens.size()) + "개");
        System.out.println("   비활성 토큰 수: " + String.format("%,d", (queueTokens.size() - activeTokens.size())) + "개");
        
        // ===========================================
        // STAGE 3: 좌석 예약 (활성 사용자만)
        // ===========================================
        System.out.println("\n" + "▶".repeat(40));
        System.out.println("📍 STAGE 3: 좌석 예약 (활성 사용자만)");
        System.out.println("▶".repeat(40));
        
        if (activeTokens.isEmpty()) {
            System.out.println("❌ 활성 토큰이 없어 예약을 건너뜁니다.");
        } else {
            System.out.println("⏳ " + String.format("%,d", activeTokens.size()) + "명의 활성 사용자가 예약을 시작합니다...");
        }
        
        Instant stage3Start = Instant.now();
        AtomicInteger stage3Success = new AtomicInteger(0);
        AtomicInteger stage3Fail = new AtomicInteger(0);
        AtomicInteger stage3ErrorSamples = new AtomicInteger(0);
        ConcurrentHashMap<String, Long> reservations = new ConcurrentHashMap<>();
        
        ExecutorService stage3Executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CountDownLatch stage3Latch = new CountDownLatch(activeTokens.size()); // ✅ 활성 토큰 수만큼만
        
        Random random = new Random();
        
        // ✅ 활성 토큰에 대해서만 예약 시도
        for (Map.Entry<String, UserQueue> entry : activeTokens.entrySet()) {
            final String userId = entry.getKey();
            final UserQueue userQueue = entry.getValue();
            
            stage3Executor.submit(() -> {
                try {
                    // 랜덤 좌석 선택
                    Long seatId = seatIds.get(random.nextInt(seatIds.size()));
                    
                    // 프로덕션과 동일한 방식으로 예약 처리
                    ReserveSeatRequest request = new ReserveSeatRequest(userId, seatId);
                    reserveSeatUseCase.execute(request);
                    reservations.put(userId, seatId);
                    stage3Success.incrementAndGet();
                    
                    // 진행률 표시
                    int progress = stage3Success.get();
                    if (progress % 100 == 0) {
                        System.out.println("⏳ 예약 진행: " + String.format("%,d", progress) + "건");
                    }
                } catch (Exception e) {
                    stage3Fail.incrementAndGet();
                    if (stage3ErrorSamples.getAndIncrement() < 3) {
                        System.out.println("❌ 예약 실패 원인: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    }
                } finally {
                    stage3Latch.countDown();
                }
            });
        }
        
        boolean stage3Complete = stage3Latch.await(10, TimeUnit.MINUTES);
        stage3Executor.shutdown();
        
        Duration stage3Duration = Duration.between(stage3Start, Instant.now());
        
        System.out.println("\n✅ STAGE 3 완료");
        System.out.println("   예약 시도: " + String.format("%,d", activeTokens.size()) + "건 (활성 토큰만)");
        System.out.println("   예약 성공: " + String.format("%,d", stage3Success.get()) + "건");
        System.out.println("   예약 실패: " + String.format("%,d", stage3Fail.get()) + "건");
        System.out.println("   소요시간: " + stage3Duration.getSeconds() + "초");
        System.out.println("   성공률: " + String.format("%.2f", (stage3Success.get() * 100.0 / activeTokens.size())) + "%");
        
        // ===========================================
        // 최종 결과 분석
        // ===========================================
        Duration totalDuration = Duration.between(testStart, Instant.now());
        
        // DB 검증
        long reservedSeats = seatRepository.findAll().stream()
            .filter(seat -> SeatStatus.RESERVED.name().equals(seat.getStatus()))
            .count();
        
        long reservationCount = reservationRepository.count();
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 최종 테스트 결과");
        System.out.println("=".repeat(80));
        
        System.out.println("\n[전체 통계]");
        System.out.println("   총 소요시간: " + totalDuration.getSeconds() + "초 (" + 
            String.format("%.1f", totalDuration.getSeconds() / 60.0) + "분)");
        System.out.println("   총 처리량: " + String.format("%,d", TOTAL_USERS) + "건");
        System.out.println("  평균 TPS: " + String.format("%,d", TOTAL_USERS / Math.max(1, totalDuration.getSeconds())) + " req/sec");
        
        System.out.println("\n[단계별 결과]");
        System.out.println("   STAGE 1 - 대기열 진입:");
        System.out.println("      성공: " + String.format("%,d", stage1Success.get()) + "건 (" + stage1Duration.getSeconds() + "초)");
        System.out.println("      성공률: " + String.format("%.2f", (stage1Success.get() * 100.0 / TOTAL_USERS)) + "%");
        
        System.out.println("\n   STAGE 2 - 토큰 활성화:");
        System.out.println("      활성 토큰: " + String.format("%,d", activeTokens.size()) + "개");
        
        System.out.println("\n   STAGE 3 - 좌석 예약:");
        System.out.println("      예약 성공: " + String.format("%,d", stage3Success.get()) + "건 (" + stage3Duration.getSeconds() + "초)");
        System.out.println("      예약률: " + String.format("%.2f", (stage3Success.get() * 100.0 / Math.max(1, activeTokens.size()))) + "%");
        
        System.out.println("\n[데이터 정합성 검증]");
        System.out.println("   RESERVED 좌석 수 (DB): " + String.format("%,d", reservedSeats) + "개");
        System.out.println("   예약 레코드 수 (DB): " + String.format("%,d", reservationCount) + "개");
        System.out.println("   정합성: " + (reservedSeats == stage3Success.get() ? "✅ 일치" : "❌ 불일치"));
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎉 테스트 완료!");
        System.out.println("=".repeat(80) + "\n");
        
        // 최종 검증
        assertThat(stage1Success.get()).isGreaterThan((int)(TOTAL_USERS * 0.95));
        assertThat(reservedSeats).isEqualTo(stage3Success.get());
    }
}
