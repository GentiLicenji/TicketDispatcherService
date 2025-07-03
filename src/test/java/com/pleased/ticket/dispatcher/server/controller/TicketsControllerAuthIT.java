package com.pleased.ticket.dispatcher.server.controller;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.proc.BadJWSException;
import com.pleased.ticket.dispatcher.server.TestUtil;
import com.pleased.ticket.dispatcher.server.model.rest.TicketCreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class TicketsControllerAuthIT {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReactiveJwtDecoder jwtDecoder;

    private static final String JWT_SECRET = "a-string-secret-at-least-256-bits-long";

    @Test
    void testJwtValidation() throws JOSEException {
        // Generate a token
        String token = TestUtil.generateValidJwt();
        log.info("Generated JWT: {}", token);

        // Test the decoder directly using StepVerifier
        StepVerifier.create(jwtDecoder.decode(token))
                .assertNext(jwt -> {
                    log.info("JWT decoded successfully:");
                    log.info("Subject: {}", jwt.getSubject());
                    log.info("Claims: {}", jwt.getClaims());
                    log.info("Headers: {}", jwt.getHeaders());
                    log.info("Expires at: {}", jwt.getExpiresAt());
                    log.info("Issued at: {}", jwt.getIssuedAt());

                    assertNotNull(jwt);
                    assertEquals("test-user", jwt.getSubject());
                    assertTrue(jwt.getClaims().containsKey("scope"));
                })
                .verifyComplete();
    }

    @Disabled
    @Test
    void testJwtWithInvalidSignature() {
        String token = TestUtil.generateInvalidJwt();
        log.info("Generated invalid JWT: {}", token);

        StepVerifier.create(jwtDecoder.decode(token))
                .verifyError(BadJWSException.class);
//        TODO: needs fixing
    }

    @Test
    void testEndpointWithValidToken_shouldReturnOK() throws JOSEException {
        String token = TestUtil.generateValidJwt();

        TicketCreateRequest request = new TicketCreateRequest();
        request.setSubject("Test Ticket");
        request.setDescription("This is a test ticket");
        request.setProjectId(UUID.randomUUID().toString());

        webTestClient.post()
                .uri("http://localhost:" + port + "/api/v1/tickets")
                .header("Authorization", "Bearer " + token)
                .header("X-Correlation-ID", UUID.randomUUID().toString())
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void testEndpointWithInvalidToken_shouldFailWithUnAuth() {
        String token = TestUtil.generateInvalidJwt();

        TicketCreateRequest request = new TicketCreateRequest();
        request.setSubject("Test Ticket");
        request.setDescription("This is a test ticket");
        request.setProjectId(UUID.randomUUID().toString());

        webTestClient.post()
                .uri("http://localhost:" + port + "/api/v1/tickets")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void testEndpointWithoutToken_shouldFailWithUnAuth() {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setSubject("Test Ticket");
        request.setDescription("This is a test ticket");
        request.setProjectId(UUID.randomUUID().toString());

        webTestClient.post()
                .uri("http://localhost:" + port + "/api/v1/tickets")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isUnauthorized();
    }


}