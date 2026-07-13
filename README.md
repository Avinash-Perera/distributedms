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

1. **Start Everything (Infrastructure + All 6 Services)**
   ```bash
   ./start-all.sh
   ```
   *(This boots Postgres, Kafka, Zipkin, Prometheus, and Grafana via Docker, and then launches all 6 Spring Boot applications in the background. Logs are saved in the `/logs` directory).*

2. **Run the Interactive Tester**
   ```bash
   ./test-saga.sh
   ```

3. **Run the gRPC vs REST Performance Benchmark**
   We have added synchronous gRPC and REST communication between the `order-service` and `inventory-service` purely to benchmark the performance difference in interservice communication. Once the services are running, run these commands to trigger 1000 requests each:
   ```bash
   # Test REST latency
   curl "http://localhost:8081/api/test/rest?count=1000"
   
   # Test gRPC latency
   curl "http://localhost:8081/api/test/grpc?count=1000"
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

---

## 🔐 Security Architecture: What We Did, Why, and What Production Looks Like

This section explains every security decision made in this POC, the industry-standard alternatives we deliberately skipped, and the exact steps to upgrade when you are ready to go to production.

### Our Guiding Principles
This POC applies **KISS** (Keep It Simple, Stupid) and **DRY** (Don't Repeat Yourself). Security is integrated but not over-engineered. The architecture is designed so every decision is **explainable and upgradeable** — not thrown away when you go to production.

---

### Decision 1: We Skipped Service Mesh (Istio / Linkerd)

#### What is a Service Mesh?
A Service Mesh like **Istio** or **Linkerd** is a dedicated infrastructure layer that sits alongside your microservices (as a "sidecar" proxy on each pod). It handles:
- **Mutual TLS (mTLS)** automatically between every service pair (zero-trust network)
- Traffic management, retries, and circuit breaking
- Distributed tracing injection
- Fine-grained authorization policies (Service A can call Service B, but not Service C)

#### Why We Skipped It for This POC

> **Justification: Istio is not KISS.** Setting it up correctly requires Kubernetes, a working understanding of `IstioOperator` CRDs, certificate rotation policies, and significant debugging overhead. For a local Docker Compose POC, it would triple the setup complexity with zero educational return on the core Saga and Kafka concepts we are demonstrating.

#### What This Means for Production
When you graduate this system to **Kubernetes (e.g., GKE, EKS, AKS)**, you would add a Service Mesh as an **infrastructure concern** — your Java code does not change at all. Here is exactly what the upgrade path looks like:

**Step 1: Install Istio in your cluster**
```bash
istioctl install --set profile=default -y
kubectl label namespace default istio-injection=enabled
```

**Step 2: Istio auto-injects an Envoy sidecar into every pod**
No code changes. No config changes in your Spring Boot apps. Istio reads your K8s manifests and injects the proxy automatically.

**Step 3: Apply a Peer Authentication policy to enforce mTLS**
```yaml
# Enforces mTLS for all services in the 'default' namespace
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: default
spec:
  mtls:
    mode: STRICT
```

**Step 4: Apply an Authorization Policy for zero-trust**
```yaml
# Only allows the 'order-service' to call the 'inventory-service'
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: inventory-access-policy
  namespace: default
spec:
  selector:
    matchLabels:
      app: inventory-service
  action: ALLOW
  rules:
  - from:
    - source:
        principals: ["cluster.local/ns/default/sa/order-service"]
```

This gives you encrypted, authenticated, and authorized service-to-service traffic with **zero Java code changes**.

---

### Decision 2: Kafka Security — SASL/PLAIN Instead of SASL/OAUTHBEARER

#### What is SASL/OAUTHBEARER with Keycloak?
This is the most enterprise-grade approach to securing Kafka. Instead of maintaining a separate set of Kafka-specific usernames and passwords, **Kafka brokers delegate authentication entirely to Keycloak**. Here is how it works:

```
┌──────────────┐   1. Client Credentials Grant   ┌─────────────┐
│ order-service│ ───────────────────────────────► │  Keycloak   │
│              │ ◄─────────────────────────────── │             │
└──────┬───────┘   2. Returns short-lived JWT      └─────────────┘
       │
       │  3. Connect to Kafka with JWT as the credential
       ▼
