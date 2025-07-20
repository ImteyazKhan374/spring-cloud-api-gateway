# 🚀 API Gateway - Spring Cloud Gateway (Local Only with Redis)

This project provides a local setup for **Spring Cloud Gateway** with the following integrations:

* ✅ Redis for caching and rate limiting
* 🔐 JWT-based security
* 🔁 Circuit Breaker and Fallback
* ❌ No Vault, No MySQL, No Kubernetes

---

## 🛠️ Requirements

* Java 17+
* Maven
* Docker (for Redis)

---

## 📦 Core Features

| Feature               | Status |
| --------------------- | ------ |
| Redis Caching         | ✅      |
| Rate Limiting         | ✅      |
| Circuit Breaker       | ✅      |
| Global Fallback/Error | ✅      |
| Eureka Client         | ✅      |
| JWT Auth              | ✅      |

---

## 🚀 Running Redis (Docker)

```bash
docker run --name redis -p 6379:6379 -d redis
```

---

## ⚙️ Configuration - `application.yml`

```yaml
server:
  port: 8085

spring:
  application:
    name: api-gateway

  data:
    redis:
      host: localhost
      port: 6379

  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            key-resolver: '#ipKeyResolver'
            replenishRate: 0
            burstCapacity: 1

resilience4j:
  timelimiter:
    instances:
      default:
        timeoutDuration: 5s
      unison-CB:
        timeoutDuration: 5s
  circuitbreaker:
    instances:
      default:
        slidingWindowSize: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
      unison-CB:
        slidingWindowSize: 5
        failureRateThreshold: 50
        waitDurationInOpenState: 10s

logging:
  level:
    io.github.resilience4j: DEBUG
    org.springframework.cloud.gateway: DEBUG
    org.springframework.security: DEBUG
    org.springframework.data.redis: TRACE
    io.lettuce.core: TRACE

fallback-messages:
  timeout: "Service '%s' timed out. Please try again later."
  circuit-open: "Service '%s' circuit is open. We are working to restore it."
  not-found: "Service '%s' could not be reached or found."
  connection-refused: "Service '%s' is currently unavailable or may be down."
  unknown: "Service '%s' failed due to an unexpected error."

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
    fetch-registry: true
    register-with-eureka: true
  instance:
    hostname: localhost

jwt:
  secret: "thisisaverylongandsecuresecretkeyforjwtauthenticationtesting"
```

---

## 🔑 JWT Auth

* JWT secret is configured in `application.yml`
* All secured routes require a valid token
* Public routes (e.g. `/auth/**`) are open

---

## 📂 Important Classes

| Class                            | Purpose                                       |
| -------------------------------- | --------------------------------------------- |
| `GatewayRouteConfig.java`        | Defines dynamic routes with circuit breaker   |
| `GlobalFallbackHandler.java`     | Central fallback for timeouts/failures        |
| `GlobalErrorWebExceptionHandler` | Custom error responses for the reactive stack |
| `SecurityConfig.java`            | Token validation & access control             |
| `AuthRequest.java`               | Login request model with username & password  |

---

## 🔧 Build and Run

```bash
mvn clean install
java -jar target/api-gateway.jar
```

---

## 🧪 Testing

* Redis must be running (`localhost:6379`)
* Eureka must be available at `http://localhost:8761`
* Access Gateway on `http://localhost:8085`

---

## ✅ Summary

* ⚡ Fast local-only setup
* 🔐 JWT + Redis + Rate Limiting
* 🌐 Ready to integrate with microservices
