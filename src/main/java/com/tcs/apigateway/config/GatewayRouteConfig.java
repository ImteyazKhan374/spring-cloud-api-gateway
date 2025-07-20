// src/main/java/com/tcs/apigateway/config/GatewayRouteConfig.java
package com.tcs.apigateway.config;

import com.tcs.apigateway.filter.CachingGatewayFilterFactory; // Import the new filter
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {

	@Autowired
	private KeyResolver ipKeyResolver; // Or userKeyResolver, depending on your application.yml choice

	// Autowire the custom caching filter factory
	@Autowired
	private CachingGatewayFilterFactory cachingFilterFactory;

	@Bean
	RouteLocator dynamicRoutes(RouteLocatorBuilder builder, DiscoveryClient discoveryClient) {
		RouteLocatorBuilder.Builder routes = builder.routes();

		// The discoveryClient.getServices().forEach loop was commented out,
		// so I'm keeping the explicit "unison" route as per your provided code.
		// If you intend to have dynamic routing for all discovered services,
		// you should uncomment the forEach loop.
		// discoveryClient.getServices().forEach(serviceId -> {
		routes.route("unison",
				r -> r.path("/unison/**").filters(f -> f.stripPrefix(1).circuitBreaker(c -> c.setName("unison-CB"))
						// --- FIX: REMOVED THE requestRateLimiter BLOCK FROM HERE ---
						// The RequestRateLimiter is now applied globally via default-filters in
						// application.yml.
						.filter((exchange, chain) -> {
							exchange.getRequest().mutate().header("X-SERVICE-NAME", "unison").build();
							return chain.filter(exchange);
						})
						// ADD THE CACHING FILTER HERE
						.filter(cachingFilterFactory.apply(new CachingGatewayFilterFactory.Config())))
						.uri("lb://unison"));
		// });

		return routes.build();
	}
}
