package com.example.concert_reservation.api.reservation.controller;

import com.example.concert_reservation.api.reservation.dto.ReserveSeatRequest;
import com.example.concert_reservation.api.reservation.dto.ReservationResponse;
import com.example.concert_reservation.api.reservation.usecase.CancelReservationUseCase;
import com.example.concert_reservation.api.reservation.usecase.ReserveSeatUseCase;
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

@WebMvcTest(ReservationController.class)
@DisplayName("ReservationController 통합 테스트")
class ReservationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private ReserveSeatUseCase reserveSeatUseCase;
    
    @MockBean
    private CancelReservationUseCase cancelReservationUseCase;
    
    @Test
    @DisplayName("POST /api/v1/reservations - 좌석 예약 성공")
    void reserveSeat_success() throws Exception {
        // given
        ReserveSeatRequest request = new ReserveSeatRequest("user123", 1L);
        ReservationResponse response = new ReservationResponse(
            1L, "user123", 1L, 1L, new BigDecimal("50000"),
            "PENDING", LocalDateTime.now(), LocalDateTime.now().plusMinutes(5), 300L
        );
        given(reserveSeatUseCase.execute(any(ReserveSeatRequest.class))).willReturn(response);
        
        // when & then
        mockMvc.perform(post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reservationId").value(1))
            .andExpect(jsonPath("$.userId").value("user123"))
            .andExpect(jsonPath("$.seatId").value(1))
            .andExpect(jsonPath("$.status").value("PENDING"));
        
        verify(reserveSeatUseCase).execute(any(ReserveSeatRequest.class));
    }
    
    @Test
    @DisplayName("POST /api/v1/reservations - 이미 예약된 좌석은 409")
    void reserveSeat_alreadyReserved_returns409() throws Exception {
        // given
        ReserveSeatRequest request = new ReserveSeatRequest("user123", 1L);
        given(reserveSeatUseCase.execute(any(ReserveSeatRequest.class)))
            .willThrow(new IllegalStateException("이미 예약된 좌석입니다"));
        
        // when & then
        mockMvc.perform(post("/api/v1/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("이미 예약된 좌석입니다"));
        
        verify(reserveSeatUseCase).execute(any(ReserveSeatRequest.class));
    }
    
    @Test
    @DisplayName("DELETE /api/v1/reservations/{id} - 예약 취소 성공")
    void cancelReservation_success() throws Exception {
        // given
        Long reservationId = 1L;
        ReservationResponse response = new ReservationResponse(
            1L, "user123", 1L, 1L, new BigDecimal("50000"),
            "CANCELLED", LocalDateTime.now(), LocalDateTime.now().plusMinutes(5), 0L
        );
        given(cancelReservationUseCase.execute(reservationId)).willReturn(response);
        
        // when & then
        mockMvc.perform(delete("/api/v1/reservations/{reservationId}", reservationId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reservationId").value(1))
            .andExpect(jsonPath("$.status").value("CANCELLED"));
        
        verify(cancelReservationUseCase).execute(reservationId);
    }
    
    @Test
    @DisplayName("DELETE /api/v1/reservations/{id} - 존재하지 않는 예약은 400")
    void cancelReservation_notFound_returns400() throws Exception {
        // given
        Long reservationId = 999L;
        given(cancelReservationUseCase.execute(reservationId))
            .willThrow(new IllegalArgumentException("존재하지 않는 예약입니다"));
        
        // when & then
        mockMvc.perform(delete("/api/v1/reservations/{reservationId}", reservationId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("존재하지 않는 예약입니다"));
        
        verify(cancelReservationUseCase).execute(reservationId);
    }
}
