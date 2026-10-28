package com.goon.paymentservice.worker;

import com.goon.paymentservice.dto.FareFinalizedPayload;
import com.goon.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;

@Component @RequiredArgsConstructor
public class FareFinalizedWorker {
    private final PaymentService payments;
    @KafkaListener(topics = "fare-finalized", groupId = "payment-fare-finalized",
            containerFactory = "paymentKafkaListenerContainerFactory")
    public void consume(String body, @Header("eventId") String eventId, Acknowledgment ack) {
        payments.acceptFare(UUID.fromString(eventId),
                JsonMapper.shared().readValue(body, FareFinalizedPayload.class));
        ack.acknowledge();
    }
}

