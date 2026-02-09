package com.example.concert_reservation.loadtest;

import com.example.concert_reservation.domain.balance.components.BalanceManager;
import com.example.concert_reservation.domain.balance.models.Balance;
import com.example.concert_reservation.domain.concert.infrastructure.ConcertDateJpaRepository;
import com.example.concert_reservation.domain.concert.infrastructure.SeatJpaRepository;
import com.example.concert_reservation.domain.concert.infrastructure.entity.ConcertDateEntity;
import com.example.concert_reservation.domain.concert.infrastructure.entity.SeatEntity;
import com.example.concert_reservation.domain.concert.models.SeatStatus;
import com.example.concert_reservation.domain.payment.components.PaymentProcessor;
import com.example.concert_reservation.domain.payment.models.Payment;
import com.example.concert_reservation.domain.queue.infrastructure.RedisQueueRepository;
import com.example.concert_reservation.domain.refund.components.RefundProcessor;
import com.example.concert_reservation.domain.refund.models.Refund;
import com.example.concert_reservation.domain.reservation.components.ReservationManager;
import com.example.concert_reservation.domain.reservation.infrastructure.ReservationJpaRepository;
import com.example.concert_reservation.domain.reservation.models.Reservation;
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
 * 부하 테스트 #3: 전체 플로우 부하 테스트
 * 
 * 목적: E2E 시나리오의 전체 처리량 및 병목 구간 식별
 * 규모: 대기열 → 예약 → 결제 → 환불 전체 프로세스
 * 단계: 30만명 대기 → 10만명 활성화 → 5만명 예약 → 3만명 결제 → 1만명 환불
 * 예상 소요시간: 20-40분
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "spring.main.allow-bean-definition-overriding=true"
})
@DisplayName("부하 테스트 #3: 전체 E2E 플로우 부하 테스트")
class LoadTest3_FullFlowLoadTest {

    @Autowired
    private RedisQueueRepository queueRepository;
    
    @Autowired
    private BalanceManager balanceManager;
    
    @Autowired
    private ReservationManager reservationManager;
    
    @Autowired
    private PaymentProcessor paymentProcessor;
    
    @Autowired
    private RefundProcessor refundProcessor;
    
    @Autowired
    private ConcertDateJpaRepository concertDateRepository;
    
    @Autowired
    private SeatJpaRepository seatRepository;
    
    @Autowired
    private ReservationJpaRepository reservationRepository;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final int STAGE1_QUEUE_USERS = 300_000;  // 대기열 진입
    private static final int STAGE2_ACTIVE_USERS = 100_000; // 활성화
    private static final int STAGE3_RESERVE_USERS = 50_000; // 예약 시도
    private static final int STAGE4_PAYMENT_USERS = 30_000; // 결제 시도
    private static final int STAGE5_REFUND_USERS = 10_000;  // 환불 시도
    
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
        
        System.out.println("⏳ 테스트 데이터 생성 중...");
        
        // 100개 콘서트 생성 (각 500석)
        for (int i = 1; i <= TOTAL_CONCERTS; i++) {
            ConcertDateEntity concert = new ConcertDateEntity(
                null,
                "콘서트 #" + i,
                LocalDate.now().plusDays(30 + i),
                SEATS_PER_CONCERT,
                SEATS_PER_CONCERT
            );
            concert = concertDateRepository.save(concert);
            concertIds.add(concert.getId());
            
            // 각 콘서트당 500개 좌석 생성
            for (int j = 1; j <= SEATS_PER_CONCERT; j++) {
                SeatEntity seat = new SeatEntity(
                    null,
                    concert.getId(),
                    j,
                    SeatStatus.AVAILABLE.name(),
                    SEAT_PRICE
                );
                seat = seatRepository.save(seat);
                seatIds.add(seat.getId());
            }
            
            if (i % 10 == 0) {
                System.out.println("   콘서트 생성: " + i + "/" + TOTAL_CONCERTS);
            }
        }
        
