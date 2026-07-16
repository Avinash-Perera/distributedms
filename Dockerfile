# Stage 1: Build the application
FROM maven:3.9-amazoncorretto-21 AS builder
WORKDIR /app

# Copy the parent pom and all module directories
COPY pom.xml .
COPY grpc-api ./grpc-api
COPY api-gateway ./api-gateway
COPY order-service ./order-service
COPY inventory-service ./inventory-service
COPY payment-service ./payment-service
COPY fulfillment-service ./fulfillment-service
COPY notification-service ./notification-service

# The module to build is passed as a build argument
ARG MODULE
RUN mvn clean package -pl ${MODULE} -am -DskipTests

# Stage 2: Create the runtime image
FROM amazoncorretto:21-alpine
WORKDIR /app

ARG MODULE

# Copy the jar from the builder stage
# We use a wildcard because the version number in the jar filename might change
COPY --from=builder /app/${MODULE}/target/*.jar app.jar

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
