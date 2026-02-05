package com.example.concert_reservation.domain.refund.infrastructure;

import com.example.concert_reservation.domain.refund.models.Refund;
import com.example.concert_reservation.domain.refund.repositories.RefundRepository;
import com.example.concert_reservation.domain.refund.infrastructure.entity.RefundEntity;
import com.example.concert_reservation.domain.refund.infrastructure.jpa.RefundJpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * 환불 저장소 구현체
 */
@Repository
public class RefundStoreRepository implements RefundRepository {

    private final RefundJpaRepository jpaRepository;

    public RefundStoreRepository(RefundJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Refund save(Refund refund) {
        RefundEntity entity = new RefundEntity(
            refund.getPaymentId(),
            refund.getReservationId(),
            refund.getUserId(),
            refund.getAmount(),
            refund.getReason(),
            refund.getStatus()
        );

        RefundEntity savedEntity = jpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    public Optional<Refund> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Refund> findByPaymentId(Long paymentId) {
        return jpaRepository.findByPaymentId(paymentId).map(this::toDomain);
    }

    @Override
    public Optional<Refund> findByReservationId(Long reservationId) {
        return jpaRepository.findByReservationId(reservationId).map(this::toDomain);
    }

    private Refund toDomain(RefundEntity entity) {
        return Refund.of(
            entity.getId(),
            entity.getPaymentId(),
            entity.getReservationId(),
            entity.getUserId(),
            entity.getAmount(),
            entity.getReason(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
