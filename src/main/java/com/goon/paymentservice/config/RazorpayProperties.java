package com.goon.paymentservice.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties("app.razorpay")
public record RazorpayProperties(String keyId, String keySecret, String webhookSecret, String apiUrl) {
    public boolean configured() {
        return keyId != null && keyId.startsWith("rzp_test_")
                && keySecret != null && !keySecret.isBlank();
    }
}

