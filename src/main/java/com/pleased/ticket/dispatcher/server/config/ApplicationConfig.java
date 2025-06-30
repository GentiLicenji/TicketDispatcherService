package com.pleased.ticket.dispatcher.server.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pleased.ticket.dispatcher.server.filter.IdempotencyFilter;
import com.pleased.ticket.dispatcher.server.filter.LoggingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class ApplicationConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * Global REST Request / Response Reactive Filters.
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
}
