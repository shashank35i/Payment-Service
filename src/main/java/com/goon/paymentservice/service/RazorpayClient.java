package com.goon.paymentservice.service;

import com.goon.paymentservice.config.RazorpayProperties;
import com.goon.paymentservice.entity.Payment;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

@Service
public class RazorpayClient {
    private final RestClient client;
    private final RazorpayProperties properties;

    public RazorpayClient(RestClient.Builder builder, RazorpayProperties properties) {
        this.properties = properties;
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3)).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = builder.baseUrl(properties.apiUrl()).requestFactory(factory)
                .defaultHeaders(h -> h.setBasicAuth(properties.keyId(), properties.keySecret())).build();
    }

    public boolean configured() { return properties.configured(); }
    public String keyId() { return properties.keyId(); }

    public JsonNode ensureOrder(Payment payment) {
        requireConfigured();
        String receipt = payment.getPaymentId().toString();
        JsonNode existing = client.get().uri(b -> b.path("/orders")
                        .queryParam("receipt", receipt).queryParam("count", 100).build())
                .retrieve().body(JsonNode.class);
        if (existing == null || !existing.path("items").isArray()) {
            throw new IllegalStateException("Invalid Razorpay order lookup");
        }
        for (JsonNode order : existing.path("items")) {
            if (receipt.equals(order.path("receipt").asText())) {
                validateOrder(order, payment);
                return order;
            }
        }
        JsonNode order = client.post().uri("/orders").contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("amount", payment.getAmountMinor(), "currency", payment.getCurrency(),
                        "receipt", receipt, "notes", Map.of(
                                "paymentId", receipt, "rideId", payment.getRideId().toString())))
                .retrieve().body(JsonNode.class);
        validateOrder(order, payment);
        return order;
    }

    public JsonNode fetchOrder(String orderId) {
        requireConfigured();
        return client.get().uri("/orders/{id}", orderId).retrieve().body(JsonNode.class);
    }

    public JsonNode fetchPayment(String paymentId) {
        requireConfigured();
        return client.get().uri("/payments/{id}", paymentId).retrieve().body(JsonNode.class);
    }

    public JsonNode capturedPayment(String orderId) {
        requireConfigured();
        JsonNode response = client.get().uri("/orders/{id}/payments", orderId)
                .retrieve().body(JsonNode.class);
        if (response == null || !response.path("items").isArray()) {
            throw new IllegalStateException("Invalid Razorpay payment lookup");
        }
        for (JsonNode item : response.path("items")) {
            if ("captured".equals(item.path("status").asText())) {
                return item;
            }
        }
        return null;
    }

    public void validateOrder(JsonNode order, Payment payment) {
        if (order == null || !order.path("id").asText().startsWith("order_")
                || !payment.getPaymentId().toString().equals(order.path("receipt").asText())
                || !order.path("amount").isIntegralNumber()
                || order.path("amount").asLong() != payment.getAmountMinor()
                || !payment.getCurrency().equals(order.path("currency").asText())
                || !payment.getRideId().toString().equals(order.path("notes").path("rideId").asText())) {
            throw new IllegalStateException("Razorpay order does not match payment");
        }
    }

    private void requireConfigured() {
        if (!configured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Razorpay test credentials are required");
        }
    }
}

