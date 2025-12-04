# Self-Healing End-to-End Deployment Workflow

## Overview

This document describes the self-healing, end-to-end deployment workflow implemented for the MuniTax application. The workflow automates deployment of all microservices, infrastructure, and frontend using GitHub Actions with built-in self-healing capabilities.

## Workflow Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Self-Healing Deployment Pipeline                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐    ┌───────────┐    ┌───────────────────┐    ┌─────────┐ │
│  │ Build & Test │───▶│  Deploy   │───▶│ Verify Deployment │───▶│ Notify  │ │
│  └──────────────┘    └───────────┘    └───────────────────┘    └─────────┘ │
│         │                  │                    │                    │      │
│         │                  │                    │                    │      │
│         ▼                  ▼                    ▼                    ▼      │
│  ┌──────────────┐    ┌───────────┐    ┌───────────────────┐    ┌─────────┐ │
│  │   Artifacts  │    │Self-Heal  │    │    AI Analysis    │    │Rollback │ │
│  └──────────────┘    └───────────┘    └───────────────────┘    └─────────┘ │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Workflow Triggers

The deployment workflow can be triggered by:

1. **Push to main branch**: Automatic deployment on merge
2. **Pull request to main**: Verification of deployment capability
3. **Manual dispatch**: On-demand deployment with configurable options

### Manual Dispatch Options

| Option | Description | Default |
|--------|-------------|---------|
| `enable_ai_analysis` | Enable AI-powered failure analysis | `true` |
| `max_retry_attempts` | Maximum retry attempts for failed services | `3` |

## Jobs Overview

### 1. Build & Test (`build-and-test`)

**Purpose**: Build all services and run tests to ensure code quality.

**Steps**:
- Checkout code
- Set up JDK 21 and Node.js 20
- Build backend microservices with Maven
- Build frontend with npm
- Run backend and frontend tests
- Upload build artifacts

**Outputs**:
- `backend_built`: Build status
- `frontend_built`: Build status

### 2. Deploy (`deploy`)

**Purpose**: Deploy all services using Docker Compose with health checks.

**Steps**:
1. Build Docker images
2. Start infrastructure (PostgreSQL, Redis, Zipkin)
3. Start Discovery Service (Eureka)
4. Start Gateway Service
5. Start backend microservices
6. Start Frontend
7. Run health checks
8. Self-heal failed services (if any)
9. Collect service logs

**Outputs**:
- `deployment_status`: `success`, `healed`, `degraded`, or `failed`
- `failed_services`: List of services that couldn't be healed

### 3. Verify Deployment (`verify-deployment`)

**Purpose**: Verify deployment with screenshots and API tests.

**Steps**:
1. Start services
2. Run API endpoint tests
3. Capture screenshots of dashboards
4. Run E2E smoke tests
5. Generate test report
6. Upload test results as artifacts

**Artifacts**:
- `deployment-test-results/screenshots/`: Screenshots of dashboards
- `deployment-test-results/api-responses/`: JSON API responses

### 4. AI Analysis (`ai-analysis`)

**Purpose**: Analyze failures and provide recommendations.

**Triggers**: Only runs when deployment status is `degraded` or `failed`.

**Provides**:
- Root cause analysis
- Service-specific recommendations
- Log excerpts
- Next steps guidance

### 5. Rollback (`rollback`)

**Purpose**: Handle critical deployment failures.

**Triggers**: Only runs when deployment status is `failed`.

**Actions**:
- Document failure
- Provide rollback instructions
- Create rollback report

### 6. Notify (`notify`)

**Purpose**: Provide deployment summary.

**Always runs**: Yes (after all other jobs)

**Reports**:
- Build status
- Deploy status
- Verification status
- Available artifacts

## Self-Healing Mechanism

The deployment includes a self-healing mechanism that:

1. **Detects failures**: Health checks identify unhealthy services
2. **Attempts recovery**: Failed services are restarted up to N times (configurable)
3. **Logs analysis**: Logs are collected before each restart attempt
4. **Reports status**: Final status indicates if healing was successful

### Recovery Process

```
For each failed service:
    For attempt in 1..MAX_RETRIES:
        1. Collect pre-restart logs
        2. Restart the service
        3. Wait 15 seconds
        4. Check health
        5. If healthy → mark as healed, break
    If still unhealthy after all attempts:
        Mark as failed
```

## Health Checks

### Infrastructure Services

| Service | Health Check |
|---------|--------------|
| PostgreSQL | `pg_isready -U postgres` |
| Redis | `redis-cli ping` |
| Zipkin | `wget http://localhost:9411/health` |

### Microservices

| Service | Port | Health Endpoint |
|---------|------|-----------------|
| Discovery Service | 8761 | `/actuator/health` |
| Gateway Service | 8080 | `/actuator/health` |
| Auth Service | 8081 | `/actuator/health` |
| Tenant Service | 8082 | `/actuator/health` |
| Extraction Service | 8083 | `/actuator/health` |
| Submission Service | 8084 | `/actuator/health` |
| Tax Engine Service | 8085 | `/actuator/health` |
| PDF Service | 8086 | `/actuator/health` |
| Ledger Service | 8087 | `/actuator/health` |
| Rule Service | 8089 | `/actuator/health` |

