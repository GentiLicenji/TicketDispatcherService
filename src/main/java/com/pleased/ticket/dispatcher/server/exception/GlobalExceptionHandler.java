package com.pleased.ticket.dispatcher.server.exception;

import com.pleased.ticket.dispatcher.server.model.rest.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import reactor.core.publisher.Mono;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
@Order(-1)
public class GlobalExceptionHandler {

    /**
     * Method handles missing required request parameters and reading failures on rest payload.
     * <p>
     * Method logs and returns error response.
     */
    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleServerWebInputException(ServerWebInputException ex, ServerWebExchange exchange) {
        String requestId = generateRequestId();

        ErrorResponse errorResponse = new ErrorResponse()
                .timestamp(System.currentTimeMillis())
                .path(exchange.getRequest().getPath().value())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getReason() != null ? ex.getReason() : "Invalid request input")
                .requestId(requestId);

        log.error("Server web input exception. RequestId: {}, Message: {}", requestId, ex.getMessage());

        Throwable cause = ex.getCause();
        if (cause instanceof ConversionFailedException) {
            ConversionFailedException cfe = (ConversionFailedException) cause;
            String requiredType = cfe.getTargetType().getType().getTypeName();
            Object value = cfe.getValue();

            log.error(" requiredType:{} - value:{}", requiredType, value);
        } else if (cause instanceof TypeMismatchException) {
            TypeMismatchException tme = (TypeMismatchException) cause;
            Object value = tme.getValue();
            Class<?> requiredType = tme.getRequiredType();
            String requiredTypeName = requiredType != null ? requiredType.getTypeName() : "unknown";

            log.error(" requiredType:{} - value:{}", requiredTypeName, value);
        }

        return Mono.just(new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST));
    }

    /**
     * Method logs and returns error response for schema validation failures on rest payload.
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleWebExchangeBindException(WebExchangeBindException ex, ServerWebExchange exchange) {
        String requestId = generateRequestId();
        String errorMessage = "Validation failed. Check logs for more details.";

        ErrorResponse errorResponse = new ErrorResponse()
                .timestamp(System.currentTimeMillis())
                .path(exchange.getRequest().getPath().value())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(errorMessage)
                .requestId(requestId);

        log.error("Web exchange bind exception. RequestId: {}, Validation errors: {}", requestId, ex.getBindingResult().getAllErrors());
        return Mono.just(new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST));
    }

    /**
     * NOTE: We wouldn't want to propagate detailed validation messages outward as it is a potential vulnerability.
     * <p>
     * It can expose field names, validation rules, and internal model structure.
     *
     * @param violationException Bean validation failure (JSR-303/380)
     * @return error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleConstraintViolationException(ConstraintViolationException violationException, ServerWebExchange exchange) {
        String requestId = generateRequestId();
        String detailedErrorMessage = prepareValidationErrorMessage(violationException.getConstraintViolations());
        String errorMessage = "Constraint violation. Schema validation errors. Check logs for more details.";

        ErrorResponse errorResponse = new ErrorResponse()
                .timestamp(System.currentTimeMillis())
                .path(exchange.getRequest().getPath().value())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(errorMessage)
                .requestId(requestId);

        log.error("Constraint violation exception. RequestId: {}, Validation errors: {}", requestId, detailedErrorMessage);
        return Mono.just(new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST));
    }

    /**
     * Method handles the wrong media type for API.
     */
    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleUnsupportedMediaType(UnsupportedMediaTypeStatusException ex, ServerWebExchange exchange) {
        String requestId = generateRequestId();
        String errorMessage = "Unsupported media type: " + ex.getContentType();

        ErrorResponse errorResponse = new ErrorResponse()
                .timestamp(System.currentTimeMillis())
                .path(exchange.getRequest().getPath().value())
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .error(HttpStatus.UNSUPPORTED_MEDIA_TYPE.getReasonPhrase())
                .message(errorMessage)
                .requestId(requestId);

        log.error("Unsupported media type exception. RequestId: {}, Content type: {}", requestId, ex.getContentType());
        return Mono.just(new ResponseEntity<>(errorResponse, HttpStatus.UNSUPPORTED_MEDIA_TYPE));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex, ServerWebExchange exchange) {
        String requestId = generateRequestId();

        ErrorResponse errorResponse = new ErrorResponse()
                .timestamp(System.currentTimeMillis())
                .path(exchange.getRequest().getPath().value())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Internal server error. Check logs for more details.")
                .requestId(requestId);

        log.error("Unexpected error. RequestId: {}, Exception: ", requestId, ex);
        return Mono.just(new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * Convenience method to generate a unique request ID
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Convenience method to join validation error messages in a single string
     */
    private String prepareValidationErrorMessage(Set<ConstraintViolation<?>> violations) {
        StringBuilder validationMessage = new StringBuilder();
        if (!violations.isEmpty()) {
            for (ConstraintViolation<?> violation : violations) {
                validationMessage.append(violation.getMessage()).append("; ");
            }
            return validationMessage.toString();
        } else {
            return "No validation error messages were provided.";
        }
    }
}