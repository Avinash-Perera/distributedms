package com.avinash.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventorySagaListenerTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;

    private InventorySagaListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listener = new InventorySagaListener(inventoryRepository, processedEventRepository, kafkaTemplate, objectMapper);
    }

    @Test
    void testOnOrderCreated_Success() throws Exception {
        String payload = "{\"eventId\":\"evt-1\", \"orderId\":\"ord-1\", \"productId\":\"prod-1\", \"eventType\":\"OrderCreatedEvent\"}";

        Inventory mockInventory = new Inventory("prod-1", 10);
        
        when(processedEventRepository.existsById("evt-1")).thenReturn(false);
        when(inventoryRepository.findById("prod-1")).thenReturn(Optional.of(mockInventory));

        listener.onOrderCreated(payload);

        verify(inventoryRepository, times(1)).save(mockInventory);
        assert mockInventory.getStock() == 9;

        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
        verify(kafkaTemplate, times(1)).send(eq("inventory-reserved"), eq("ord-1"), eq(payload));
    }

    @Test
    void testOnOrderCreated_InsufficientStock() throws Exception {
        String payload = "{\"eventId\":\"evt-2\", \"orderId\":\"ord-2\", \"productId\":\"prod-2\", \"eventType\":\"OrderCreatedEvent\"}";

        Inventory mockInventory = new Inventory("prod-2", 0);
        
        when(processedEventRepository.existsById("evt-2")).thenReturn(false);
        when(inventoryRepository.findById("prod-2")).thenReturn(Optional.of(mockInventory));

        listener.onOrderCreated(payload);

        verify(inventoryRepository, never()).save(any());
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
        verify(kafkaTemplate, times(1)).send(eq("inventory-failed"), eq("ord-2"), eq(payload));
    }
}
