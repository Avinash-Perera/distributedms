#!/bin/bash
export PATH=$PATH:/usr/local/bin

# Define colors for the UI
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

clear
echo -e "${BLUE}====================================================${NC}"
echo -e "${BLUE}      SAGA CHOREOGRAPHY INTERACTIVE TESTER UI       ${NC}"
echo -e "${BLUE}====================================================${NC}"
echo ""
echo "Which Saga scenario would you like to visualize?"
echo -e "  ${GREEN}1) The Happy Path (Everything succeeds)${NC}"
echo -e "  ${YELLOW}2) Payment Failure (Inventory & Order Compensate)${NC}"
echo -e "  ${RED}3) Fulfillment Failure (Payment, Inventory & Order Compensate)${NC}"
echo ""
read -p "Enter your choice (1-3): " choice

case $choice in
  1)
    CUST="CUST-12345"
    echo -e "\n${GREEN}[START] Initiating Happy Path for customer: $CUST ${NC}"
    ;;
  2)
    CUST="POOR-GUY"
    echo -e "\n${YELLOW}[START] Initiating Payment Failure for customer: $CUST ${NC}"
    ;;
  3)
    CUST="BAD-ADDRESS"
    echo -e "\n${RED}[START] Initiating Fulfillment Failure for customer: $CUST ${NC}"
    ;;
  *)
    echo "Invalid choice. Exiting."
    exit 1
    ;;
esac

echo -e "\n${BLUE}➤ Step 1: Client sends POST request to API Gateway (Port 8080)...${NC}"
sleep 1
RESPONSE=$(curl -s -X POST http://localhost:8080/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "'$CUST'",
    "customerEmail": "tester@example.com",
    "productId": "PROD-999"
  }')

echo -e "Gateway Response:"
echo "$RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$RESPONSE"

# Extract Order ID using basic string manipulation (fallback if jq isn't installed)
ORDER_ID=$(echo "$RESPONSE" | grep -o '"id":"[^"]*' | cut -d'"' -f4)

if [ -z "$ORDER_ID" ]; then
  echo -e "${RED}Failed to create order! Are the services running?${NC}"
  exit 1
fi

echo -e "\n${BLUE}➤ Step 2: The Gateway responded instantly. The Saga is now choreographing in the background via Kafka!${NC}"
echo -e "Waiting 7 seconds for Kafka events to propagate through all services...\n"

# A fancy loading bar
for i in {1..7}; do
    echo -ne "   Working... [${YELLOW}$(printf '%0.s#' $(seq 1 $i))${NC}$(printf '%0.s.' $(seq $i 7))] \r"
    sleep 1
done
echo -e "\n"

echo -e "${BLUE}➤ Step 3: Fetching the final state across all distributed databases...${NC}"
sleep 1

echo -e "\n${GREEN}--- ORDER DATABASE STATE ---${NC}"
echo "Did the Order Service update the status?"
docker exec saga-postgres psql -U admin -d order_db -t -c "SELECT 'Order ' || id || ' is now: ' || status FROM orders WHERE id = '$ORDER_ID';"

echo -e "\n${GREEN}--- INVENTORY DATABASE STATE ---${NC}"
echo "What is the final stock count for PROD-999? (Started at 100)"
docker exec saga-postgres psql -U admin -d inventory_db -t -c "SELECT 'Product ' || product_id || ' stock: ' || stock FROM inventory WHERE product_id = 'PROD-999';"

if [ "$choice" == "1" ]; then
  echo -e "\n${GREEN}--- FULFILLMENT DATABASE STATE ---${NC}"
  echo "Was a shipment created?"
  docker exec saga-postgres psql -U admin -d fulfillment_db -t -c "SELECT 'Shipment ' || id || ' for order ' || order_id || ' is ' || status FROM shipments WHERE order_id = '$ORDER_ID';"
fi

echo -e "\n${BLUE}====================================================${NC}"
echo -e "${GREEN}                  TEST COMPLETE!                    ${NC}"
echo -e "${BLUE}====================================================${NC}"
echo "Check your terminal running the Notification Service to see the final mock email log!"
