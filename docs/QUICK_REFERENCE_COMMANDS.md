# Microservices Quick Reference & Commands

## 🚀 Quick Start

### Local Development (Docker Compose)
```bash
# Clone and navigate
cd ecommerce-microservices
git submodule update --init --recursive

# Build all modules
mvn clean package -DskipTests -q

# Start infrastructure
docker-compose up -d

# Verify services are running
docker-compose ps
docker logs eureka-server | grep "Started"

# Check Eureka dashboard
open http://localhost:8761

# Test API Gateway
curl -X GET http://localhost:8080/actuator/health

# Stop all services
docker-compose down -v  # -v removes volumes
```

### Kubernetes Deployment
```bash
# Create namespace and configs
kubectl create namespace ecommerce
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# Deploy infrastructure (postgres, redis, kafka)
kubectl apply -f k8s/postgres-statefulset.yaml
kubectl apply -f k8s/redis-deployment.yaml
kubectl apply -f k8s/kafka-statefulset.yaml

# Deploy services
kubectl apply -f k8s/eureka-deployment.yaml
kubectl apply -f k8s/api-gateway-deployment.yaml
kubectl apply -f k8s/user-service-deployment.yaml
kubectl apply -f k8s/product-service-deployment.yaml
kubectl apply -f k8s/order-service-deployment.yaml
kubectl apply -f k8s/payment-service-deployment.yaml
kubectl apply -f k8s/notification-service-deployment.yaml

# Deploy autoscaling & ingress
kubectl apply -f k8s/hpa.yaml
kubectl apply -f k8s/ingress.yaml

# Monitor
kubectl get pods -n ecommerce -w
kubectl get svc -n ecommerce
```

---

## Service Port Mapping

| Service | Port | Health Check | 
|---------|------|--------------|
| API Gateway | 8080 | `curl http://localhost:8080/actuator/health` |
| Eureka Server | 8761 | `curl http://localhost:8761/eureka/apps` |
| Config Server | 8888 | `curl http://localhost:8888/actuator/health` |
| User Service | 8001 | `curl http://localhost:8001/actuator/health` |
| Product Service | 8002 | `curl http://localhost:8002/actuator/health` |
| Order Service | 8003 | `curl http://localhost:8003/actuator/health` |
| Payment Service | 8004 | `curl http://localhost:8004/actuator/health` |
| Notification Service | 8005 | `curl http://localhost:8005/actuator/health` |
| PostgreSQL (User) | 5432 | `psql -h localhost -U postgres` |
| PostgreSQL (Product) | 5433 | `psql -h localhost -p 5433 -U postgres` |
| PostgreSQL (Order) | 5434 | `psql -h localhost -p 5434 -U postgres` |
| PostgreSQL (Payment) | 5435 | `psql -h localhost -p 5435 -U postgres` |
| Redis | 6379 | `redis-cli ping` |
| Zookeeper | 2181 | N/A |
| Kafka | 9092 | `kafka-broker-api-versions --bootstrap-server localhost:9092` |
| Prometheus | 9090 | `curl http://localhost:9090/-/healthy` |
| Grafana | 3000 | Login: admin/admin |
| Elasticsearch | 9200 | `curl http://localhost:9200/` |
| Kibana | 5601 | `curl http://localhost:5601/api/status` |

---

## API Endpoints Reference

### User Service
```bash
# Register user
curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "password": "SecurePassword123"
  }'

# Login (returns JWT token)
curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "SecurePassword123"}'

# Get user profile
curl -X GET http://localhost:8080/api/users/{userId} \
  -H "Authorization: Bearer {token}"

# Update user
curl -X PUT http://localhost:8080/api/users/{userId} \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"firstName": "Jane"}'
```

### Product Service
```bash
# Get all products
curl -X GET http://localhost:8080/api/products

# Get product by ID
curl -X GET http://localhost:8080/api/products/{productId}

# Search products
curl -X GET "http://localhost:8080/api/products/search?keyword=laptop&category=electronics"

# Get by category
curl -X GET "http://localhost:8080/api/products/category/electronics"

# Create product (Admin only)
curl -X POST http://localhost:8080/api/products \
  -H "Authorization: Bearer {admin-token}" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Laptop",
    "description": "High-performance laptop",
    "price": 999.99,
    "stock": 50,
    "category": "electronics"
  }'
```

