package com.goon.paymentservice.service;

import com.goon.paymentservice.dto.*;
import com.goon.paymentservice.entity.*;
import com.goon.paymentservice.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository payments;
    private final ProcessedFareEventRepository fareEvents;
    private final ProcessedWebhookRepository webhooks;
    private final PaymentOutboxRepository outbox;
    private final RazorpayClient provider;
    private final RazorpaySignatures signatures;

    @Transactional
    public void acceptFare(UUID eventId, FareFinalizedPayload event) {
        if (fareEvents.existsById(eventId)) {
            return;
        }
        if (event.rideId() == null || event.customerId() == null || event.driverId() == null
                || event.finalizedAt() == null || event.amountMinor() < 0
                || !"INR".equals(event.currency())) {
            throw new IllegalArgumentException("Invalid finalized fare");
        }
        Payment payment = payments.findByRideIdForUpdate(event.rideId()).orElse(null);
        if (payment == null) {
            payment = new Payment();
            payment.setPaymentId(UUID.randomUUID());
            payment.setRideId(event.rideId());
            payment.setCustomerId(event.customerId());
            payment.setAmountMinor(event.amountMinor());
            payment.setCurrency(event.currency());
            payment = payments.saveAndFlush(payment);
            if (payment.getAmountMinor() == 0) {
                complete(payment, null, true);
            }
        } else if (!payment.getCustomerId().equals(event.customerId())
                || payment.getAmountMinor() != event.amountMinor()
                || !payment.getCurrency().equals(event.currency())) {
            throw new IllegalStateException("Final fare changed for existing payment");
        }
        fareEvents.saveAndFlush(new ProcessedFareEvent(eventId, Instant.now()));
    }

    @Transactional
    public CheckoutResponse checkout(UUID rideId, UUID customerId) {
        Payment payment = ownedPayment(rideId, customerId);
        if (payment.getStatus() != PaymentStatus.PAID && payment.getProviderOrderId() == null) {
            ensureOrder(payment);
        }
        return CheckoutResponse.from(payment, provider.keyId());
    }

    @Transactional(readOnly = true)
    public CheckoutResponse status(UUID rideId, UUID customerId) {
        // Ownership is checked without exposing a different customer's payment.
        Payment payment = payments.findByRideId(rideId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not ready"));
        requireOwner(payment, customerId);
        return CheckoutResponse.from(payment, provider.keyId());
    }

    @Transactional
    public CheckoutResponse verifyCheckout(UUID rideId, UUID customerId,
                                           String orderId, String paymentId, String signature) {
        Payment payment = ownedPayment(rideId, customerId);
        if (payment.getProviderOrderId() == null
                || !payment.getProviderOrderId().equals(orderId)
                || paymentId == null || !paymentId.matches("pay_[A-Za-z0-9]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Checkout order mismatch");
        }
        signatures.verifyCheckout(payment.getProviderOrderId(), paymentId, signature);
        JsonNode remote = provider.fetchPayment(paymentId);
        validatePayment(remote, payment, null);
        if ("captured".equals(remote.path("status").asText())) {
            complete(payment, paymentId, true);
        }
        return CheckoutResponse.from(payment, provider.keyId());
    }

    @Transactional
    public void handleWebhook(byte[] rawBody, String signature, String providerEventId) {
        signatures.verifyWebhook(rawBody, signature);
        String hash = signatures.bodyHash(rawBody);
        if (webhooks.existsById(hash)) {
            return;
        }
        JsonNode root;
        try {
            root = JsonMapper.shared().readTree(rawBody);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook JSON");
        }
        String type = root.path("event").asText();
        if (!"payment.captured".equals(type) && !"payment.failed".equals(type)) {
            return;
        }
        JsonNode remote = root.path("payload").path("payment").path("entity");
        String orderId = remote.path("order_id").asText();
        if (!orderId.matches("order_[A-Za-z0-9]+")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid webhook order");
        }
        Payment payment = payments.findByOrderForUpdate(orderId).orElse(null);
        if (payment == null) {
            // Recover an order created remotely just before a timeout or database rollback.
            JsonNode order = provider.fetchOrder(orderId);
            UUID id;
            try {
                id = UUID.fromString(order.path("receipt").asText());
            } catch (RuntimeException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown payment order");
            }
            payment = payments.findByIdForUpdate(id).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.CONFLICT, "Payment order not recorded yet"));
            provider.validateOrder(order, payment);
            if (payment.getProviderOrderId() != null && !orderId.equals(payment.getProviderOrderId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment order mismatch");
            }
            payment.setProviderOrderId(orderId);
        }
        if (webhooks.existsById(hash)) {
            return;
        }
        boolean captured = "payment.captured".equals(type);
        validatePayment(remote, payment, captured ? "captured" : "failed");
        complete(payment, remote.path("id").asText(), captured);
        webhooks.saveAndFlush(new ProcessedWebhook(hash,
                providerEventId == null ? null : providerEventId.substring(0, Math.min(128, providerEventId.length())),
                Instant.now()));
    }

    @Transactional
    public void refreshProvider(UUID paymentId) {
        Payment payment = payments.findByIdForUpdate(paymentId).orElseThrow();
        if (payment.getStatus() == PaymentStatus.PAID) {
            return;
        }
        if (payment.getProviderOrderId() == null) {
            ensureOrder(payment);
        }
        JsonNode captured = provider.capturedPayment(payment.getProviderOrderId());
        if (captured != null) {
            validatePayment(captured, payment, "captured");
            complete(payment, captured.path("id").asText(), true);
        }
        payment.setNextProviderAttemptAt(Instant.now().plusSeconds(30));
    }

    private Payment ownedPayment(UUID rideId, UUID customerId) {
        Payment payment = payments.findByRideIdForUpdate(rideId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not ready"));
        requireOwner(payment, customerId);
        return payment;
    }

    private void requireOwner(Payment payment, UUID customerId) {
        if (!payment.getCustomerId().equals(customerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Payment belongs to another customer");
        }
    }

    private void ensureOrder(Payment payment) {
        if (payment.getAmountMinor() < 100) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Razorpay minimum charge is INR 1");
        }
        JsonNode order = provider.ensureOrder(payment);
        payment.setProviderOrderId(order.path("id").asText());
        payment.setUpdatedAt(Instant.now());
    }

    private void validatePayment(JsonNode remote, Payment payment, String expectedStatus) {
        if (remote == null || !remote.path("id").asText().matches("pay_[A-Za-z0-9]+")
                || !payment.getProviderOrderId().equals(remote.path("order_id").asText())
                || !remote.path("amount").isIntegralNumber()
                || remote.path("amount").asLong() != payment.getAmountMinor()
                || !payment.getCurrency().equals(remote.path("currency").asText())
                || (expectedStatus != null && !expectedStatus.equals(remote.path("status").asText()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provider payment mismatch");
        }
    }

    private void complete(Payment payment, String providerPaymentId, boolean captured) {
        if (payment.getStatus() == PaymentStatus.PAID) {
            return;
        }
        PaymentStatus next = captured ? PaymentStatus.PAID : PaymentStatus.FAILED;
        if (payment.getStatus() == next && java.util.Objects.equals(
                payment.getProviderPaymentId(), providerPaymentId)) {
            return;
        }
        payment.setStatus(next);
        payment.setProviderPaymentId(providerPaymentId);
        payment.setRevision(payment.getRevision() + 1);
        payment.setUpdatedAt(Instant.now());
        var event = new PaymentResultPayload(payment.getPaymentId(), payment.getRideId(),
                payment.getCustomerId(), payment.getAmountMinor(), payment.getCurrency(),
                next.name(), payment.getRevision(), payment.getUpdatedAt());
        PaymentOutbox row = new PaymentOutbox();
        row.setRideId(payment.getRideId());
        row.setTopic(captured ? "payment-completed" : "payment-failed");
        row.setPayload(JsonMapper.shared().writeValueAsString(event));
        outbox.save(row);
    }
}

