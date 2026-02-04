package com.example.concert_reservation.api.balance.controller;

import com.example.concert_reservation.api.balance.dto.BalanceResponse;
import com.example.concert_reservation.api.balance.dto.ChargeBalanceRequest;
import com.example.concert_reservation.api.balance.usecase.ChargeBalanceUseCase;
import com.example.concert_reservation.api.balance.usecase.GetBalanceUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/balance")
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
    
    /**
     * 잔액 조회 API
     * GET /api/balance/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String userId) {
        BalanceResponse response = getBalanceUseCase.execute(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 잔액 충전 API
     * POST /api/balance/charge
     */
    @PostMapping("/charge")
    public ResponseEntity<BalanceResponse> chargeBalance(@Valid @RequestBody ChargeBalanceRequest request) {
        BalanceResponse response = chargeBalanceUseCase.execute(request.userId(), request.amount());
        return ResponseEntity.ok(response);
    }
}
