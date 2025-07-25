package com.abkatk.apigateway.filter;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode; // Import HttpStatusCode
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * Custom GatewayFilterFactory for caching API responses. This filter intercepts
 * requests, checks if a cached response exists, serves it if available, or
 * caches the new response if not. It's configured to work with Spring's
 * CacheManager.
 */
@Component
public class CachingGatewayFilterFactory extends AbstractGatewayFilterFactory<CachingGatewayFilterFactory.Config> {

	private static final String CACHE_NAME = "apiResponses"; // Must match the cache name in CacheConfig
	private final CacheManager cacheManager;

	public CachingGatewayFilterFactory(CacheManager cacheManager) {
		super(Config.class);
		this.cacheManager = cacheManager;
	}

	/**
	 * Applies the caching logic to the gateway request.
	 *
	 * @param config The configuration for this filter instance.
	 * @return A GatewayFilter instance.
	 */
	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			ServerHttpRequest request = exchange.getRequest();
			ServerHttpResponse originalResponse = exchange.getResponse();

			// Only cache GET requests
			if (!HttpMethod.GET.equals(request.getMethod())) {
				return chain.filter(exchange);
			}

			// Generate a cache key based on the request URI.
			// For more robust caching, consider including query parameters,
			// relevant headers (e.g., Accept, Authorization if caching per-user), etc.
			String cacheKey = request.getURI().toString();
			Cache cache = cacheManager.getCache(CACHE_NAME);

			if (cache != null) {
				// Try to retrieve the response from the cache
				Cache.ValueWrapper cachedResponseWrapper = cache.get(cacheKey);

				if (cachedResponseWrapper != null) {
					// Cache hit: Serve the cached response
					CachedResponse cachedResponse = (CachedResponse) cachedResponseWrapper.get();
					if (cachedResponse != null) {
						System.out.println("Cache hit for: " + cacheKey);
						// Set status and headers from cached response
						originalResponse.setStatusCode(cachedResponse.getStatus());
						cachedResponse.getHeaders()
								.forEach((name, values) -> originalResponse.getHeaders().put(name, values));

						// Write the cached body to the response
						DataBuffer buffer = originalResponse.bufferFactory().wrap(cachedResponse.getBody());
						return originalResponse.writeWith(Mono.just(buffer));
					}
				}
			}

			// Cache miss or no cache manager: Proceed with the request and cache the
			// response
			System.out.println("Cache miss for: " + cacheKey);

			// Create a custom response decorator to capture the response body
			ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
				@Override
				public Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
					// Get the HttpStatusCode from the original response.
					HttpStatusCode statusCode = originalResponse.getStatusCode();

					if (statusCode != null && statusCode.is2xxSuccessful()) {
						return DataBufferUtils.join(Flux.from(body)).flatMap(dataBuffer -> {
							// Retain the data buffer immediately after joining.
							// This ensures it's available for reading and for passing downstream.
							DataBuffer retainedBuffer = DataBufferUtils.retain(dataBuffer);
							// Immediately release the original 'joined' dataBuffer
							// as we are now working with the retained copy.
							DataBufferUtils.release(dataBuffer);

							try {
								// It's crucial that 'retainedBuffer' is valid here.
								// If you still see an error on this line, ensure 'retain' worked correctly.
								byte[] responseBodyBytes = new byte[retainedBuffer.readableByteCount()];
								retainedBuffer.read(responseBodyBytes);

								CachedResponse newCachedResponse = new CachedResponse((HttpStatus) statusCode,
										getHeaders(), responseBodyBytes);
								if (cache != null) {
									cache.put(cacheKey, newCachedResponse);
									System.out.println("Cached response for: " + cacheKey);
								}

								// Return the retained buffer for the actual response,
								// and ensure it's released once the response is written to the client.
								return originalResponse.writeWith(Mono.just(retainedBuffer))
										.doFinally(signalType -> DataBufferUtils.release(retainedBuffer));
							} catch (Exception e) {
								System.err.println("Error processing response body for caching: " + e.getMessage());
								// If an error occurs, ensure the retained buffer is released.
								DataBufferUtils.release(retainedBuffer);
								// Propagate the error so the GlobalErrorWebExceptionHandler can handle it.
								return Mono.error(new RuntimeException("Failed to cache API response", e));
							}
						})
								// This onErrorResume handles errors that occur during the DataBufferUtils.join
								// operation itself.
								.onErrorResume(e -> {
									System.err.println(
											"Error joining response body for caching (pre-flatMap): " + e.getMessage());
									// If joining fails, propagate the error. The original 'body' might not be
									// re-consumable.
									return Mono.error(new RuntimeException("Error capturing response body", e));
								});
					} else {
						// If not successful, just pass through the original body without caching
						return originalResponse.writeWith(body);
					}
				}

				@Override
				public Mono<Void> writeAndFlushWith(
						org.reactivestreams.Publisher<? extends org.reactivestreams.Publisher<? extends DataBuffer>> body) {
					return writeWith(Flux.from(body).flatMap(p -> p));
				}
			};

			// Continue the filter chain with the decorated response
			return chain.filter(exchange.mutate().response(decoratedResponse).build());
		};
	}

	/**
	 * Configuration class for the CachingGatewayFilterFactory. Currently empty, but
	 * can be extended to add filter-specific properties.
	 */
	public static class Config {
		// No specific configuration properties needed for this basic example
	}

	/**
	 * A simple class to hold cached HTTP responses. In a real application, you
	 * might want to serialize/deserialize this if using a distributed cache like
	 * Redis.
	 */
	private static class CachedResponse {
		private final HttpStatus status;
		private final HttpHeaders headers;
		private final byte[] body;

		public CachedResponse(HttpStatus status, HttpHeaders headers, byte[] body) {
			this.status = status;
			// Create a new HttpHeaders object to avoid modifying the original
			this.headers = new HttpHeaders();
			this.headers.addAll(headers); // Copy headers
			this.body = body;
		}

		public HttpStatus getStatus() {
			return status;
		}

		public HttpHeaders getHeaders() {
			return headers;
		}

		public byte[] getBody() {
			return body;
		}
	}

	/**
	 * Specify the shortcut for applying this filter in route definitions.
	 */
	@Override
	public List<String> shortcutFieldOrder() {
		return Arrays.asList("name"); // Example: if you had a 'name' property in Config
	}
}