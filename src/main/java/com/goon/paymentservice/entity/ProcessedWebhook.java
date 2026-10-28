package com.goon.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name = "processed_webhooks")
@Getter @NoArgsConstructor @AllArgsConstructor
public class ProcessedWebhook {
    @Id @Column(name = "payload_hash", length = 64)
    private String payloadHash;
    @Column(name = "provider_event_id", length = 128)
    private String providerEventId;
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}

