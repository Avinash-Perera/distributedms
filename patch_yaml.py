import sys

with open("k8s/03-microservices.yaml", "r") as f:
    content = f.read()

services = {
    "api-gateway": 8080,
    "order-service": 8081,
    "inventory-service": 8082,
    "payment-service": 8083,
    "fulfillment-service": 8084,
    "notification-service": 8085
}

for svc, port in services.items():
    find_str = f"      - name: {svc}\n        image: distributedms-{svc}\n        imagePullPolicy: Never"
    replace_str = f"""      - name: {svc}
        image: distributedms-{svc}
        imagePullPolicy: Never
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: {port}
          initialDelaySeconds: 15
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: {port}
          initialDelaySeconds: 15
          periodSeconds: 10"""
    content = content.replace(find_str, replace_str)

with open("k8s/03-microservices.yaml", "w") as f:
    f.write(content)
