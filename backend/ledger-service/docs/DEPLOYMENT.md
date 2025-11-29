# Deployment Guide - Ledger Service

This guide covers deploying the Ledger Service to various environments.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Configuration](#environment-configuration)
3. [Docker Deployment](#docker-deployment)
4. [Kubernetes Deployment](#kubernetes-deployment)
5. [Cloud Platform Deployment](#cloud-platform-deployment)
6. [Database Migrations](#database-migrations)
7. [Health Checks & Monitoring](#health-checks--monitoring)
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

- **Java**: JDK 17 or later
- **Maven**: 3.6+ (for building)
- **Docker**: 20.10+ (for containerized deployment)
- **PostgreSQL**: 13+ (for database)

### Required Access

- Database credentials
- Service registry (Eureka) access
- Payment gateway credentials (for production mode)
- Cloud platform credentials (if deploying to cloud)

---

## Environment Configuration

### Required Environment Variables

```bash
# Database
export DATABASE_URL="jdbc:postgresql://postgres-host:5432/munitax_ledger"
export DATABASE_USERNAME="ledger_user"
export DATABASE_PASSWORD="your-secure-password"

# Payment Provider
export PAYMENT_MODE="TEST"  # or "PRODUCTION"
export PAYMENT_PROVIDER="MOCK"  # or "STRIPE", "AUTHORIZE_NET", etc.

# Security
export CORS_ALLOWED_ORIGINS="https://app.munitax.com,https://admin.munitax.com"
export RATE_LIMIT_ENABLED="true"

# Service Discovery (optional)
export EUREKA_URL="http://eureka-server:8761/eureka/"
```

### Optional Environment Variables

```bash
# Logging
export LOG_LEVEL_ROOT="INFO"
export LOG_LEVEL_APP="DEBUG"
export LOG_FILE_PATH="/var/log/munitax/ledger-service.log"

# Performance
export SERVER_THREADS_MAX="200"
export DB_POOL_SIZE="20"

# Features
export FEATURE_REFUNDS_ENABLED="true"
export FEATURE_ACH_ENABLED="true"
export FEATURE_AUTO_RECON_ENABLED="false"

# Swagger
export SWAGGER_ENABLED="true"  # Set to false in production for security
```

### Creating .env File

For Docker Compose, create a `.env` file:

```bash
# .env
POSTGRES_PASSWORD=your-secure-postgres-password
PAYMENT_MODE=TEST
CORS_ALLOWED_ORIGINS=http://localhost:3000
SWAGGER_ENABLED=true
LOG_LEVEL_APP=DEBUG
SPRING_PROFILES_ACTIVE=production
```

---

## Docker Deployment

### 1. Build Docker Image

```bash
cd backend/ledger-service
docker build -t ledger-service:latest .
```

### 2. Run with Docker Compose

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f ledger-service

# Stop all services
docker-compose down
```

### 3. Run Individual Container

```bash
docker run -d \
  --name ledger-service \
  -p 8087:8087 \
  -e DATABASE_URL="jdbc:postgresql://host.docker.internal:5432/munitax_ledger" \
  -e DATABASE_USERNAME="postgres" \
  -e DATABASE_PASSWORD="postgres" \
  -e PAYMENT_MODE="TEST" \
  ledger-service:latest
```

### 4. Development Mode (with PgAdmin)

```bash
docker-compose --profile dev up -d
```

Access PgAdmin at: http://localhost:5050

### 5. With Monitoring (Prometheus + Grafana)

```bash
docker-compose --profile monitoring up -d
```

- Prometheus: http://localhost:9090
- Grafana: http://localhost:3001

---

## Kubernetes Deployment

### 1. Create Namespace

```bash
kubectl create namespace munitax
```

### 2. Create ConfigMap

```yaml
# configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ledger-service-config
  namespace: munitax
data:
  PAYMENT_MODE: "TEST"
  CORS_ALLOWED_ORIGINS: "https://app.munitax.com"
  RATE_LIMIT_ENABLED: "true"
  LOG_LEVEL_ROOT: "INFO"
  LOG_LEVEL_APP: "INFO"
```

```bash
kubectl apply -f configmap.yaml
```

### 3. Create Secret

```bash
kubectl create secret generic ledger-service-secret \
  --from-literal=DATABASE_PASSWORD='your-password' \
  --from-literal=PAYMENT_GATEWAY_API_KEY='your-api-key' \
  --namespace=munitax
```

### 4. Deploy Service

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ledger-service
  namespace: munitax
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ledger-service
  template:
    metadata:
      labels:
        app: ledger-service
    spec:
      containers:
      - name: ledger-service
        image: your-registry/ledger-service:latest
        ports:
        - containerPort: 8087
        env:
        - name: DATABASE_URL
          value: "jdbc:postgresql://postgres-service:5432/munitax_ledger"
        - name: DATABASE_USERNAME
          value: "ledger_user"
        - name: DATABASE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: ledger-service-secret
              key: DATABASE_PASSWORD
        envFrom:
        - configMapRef:
            name: ledger-service-config
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "2Gi"
            cpu: "2000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8087
          initialDelaySeconds: 60
          periodSeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8087
          initialDelaySeconds: 30
          periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: ledger-service
  namespace: munitax
spec:
  type: LoadBalancer
  ports:
  - port: 80
    targetPort: 8087
  selector:
    app: ledger-service
```

```bash
kubectl apply -f deployment.yaml
```

### 5. Horizontal Pod Autoscaling

```yaml
# hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ledger-service-hpa
  namespace: munitax
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ledger-service
  minReplicas: 3
  maxReplicas: 10
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
```

```bash
kubectl apply -f hpa.yaml
```

---

## Cloud Platform Deployment

### AWS (Elastic Beanstalk)

1. **Install EB CLI**:
```bash
pip install awsebcli
```

2. **Initialize EB**:
```bash
eb init -p docker ledger-service
```

3. **Create Environment**:
```bash
eb create ledger-service-prod \
  --envvars DATABASE_URL=jdbc:postgresql://your-rds-endpoint:5432/munitax_ledger,\
DATABASE_USERNAME=ledger_user,\
DATABASE_PASSWORD=your-password,\
PAYMENT_MODE=PRODUCTION
```

4. **Deploy**:
```bash
eb deploy
```

### AWS (ECS)

See AWS ECS documentation for deploying Docker containers.

### Google Cloud Platform (Cloud Run)

```bash
# Build and push to Container Registry
gcloud builds submit --tag gcr.io/YOUR_PROJECT_ID/ledger-service

# Deploy to Cloud Run
gcloud run deploy ledger-service \
  --image gcr.io/YOUR_PROJECT_ID/ledger-service \
  --platform managed \
  --region us-central1 \
  --allow-unauthenticated \
  --set-env-vars DATABASE_URL=$DATABASE_URL,\
DATABASE_USERNAME=$DATABASE_USERNAME,\
DATABASE_PASSWORD=$DATABASE_PASSWORD,\
PAYMENT_MODE=PRODUCTION
```

### Azure (App Service)

```bash
# Create App Service Plan
az appservice plan create --name ledger-service-plan --resource-group munitax --sku B1 --is-linux

# Create Web App
az webapp create --resource-group munitax --plan ledger-service-plan --name ledger-service --deployment-container-image-name ledger-service:latest

# Configure environment variables
az webapp config appsettings set --resource-group munitax --name ledger-service --settings \
  DATABASE_URL=$DATABASE_URL \
  DATABASE_USERNAME=$DATABASE_USERNAME \
  DATABASE_PASSWORD=$DATABASE_PASSWORD \
  PAYMENT_MODE=PRODUCTION
```

---

## Database Migrations

### Running Flyway Migrations

Migrations run automatically on application startup via Flyway.

### Manual Migration

```bash
# Using Maven
mvn flyway:migrate \
  -Dflyway.url=jdbc:postgresql://your-host:5432/munitax_ledger \
  -Dflyway.user=postgres \
  -Dflyway.password=your-password
```

### Rollback

```bash
# View migration history
mvn flyway:info

# Rollback (requires Flyway Teams edition or manual SQL)
# Manually run rollback scripts from src/main/resources/db/migration
```

### Backup Before Migration

```bash
# PostgreSQL backup
pg_dump -h your-host -U postgres -d munitax_ledger -F c -f backup_$(date +%Y%m%d_%H%M%S).dump

# PostgreSQL restore (if needed)
pg_restore -h your-host -U postgres -d munitax_ledger -c backup_YYYYMMDD_HHMMSS.dump
```

---

## Health Checks & Monitoring

### Health Check Endpoints

```bash
# Overall health
curl http://localhost:8087/actuator/health

# Detailed health (requires authorization)
curl http://localhost:8087/actuator/health/details

# Readiness probe
curl http://localhost:8087/actuator/health/readiness

# Liveness probe
curl http://localhost:8087/actuator/health/liveness
```

### Metrics Endpoints

```bash
# All metrics
curl http://localhost:8087/actuator/metrics

# Specific metric
curl http://localhost:8087/actuator/metrics/jvm.memory.used

# Prometheus format
curl http://localhost:8087/actuator/prometheus
```

### Application Info

```bash
curl http://localhost:8087/actuator/info
```

### Log Monitoring

```bash
# Docker logs
docker logs -f ledger-service

# Kubernetes logs
kubectl logs -f deployment/ledger-service -n munitax

# Tail log file
tail -f /var/log/munitax/ledger-service.log
```

---

## Troubleshooting

### Service Won't Start

1. **Check logs**:
```bash
docker logs ledger-service
# or
kubectl logs deployment/ledger-service -n munitax
```

2. **Check database connectivity**:
```bash
psql -h postgres-host -U postgres -d munitax_ledger -c "SELECT 1;"
```

3. **Verify environment variables**:
```bash
docker exec ledger-service env | grep DATABASE
```

### Database Connection Errors

```bash
# Check PostgreSQL is running
docker ps | grep postgres
# or
kubectl get pods -n munitax | grep postgres

# Test connection
psql -h your-host -U postgres -d munitax_ledger
```

### Out of Memory Errors

1. **Increase container memory**:
```yaml
# docker-compose.yml
services:
  ledger-service:
    deploy:
      resources:
        limits:
          memory: 2G
```

2. **Adjust JVM settings**:
```bash
export JAVA_OPTS="-Xmx1g -Xms512m"
```

### Slow Performance

1. **Check database connection pool**:
```bash
# Increase pool size
export DB_POOL_SIZE=30
```

2. **Check thread pool**:
```bash
export SERVER_THREADS_MAX=300
```

3. **Monitor metrics**:
```bash
curl http://localhost:8087/actuator/metrics/hikaricp.connections
curl http://localhost:8087/actuator/metrics/jvm.threads.live
```

### Migration Failures

1. **Check Flyway history**:
```bash
mvn flyway:info
```

2. **Repair Flyway**:
```bash
mvn flyway:repair
```

3. **Manual fix**:
```sql
-- Connect to database
SELECT * FROM flyway_schema_history;

-- Delete failed migration record
DELETE FROM flyway_schema_history WHERE version = 'X.X';
```

---

## Security Checklist

Before deploying to production:

- [ ] Change all default passwords
- [ ] Use HTTPS/TLS for all connections
- [ ] Set `PAYMENT_MODE=PRODUCTION` only when ready
- [ ] Disable Swagger in production (`SWAGGER_ENABLED=false`)
- [ ] Enable rate limiting
- [ ] Configure CORS properly
- [ ] Use secrets management (Vault, AWS Secrets Manager, etc.)
- [ ] Enable audit logging
- [ ] Set up monitoring and alerting
- [ ] Configure firewall rules
- [ ] Use non-root user in containers
- [ ] Regularly update dependencies
- [ ] Back up database regularly

---

## Support

For deployment support:
- Email: devops@munitax.com
- Slack: #infrastructure
- Documentation: See README.md for additional information
