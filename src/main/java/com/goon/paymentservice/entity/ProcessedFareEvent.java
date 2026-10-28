package com.goon.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "processed_fare_events")
@Getter @NoArgsConstructor @AllArgsConstructor
public class ProcessedFareEvent {
    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "event_id", columnDefinition = "BINARY(16)")
    private UUID eventId;
    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}

