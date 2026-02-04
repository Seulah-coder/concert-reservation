package com.example.concert_reservation.integration;

import com.example.concert_reservation.api.reservation.dto.ReserveSeatRequest;
import com.example.concert_reservation.api.reservation.dto.ReservationResponse;
import com.example.concert_reservation.api.reservation.usecase.CancelReservationUseCase;
import com.example.concert_reservation.api.reservation.usecase.ReserveSeatUseCase;
import com.example.concert_reservation.domain.concert.components.SeatManager;
import com.example.concert_reservation.domain.concert.infrastructure.ConcertDateJpaRepository;
import com.example.concert_reservation.domain.concert.infrastructure.SeatJpaRepository;
import com.example.concert_reservation.domain.concert.infrastructure.entity.ConcertDateEntity;
import com.example.concert_reservation.domain.concert.infrastructure.entity.SeatEntity;
import com.example.concert_reservation.domain.concert.models.SeatStatus;
import com.example.concert_reservation.domain.reservation.components.ReservationManager;
import com.example.concert_reservation.domain.reservation.infrastructure.ReservationJpaRepository;
import com.example.concert_reservation.domain.reservation.infrastructure.entity.ReservationEntity;
import com.example.concert_reservation.domain.reservation.models.Reservation;
import com.example.concert_reservation.domain.reservation.models.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 예약 전체 생명주기 통합 테스트
 * Clock injection을 사용하여 시간 제어
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@DisplayName("예약 생명주기 통합 테스트")
class ReservationLifecycleIntegrationTest {

    @Autowired
    private ReserveSeatUseCase reserveSeatUseCase;
    
    @Autowired
    private CancelReservationUseCase cancelReservationUseCase;
    
    @Autowired
    private ReservationManager reservationManager;
    
    @Autowired
    private SeatManager seatManager;
    
    @Autowired
    private ConcertDateJpaRepository concertDateJpaRepository;
    
    @Autowired
    private SeatJpaRepository seatJpaRepository;
    
    @Autowired
    private ReservationJpaRepository reservationJpaRepository;
    
    private Long concertDateId;
    private Long seatId;
    private final String userId = "user123";
    
    @BeforeEach
    void setUp() {
        // 테스트 데이터 초기화
        reservationJpaRepository.deleteAll();
        seatJpaRepository.deleteAll();
        concertDateJpaRepository.deleteAll();
        
        // 콘서트 날짜 생성 (Builder 패턴 사용)
        ConcertDateEntity concertDate = new ConcertDateEntity(
            null,  // ID는 auto-generated
            "테스트 콘서트",
            LocalDate.now().plusDays(7),
            50,
            50
        );
        concertDate = concertDateJpaRepository.save(concertDate);
        concertDateId = concertDate.getId();
        
        // 좌석 생성 (Builder 패턴 사용)
        SeatEntity seat = new SeatEntity(
            null,  // ID는 auto-generated
            concertDateId,
            1,
            SeatStatus.AVAILABLE.name(),
            new BigDecimal("50000")
        );
        seat = seatJpaRepository.save(seat);
        seatId = seat.getId();
    }
    
    @Test
    @Transactional
    @DisplayName("전체 생명주기: 예약 생성 → 확인 → 좌석 판매 완료")
    void testFullLifecycle_CreateConfirmSell() {
        // 1. 좌석 예약 (AVAILABLE → RESERVED)
        var request = new ReserveSeatRequest(userId, seatId);
        
        ReservationResponse response = reserveSeatUseCase.execute(request);
        
        assertThat(response).isNotNull();
        assertThat(response.getReservationId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.PENDING.name());
        assertThat(response.getRemainingSeconds()).isGreaterThan(0).isLessThanOrEqualTo(300); // 5분 = 300초
        
        // 좌석 상태 확인
        var seat = seatManager.getSeatByIdWithLock(seatId);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.RESERVED);
        
        // 2. 예약 확정 (PENDING → CONFIRMED)
        var reservation = reservationManager.getReservationById(response.getReservationId());
        reservationManager.confirmReservation(reservation);
        
        var confirmedReservation = reservationJpaRepository.findById(response.getReservationId()).orElseThrow();
        assertThat(confirmedReservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED.name());
        
