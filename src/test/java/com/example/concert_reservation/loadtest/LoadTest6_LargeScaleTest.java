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
 * 부하 테스트 #6: 대규모 부하 테스트 (30,000명)
 * 
 * 목적: 30,000명 규모로 시스템 한계 및 병목 지점 확인
 * 규모: 30,000명 (이전 테스트 대비 3배)
 * 단계: 대기열 진입 → 토큰 활성화 → 잔액 충전 → 좌석 예약 → 결제
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.profiles.active=loadtest",
    "spring.main.allow-bean-definition-overriding=true"
})
@DisplayName("부하 테스트 #6: 대규모 트래픽 테스트 (30,000명)")
class LoadTest6_LargeScaleTest {

    @Autowired private RedisQueueRepository queueRepository;
    @Autowired private ReserveSeatUseCase reserveSeatUseCase;
    @Autowired private ChargeBalanceUseCase chargeBalanceUseCase;
    @Autowired private ProcessPaymentUseCase processPaymentUseCase;
    @Autowired private ConcertDateJpaRepository concertDateRepository;
    @Autowired private SeatJpaRepository seatRepository;
    @Autowired private ReservationJpaRepository reservationRepository;
    @Autowired private RedisTemplate<String, String> redisTemplate;
    
    // Config
    private static final int TOTAL_USERS = 30_000;  // 3만명
    private static final int THREAD_POOL_SIZE = 200; // 스레드 풀 증가 (100 -> 200)
    private static final int TOTAL_CONCERTS = 100;
    private static final int SEATS_PER_CONCERT = 500; // 총 50,000 좌석
    private static final BigDecimal SEAT_PRICE = new BigDecimal("50000");
    private static final BigDecimal CHARGE_AMOUNT = new BigDecimal("100000");
    
    private List<Long> seatIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        concertDateRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        
        System.out.println("✅ DB/Redis 초기화 완료");
        System.out.println("⏳ 테스트 데이터 생성 중 (총 50,000 좌석)...");
        
        List<ConcertDateEntity> concerts = new ArrayList<>();
        for (int i = 1; i <= TOTAL_CONCERTS; i++) {
            concerts.add(new ConcertDateEntity(
                null, "콘서트 " + i, LocalDate.now().plusDays(i),
                SEATS_PER_CONCERT, SEATS_PER_CONCERT
            ));
        }
        concerts = concertDateRepository.saveAll(concerts);
        
        List<SeatEntity> allSeats = new ArrayList<>();
        for (ConcertDateEntity concert : concerts) {
            List<SeatEntity> seats = new ArrayList<>();
            for (int seatNum = 1; seatNum <= SEATS_PER_CONCERT; seatNum++) {
                seats.add(new SeatEntity(null, concert.getId(), seatNum, SeatStatus.AVAILABLE.name(), SEAT_PRICE));
            }
            allSeats.addAll(seats);
        }
        
        // Batch Insert 로 성능 개선 (JPA saveAll)
        // 5만개 한번에는 많을 수 있으므로 쪼개서 저장
        int batchSize = 5000;
        for (int i = 0; i < allSeats.size(); i += batchSize) {
            int end = Math.min(i + batchSize, allSeats.size());
            List<SeatEntity> batch = seatRepository.saveAll(allSeats.subList(i, end));
            batch.forEach(s -> seatIds.add(s.getId()));
            System.out.println("   ...좌석 " + end + "개 생성 완료");
        }
        
