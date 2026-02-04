package com.example.concert_reservation.api.balance.controller;

import com.example.concert_reservation.api.balance.dto.BalanceResponse;
import com.example.concert_reservation.api.balance.dto.ChargeBalanceRequest;
import com.example.concert_reservation.api.balance.usecase.ChargeBalanceUseCase;
import com.example.concert_reservation.api.balance.usecase.GetBalanceUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/balance")
@Tag(name = "Balance", description = "사용자 잔액 관리 API - 잔액 조회 및 충전")
public class BalanceController {
    
    private final ChargeBalanceUseCase chargeBalanceUseCase;
    private final GetBalanceUseCase getBalanceUseCase;
    
    public BalanceController(
        ChargeBalanceUseCase chargeBalanceUseCase,
        GetBalanceUseCase getBalanceUseCase
    ) {
        this.chargeBalanceUseCase = chargeBalanceUseCase;
        this.getBalanceUseCase = getBalanceUseCase;
    }
    
    @Operation(
        summary = "잔액 조회",
        description = "사용자 ID로 현재 잔액을 조회합니다. 사용자가 존재하지 않으면 자동으로 생성됩니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "잔액 조회 성공",
            content = @Content(schema = @Schema(implementation = BalanceResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (유효하지 않은 사용자 ID)",
            content = @Content
        )
    })
    @GetMapping("/{userId}")
    public ResponseEntity<BalanceResponse> getBalance(
        @Parameter(description = "사용자 ID", example = "user123", required = true)
        @PathVariable String userId
    ) {
        BalanceResponse response = getBalanceUseCase.execute(userId);
        return ResponseEntity.ok(response);
    }
    
    @Operation(
        summary = "잔액 충전",
        description = """
            사용자 계정에 잔액을 충전합니다.
            - 충전 금액은 1,000원 이상이어야 합니다.
            - 충전된 잔액은 즉시 사용 가능합니다.
            - 사용자가 존재하지 않으면 자동으로 생성됩니다.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "잔액 충전 성공",
            content = @Content(schema = @Schema(implementation = BalanceResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (음수 금액, 0원 등)",
            content = @Content
        )
    })
    @PostMapping("/charge")
    public ResponseEntity<BalanceResponse> chargeBalance(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "충전 요청 정보",
            required = true,
            content = @Content(schema = @Schema(implementation = ChargeBalanceRequest.class))
        )
        @Valid @RequestBody ChargeBalanceRequest request
    ) {
        BalanceResponse response = chargeBalanceUseCase.execute(request.userId(), request.amount());
        return ResponseEntity.ok(response);
    }
}

