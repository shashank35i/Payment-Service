package com.goon.paymentservice.dto;
import java.time.Instant;
import java.util.UUID;
public record FareFinalizedPayload(UUID rideId, UUID customerId, UUID driverId,
                                   long amountMinor, String currency, Instant finalizedAt) {}