### Order Service
```bash
# Create order
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-uuid",
    "items": [
      {"productId": "prod-1", "quantity": 2, "price": 99.99}
    ],
    "totalAmount": 199.98
  }'

# Get user orders
curl -X GET http://localhost:8080/api/orders/user/{userId} \
  -H "Authorization: Bearer {token}"

# Get order details
curl -X GET http://localhost:8080/api/orders/{orderId} \
  -H "Authorization: Bearer {token}"

# Update order status (Admin)
curl -X PATCH http://localhost:8080/api/orders/{orderId} \
  -H "Authorization: Bearer {admin-token}" \
  -H "Content-Type: application/json" \
  -d '{"status": "SHIPPED"}'
```

### Payment Service
```bash
# Process payment
curl -X POST http://localhost:8080/api/payments \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "order-uuid",
    "amount": 199.98,
    "currency": "USD",
    "tokenId": "tok_visa"
  }'

# Get payment status
curl -X GET http://localhost:8080/api/payments/{paymentId} \
  -H "Authorization: Bearer {token}"
```

---

## Database Commands

### PostgreSQL Access
```bash
# User Service DB
psql -h localhost -U postgres -d ecommerce_user

# Product Service DB
psql -h localhost -p 5433 -U postgres -d ecommerce_product

# Order Service DB
psql -h localhost -p 5434 -U postgres -d ecommerce_order

# Payment Service DB
psql -h localhost -p 5435 -U postgres -d ecommerce_payment

# Basic queries inside psql
\dt                          # List tables
\d table_name                # Describe table
SELECT * FROM users;         # Query data
\q                           # Quit
```

### Database Initialization (SQL Scripts)
```bash
# Create extensions (if using UUID type)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

# Create indexes for better performance
CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_order_user_id ON orders(user_id);
CREATE INDEX idx_order_status ON orders(status);
CREATE INDEX idx_product_category ON products(category);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);

# Check index usage
SELECT * FROM pg_stat_user_indexes;
```

### Redis Commands
```bash
# Connect to Redis
redis-cli

# Basic commands
PING                         # Test connection
SET key value                # Set key-value
GET key                       # Get value
DEL key                       # Delete key
FLUSHALL                      # Clear all data
KEYS *                        # List all keys
TTL key                       # Time to live
EXPIRE key 3600              # Set expiration (seconds)

# Monitor commands
MONITOR                       # Watch all commands
INFO                          # Server statistics
```

---

## Kafka Commands

### Topic Management
```bash
# List topics
kafka-topics.sh --list --bootstrap-server localhost:9092

# Create topic
kafka-topics.sh --create \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --partitions 3 \
  --replication-factor 1

# Describe topic
kafka-topics.sh --describe \
  --bootstrap-server localhost:9092 \
  --topic order-events

# Delete topic
kafka-topics.sh --delete \
  --bootstrap-server localhost:9092 \
  --topic order-events
```

### Message Consumption
```bash
# Read messages from topic
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning

# Read last 10 messages
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic order-events \
  --max-messages 10

# Monitor consumer group
kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group notification-service \
  --describe
```

---

## Docker Commands

### Container Management
```bash
# View running containers
docker-compose ps

# View logs
docker-compose logs -f eureka-server     # Follow logs
docker-compose logs --tail=50 user-service  # Last 50 lines
docker-compose logs user-service order-service  # Multiple services

# Access container shell
docker-compose exec user-service /bin/bash
docker-compose exec postgres-user psql -U postgres

# Restart services
docker-compose restart user-service
docker-compose up -d user-service       # Rebuild if needed

# Stop and remove
docker-compose down                      # Stop all
docker-compose down -v                   # Remove volumes too
```

### Build Commands
```bash
# Build single service
docker build -f user-service/Dockerfile -t ecommerce/user-service:1.0.0 .

# Build all with docker-compose
docker-compose build --no-cache

# Push to registry
docker push your-registry/user-service:1.0.0
```

---

## Kubernetes Commands

### Cluster Status
```bash
# Cluster info
kubectl cluster-info
kubectl get nodes
kubectl describe node <node-name>

# Namespace operations
kubectl get namespaces
kubectl get pods -n ecommerce
kubectl get svc -n ecommerce
kubectl get pvc -n ecommerce

# Watch resources
kubectl get pods -n ecommerce -w
kubectl get deployment -n ecommerce -w
```