        System.out.println("✅ 테스트 데이터 준비 완료");
        System.out.println("   콘서트 수: " + concertIds.size());
        System.out.println("   총 좌석 수: " + String.format("%,d", seatIds.size()));
    }

    @Test
    @DisplayName("⚡ 전체 E2E 플로우: 대기열 → 예약 → 결제 → 환불")
    void test_full_flow_30_10_5_3_1() throws InterruptedException {
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🚀 부하 테스트 시작: 전체 E2E 플로우");
        System.out.println("=".repeat(80));
        System.out.println("📊 테스트 단계:");
        System.out.println("   Stage 1: " + String.format("%,d", STAGE1_QUEUE_USERS) + "명 대기열 진입");
        System.out.println("   Stage 2: " + String.format("%,d", STAGE2_ACTIVE_USERS) + "명 활성화");
        System.out.println("   Stage 3: " + String.format("%,d", STAGE3_RESERVE_USERS) + "명 예약 시도");
        System.out.println("   Stage 4: " + String.format("%,d", STAGE4_PAYMENT_USERS) + "명 결제 시도");
        System.out.println("   Stage 5: " + String.format("%,d", STAGE5_REFUND_USERS) + "명 환불 시도");
        System.out.println("=".repeat(80) + "\n");
        
        Map<String, Object> testResults = new ConcurrentHashMap<>();
        
        // ===========================================
        // STAGE 1: 30만명 대기열 진입
        // ===========================================
        System.out.println("\n" + "▶".repeat(40));
        System.out.println("📍 STAGE 1: 대기열 진입 (" + String.format("%,d", STAGE1_QUEUE_USERS) + "명)");
        System.out.println("▶".repeat(40));
        
        Instant stage1Start = Instant.now();
        AtomicInteger stage1Success = new AtomicInteger(0);
        
        ExecutorService stage1Executor = Executors.newFixedThreadPool(500);
        CountDownLatch stage1Latch = new CountDownLatch(STAGE1_QUEUE_USERS);
        
        for (int i = 0; i < STAGE1_QUEUE_USERS; i++) {
            final String userId = "user_" + i;
            stage1Executor.submit(() -> {
                try {
                    queueRepository.addToWaitingQueue(userId);
                    stage1Success.incrementAndGet();
                } catch (Exception e) {
                    // 실패 카운트
                } finally {
                    stage1Latch.countDown();
                }
            });
            
            if ((i + 1) % 30000 == 0) {
                System.out.println("⏳ 진행: " + String.format("%,d", i + 1) + "/" + String.format("%,d", STAGE1_QUEUE_USERS));
            }
        }
        
        stage1Latch.await(10, TimeUnit.MINUTES);
        stage1Executor.shutdown();
        
        Duration stage1Duration = Duration.between(stage1Start, Instant.now());
        testResults.put("stage1_success", stage1Success.get());
        testResults.put("stage1_duration_sec", stage1Duration.getSeconds());
        
        System.out.println("✅ STAGE 1 완료: " + String.format("%,d", stage1Success.get()) + "명 진입 (" + stage1Duration.getSeconds() + "초)");
        
        // ===========================================
        // STAGE 2: 10만명 활성화
        // ===========================================
        System.out.println("\n" + "▶".repeat(40));
        System.out.println("📍 STAGE 2: 대기열 활성화 (" + String.format("%,d", STAGE2_ACTIVE_USERS) + "명)");
        System.out.println("▶".repeat(40));
        
        Instant stage2Start = Instant.now();
        queueRepository.activateTokens(STAGE2_ACTIVE_USERS);
        Duration stage2Duration = Duration.between(stage2Start, Instant.now());
        
        testResults.put("stage2_activated", STAGE2_ACTIVE_USERS);
        testResults.put("stage2_duration_sec", stage2Duration.getSeconds());
        
        System.out.println("✅ STAGE 2 완료: " + String.format("%,d", STAGE2_ACTIVE_USERS) + "명 활성화 (" + stage2Duration.getSeconds() + "초)");
        
        // ===========================================
        // STAGE 3: 5만명 예약 시도
        // ===========================================
        System.out.println("\n" + "▶".repeat(40));
        System.out.println("📍 STAGE 3: 좌석 예약 (" + String.format("%,d", STAGE3_RESERVE_USERS) + "명)");
        System.out.println("▶".repeat(40));
        
        Instant stage3Start = Instant.now();
        AtomicInteger stage3Success = new AtomicInteger(0);
        ConcurrentHashMap<String, Reservation> reservations = new ConcurrentHashMap<>();
        
        ExecutorService stage3Executor = Executors.newFixedThreadPool(1000);
        CountDownLatch stage3Latch = new CountDownLatch(STAGE3_RESERVE_USERS);
        
        for (int i = 0; i < STAGE3_RESERVE_USERS; i++) {
            final String userId = "user_" + i;
            final Long seatId = seatIds.get(i % seatIds.size());
            
            stage3Executor.submit(() -> {
                try {
                    balanceManager.chargeBalance(userId, new BigDecimal("100000"));
                    Reservation reservation = reservationManager.reserveSeat(userId, seatId);
                    reservations.put(userId, reservation);
                    stage3Success.incrementAndGet();
                } catch (Exception e) {
                    // 실패 (좌석 중복 등)
                } finally {
                    stage3Latch.countDown();
                }
            });
            
            if ((i + 1) % 5000 == 0) {
                System.out.println("⏳ 진행: " + String.format("%,d", i + 1) + "/" + String.format("%,d", STAGE3_RESERVE_USERS));
            }
        }
        
        stage3Latch.await(30, TimeUnit.MINUTES);
        stage3Executor.shutdown();
        
        Duration stage3Duration = Duration.between(stage3Start, Instant.now());
        testResults.put("stage3_success", stage3Success.get());
        testResults.put("stage3_duration_sec", stage3Duration.getSeconds());
        
        System.out.println("✅ STAGE 3 완료: " + String.format("%,d", stage3Success.get()) + "건 예약 성공 (" + stage3Duration.getSeconds() + "초)");
        
        // ===========================================
        // STAGE 4: 3만명 결제 시도
        // ===========================================
        System.out.println("\n" + "▶".repeat(40));
        System.out.println("📍 STAGE 4: 결제 처리 (" + String.format("%,d", STAGE4_PAYMENT_USERS) + "명)");
        System.out.println("▶".repeat(40));
        
        Instant stage4Start = Instant.now();
        AtomicInteger stage4Success = new AtomicInteger(0);
        ConcurrentHashMap<String, Payment> payments = new ConcurrentHashMap<>();
        
        ExecutorService stage4Executor = Executors.newFixedThreadPool(500);
        List<String> reservedUsers = new ArrayList<>(reservations.keySet());
        int paymentTarget = Math.min(STAGE4_PAYMENT_USERS, reservedUsers.size());
        CountDownLatch stage4Latch = new CountDownLatch(paymentTarget);
        
        for (int i = 0; i < paymentTarget; i++) {
            final String userId = reservedUsers.get(i);
            final Reservation reservation = reservations.get(userId);
            
            stage4Executor.submit(() -> {
                try {
                    Payment payment = paymentProcessor.processPayment(reservation.getId(), userId);
                    payments.put(userId, payment);
                    stage4Success.incrementAndGet();
                } catch (Exception e) {
                    // 결제 실패 (잔액 부족 등)
                } finally {
                    stage4Latch.countDown();
                }
            });
            
            if ((i + 1) % 3000 == 0) {
                System.out.println("⏳ 진행: " + String.format("%,d", i + 1) + "/" + String.format("%,d", paymentTarget));
            }
        }
        
        stage4Latch.await(20, TimeUnit.MINUTES);
        stage4Executor.shutdown();
        
        Duration stage4Duration = Duration.between(stage4Start, Instant.now());
        testResults.put("stage4_success", stage4Success.get());
        testResults.put("stage4_duration_sec", stage4Duration.getSeconds());
        
        System.out.println("✅ STAGE 4 완료: " + String.format("%,d", stage4Success.get()) + "건 결제 성공 (" + stage4Duration.getSeconds() + "초)");
        
        // ===========================================
        // STAGE 5: 1만명 환불 시도
        // ===========================================
        System.out.println("\n" + "▶".repeat(40));
        System.out.println("📍 STAGE 5: 환불 처리 (" + String.format("%,d", STAGE5_REFUND_USERS) + "명)");
        System.out.println("▶".repeat(40));
        
        Instant stage5Start = Instant.now();
        AtomicInteger stage5Success = new AtomicInteger(0);
        
        ExecutorService stage5Executor = Executors.newFixedThreadPool(500);
        List<String> paidUsers = new ArrayList<>(payments.keySet());
        int refundTarget = Math.min(STAGE5_REFUND_USERS, paidUsers.size());
        CountDownLatch stage5Latch = new CountDownLatch(refundTarget);
        
        for (int i = 0; i < refundTarget; i++) {
            final String userId = paidUsers.get(i);
            final Payment payment = payments.get(userId);
            
            stage5Executor.submit(() -> {
                try {
                    Refund refund = refundProcessor.processRefund(payment.getId(), userId, "부하 테스트");
                    stage5Success.incrementAndGet();
                } catch (Exception e) {
                    // 환불 실패
                } finally {
                    stage5Latch.countDown();
                }
            });
            
            if ((i + 1) % 1000 == 0) {
                System.out.println("⏳ 진행: " + String.format("%,d", i + 1) + "/" + String.format("%,d", refundTarget));
            }
        }
        
        stage5Latch.await(15, TimeUnit.MINUTES);
        stage5Executor.shutdown();
        
        Duration stage5Duration = Duration.between(stage5Start, Instant.now());
        testResults.put("stage5_success", stage5Success.get());
        testResults.put("stage5_duration_sec", stage5Duration.getSeconds());
        
        System.out.println("✅ STAGE 5 완료: " + String.format("%,d", stage5Success.get()) + "건 환불 성공 (" + stage5Duration.getSeconds() + "초)");
        
        // ===========================================
        // 최종 결과 분석
        // ===========================================
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 전체 E2E 플로우 테스트 결과");
        System.out.println("=".repeat(80));
        
        System.out.println("\n[단계별 결과]");
        System.out.println("   Stage 1 (대기열 진입): " + String.format("%,d", (int)testResults.get("stage1_success")) + "명 / " + testResults.get("stage1_duration_sec") + "초");
        System.out.println("   Stage 2 (활성화): " + String.format("%,d", (int)testResults.get("stage2_activated")) + "명 / " + testResults.get("stage2_duration_sec") + "초");
        System.out.println("   Stage 3 (예약): " + String.format("%,d", (int)testResults.get("stage3_success")) + "건 / " + testResults.get("stage3_duration_sec") + "초");
        System.out.println("   Stage 4 (결제): " + String.format("%,d", (int)testResults.get("stage4_success")) + "건 / " + testResults.get("stage4_duration_sec") + "초");
        System.out.println("   Stage 5 (환불): " + String.format("%,d", (int)testResults.get("stage5_success")) + "건 / " + testResults.get("stage5_duration_sec") + "초");
        
        long totalSeconds = (long)testResults.get("stage1_duration_sec") + 
                           (long)testResults.get("stage2_duration_sec") +
                           (long)testResults.get("stage3_duration_sec") +
                           (long)testResults.get("stage4_duration_sec") +
                           (long)testResults.get("stage5_duration_sec");
        
        System.out.println("\n[전체 메트릭]");
        System.out.println("   총 소요시간: " + totalSeconds + "초 (" + (totalSeconds / 60) + "분)");
        System.out.println("   대기열 → 결제 전환율: " + String.format("%.2f%%", ((int)testResults.get("stage4_success") * 100.0 / STAGE1_QUEUE_USERS)));
        System.out.println("   예약 → 결제 전환율: " + String.format("%.2f%%", ((int)testResults.get("stage4_success") * 100.0 / (int)testResults.get("stage3_success"))));
        System.out.println("   환불율: " + String.format("%.2f%%", ((int)testResults.get("stage5_success") * 100.0 / (int)testResults.get("stage4_success"))));
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎉 전체 플로우 테스트 완료!");
        System.out.println("=".repeat(80) + "\n");
        
        // 검증
        assertThat((int)testResults.get("stage1_success")).isGreaterThan((int)(STAGE1_QUEUE_USERS * 0.95));
        assertThat((int)testResults.get("stage3_success")).isGreaterThan(0);
        assertThat((int)testResults.get("stage4_success")).isGreaterThan(0);
    }
}
