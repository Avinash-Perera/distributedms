package com.avinash.fulfillment;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FulfillmentSagaListenerTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;
    
    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;
    private FulfillmentSagaListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listener = new FulfillmentSagaListener(processedEventRepository, shipmentRepository, kafkaTemplate, objectMapper);
    }

    @Test
    void testOnPaymentSucceeded_Success() {
        String payload = "{\"eventId\":\"evt-1\", \"orderId\":\"ord-1\", \"customerId\":\"GOOD-GUY\"}";
        when(processedEventRepository.existsById("evt-1")).thenReturn(false);

        listener.onPaymentSucceeded(payload);

        verify(shipmentRepository, times(1)).save(any(Shipment.class));
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
        verify(kafkaTemplate, times(1)).send(eq("order-shipped"), eq("ord-1"), eq(payload));
    }

    @Test
    void testOnPaymentSucceeded_Failed() {
        String payload = "{\"eventId\":\"evt-2\", \"orderId\":\"ord-2\", \"customerId\":\"BAD-ADDRESS\"}";
        when(processedEventRepository.existsById("evt-2")).thenReturn(false);

        listener.onPaymentSucceeded(payload);

        verify(shipmentRepository, never()).save(any(Shipment.class));
        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
        verify(kafkaTemplate, times(1)).send(eq("fulfillment-failed"), eq("ord-2"), eq(payload));
    }
}
