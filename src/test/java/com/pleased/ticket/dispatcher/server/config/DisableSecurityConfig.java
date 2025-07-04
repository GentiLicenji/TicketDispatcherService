package com.pleased.ticket.dispatcher.server.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Custom Security Config setup to disable the security filter chain by allowing all API calls.
 */
@EnableWebFluxSecurity
@TestConfiguration
@Profile("test") // active when testing
public class DisableSecurityConfig {

    @Bean
    @Primary
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf().disable()
                .authorizeExchange(exchanges -> exchanges
                        .anyExchange().permitAll()  // Allow all requests for local dev
                )
                .build();
    }
}
