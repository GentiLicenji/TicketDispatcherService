package com.pleased.ticket.dispatcher.server.controller;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.proc.BadJWSException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;
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
        String token = generateValidJwt();
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
        String token = generateInvalidJwt();
        log.info("Generated invalid JWT: {}", token);

        StepVerifier.create(jwtDecoder.decode(token))
                .verifyError(BadJWSException.class);
//        TODO: needs fixing
    }

    @Test
    void testEndpointWithValidToken() throws JOSEException {
        String token = generateValidJwt();

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
    void testEndpointWithInvalidToken() {
        String token = generateInvalidJwt();

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
    void testEndpointWithoutToken() {
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

    private String generateValidJwt() throws JOSEException {
        // Create the JWT claims
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("test-user")
                .claim("scope", "read write")
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 3600 * 1000)) // 1 hour
                .build();

        // Create HMAC signer
        JWSSigner signer = new MACSigner(JWT_SECRET.getBytes());

        // Prepare JWS object
        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader(JWSAlgorithm.HS256),
                claims
        );

        // Compute the HMAC signature
        signedJWT.sign(signer);

        // Serialize to compact form
        return signedJWT.serialize();
    }

    private String generateInvalidJwt() {
        String header = base64UrlEncode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());

        long now = System.currentTimeMillis() / 1000;
        String payload = base64UrlEncode(
                ("{\"sub\":\"test-user\",\"scope\":\"read write\",\"iat\":" + now +
                        ",\"exp\":" + (now + 3600) + "}").getBytes());

        String content = header + "." + payload;
        byte[] signature = hmacSha256(content.getBytes(), "wrong-secret".getBytes());
        String encodedSignature = base64UrlEncode(signature);

        return header + "." + payload + "." + encodedSignature;
    }

    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private byte[] hmacSha256(byte[] data, byte[] key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create HMAC-SHA256 signature", e);
        }
    }
}