### Pod Operations
```bash
# Pod logs
kubectl logs -n ecommerce deployment/user-service
kubectl logs -n ecommerce pod/user-service-xxxxx
kubectl logs -f -n ecommerce deployment/user-service  # Follow

# Execute commands in pod
kubectl exec -it -n ecommerce pod/user-service-xxxxx -- /bin/bash
kubectl exec -n ecommerce deployment/user-service -- curl http://localhost:8001/actuator/health

# Port forwarding
kubectl port-forward -n ecommerce svc/user-service 8001:8001
kubectl port-forward -n ecommerce svc/eureka-server 8761:8761
kubectl port-forward -n ecommerce deployment/user-service 8001:8001

# Describe resources
kubectl describe pod -n ecommerce user-service-xxxxx
kubectl describe svc -n ecommerce user-service
```

### Deployments
```bash
# Deployment operations
kubectl get deployment -n ecommerce
kubectl describe deployment -n ecommerce user-service
kubectl scale deployment user-service --replicas=3 -n ecommerce

# Rollout operations
kubectl rollout history deployment/user-service -n ecommerce
kubectl rollout status deployment/user-service -n ecommerce
kubectl rollout undo deployment/user-service -n ecommerce

# Update image
kubectl set image deployment/user-service \
  user-service=your-registry/user-service:1.1.0 \
  -n ecommerce
```

### Debugging
```bash
# Get events
kubectl get events -n ecommerce --sort-by='.lastTimestamp'

# Describe pod details
kubectl describe pod <pod-name> -n ecommerce

# Check resource metrics
kubectl top pods -n ecommerce
kubectl top nodes

# Check HPA status
kubectl get hpa -n ecommerce -w

# Check persistent volumes
kubectl get pv
kubectl get pvc -n ecommerce
```

---

## Monitoring & Debugging

### Prometheus Queries
```
# CPU usage
rate(process_cpu_usage[1m])

# Memory usage
process_resident_memory_bytes / 1024 / 1024

# HTTP request count
rate(http_requests_total[1m])

# Request duration (p95)
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))

# Error rate
rate(http_requests_total{status=~"5.."}[1m])

# JVM heap usage
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
```

### Grafana Dashboards
- Spring Boot Statistics: Dashboard ID 4378
- JVM Metrics: Dashboard ID 3457
- Kubernetes Cluster: Dashboard ID 8588
- PostgreSQL: Dashboard ID 3794

### Health Checks
```bash
# All endpoints health
curl http://localhost:8001/actuator/health

# Detailed health with dependencies
curl http://localhost:8001/actuator/health?full=true

# Individual components
curl http://localhost:8001/actuator/health/db
curl http://localhost:8001/actuator/health/redis
curl http://localhost:8001/actuator/health/kafka
```

---

## Troubleshooting Checklist

### Service Not Starting
- [ ] Check logs: `docker-compose logs service-name`
- [ ] Verify ports are not in use: `lsof -i :8001`
- [ ] Check dependencies are running
- [ ] Verify environment variables: `docker-compose exec service-name env`
- [ ] Check database connection: `docker-compose exec postgres-user psql -U postgres`

### Service Discovery Issues (Eureka)
- [ ] Eureka server running: `curl http://localhost:8761`
- [ ] Service registered: Check Eureka dashboard
- [ ] Check eureka.client.service-url.defaultZone matches in all services
- [ ] Network connectivity: `docker-compose exec user-service ping eureka-server`
- [ ] Clear Eureka cache if needed: Stop service, wait 90s, restart

### Database Connection Issues
- [ ] Database container running: `docker-compose ps postgres-user`
- [ ] Credentials in application.properties correct
- [ ] Connection string format: `jdbc:postgresql://host:5432/db-name`
- [ ] Test connection: `psql -h localhost -U postgres`
- [ ] Check network: `docker-compose exec user-service ping postgres-user`

### Kafka Event Issues
- [ ] Kafka running: `docker-compose ps kafka`
- [ ] Topic exists: `kafka-topics.sh --list --bootstrap-server localhost:9092`
- [ ] Consumer group active: Check in logs
- [ ] Broker connectivity: `docker-compose exec order-service nc -zv kafka 9092`
- [ ] Check message lag: `kafka-consumer-groups.sh --describe --group notification-service`

