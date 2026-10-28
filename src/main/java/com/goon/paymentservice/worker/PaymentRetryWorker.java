package com.goon.paymentservice.worker;

import com.goon.paymentservice.repository.PaymentRepository;
import com.goon.paymentservice.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component @RequiredArgsConstructor @Slf4j
public class PaymentRetryWorker {
    private final PaymentRepository payments;
    private final PaymentService service;
    private final PaymentOutboxPublisher outbox;
    private final RazorpayClient provider;

    @Scheduled(fixedDelayString = "${app.payment.provider-retry-ms:30000}")
    public void refreshPending() {
        if (!provider.configured()) {
            return;
        }
        for (var id : payments.findDue(Instant.now(), PageRequest.of(0, 20))) {
            try {
                service.refreshProvider(id);
            } catch (RuntimeException e) {
                log.warn("Provider reconciliation deferred for payment {} ({})",
                        id, e.getClass().getSimpleName());
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.outbox.delay-ms:500}")
    public void publish() {
        try {
            outbox.publish();
        } catch (Exception e) {
            log.warn("Payment outbox publication deferred ({})", e.getClass().getSimpleName());
        }
    }
}

