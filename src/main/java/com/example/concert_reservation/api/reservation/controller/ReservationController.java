package com.example.concert_reservation.api.reservation.controller;

import com.example.concert_reservation.api.reservation.dto.ReserveSeatRequest;
import com.example.concert_reservation.api.reservation.dto.ReservationResponse;
import com.example.concert_reservation.api.reservation.usecase.CancelReservationUseCase;
import com.example.concert_reservation.api.reservation.usecase.ReserveSeatUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reservations")
@Tag(name = "Reservations", description = "좌석 예약 관리 API - 예약 생성 및 취소")
public class ReservationController {
    
    private final ReserveSeatUseCase reserveSeatUseCase;
    private final CancelReservationUseCase cancelReservationUseCase;
    
    public ReservationController(ReserveSeatUseCase reserveSeatUseCase,
                                 CancelReservationUseCase cancelReservationUseCase) {
        this.reserveSeatUseCase = reserveSeatUseCase;
        this.cancelReservationUseCase = cancelReservationUseCase;
    }
    
    @Operation(
        summary = "좌석 예약",
        description = """
            선택한 좌석을 임시 예약합니다.
            - 예약은 5분간 유효하며, 그 안에 결제를 완료해야 합니다.
            - 이미 예약된 좌석은 예약할 수 없습니다.
            - 비관적 락으로 동시성 제어를 보장합니다.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "예약 생성 성공",
            content = @Content(schema = @Schema(implementation = ReservationResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (이미 예약된 좌석, 유효하지 않은 좌석 등)",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "좌석을 찾을 수 없음",
            content = @Content
        )
    })
    @PostMapping
    public ResponseEntity<ReservationResponse> reserveSeat(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "예약 요청 정보",
            required = true,
            content = @Content(schema = @Schema(implementation = ReserveSeatRequest.class))
        )
        @RequestBody ReserveSeatRequest request
    ) {
        ReservationResponse response = reserveSeatUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @Operation(
        summary = "예약 취소",
        description = """
            예약을 취소하고 좌석을 다시 예약 가능한 상태로 변경합니다.
            - 이미 결제 완료된 예약은 취소할 수 없습니다 (환불 API 사용).
            - 취소된 예약은 복구할 수 없습니다.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "예약 취소 성공",
            content = @Content(schema = @Schema(implementation = ReservationResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "예약을 찾을 수 없음",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "400",
            description = "이미 결제 완료된 예약은 취소 불가",
            content = @Content
        )
    })
    @DeleteMapping("/{reservationId}")
    public ResponseEntity<ReservationResponse> cancelReservation(
        @Parameter(description = "예약 ID", example = "1", required = true)
        @PathVariable Long reservationId
    ) {
        ReservationResponse response = cancelReservationUseCase.execute(reservationId);
        return ResponseEntity.ok(response);
    }
}
