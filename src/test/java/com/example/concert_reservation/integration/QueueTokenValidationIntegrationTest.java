package com.example.concert_reservation.integration;

import com.example.concert_reservation.domain.concert.infrastructure.ConcertDateJpaRepository;
import com.example.concert_reservation.domain.concert.infrastructure.SeatJpaRepository;
import com.example.concert_reservation.domain.concert.infrastructure.entity.ConcertDateEntity;
import com.example.concert_reservation.domain.concert.infrastructure.entity.SeatEntity;
import com.example.concert_reservation.domain.concert.models.SeatStatus;
import com.example.concert_reservation.domain.queue.infrastructure.QueueJpaRepository;
import com.example.concert_reservation.domain.queue.infrastructure.entity.UserQueueEntity;
import com.example.concert_reservation.domain.queue.models.QueueStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.sql.init.mode=never",
    "spring.main.allow-bean-definition-overriding=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("대기열 토큰 검증 통합 테스트")
class QueueTokenValidationIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private QueueJpaRepository queueJpaRepository;
    
    @Autowired
    private ConcertDateJpaRepository concertDateJpaRepository;
    
    @Autowired
    private SeatJpaRepository seatJpaRepository;
    
    private String activeToken;
    private String waitingToken;
    private String expiredToken;
    private Long seatId;
    
    @BeforeEach
    void setUp() {
        // 데이터 초기화
        queueJpaRepository.deleteAll();
        seatJpaRepository.deleteAll();
        concertDateJpaRepository.deleteAll();
        
        // 콘서트 및 좌석 생성
        ConcertDateEntity concertDate = new ConcertDateEntity(
            null, "테스트 콘서트", LocalDate.now().plusDays(7), 50, 50
        );
        concertDate = concertDateJpaRepository.save(concertDate);
        
        SeatEntity seat = new SeatEntity(null, concertDate.getId(), 1, SeatStatus.AVAILABLE.name(), new BigDecimal("50000"));
        seat = seatJpaRepository.save(seat);
        seatId = seat.getId();
        
        // ACTIVE 토큰
        activeToken = java.util.UUID.randomUUID().toString();
        UserQueueEntity activeQueue = UserQueueEntity.of(
            null,
            activeToken,
            "user_active",
            100L,
            QueueStatus.ACTIVE.name(),
            LocalDateTime.now().minusMinutes(5),
            LocalDateTime.now().plusMinutes(25)
        );
        queueJpaRepository.save(activeQueue);
        
        // WAITING 토큰
        waitingToken = java.util.UUID.randomUUID().toString();
        UserQueueEntity waitingQueue = UserQueueEntity.of(
            null,
            waitingToken,
            "user_waiting",
            200L,
            QueueStatus.WAITING.name(),
            LocalDateTime.now().minusMinutes(10),
            null
        );
        queueJpaRepository.save(waitingQueue);
        
        // EXPIRED 토큰
        expiredToken = java.util.UUID.randomUUID().toString();
        UserQueueEntity expiredQueue = UserQueueEntity.of(
            null,
            expiredToken,
            "user_expired",
            300L,
            QueueStatus.EXPIRED.name(),
            LocalDateTime.now().minusMinutes(40),
            LocalDateTime.now().minusMinutes(10)
        );
        queueJpaRepository.save(expiredQueue);
    }
    
    @Test
    @DisplayName("예약 API - ACTIVE 토큰으로 호출하면 성공한다")
    void reservationAPI_withActiveToken_success() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                .header("X-Queue-Token", activeToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"userId\":\"user_active\",\"seatId\":%d}", seatId)))
                .andExpect(status().isCreated());
    }
    
    @Test
    @DisplayName("예약 API - 토큰 없이 호출하면 401 Unauthorized")
    void reservationAPI_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"userId\":\"user_active\",\"seatId\":%d}", seatId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("대기열 토큰이 필요합니다")));
    }
    
    @Test
    @DisplayName("예약 API - WAITING 토큰으로 호출하면 403 Forbidden")
    void reservationAPI_withWaitingToken_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                .header("X-Queue-Token", waitingToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"userId\":\"user_waiting\",\"seatId\":%d}", seatId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("활성 상태가 아닙니다")));
    }
    
    @Test
    @DisplayName("예약 API - EXPIRED 토큰으로 호출하면 403 Forbidden")
    void reservationAPI_withExpiredToken_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                .header("X-Queue-Token", expiredToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"userId\":\"user_expired\",\"seatId\":%d}", seatId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("활성 상태가 아닙니다")));
    }
    
    @Test
    @DisplayName("예약 API - 유효하지 않은 토큰으로 호출하면 401 Unauthorized")
    void reservationAPI_withInvalidToken_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/reservations")
                .header("X-Queue-Token", "invalid-token-12345")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"userId\":\"user_test\",\"seatId\":%d}", seatId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("유효하지 않은 토큰")));
    }
    
    @Test
    @DisplayName("콘서트 조회 API - 토큰 없이 호출해도 성공한다 (토큰 검증 제외)")
    void concertAPI_withoutToken_success() throws Exception {
        mockMvc.perform(get("/api/v1/concerts/dates"))
                .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("잔액 조회 API - 토큰 없이 호출해도 성공한다 (토큰 검증 제외)")
    void balanceAPI_withoutToken_success() throws Exception {
        mockMvc.perform(get("/api/balance/user123"))
                .andExpect(status().isOk());
    }
    
    @Test
    @DisplayName("대기열 상태 API - 자체 토큰 검증 로직 사용 (Interceptor 제외)")
    void queueStatusAPI_withoutToken_handled() throws Exception {
        // 대기열 상태 API는 컨트롤러에서 직접 토큰 검증
        mockMvc.perform(get("/api/v1/queue/status")
                .header("X-Queue-Token", activeToken))
                .andExpect(status().isOk());
    }
}
