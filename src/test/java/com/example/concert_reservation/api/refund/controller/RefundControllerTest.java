package com.example.concert_reservation.api.refund.controller;

import com.example.concert_reservation.api.refund.dto.ProcessRefundRequest;
import com.example.concert_reservation.api.refund.dto.RefundResponse;
import com.example.concert_reservation.api.refund.usecase.ProcessRefundUseCase;
import com.example.concert_reservation.domain.refund.models.RefundStatus;
import com.example.concert_reservation.support.exception.DomainConflictException;
import com.example.concert_reservation.support.exception.DomainForbiddenException;
import com.example.concert_reservation.support.exception.DomainNotFoundException;
import com.example.concert_reservation.support.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RefundControllerTest {

    @Mock
    private ProcessRefundUseCase processRefundUseCase;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
            .standaloneSetup(new RefundController(processRefundUseCase))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    @Test
    void should_return_200_ok_when_refund_processed_successfully() throws Exception {
        // Given
        ProcessRefundRequest request = new ProcessRefundRequest(1L, "user1", "Customer requested");
        RefundResponse response = new RefundResponse(
            1L, 1L, 1L, "user1", BigDecimal.TEN, "reason", RefundStatus.APPROVED, LocalDateTime.now(), LocalDateTime.now()
        );

        when(processRefundUseCase.execute(any(ProcessRefundRequest.class)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/refunds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.paymentId").value(1))
            .andExpect(jsonPath("$.userId").value("user1"));
    }

    @Test
    void should_return_404_when_payment_not_found() throws Exception {
        // Given
        ProcessRefundRequest request = new ProcessRefundRequest(999L, "user1", "reason");

        when(processRefundUseCase.execute(any(ProcessRefundRequest.class)))
            .thenThrow(new DomainNotFoundException("결제를 찾을 수 없습니다"));

        // When & Then
        mockMvc.perform(post("/api/refunds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void should_return_403_when_non_owner_requests_refund() throws Exception {
        // Given
        ProcessRefundRequest request = new ProcessRefundRequest(1L, "user2", "reason");

        when(processRefundUseCase.execute(any(ProcessRefundRequest.class)))
            .thenThrow(new DomainForbiddenException("본인의 결제만 환불할 수 있습니다"));

        // When & Then
        mockMvc.perform(post("/api/refunds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    @Test
    void should_return_409_when_already_refunded() throws Exception {
        // Given
        ProcessRefundRequest request = new ProcessRefundRequest(1L, "user1", "reason");

        when(processRefundUseCase.execute(any(ProcessRefundRequest.class)))
            .thenThrow(new DomainConflictException("이미 환불된 결제입니다"));

        // When & Then
        mockMvc.perform(post("/api/refunds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void should_return_409_when_reservation_not_confirmed() throws Exception {
        // Given
        ProcessRefundRequest request = new ProcessRefundRequest(1L, "user1", "reason");

        when(processRefundUseCase.execute(any(ProcessRefundRequest.class)))
            .thenThrow(new DomainConflictException("확정된 예약만 환불할 수 있습니다"));

        // When & Then
        mockMvc.perform(post("/api/refunds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
    }

    @Test
    void should_return_400_when_validation_fails() throws Exception {
        // Given - missing required fields
        String invalidRequest = "{}";

        // When & Then
        mockMvc.perform(post("/api/refunds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());
    }
}
