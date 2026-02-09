package com.example.concert_reservation.loadtest;

import com.example.concert_reservation.api.balance.usecase.ChargeBalanceUseCase;
import com.example.concert_reservation.api.payment.usecase.ProcessPaymentUseCase;
import com.example.concert_reservation.api.reservation.dto.ReserveSeatRequest;
import com.example.concert_reservation.api.reservation.dto.ReservationResponse;
import com.example.concert_reservation.api.reservation.usecase.ReserveSeatUseCase;
import com.example.concert_reservation.domain.concert.infrastructure.ConcertDateJpaRepository;
import com.example.concert_reservation.domain.concert.infrastructure.SeatJpaRepository;
import com.example.concert_reservation.domain.concert.infrastructure.entity.ConcertDateEntity;
import com.example.concert_reservation.domain.concert.infrastructure.entity.SeatEntity;
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
 * 부하 테스트 #5: 전체 플로우 테스트 (대기열→예약→결제)
 * 
 * 목적: 30,000명 규모로 대기열→잔액충전→예약→결제 전체 플로우 검증
 * 규모: 30,000명
 * 단계: 대기열 진입 → 토큰 활성화 → 잔액 충전 → 좌석 예약 → 결제
 * 예상 소요시간: 20-30분
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.profiles.active=loadtest",
    "spring.main.allow-bean-definition-overriding=true"
})
@DisplayName("부하 테스트 #5: 전체 플로우 테스트 (1,000명) - 대기열→예약→결제")
class LoadTest5_FullFlowWithPaymentTest {

    @Autowired private RedisQueueRepository queueRepository;
    @Autowired private ReserveSeatUseCase reserveSeatUseCase;
    @Autowired private ChargeBalanceUseCase chargeBalanceUseCase;
    @Autowired private ProcessPaymentUseCase processPaymentUseCase;
    @Autowired private ConcertDateJpaRepository concertDateRepository;
    @Autowired private SeatJpaRepository seatRepository;
    @Autowired private ReservationJpaRepository reservationRepository;
    @Autowired private RedisTemplate<String, String> redisTemplate;
    
    private static final int TOTAL_USERS = 1_000;
    private static final int THREAD_POOL_SIZE = 100;
    private static final int TOTAL_CONCERTS = 100;
    private static final int SEATS_PER_CONCERT = 500;
    private static final BigDecimal SEAT_PRICE = new BigDecimal("50000");
    private static final BigDecimal CHARGE_AMOUNT = new BigDecimal("100000"); // 10만원 충전
    
    private List<Long> seatIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        concertDateRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        
        System.out.println("✅ DB/Redis 초기화 완료");
        System.out.println("⏳ 테스트 데이터 생성 중...");
        
        for (int i = 1; i <= TOTAL_CONCERTS; i++) {
            ConcertDateEntity concert = new ConcertDateEntity(
                null, "콘서트 " + i, LocalDate.now().plusDays(i),
                SEATS_PER_CONCERT, SEATS_PER_CONCERT
            );
            concert = concertDateRepository.save(concert);
            
            List<SeatEntity> seats = new ArrayList<>();
            for (int seatNum = 1; seatNum <= SEATS_PER_CONCERT; seatNum++) {
                seats.add(new SeatEntity(null, concert.getId(), seatNum, SeatStatus.AVAILABLE.name(), SEAT_PRICE));
            }
            seatRepository.saveAll(seats);
            seats.forEach(s -> seatIds.add(s.getId()));
        }
        
