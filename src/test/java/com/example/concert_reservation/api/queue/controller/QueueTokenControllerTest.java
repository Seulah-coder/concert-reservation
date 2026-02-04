package com.example.concert_reservation.api.queue.controller;

import com.example.concert_reservation.api.queue.dto.IssueTokenRequest;
import com.example.concert_reservation.api.queue.dto.IssueTokenResponse;
import com.example.concert_reservation.api.queue.dto.QueueStatusResponse;
import com.example.concert_reservation.domain.queue.models.QueueStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * QueueTokenController 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("대기열 토큰 API 통합 테스트")
class QueueTokenControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("POST /api/v1/queue/token - 새로운 사용자는 토큰을 발급받을 수 있다")
    void issueToken_newUser_success() throws Exception {
        // given
        IssueTokenRequest request = new IssueTokenRequest("user123");
        
        // when & then
        mockMvc.perform(post("/api/v1/queue/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.token").exists())
            .andExpect(jsonPath("$.queueNumber").value(1))
            .andExpect(jsonPath("$.status").value(QueueStatus.WAITING.name()))
            .andExpect(jsonPath("$.enteredAt").exists());
    }
    
    @Test
    @DisplayName("POST /api/v1/queue/token - userId가 없으면 400 에러가 발생한다")
    void issueToken_missingUserId_badRequest() throws Exception {
        // given
        IssueTokenRequest request = new IssueTokenRequest("");
        
        // when & then
        mockMvc.perform(post("/api/v1/queue/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("POST /api/v1/queue/token - 이미 토큰이 있는 사용자는 중복 발급이 불가하다")
    void issueToken_duplicateUser_conflict() throws Exception {
        // given
        IssueTokenRequest request = new IssueTokenRequest("user123");
        
        // 첫 번째 발급
        mockMvc.perform(post("/api/v1/queue/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
        
        // when & then - 두 번째 발급 시도
        mockMvc.perform(post("/api/v1/queue/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().is5xxServerError());  // IllegalStateException
    }
    
    @Test
    @DisplayName("GET /api/v1/queue/status - 토큰으로 대기열 상태를 조회할 수 있다")
    void getQueueStatus_validToken_success() throws Exception {
        // given - 토큰 발급
        IssueTokenRequest issueRequest = new IssueTokenRequest("user123");
        MvcResult issueResult = mockMvc.perform(post("/api/v1/queue/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(issueRequest)))
            .andExpect(status().isCreated())
            .andReturn();
        
        IssueTokenResponse issueResponse = objectMapper.readValue(
            issueResult.getResponse().getContentAsString(),
            IssueTokenResponse.class
        );
        String token = issueResponse.getToken();
        
        // when & then - 상태 조회
        mockMvc.perform(get("/api/v1/queue/status")
                .header("X-Queue-Token", token))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value(token))
            .andExpect(jsonPath("$.userId").value("user123"))
            .andExpect(jsonPath("$.queueNumber").value(1))
            .andExpect(jsonPath("$.status").value(QueueStatus.WAITING.name()))
            .andExpect(jsonPath("$.waitingAhead").value(0))
            .andExpect(jsonPath("$.enteredAt").exists());
    }
    
    @Test
    @DisplayName("GET /api/v1/queue/status - 유효하지 않은 토큰은 에러가 발생한다")
    void getQueueStatus_invalidToken_notFound() throws Exception {
        // given
        String invalidToken = "invalid-token-12345";
        
        // when & then
        mockMvc.perform(get("/api/v1/queue/status")
                .header("X-Queue-Token", invalidToken))
            .andDo(print())
            .andExpect(status().is5xxServerError());  // IllegalArgumentException
    }
    
    @Test
    @DisplayName("GET /api/v1/queue/status - 여러 사용자의 대기 순서가 올바르게 표시된다")
    void getQueueStatus_multipleUsers_correctWaitingAhead() throws Exception {
        // given - 3명의 사용자 토큰 발급
        String token1 = issueTokenForUser("user1");
        String token2 = issueTokenForUser("user2");
        String token3 = issueTokenForUser("user3");
        
        // when & then - user3는 앞에 2명이 대기 중
        mockMvc.perform(get("/api/v1/queue/status")
                .header("X-Queue-Token", token3))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.queueNumber").value(3))
            .andExpect(jsonPath("$.waitingAhead").value(2));
        
        // user1은 앞에 0명
        mockMvc.perform(get("/api/v1/queue/status")
                .header("X-Queue-Token", token1))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.queueNumber").value(1))
            .andExpect(jsonPath("$.waitingAhead").value(0));
    }
    
    @Test
    @DisplayName("E2E 시나리오 - 토큰 발급부터 상태 조회까지")
    void e2e_issueAndCheckStatus() throws Exception {
        // 1. 토큰 발급
        IssueTokenRequest request = new IssueTokenRequest("e2e-user");
        MvcResult issueResult = mockMvc.perform(post("/api/v1/queue/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();
        
        IssueTokenResponse issueResponse = objectMapper.readValue(
            issueResult.getResponse().getContentAsString(),
            IssueTokenResponse.class
        );
        
        assertThat(issueResponse.getToken()).isNotNull();
        assertThat(issueResponse.getStatus()).isEqualTo(QueueStatus.WAITING.name());
        
        // 2. 상태 조회
        MvcResult statusResult = mockMvc.perform(get("/api/v1/queue/status")
                .header("X-Queue-Token", issueResponse.getToken()))
            .andExpect(status().isOk())
            .andReturn();
        
        QueueStatusResponse statusResponse = objectMapper.readValue(
            statusResult.getResponse().getContentAsString(),
            QueueStatusResponse.class
        );
        
        assertThat(statusResponse.getToken()).isEqualTo(issueResponse.getToken());
        assertThat(statusResponse.getUserId()).isEqualTo("e2e-user");
        assertThat(statusResponse.getStatus()).isEqualTo(QueueStatus.WAITING.name());
    }
    
    // 헬퍼 메서드: 사용자 토큰 발급
    private String issueTokenForUser(String userId) throws Exception {
        IssueTokenRequest request = new IssueTokenRequest(userId);
        MvcResult result = mockMvc.perform(post("/api/v1/queue/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();
        
        IssueTokenResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            IssueTokenResponse.class
        );
        return response.getToken();
    }
}
