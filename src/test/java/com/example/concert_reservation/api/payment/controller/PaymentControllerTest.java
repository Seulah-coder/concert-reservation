package com.example.concert_reservation.api.payment.controller;

import com.example.concert_reservation.api.payment.dto.PaymentResponse;
import com.example.concert_reservation.api.payment.dto.ProcessPaymentRequest;
import com.example.concert_reservation.api.payment.usecase.ProcessPaymentUseCase;
import com.example.concert_reservation.domain.payment.models.PaymentStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.BDDMockito.*;
import static org.springframework.http.MediaType.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@DisplayName("결제 API 컨트롤러 테스트")
class PaymentControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockitoBean
    private ProcessPaymentUseCase processPaymentUseCase;
    
    @Test
    @DisplayName("결제 처리 성공 - POST /api/payments")
    void processPayment_success() throws Exception {
        // given
        Long reservationId = 1L;
        String userId = "user123";
        BigDecimal amount = new BigDecimal("50000");
        
        PaymentResponse expectedResponse = new PaymentResponse(
            1L, reservationId, userId, amount,
            PaymentStatus.COMPLETED,
            LocalDateTime.now()
        );
        
        given(processPaymentUseCase.execute(reservationId, userId))
            .willReturn(expectedResponse);
        
        ProcessPaymentRequest request = new ProcessPaymentRequest(reservationId, userId);
        
        // when & then
        mockMvc.perform(post("/api/payments")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.reservationId").value(reservationId))
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.amount").value(50000))
            .andExpect(jsonPath("$.status").value("COMPLETED"));
        
        verify(processPaymentUseCase).execute(reservationId, userId);
    }
    
    @Test
    @DisplayName("예약 ID 없으면 요청 실패")
    void processPayment_noReservationId_badRequest() throws Exception {
        // given
        String request = """
            {"userId": "user123"}
            """;
        
        // when & then
        mockMvc.perform(post("/api/payments")
                .contentType(APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("사용자 ID 없으면 요청 실패")
    void processPayment_noUserId_badRequest() throws Exception {
        // given
        String request = """
            {"reservationId": 1}
            """;
        
        // when & then
        mockMvc.perform(post("/api/payments")
                .contentType(APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("예약이 없으면 400 에러")
    void processPayment_reservationNotFound_throwsException() throws Exception {
        // given
        Long reservationId = 999L;
        String userId = "user123";
        
        given(processPaymentUseCase.execute(reservationId, userId))
            .willThrow(new IllegalArgumentException("예약을 찾을 수 없습니다"));
        
        ProcessPaymentRequest request = new ProcessPaymentRequest(reservationId, userId);
        
        // when & then
        mockMvc.perform(post("/api/payments")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("다른 사용자의 예약은 결제할 수 없다")
    void processPayment_differentUser_unauthorized() throws Exception {
        // given
        Long reservationId = 1L;
        String userId = "hacker";
        
        given(processPaymentUseCase.execute(reservationId, userId))
            .willThrow(new IllegalArgumentException("본인의 예약만 결제할 수 있습니다"));
        
        ProcessPaymentRequest request = new ProcessPaymentRequest(reservationId, userId);
        
        // when & then
        mockMvc.perform(post("/api/payments")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }
    
    @Test
    @DisplayName("잔액이 부족하면 결제할 수 없다")
    void processPayment_insufficientBalance_paymentFailed() throws Exception {
        // given
        Long reservationId = 1L;
        String userId = "user123";
        
        given(processPaymentUseCase.execute(reservationId, userId))
            .willThrow(new IllegalStateException("잔액이 부족합니다"));
        
        ProcessPaymentRequest request = new ProcessPaymentRequest(reservationId, userId);
        
        // when & then
        mockMvc.perform(post("/api/payments")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }
}
