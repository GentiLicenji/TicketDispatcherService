package com.pleased.ticket.dispatcher.server.filter;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Hashmap implementation is NOT suitable for a production ready distributed service.
 * <p>
 * To be replaced with an in memory data store solution like Redis.
 */
@Component
public class IdempotencyFilter implements WebFilter {

    private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // Extract idempotency key from headers
        List<String> idempotencyHeaders = request.getHeaders().get("Idempotency-Key");
        String idempotencyKey = (idempotencyHeaders != null && !idempotencyHeaders.isEmpty())
                ? idempotencyHeaders.get(0) : null;

        // Check if we have a cached response
        if (idempotencyKey != null && cache.containsKey(idempotencyKey)) {
            CachedResponse cached = cache.get(idempotencyKey);

            // Set response status and headers
            response.setStatusCode(org.springframework.http.HttpStatus.valueOf(cached.getStatus()));
            response.getHeaders().add("Content-Type", "application/json");

            // Write cached response body
            DataBufferFactory bufferFactory = response.bufferFactory();
            DataBuffer buffer = bufferFactory.wrap(cached.getBody().getBytes(StandardCharsets.UTF_8));

            return response.writeWith(Mono.just(buffer));
        }

        // If no cached response, proceed with the request and capture the response
        if (idempotencyKey != null) {
            return captureAndCacheResponse(exchange, chain, idempotencyKey);
        } else {
            // No idempotency key, just proceed normally
            return chain.filter(exchange);
        }
    }

    private Mono<Void> captureAndCacheResponse(ServerWebExchange exchange, WebFilterChain chain, String idempotencyKey) {
        ServerHttpResponse response = exchange.getResponse();
        DataBufferFactory bufferFactory = response.bufferFactory();

        // Create a decorated response to capture the body
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(response) {
            @Override
            public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
                return DataBufferUtils.join(body)
                        .flatMap(dataBuffer -> {
                            // Extract response body as string
                            byte[] content = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(content);
                            String responseBody = new String(content, StandardCharsets.UTF_8);

                            // Cache the response
                            int statusCode = response.getStatusCode() != null ? response.getStatusCode().value() : 200;
                            cache.put(idempotencyKey, new CachedResponse(statusCode, responseBody));

                            // Release the original buffer and create a new one for writing
                            DataBufferUtils.release(dataBuffer);
                            DataBuffer newBuffer = bufferFactory.wrap(content);

                            return super.writeWith(Mono.just(newBuffer));
                        });
            }
        };

        // Create new exchange with decorated response
        ServerWebExchange decoratedExchange = exchange.mutate()
                .response(decoratedResponse)
                .build();

        return chain.filter(decoratedExchange);
    }

    private static class ServerHttpResponseDecorator implements ServerHttpResponse {
        private final ServerHttpResponse delegate;

        public ServerHttpResponseDecorator(ServerHttpResponse delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean setStatusCode(HttpStatus status) {
            return delegate.setStatusCode(status);
        }

        @Override
        public HttpStatus getStatusCode() {
            return delegate.getStatusCode();
        }

        // This method signature changed in Spring Boot 2.7.x
        @Override
        public boolean setRawStatusCode(Integer value) {
            return delegate.setRawStatusCode(value);
        }

        // This method signature changed in Spring Boot 2.7.x
        @Override
        public Integer getRawStatusCode() {
            return delegate.getRawStatusCode();
        }

        @Override
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }

        @Override
        public DataBufferFactory bufferFactory() {
            return delegate.bufferFactory();
        }

        // Changed signature - now takes Supplier instead of Runnable
        @Override
        public void beforeCommit(Supplier<? extends Mono<Void>> action) {
            delegate.beforeCommit(action);
        }

        @Override
        public boolean isCommitted() {
            return delegate.isCommitted();
        }

        @Override
        public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
            return delegate.writeWith(body);
        }

        @Override
        public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
            return delegate.writeAndFlushWith(body);
        }

        @Override
        public Mono<Void> setComplete() {
            return delegate.setComplete();
        }

        // This method signature changed - now returns MultiValueMap
        @Override
        public MultiValueMap<String, ResponseCookie> getCookies() {
            return delegate.getCookies();
        }

        @Override
        public void addCookie(ResponseCookie cookie) {
            delegate.addCookie(cookie);
        }
    }

    // Inner class to represent cached response
    private static class CachedResponse {
        private final int status;
        private final String body;

        public CachedResponse(int status, String body) {
            this.status = status;
            this.body = body;
        }

        public int getStatus() {
            return status;
        }

        public String getBody() {
            return body;
        }
    }
}