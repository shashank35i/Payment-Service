package com.goon.paymentservice.dto;
import java.time.Instant;
import java.util.UUID;
public record PaymentResultPayload(UUID paymentId, UUID rideId, UUID customerId,
                                   long amountMinor, String currency, String status,
                                   long revision, Instant occurredAt) {}

