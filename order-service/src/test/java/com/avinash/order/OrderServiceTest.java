package com.avinash.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private ObjectMapper objectMapper;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        orderService = new OrderService(orderRepository, outboxEventRepository, objectMapper);
    }

    @Test
    void testCreateOrder() {
        // Arrange
        CreateOrderRequest request = new CreateOrderRequest("cust-1", "test@test.com", "prod-1");
        
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArguments()[0]);
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        Order createdOrder = orderService.createOrder(request);

        // Assert
        assertNotNull(createdOrder);
        assertEquals("cust-1", createdOrder.getCustomerId());
        assertEquals("PENDING", createdOrder.getStatus());
        assertTrue(createdOrder.getId().startsWith("ORD-"));

        // Verify Order saved
        verify(orderRepository, times(1)).save(any(Order.class));

        // Verify Outbox Event saved
        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(1)).save(outboxCaptor.capture());
        
        OutboxEvent savedEvent = outboxCaptor.getValue();
        assertEquals(createdOrder.getId(), savedEvent.getAggregateId());
        assertEquals("OrderCreatedEvent", savedEvent.getEventType());
        assertTrue(savedEvent.getPayload().contains("cust-1"));
    }
}
