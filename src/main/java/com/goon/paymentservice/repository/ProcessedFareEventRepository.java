package com.goon.paymentservice.repository;
import com.goon.paymentservice.entity.ProcessedFareEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface ProcessedFareEventRepository extends JpaRepository<ProcessedFareEvent, UUID> {}

