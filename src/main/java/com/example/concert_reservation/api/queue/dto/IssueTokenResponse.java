package com.example.concert_reservation.api.queue.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 대기열 토큰 발급 응답 DTO
 */
public class IssueTokenResponse {
    
    private String token;
    private String userId;
    private Long queueNumber;
    private String status;
    private String estimatedWaitTime; // "X분 Y초" 형식
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime enteredAt;
    
    // 기본 생성자
    public IssueTokenResponse() {
    }
    
    public IssueTokenResponse(String token, String userId, Long queueNumber, String status, 
                             String estimatedWaitTime, LocalDateTime enteredAt) {
        this.token = token;
        this.userId = userId;
        this.queueNumber = queueNumber;
        this.status = status;
        this.estimatedWaitTime = estimatedWaitTime;
        this.enteredAt = enteredAt;
    }
    
    // 이전 버전과의 호환성을 위한 생성자
    public IssueTokenResponse(String token, Long queueNumber, String status, LocalDateTime enteredAt) {
        this.token = token;
        this.queueNumber = queueNumber;
        this.status = status;
        this.enteredAt = enteredAt;
    }
    
    public String getToken() {
        return token;
    }
    
    public void setToken(String token) {
        this.token = token;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Long getQueueNumber() {
        return queueNumber;
    }
    
    public void setQueueNumber(Long queueNumber) {
        this.queueNumber = queueNumber;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getEstimatedWaitTime() {
        return estimatedWaitTime;
    }
    
    public void setEstimatedWaitTime(String estimatedWaitTime) {
        this.estimatedWaitTime = estimatedWaitTime;
    }
    
    public LocalDateTime getEnteredAt() {
        return enteredAt;
    }
    
    public void setEnteredAt(LocalDateTime enteredAt) {
        this.enteredAt = enteredAt;
    }
}
