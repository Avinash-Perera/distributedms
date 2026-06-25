package com.avinash.fulfillment;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "shipments")
public class Shipment {
    @Id
    private String id;
    private String orderId;
    private String status;

    protected Shipment() {}

    public Shipment(String orderId, String status) {
        this.id = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.status = status;
    }

    public String getId() { return id; }
    public String getOrderId() { return orderId; }
    public String getStatus() { return status; }
}
