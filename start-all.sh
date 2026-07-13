#!/bin/bash
export PATH=$PATH:/usr/local/bin

# Define colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}====================================================${NC}"
echo -e "${BLUE}        STARTING DISTRIBUTED MICROSERVICES          ${NC}"
echo -e "${BLUE}====================================================${NC}"

echo -e "\n${GREEN}[1/3] Starting Docker Compose Infrastructure...${NC}"
docker compose up -d

echo -e "\nWaiting for Kafka and Postgres to be ready..."
sleep 10

echo -e "\n${GREEN}Waiting for Keycloak to be ready (this can take 30-60s)...${NC}"
KEYCLOAK_URL="http://localhost:8180/realms/ecommerce-realm/.well-known/openid-configuration"
MAX_WAIT=120
WAITED=0
until curl -s --fail "$KEYCLOAK_URL" > /dev/null 2>&1; do
  if [ $WAITED -ge $MAX_WAIT ]; then
    echo "❌ Keycloak did not become ready after ${MAX_WAIT}s. Check: docker logs saga-keycloak"
    exit 1
  fi
  echo "  ⏳ Keycloak not ready yet... retrying in 5s (${WAITED}s elapsed)"
  sleep 5
  WAITED=$((WAITED + 5))
done
echo "✅ Keycloak is ready!"

echo -e "\n${GREEN}[2/3] Preparing Log Directory...${NC}"
mkdir -p logs
echo "All Spring Boot logs will be saved to the ./logs/ directory."

echo -e "\n${GREEN}[3/3] Starting 6 Spring Boot Services in the background...${NC}"

./mvnw spring-boot:run -pl api-gateway > logs/api-gateway.log 2>&1 &
echo "✅ Started API Gateway (Port 8080)"

./mvnw spring-boot:run -pl order-service > logs/order-service.log 2>&1 &
echo "✅ Started Order Service (Port 8081)"

./mvnw spring-boot:run -pl inventory-service > logs/inventory-service.log 2>&1 &
echo "✅ Started Inventory Service (Port 8082)"

./mvnw spring-boot:run -pl payment-service > logs/payment-service.log 2>&1 &
echo "✅ Started Payment Service (Port 8083)"

./mvnw spring-boot:run -pl fulfillment-service > logs/fulfillment-service.log 2>&1 &
echo "✅ Started Fulfillment Service (Port 8084)"

./mvnw spring-boot:run -pl notification-service > logs/notification-service.log 2>&1 &
echo "✅ Started Notification Service (Port 8085)"

echo -e "\n${BLUE}====================================================${NC}"
echo -e "${GREEN}  ALL SERVICES DEPLOYED AND STARTING!               ${NC}"
echo -e "${BLUE}====================================================${NC}"
echo "It may take ~10-15 seconds for all JVMs to fully initialize."
echo "You can monitor the startup of any service by running:  tail -f logs/order-service.log"
echo "To shut down all services later, run:  pkill -f 'spring-boot:run'"
echo ""
echo "Once initialized, run ./test-saga.sh to test!"