┌─────────────┐   4. Broker calls Keycloak JWKS   ┌─────────────┐
│ Kafka Broker│ ───────────────────────────────── │  Keycloak   │
│             │ ◄─────────────────────────────── │  (JWKS URI) │
└─────────────┘   5. Validates token, grants access└─────────────┘
```

**Benefits of OAUTHBEARER:**
- **One identity system** for everything — API calls AND Kafka events
- **Short-lived tokens** (15–60 min) — compromised credentials expire fast
- **No password rotation problem** — rotate Keycloak client secrets in one place
- **Audit trail** — every Kafka connection is traceable to a Keycloak service account

#### Why We Chose SASL/PLAIN for This POC

> **Justification:** Configuring a Confluent Kafka broker to validate JWTs against Keycloak requires writing a custom Java `OAuthBearerLoginCallbackHandler` and `OAuthBearerValidatorCallbackHandler`, compiling them, and injecting them into the Kafka broker's classpath inside the Docker container. This is a non-trivial infrastructure task that would obscure the core Saga pattern this POC is teaching. **SASL/PLAIN is a legitimate, widely-used industry standard** (especially when paired with TLS) and demonstrates the same security concept: unauthenticated clients are rejected.

#### Is SASL/PLAIN Used in Real Production? Yes.
| Approach | Used In Production? | When To Use |
|---|---|---|
| `SASL/PLAIN` | ✅ Yes, with TLS | Small-medium teams, Confluent Cloud, Azure Event Hubs |
| `SASL/SCRAM-SHA-512` | ✅ Yes, widely | On-prem Kafka, credentials hashed, no restart needed for rotation |
| `SASL/OAUTHBEARER` | ✅ Yes, enterprise | Large orgs with centralized IdP (Keycloak, Azure AD, Okta) |
| Service Mesh (mTLS) | ✅ Yes, Kubernetes | Zero-trust networks, cloud-native deployments |

---

### How to Upgrade Kafka to SASL/OAUTHBEARER with Keycloak

When you are ready to implement the production-grade approach, here is the exact upgrade path.

#### Step 1: Add a Kafka Service Account in Keycloak
In the `ecommerce-realm`, create a new **confidential client** for Kafka itself and each microservice that connects to it.

```
Keycloak Admin UI → ecommerce-realm → Clients → Create Client
  Client ID:         kafka-broker
  Client Type:       Confidential
  Service Account:   Enabled (enables Client Credentials Grant)
```

Do the same for each microservice: `order-service-kafka`, `inventory-service-kafka`, etc.

#### Step 2: Write the Custom Callback Handler (Java)
Confluent Kafka does not natively know how to talk to Keycloak. You write a small handler that fetches and refreshes Keycloak tokens.

```java
// src/main/java/com/avinash/kafka/KeycloakOAuthBearerLoginCallbackHandler.java
public class KeycloakOAuthBearerLoginCallbackHandler implements AuthenticateCallbackHandler {

    private String tokenEndpoint;
    private String clientId;
    private String clientSecret;

