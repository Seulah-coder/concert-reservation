package com.example.concert_reservation.loadtest;

import com.example.concert_reservation.domain.balance.components.BalanceManager;
import com.example.concert_reservation.domain.concert.infrastructure.ConcertDateJpaRepository;
import com.example.concert_reservation.domain.concert.infrastructure.SeatJpaRepository;
import com.example.concert_reservation.domain.concert.infrastructure.entity.ConcertDateEntity;
import com.example.concert_reservation.domain.concert.infrastructure.entity.SeatEntity;
import com.example.concert_reservation.domain.concert.models.SeatStatus;
import com.example.concert_reservation.domain.queue.infrastructure.RedisQueueRepository;
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
 * 부하 테스트 #2: 좌석 예약 동시성 경쟁 테스트
 * 
 * 목적: 비관적 락(Pessimistic Lock)의 동시성 제어 검증
 * 규모: 30만명이 50개 좌석 경쟁 (6,000:1 경쟁률)
 * 예상 소요시간: 10-20분
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.profiles.active=test",
    "spring.main.allow-bean-definition-overriding=true"
})
@DisplayName("부하 테스트 #2: 좌석 예약 동시성 테스트")
class LoadTest2_ReservationConcurrencyTest {

    @Autowired
    private ReservationManager reservationManager;
    
    @Autowired
    private BalanceManager balanceManager;
    
    @Autowired
    private RedisQueueRepository queueRepository;
    
    @Autowired
    private ConcertDateJpaRepository concertDateRepository;
    
    @Autowired
    private SeatJpaRepository seatRepository;
    
    @Autowired
    private ReservationJpaRepository reservationRepository;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    private static final int TOTAL_USERS = 300_000;
    private static final int TOTAL_SEATS = 50;
    private static final int THREAD_POOL_SIZE = 1000;
    private static final BigDecimal SEAT_PRICE = new BigDecimal("50000");
    
    private Long concertDateId;
    private List<Long> seatIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        // 데이터 초기화
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        concertDateRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        
        // 콘서트 생성
        ConcertDateEntity concert = new ConcertDateEntity(
            null,
            "인기 콘서트 - 아이유",
            LocalDate.now().plusDays(30),
            TOTAL_SEATS,
            TOTAL_SEATS
        );
        concert = concertDateRepository.save(concert);
        concertDateId = concert.getId();
        
        // 50개 좌석 생성
        for (int i = 1; i <= TOTAL_SEATS; i++) {
            SeatEntity seat = new SeatEntity(
                null,
                concertDateId,
                i,
                SeatStatus.AVAILABLE.name(),
                SEAT_PRICE
            );
            seat = seatRepository.save(seat);
            seatIds.add(seat.getId());
        }
        
