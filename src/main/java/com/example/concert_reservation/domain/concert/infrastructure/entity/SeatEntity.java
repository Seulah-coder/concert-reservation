package com.example.concert_reservation.domain.concert.infrastructure.entity;

import com.example.concert_reservation.support.common.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * 좌석 엔티티
 * JPA 엔티티로 infrastructure 레이어에 위치
 */
@Entity
@Table(name = "seats", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"concert_date_id", "seat_number"}))
public class SeatEntity extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "concert_date_id", nullable = false)
    private Long concertDateId;
    
    @Column(nullable = false)
    private Integer seatNumber;
    
    @Column(nullable = false, length = 20)
    private String status; // SeatStatus enum의 name을 String으로 저장
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    
    // JPA 기본 생성자
    protected SeatEntity() {
    }
    
    // 생성자
    public SeatEntity(Long id, Long concertDateId, Integer seatNumber, 
                      String status, BigDecimal price) {
        this.id = id;
        this.concertDateId = concertDateId;
        this.seatNumber = seatNumber;
        this.status = status;
        this.price = price;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getConcertDateId() {
        return concertDateId;
    }
    
    public void setConcertDateId(Long concertDateId) {
        this.concertDateId = concertDateId;
    }
    
    public Integer getSeatNumber() {
        return seatNumber;
    }
    
    public void setSeatNumber(Integer seatNumber) {
        this.seatNumber = seatNumber;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public BigDecimal getPrice() {
        return price;
    }
    
    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
