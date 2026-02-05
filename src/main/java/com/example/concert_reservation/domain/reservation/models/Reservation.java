package com.example.concert_reservation.domain.reservation.models;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 예약 도메인 모델 (순수 자바 - JPA 의존 없음)
 * 콘서트 좌석 예약 정보와 5분 타임아웃 로직을 관리
 */
public class Reservation {
    
    private static final int TIMEOUT_MINUTES = 5;
    
    private Long id;
    private String userId;
    private Long seatId;
    private Long concertDateId;
    private BigDecimal price;
    private ReservationStatus status;
    private LocalDateTime reservedAt;
    private LocalDateTime expiresAt;
    
    // 기본 생성자
    protected Reservation() {
    }
    
    // 전체 필드 생성자
    private Reservation(Long id, String userId, Long seatId, Long concertDateId,
                        BigDecimal price, ReservationStatus status,
                        LocalDateTime reservedAt, LocalDateTime expiresAt) {
        this.id = id;
        this.userId = userId;
        this.seatId = seatId;
        this.concertDateId = concertDateId;
        this.price = price;
        this.status = status;
        this.reservedAt = reservedAt;
        this.expiresAt = expiresAt;
    }
    
    /**
     * 새로운 예약 생성 (Static Factory Method)
     * @param userId 사용자 ID
     * @param seatId 좌석 ID
     * @param concertDateId 콘서트 날짜 ID
     * @param price 가격
     * @return 생성된 Reservation (PENDING 상태, 5분 후 만료)
     */
    public static Reservation create(String userId, Long seatId, Long concertDateId, BigDecimal price) {
        return create(userId, seatId, concertDateId, price, Clock.systemDefaultZone());
    }
    
    /**
     * 새로운 예약 생성 with Clock injection (테스트용)
     * @param userId 사용자 ID
     * @param seatId 좌석 ID
     * @param concertDateId 콘서트 날짜 ID
     * @param price 가격
     * @param clock 시간 제어용 Clock
     * @return 생성된 Reservation (PENDING 상태, 5분 후 만료)
     */
    public static Reservation create(String userId, Long seatId, Long concertDateId, BigDecimal price, Clock clock) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }
        if (seatId == null) {
            throw new IllegalArgumentException("좌석 ID는 필수입니다");
        }
        if (concertDateId == null) {
            throw new IllegalArgumentException("콘서트 날짜 ID는 필수입니다");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("가격은 0보다 커야 합니다");
        }
        
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime expiresAt = now.plusMinutes(TIMEOUT_MINUTES);
        
        return new Reservation(null, userId, seatId, concertDateId, price,
                             ReservationStatus.PENDING, now, expiresAt);
    }
    
    /**
     * 기존 데이터로부터 Reservation 생성 (재구성용)
     */
    public static Reservation of(Long id, String userId, Long seatId, Long concertDateId,
                                 BigDecimal price, ReservationStatus status,
                                 LocalDateTime reservedAt, LocalDateTime expiresAt) {
        return new Reservation(id, userId, seatId, concertDateId, price,
                             status, reservedAt, expiresAt);
    }
    
    /**
     * 예약 확정 (결제 완료 시)
     * PENDING → CONFIRMED
     */
    public void confirm() {
        if (this.status != ReservationStatus.PENDING) {
            throw new IllegalStateException(
                "임시 예약 상태만 확정할 수 있습니다. 현재 상태: " + this.status
            );
        }
        if (isExpired()) {
            throw new IllegalStateException("만료된 예약은 확정할 수 없습니다");
        }
        this.status = ReservationStatus.CONFIRMED;
    }
    
    /**
     * 예약 취소
     * PENDING → CANCELLED (일반 취소)
     * CONFIRMED → CANCELLED (환불로 인한 취소)
     */
    public void cancel() {
        if (this.status != ReservationStatus.PENDING && this.status != ReservationStatus.CONFIRMED) {
            throw new IllegalStateException(
                "임시 예약 또는 확정된 예약만 취소할 수 있습니다. 현재 상태: " + this.status
            );
        }
        this.status = ReservationStatus.CANCELLED;
    }
    
    /**
     * 예약 만료 처리
     * PENDING → EXPIRED
     */
    public void expire() {
        if (this.status != ReservationStatus.PENDING) {
            throw new IllegalStateException(
                "임시 예약 상태만 만료 처리할 수 있습니다. 현재 상태: " + this.status
            );
        }
        this.status = ReservationStatus.EXPIRED;
    }
    
    /**
     * 예약이 만료되었는지 확인
     * @return 현재 시간이 만료 시간을 초과했으면 true
     */
    public boolean isExpired() {
        return isExpired(Clock.systemDefaultZone());
    }
    
    /**
     * 예약이 만료되었는지 확인 with Clock injection
     * @param clock 시간 제어용 Clock
     * @return 현재 시간이 만료 시간을 초과했으면 true
     */
    public boolean isExpired(Clock clock) {
        if (this.status != ReservationStatus.PENDING) {
            return false;
        }
        return LocalDateTime.now(clock).isAfter(this.expiresAt);
    }
    
    /**
     * 예약이 활성 상태인지 확인
     * @return PENDING 또는 CONFIRMED 상태이면 true
     */
    public boolean isActive() {
        return this.status.isActive();
    }
    
    /**
     * 남은 시간(초) 계산
     * @return 만료까지 남은 초 (만료되었거나 PENDING이 아니면 0)
     */
    public long getRemainingSeconds() {
        return getRemainingSeconds(Clock.systemDefaultZone());
    }
    
    /**
     * 남은 시간(초) 계산 with Clock injection
     * @param clock 시간 제어용 Clock
     * @return 만료까지 남은 초 (만료되었거나 PENDING이 아니면 0)
     */
    public long getRemainingSeconds(Clock clock) {
        if (this.status != ReservationStatus.PENDING || isExpired(clock)) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(clock), this.expiresAt).getSeconds();
    }
    
    // Getters
    public Long getId() {
        return id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public Long getSeatId() {
        return seatId;
    }
    
    public Long getConcertDateId() {
        return concertDateId;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public ReservationStatus getStatus() {
        return status;
    }
    
    public LocalDateTime getReservedAt() {
        return reservedAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Reservation that = (Reservation) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
