package com.example.concert_reservation.api.concert.controller;

import com.example.concert_reservation.api.concert.dto.AvailableDateResponse;
import com.example.concert_reservation.api.concert.dto.SeatResponse;
import com.example.concert_reservation.api.concert.usecase.GetAvailableDatesUseCase;
import com.example.concert_reservation.api.concert.usecase.GetSeatsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/concerts")
@Tag(name = "Concerts", description = "콘서트 정보 조회 API - 예약 가능한 날짜 및 좌석 조회")
public class ConcertController {
    
    private final GetAvailableDatesUseCase getAvailableDatesUseCase;
    private final GetSeatsUseCase getSeatsUseCase;
    
    public ConcertController(GetAvailableDatesUseCase getAvailableDatesUseCase,
                             GetSeatsUseCase getSeatsUseCase) {
        this.getAvailableDatesUseCase = getAvailableDatesUseCase;
        this.getSeatsUseCase = getSeatsUseCase;
    }
    
    @Operation(
        summary = "예약 가능한 콘서트 날짜 조회",
        description = "현재 예약 가능한 모든 콘서트 날짜를 조회합니다. 좌석이 남아있는 날짜만 반환됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = AvailableDateResponse.class))
        )
    })
    @GetMapping("/dates")
    public ResponseEntity<List<AvailableDateResponse>> getAvailableDates() {
        List<AvailableDateResponse> dates = getAvailableDatesUseCase.execute();
        return ResponseEntity.ok(dates);
    }
    
    @Operation(
        summary = "특정 콘서트 날짜의 좌석 조회",
        description = "선택한 콘서트 날짜의 모든 좌석 정보를 조회합니다. 좌석 번호, 가격, 예약 상태를 확인할 수 있습니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = SeatResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "존재하지 않는 콘서트 날짜",
            content = @Content
        )
    })
    @GetMapping("/{concertDateId}/seats")
    public ResponseEntity<List<SeatResponse>> getSeats(
        @Parameter(description = "콘서트 날짜 ID", example = "1", required = true)
        @PathVariable Long concertDateId
    ) {
        List<SeatResponse> seats = getSeatsUseCase.execute(concertDateId);
        return ResponseEntity.ok(seats);
    }
}