        // 좌석 상태는 여전히 RESERVED (결제 후 SOLD로 전환)
        seat = seatManager.getSeatByIdWithLock(seatId);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.RESERVED);
        
        // 3. 좌석 판매 완료 (RESERVED → SOLD) - 결제 시스템에서 호출 예정
        seatManager.sellSeat(seat);
        
        seat = seatManager.getSeatByIdWithLock(seatId);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.SOLD);
    }
    
    @Test
    @Transactional
    @DisplayName("예약 취소: 예약 생성 → 취소 → 좌석 복구")
    void testCancellation_ReservationAndSeatRestored() {
        // 1. 좌석 예약
        var request = new ReserveSeatRequest(userId, seatId);
        
        ReservationResponse response = reserveSeatUseCase.execute(request);
        Long reservationId = response.getReservationId();
        
        // 좌석이 RESERVED 상태인지 확인
        var seat = seatManager.getSeatByIdWithLock(seatId);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.RESERVED);
        
        // 2. 예약 취소
        cancelReservationUseCase.execute(reservationId);
        
        // 예약 상태 확인
        var cancelledReservation = reservationJpaRepository.findById(reservationId).orElseThrow();
        assertThat(cancelledReservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED.name());
        
        // 좌석 상태 복구 확인
        seat = seatManager.getSeatByIdWithLock(seatId);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }
    
    @Test
    @Transactional
    @DisplayName("Clock Injection: 5분 경과 후 예약 만료 확인")
    void testExpiration_WithClockInjection() {
        // 고정된 시간 설정 (2024-01-15 10:00:00)
        Instant fixedInstant = Instant.parse("2024-01-15T10:00:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        
        // 1. 예약 생성 (Clock 사용)
        Reservation reservation = Reservation.create(userId, seatId, concertDateId, 
                                                     new BigDecimal("50000"), fixedClock);
        
        // 생성 직후에는 만료되지 않음
        assertThat(reservation.isExpired(fixedClock)).isFalse();
        assertThat(reservation.getRemainingSeconds(fixedClock)).isEqualTo(300); // 5분 = 300초
        
        // 2. 시간 진행: 3분 후
        Clock clock3MinLater = Clock.fixed(fixedInstant.plusSeconds(180), ZoneId.systemDefault());
        assertThat(reservation.isExpired(clock3MinLater)).isFalse();
        assertThat(reservation.getRemainingSeconds(clock3MinLater)).isEqualTo(120); // 2분 = 120초 남음
        
        // 3. 시간 진행: 5분 후 (정확히 만료 시점)
        Clock clock5MinLater = Clock.fixed(fixedInstant.plusSeconds(300), ZoneId.systemDefault());
        assertThat(reservation.isExpired(clock5MinLater)).isFalse(); // 정확히 같은 시점은 만료 아님
        
        // 4. 시간 진행: 5분 1초 후 (만료)
        Clock clock5Min1SecLater = Clock.fixed(fixedInstant.plusSeconds(301), ZoneId.systemDefault());
        assertThat(reservation.isExpired(clock5Min1SecLater)).isTrue();
        assertThat(reservation.getRemainingSeconds(clock5Min1SecLater)).isEqualTo(0);
    }
    
    @Test
    @Transactional
    @DisplayName("만료된 예약 일괄 처리: 과거 시간으로 생성된 예약의 자동 만료")
    void testBatchExpiration_WithExpiredTime() {
        // 1. 과거 시간으로 예약 생성 (이미 만료된 상태)
        LocalDateTime pastTime = LocalDateTime.now().minusMinutes(10);
        LocalDateTime expiredTime = pastTime.plusMinutes(5);  // 5분 전에 만료됨
        
        Reservation reservation = Reservation.of(
            null,
            userId,
            seatId,
            concertDateId,
            new BigDecimal("50000"),
            ReservationStatus.PENDING,
            pastTime,
            expiredTime
        );
        
        // DB에 저장
        ReservationEntity entity = new ReservationEntity(
            null,
            reservation.getUserId(),
            reservation.getSeatId(),
            reservation.getConcertDateId(),
            reservation.getPrice(),
            reservation.getStatus().name(),
            reservation.getReservedAt(),
            reservation.getExpiresAt()
        );
        entity = reservationJpaRepository.save(entity);
        
        // 좌석 예약 상태로 변경
        var seat = seatManager.getSeatByIdWithLock(seatId);
        seatManager.reserveSeat(seat);
        
        // 2. 만료된 예약 일괄 처리 (스케줄러가 호출할 메서드)
        int expiredCount = reservationManager.expireReservations();
        
        // 3. 검증
        assertThat(expiredCount).isEqualTo(1);
        
        // 예약 상태 확인
        var expiredReservation = reservationJpaRepository.findById(entity.getId()).orElseThrow();
        assertThat(expiredReservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED.name());
        
        // 좌석 복구 확인
        seat = seatManager.getSeatByIdWithLock(seatId);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }
    
    @Test
    @Transactional
    @DisplayName("복잡한 시나리오: 여러 시간대의 예약 상태 확인")
    void testComplexScenario_MultipleTimeStates() {
        Instant baseInstant = Instant.parse("2024-01-15T10:00:00Z");
        Clock baseClock = Clock.fixed(baseInstant, ZoneId.systemDefault());
        
        // 1. 예약 생성
        Reservation reservation = Reservation.create(userId, seatId, concertDateId, 
                                                     new BigDecimal("50000"), baseClock);
        
        // 시간대별 검증
        Clock[] clocks = {
            Clock.fixed(baseInstant.plusSeconds(0), ZoneId.systemDefault()),    // 생성 직후
            Clock.fixed(baseInstant.plusSeconds(60), ZoneId.systemDefault()),   // 1분 후
            Clock.fixed(baseInstant.plusSeconds(120), ZoneId.systemDefault()),  // 2분 후
            Clock.fixed(baseInstant.plusSeconds(180), ZoneId.systemDefault()),  // 3분 후
            Clock.fixed(baseInstant.plusSeconds(240), ZoneId.systemDefault()),  // 4분 후
            Clock.fixed(baseInstant.plusSeconds(299), ZoneId.systemDefault()),  // 4분 59초 후
            Clock.fixed(baseInstant.plusSeconds(300), ZoneId.systemDefault()),  // 정확히 5분 후
            Clock.fixed(baseInstant.plusSeconds(301), ZoneId.systemDefault())   // 5분 1초 후
        };
        
        long[] expectedRemaining = {300, 240, 180, 120, 60, 1, 0, 0};
        boolean[] expectedExpired = {false, false, false, false, false, false, false, true};
        
        for (int i = 0; i < clocks.length; i++) {
            long remaining = reservation.getRemainingSeconds(clocks[i]);
            boolean expired = reservation.isExpired(clocks[i]);
            
            assertThat(remaining)
                .as("시간 %d초 경과 시 남은 시간", i * 60)
                .isEqualTo(expectedRemaining[i]);
            assertThat(expired)
                .as("시간 %d초 경과 시 만료 여부", i * 60)
                .isEqualTo(expectedExpired[i]);
        }
    }
    
    @Test
    @Transactional
    @DisplayName("확정된 예약은 만료되지 않음 - Clock injection으로 시간 제어")
    void testConfirmedReservation_NeverExpires() {
        Instant fixedInstant = Instant.parse("2024-01-15T10:00:00Z");
        Clock fixedClock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        
        // 1. 예약 생성 - 미래 시간으로 생성하여 현재 기준으로 만료되지 않도록 함
        LocalDateTime futureReservedAt = LocalDateTime.now().minusMinutes(2);  // 2분 전 예약
        LocalDateTime futureExpiresAt = futureReservedAt.plusMinutes(5);  // 아직 3분 남음
        
        Reservation reservation = Reservation.of(
            null,
            userId,
            seatId,
            concertDateId,
            new BigDecimal("50000"),
            ReservationStatus.PENDING,
            futureReservedAt,
            futureExpiresAt
        );
        
        // 생성 직후에는 만료되지 않음 확인 (Clock 사용)
        assertThat(reservation.isExpired(fixedClock)).isFalse();
        assertThat(reservation.isExpired()).isFalse();  // 실제 시간으로도 만료 안됨
        
        // 2. 예약 확정
        reservation.confirm();
        
        // 3. 10분 후에도 만료되지 않음 (CONFIRMED 상태는 isExpired() == false)
        Clock clock10MinLater = Clock.fixed(fixedInstant.plusSeconds(600), ZoneId.systemDefault());
        assertThat(reservation.isExpired(clock10MinLater)).isFalse();
        assertThat(reservation.getRemainingSeconds(clock10MinLater)).isEqualTo(0); // CONFIRMED는 남은 시간 0
        
        // 4. 상태 확인
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }
}
