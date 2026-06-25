# Event-Driven E-Commerce Microservices (Saga Choreography PoC)

A complete, production-grade Proof-of-Concept demonstrating a 5-service Event-Driven Architecture using Java 21, Spring Boot 3.2, Kafka (KRaft), PostgreSQL, Zipkin, and Prometheus.

## 🚀 Architecture Highlights
- **Transactional Outbox Pattern**: Guarantees atomic database writes and Kafka event publishing.
- **Saga Pattern (Choreography)**: Fully decoupled, event-driven distributed transactions with automatic rollbacks (compensating transactions) for Inventory, Payment, and Fulfillment failures.
- **Resiliency**: Idempotency checks (`ProcessedEvent` tables), Optimistic Locking (`@Version`), and Dead Letter Topics (`.DLT`).
- **Observability**: Distributed Tracing (Zipkin) and Metrics (Prometheus/Grafana) wired via Micrometer Actuator.

---

## 📂 Folder Structure

```text
distributedms/
├── api-gateway/               # Port 8080 - Spring Cloud Gateway (No Java logic, pure YAML routing)
├── order-service/             # Port 8081 - Receives POST, saves Order + OutboxEvent, publishes 'order-events'
├── inventory-service/         # Port 8082 - Listens to 'order-events', deducts stock (Optimistic Locking)
├── payment-service/           # Port 8083 - Mocks payment logic, triggers success or failure
├── fulfillment-service/       # Port 8084 - Saves shipment status
├── notification-service/      # Port 8085 - Stateless terminal listener (logs mock emails)
├── compose.yaml               # Infrastructure: Postgres, KRaft Kafka, Kafka UI, Zipkin, Prometheus, Grafana
├── postgres-init.sh           # Auto-creates isolated database schemas for each service
├── prometheus.yml             # Config to scrape actuator metrics from all services
├── test-saga.sh               # Interactive Bash UI to test the Saga flows
└── pom.xml                    # Parent Multi-Module POM (Manages all 6 services)
```

---

## 🛠 How to Run

1. **Start the Infrastructure (Docker)**
   ```bash
   docker compose up -d
   ```
2. **Start the Services (Open 6 Terminal Tabs)**
   ```bash
   ./mvnw spring-boot:run -pl api-gateway
   ./mvnw spring-boot:run -pl order-service
   ./mvnw spring-boot:run -pl inventory-service
   ./mvnw spring-boot:run -pl payment-service
   ./mvnw spring-boot:run -pl fulfillment-service
   ./mvnw spring-boot:run -pl notification-service
   ```
3. **Run the Interactive Tester**
   ```bash
   ./test-saga.sh
   ```

---

## 📖 Expanding the Architecture: Adding a "Catalog Service"

Our current architecture uses **Kafka for asynchronous WRITES** (state mutations). 
But what if you need to build a new service (like a `Catalog Service`) that requires **synchronous READS**? 

Best practice dictates using **REST** for external clients (frontend/mobile) and **gRPC** for lightning-fast internal service-to-service communication. Here is exactly how you do both.

### 1. External Communication: Exposing a REST API
If the frontend needs to fetch a product, it shouldn't use Kafka. You build a standard Spring Boot REST controller in your new `catalog-service`.

**CatalogController.java (catalog-service)**
```java
@RestController
@RequestMapping("/products")
public class CatalogController {

    @GetMapping("/{id}")
    public Product getProduct(@PathVariable String id) {
        return new Product(id, "Laptop", 999.99);
    }
}
```

**Route it through the API Gateway!**
To let the frontend reach it, you simply update `api-gateway/src/main/resources/application.yml` and add a new route:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: http://localhost:8081
          predicates:
            - Path=/orders/**
            
        # NEW ROUTE FOR CATALOG SERVICE
        - id: catalog-service
          uri: http://localhost:8086 # Assuming catalog-service runs on 8086
          predicates:
            - Path=/products/**
```
*Now the frontend can call `http://localhost:8080/products/123` securely.*

### 2. Internal Communication: Using gRPC
If the `Order Service` needs to quickly verify a product's price *before* accepting an order, doing it via REST is slow, and doing it via Kafka is too complex for a simple read. This is where gRPC shines.

**Step A: Add the Dependency**
Add the gRPC Spring Boot starter to both `catalog-service` and `order-service`.
```xml
<dependency>
    <groupId>net.devh</groupId>
    <artifactId>grpc-spring-boot-starter</artifactId>
    <version>3.0.0.RELEASE</version>
</dependency>
```

**Step B: Expose the gRPC Server (catalog-service)**
```java
import net.devh.boot.grpc.server.service.GrpcService;
import io.grpc.stub.StreamObserver;

@GrpcService
public class CatalogGrpcService extends CatalogServiceGrpc.CatalogServiceImplBase {
    
    @Override
    public void getProduct(ProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        // Fetch from database
        ProductResponse response = ProductResponse.newBuilder()
                .setId(request.getId())
                .setPrice(999.99)
                .build();
                
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
```

**Step C: Call the gRPC Server (order-service)**
Now, the Order Service can fetch the data over HTTP/2 synchronously with zero boilerplate.
```java
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class OrderValidator {

    // Automatically injects the gRPC stub to call the Catalog Service
    @GrpcClient("catalog-service")
    private CatalogServiceGrpc.CatalogServiceBlockingStub catalogStub;

    public boolean validatePrice(String productId, double expectedPrice) {
        // Synchronous gRPC call
        ProductResponse response = catalogStub.getProduct(
            ProductRequest.newBuilder().setId(productId).build()
        );
        
        return response.getPrice() == expectedPrice;
    }
}
```

### The Golden Rule of this Architecture
- Use **Kafka (Outbox/Saga)** for **WRITES** (Commands).
- Use **REST (API Gateway)** for **EXTERNAL READS**.
- Use **gRPC** for **INTERNAL READS**.
