package com.goon.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_outbox", indexes =
        @Index(name = "idx_payment_outbox_pending", columnList = "published_at,created_at"))
@Getter @Setter @NoArgsConstructor
public class PaymentOutbox {
    @Id @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId = UUID.randomUUID();
    @Column(name = "ride_id", nullable = false)
    private UUID rideId;
    @Column(nullable = false, length = 64)
    private String topic;
    @Column(nullable = false, columnDefinition = "JSON")
    private String payload;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "published_at")
    private Instant publishedAt;
}

