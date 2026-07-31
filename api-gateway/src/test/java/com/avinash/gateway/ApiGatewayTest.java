package com.avinash.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiGatewayTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    void testOrderRouteWithoutToken() {
        // Since security is enabled, hitting /orders/** without a token should return 401 Unauthorized
        webClient.get().uri("/orders/123")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
