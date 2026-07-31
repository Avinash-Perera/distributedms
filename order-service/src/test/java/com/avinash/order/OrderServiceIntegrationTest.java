package com.avinash.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Autowired
    private OrderService orderService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void testOrderCreationAndOutboxEvent() {
        CreateOrderRequest request = new CreateOrderRequest("cust-int", "int@test.com", "prod-int");

        Order order = orderService.createOrder(request);
        assertNotNull(order);
        assertTrue(order.getId().startsWith("ORD-"));

        // Verify outbox event is saved in the real DB
        await().atMost(Duration.ofSeconds(5)).until(() -> {
            return outboxEventRepository.findAll().stream()
                    .anyMatch(event -> event.getAggregateId().equals(order.getId()));
        });
    }
}
