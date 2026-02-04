package com.example.concert_reservation.api.queue.controller;

import com.example.concert_reservation.api.queue.dto.IssueTokenRequest;
import com.example.concert_reservation.api.queue.dto.IssueTokenResponse;
import com.example.concert_reservation.api.queue.dto.QueueStatusResponse;
import com.example.concert_reservation.api.queue.usecase.GetQueueStatusUseCase;
import com.example.concert_reservation.api.queue.usecase.IssueQueueTokenUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 대기열 토큰 관리 REST API
 * 
 * API 목록:
 * - POST /api/v1/queue/token : 토큰 발급
 * - GET /api/v1/queue/status : 대기 상태 조회 (폴링용)
 */
@RestController
@RequestMapping("/api/v1/queue")
public class QueueTokenController {
    
    private final IssueQueueTokenUseCase issueQueueTokenUseCase;
    private final GetQueueStatusUseCase getQueueStatusUseCase;
    
    public QueueTokenController(IssueQueueTokenUseCase issueQueueTokenUseCase,
                                GetQueueStatusUseCase getQueueStatusUseCase) {
        this.issueQueueTokenUseCase = issueQueueTokenUseCase;
        this.getQueueStatusUseCase = getQueueStatusUseCase;
    }
    
    /**
     * 대기열 토큰 발급
     * 
     * @param request 토큰 발급 요청 (userId 필수)
     * @return 201 Created - 발급된 토큰 정보
     */
    @PostMapping("/token")
    public ResponseEntity<IssueTokenResponse> issueToken(@Valid @RequestBody IssueTokenRequest request) {
        IssueTokenResponse response = issueQueueTokenUseCase.execute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * 대기열 상태 조회 (폴링용)
     * 
     * @param token 조회할 토큰 값 (헤더에서 전달)
     * @return 200 OK - 대기열 상태 정보
     */
    @GetMapping("/status")
    public ResponseEntity<QueueStatusResponse> getQueueStatus(
            @RequestHeader("X-Queue-Token") String token) {
        QueueStatusResponse response = getQueueStatusUseCase.execute(token);
        return ResponseEntity.ok(response);
    }
}
