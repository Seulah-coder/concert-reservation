package com.example.concert_reservation.domain.concert.infrastructure;

import com.example.concert_reservation.domain.concert.infrastructure.entity.SeatEntity;
import com.example.concert_reservation.domain.concert.models.Seat;
import com.example.concert_reservation.domain.concert.models.SeatStatus;
import com.example.concert_reservation.domain.concert.repositories.SeatStoreRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 좌석 Repository 구현체
 * Infrastructure 레이어에서 Domain 레이어의 인터페이스를 구현
 */
@Repository
public class SeatCoreStoreRepository implements SeatStoreRepository {
    
    private final SeatJpaRepository seatJpaRepository;
    
    public SeatCoreStoreRepository(SeatJpaRepository seatJpaRepository) {
        this.seatJpaRepository = seatJpaRepository;
    }
    
    @Override
    public Seat save(Seat seat) {
        SeatEntity entity = toEntity(seat);
        SeatEntity saved = seatJpaRepository.save(entity);
        return toDomain(saved);
    }
    
    @Override
    public List<Seat> findByConcertDateId(Long concertDateId) {
        return seatJpaRepository.findByConcertDateId(concertDateId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Seat> findByConcertDateIdAndSeatNumber(Long concertDateId, Integer seatNumber) {
        return seatJpaRepository.findByConcertDateIdAndSeatNumber(concertDateId, seatNumber)
            .map(this::toDomain);
    }
    
    @Override
    public Optional<Seat> findById(Long id) {
        return seatJpaRepository.findById(id)
            .map(this::toDomain);
    }
    
    @Override
    public Optional<Seat> findByIdWithLock(Long id) {
        return seatJpaRepository.findByIdWithLock(id)
            .map(this::toDomain);
    }
    
    /**
     * Entity를 Domain 모델로 변환
     */
    private Seat toDomain(SeatEntity entity) {
        return Seat.of(
            entity.getId(),
            entity.getConcertDateId(),
            entity.getSeatNumber(),
            SeatStatus.valueOf(entity.getStatus()),
            entity.getPrice()
        );
    }
    
    /**
     * Domain 모델을 Entity로 변환
     */
    private SeatEntity toEntity(Seat domain) {
        return new SeatEntity(
            domain.getId(),
            domain.getConcertDateId(),
            domain.getSeatNumber(),
            domain.getStatus().name(),
            domain.getPrice()
        );
    }
}
