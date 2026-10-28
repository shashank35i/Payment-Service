package com.goon.paymentservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payment_ride", columnNames = "ride_id"),
        @UniqueConstraint(name = "uk_payment_order", columnNames = "provider_order_id"),
        @UniqueConstraint(name = "uk_provider_payment", columnNames = "provider_payment_id")
}, indexes = @Index(name = "idx_payment_retry", columnList = "status,next_provider_attempt_at"))
@Getter @Setter @NoArgsConstructor
public class Payment {
    @Id
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "payment_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID paymentId;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "ride_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID rideId;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "customer_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
    private UUID customerId;
    @Column(name = "amount_minor", nullable = false, updatable = false)
    private long amountMinor;
    @Column(nullable = false, length = 3, updatable = false)
    private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private PaymentStatus status = PaymentStatus.PENDING;
    @Column(name = "provider_order_id", length = 80)
    private String providerOrderId;
    @Column(name = "provider_payment_id", length = 80)
    private String providerPaymentId;
    @Column(nullable = false)
    private long revision;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    @Column(name = "next_provider_attempt_at", nullable = false)
    private Instant nextProviderAttemptAt = Instant.now();
}

