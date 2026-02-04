package com.example.concert_reservation.api.queue.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 대기열 토큰 발급 요청 DTO
 */
public class IssueTokenRequest {
    
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;
    
    // 기본 생성자 (Jackson용)
    public IssueTokenRequest() {
    }
    
    public IssueTokenRequest(String userId) {
        this.userId = userId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
}
