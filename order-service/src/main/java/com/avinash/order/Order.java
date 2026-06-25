package com.avinash.order;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {
    @Id
    private String id;
    private String customerId;
    private String customerEmail;
    private String productId;
    private String status;

    protected Order() {} // JPA requires default constructor

    public Order(String id, String customerId, String customerEmail, String productId, String status) {
        this.id = id;
        this.customerId = customerId;
        this.customerEmail = customerEmail;
        this.productId = productId;
        this.status = status;
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getCustomerEmail() { return customerEmail; }
    public String getProductId() { return productId; }
    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }
}
