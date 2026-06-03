# E-Commerce Microservices Architecture: Complete Implementation Guide

**Target Stack**: Spring Boot 4.x, Spring Cloud 2025.1.1, Kafka, Redis, PostgreSQL, Docker, Kubernetes, Spring Cloud Gateway

**Timeline**: 4-5 months to production-ready internship showcase

---

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Phase 1: Foundation (Weeks 1-2)](#phase-1-foundation-weeks-1-2)
3. [Phase 2: Core Services (Weeks 3-6)](#phase-2-core-services-weeks-3-6)
4. [Phase 3: Event-Driven Features (Weeks 7-9)](#phase-3-event-driven-features-weeks-7-9)
5. [Phase 4: Infrastructure & Deployment (Weeks 10-12)](#phase-4-infrastructure--deployment-weeks-10-12)
6. [Phase 5: Monitoring & Polish (Weeks 13-16)](#phase-5-monitoring--polish-weeks-13-16)
7. [Testing & CI/CD Strategy](#testing--cicd-strategy)
8. [Database Schemas](#database-schemas)
9. [Docker & Kubernetes Configuration](#docker--kubernetes-configuration)

---

## Architecture Overview

### Services Breakdown

| Service | Responsibility | Database | Key Technologies |
|---------|-----------------|----------|-------------------|
| **API Gateway** | Route requests, load balancing | None | Spring Cloud Gateway, Resilience4j |
| **User Service** | Authentication, user profiles, roles | PostgreSQL | Spring Security, JWT, Bcrypt |
| **Product Service** | Catalog, inventory, search | PostgreSQL | JPA, Spring Data, Elasticsearch (optional) |
| **Order Service** | Order creation, fulfillment workflow | PostgreSQL | Saga pattern, Kafka events |
| **Payment Service** | Payment processing, transactions | PostgreSQL | Stripe/PayPal API, Idempotency keys |
| **Notification Service** | Email, SMS, in-app notifications | Redis (cache) | Kafka consumer, JavaMailSender |
| **Service Registry** | Service discovery | In-memory | Eureka Server |
| **Config Server** | Centralized configuration | Git | Spring Cloud Config Server |

### Data Flow Pattern
```
Client → API Gateway → [Service A] ⇄ [Service B]
                         ↓           ↓
                      [Cache]    [Kafka]
                         ↓           ↓
                      [DB]      [Event Consumers]
                         ↓
                    [Monitoring]
```

---

## Phase 1: Foundation (Weeks 1-2)

### Week 1: Project Setup & Infrastructure Services

#### Step 1.1: Create Parent POM Structure
```bash
mkdir ecommerce-microservices && cd ecommerce-microservices
git init
```

**pom.xml** (Parent)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.ecommerce</groupId>
  <artifactId>ecommerce-parent</artifactId>
  <version>1.0.0</version>
  <packaging>pom</packaging>

  <properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <spring-boot.version>4.0.0</spring-boot.version>
    <spring-cloud.version>2025.1.1</spring-cloud.version>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-dependencies</artifactId>
        <version>${spring-boot.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-dependencies</artifactId>
        <version>${spring-cloud.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <modules>
    <module>eureka-server</module>
    <module>config-server</module>
    <module>api-gateway</module>
    <module>user-service</module>
    <module>product-service</module>
    <module>order-service</module>
    <module>payment-service</module>
    <module>notification-service</module>
    <module>common-lib</module>
  </modules>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
        <version>${spring-boot.version}</version>
      </plugin>
    </plugins>
  </build>
</project>
```

#### Step 1.2: Eureka Server (Service Discovery)
```bash
mkdir eureka-server && cd eureka-server
```

**pom.xml**
```xml
<parent>
  <groupId>com.ecommerce</groupId>
  <artifactId>ecommerce-parent</artifactId>
  <version>1.0.0</version>
</parent>

<artifactId>eureka-server</artifactId>

<dependencies>
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
  </dependency>
</dependencies>
```

**EurekaServerApplication.java**
```java
package com.ecommerce.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {
  public static void main(String[] args) {
    SpringApplication.run(EurekaServerApplication.class, args);
  }
}
```

**application.properties** (Port 8761)
```properties
spring.application.name=eureka-server
server.port=8761

eureka.instance.hostname=localhost
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
eureka.client.serviceUrl.defaultZone=http://${eureka.instance.hostname}:${server.port}/eureka/

# Production: use hostname resolution
eureka.instance.prefer-ip-address=false
eureka.instance.hostname=${EUREKA_HOSTNAME:localhost}
```

#### Step 1.3: Config Server
```bash
mkdir config-server && cd config-server
```

**pom.xml**
```xml
<artifactId>config-server</artifactId>

<dependencies>
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-config-server</artifactId>
  </dependency>
</dependencies>
```

**ConfigServerApplication.java**
```java
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
  public static void main(String[] args) {
    SpringApplication.run(ConfigServerApplication.class, args);
  }
}
```

**application.properties** (Port 8888)
```properties
spring.application.name=config-server
server.port=8888

# Git repository (use your own GitHub repo for configs)
spring.cloud.config.server.git.uri=https://github.com/yourusername/ecommerce-config.git
spring.cloud.config.server.git.clone-on-start=true

# Alternative: Use local file system for dev
# spring.cloud.config.server.native.search-locations=classpath:/config
# spring.profiles.active=native

spring.security.user.name=admin
spring.security.user.password=admin-password
```

**GitHub Configuration Repo Structure** (create separate repo `ecommerce-config`):
```
ecommerce-config/
├── application.properties          # Shared config
├── api-gateway.properties
├── user-service.properties
├── product-service.properties
├── order-service.properties
├── payment-service.properties
└── notification-service.properties
```

#### Step 1.4: Common Library Module
```bash
mkdir common-lib && cd common-lib
```

**pom.xml**
```xml
<artifactId>common-lib</artifactId>
<packaging>jar</packaging>

<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
  </dependency>
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
  </dependency>
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.3</version>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
  </dependency>
</dependencies>
```

**Common Classes**:
```java
// src/main/java/com/ecommerce/common/dto/ApiResponse.java
package com.ecommerce.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ApiResponse<T> {
  private int status;
  private String message;
  private T data;
  private LocalDateTime timestamp;

  public static <T> ApiResponse<T> success(T data, String message) {
    return new ApiResponse<>(200, message, data, LocalDateTime.now());
  }

  public static <T> ApiResponse<T> error(int status, String message) {
    return new ApiResponse<>(status, message, null, LocalDateTime.now());
  }
}

// src/main/java/com/ecommerce/common/exception/GlobalExceptionHandler.java
package com.ecommerce.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
    return ResponseEntity.status(404).body(ApiResponse.error(404, ex.getMessage()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleGeneric(Exception ex) {
    return ResponseEntity.status(500).body(ApiResponse.error(500, "Internal Server Error"));
  }
}
```

### Week 2: API Gateway & Security Setup

#### Step 2.1: API Gateway
```bash
mkdir api-gateway && cd api-gateway
```

**pom.xml**
```xml
<artifactId>api-gateway</artifactId>

<dependencies>
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
  </dependency>
  <dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-cloud-gateway</artifactId>
    <version>2.1.0</version>
  </dependency>
  <dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-circuitbreaker</artifactId>
    <version>2.1.0</version>
  </dependency>
  <dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
  </dependency>
  <dependency>
    <groupId>com.ecommerce</groupId>
    <artifactId>common-lib</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

**ApiGatewayApplication.java**
```java
@SpringBootApplication
@EnableEurekaClient
public class ApiGatewayApplication {
  public static void main(String[] args) {
    SpringApplication.run(ApiGatewayApplication.class, args);
  }
}
```

**application.properties** (Port 8080)
```properties
spring.application.name=api-gateway
server.port=8080

eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.instance.prefer-ip-address=true

# Routes to services via Eureka
spring.cloud.gateway.routes[0].id=user-service
spring.cloud.gateway.routes[0].uri=lb://user-service
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/users/**

spring.cloud.gateway.routes[1].id=product-service
spring.cloud.gateway.routes[1].uri=lb://product-service
spring.cloud.gateway.routes[1].predicates[0]=Path=/api/products/**

spring.cloud.gateway.routes[2].id=order-service
spring.cloud.gateway.routes[2].uri=lb://order-service
spring.cloud.gateway.routes[2].predicates[0]=Path=/api/orders/**

spring.cloud.gateway.routes[3].id=payment-service
spring.cloud.gateway.routes[3].uri=lb://payment-service
spring.cloud.gateway.routes[3].predicates[0]=Path=/api/payments/**

# Resilience4j Circuit Breaker
resilience4j.circuitbreaker.instances.user-service.sliding-window-size=10
resilience4j.circuitbreaker.instances.user-service.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.user-service.slow-call-rate-threshold=50
resilience4j.circuitbreaker.instances.user-service.slow-call-duration-threshold=2000
```

**JwtAuthenticationFilter.java**
```java
package com.ecommerce.gateway.filter;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

  @Value("${jwt.secret:your-secret-key}")
  private String secret;

  public JwtAuthenticationFilter() {
    super(Config.class);
  }

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      String token = extractToken(exchange);

      if (token == null || !validateToken(token)) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
      }

      return chain.filter(exchange);
    };
  }

  private String extractToken(ServerWebExchange exchange) {
    String header = exchange.getRequest().getHeaders().getFirst("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      return header.substring(7);
    }
    return null;
  }

  private boolean validateToken(String token) {
    try {
      Jwts.parserBuilder()
        .setSigningKey(secret.getBytes())
        .build()
        .parseClaimsJws(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static class Config {
  }
}
```

---

## Phase 2: Core Services (Weeks 3-6)

### Week 3-4: User Service

#### Step 3.1: User Service Setup
```bash
mkdir user-service && cd user-service
```

**pom.xml**
```xml
<artifactId>user-service</artifactId>

<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>
  <dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.1</version>
    <scope>runtime</scope>
  </dependency>
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-config</artifactId>
  </dependency>
  <dependency>
    <groupId>com.ecommerce</groupId>
    <artifactId>common-lib</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

**User Entity**
```java
package com.ecommerce.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(unique = true, nullable = false)
  private String email;

  private String firstName;
  private String lastName;

  @Column(nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  private UserRole role;

  private boolean enabled;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}

enum UserRole {
  CUSTOMER, ADMIN, VENDOR
}
```

**User Repository**
```java
package com.ecommerce.user.repository;

import com.ecommerce.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
  Optional<User> findByEmail(String email);
  boolean existsByEmail(String email);
}
```

**User Service**
```java
package com.ecommerce.user.service;

import com.ecommerce.user.entity.User;
import com.ecommerce.user.repository.UserRepository;
import com.ecommerce.user.dto.UserRegistrationRequest;
import com.ecommerce.user.dto.UserResponseDTO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;
  private final BCryptPasswordEncoder passwordEncoder;

  public UserResponseDTO registerUser(UserRegistrationRequest request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new IllegalArgumentException("Email already exists");
    }

    User user = new User();
    user.setEmail(request.getEmail());
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    user.setRole(UserRole.CUSTOMER);
    user.setEnabled(true);

    User savedUser = userRepository.save(user);
    return UserResponseDTO.fromEntity(savedUser);
  }

  public UserResponseDTO getUserById(String id) {
    User user = userRepository.findById(id)
      .orElseThrow(() -> new RuntimeException("User not found"));
    return UserResponseDTO.fromEntity(user);
  }
}
```

**User Controller**
```java
package com.ecommerce.user.controller;

import com.ecommerce.common.dto.ApiResponse;
import com.ecommerce.user.service.UserService;
import com.ecommerce.user.dto.UserRegistrationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
  private final UserService userService;

  @PostMapping("/register")
  public ResponseEntity<?> register(@RequestBody UserRegistrationRequest request) {
    var user = userService.registerUser(request);
    return ResponseEntity.ok(ApiResponse.success(user, "User registered successfully"));
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getUser(@PathVariable String id) {
    var user = userService.getUserById(id);
    return ResponseEntity.ok(ApiResponse.success(user, "User fetched"));
  }
}
```

**application.properties**
```properties
spring.application.name=user-service
server.port=8001

spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce_user
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.instance.prefer-ip-address=true

# Config Server
spring.config.import=configserver:http://localhost:8888
```

### Week 5: Product Service

**Product Entity** (Similar pattern):
```java
@Entity
@Table(name = "products")
@Data
public class Product {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  private String name;
  private String description;
  private BigDecimal price;
  private Integer stock;
  private String category;
  private LocalDateTime createdAt;
}
```

**Key Patterns**: Use same Eureka client, Config Server, PostgreSQL setup. Implement search with JPA Specifications for flexible querying.

### Week 6: Order Service & Payment Service

**Order Entity**:
```java
@Entity
@Table(name = "orders")
@Data
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @Column(nullable = false)
  private String userId;

  private String orderId; // Unique order number

  @OneToMany(cascade = CascadeType.ALL)
  private List<OrderItem> items;

  @Enumerated(EnumType.STRING)
  private OrderStatus status; // PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED

  private BigDecimal totalAmount;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
```

---

## Phase 3: Event-Driven Features (Weeks 7-9)

### Week 7-8: Kafka Event Streaming

#### Step 7.1: Add Kafka Dependencies
```xml
<!-- In all service pom.xml files that need events -->
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-stream-kafka</artifactId>
</dependency>
```

#### Step 7.2: Order Service with Kafka Publisher

**OrderCreatedEvent.java**
```java
package com.ecommerce.order.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent implements Serializable {
  private String orderId;
  private String userId;
  private List<OrderItemDTO> items;
  private BigDecimal totalAmount;
  private long timestamp;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OrderItemDTO {
    private String productId;
    private Integer quantity;
    private BigDecimal price;
  }
}
```

**OrderEventPublisher.java**
```java
package com.ecommerce.order.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RequiredArgsConstructor
public class OrderEventPublisher {
  private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
  private static final Logger logger = LoggerFactory.getLogger(OrderEventPublisher.class);

  public void publishOrderCreatedEvent(OrderCreatedEvent event) {
    kafkaTemplate.send("order-events", event.getOrderId(), event)
      .addCallback(
        result -> logger.info("Published order event: {}", event.getOrderId()),
        ex -> logger.error("Failed to publish order event", ex)
      );
  }
}
```

**Order Service Integration**
```java
@Service
@RequiredArgsConstructor
public class OrderService {
  private final OrderRepository orderRepository;
  private final OrderEventPublisher eventPublisher;

  public OrderResponseDTO createOrder(CreateOrderRequest request) {
    Order order = new Order();
    order.setUserId(request.getUserId());
    order.setItems(request.getItems());
    order.setTotalAmount(request.getTotalAmount());
    order.setStatus(OrderStatus.PENDING);

    Order savedOrder = orderRepository.save(order);

    // Publish event
    OrderCreatedEvent event = new OrderCreatedEvent(
      savedOrder.getId(),
      savedOrder.getUserId(),
      convertItems(savedOrder.getItems()),
      savedOrder.getTotalAmount(),
      System.currentTimeMillis()
    );

    eventPublisher.publishOrderCreatedEvent(event);
    return OrderResponseDTO.fromEntity(savedOrder);
  }
}
```

#### Step 7.3: Notification Service (Kafka Consumer)

**NotificationService.java**
```java
package com.ecommerce.notification.service;

import com.ecommerce.order.event.OrderCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.JavaMailSender;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {
  private final JavaMailSender mailSender;

  @KafkaListener(topics = "order-events", groupId = "notification-service")
  public void handleOrderCreatedEvent(OrderCreatedEvent event) {
    sendOrderConfirmationEmail(event);
  }

  private void sendOrderConfirmationEmail(OrderCreatedEvent event) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo("user@example.com"); // Get from event
    message.setSubject("Order Confirmation: " + event.getOrderId());
    message.setText("Your order has been created for amount: " + event.getTotalAmount());

    mailSender.send(message);
  }
}
```

**application.properties**
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=notification-service
spring.kafka.consumer.auto-offset-reset=earliest

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

#### Step 7.4: Payment Service with Stripe Integration

**PaymentService.java**
```java
package com.ecommerce.payment.service;

import com.stripe.Stripe;
import com.stripe.model.Charge;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
  @Value("${stripe.api.key}")
  private String stripeApiKey;

  public PaymentResponseDTO processPayment(PaymentRequest request) {
    Stripe.apiKey = stripeApiKey;

    try {
      Charge charge = Charge.create(
        new java.util.HashMap<String, Object>() {{
          put("amount", (long)(request.getAmount().doubleValue() * 100));
          put("currency", "usd");
          put("source", request.getTokenId());
          put("description", "Order: " + request.getOrderId());
          put("idempotency_key", request.getIdempotencyKey()); // Critical for safety
        }}
      );

      return new PaymentResponseDTO(
        charge.getId(),
        charge.getStatus(),
        request.getAmount()
      );
    } catch (Exception e) {
      throw new PaymentProcessingException("Payment failed: " + e.getMessage());
    }
  }
}
```

### Week 9: Redis Caching

**Redis Configuration**
```java
package com.ecommerce.cache.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(factory);

    StringRedisSerializer stringSerializer = new StringRedisSerializer();
    template.setKeySerializer(stringSerializer);
    template.setValueSerializer(stringSerializer);

    return template;
  }
}
```

**Caching in Product Service**
```java
@Service
@RequiredArgsConstructor
public class ProductService {
  private final ProductRepository productRepository;
  private final RedisTemplate<String, Object> redisTemplate;

  @Cacheable(value = "products", key = "#id")
  public Product getProductById(String id) {
    return productRepository.findById(id)
      .orElseThrow(() -> new RuntimeException("Product not found"));
  }

  @CacheEvict(value = "products", key = "#id")
  public void updateProduct(String id, ProductUpdateRequest request) {
    // Update logic
  }
}
```

**application.properties**
```properties
spring.redis.host=localhost
spring.redis.port=6379
spring.cache.type=redis
spring.cache.redis.time-to-live=600000 # 10 minutes
```

---

## Phase 4: Infrastructure & Deployment (Weeks 10-12)

### Week 10: Docker Setup

#### Step 10.1: Dockerfile Template for Each Service
```dockerfile
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY target/user-service-1.0.0.jar app.jar

EXPOSE 8001

ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Step 10.2: Docker Compose (Local Development)

**docker-compose.yml**
```yaml
version: '3.8'

services:
  postgres-user:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: ecommerce_user
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_user_data:/var/lib/postgresql/data

  postgres-product:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: ecommerce_product
      POSTGRES_PASSWORD: postgres
    ports:
      - "5433:5432"
    volumes:
      - postgres_product_data:/var/lib/postgresql/data

  postgres-order:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: ecommerce_order
      POSTGRES_PASSWORD: postgres
    ports:
      - "5434:5432"

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"

  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_SYNC_LIMIT: 2
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"

  eureka-server:
    build:
      context: ./eureka-server
      dockerfile: Dockerfile
    ports:
      - "8761:8761"
    environment:
      EUREKA_HOSTNAME: eureka-server

  config-server:
    build:
      context: ./config-server
      dockerfile: Dockerfile
    ports:
      - "8888:8888"
    depends_on:
      - eureka-server

  api-gateway:
    build:
      context: ./api-gateway
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    depends_on:
      - eureka-server
      - config-server
    environment:
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/

  user-service:
    build:
      context: ./user-service
      dockerfile: Dockerfile
    ports:
      - "8001:8001"
    depends_on:
      - postgres-user
      - eureka-server
      - redis
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-user:5432/ecommerce_user
      SPRING_DATASOURCE_PASSWORD: postgres
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
      SPRING_REDIS_HOST: redis

  product-service:
    build:
      context: ./product-service
      dockerfile: Dockerfile
    ports:
      - "8002:8002"
    depends_on:
      - postgres-product
      - eureka-server
      - redis
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-product:5432/ecommerce_product
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/

  order-service:
    build:
      context: ./order-service
      dockerfile: Dockerfile
    ports:
      - "8003:8003"
    depends_on:
      - postgres-order
      - eureka-server
      - kafka
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-order:5432/ecommerce_order
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092

volumes:
  postgres_user_data:
  postgres_product_data:
```

**Build & Run**:
```bash
docker-compose up --build
```

### Week 11: Kubernetes Deployment

#### Step 11.1: Kubernetes Manifests

**user-service-deployment.yaml**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  labels:
    app: user-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
      - name: user-service
        image: your-registry/user-service:1.0.0
        ports:
        - containerPort: 8001
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            configMapKeyRef:
              name: db-config
              key: user-db-url
        - name: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
          value: "http://eureka-server.default.svc.cluster.local:8761/eureka/"
        - name: SPRING_REDIS_HOST
          value: "redis.default.svc.cluster.local"
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8001
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8001
          initialDelaySeconds: 10
          periodSeconds: 5

---
apiVersion: v1
kind: Service
metadata:
  name: user-service
spec:
  selector:
    app: user-service
  ports:
  - port: 8001
    targetPort: 8001
  type: ClusterIP
```

**postgres-statefulset.yaml**
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres-user
spec:
  serviceName: postgres-user
  replicas: 1
  selector:
    matchLabels:
      app: postgres-user
  template:
    metadata:
      labels:
        app: postgres-user
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: ecommerce_user
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-secret
              key: password
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
  volumeClaimTemplates:
  - metadata:
      name: postgres-storage
    spec:
      accessModes: [ "ReadWriteOnce" ]
      resources:
        requests:
          storage: 10Gi
```

**ConfigMap**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: db-config
data:
  user-db-url: "jdbc:postgresql://postgres-user:5432/ecommerce_user"
  product-db-url: "jdbc:postgresql://postgres-product:5432/ecommerce_product"
  order-db-url: "jdbc:postgresql://postgres-order:5432/ecommerce_order"
```

**Secret**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: db-secret
type: Opaque
data:
  password: cG9zdGdyZXM= # base64 encoded "postgres"
```

**Ingress**
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ecommerce-ingress
spec:
  ingressClassName: nginx
  rules:
  - host: api.ecommerce.local
    http:
      paths:
      - path: /api/users
        pathType: Prefix
        backend:
          service:
            name: user-service
            port:
              number: 8001
      - path: /api/products
        pathType: Prefix
        backend:
          service:
            name: product-service
            port:
              number: 8002
      - path: /api/orders
        pathType: Prefix
        backend:
          service:
            name: order-service
            port:
              number: 8003
```

**Deploy to Kubernetes**:
```bash
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/postgres-statefulset.yaml
kubectl apply -f k8s/user-service-deployment.yaml
kubectl apply -f k8s/ingress.yaml

# Verify
kubectl get pods
kubectl get svc
kubectl logs -f deployment/user-service
```

### Week 12: Database Initialization & Load Testing

**Liquibase for Schema Management** (Optional but recommended):

```xml
<dependency>
  <groupId>org.liquibase</groupId>
  <artifactId>liquibase-core</artifactId>
</dependency>
```

```yaml
# db/changelog/db.changelog-master.yaml
databaseChangeLog:
  - changeSet:
      id: 1
      author: dev
      changes:
        - createTable:
            tableName: users
            columns:
              - column:
                  name: id
                  type: VARCHAR(36)
                  constraints:
                    primaryKey: true
              - column:
                  name: email
                  type: VARCHAR(255)
                  constraints:
                    unique: true
                    nullable: false
              - column:
                  name: password_hash
                  type: VARCHAR(255)
                  constraints:
                    nullable: false
              - column:
                  name: created_at
                  type: TIMESTAMP
```

---

## Phase 5: Monitoring & Polish (Weeks 13-16)

### Week 13: Monitoring with Spring Boot Actuator + Prometheus

**Add Dependencies**:
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**application.properties**:
```properties
management.endpoints.web.exposure.include=health,metrics,prometheus
management.metrics.export.prometheus.enabled=true
management.endpoint.health.show-details=always
```

**Prometheus Config** (`prometheus.yml`):
```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'user-service'
    static_configs:
      - targets: ['localhost:8001']
    metrics_path: '/actuator/prometheus'

  - job_name: 'product-service'
    static_configs:
      - targets: ['localhost:8002']
    metrics_path: '/actuator/prometheus'
```

**Grafana Dashboards**: Use pre-built Spring Boot dashboard (ID: 4378).

### Week 14-15: Integration & Load Testing

**JUnit 5 + Testcontainers**:
```java
@SpringBootTest
@Testcontainers
class UserServiceIntegrationTest {
  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
    .withDatabaseName("testdb");

  @Test
  void testUserRegistration() {
    // Test logic
  }
}
```

**Load Testing with JMeter**:
- Create test plan for order creation (100 users, 10 iterations)
- Validate response times < 500ms at p95
- Check error rate < 0.1%

### Week 16: Documentation & Final Polish

**Create README.md** with:
- Architecture diagram
- Setup instructions
- API documentation (Swagger)
- Deployment guide
- Troubleshooting

**Add Swagger**:
```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.0.2</version>
</dependency>
```

---

## Testing & CI/CD Strategy

### Unit Testing
- 70%+ coverage on service layer
- Mock repositories, external APIs
- Framework: JUnit 5 + Mockito

### Integration Testing
- Testcontainers for databases
- Embedded Kafka for event testing
- Test order-to-notification flow end-to-end

### GitHub Actions CI/CD
```yaml
name: Build & Deploy

on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '21'
      - run: mvn clean package -DskipTests
      - run: mvn test
      - name: Build Docker images
        run: docker-compose build
      - name: Push to Registry
        run: docker push your-registry/user-service:latest
```

---

## Database Schemas

### User Service
```sql
CREATE TABLE users (
  id VARCHAR(36) PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(50) DEFAULT 'CUSTOMER',
  enabled BOOLEAN DEFAULT true,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email ON users(email);
```

### Product Service
```sql
CREATE TABLE products (
  id VARCHAR(36) PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  price DECIMAL(10, 2) NOT NULL,
  stock INTEGER DEFAULT 0,
  category VARCHAR(100),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_category ON products(category);
```

### Order Service
```sql
CREATE TABLE orders (
  id VARCHAR(36) PRIMARY KEY,
  order_number VARCHAR(50) UNIQUE,
  user_id VARCHAR(36) NOT NULL,
  status VARCHAR(50) DEFAULT 'PENDING',
  total_amount DECIMAL(10, 2),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
  id VARCHAR(36) PRIMARY KEY,
  order_id VARCHAR(36) NOT NULL REFERENCES orders(id),
  product_id VARCHAR(36) NOT NULL,
  quantity INTEGER,
  price DECIMAL(10, 2),
  FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_id ON orders(user_id);
CREATE INDEX idx_status ON orders(status);
```

---

## Docker & Kubernetes Configuration

### Multi-stage Docker Build (Optimized)
```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8001
ENTRYPOINT ["java", "-Xmx256m", "-Xms128m", "-jar", "app.jar"]
```

### Kubernetes Resource Limits
```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "250m"
  limits:
    memory: "512Mi"
    cpu: "500m"
```

---

## Key Milestones & Timeline

| Week | Milestone | Status |
|------|-----------|--------|
| 1-2 | Eureka, Config Server, API Gateway | Foundation |
| 3-6 | User, Product, Order, Payment Services | Core Services |
| 7-9 | Kafka Events, Notifications, Caching | Event-Driven |
| 10-12 | Docker, Kubernetes, Database | Infrastructure |
| 13-16 | Monitoring, Testing, Documentation | Production-Ready |

---

## Troubleshooting

### Eureka not registering services
- Check `eureka.client.service-url.defaultZone` points to correct URL
- Verify network connectivity between services
- Check logs: `curl http://localhost:8761`

### Kafka topic not receiving events
- Ensure Kafka broker is running: `kafka-topics.sh --list --bootstrap-server localhost:9092`
- Check topic exists: `kafka-topics.sh --create --topic order-events --bootstrap-server localhost:9092`

### Database connection failures
- Verify PostgreSQL is running on correct port
- Check credentials in `application.properties`
- Run: `psql -h localhost -U postgres -d ecommerce_user`

### Pod CrashLoopBackOff in Kubernetes
- Check logs: `kubectl logs <pod-name>`
- Verify ConfigMaps and Secrets are created
- Check resource limits

---

## Next Steps for Internship Success

1. **Add API Documentation**: Integrate SpringDoc OpenAPI (Swagger) for all endpoints
2. **Security Hardening**: Implement OAuth2, API rate limiting, input validation
3. **Advanced Caching**: Implement cache invalidation strategies, distributed caching
4. **Circuit Breaker Patterns**: Add Resilience4j for fault tolerance
5. **CQRS Pattern** (Optional): Separate read/write models for scalability
6. **Performance Optimization**: Database query optimization, N+1 prevention
7. **Transaction Management**: Implement @Transactional, distributed transactions
8. **Observability**: Add distributed tracing with Sleuth + Zipkin
9. **README Excellence**: Detailed setup, architecture diagrams, usage examples
10. **GitHub Portfolio**: Clean commits, meaningful PR descriptions, complete docs

---

## Quick Start Commands

```bash
# Build all modules
mvn clean package -DskipTests

# Start infrastructure
docker-compose up -d

# Run tests
mvn test

# Build Docker images
docker-compose build

# Deploy to Kubernetes
kubectl apply -f k8s/

# Check status
curl http://localhost:8761  # Eureka
curl http://localhost:8080/api/users/register  # API Gateway
kubectl get pods
kubectl logs -f deployment/user-service
```

This guide gives you a production-grade microservices architecture ready for any backend internship interview. The codebase demonstrates mastery of Spring Cloud, distributed systems, DevOps, and modern backend engineering practices.
