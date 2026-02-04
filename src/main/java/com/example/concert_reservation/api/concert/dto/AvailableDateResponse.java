package com.example.concert_reservation.api.concert.dto;

import java.time.LocalDate;

/**
 * 예약 가능한 콘서트 날짜 응답 DTO
 */
public class AvailableDateResponse {
    
    private Long concertDateId;
    private String concertName;
    private LocalDate concertDate;
    private Integer totalSeats;
    private Integer availableSeats;
    
    // 기본 생성자
    public AvailableDateResponse() {
    }
    
    // 전체 필드 생성자
    public AvailableDateResponse(Long concertDateId, String concertName, LocalDate concertDate,
                                 Integer totalSeats, Integer availableSeats) {
        this.concertDateId = concertDateId;
        this.concertName = concertName;
        this.concertDate = concertDate;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
    }
    
    // Getters and Setters
    public Long getConcertDateId() {
        return concertDateId;
    }
    
    public void setConcertDateId(Long concertDateId) {
        this.concertDateId = concertDateId;
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
