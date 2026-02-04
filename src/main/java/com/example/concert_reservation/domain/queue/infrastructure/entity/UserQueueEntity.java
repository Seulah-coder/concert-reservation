package com.example.concert_reservation.domain.queue.infrastructure.entity;

import com.example.concert_reservation.support.common.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 사용자 대기열 JPA 엔티티
 * DB 테이블 매핑을 담당하며, 도메인 모델(UserQueue)과 분리됨
 */
@Entity
@Table(name = "user_queue", indexes = {
    @Index(name = "idx_token", columnList = "token"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status")
})
public class UserQueueEntity extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 100)
    private String token;
    
    @Column(name = "user_id", nullable = false, length = 100)
    private String userId;
    
    @Column(name = "queue_number", nullable = false)
    private Long queueNumber;
    
    @Column(nullable = false, length = 20)
    private String status;
    
    @Column(name = "entered_at", nullable = false)
    private LocalDateTime enteredAt;
    
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;
    
    // JPA용 기본 생성자
    protected UserQueueEntity() {
    }
    
    // 전체 필드 생성자
    private UserQueueEntity(Long id, String token, String userId, Long queueNumber,
                           String status, LocalDateTime enteredAt, LocalDateTime expiredAt) {
        this.id = id;
        this.token = token;
        this.userId = userId;
        this.queueNumber = queueNumber;
        this.status = status;
        this.enteredAt = enteredAt;
        this.expiredAt = expiredAt;
    }
    
    /**
     * 빌더 패턴을 위한 정적 팩토리 메서드
     */
    public static UserQueueEntity of(Long id, String token, String userId, Long queueNumber,
                                     String status, LocalDateTime enteredAt, LocalDateTime expiredAt) {
        return new UserQueueEntity(id, token, userId, queueNumber, status, enteredAt, expiredAt);
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
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
