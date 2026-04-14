# MicroCartix

## Services
- `service/`: Eureka Server (port 8761)
- `users/`: User Service (port 8080, /api/v0)
- `product-service/`: Product Service (port 8081, /api/v0)
- `api-gateway/`: API Gateway (port 8082, /api/v0) - New! Handles auth proxy, JWT validation, routing.

## Running
1. Start Eureka: cd service && mvnw.cmd spring-boot:run
2. Start Gateway: cd api-gateway && mvnw.cmd spring-boot:run (add mvnw or use mvn)
3. Start users/product.

All services register to Eureka. Gateway proxies /api/v0/auth/** to user-service, /products/** to product-service with JWT validation on protected paths.

JWT_SECRET must be set in .env.
