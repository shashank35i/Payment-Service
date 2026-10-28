package com.goon.paymentservice.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.FixedBackOff;
import java.util.Map;

@Configuration
public class PaymentKafkaConfig {
    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> paymentKafkaListenerContainerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String servers,
            KafkaTemplate<String, String> template) {
        var recoverer = new DeadLetterPublishingRecoverer(template,
                (record, exception) -> new TopicPartition(record.topic() + "-dlt", record.partition()));
        recoverer.setFailIfSendResultIsError(true);
        var errors = new DefaultErrorHandler(recoverer, new FixedBackOff(2000L, 5L));
        errors.setCommitRecovered(true);
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, servers,
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false,
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(errors);
        return factory;
    }
}

