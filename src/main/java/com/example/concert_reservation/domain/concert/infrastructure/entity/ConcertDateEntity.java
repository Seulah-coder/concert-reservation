package com.example.concert_reservation.domain.concert.infrastructure.entity;

import com.example.concert_reservation.support.common.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDate;

/**
 * 콘서트 날짜 엔티티
 * JPA 엔티티로 infrastructure 레이어에 위치
 */
@Entity
@Table(name = "concert_dates")
public class ConcertDateEntity extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String concertName;
    
    @Column(nullable = false, unique = true)
    private LocalDate concertDate;
    
    @Column(nullable = false)
    private Integer totalSeats;
    
    @Column(nullable = false)
    private Integer availableSeats;
    
    // JPA 기본 생성자
    protected ConcertDateEntity() {
    }
    
    // 생성자
    public ConcertDateEntity(Long id, String concertName, LocalDate concertDate, 
                             Integer totalSeats, Integer availableSeats) {
        this.id = id;
        this.concertName = concertName;
        this.concertDate = concertDate;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getConcertName() {
        return concertName;
    }
    
    public void setConcertName(String concertName) {
        this.concertName = concertName;
    }
    
    public LocalDate getConcertDate() {
        return concertDate;
    }
    
    public void setConcertDate(LocalDate concertDate) {
        this.concertDate = concertDate;
    }
    
    public Integer getTotalSeats() {
        return totalSeats;
    }
    
    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }
    
    public Integer getAvailableSeats() {
        return availableSeats;
    }
    
    public void setAvailableSeats(Integer availableSeats) {
        this.availableSeats = availableSeats;
    }
}
