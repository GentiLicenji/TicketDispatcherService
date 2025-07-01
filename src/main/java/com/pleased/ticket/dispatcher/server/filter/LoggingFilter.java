package com.pleased.ticket.dispatcher.server.filter;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

public class LoggingFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = UUID.randomUUID().toString();
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse originalResponse = exchange.getResponse();

        return DataBufferUtils.join(request.getBody())
                .flatMap(requestBodyBuffer -> {
                    byte[] reqBodyBytes = new byte[requestBodyBuffer.readableByteCount()];
                    requestBodyBuffer.read(reqBodyBytes);
                    DataBufferUtils.release(requestBodyBuffer);

                    String requestBody = new String(reqBodyBytes, StandardCharsets.UTF_8);

                    log.info("Request Log | TraceID: {} | Time: {} | Method: {} | URI: {} | Headers: {} | Body: {}",
                            traceId,
                            Instant.now(),
                            request.getMethod(),
                            request.getURI(),
                            request.getHeaders(),
                            compact(requestBody)
                    );

                    // Wrap request with body cached
                    ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(request) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return Flux.just(
                                    new DefaultDataBufferFactory().wrap(reqBodyBytes)
                            );
                        }
                    };

                    // Decorate response to capture body
                    ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
                        @Override
                        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                            if (body instanceof Flux) {
                                Flux<? extends DataBuffer> flux = (Flux<? extends DataBuffer>) body;
                                return super.writeWith(
                                        flux.buffer().map(dataBuffers -> {
                                            DataBuffer joined = new DefaultDataBufferFactory().join(dataBuffers);
                                            byte[] responseBytes = new byte[joined.readableByteCount()];
                                            joined.read(responseBytes);
                                            DataBufferUtils.release(joined);

                                            String responseBody = new String(responseBytes, StandardCharsets.UTF_8);
                                            HttpStatus status = getStatusCode() != null ? getStatusCode() : HttpStatus.OK;

                                            log.info("Response Log | TraceID: {} | Time: {} | Status: {} | Headers: {} | Body: {}",
                                                    traceId,
                                                    Instant.now(),
                                                    status,
                                                    getHeaders(),
                                                    compact(responseBody)
                                            );

                                            return bufferFactory().wrap(responseBytes);
                                        })
                                );
                            }
                            return super.writeWith(body);
                        }
                    };

                    return chain.filter(exchange.mutate()
                            .request(decoratedRequest)
                            .response(decoratedResponse)
                            .build());
                });
    }

    private String compact(String body) {
        if (body == null || body.trim().isEmpty()) return "[empty]";
        return StringUtils.trimWhitespace(body.replaceAll("[\n\r]+", " "));
    }
}
