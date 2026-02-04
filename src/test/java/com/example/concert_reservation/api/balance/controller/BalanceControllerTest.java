package com.example.concert_reservation.api.balance.controller;

import com.example.concert_reservation.api.balance.dto.BalanceResponse;
import com.example.concert_reservation.api.balance.dto.ChargeBalanceRequest;
import com.example.concert_reservation.api.balance.usecase.ChargeBalanceUseCase;
import com.example.concert_reservation.api.balance.usecase.GetBalanceUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BalanceController.class)
@DisplayName("BalanceController 테스트")
class BalanceControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private ChargeBalanceUseCase chargeBalanceUseCase;
    
    @MockBean
    private GetBalanceUseCase getBalanceUseCase;
    
    @Test
    @DisplayName("GET /api/balance/{userId} - 잔액 조회 성공")
    void getBalance_success() throws Exception {
        // given
        String userId = "user123";
        BalanceResponse response = new BalanceResponse(
            1L,
            userId,
            new BigDecimal("50000"),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        
        given(getBalanceUseCase.execute(userId)).willReturn(response);
        
        // when & then
        mockMvc.perform(get("/api/balance/{userId}", userId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value(userId))
            .andExpect(jsonPath("$.amount").value(50000));
        
        verify(getBalanceUseCase).execute(userId);
    }
    
    @Test
    @DisplayName("POST /api/balance/charge - 잔액 충전 성공")
    void chargeBalance_success() throws Exception {
        // given
        ChargeBalanceRequest request = new ChargeBalanceRequest("user123", new BigDecimal("30000"));
        BalanceResponse response = new BalanceResponse(
            1L,
            "user123",
            new BigDecimal("30000"),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
        
        given(chargeBalanceUseCase.execute("user123", new BigDecimal("30000"))).willReturn(response);
        
        // when & then
        mockMvc.perform(post("/api/balance/charge")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userId").value("user123"))
            .andExpect(jsonPath("$.amount").value(30000));
        
        verify(chargeBalanceUseCase).execute("user123", new BigDecimal("30000"));
    }
}
