package com.example.concert_reservation.api.concert.controller;

import com.example.concert_reservation.api.concert.dto.AvailableDateResponse;
import com.example.concert_reservation.api.concert.dto.SeatResponse;
import com.example.concert_reservation.api.concert.usecase.GetAvailableDatesUseCase;
import com.example.concert_reservation.api.concert.usecase.GetSeatsUseCase;
import com.example.concert_reservation.config.QueueTokenInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConcertController.class)
@DisplayName("ConcertController 통합 테스트")
class ConcertControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private GetAvailableDatesUseCase getAvailableDatesUseCase;
    
    @MockBean
    private GetSeatsUseCase getSeatsUseCase;
    
    @MockBean
    private QueueTokenInterceptor queueTokenInterceptor;
    
    @Test
    @DisplayName("GET /api/v1/concerts/dates - 예약 가능한 콘서트 날짜 조회 성공")
    void getAvailableDates_success() throws Exception {
        // given
        List<AvailableDateResponse> responses = List.of(
            new AvailableDateResponse(1L, "아이유 콘서트", LocalDate.of(2024, 12, 31), 50, 30),
            new AvailableDateResponse(2L, "BTS 콘서트", LocalDate.of(2024, 12, 25), 50, 20)
        );
        given(getAvailableDatesUseCase.execute()).willReturn(responses);
        
        // when & then
        mockMvc.perform(get("/api/v1/concerts/dates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].concertDateId").value(1))
            .andExpect(jsonPath("$[0].concertName").value("아이유 콘서트"))
            .andExpect(jsonPath("$[0].availableSeats").value(30))
            .andExpect(jsonPath("$[1].concertDateId").value(2))
            .andExpect(jsonPath("$[1].concertName").value("BTS 콘서트"));
        
        verify(getAvailableDatesUseCase).execute();
    }
    
    @Test
    @DisplayName("GET /api/v1/concerts/dates - 예약 가능한 콘서트가 없으면 빈 배열 반환")
    void getAvailableDates_empty() throws Exception {
        // given
        given(getAvailableDatesUseCase.execute()).willReturn(List.of());
        
        // when & then
        mockMvc.perform(get("/api/v1/concerts/dates"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
        
        verify(getAvailableDatesUseCase).execute();
    }
    
    @Test
    @DisplayName("GET /api/v1/concerts/{concertDateId}/seats - 좌석 조회 성공")
    void getSeats_success() throws Exception {
        // given
        Long concertDateId = 1L;
        List<SeatResponse> responses = List.of(
            new SeatResponse(1L, 1, "AVAILABLE", new BigDecimal("50000")),
            new SeatResponse(2L, 2, "RESERVED", new BigDecimal("50000")),
            new SeatResponse(3L, 3, "SOLD", new BigDecimal("50000"))
        );
        given(getSeatsUseCase.execute(concertDateId)).willReturn(responses);
        
        // when & then
        mockMvc.perform(get("/api/v1/concerts/{concertDateId}/seats", concertDateId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].seatId").value(1))
            .andExpect(jsonPath("$[0].seatNumber").value(1))
            .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
            .andExpect(jsonPath("$[0].price").value(50000))
            .andExpect(jsonPath("$[1].status").value("RESERVED"))
            .andExpect(jsonPath("$[2].status").value("SOLD"));
        
        verify(getSeatsUseCase).execute(concertDateId);
    }
    
    @Test
    @DisplayName("GET /api/v1/concerts/{concertDateId}/seats - 존재하지 않는 콘서트 조회 시 400")
    void getSeats_concertNotFound_returns400() throws Exception {
        // given
        Long concertDateId = 999L;
        given(getSeatsUseCase.execute(concertDateId))
            .willThrow(new IllegalArgumentException("존재하지 않는 콘서트입니다"));
        
        // when & then
        mockMvc.perform(get("/api/v1/concerts/{concertDateId}/seats", concertDateId))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("존재하지 않는 콘서트입니다"));
        
        verify(getSeatsUseCase).execute(concertDateId);
    }
}