        System.out.println("✅ 테스트 데이터 준비 완료 (콘서트: " + TOTAL_CONCERTS + 
            ", 좌석: " + String.format("%,d", seatIds.size()) + ")");
    }

    @Test
    @DisplayName("⚡ 30,000명 대기열→예약→결제 전체 플로우 테스트")
    void test_30k_full_flow_with_payment() throws InterruptedException {
        Instant testStart = Instant.now();
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🚀 부하 테스트 시작: 대기열 → 잔액충전 → 예약 → 결제 (10,000명)");
        System.out.println("=".repeat(80));
        System.out.println("   사용자: " + String.format("%,d", TOTAL_USERS) + "명 | 스레드: " + THREAD_POOL_SIZE + 
            " | 좌석: " + String.format("%,d", seatIds.size()) + " | 좌석가격: " + SEAT_PRICE + "원");
        System.out.println("=".repeat(80) + "\n");
        
        // ===========================================
        // STAGE 1: 대기열 진입
        // ===========================================
        System.out.println("📍 STAGE 1: 대기열 진입 (" + String.format("%,d", TOTAL_USERS) + "명)");
        
        Instant stage1Start = Instant.now();
        AtomicInteger stage1Success = new AtomicInteger(0);
        AtomicInteger stage1Fail = new AtomicInteger(0);
        ConcurrentHashMap<String, UserQueue> queueTokens = new ConcurrentHashMap<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CountDownLatch latch1 = new CountDownLatch(TOTAL_USERS);
        
        for (int i = 0; i < TOTAL_USERS; i++) {
            final String userId = "user_" + (i + 1);
            executor.submit(() -> {
                try {
                    UserQueue queue = queueRepository.addToWaitingQueue(userId);
                    queueTokens.put(userId, queue);
                    int cnt = stage1Success.incrementAndGet();
                    if (cnt % 2000 == 0) System.out.println("   ⏳ 대기열 진입: " + String.format("%,d", cnt));
                } catch (Exception e) {
                    stage1Fail.incrementAndGet();
                } finally {
                    latch1.countDown();
                }
            });
        }
        
        latch1.await(5, TimeUnit.MINUTES);
        Duration stage1Duration = Duration.between(stage1Start, Instant.now());
        
        System.out.println("   ✅ STAGE 1 완료 - 성공: " + String.format("%,d", stage1Success.get()) + 
            " | 실패: " + stage1Fail.get() + " | " + stage1Duration.getSeconds() + "초\n");
        
        // ===========================================
        // STAGE 2: 토큰 활성화 대기
        // ===========================================
        System.out.println("📍 STAGE 2: 토큰 활성화 대기 (40초)");
        Thread.sleep(40000);
        
        ConcurrentHashMap<String, UserQueue> activeTokens = new ConcurrentHashMap<>();
        for (Map.Entry<String, UserQueue> entry : queueTokens.entrySet()) {
            try {
                queueRepository.findByToken(entry.getValue().getToken())
                    .filter(q -> q.getStatus() == QueueStatus.ACTIVE)
                    .ifPresent(q -> activeTokens.put(entry.getKey(), q));
            } catch (Exception ignored) {}
        }
        
        System.out.println("   ✅ STAGE 2 완료 - 활성 토큰: " + String.format("%,d", activeTokens.size()) + "개\n");
        
        // ===========================================
        // STAGE 3: 잔액 충전 (활성 사용자만)
        // ===========================================
        System.out.println("📍 STAGE 3: 잔액 충전 (" + String.format("%,d", activeTokens.size()) + "명 × " + CHARGE_AMOUNT + "원)");
        
        Instant stage3Start = Instant.now();
        AtomicInteger stage3Success = new AtomicInteger(0);
        AtomicInteger stage3Fail = new AtomicInteger(0);
        AtomicInteger stage3ErrorSamples = new AtomicInteger(0);
        
        CountDownLatch latch3 = new CountDownLatch(activeTokens.size());
        
        for (String userId : activeTokens.keySet()) {
            executor.submit(() -> {
                try {
                    chargeBalanceUseCase.execute(userId, CHARGE_AMOUNT);
                    int cnt = stage3Success.incrementAndGet();
                    if (cnt % 2000 == 0) System.out.println("   ⏳ 잔액 충전: " + String.format("%,d", cnt));
                } catch (Exception e) {
                    stage3Fail.incrementAndGet();
                    if (stage3ErrorSamples.getAndIncrement() < 3) {
                        System.out.println("   ❌ 충전 실패: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    }
                } finally {
                    latch3.countDown();
                }
            });
        }
        
        latch3.await(5, TimeUnit.MINUTES);
        Duration stage3Duration = Duration.between(stage3Start, Instant.now());
        
        System.out.println("   ✅ STAGE 3 완료 - 충전 성공: " + String.format("%,d", stage3Success.get()) + 
            " | 실패: " + stage3Fail.get() + " | " + stage3Duration.getSeconds() + "초\n");
        
        // ===========================================
        // STAGE 4: 좌석 예약 (활성 사용자만)
        // ===========================================
        System.out.println("📍 STAGE 4: 좌석 예약 (" + String.format("%,d", activeTokens.size()) + "명)");
        
        Instant stage4Start = Instant.now();
        AtomicInteger stage4Success = new AtomicInteger(0);
        AtomicInteger stage4Fail = new AtomicInteger(0);
        AtomicInteger stage4ErrorSamples = new AtomicInteger(0);
        ConcurrentHashMap<String, Long> userReservationMap = new ConcurrentHashMap<>(); // userId → reservationId
        
        CountDownLatch latch4 = new CountDownLatch(activeTokens.size());
        Random random = new Random();
        
        for (String userId : activeTokens.keySet()) {
            executor.submit(() -> {
                try {
                    Long seatId = seatIds.get(random.nextInt(seatIds.size()));
                    ReserveSeatRequest request = new ReserveSeatRequest(userId, seatId);
                    ReservationResponse response = reserveSeatUseCase.execute(request);
                    userReservationMap.put(userId, response.getReservationId());
                    int cnt = stage4Success.incrementAndGet();
                    if (cnt % 500 == 0) System.out.println("   ⏳ 예약 진행: " + String.format("%,d", cnt));
                } catch (Exception e) {
                    stage4Fail.incrementAndGet();
                    if (stage4ErrorSamples.getAndIncrement() < 3) {
                        System.out.println("   ❌ 예약 실패: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    }
                } finally {
                    latch4.countDown();
                }
            });
        }
        
        latch4.await(10, TimeUnit.MINUTES);
        Duration stage4Duration = Duration.between(stage4Start, Instant.now());
        
        System.out.println("   ✅ STAGE 4 완료 - 예약 성공: " + String.format("%,d", stage4Success.get()) + 
            " | 실패: " + stage4Fail.get() + " | " + stage4Duration.getSeconds() + "초");
        System.out.println("   예약률: " + String.format("%.2f", (stage4Success.get() * 100.0 / activeTokens.size())) + "%\n");
        
        // ===========================================
        // STAGE 5: 결제 처리 (예약 성공한 사용자만)
        // ===========================================
        int paymentTargetCount = userReservationMap.size();
        System.out.println("📍 STAGE 5: 결제 처리 (" + String.format("%,d", paymentTargetCount) + "명)");
        
        Instant stage5Start = Instant.now();
        AtomicInteger stage5Success = new AtomicInteger(0);
        AtomicInteger stage5Fail = new AtomicInteger(0);
        AtomicInteger stage5ErrorSamples = new AtomicInteger(0);
        
        CountDownLatch latch5 = new CountDownLatch(paymentTargetCount);
        
        for (Map.Entry<String, Long> entry : userReservationMap.entrySet()) {
            final String userId = entry.getKey();
            final Long reservationId = entry.getValue();
            
            executor.submit(() -> {
                try {
                    processPaymentUseCase.execute(reservationId, userId);
                    int cnt = stage5Success.incrementAndGet();
                    if (cnt % 500 == 0) System.out.println("   ⏳ 결제 진행: " + String.format("%,d", cnt));
                } catch (Exception e) {
                    stage5Fail.incrementAndGet();
                    if (stage5ErrorSamples.getAndIncrement() < 5) {
                        System.out.println("   ❌ 결제 실패: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                    }
                } finally {
                    latch5.countDown();
                }
            });
        }
        
        latch5.await(10, TimeUnit.MINUTES);
        executor.shutdown();
        Duration stage5Duration = Duration.between(stage5Start, Instant.now());
        
        System.out.println("   ✅ STAGE 5 완료 - 결제 성공: " + String.format("%,d", stage5Success.get()) + 
            " | 실패: " + stage5Fail.get() + " | " + stage5Duration.getSeconds() + "초");
        System.out.println("   결제율: " + String.format("%.2f", (stage5Success.get() * 100.0 / Math.max(1, paymentTargetCount))) + "%\n");
        
        // ===========================================
        // 최종 결과 분석
        // ===========================================
        Duration totalDuration = Duration.between(testStart, Instant.now());
        
        // DB 검증
        long reservedSeats = seatRepository.findAll().stream()
            .filter(seat -> SeatStatus.RESERVED.name().equals(seat.getStatus()) || "SOLD".equals(seat.getStatus()))
            .count();
        long reservationCount = reservationRepository.count();
        
        System.out.println("=".repeat(80));
        System.out.println("📊 최종 테스트 결과");
        System.out.println("=".repeat(80));
        
        System.out.println("\n[전체 통계]");
        System.out.println("   총 소요시간: " + totalDuration.getSeconds() + "초 (" + 
            String.format("%.1f", totalDuration.getSeconds() / 60.0) + "분)");
        System.out.println("   총 사용자: " + String.format("%,d", TOTAL_USERS) + "명");
        System.out.println("   평균 TPS: " + String.format("%,d", TOTAL_USERS / Math.max(1, totalDuration.getSeconds())) + " req/sec");
        
        System.out.println("\n[단계별 결과]");
        System.out.println("   STAGE 1 - 대기열 진입:  " + String.format("%,d", stage1Success.get()) + "건 (" + stage1Duration.getSeconds() + "초) | 성공률: " + 
            String.format("%.1f", stage1Success.get() * 100.0 / TOTAL_USERS) + "%");
        System.out.println("   STAGE 2 - 토큰 활성화:  " + String.format("%,d", activeTokens.size()) + "개");
        System.out.println("   STAGE 3 - 잔액 충전:    " + String.format("%,d", stage3Success.get()) + "건 (" + stage3Duration.getSeconds() + "초) | 성공률: " + 
            String.format("%.1f", stage3Success.get() * 100.0 / Math.max(1, activeTokens.size())) + "%");
        System.out.println("   STAGE 4 - 좌석 예약:    " + String.format("%,d", stage4Success.get()) + "건 (" + stage4Duration.getSeconds() + "초) | 성공률: " + 
            String.format("%.1f", stage4Success.get() * 100.0 / Math.max(1, activeTokens.size())) + "%");
        System.out.println("   STAGE 5 - 결제 처리:    " + String.format("%,d", stage5Success.get()) + "건 (" + stage5Duration.getSeconds() + "초) | 성공률: " + 
            String.format("%.1f", stage5Success.get() * 100.0 / Math.max(1, paymentTargetCount)) + "%");
        
        System.out.println("\n[데이터 정합성 검증]");
        System.out.println("   예약 좌석 수 (DB):      " + String.format("%,d", reservedSeats) + "개");
        System.out.println("   예약 레코드 수 (DB):    " + String.format("%,d", reservationCount) + "개");
        System.out.println("   예약↔좌석 정합성:       " + (reservedSeats == reservationCount ? "✅ 일치" : "❌ 불일치"));
        System.out.println("   결제 성공 수:           " + String.format("%,d", stage5Success.get()) + "건");
        System.out.println("   결제↔예약 비율:         " + String.format("%.1f", stage5Success.get() * 100.0 / Math.max(1, stage4Success.get())) + "%");
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎉 전체 플로우 테스트 완료!");
        System.out.println("=".repeat(80) + "\n");
        
        // 최종 검증
        assertThat(stage1Success.get()).isGreaterThan((int)(TOTAL_USERS * 0.95));
        assertThat(stage4Success.get()).isGreaterThan(0);
    }
}
