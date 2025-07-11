package com.tcs.apigateway.config;

import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// No change needed for ServerWebExchangeUtils import here, it's used in the controller.

@Configuration
public class GatewayRouteConfig {

	@Bean
	RouteLocator dynamicRoutes(RouteLocatorBuilder builder, DiscoveryClient discoveryClient) {
		RouteLocatorBuilder.Builder routes = builder.routes();

		// Iterate over all discovered services to create dynamic routes
		discoveryClient.getServices().forEach(serviceId -> {
			routes.route(serviceId,
					r -> r.path("/" + serviceId.toLowerCase() + "/**") // Matches paths like /unison/**
							.filters(f -> f.stripPrefix(1) // Removes the first path segment (e.g., /unison)
									.circuitBreaker(c -> c.setName(serviceId + "-CB") // Apply Circuit Breaker with a unique name
											// Simplified fallback URI: forward to /fallback/{serviceId}
											// The actual exception details will be extracted in the GlobalFallbackController
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