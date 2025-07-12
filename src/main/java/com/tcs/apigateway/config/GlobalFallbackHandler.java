package com.tcs.apigateway.config;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Value; // Import for @Value
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
import org.springframework.web.server.ServerWebExchange;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import reactor.core.publisher.Mono;

//@Component
public class GlobalFallbackHandler {

//    // Inject fallback messages from application.yml
//    @Value("${fallback-messages.timeout:Service '%s' timed out.}")
//    private String timeoutMessage;
//
//    @Value("${fallback-messages.circuit-open:Service '%s' circuit is open.}")
//    private String circuitOpenMessage;
//
//    @Value("${fallback-messages.not-found:Service '%s' not found or unreachable.}")
//    private String notFoundMessage;
//
//    @Value("${fallback-messages.connection-refused:Service '%s' is currently down.}")
//    private String connectionRefusedMessage;
//
//    @Value("${fallback-messages.unknown:Service '%s' failed due to an unknown reason.}")
//    private String unknownMessage;
//
//	@Bean
//	RouterFunction<ServerResponse> fallbackRoute() {
//		return RouterFunctions.route(RequestPredicates.path("/fallback/{serviceName}"), this::fallbackResponse);
//	}
//
//	public Mono<ServerResponse> fallbackResponse(ServerRequest request) {
//		String serviceName = request.pathVariable("serviceName");
//		ServerWebExchange exchange = request.exchange();
//
//		Throwable exception = exchange.getAttribute(ServerWebExchangeUtils.CIRCUITBREAKER_EXECUTION_EXCEPTION_ATTR);
//
//		String message;
//		HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
//
//		if (exception instanceof TimeoutException) {
//			message = String.format(timeoutMessage, serviceName);
//		} else if (exception instanceof CallNotPermittedException) {
//			message = String.format(circuitOpenMessage, serviceName);
//		} else if (exception instanceof UnknownHostException) {
//			message = String.format(notFoundMessage, serviceName);
//		} else if (exception instanceof ConnectException) {
//			message = String.format(connectionRefusedMessage, serviceName);
//		} else {
//			message = String.format(unknownMessage, serviceName);
//		}
//
//		return ServerResponse.status(status)
//				.contentType(MediaType.APPLICATION_JSON)
//				.bodyValue(Map.of("message", message));
//	}
}
