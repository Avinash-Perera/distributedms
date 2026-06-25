package com.avinash.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "inventory")
public class Inventory {
    @Id
    private String productId;
    
    private int stock;
    
    @Version
    private Long version;

    protected Inventory() {}

    public Inventory(String productId, int stock) {
        this.productId = productId;
        this.stock = stock;
    }

    public String getProductId() { return productId; }
    public int getStock() { return stock; }
    public Long getVersion() { return version; }

    public void setStock(int stock) { this.stock = stock; }
}
