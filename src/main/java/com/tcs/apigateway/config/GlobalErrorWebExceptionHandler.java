package com.tcs.apigateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException; // Import for AccessDeniedException
import org.springframework.security.core.AuthenticationException; // Import for AuthenticationException
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@Order(-1) // Higher precedence than default handlers
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        String serviceName = exchange.getRequest().getHeaders().getFirst("X-SERVICE-NAME");
        if (serviceName == null) {
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            if (route != null) {
                serviceName = route.getId();
            } else {
                serviceName = "unknown-service";
            }
        }

        String message;
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (ex instanceof AuthenticationException) {
            message = "Authentication failed: Invalid or missing token.";
            status = HttpStatus.UNAUTHORIZED; // 401 Unauthorized
        } else if (ex instanceof AccessDeniedException) {
            message = "Authorization failed: You do not have permission to access this resource.";
            status = HttpStatus.FORBIDDEN; // 403 Forbidden
        } else if (ex instanceof TimeoutException) {
            message = "Service '" + serviceName + "' timed out.";
            status = HttpStatus.GATEWAY_TIMEOUT;
        } else if (ex instanceof CallNotPermittedException) {
            message = "Service '" + serviceName + "' circuit is open. Please try again later.";
            status = HttpStatus.SERVICE_UNAVAILABLE;
        } else if (ex instanceof ConnectException) {
            message = "Service '" + serviceName + "' is currently down or unreachable.";
            status = HttpStatus.SERVICE_UNAVAILABLE;
        } else if (ex instanceof UnknownHostException) {
            message = "Service '" + serviceName + "' not found or unreachable.";
            status = HttpStatus.SERVICE_UNAVAILABLE;
        } else if (ex instanceof NotFoundException) {
            message = "Service '" + serviceName + "' not found or route is misconfigured.";
            status = HttpStatus.NOT_FOUND;
        } else if (ex instanceof ResponseStatusException) {
            status = (HttpStatus) ((ResponseStatusException) ex).getStatusCode();
            message = "Service '" + serviceName + "' returned an error: " + ex.getMessage();
        } else {
            message = "An unexpected error occurred with service '" + serviceName + "'.";
        }

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> errorBody = new HashMap<>();
        errorBody.put("message", message);
        errorBody.put("timestamp", String.valueOf(System.currentTimeMillis()));
        errorBody.put("status", String.valueOf(status.value()));
        errorBody.put("error", status.getReasonPhrase());
        errorBody.put("path", exchange.getRequest().getPath().value());

        DataBufferFactory bufferFactory = response.bufferFactory();
        return response.writeWith(Mono.fromCallable(() -> {
            try {
                return bufferFactory.wrap(objectMapper.writeValueAsBytes(errorBody));
            } catch (JsonProcessingException e) {
                return bufferFactory.wrap("{\"message\":\"Internal Server Error\"}".getBytes());
            }
        }));
    }
}