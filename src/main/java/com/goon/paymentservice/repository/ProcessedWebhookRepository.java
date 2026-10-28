package com.goon.paymentservice.repository;
import com.goon.paymentservice.entity.ProcessedWebhook;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProcessedWebhookRepository extends JpaRepository<ProcessedWebhook, String> {}

