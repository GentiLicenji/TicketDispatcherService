package com.pleased.ticket.dispatcher.server.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pleased.ticket.dispatcher.server.filter.CorrelationIdFilter;
import com.pleased.ticket.dispatcher.server.filter.IdempotencyFilter;
import com.pleased.ticket.dispatcher.server.filter.JwtAuthenticationFilter;
import com.pleased.ticket.dispatcher.server.filter.LoggingFilter;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

@Configuration
public class ApplicationConfig {

    /**
     * Shared global object mapper.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * Global REST Reactive Filters.
     */
    @Bean
    @Order(1)
    public LoggingFilter loggingFilter() {
        return new LoggingFilter();
    }

    @Bean
    @Order(2)
    public IdempotencyFilter idempotencyFilter() {
        return new IdempotencyFilter();
    }

    @Bean
    @Order(3)
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    @Order(-100)  // High priority to run early in the filter chain
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter();
    }

    /**
     * DB schema initializer on startup.
     */
    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("h2-db-schema.sql")
        );
        initializer.setDatabasePopulator(populator);
        return initializer;
    }

    /**
     * Warms database by preparing with load test data.
     */
    @Bean
    @Profile("load-test")
    public ConnectionFactoryInitializer warmDataBase(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("seed_load_test_data.sql")
        );
        initializer.setDatabasePopulator(populator);
        return initializer;
    }
}
