#!/bin/bash

# MuniTax Docker Deployment Script
# This script builds and deploys all microservices

set -e

echo "🚀 MuniTax - Docker Deployment"
echo "================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Step 1: Clean up existing containers
echo -e "${BLUE}Step 1: Cleaning up existing containers...${NC}"
docker-compose down -v 2>/dev/null || true
echo -e "${GREEN}✓ Cleanup complete${NC}"
echo ""

# Step 2: Build backend services
echo -e "${BLUE}Step 2: Building backend services...${NC}"
cd backend
mvn clean package -DskipTests
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Backend build successful${NC}"
else
    echo -e "${RED}✗ Backend build failed${NC}"
    exit 1
fi
cd ..
echo ""

# Step 3: Build and start all services
echo -e "${BLUE}Step 3: Building and starting Docker containers...${NC}"
docker-compose up -d --build

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ All containers started successfully${NC}"
else
    echo -e "${RED}✗ Failed to start containers${NC}"
    exit 1
fi
echo ""

# Step 4: Wait for services to be healthy
echo -e "${BLUE}Step 4: Waiting for services to be ready...${NC}"
echo "This may take 1-2 minutes..."

# Wait for PostgreSQL
echo -n "Waiting for PostgreSQL..."
until docker exec munitax-postgres pg_isready -U postgres > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo -e " ${GREEN}✓${NC}"

# Wait for Eureka (Discovery Service)
echo -n "Waiting for Eureka Discovery Service..."
until curl -s http://localhost:8761/actuator/health > /dev/null 2>&1; do
    echo -n "."
    sleep 3
done
echo -e " ${GREEN}✓${NC}"

# Wait for Gateway
echo -n "Waiting for API Gateway..."
until curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; do
    echo -n "."
    sleep 3
done
echo -e " ${GREEN}✓${NC}"

# Wait for Auth Service
echo -n "Waiting for Auth Service..."
sleep 10
echo -e " ${GREEN}✓${NC}"

echo ""
echo -e "${GREEN}================================${NC}"
echo -e "${GREEN}🎉 Deployment Complete!${NC}"
echo -e "${GREEN}================================${NC}"
echo ""
echo -e "${YELLOW}Service URLs:${NC}"
echo "  Frontend:          http://localhost:3000"
echo "  API Gateway:       http://localhost:8080"
echo "  Eureka Dashboard:  http://localhost:8761"
echo "  Zipkin Tracing:    http://localhost:9411"
echo "  PostgreSQL:        localhost:5432"
echo ""
echo -e "${YELLOW}Service Status:${NC}"
docker-compose ps
echo ""
echo -e "${YELLOW}To view logs:${NC}"
echo "  All services:      docker-compose logs -f"
echo "  Specific service:  docker-compose logs -f <service-name>"
echo ""
echo -e "${YELLOW}To stop all services:${NC}"
echo "  docker-compose down"
echo ""
