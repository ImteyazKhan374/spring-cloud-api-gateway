package com.tcs.apigateway.config;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {

	@Bean
	RouteLocator dynamicRoutes(RouteLocatorBuilder builder, DiscoveryClient discoveryClient) {
		RouteLocatorBuilder.Builder routes = builder.routes();

		// Dynamically create routes for each discovered service
		discoveryClient.getServices().forEach(serviceId -> {
			routes.route(serviceId,
					r -> r.path("/" + serviceId.toLowerCase() + "/**") // Matches paths like /unison/**
							.filters(f -> f.stripPrefix(1) // Removes the first path segment (e.g., /unison)
									.circuitBreaker(c -> c.setName(serviceId + "-CB") // Apply Circuit Breaker with a unique name
											// Forward to the centralized fallback handler
											.setFallbackUri("forward:/fallback/" + serviceId))
									.filter((exchange, chain) -> {
										// Add a custom header to the request for traceability (optional)
										exchange.getRequest().mutate().header("X-SERVICE-NAME", serviceId).build();
										return chain.filter(exchange);
									}))
							.uri("lb://" + serviceId)); // Route to the load-balanced service
		});

		return routes.build();
	}
}