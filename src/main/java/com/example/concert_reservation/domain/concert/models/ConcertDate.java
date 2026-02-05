package com.example.concert_reservation.domain.concert.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 콘서트 날짜 도메인 모델 (순수 자바 - JPA 의존 없음)
 * 특정 날짜의 콘서트 정보와 좌석 가용성을 관리
 */
public class ConcertDate {
    
    private Long id;
    private String concertName;
    private LocalDate concertDate;
    private Integer totalSeats;
    private Integer availableSeats;
    
    // 기본 생성자
    protected ConcertDate() {
    }
    
    // 전체 필드 생성자
    private ConcertDate(Long id, String concertName, LocalDate concertDate, 
                        Integer totalSeats, Integer availableSeats) {
        this.id = id;
        this.concertName = concertName;
        this.concertDate = concertDate;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
    }
    
    /**
     * 새로운 콘서트 날짜 생성 (Static Factory Method)
     * @param concertName 콘서트 이름
     * @param concertDate 콘서트 날짜
     * @param totalSeats 총 좌석 수
     * @return 생성된 ConcertDate
     */
    public static ConcertDate create(String concertName, LocalDate concertDate, Integer totalSeats) {
        if (concertName == null || concertName.trim().isEmpty()) {
            throw new IllegalArgumentException("콘서트 이름은 필수입니다");
        }
        if (concertDate == null) {
            throw new IllegalArgumentException("콘서트 날짜는 필수입니다");
        }
        if (totalSeats == null || totalSeats <= 0) {
            throw new IllegalArgumentException("총 좌석 수는 1 이상이어야 합니다");
        }
        
        return new ConcertDate(null, concertName, concertDate, totalSeats, totalSeats);
    }
    
    /**
     * 기존 데이터로부터 ConcertDate 생성 (재구성용)
     */
    public static ConcertDate of(Long id, String concertName, LocalDate concertDate,
                                 Integer totalSeats, Integer availableSeats) {
        return new ConcertDate(id, concertName, concertDate, totalSeats, availableSeats);
    }
    
    /**
     * 가용 좌석 감소 (예약 시)
     */
    public void decreaseAvailableSeats() {
        if (this.availableSeats <= 0) {
            throw new IllegalStateException("예약 가능한 좌석이 없습니다");
        }
        this.availableSeats--;
    }
    
    /**
     * 가용 좌석 증가 (예약 취소 시)
     */
    public void increaseAvailableSeats() {
        if (this.availableSeats >= this.totalSeats) {
            throw new IllegalStateException("가용 좌석 수가 총 좌석 수를 초과할 수 없습니다");
        }
        this.availableSeats++;
    }
    
    /**
     * 예약 가능한 좌석이 있는지 확인
     * @return 예약 가능한 좌석이 있으면 true
     */
    public boolean hasAvailableSeats() {
        return this.availableSeats > 0;
    }
    
    /**
     * 좌석이 매진되었는지 확인
     * @return 매진이면 true
     */
    public boolean isSoldOut() {
        return this.availableSeats <= 0;
    }
    
    // Getters
    public Long getId() {
        return id;
    }
    
    public String getConcertName() {
        return concertName;
    }
    
    public LocalDate getConcertDate() {
        return concertDate;
    }
    
    public Integer getTotalSeats() {
        return totalSeats;
    }
    
    public Integer getAvailableSeats() {
        return availableSeats;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConcertDate that = (ConcertDate) o;
        return Objects.equals(id, that.id) && 
               Objects.equals(concertDate, that.concertDate);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, concertDate);
    }
}
