package com.pleased.ticket.dispatcher.server.util;

import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Utility class to access user context
 */
@Component
public class ReactiveSecurityContextHolder {

    private static final String USER_ID_CONTEXT_KEY = "userId";

    public static Mono<String> getUserId() {
        return Mono.deferContextual(contextView -> {
            if (contextView.hasKey(USER_ID_CONTEXT_KEY)) {
                return Mono.just(contextView.get(USER_ID_CONTEXT_KEY));
            }
            return Mono.error(new SecurityException("User ID not found in context"));
        });
    }

    public static Mono<String> getUserIdOrEmpty() {
        return Mono.deferContextual(contextView -> {
            if (contextView.hasKey(USER_ID_CONTEXT_KEY)) {
                return Mono.just(contextView.get(USER_ID_CONTEXT_KEY));
            }
            return Mono.empty();
        });
    }
}
