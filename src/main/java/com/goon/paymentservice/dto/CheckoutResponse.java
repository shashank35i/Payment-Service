package com.goon.paymentservice.dto;
import com.goon.paymentservice.entity.*;
import java.util.UUID;
public record CheckoutResponse(UUID paymentId, UUID rideId, PaymentStatus status,
                               long amountMinor, String currency, String keyId, String orderId) {
    public static CheckoutResponse from(Payment p, String keyId) {
        return new CheckoutResponse(p.getPaymentId(), p.getRideId(), p.getStatus(),
                p.getAmountMinor(), p.getCurrency(), keyId, p.getProviderOrderId());
    }
}