### API Gateway Routing Issues
- [ ] Service registered in Eureka
- [ ] Gateway route configuration correct in application.properties
- [ ] Service hostname matches route: `lb://service-name`
- [ ] Test direct service: `curl http://localhost:8001/actuator/health`
- [ ] Check gateway logs: `docker-compose logs api-gateway | grep -i route`

### Performance Issues
- [ ] Monitor resource usage: `docker stats`
- [ ] Check database query performance: Enable `spring.jpa.show-sql=true`
- [ ] Redis cache hit rate: `redis-cli INFO stats`
- [ ] Kafka consumer lag: `kafka-consumer-groups.sh --describe`
- [ ] Check slow endpoints: Prometheus dashboard

### Memory/Heap Issues
- [ ] Increase JVM heap: Update `-Xmx512m` in Dockerfile or docker-compose
- [ ] Check memory leaks: `jcmd <pid> GC.heap_dump filename=heap.bin`
- [ ] Monitor GC frequency: Check Grafana
- [ ] Reduce cache TTL if necessary

### Network Issues (Kubernetes)
- [ ] DNS resolution: `kubectl run -it --rm debug --image=busybox --restart=Never -- nslookup user-service.ecommerce.svc.cluster.local`
- [ ] Network policies: Check if blocking traffic: `kubectl get networkpolicies -n ecommerce`
- [ ] Service endpoints: `kubectl get endpoints -n ecommerce`
- [ ] Ingress configuration: `kubectl describe ingress ecommerce-ingress -n ecommerce`

---

## Performance Tuning

### JVM Settings
```dockerfile
# In Dockerfile
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=200", "-jar", "app.jar"]
```

### Database Connection Pool
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### Redis Configuration
```properties
spring.redis.timeout=2000
spring.redis.jedis.pool.max-active=20
spring.redis.jedis.pool.max-idle=10
spring.redis.jedis.pool.min-idle=5
```

### Kafka Producer Optimization
```properties
spring.kafka.producer.batch-size=32768
spring.kafka.producer.linger-ms=10
spring.kafka.producer.compression-type=snappy
spring.kafka.producer.acks=1
```

---

## CI/CD Integration

### GitHub Actions Workflow
```yaml
name: Build & Deploy
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Build with Maven
        run: mvn clean package -DskipTests
      - name: Run Tests
        run: mvn verify
      - name: Build Docker images
        run: docker-compose build
      - name: Push to DockerHub
        run: |
          echo ${{ secrets.DOCKER_PASSWORD }} | docker login -u ${{ secrets.DOCKER_USERNAME }} --password-stdin
          docker push your-registry/user-service:latest
```

---

## Emergency Commands

```bash
# Clear everything and restart clean
docker-compose down -v && docker-compose up -d

# Reset Kubernetes cluster
kubectl delete namespace ecommerce
kubectl create namespace ecommerce
kubectl apply -f k8s/

# Kill process on port
lsof -ti:8001 | xargs kill -9

# Monitor real-time logs
docker-compose logs -f --tail=100

# Check disk space
docker system df

# Clean up dangling resources
docker system prune -a --volumes
```

---

## Documentation Resources

- **Spring Cloud**: https://spring.io/projects/spring-cloud
- **Spring Cloud Gateway**: https://cloud.spring.io/spring-cloud-gateway
- **Spring Data JPA**: https://spring.io/projects/spring-data-jpa
- **Apache Kafka**: https://kafka.apache.org/documentation/
- **PostgreSQL**: https://www.postgresql.org/docs/
- **Redis**: https://redis.io/documentation
- **Kubernetes**: https://kubernetes.io/docs/
- **Docker**: https://docs.docker.com/

---

## Success Metrics for Internship

✅ **Demonstrate understanding of**:
- Microservices architecture patterns
- Event-driven communication with Kafka
- Spring Cloud ecosystem (Eureka, Config Server, Gateway)
- Distributed transaction handling
- Caching strategies
- Container orchestration (Docker & Kubernetes)
- Monitoring and observability

✅ **Code quality**:
- Clean, readable, well-documented code
- Comprehensive error handling
- Proper logging at appropriate levels
- 70%+ test coverage
- Adherence to SOLID principles

✅ **Production readiness**:
- Health checks and graceful shutdown
- Resource limits and auto-scaling
- Security (JWT, encrypted secrets)
- Database migrations (Liquibase/Flyway)
- API documentation (Swagger/OpenAPI)
- CI/CD pipeline
- Monitoring and alerting

This checklist shows mastery of modern backend engineering practices!
