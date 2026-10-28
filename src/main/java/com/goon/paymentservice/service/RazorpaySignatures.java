package com.goon.paymentservice.service;

import com.goon.paymentservice.config.RazorpayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class RazorpaySignatures {
    private final RazorpayProperties properties;

    public void verifyWebhook(byte[] rawBody, String signature) {
        verify(rawBody, signature, properties.webhookSecret());
    }

    public void verifyCheckout(String orderId, String paymentId, String signature) {
        verify((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8),
                signature, properties.keySecret());
    }

    public String bodyHash(byte[] body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void verify(byte[] body, String signature, String secret) {
        if (secret == null || secret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payment secret not configured");
        }
        if (signature == null || !signature.matches("[a-fA-F0-9]{64}")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid payment signature");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            if (!MessageDigest.isEqual(mac.doFinal(body), HexFormat.of().parseHex(signature))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid payment signature");
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not verify payment signature", e);
        }
    }
}

