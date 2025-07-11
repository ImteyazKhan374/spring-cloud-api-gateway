package com.tcs.apigateway.controller;

import java.net.ConnectException; // Import for connection refused errors
import java.net.UnknownHostException; // Import for service not found errors
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils; // Import for accessing exchange attributes
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange; // Import for ServerWebExchange

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import reactor.core.publisher.Mono; // Import for reactive return type // Import for timeout exceptions

@RestController
@RequestMapping("/fallback")
public class GlobalFallbackController {

	// The fallback method now only takes the serviceName from the path
	// The exception details are retrieved from the ServerWebExchange
	@GetMapping("/{serviceName}")
	public Mono<ResponseEntity<Map<String, String>>> fallback(
			@PathVariable String serviceName,
			ServerWebExchange exchange) {

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
			// This can happen if the service ID cannot be resolved (e.g., service not registered with Eureka)
			message = "Service '" + serviceName + "' not found or unreachable.";
		} else if (exception instanceof ConnectException) {
			// This can happen if the service is down and connection is refused
			message = "Service '" + serviceName + "' is currently down or unreachable.";
		}
		else if (exception != null) {
			// Generic message for other types of exceptions
			message = "Service '" + serviceName + "' failed due to: " + exception.getClass().getSimpleName() + ".";
			// Optionally, you can include the exception message for more detail (be cautious in production)
			// message += " Details: " + exception.getMessage();
		} else {
			// Fallback for cases where no specific exception is found
			message = "Service '" + serviceName + "' failed due to an unknown reason.";
		}

		// Return a ResponseEntity with the appropriate status and message
		return Mono.just(ResponseEntity.status(status)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("message", message)));
	}
}