### Frontend

| Service | Port | Health Check |
|---------|------|--------------|
| Frontend (nginx) | 3000 (external), 80 (internal) | HTTP GET `/` |

## Artifacts

### Deployment Logs (`deployment-logs`)

Contains:
- `docker-build.log`: Docker build output
- `all-services.log`: Combined service logs
- `{service}.log`: Individual service logs
- `health-check.json`: Health check results
- `healing-report.json`: Self-healing results
- `container-status.json`: Docker container status

### Test Results (`deployment-test-results`)

Contains:
- `screenshots/frontend.png`: Frontend screenshot
- `screenshots/eureka-dashboard.png`: Eureka dashboard screenshot
- `screenshots/zipkin-dashboard.png`: Zipkin dashboard screenshot
- `api-responses/`: JSON API responses
- `deployment-verification-report.json`: Summary report

## Failure Scenarios & Handling

### Scenario 1: Service Crash

**Detection**: Health check fails
**Action**: Service is automatically restarted (up to MAX_RETRIES times)
**Result**: Service marked as `healed` or `failed`

### Scenario 2: Database Unavailable

**Detection**: PostgreSQL health check fails
**Action**: Wait for PostgreSQL to be ready (30 retries)
**Result**: Dependent services wait for `service_healthy` condition

### Scenario 3: Build Failure

**Detection**: Maven or npm build fails
**Action**: Workflow fails at build stage
**Result**: Deployment does not proceed

### Scenario 4: Port Conflict

**Detection**: Service fails to start on expected port
**Action**: Collected in logs for analysis
**Result**: AI analysis provides recommendations

## Test Cases

### Deployment Verification Tests

The `deployment-verification.spec.ts` test suite includes:

1. **Infrastructure Health Checks**
   - Eureka Discovery Service health
   - Gateway Service health
   - Zipkin availability

2. **Frontend Application**
   - Frontend loads successfully
   - Main content is rendered

3. **Service Dashboards**
   - Eureka Dashboard accessible
   - Zipkin Dashboard accessible

4. **API Gateway Routes**
   - Actuator info endpoint
   - Routes configuration

5. **Eureka Service Registry**
   - Registered services listing

6. **Deployment Summary**
   - Generate comprehensive summary report

### Running Tests Locally

```bash
# Run deployment verification tests
npm run test:deployment

# Run with headed browser (for debugging)
npm run test:deployment:headed
```

## Usage

### Automatic Deployment

Push to main branch or merge a PR:

```bash
git push origin main
```

### Manual Deployment

1. Go to GitHub Actions
2. Select "Self-Healing End-to-End Deployment"
3. Click "Run workflow"
4. Configure options:
   - Enable/disable AI analysis
   - Set max retry attempts
5. Click "Run workflow"

### Viewing Results

1. Go to the workflow run
2. Download artifacts:
   - `deployment-logs`: Service logs and status
   - `deployment-test-results`: Screenshots and API responses
3. Review job summaries for status

## Troubleshooting

### Common Issues

1. **Services taking too long to start**
   - Increase `start_period` in health checks
   - Check for resource constraints

2. **Database connection failures**
   - Verify PostgreSQL is healthy
   - Check database credentials

3. **Service discovery issues**
   - Ensure Discovery Service is healthy
   - Check Eureka configuration

### Debug Mode

To debug locally:

```bash
# Start services with logs
docker compose up

# Check specific service logs
docker compose logs -f service-name

# Run health checks manually
curl http://localhost:8761/actuator/health
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `FRONTEND_URL` | Frontend URL for tests | `http://localhost:3000` |
| `GATEWAY_URL` | Gateway URL for tests | `http://localhost:8080` |
| `EUREKA_URL` | Eureka URL for tests | `http://localhost:8761` |
| `ZIPKIN_URL` | Zipkin URL for tests | `http://localhost:9411` |
| `RESULTS_DIR` | Test results directory | `test-results` |

### Modifying Retry Behavior

Edit the workflow dispatch inputs or modify the `MAX_RETRIES` environment variable.

## Security Considerations

1. **Workflow Permissions**: The workflow uses explicit minimal permissions (`contents: read`, `actions: read`) following security best practices
2. **Secrets**: Database passwords should use GitHub Secrets in production
3. **JWT Secrets**: Use strong, unique secrets via environment variables
4. **Network**: Services communicate over internal Docker network
5. **Access**: External access limited to specific ports

## Future Enhancements

1. **Blue-Green Deployment**: Zero-downtime deployments
2. **Canary Releases**: Gradual rollout capability
3. **Prometheus Metrics**: Enhanced monitoring
4. **Slack/Teams Notifications**: Integrated alerting
5. **Kubernetes Support**: K8s deployment option
