package com.pleased.ticket.dispatcher.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Profile("!test") // active when NOT in test
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String JWT_SECRET = "a-string-secret-at-least-256-bits-long";

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf().disable()
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/v1/**").authenticated()
                        .anyExchange().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt ->
                                jwt.jwtDecoder(jwtDecoder()))
                )
                .build();
    }

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        byte[] secretBytes = JWT_SECRET.getBytes(StandardCharsets.UTF_8);

        return NimbusReactiveJwtDecoder.withSecretKey(
                        new SecretKeySpec(secretBytes, "HmacSHA256")
                )
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
