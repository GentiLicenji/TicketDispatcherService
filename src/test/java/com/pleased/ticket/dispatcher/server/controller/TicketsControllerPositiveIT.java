package com.pleased.ticket.dispatcher.server.controller;

import com.nimbusds.jose.JOSEException;
import com.pleased.ticket.dispatcher.server.TestUtil;
import com.pleased.ticket.dispatcher.server.config.DisableSecurityConfig;
import com.pleased.ticket.dispatcher.server.delegate.TicketsDelegate;
import com.pleased.ticket.dispatcher.server.exception.GlobalExceptionHandler;
import com.pleased.ticket.dispatcher.server.filter.CorrelationIdFilter;
import com.pleased.ticket.dispatcher.server.filter.IdempotencyFilter;
import com.pleased.ticket.dispatcher.server.filter.JwtAuthenticationFilter;
import com.pleased.ticket.dispatcher.server.filter.LoggingFilter;
import com.pleased.ticket.dispatcher.server.model.events.TicketCreated;
import com.pleased.ticket.dispatcher.server.model.rest.TicketCreateRequest;
import com.pleased.ticket.dispatcher.server.model.rest.TicketResponse;
import com.pleased.ticket.dispatcher.server.service.TicketEventProducer;
import com.pleased.ticket.dispatcher.server.service.TicketsApiService;
import com.pleased.ticket.dispatcher.server.util.mapper.EventMapper;
import com.pleased.ticket.dispatcher.server.util.mapper.EventMapperImpl;
import com.pleased.ticket.dispatcher.server.util.mapper.TicketsMapperImpl;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderRecord;
import reactor.kafka.sender.SenderResult;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ActiveProfiles("test")
@WebFluxTest
@ContextConfiguration(classes = {TicketsController.class,
        TicketsDelegate.class,
        TicketsApiService.class,
        TicketEventProducer.class,
        CorrelationIdFilter.class,
        IdempotencyFilter.class,
        LoggingFilter.class,
        JwtAuthenticationFilter.class,
        GlobalExceptionHandler.class,
        DisableSecurityConfig.class})
@AutoConfigureWebTestClient
@Import({TicketsMapperImpl.class, EventMapperImpl.class}) // import the generated impl class
public class TicketsControllerPositiveIT {

    @MockBean
    private TicketEventProducer ticketEventProducer;

    @Autowired
    private WebTestClient webTestClient;

    private static UUID ticketId;
    private static UUID projectId;
    private static UUID correlationId;
    private static UUID assigneeId;
    private static UUID idempotencyKey;

    @BeforeAll
    static void setUp() {
        ticketId = UUID.randomUUID();
        projectId = UUID.randomUUID();
        correlationId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();
        idempotencyKey = UUID.randomUUID();
    }

    @BeforeEach
    void setUpEach() {
        // Create mocks
        when(ticketEventProducer.publishTicketCreated(any(TicketCreated.class), eq(correlationId)))
                .thenReturn(Mono.empty());
    }

    @Test
    void createTicket_withValidPayload_ShouldReturnCorrectResponse() throws JOSEException {

        TicketCreateRequest request = new TicketCreateRequest();
        request.setSubject("Test Ticket");
        request.setDescription("This is a test ticket");
        request.setProjectId(projectId.toString());

        // Act
        webTestClient.post()
                .uri("/api/v1/tickets")
                .header("Authorization", "Bearer "+TestUtil.generateValidJwt())
                .header("X-Correlation-ID", correlationId.toString())
                .header("Idempotency-Key", ticketId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                // Assert
                .expectStatus().isOk()
                .expectBody(TicketResponse.class)
                .value(response -> {
                    assertNotNull(response);
                    assertNotNull(response.getTicketId());
                    assertEquals(request.getSubject(), response.getSubject());
                    assertEquals(request.getDescription(), response.getDescription());
                    assertEquals(projectId.toString(), response.getProjectId());
                    assertEquals("open", response.getStatus().toString());
                });
    }


    @Test
    void createTicket_WithDuplicateIdempotencyKey_ShouldReturnSameResponse() throws JOSEException {

        TicketCreateRequest request = new TicketCreateRequest();
        request.setSubject("Test Ticket for Deduplication");
        request.setDescription("This is a test ticket for deduplication");
        request.setProjectId(projectId.toString());

        // Act - First request
        TicketResponse firstResponse = webTestClient.post()
                .uri("/api/v1/tickets")
                .header("Authorization", "Bearer "+TestUtil.generateValidJwt())
                .header("Idempotency-Key", idempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TicketResponse.class)
                .returnResult()
                .getResponseBody();

        // Act - Second request with same idempotency key
        TicketResponse secondResponse = webTestClient.post()
                .uri("/api/v1/tickets")
                .header("Authorization", "Bearer "+TestUtil.generateValidJwt())
                .header("Idempotency-Key", idempotencyKey.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isOk()
                .expectBody(TicketResponse.class)
                .returnResult()
                .getResponseBody();

        // Assert that both responses have the same fields
        assertThat(secondResponse.getTicketId()).isEqualTo(firstResponse.getTicketId());
        assertThat(secondResponse.getSubject()).isEqualTo(firstResponse.getSubject());
        assertThat(secondResponse.getDescription()).isEqualTo(firstResponse.getDescription());
    }
}