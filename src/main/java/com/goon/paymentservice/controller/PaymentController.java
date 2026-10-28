package com.goon.paymentservice.controller;

import com.goon.paymentservice.dto.CheckoutResponse;
import com.goon.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequiredArgsConstructor
public class PaymentController {
    private final PaymentService payments;

    @PostMapping("/payments/rides/{rideId}/checkout")
    public CheckoutResponse checkout(@PathVariable UUID rideId, @RequestHeader("X-User-Id") UUID userId) {
        return payments.checkout(rideId, userId);
    }

    @GetMapping("/payments/rides/{rideId}")
    public CheckoutResponse status(@PathVariable UUID rideId, @RequestHeader("X-User-Id") UUID userId) {
        return payments.status(rideId, userId);
    }

    public record Verification(String razorpay_order_id, String razorpay_payment_id,
                               String razorpay_signature) {}

    @PostMapping("/payments/rides/{rideId}/verify")
    public CheckoutResponse verify(@PathVariable UUID rideId, @RequestHeader("X-User-Id") UUID userId,
                                   @RequestBody Verification request) {
        return payments.verifyCheckout(rideId, userId, request.razorpay_order_id(),
                request.razorpay_payment_id(), request.razorpay_signature());
    }

    @PostMapping("/webhooks/razorpay")
    public ResponseEntity<Void> webhook(@RequestBody byte[] rawBody,
                                       @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
                                       @RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) {
        payments.handleWebhook(rawBody, signature, eventId);
        return ResponseEntity.noContent().build();
    }
}

