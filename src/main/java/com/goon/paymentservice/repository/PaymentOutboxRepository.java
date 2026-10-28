package com.goon.paymentservice.repository;

import com.goon.paymentservice.entity.PaymentOutbox;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, UUID> {
    @Query(value = "SELECT * FROM payment_outbox WHERE published_at IS NULL ORDER BY created_at,event_id LIMIT 100 FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<PaymentOutbox> lockPending();
}

