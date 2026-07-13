package com.avinash.inventory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryRestController {

    @Autowired
    private InventoryRepository inventoryRepository;

    @PostMapping("/check")
    @PreAuthorize("hasRole('CUSTOMER')")
    public InventoryCheckResponse checkInventory(@RequestBody InventoryCheckRequest request) {
        boolean available = inventoryRepository.findById(request.getProductId())
                .map(inventory -> inventory.getStock() >= request.getQuantity())
                .orElse(false);
        return new InventoryCheckResponse(available);
    }

    public static class InventoryCheckRequest {
        private String productId;
        private int quantity;

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    public static class InventoryCheckResponse {
        private boolean available;

        public InventoryCheckResponse(boolean available) { this.available = available; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
    }
}
