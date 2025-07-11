package com.tcs.apigateway.config;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.NotFoundException; // Specific for 404s if service not found
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException; // For HTTP status exceptions
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import reactor.core.publisher.Mono;

// @Order(Integer.MIN_VALUE) ensures this handler is processed first
//@Configuration
//@Order(-1) // Higher precedence than default handlers
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        String serviceName = exchange.getRequest().getHeaders().getFirst("X-SERVICE-NAME");
        if (serviceName == null) {
            // If X-SERVICE-NAME header is not present, try to get it from route attributes
        	 Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
             if (route != null) {
                 serviceName = route.getId(); // Get the service ID from the Route object
             } else {
                 serviceName = "unknown-service"; // Fallback if route attribute is not found
             }            
        }


        String message;
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR; // Default status

        // Determine the specific error message based on the exception type
        if (ex instanceof TimeoutException) {
            message = "Service '" + serviceName + "' timed out.";
            status = HttpStatus.GATEWAY_TIMEOUT; // 504 Gateway Timeout
        } else if (ex instanceof CallNotPermittedException) {
            message = "Service '" + serviceName + "' circuit is open. Please try again later.";
            status = HttpStatus.SERVICE_UNAVAILABLE; // 503 Service Unavailable
        } else if (ex instanceof ConnectException) {
            message = "Service '" + serviceName + "' is currently down or unreachable.";
            status = HttpStatus.SERVICE_UNAVAILABLE; // 503 Service Unavailable
        } else if (ex instanceof UnknownHostException) {
            message = "Service '" + serviceName + "' not found or unreachable.";
            status = HttpStatus.SERVICE_UNAVAILABLE; // 503 Service Unavailable
        } else if (ex instanceof NotFoundException) {
            // This can be thrown by Spring Cloud Gateway if a route is not found or service is not discovered
            message = "Service '" + serviceName + "' not found or route is misconfigured.";
            status = HttpStatus.NOT_FOUND; // 404 Not Found
        } else if (ex instanceof ResponseStatusException) {
            // Catches exceptions like WebClientResponseException (e.g., 4xx/5xx from downstream)
            status = (HttpStatus) ((ResponseStatusException) ex).getStatusCode();
            message = "Service '" + serviceName + "' returned an error: " + ex.getMessage();
        }
        else {
            // Generic message for other types of exceptions
            message = "An unexpected error occurred with service '" + serviceName + "'.";
            // For debugging, you might include ex.getMessage() but be careful in production
            // message += " Details: " + ex.getMessage();
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> errorBody = new HashMap<>();
        errorBody.put("message", message);
        errorBody.put("timestamp", String.valueOf(System.currentTimeMillis()));
        errorBody.put("status", String.valueOf(status.value()));
        errorBody.put("error", status.getReasonPhrase());
        errorBody.put("path", exchange.getRequest().getPath().value());

        // Write the error response body
        DataBufferFactory bufferFactory = response.bufferFactory();
        return response.writeWith(Mono.fromCallable(() -> {
            try {
                return bufferFactory.wrap(objectMapper.writeValueAsBytes(errorBody));
            } catch (JsonProcessingException e) {
                // Fallback if JSON serialization fails
                return bufferFactory.wrap("{\"message\":\"Internal Server Error\"}".getBytes());
            }
        }));
    }
}
