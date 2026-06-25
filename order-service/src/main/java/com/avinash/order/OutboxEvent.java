package com.avinash.order;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    private String eventId;
    private String aggregateId;
    private String eventType;
    private String payload;
    private boolean processed;

    protected OutboxEvent() {}

    public OutboxEvent(String eventId, String aggregateId, String eventType, String payload) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
        this.processed = false;
    }

    public String getEventId() { return eventId; }
    public String getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public boolean isProcessed() { return processed; }

    public void setProcessed(boolean processed) { this.processed = processed; }
}
