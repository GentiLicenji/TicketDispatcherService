package com.pleased.ticket.dispatcher.server.filter;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.pleased.ticket.dispatcher.server.config.SecurityConfig;
import com.pleased.ticket.dispatcher.server.exception.JWTVerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import java.text.ParseException;
import java.util.Date;

public class JwtAuthenticationFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_CONTEXT_KEY = "userId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // No JWT token, continue without authentication
            return chain.filter(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        return validateJwtAndExtractUserId(token)
                .flatMap(userId -> {
                    // Store userId in reactive context
                    return chain.filter(exchange)
                            .contextWrite(Context.of(USER_ID_CONTEXT_KEY, userId));
                })
                .onErrorResume(ex -> {
                    logger.error("JWT validation failed", ex);
                    // Return 401 Unauthorized
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                });
    }

    private Mono<String> validateJwtAndExtractUserId(String token) {
        return Mono.fromCallable(() -> {
                    try {
                        SignedJWT signedJWT = SignedJWT.parse(token);

                        // Verify signature
                        JWSVerifier verifier = new MACVerifier(SecurityConfig.JWT_SECRET.getBytes());
                        if (!signedJWT.verify(verifier)) {
                            throw new JWTVerificationException("Invalid JWT signature");
                        }

                        // Check expiration
                        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
                        if (claims.getExpirationTime() != null && claims.getExpirationTime().before(new Date())) {
                            throw new JWTVerificationException("JWT token has expired");
                        }

                        // Extract user ID from subject
                        String userId = claims.getSubject();
                        if (userId == null || userId.isEmpty()) {
                            throw new JWTVerificationException("JWT subject (userId) is missing");
                        }

                        return userId;
                    } catch (ParseException | JOSEException e) {
                        throw new JWTVerificationException("JWT validation failed", e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
