package com.goon.paymentservice.service;

import com.goon.paymentservice.repository.PaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service @RequiredArgsConstructor
public class PaymentOutboxPublisher {
    private final PaymentOutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    @Transactional
    public void publish() throws Exception {
        for (var row : outbox.lockPending()) {
            var record = new ProducerRecord<String, String>(
                    row.getTopic(), row.getRideId().toString(), row.getPayload());
            record.headers().add("eventId", row.getEventId().toString().getBytes(StandardCharsets.UTF_8));
            kafka.send(record).get(10, TimeUnit.SECONDS);
            row.setPublishedAt(Instant.now());
        }
    }
}

