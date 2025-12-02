# GitHub Actions Workflows

This directory contains GitHub Actions workflows for CI/CD and agent orchestration in the MuniTax project.

## Overview

The workflows are designed to:
1. **Test and build** the frontend (React/TypeScript) and backend (Java/Spring Boot)
2. **Deploy** the full stack including Docker images for microservices
3. **Run E2E tests** with Postgres, Redis, and backend services
4. **Orchestrate speckit agents** for spec-driven development

## Workflows

### Frontend CI (`frontend-ci.yml`)

Builds and tests the React/TypeScript frontend.

**Triggers:**
- Push to `main` branch (frontend-related paths)
- Pull requests to `main` branch (frontend-related paths)
- Manual workflow dispatch

**Steps:**
1. Install Node.js dependencies
2. TypeScript type checking
3. Run unit tests (Vitest)
4. Build production bundle

**Artifacts:**
- `frontend-dist` - Production build output

---

### Backend CI (`backend-ci.yml`)

Builds and tests the Java/Spring Boot backend microservices.

**Triggers:**
- Push to `main` branch (backend paths)
- Pull requests to `main` branch (backend paths)
- Manual workflow dispatch

**Services:**
- PostgreSQL 16 (for integration tests)

**Steps:**
1. Setup JDK 21
2. Maven build with tests (`mvn clean verify`)
3. Upload test reports
4. Upload JaCoCo coverage reports

**Artifacts:**
- `backend-test-reports` - Surefire test reports
- `backend-coverage-reports` - JaCoCo coverage HTML reports

---

### Full Stack Deploy (`deploy.yml`)

Builds Docker images and deploys all services.

**Triggers:**
- Push to `main` branch
- Version tags (`v*`)
- Manual workflow dispatch with environment selection

**Inputs:**
- `environment` - Target deployment environment (staging/production)

**Jobs:**
1. **build-frontend** - Builds and pushes frontend Docker image
2. **build-backend** - Builds and pushes all 10 backend service images (parallel matrix)
3. **deploy** - Deploys to the selected environment

**Docker Images:**
Images are pushed to GitHub Container Registry (`ghcr.io`) with tags:
- Git SHA
- Branch name
- Version tag (for releases)

---

### E2E Tests (`e2e-tests.yml`)

Runs Playwright E2E tests against the full stack.

**Triggers:**
- Push to `main` branch
- Pull requests to `main` branch
- Manual workflow dispatch

**Services:**
- PostgreSQL 16
- Redis 7

**Steps:**
1. Install frontend dependencies
2. Install Playwright browsers
3. Build backend services
4. Start discovery and gateway services
5. Run Playwright tests

**Artifacts:**
- `playwright-report` - HTML test report
- `playwright-screenshots` - Screenshots on failure

---

### Speckit Agent Orchestration (`speckit-orchestration.yml`)

Orchestrates the full spec-driven development workflow using speckit agents.

**Triggers:**
- Manual workflow dispatch only

**Inputs:**
- `feature_description` (required) - Description for the new feature
- `short_name` (optional) - Short name for feature branch
- `run_agents` - Comma-separated list of agents to run
- `skip_implementation` - Skip the implementation step (default: true)

**Available Agents:**
| Agent | Purpose |
|-------|---------|
| `specify` | Create feature specification from description |
| `clarify` | Identify underspecified areas with targeted questions |
| `plan` | Generate implementation plan (plan.md) |
| `tasks` | Generate actionable task list (tasks.md) |
| `checklist` | Generate verification checklist |
| `analyze` | Cross-artifact consistency analysis |
| `implement` | Execute implementation tasks |
| `documentation` | Generate feature documentation |
| `constitution` | Verify constitution compliance |

**Workflow Sequence:**
```
specify → clarify → plan → tasks → checklist → analyze
                                        ↓
                                   implement → documentation
                                        ↓
                                   constitution
```

**Example Usage:**
```bash
gh workflow run speckit-orchestration.yml \
  --field feature_description="Add NOL carryforward tracking" \
  --field short_name="nol-carryforward" \
  --field run_agents="specify,clarify,plan,tasks"
```

---

## Environment Variables

### Required Secrets

| Secret | Description |
|--------|-------------|
| `GITHUB_TOKEN` | Auto-provided, used for container registry |
| `GEMINI_API_KEY` | Optional, for AI extraction service |

### Deployment Environments

Configure these environments in GitHub repository settings:
- `staging` - Pre-production environment
- `production` - Production environment (requires approval)

---

## Extending Workflows

### Adding a New Service

1. Add service name to the matrix in `deploy.yml`:
```yaml
matrix:
  service:
    - your-new-service
```

2. Ensure the service follows the standard Dockerfile pattern in `backend/`

### Adding New Agents

1. Create agent prompt in `.github/prompts/speckit.<agent>.prompt.md`
2. Add job to `speckit-orchestration.yml`:
```yaml
your-agent:
  name: Your Agent Description
  runs-on: ubuntu-latest
  needs: [specify, ...]
  if: contains(github.event.inputs.run_agents, 'your-agent')
  steps:
    - uses: actions/checkout@v4
      with:
        ref: ${{ needs.specify.outputs.branch_name }}
    # Add agent-specific steps
```

3. Update the summary job to include the new agent
4. Document the agent in this README

---

## Local Testing

### Test Frontend Locally
```bash
npm ci
npm run test
npm run build
```

### Test Backend Locally
```bash
cd backend
mvn clean verify
```

### Run E2E Tests Locally
```bash
# Start services (requires Docker)
docker-compose up -d postgres redis

# Build and run backend
cd backend && mvn clean package -DskipTests
java -jar discovery-service/target/*.jar &
java -jar gateway-service/target/*.jar &

# Run tests
npm run test:e2e
```

### Test Speckit Scripts
```bash
# Create a new feature
pwsh .specify/scripts/powershell/create-new-feature.ps1 "My feature description"

# Check prerequisites
pwsh .specify/scripts/powershell/check-prerequisites.ps1 -Json
```

---

## Troubleshooting

### Backend Build Fails
- Ensure JDK 21 is available
- Check PostgreSQL connection in tests
- Review Maven logs for dependency issues

### E2E Tests Fail
- Check if all services are healthy
- Review Playwright screenshots in artifacts
- Ensure ports are not conflicting

### Docker Build Fails
- Verify Dockerfile syntax
- Check build context paths
- Ensure base images are accessible

---

## Related Documentation

- [Deploy Script](../../../deploy.sh) - Original deployment script
- [Docker Compose](../../../docker-compose.yml) - Local development setup
- [Constitution](../../../.specify/memory/constitution.md) - Project principles
- [Spec Example](../../../specs/1-withholding-reconciliation/) - Feature specification example