        System.out.println("✅ 테스트 데이터 준비 완료 (콘서트: " + TOTAL_CONCERTS + 
            ", 좌석: " + String.format("%,d", seatIds.size()) + ")");
    }

    @Test
    @DisplayName("⚡ 30,000명 대기열→예약→결제 대규모 부하 테스트")
    void test_30k_large_scale_load() throws InterruptedException {
        Instant testStart = Instant.now();
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🚀 부하 테스트 시작: 대기열 → 잔액충전 → 예약 → 결제 (30,000명)");
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
        
        // 대기열 진입 부하 분산 (약간의 delay 없이 최대한 빠르게)
        for (int i = 0; i < TOTAL_USERS; i++) {
            final String userId = "user_" + (i + 1);
            executor.submit(() -> {
                try {
                    UserQueue queue = queueRepository.addToWaitingQueue(userId);
                    queueTokens.put(userId, queue);
                    int cnt = stage1Success.incrementAndGet();
                    if (cnt % 5000 == 0) System.out.println("   ⏳ 대기열 진입: " + String.format("%,d", cnt));
                } catch (Exception e) {
                    stage1Fail.incrementAndGet();
                } finally {
                    latch1.countDown();
                }
            });
        }
        
        latch1.await(10, TimeUnit.MINUTES);
        Duration stage1Duration = Duration.between(stage1Start, Instant.now());
        
        System.out.println("   ✅ STAGE 1 완료 - 성공: " + String.format("%,d", stage1Success.get()) + 
            " | 실패: " + stage1Fail.get() + " | " + stage1Duration.getSeconds() + "초\n");
        
        // ===========================================
        // STAGE 2: 토큰 활성화 대기 (3만명이므로 시간 좀 더 줌)
        // ===========================================
        System.out.println("📍 STAGE 2: 토큰 활성화 대기 (스케줄러 동작 대기 - 60초)");
        // 3만명이면 스케줄러가 여러번 돌아야 할 수 있음. (배치 사이즈에 따라 다름)
        // 보통 스케줄러가 1초마다 N명씩 활성화함. 테스트 환경에서 빠르게 하기 위해 sleep.
        // 현재 설정상 스케줄러를 직접 호출하지 않고 시스템에 맡긴다면 기다려야 함.
        Thread.sleep(60000); 
        
        ConcurrentHashMap<String, UserQueue> activeTokens = new ConcurrentHashMap<>();
        int scanCount = 0;
        for (Map.Entry<String, UserQueue> entry : queueTokens.entrySet()) {
            scanCount++;
            try {
                // Redis 부하 고려하여 조회 실패시 무시하지 않고 로깅하거나 재시도? -> 조회는 가벼움
                 queueRepository.findByToken(entry.getValue().getToken())
                     .filter(q -> q.getStatus() == QueueStatus.ACTIVE)
                     .ifPresent(q -> activeTokens.put(entry.getKey(), q));
            } catch (Exception ignored) {}
            if (scanCount % 5000 == 0) System.out.println("   ⏳ 토큰 상태 확인 중: " + scanCount + " / " + TOTAL_USERS);
        }
        
        System.out.println("   ✅ STAGE 2 완료 - 활성 토큰: " + String.format("%,d", activeTokens.size()) + "개 (전체 대비 " + 
            String.format("%.1f", activeTokens.size() * 100.0 / TOTAL_USERS) + "%)\n");
        
        // ===========================================
        // STAGE 3: 잔액 충전 (활성 사용자만)
        // ===========================================
        System.out.println("📍 STAGE 3: 잔액 충전 (" + String.format("%,d", activeTokens.size()) + "명)");
        
        Instant stage3Start = Instant.now();
        AtomicInteger stage3Success = new AtomicInteger(0);
        AtomicInteger stage3Fail = new AtomicInteger(0);
        
        CountDownLatch latch3 = new CountDownLatch(activeTokens.size());
        
        for (String userId : activeTokens.keySet()) {
            executor.submit(() -> {
                try {
                    chargeBalanceUseCase.execute(userId, CHARGE_AMOUNT);
                    int cnt = stage3Success.incrementAndGet();
                    if (cnt % 5000 == 0) System.out.println("   ⏳ 잔액 충전: " + String.format("%,d", cnt));
                } catch (Exception e) {
                    stage3Fail.incrementAndGet();
                } finally {
                    latch3.countDown();
                }
            });
        }
        
        latch3.await(10, TimeUnit.MINUTES);
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
        ConcurrentHashMap<String, Long> userReservationMap = new ConcurrentHashMap<>();
        
        CountDownLatch latch4 = new CountDownLatch(activeTokens.size());
        Random random = new Random();
        
        for (String userId : activeTokens.keySet()) {
            executor.submit(() -> {
                try {
                    // 5만개 좌석 중 랜덤 선택 -> 충돌 가능성 높음
                    Long seatId = seatIds.get(random.nextInt(seatIds.size()));
                    ReserveSeatRequest request = new ReserveSeatRequest(userId, seatId);
                    ReservationResponse response = reserveSeatUseCase.execute(request);
                    userReservationMap.put(userId, response.getReservationId());
                    int cnt = stage4Success.incrementAndGet();
                    if (cnt % 2000 == 0) System.out.println("   ⏳ 예약 성공: " + String.format("%,d", cnt));
                } catch (Exception e) {
                    // 낙관적 락/이미 예약된 좌석 등 실패 케이스
                    stage4Fail.incrementAndGet();
                } finally {
                    latch4.countDown();
                }
            });
        }
        
        latch4.await(15, TimeUnit.MINUTES);
        Duration stage4Duration = Duration.between(stage4Start, Instant.now());
        
        System.out.println("   ✅ STAGE 4 완료 - 예약 성공: " + String.format("%,d", stage4Success.get()) + 
            " | 실패: " + stage4Fail.get() + " | " + stage4Duration.getSeconds() + "초");
        
        // ===========================================
        // STAGE 5: 결제 처리 (예약 성공한 사용자만)
        // ===========================================
        int paymentTargetCount = userReservationMap.size();
        System.out.println("📍 STAGE 5: 결제 처리 (" + String.format("%,d", paymentTargetCount) + "명)");
        
        Instant stage5Start = Instant.now();
        AtomicInteger stage5Success = new AtomicInteger(0);
        AtomicInteger stage5Fail = new AtomicInteger(0);
        
        CountDownLatch latch5 = new CountDownLatch(paymentTargetCount);
        
        for (Map.Entry<String, Long> entry : userReservationMap.entrySet()) {
            final String userId = entry.getKey();
            final Long reservationId = entry.getValue();
            
            executor.submit(() -> {
                try {
                    processPaymentUseCase.execute(reservationId, userId);
                    int cnt = stage5Success.incrementAndGet();
                    if (cnt % 2000 == 0) System.out.println("   ⏳ 결제 완료: " + String.format("%,d", cnt));
                } catch (Exception e) {
                    stage5Fail.incrementAndGet();
                } finally {
                    latch5.countDown();
                }
            });
        }
        
        latch5.await(15, TimeUnit.MINUTES);
        executor.shutdown();
        Duration stage5Duration = Duration.between(stage5Start, Instant.now());
        
        System.out.println("   ✅ STAGE 5 완료 - 결제 성공: " + String.format("%,d", stage5Success.get()) + 
            " | 실패: " + stage5Fail.get() + " | " + stage5Duration.getSeconds() + "초\n");
        
        // ===========================================
        // 최종 결과 분석
        // ===========================================
        Duration totalDuration = Duration.between(testStart, Instant.now());
        long reservedSeats = seatRepository.findAll().stream()
            .filter(seat -> SeatStatus.RESERVED.name().equals(seat.getStatus()) || "SOLD".equals(seat.getStatus()))
            .count();
        long reservationCount = reservationRepository.count();
        
        System.out.println("=".repeat(80));
        System.out.println("📊 최종 테스트 결과 (30,000 Users)");
        System.out.println("=".repeat(80));
        
        System.out.println("   총 소요시간: " + totalDuration.getSeconds() + "초");
        System.out.println("   Total Users: " + TOTAL_USERS);
        
        System.out.println("\n[단계별 지표]");
        System.out.println("   1. Queue Enter:  " + String.format("%,d", stage1Success.get()) + " (" + 
            String.format("%.1f", stage1Success.get() * 100.0 / TOTAL_USERS) + "%)");
        System.out.println("   2. Token Active: " + String.format("%,d", activeTokens.size()) + " (" + 
            String.format("%.1f", activeTokens.size() * 100.0 / TOTAL_USERS) + "%)");
        System.out.println("   3. Recharge:     " + String.format("%,d", stage3Success.get()) + " (Success Rate of Active)");
        System.out.println("   4. Reservation:  " + String.format("%,d", stage4Success.get()) + " (Collision Failures Expected)");
        System.out.println("   5. Payment:      " + String.format("%,d", stage5Success.get()) + " (Success Rate of Reserved)");
        
        System.out.println("\n[정합성]");
        System.out.println("   Reserved Seats (DB): " + reservedSeats);
        System.out.println("   Reservations (DB):   " + reservationCount);
        
        assertThat(stage1Success.get()).isGreaterThan((int)(TOTAL_USERS * 0.9));
    }
}
