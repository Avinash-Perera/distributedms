package com.avinash.order;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxRelayScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxRelayScheduler(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void relayEvents() {
        List<OutboxEvent> events = outboxEventRepository.findByProcessedFalse();
        for (OutboxEvent event : events) {
            // Use the orderId (aggregateId) as the Kafka Message Key to guarantee partition ordering
            kafkaTemplate.send("order-events", event.getAggregateId(), event.getPayload())
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        event.setProcessed(true);
                        outboxEventRepository.save(event);
                    }
                });
        }
    }
}
