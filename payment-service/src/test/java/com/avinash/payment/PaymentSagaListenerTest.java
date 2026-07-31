package com.avinash.payment;

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
class PaymentSagaListenerTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;
    private PaymentSagaListener listener;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        listener = new PaymentSagaListener(processedEventRepository, kafkaTemplate, objectMapper);
    }

    @Test
    void testOnInventoryReserved_Success() {
        String payload = "{\"eventId\":\"evt-1\", \"orderId\":\"ord-1\", \"customerId\":\"GOOD-GUY\"}";
        when(processedEventRepository.existsById("evt-1")).thenReturn(false);

        listener.onInventoryReserved(payload);

        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
        verify(kafkaTemplate, times(1)).send(eq("payment-succeeded"), eq("ord-1"), eq(payload));
    }

    @Test
    void testOnInventoryReserved_Failed() {
        String payload = "{\"eventId\":\"evt-2\", \"orderId\":\"ord-2\", \"customerId\":\"POOR-GUY\"}";
        when(processedEventRepository.existsById("evt-2")).thenReturn(false);

        listener.onInventoryReserved(payload);

        verify(processedEventRepository, times(1)).save(any(ProcessedEvent.class));
        verify(kafkaTemplate, times(1)).send(eq("payment-failed"), eq("ord-2"), eq(payload));
    }
}
