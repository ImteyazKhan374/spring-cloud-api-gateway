package com.tcs.apigateway.config;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange; // Still needed for type casting the attribute

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import reactor.core.publisher.Mono;

@Component
public class GlobalFallbackHandler {

	@Bean
	RouterFunction<ServerResponse> fallbackRoute() {
		// Define a route that matches requests to /fallback/{serviceName}
		// and delegates handling to the fallbackResponse method.
		return RouterFunctions.route(RequestPredicates.path("/fallback/{serviceName}"), this::fallbackResponse);
	}

	// This method now returns Mono<ServerResponse> as required by RouterFunction
	public Mono<ServerResponse> fallbackResponse(ServerRequest request) {
		String serviceName = request.pathVariable("serviceName");

		// Access the ServerWebExchange from the ServerRequest
		ServerWebExchange exchange = request.exchange();

		// Retrieve the exception that caused the fallback from the exchange attributes
		// This attribute is set by Spring Cloud Gateway's CircuitBreaker filter
		Throwable exception = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);

		String message;
		HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE; // Default status for service unavailability

		// Determine the specific error message based on the exception type
		if (exception instanceof TimeoutException) {
			message = "Service '" + serviceName + "' timed out after 5 seconds.";
		} else if (exception instanceof CallNotPermittedException) {
			// This exception occurs when the circuit breaker is open (too many failures)
			message = "Service '" + serviceName + "' circuit is open. Please try again later.";
		} else if (exception instanceof UnknownHostException) {
			// This can happen if the service ID cannot be resolved (e.g., service not
			// registered with Eureka)
			message = "Service '" + serviceName + "' not found or unreachable.";
		} else if (exception instanceof ConnectException) {
			// This can happen if the service is down and connection is refused
			message = "Service '" + serviceName + "' is currently down or unreachable.";
		} else if (exception != null) {
			// Generic message for other types of exceptions
			message = "Service '" + serviceName + "' failed due to: " + exception.getClass().getSimpleName() + ".";
			// Optionally, you can include the exception message for more detail (be
			// cautious in production)
			// message += " Details: " + exception.getMessage();
		} else {
			// Fallback for cases where no specific exception is found
			message = "Service '" + serviceName + "' failed due to an unknown reason.";
		}

		// Build and return the ServerResponse directly
		return ServerResponse.status(status)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(Map.of("message", message));
	}
}