package com.example.concert_reservation.domain.queue.models;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 사용자 대기열 도메인 모델 (순수 자바 - JPA 의존 없음)
 * 대기열에 등록된 사용자의 상태와 토큰 정보를 관리
 */
public class UserQueue {
    
    private Long id;
    private QueueToken token;
    private String userId;
    private Long queueNumber;
    private QueueStatus status;
    private LocalDateTime enteredAt;
    private LocalDateTime expiredAt;
    
    // 기본 생성자 (프레임워크용)
    protected UserQueue() {
    }
    
    // 전체 필드 생성자
    private UserQueue(Long id, QueueToken token, String userId, Long queueNumber, 
                      QueueStatus status, LocalDateTime enteredAt, LocalDateTime expiredAt) {
        this.id = id;
        this.token = token;
        this.userId = userId;
        this.queueNumber = queueNumber;
        this.status = status;
        this.enteredAt = enteredAt;
        this.expiredAt = expiredAt;
    }
    
    /**
     * 새로운 대기열 생성 (Static Factory Method)
     * @param userId 사용자 ID
     * @param queueNumber 대기 번호
     * @return 생성된 UserQueue (WAITING 상태)
     */
    public static UserQueue create(String userId, Long queueNumber) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
        if (queueNumber == null || queueNumber < 1) {
            throw new IllegalArgumentException("대기 번호는 1 이상이어야 합니다");
        }
        
        return new UserQueue(
            null,
            QueueToken.generate(),
            userId,
            queueNumber,
            QueueStatus.WAITING,
            LocalDateTime.now(),
            null
        );
    }
    
    /**
     * 기존 데이터로부터 UserQueue 생성 (재구성용)
     */
    public static UserQueue of(Long id, QueueToken token, String userId, Long queueNumber,
                               QueueStatus status, LocalDateTime enteredAt, LocalDateTime expiredAt) {
        return new UserQueue(id, token, userId, queueNumber, status, enteredAt, expiredAt);
    }
    
    /**
     * 대기열 활성화 (WAITING → ACTIVE)
     * @param validMinutes 활성 상태 유지 시간(분)
     */
    public void activate(int validMinutes) {
        if (this.status != QueueStatus.WAITING) {
            throw new IllegalStateException("WAITING 상태만 활성화할 수 있습니다. 현재 상태: " + this.status);
        }
        if (validMinutes <= 0) {
            throw new IllegalArgumentException("유효 시간은 0보다 커야 합니다");
        }
        
        this.status = QueueStatus.ACTIVE;
        this.expiredAt = LocalDateTime.now().plusMinutes(validMinutes);
    }
    
    /**
     * 대기열 만료 처리
     */
    public void expire() {
        this.status = QueueStatus.EXPIRED;
        this.expiredAt = LocalDateTime.now();
    }
    
    /**
     * 토큰이 활성 상태인지 확인
     * @return 활성 상태이면 true
     */
    public boolean isActive() {
        return this.status == QueueStatus.ACTIVE && 
               (this.expiredAt == null || LocalDateTime.now().isBefore(this.expiredAt));
    }
    
    /**
     * 토큰이 대기 중인지 확인
     * @return 대기 중이면 true
     */
    public boolean isWaiting() {
        return this.status == QueueStatus.WAITING;
    }
    
    /**
     * 토큰이 만료되었는지 확인
     * @return 만료되었으면 true
     */
    public boolean isExpired() {
        return this.status == QueueStatus.EXPIRED || 
               (this.expiredAt != null && LocalDateTime.now().isAfter(this.expiredAt));
    }
    
    // Getters
    public Long getId() {
        return id;
    }
    
    public QueueToken getToken() {
        return token;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public Long getQueueNumber() {
        return queueNumber;
    }
    
    public QueueStatus getStatus() {
        return status;
    }
    
    public LocalDateTime getEnteredAt() {
        return enteredAt;
    }
    
    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserQueue userQueue = (UserQueue) o;
        return Objects.equals(id, userQueue.id) && 
               Objects.equals(token, userQueue.token);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, token);
    }
}
