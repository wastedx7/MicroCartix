# docker-compose.yml - Complete Local Development Stack

version: '3.8'

services:
  # ===== DATABASES =====
  postgres-user:
    image: postgres:15-alpine
    container_name: postgres-user
    environment:
      POSTGRES_DB: ecommerce_user
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres_user_data:/var/lib/postgresql/data
    networks:
      - ecommerce-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  postgres-product:
    image: postgres:15-alpine
    container_name: postgres-product
    environment:
      POSTGRES_DB: ecommerce_product
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5433:5432"
    volumes:
      - postgres_product_data:/var/lib/postgresql/data
    networks:
      - ecommerce-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  postgres-order:
    image: postgres:15-alpine
    container_name: postgres-order
    environment:
      POSTGRES_DB: ecommerce_order
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5434:5432"
    volumes:
      - postgres_order_data:/var/lib/postgresql/data
    networks:
      - ecommerce-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  postgres-payment:
    image: postgres:15-alpine
    container_name: postgres-payment
    environment:
      POSTGRES_DB: ecommerce_payment
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5435:5432"
    volumes:
      - postgres_payment_data:/var/lib/postgresql/data
    networks:
      - ecommerce-network

  # ===== CACHING =====
  redis:
    image: redis:7-alpine
    container_name: redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    networks:
      - ecommerce-network
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

  # ===== MESSAGE BROKER =====
  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_SYNC_LIMIT: 2
    ports:
      - "2181:2181"
    networks:
      - ecommerce-network

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    container_name: kafka
    depends_on:
      zookeeper:
        condition: service_started
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    ports:
      - "9092:9092"
    networks:
      - ecommerce-network
    healthcheck:
      test: ["CMD-SHELL", "kafka-broker-api-versions.sh --bootstrap-server localhost:9092 | grep -q ApiVersion"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ===== SERVICE DISCOVERY =====
  eureka-server:
    build:
      context: ./eureka-server
      dockerfile: Dockerfile
    container_name: eureka-server
    ports:
      - "8761:8761"
    environment:
      EUREKA_HOSTNAME: eureka-server
      SERVER_PORT: 8761
    networks:
      - ecommerce-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8761/eureka/apps"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ===== CONFIG SERVER =====
  config-server:
    build:
      context: ./config-server
      dockerfile: Dockerfile
    container_name: config-server
    ports:
      - "8888:8888"
    environment:
      SERVER_PORT: 8888
      SPRING_PROFILES_ACTIVE: native
      SPRING_CLOUD_CONFIG_SERVER_NATIVE_SEARCH_LOCATIONS: classpath:/config
    depends_on:
      eureka-server:
        condition: service_healthy
    networks:
      - ecommerce-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8888/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ===== API GATEWAY =====
  api-gateway:
    build:
      context: ./api-gateway
      dockerfile: Dockerfile
    container_name: api-gateway
    ports:
      - "8080:8080"
    environment:
      SERVER_PORT: 8080
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
      SPRING_CONFIG_IMPORT: configserver:http://config-server:8888
      SPRING_CLOUD_CONFIG_ENABLED: "true"
    depends_on:
      eureka-server:
        condition: service_healthy
      config-server:
        condition: service_healthy
    networks:
      - ecommerce-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ===== MICROSERVICES =====
  user-service:
    build:
      context: ./user-service
      dockerfile: Dockerfile
    container_name: user-service
    ports:
      - "8001:8001"
    environment:
      SERVER_PORT: 8001
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-user:5432/ecommerce_user
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_DATASOURCE_DRIVER_CLASS_NAME: org.postgresql.Driver
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
      SPRING_CONFIG_IMPORT: configserver:http://config-server:8888
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
    depends_on:
      postgres-user:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - ecommerce-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8001/actuator/health"]
      interval: 10s
      timeout: 5s
      retries: 5

  product-service:
    build:
      context: ./product-service
      dockerfile: Dockerfile
    container_name: product-service
    ports:
      - "8002:8002"
    environment:
      SERVER_PORT: 8002
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-product:5432/ecommerce_product
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_DATASOURCE_DRIVER_CLASS_NAME: org.postgresql.Driver
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
      SPRING_CONFIG_IMPORT: configserver:http://config-server:8888
      SPRING_REDIS_HOST: redis
    depends_on:
      postgres-product:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - ecommerce-network

  order-service:
    build:
      context: ./order-service
      dockerfile: Dockerfile
    container_name: order-service
    ports:
      - "8003:8003"
    environment:
      SERVER_PORT: 8003
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-order:5432/ecommerce_order
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_DATASOURCE_DRIVER_CLASS_NAME: org.postgresql.Driver
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
      SPRING_CONFIG_IMPORT: configserver:http://config-server:8888
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      SPRING_REDIS_HOST: redis
    depends_on:
      postgres-order:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
      kafka:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - ecommerce-network

  payment-service:
    build:
      context: ./payment-service
      dockerfile: Dockerfile
    container_name: payment-service
    ports:
      - "8004:8004"
    environment:
      SERVER_PORT: 8004
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres-payment:5432/ecommerce_payment
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SPRING_DATASOURCE_DRIVER_CLASS_NAME: org.postgresql.Driver
      SPRING_JPA_HIBERNATE_DDL_AUTO: update
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
      SPRING_CONFIG_IMPORT: configserver:http://config-server:8888
      SPRING_REDIS_HOST: redis
      STRIPE_API_KEY: ${STRIPE_API_KEY:-sk_test_fake}
    depends_on:
      postgres-payment:
        condition: service_healthy
      eureka-server:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - ecommerce-network

  notification-service:
    build:
      context: ./notification-service
      dockerfile: Dockerfile
    container_name: notification-service
    ports:
      - "8005:8005"
    environment:
      SERVER_PORT: 8005
      EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka/
      SPRING_CONFIG_IMPORT: configserver:http://config-server:8888
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      SPRING_MAIL_HOST: smtp.gmail.com
      SPRING_MAIL_PORT: 587
      SPRING_MAIL_USERNAME: ${MAIL_USERNAME:-your-email@gmail.com}
      SPRING_MAIL_PASSWORD: ${MAIL_PASSWORD:-your-app-password}
    depends_on:
      eureka-server:
        condition: service_healthy
      kafka:
        condition: service_healthy
    networks:
      - ecommerce-network

  # ===== MONITORING =====
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus_data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
    networks:
      - ecommerce-network

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_PASSWORD: admin
      GF_INSTALL_PLUGINS: grafana-piechart-panel
    volumes:
      - grafana_data:/var/lib/grafana
    depends_on:
      - prometheus
    networks:
      - ecommerce-network

  # ===== LOGGING (OPTIONAL) =====
  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.5.0
    container_name: elasticsearch
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch_data:/usr/share/elasticsearch/data
    networks:
      - ecommerce-network

  kibana:
    image: docker.elastic.co/kibana/kibana:8.5.0
    container_name: kibana
    ports:
      - "5601:5601"
    environment:
      ELASTICSEARCH_HOSTS: http://elasticsearch:9200
    depends_on:
      - elasticsearch
    networks:
      - ecommerce-network

networks:
  ecommerce-network:
    driver: bridge

volumes:
  postgres_user_data:
  postgres_product_data:
  postgres_order_data:
  postgres_payment_data:
  redis_data:
  prometheus_data:
  grafana_data:
  elasticsearch_data:


---

# monitoring/prometheus.yml - Prometheus Configuration

global:
  scrape_interval: 15s
  evaluation_interval: 15s
  external_labels:
    monitor: 'ecommerce-monitor'

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'user-service'
    static_configs:
      - targets: ['user-service:8001']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s

  - job_name: 'product-service'
    static_configs:
      - targets: ['product-service:8002']
    metrics_path: '/actuator/prometheus'

  - job_name: 'order-service'
    static_configs:
      - targets: ['order-service:8003']
    metrics_path: '/actuator/prometheus'

  - job_name: 'payment-service'
    static_configs:
      - targets: ['payment-service:8004']
    metrics_path: '/actuator/prometheus'

  - job_name: 'notification-service'
    static_configs:
      - targets: ['notification-service:8005']
    metrics_path: '/actuator/prometheus'

  - job_name: 'api-gateway'
    static_configs:
      - targets: ['api-gateway:8080']
    metrics_path: '/actuator/prometheus'

  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka:9092']

  - job_name: 'redis'
    static_configs:
      - targets: ['redis:6379']


---

# k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: ecommerce

---

# k8s/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: db-config
  namespace: ecommerce
data:
  user-db-url: "jdbc:postgresql://postgres-user:5432/ecommerce_user"
  product-db-url: "jdbc:postgresql://postgres-product:5432/ecommerce_product"
  order-db-url: "jdbc:postgresql://postgres-order:5432/ecommerce_order"
  payment-db-url: "jdbc:postgresql://postgres-payment:5432/ecommerce_payment"
  eureka-url: "http://eureka-server.ecommerce.svc.cluster.local:8761/eureka/"
  kafka-brokers: "kafka.ecommerce.svc.cluster.local:9092"
  redis-host: "redis.ecommerce.svc.cluster.local"

---

# k8s/secret.yaml
apiVersion: v1
kind: Secret
metadata:
  name: db-credentials
  namespace: ecommerce
type: Opaque
stringData:
  db-password: postgres
  stripe-key: sk_test_XXXXXXXXXXXX
  mail-username: your-email@gmail.com
  mail-password: your-app-password

---

# k8s/postgres-statefulset.yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: postgres-user-pv
spec:
  capacity:
    storage: 10Gi
  accessModes:
    - ReadWriteOnce
  hostPath:
    path: /data/postgres-user

---

apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres-user
  namespace: ecommerce
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
          name: postgres
        env:
        - name: POSTGRES_DB
          value: ecommerce_user
        - name: POSTGRES_USER
          value: postgres
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: db-password
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          exec:
            command:
            - /bin/sh
            - -c
            - pg_isready -U postgres
          initialDelaySeconds: 30
          periodSeconds: 10
  volumeClaimTemplates:
  - metadata:
      name: postgres-storage
    spec:
      accessModes: ["ReadWriteOnce"]
      resources:
        requests:
          storage: 10Gi

---

apiVersion: v1
kind: Service
metadata:
  name: postgres-user
  namespace: ecommerce
spec:
  clusterIP: None
  selector:
    app: postgres-user
  ports:
  - port: 5432
    targetPort: 5432

---

# k8s/eureka-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: eureka-server
  namespace: ecommerce
spec:
  replicas: 1
  selector:
    matchLabels:
      app: eureka-server
  template:
    metadata:
      labels:
        app: eureka-server
    spec:
      containers:
      - name: eureka-server
        image: your-registry/eureka-server:1.0.0
        ports:
        - containerPort: 8761
        env:
        - name: SERVER_PORT
          value: "8761"
        - name: EUREKA_HOSTNAME
          value: eureka-server.ecommerce.svc.cluster.local
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /eureka/apps
            port: 8761
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /eureka/apps
            port: 8761
          initialDelaySeconds: 10
          periodSeconds: 5

---

apiVersion: v1
kind: Service
metadata:
  name: eureka-server
  namespace: ecommerce
spec:
  selector:
    app: eureka-server
  ports:
  - port: 8761
    targetPort: 8761
  type: ClusterIP

---

# k8s/user-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  namespace: ecommerce
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
      affinity:
        podAntiAffinity:
          preferredDuringSchedulingIgnoredDuringExecution:
          - weight: 100
            podAffinityTerm:
              labelSelector:
                matchExpressions:
                - key: app
                  operator: In
                  values:
                  - user-service
              topologyKey: kubernetes.io/hostname
      containers:
      - name: user-service
        image: your-registry/user-service:1.0.0
        imagePullPolicy: Always
        ports:
        - containerPort: 8001
          name: http
        env:
        - name: SERVER_PORT
          value: "8001"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            configMapKeyRef:
              name: db-config
              key: user-db-url
        - name: SPRING_DATASOURCE_USERNAME
          value: postgres
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: db-credentials
              key: db-password
        - name: EUREKA_CLIENT_SERVICEURL_DEFAULTZONE
          valueFrom:
            configMapKeyRef:
              name: db-config
              key: eureka-url
        - name: SPRING_REDIS_HOST
          valueFrom:
            configMapKeyRef:
              name: db-config
              key: redis-host
        resources:
          requests:
            memory: "256Mi"
            cpu: "250m"
          limits:
            memory: "512Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8001
          initialDelaySeconds: 40
          periodSeconds: 10
          timeoutSeconds: 5
          failureThreshold: 3
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8001
          initialDelaySeconds: 20
          periodSeconds: 5
          timeoutSeconds: 3
          failureThreshold: 2

---

apiVersion: v1
kind: Service
metadata:
  name: user-service
  namespace: ecommerce
spec:
  selector:
    app: user-service
  ports:
  - port: 8001
    targetPort: 8001
    protocol: TCP
  type: ClusterIP

---

# k8s/ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ecommerce-ingress
  namespace: ecommerce
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  ingressClassName: nginx
  rules:
  - host: api.ecommerce.local
    http:
      paths:
      - path: /users
        pathType: Prefix
        backend:
          service:
            name: user-service
            port:
              number: 8001
      - path: /products
        pathType: Prefix
        backend:
          service:
            name: product-service
            port:
              number: 8002
      - path: /orders
        pathType: Prefix
        backend:
          service:
            name: order-service
            port:
              number: 8003
      - path: /payments
        pathType: Prefix
        backend:
          service:
            name: payment-service
            port:
              number: 8004
  tls:
  - hosts:
    - api.ecommerce.local
    secretName: ecommerce-tls

---

# k8s/hpa.yaml - Horizontal Pod Autoscaler
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: user-service-hpa
  namespace: ecommerce
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: user-service
  minReplicas: 2
  maxReplicas: 5
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80

---

# k8s/network-policy.yaml - Network Security
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: ecommerce-network-policy
  namespace: ecommerce
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - podSelector: {}
    - namespaceSelector:
        matchLabels:
          name: ingress-nginx
  egress:
  - to:
    - podSelector: {}
  - to:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 53
    - protocol: UDP
      port: 53

---

# Deployment commands

# Initialize cluster
kubectl create namespace ecommerce
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/network-policy.yaml

# Deploy infrastructure
kubectl apply -f k8s/postgres-statefulset.yaml
kubectl apply -f k8s/eureka-deployment.yaml

# Deploy services
kubectl apply -f k8s/user-service-deployment.yaml
kubectl apply -f k8s/product-service-deployment.yaml
kubectl apply -f k8s/order-service-deployment.yaml
kubectl apply -f k8s/payment-service-deployment.yaml
kubectl apply -f k8s/notification-service-deployment.yaml

# Deploy autoscaling
kubectl apply -f k8s/hpa.yaml

# Deploy ingress
kubectl apply -f k8s/ingress.yaml

# Verify deployment
kubectl get pods -n ecommerce
kubectl get svc -n ecommerce
kubectl logs -f deployment/user-service -n ecommerce
kubectl describe pod user-service-xxxxx -n ecommerce

# Port forwarding for local testing
kubectl port-forward svc/user-service 8001:8001 -n ecommerce
kubectl port-forward svc/eureka-server 8761:8761 -n ecommerce
