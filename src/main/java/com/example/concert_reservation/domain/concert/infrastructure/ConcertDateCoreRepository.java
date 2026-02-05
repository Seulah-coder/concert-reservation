package com.example.concert_reservation.domain.concert.infrastructure;

import com.example.concert_reservation.domain.concert.infrastructure.entity.ConcertDateEntity;
import com.example.concert_reservation.domain.concert.models.ConcertDate;
import com.example.concert_reservation.domain.concert.repositories.ConcertReaderRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 콘서트 날짜 Repository 구현체
 * Infrastructure 레이어에서 Domain 레이어의 인터페이스를 구현
 */
@Repository
public class ConcertDateCoreRepository implements ConcertReaderRepository {
    
    private final ConcertDateJpaRepository concertDateJpaRepository;
    
    public ConcertDateCoreRepository(ConcertDateJpaRepository concertDateJpaRepository) {
        this.concertDateJpaRepository = concertDateJpaRepository;
    }
    
    @Override
    public List<ConcertDate> findAvailableDates() {
        return concertDateJpaRepository.findAvailableDates().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<ConcertDate> findById(Long id) {
        return concertDateJpaRepository.findById(id)
            .map(this::toDomain);
    }
    
    @Override
    public Optional<ConcertDate> findByDate(LocalDate date) {
        return concertDateJpaRepository.findByConcertDate(date)
            .map(this::toDomain);
    }
    
    /**
     * Entity를 Domain 모델로 변환
     */
    private ConcertDate toDomain(ConcertDateEntity entity) {
        return ConcertDate.of(
            entity.getId(),
            entity.getConcertName(),
            entity.getConcertDate(),
            entity.getTotalSeats(),
            entity.getAvailableSeats()
        );
    }
    
    /**
     * Domain 모델을 Entity로 변환
     */
    private ConcertDateEntity toEntity(ConcertDate domain) {
        return new ConcertDateEntity(
            domain.getId(),
            domain.getConcertName(),
            domain.getConcertDate(),
            domain.getTotalSeats(),
            domain.getAvailableSeats()
        );
    }
}
