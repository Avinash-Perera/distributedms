package com.avinash.order;

import com.avinash.grpc.InventoryGrpcServiceGrpc;
import com.avinash.grpc.InventoryRequest;
import com.avinash.grpc.InventoryResponse;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.avinash.order.config.RestTemplateHeaderModifierInterceptor;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/test")
public class PerformanceTestController {

    @GrpcClient("inventory")
    private InventoryGrpcServiceGrpc.InventoryGrpcServiceBlockingStub inventoryGrpcStub;

    private final RestTemplate restTemplate;

    public PerformanceTestController(RestTemplateBuilder restTemplateBuilder, RestTemplateHeaderModifierInterceptor interceptor) {
        this.restTemplate = restTemplateBuilder
                .additionalInterceptors(interceptor)
                .build();
    }

    @GetMapping("/rest")
    public String testRest(@RequestParam(defaultValue = "100") int count) {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            InventoryCheckRequest req = new InventoryCheckRequest("PROD1", 1);
            restTemplate.postForObject("http://localhost:8082/api/inventory/check", req, InventoryCheckResponse.class);
        }
        long endTime = System.currentTimeMillis();
        return "REST Test completed " + count + " requests in " + (endTime - startTime) + " ms";
    }

    @GetMapping("/grpc")
    public String testGrpc(@RequestParam(defaultValue = "100") int count) {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            InventoryRequest req = InventoryRequest.newBuilder()
                    .setProductId("PROD1")
                    .setQuantity(1)
                    .build();
            inventoryGrpcStub.checkInventory(req);
        }
        long endTime = System.currentTimeMillis();
        return "gRPC Test completed " + count + " requests in " + (endTime - startTime) + " ms";
    }

    public static class InventoryCheckRequest {
        private String productId;
        private int quantity;

        public InventoryCheckRequest(String productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    public static class InventoryCheckResponse {
        private boolean available;
        public InventoryCheckResponse() {}
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
    }
}
