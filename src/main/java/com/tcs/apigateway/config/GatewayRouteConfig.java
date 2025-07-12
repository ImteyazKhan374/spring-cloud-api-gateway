package com.tcs.apigateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
// Removed direct import of RedisRateLimiter as it's not instantiated here
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRouteConfig {

	// userKeyResolver is still autowired if you plan to use it in other custom filters
	// or if you switch the default-filters key-resolver in application.yml to #userKeyResolver
	@Autowired
	private KeyResolver userKeyResolver;

	@Bean
	RouteLocator dynamicRoutes(RouteLocatorBuilder builder, DiscoveryClient discoveryClient) {
		RouteLocatorBuilder.Builder routes = builder.routes();

		// The discoveryClient.getServices().forEach loop was commented out,
		// so I'm keeping the explicit "unison" route as per your provided code.
		// If you intend to have dynamic routing for all discovered services,
		// you should uncomment the forEach loop.
		// discoveryClient.getServices().forEach(serviceId -> {
			routes.route("unison",
					r -> r.path("/unison/**")
							.filters(f -> f.stripPrefix(1)
									.circuitBreaker(c -> c.setName("unison-CB"))
									// Removed the problematic .requestRateLimiter() block here.
									// The RequestRateLimiter is now applied via default-filters in application.yml.
									.filter((exchange, chain) -> {
										exchange.getRequest().mutate().header("X-SERVICE-NAME", "unison").build();
										return chain.filter(exchange);
									}))
							.uri("lb://unison"));
		//});

		return routes.build();
	}
}
