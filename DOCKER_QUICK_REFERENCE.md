# Docker Deployment - Quick Reference

## 🚀 Deployment Status

**Successfully deployed all services!**

### Services Running:
- ✅ PostgreSQL Database (port 5432)
- ✅ Redis Cache (port 6379)
- ✅ Zipkin Tracing (port 9411)
- ✅ Eureka Discovery Service (port 8761)
- ✅ API Gateway (port 8080)
- ✅ Frontend React App (port 3000)
- ✅ 7 Backend Microservices

## 🔗 Access URLs

| Service | URL | Description |
|---------|-----|-------------|
| **Frontend** | http://localhost:3000 | Main React application |
| **API Gateway** | http://localhost:8080 | Gateway for all backend APIs |
| **Eureka Dashboard** | http://localhost:8761 | Service discovery dashboard |
| **Zipkin Tracing** | http://localhost:9411 | Distributed tracing UI |
| **PostgreSQL** | localhost:5432 | Database (user: postgres, pass: password) |

## 📋 Common Commands

### Start all services
```bash
docker-compose up -d
```

### Stop all services
```bash
docker-compose down
```

### Stop and remove volumes (clean slate)
```bash
docker-compose down -v
```

### View logs
```bash
# All services
docker-compose logs -f

# Specific service
docker logs -f auth-service
docker logs -f gateway-service
docker logs -f munitax-frontend
```

### Check status
```bash
docker ps
docker-compose ps
```

### Rebuild services
```bash
# Rebuild all
docker-compose build

# Rebuild specific service
docker-compose build auth-service
docker-compose build frontend
```

### Restart specific service
```bash
docker-compose restart auth-service
docker-compose restart gateway-service
```

## 🔍 Health Checks

```bash
# Check Eureka
curl http://localhost:8761/actuator/health

# Check Frontend
curl http://localhost:3000

# Check Gateway
curl http://localhost:8080/actuator/health

# Check Auth Service (via gateway)
curl http://localhost:8080/api/v1/users/health
```

## 🛠️ Troubleshooting

### Services not starting?
```bash
# Check logs for errors
docker-compose logs | grep -i error

# Check specific service
docker logs auth-service 2>&1 | grep -i error
```

### Database connection issues?
```bash
# Check PostgreSQL is healthy
docker ps | grep postgres

# Access database
docker exec -it munitax-postgres psql -U postgres -d munitax_db
```

### Port conflicts?
```bash
# Check what's using a port
lsof -i :3000
lsof -i :8080
lsof -i :5432
```

### Clean rebuild
```bash
# Remove everything and start fresh
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d
```

## 📊 Monitoring

### View service registration in Eureka
Open http://localhost:8761 to see all registered microservices

### View traces in Zipkin
Open http://localhost:9411 to see distributed traces

### Check container resources
```bash
docker stats
```

## 🔄 Update Flow

1. Make code changes
2. Rebuild affected service:
   ```bash
   docker-compose build [service-name]
   ```
3. Restart service:
   ```bash
   docker-compose up -d [service-name]
   ```

## 🗄️ Database Access

```bash
# Connect to PostgreSQL
docker exec -it munitax-postgres psql -U postgres -d munitax_db

# Backup database
docker exec munitax-postgres pg_dump -U postgres munitax_db > backup.sql

# Restore database
docker exec -i munitax-postgres psql -U postgres -d munitax_db < backup.sql
```

## 📦 Image Management

```bash
# List images
docker images | grep munitax

# Remove unused images
docker image prune

# Remove specific image
docker rmi munitax---dublin-municipality-tax-calculator-frontend
```

## 🎯 Testing

### Test Frontend
```bash
curl http://localhost:3000
```

### Test Registration via Gateway
```bash
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test123!",
    "firstName": "Test",
    "lastName": "User",
    "phoneNumber": "+15551234567"
  }'
```

### Test Service Discovery
```bash
# See registered services
curl http://localhost:8761/eureka/apps | grep "<app>"
```

## 🔐 Environment Variables

Services use these default credentials:
- **Database:** postgres / password
- **Database Name:** munitax_db
- **JWT Secret:** (configured in auth-service)
- **Gemini API Key:** (configured in extraction-service)

## 📝 Notes

- Services automatically register with Eureka on startup
- Frontend proxies API calls to the gateway (port 8080)
- All inter-service communication goes through Eureka discovery
- PostgreSQL data persists in Docker volume `postgres_data`
- First startup takes longer as services register and initialize

## 🚨 Important

- Always use `docker-compose down -v` if you want to reset the database
- Services need 30-60 seconds to fully start and register with Eureka
- Check Eureka dashboard to verify all services are registered before testing