        System.out.println("✅ 테스트 데이터 준비 완료");
        System.out.println("   콘서트 ID: " + concertDateId);
        System.out.println("   생성된 좌석 수: " + seatIds.size());
    }

    @Test
    @DisplayName("⚡ 30만명이 50개 좌석 동시 예약 경쟁")
    void test_300k_users_compete_for_50_seats() throws InterruptedException {
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🚀 부하 테스트 시작: 좌석 예약 동시성 테스트");
        System.out.println("=".repeat(80));
        System.out.println("📊 테스트 설정:");
        System.out.println("   - 총 사용자 수: " + String.format("%,d", TOTAL_USERS) + "명");
        System.out.println("   - 총 좌석 수: " + TOTAL_SEATS + "석");
        System.out.println("   - 경쟁률: " + String.format("%,d", TOTAL_USERS / TOTAL_SEATS) + ":1");
        System.out.println("   - 스레드 풀 크기: " + THREAD_POOL_SIZE);
        System.out.println("=".repeat(80) + "\n");
        
        // 30만명 사용자 준비 (대기열 활성화 + 잔액 충전)
        System.out.println("⏳ 사용자 준비 중...");
        List<String> userIds = new ArrayList<>();
        for (int i = 0; i < TOTAL_USERS; i++) {
            userIds.add("load_test_user_" + i);
        }
        System.out.println("✅ " + String.format("%,d", userIds.size()) + "명 사용자 ID 생성 완료");
        
        // 결과 수집용
        ConcurrentHashMap<String, Reservation> successReservations = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        ConcurrentHashMap<String, String> failureReasons = new ConcurrentHashMap<>();
        List<Long> responseTimes = new CopyOnWriteArrayList<>();
        
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        CountDownLatch latch = new CountDownLatch(TOTAL_USERS);
        
        Instant startTime = Instant.now();
        
        // 30만명이 동시에 예약 시도
        for (int i = 0; i < TOTAL_USERS; i++) {
            final String userId = userIds.get(i);
            final Long targetSeatId = seatIds.get(i % TOTAL_SEATS); // 좌석을 순환하며 배정
            
            executorService.submit(() -> {
                try {
                    // 잔액 충전 (예약 전 필요)
                    balanceManager.chargeBalance(userId, new BigDecimal("100000"));
                    
                    // 대기열 토큰 활성화
                    queueRepository.addToWaitingQueue(userId);
                    queueRepository.activateTokens(TOTAL_USERS);
                    
                    long requestStart = System.currentTimeMillis();
                    
                    try {
                        Reservation reservation = reservationManager.reserveSeat(userId, targetSeatId);
                        long requestEnd = System.currentTimeMillis();
                        
                        responseTimes.add(requestEnd - requestStart);
                        successReservations.put(userId, reservation);
                        successCount.incrementAndGet();
                        
                    } catch (Exception e) {
                        long requestEnd = System.currentTimeMillis();
                        responseTimes.add(requestEnd - requestStart);
                        failureCount.incrementAndGet();
                        failureReasons.put(userId, e.getMessage());
                    }
                    
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
            
            // 진행 상황 출력
            if ((i + 1) % 30000 == 0) {
                System.out.println("⏳ 진행률: " + ((i + 1) * 100 / TOTAL_USERS) + "% (" + String.format("%,d", i + 1) + "/" + String.format("%,d", TOTAL_USERS) + ")");
            }
        }
        
        // 모든 작업 완료 대기
        boolean completed = latch.await(30, TimeUnit.MINUTES);
        executorService.shutdown();
        
        Instant endTime = Instant.now();
        Duration duration = Duration.between(startTime, endTime);
        
        // 결과 분석
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 테스트 결과 분석");
        System.out.println("=".repeat(80));
        
        assertThat(completed).isTrue();
        
        int totalProcessed = successCount.get() + failureCount.get();
        System.out.println("\n[처리 결과]");
        System.out.println("   총 시도: " + String.format("%,d", totalProcessed) + "건");
        System.out.println("   예약 성공: " + String.format("%,d", successCount.get()) + "건");
        System.out.println("   예약 실패: " + String.format("%,d", failureCount.get()) + "건");
        System.out.println("   성공률: " + String.format("%.4f%%", (successCount.get() * 100.0 / totalProcessed)));
        
        // 성능 메트릭
        double totalSeconds = duration.getSeconds() + duration.getNano() / 1_000_000_000.0;
        double tps = totalProcessed / totalSeconds;
        
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
            
            System.out.println("\n[응답시간 분석]");
            System.out.println("   평균: " + avgResponseTime + "ms");
            System.out.println("   50th percentile: " + p50 + "ms");
            System.out.println("   95th percentile: " + p95 + "ms");
            System.out.println("   99th percentile: " + p99 + "ms");
        }
        
        // 실패 원인 분석
        System.out.println("\n[실패 원인 분석]");
        Map<String, Integer> errorCounts = new HashMap<>();
        for (String reason : failureReasons.values()) {
            errorCounts.merge(reason, 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : errorCounts.entrySet()) {
            System.out.println("   " + entry.getKey() + ": " + String.format("%,d", entry.getValue()) + "건");
        }
        
        // 좌석별 예약 분포
        System.out.println("\n[좌석별 예약 분포]");
        Map<Long, Integer> seatReservationCount = new HashMap<>();
        for (Reservation reservation : successReservations.values()) {
            seatReservationCount.merge(reservation.getSeatId(), 1, Integer::sum);
        }
        System.out.println("   예약된 좌석 수: " + seatReservationCount.size() + "/" + TOTAL_SEATS);
        
        // 데이터 정합성 검증
        long actualReservedSeats = seatRepository.findAll().stream()
            .filter(seat -> seat.getStatus().equals(SeatStatus.RESERVED.name()))
            .count();
            
        System.out.println("\n[데이터 정합성 검증]");
        System.out.println("   예약 성공 건수: " + successCount.get());
        System.out.println("   DB 예약된 좌석 수: " + actualReservedSeats);
        System.out.println("   정합성 일치 여부: " + (successCount.get() == actualReservedSeats ? "✅" : "❌"));
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎉 테스트 완료!");
        System.out.println("=".repeat(80) + "\n");
        
        // 검증
        assertThat(successCount.get()).isLessThanOrEqualTo(TOTAL_SEATS); // 좌석 수 이하로만 예약 성공
        assertThat(successCount.get()).isEqualTo(actualReservedSeats); // DB와 일치
        assertThat(successCount.get()).isGreaterThan(0); // 최소 1건 이상 성공
    }
}
