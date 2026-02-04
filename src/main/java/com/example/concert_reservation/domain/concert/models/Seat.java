package com.example.concert_reservation.domain.concert.models;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 좌석 도메인 모델 (순수 자바 - JPA 의존 없음)
 * 개별 좌석의 정보와 상태를 관리
 */
public class Seat {
    
    private Long id;
    private Long concertDateId;
    private Integer seatNumber;
    private SeatStatus status;
    private BigDecimal price;
    
    // 기본 생성자
    protected Seat() {
    }
    
    // 전체 필드 생성자
    private Seat(Long id, Long concertDateId, Integer seatNumber, 
                 SeatStatus status, BigDecimal price) {
        this.id = id;
        this.concertDateId = concertDateId;
        this.seatNumber = seatNumber;
        this.status = status;
        this.price = price;
    }
    
    /**
     * 새로운 좌석 생성 (Static Factory Method)
     * @param concertDateId 콘서트 날짜 ID
     * @param seatNumber 좌석 번호
     * @param price 가격
     * @return 생성된 Seat (AVAILABLE 상태)
     */
    public static Seat create(Long concertDateId, Integer seatNumber, BigDecimal price) {
        if (concertDateId == null) {
            throw new IllegalArgumentException("콘서트 날짜 ID는 필수입니다");
        }
        if (seatNumber == null || seatNumber <= 0) {
            throw new IllegalArgumentException("좌석 번호는 1 이상이어야 합니다");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("가격은 0보다 커야 합니다");
        }
        
        return new Seat(null, concertDateId, seatNumber, SeatStatus.AVAILABLE, price);
    }
    
    /**
     * 기존 데이터로부터 Seat 생성 (재구성용)
     */
    public static Seat of(Long id, Long concertDateId, Integer seatNumber,
                         SeatStatus status, BigDecimal price) {
        return new Seat(id, concertDateId, seatNumber, status, price);
    }
    
    /**
     * 좌석 임시 예약 (AVAILABLE → RESERVED)
     */
    public void reserve() {
        if (this.status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException(
                "예약 가능한 좌석만 예약할 수 있습니다. 현재 상태: " + this.status
            );
        }
        this.status = SeatStatus.RESERVED;
    }
    
    /**
     * 좌석 판매 (RESERVED → SOLD)
     */
    public void sell() {
        if (this.status != SeatStatus.RESERVED) {
            throw new IllegalStateException(
                "임시 예약된 좌석만 판매할 수 있습니다. 현재 상태: " + this.status
            );
        }
        this.status = SeatStatus.SOLD;
    }
    
    /**
     * 좌석 해제 (RESERVED → AVAILABLE)
     * 임시 예약이 만료되었을 때 사용
     */
    public void release() {
        if (this.status != SeatStatus.RESERVED) {
            throw new IllegalStateException(
                "임시 예약된 좌석만 해제할 수 있습니다. 현재 상태: " + this.status
            );
        }
        this.status = SeatStatus.AVAILABLE;
    }
    
    /**
     * 예약 가능한 좌석인지 확인
     * @return 예약 가능하면 true
     */
    public boolean isAvailable() {
        return this.status == SeatStatus.AVAILABLE;
    }
    
    /**
     * 임시 예약된 좌석인지 확인
     * @return 임시 예약 상태이면 true
     */
    public boolean isReserved() {
        return this.status == SeatStatus.RESERVED;
    }
    
    /**
     * 판매된 좌석인지 확인
     * @return 판매 완료 상태이면 true
     */
    public boolean isSold() {
        return this.status == SeatStatus.SOLD;
    }
    
    // Getters
    public Long getId() {
        return id;
    }
    
    public Long getConcertDateId() {
        return concertDateId;
    }
    
    public Integer getSeatNumber() {
        return seatNumber;
    }
    
    public SeatStatus getStatus() {
        return status;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Seat seat = (Seat) o;
        return Objects.equals(id, seat.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
