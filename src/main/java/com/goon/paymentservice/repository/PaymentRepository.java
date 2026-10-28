package com.goon.paymentservice.repository;

import com.goon.paymentservice.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByRideId(UUID rideId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.rideId = :rideId")
    Optional<Payment> findByRideIdForUpdate(@Param("rideId") UUID rideId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.paymentId = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") UUID id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.providerOrderId = :orderId")
    Optional<Payment> findByOrderForUpdate(@Param("orderId") String orderId);
    @Query("select p.paymentId from Payment p where p.status <> com.goon.paymentservice.entity.PaymentStatus.PAID and p.nextProviderAttemptAt <= :now order by p.nextProviderAttemptAt")
    List<UUID> findDue(@Param("now") Instant now, Pageable page);
}

