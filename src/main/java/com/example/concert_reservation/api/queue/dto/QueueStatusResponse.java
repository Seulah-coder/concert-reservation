package com.example.concert_reservation.api.queue.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 대기열 상태 조회 응답 DTO
 */
public class QueueStatusResponse {
    
    private String token;
    private String userId;
    private Long queueNumber;
    private String status;
    private Long waitingAhead;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime enteredAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime expiredAt;
    
    // 기본 생성자
    public QueueStatusResponse() {
    }
    
    public QueueStatusResponse(String token, String userId, Long queueNumber, String status,
                               Long waitingAhead, LocalDateTime enteredAt, LocalDateTime expiredAt) {
        this.token = token;
        this.userId = userId;
        this.queueNumber = queueNumber;
        this.status = status;
        this.waitingAhead = waitingAhead;
        this.enteredAt = enteredAt;
        this.expiredAt = expiredAt;
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
    
    public Long getWaitingAhead() {
        return waitingAhead;
    }
    
    public void setWaitingAhead(Long waitingAhead) {
        this.waitingAhead = waitingAhead;
    }
    
    public LocalDateTime getEnteredAt() {
        return enteredAt;
    }
    
    public void setEnteredAt(LocalDateTime enteredAt) {
        this.enteredAt = enteredAt;
    }
    
    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }
    
    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }
}
