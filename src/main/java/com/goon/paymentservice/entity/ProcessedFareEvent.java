package com.goon.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "processed_fare_events")
@Getter @NoArgsConstructor @AllArgsConstructor
public class ProcessedFareEvent {
    @Id @Column(name = "event_id")
    private UUID eventId;
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}