    @Override
    public void configure(Map<String, ?> configs, String mechanism, List<AppConfigurationEntry> entries) {
        // Read values from JAAS config entries
        this.tokenEndpoint = (String) configs.get("oauth.token.endpoint.uri");
        this.clientId      = (String) configs.get("oauth.client.id");
        this.clientSecret  = (String) configs.get("oauth.client.secret");
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException {
        for (Callback callback : callbacks) {
            if (callback instanceof OAuthBearerTokenCallback) {
                // Fetch token from Keycloak using Client Credentials Grant
                String token = fetchTokenFromKeycloak();
                ((OAuthBearerTokenCallback) callback).token(new OAuthBearerTokenJwt(token));
            }
        }
    }

    private String fetchTokenFromKeycloak() {
        // POST to tokenEndpoint with client_id, client_secret, grant_type=client_credentials
        // Returns the access_token string
        // Use RestTemplate or Apache HttpClient here
        return "...";
    }
}
```

#### Step 3: Configure the Kafka Broker in compose.yaml
```yaml
kafka:
  environment:
    # Enable OAUTHBEARER on the internal SASL listener
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:SASL_PLAINTEXT,PLAINTEXT_HOST:SASL_PLAINTEXT
    KAFKA_SASL_ENABLED_MECHANISMS: OAUTHBEARER
    KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL: OAUTHBEARER
    
    # Point the broker to Keycloak's JWKS endpoint to validate incoming tokens
    KAFKA_LISTENER_NAME_PLAINTEXT_OAUTHBEARER_SASL_JAAS_CONFIG: |
      org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required
      oauth.jwks.endpoint.uri="http://keycloak:8080/realms/ecommerce-realm/protocol/openid-connect/certs"
      oauth.valid.issuer.uri="http://keycloak:8080/realms/ecommerce-realm";
    KAFKA_LISTENER_NAME_PLAINTEXT_OAUTHBEARER_SASL_SERVER_CALLBACK_HANDLER_CLASS: >
      org.apache.kafka.common.security.oauthbearer.secured.OAuthBearerValidatorCallbackHandler
```

#### Step 4: Configure Each Microservice's Kafka Client (application.yml)
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    properties:
      security.protocol: SASL_PLAINTEXT
      sasl.mechanism: OAUTHBEARER
      sasl.jaas.config: |
        org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required
        oauth.token.endpoint.uri="http://localhost:8180/realms/ecommerce-realm/protocol/openid-connect/token"
        oauth.client.id="order-service-kafka"
        oauth.client.secret="your-client-secret";
      sasl.login.callback.handler.class: com.avinash.kafka.KeycloakOAuthBearerLoginCallbackHandler
```

#### The Result
Every microservice authenticates to Kafka using a Keycloak-issued JWT, rotating automatically before expiry. A compromised service account can be **immediately revoked in Keycloak** — instantly blocking that service from producing or consuming any Kafka events, with no broker restart required.

---

### Security Architecture Summary

```
                         [ INTERNET ]
                              │
                              │ HTTPS + JWT (Keycloak)
                              ▼
                      ┌───────────────┐
                      │  API Gateway  │  ← Validates JWT signature (JWKS)
                      │  :8080        │  ← Relays token downstream
                      └──────┬────────┘
                             │ Bearer Token (relayed)
            ┌────────────────┼─────────────────┐
            ▼                ▼                 ▼
     ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
     │order-service│  │ payment-svc │  │inventory-svc│
     │   :8081     │  │   :8083     │  │   :8082     │  ← Each validates JWT locally
     └──────┬──────┘  └─────────────┘  └─────────────┘
            │ SASL/PLAIN (POC)
            │ SASL/OAUTHBEARER (Production target)
            ▼
     ┌─────────────┐
     │    Kafka    │  ← Auth enforced at broker level
     │   :9092     │
     └─────────────┘
            │ Events
     ┌──────┴──────┐
     ▼             ▼
fulfillment     notification
```

**Production Upgrade Path:**
1. ☑️ **Now (POC):** Keycloak JWT for HTTP, SASL/PLAIN for Kafka
2. ➡️ **Next (Staging):** Swap SASL/PLAIN → SASL/SCRAM-SHA-512 + TLS
3. ➡️ **Production:** SASL/OAUTHBEARER (Keycloak Client Credentials) for Kafka + Kubernetes with Istio mTLS
