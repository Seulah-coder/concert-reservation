package com.example.concert_reservation.domain.reservation.infrastructure;

import com.example.concert_reservation.domain.reservation.infrastructure.entity.ReservationEntity;
import com.example.concert_reservation.domain.reservation.models.Reservation;
import com.example.concert_reservation.domain.reservation.models.ReservationStatus;
import com.example.concert_reservation.domain.reservation.repositories.ReservationStoreRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 예약 Repository 구현체
 * Infrastructure 레이어에서 Domain 레이어의 인터페이스를 구현
 */
@Repository
public class ReservationCoreStoreRepository implements ReservationStoreRepository {
    
    private final ReservationJpaRepository reservationJpaRepository;
    
    public ReservationCoreStoreRepository(ReservationJpaRepository reservationJpaRepository) {
        this.reservationJpaRepository = reservationJpaRepository;
    }
    
    @Override
    public Reservation save(Reservation reservation) {
        ReservationEntity entity = toEntity(reservation);
        ReservationEntity saved = reservationJpaRepository.save(entity);
        return toDomain(saved);
    }
    
    @Override
    public Optional<Reservation> findById(Long id) {
        return reservationJpaRepository.findById(id)
            .map(this::toDomain);
    }
    
    @Override
    public List<Reservation> findByUserId(String userId) {
        return reservationJpaRepository.findByUserId(userId).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Reservation> findActiveBySeatId(Long seatId) {
        return reservationJpaRepository.findActiveBySeatId(seatId)
            .map(this::toDomain);
    }
    
    @Override
    public List<Reservation> findExpiredReservations(LocalDateTime now) {
        return reservationJpaRepository.findExpiredReservations(now).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    /**
     * Entity를 Domain 모델로 변환
     */
    private Reservation toDomain(ReservationEntity entity) {
        return Reservation.of(
            entity.getId(),
            entity.getUserId(),
            entity.getSeatId(),
            entity.getConcertDateId(),
            entity.getPrice(),
            ReservationStatus.valueOf(entity.getStatus()),
            entity.getReservedAt(),
            entity.getExpiresAt()
        );
    }
    
    /**
     * Domain 모델을 Entity로 변환
     */
    private ReservationEntity toEntity(Reservation domain) {
        return new ReservationEntity(
            domain.getId(),
            domain.getUserId(),
            domain.getSeatId(),
            domain.getConcertDateId(),
            domain.getPrice(),
            domain.getStatus().name(),
            domain.getReservedAt(),
            domain.getExpiresAt()
        );
    }
}
