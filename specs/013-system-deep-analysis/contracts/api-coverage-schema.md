# API Coverage Report Schema

**Purpose**: Defines the structure for the API coverage analysis output.

---

## Report Structure

```markdown
# API Coverage Report

**Generated**: [DATE]
**Total Endpoints**: [COUNT]
**Coverage**: [PERCENTAGE]%

## Summary

| Metric | Count |
|--------|-------|
| Total Backend Endpoints | X |
| Endpoints with Frontend Consumers | Y |
| Unused Endpoints | Z |
| Frontend Calls without Backend | W |

## By Service

### [service-name]-service (Port [PORT])

**Swagger**: [AVAILABLE/MISSING] | **Endpoints**: [COUNT]

| Method | Path | Status | Consumers |
|--------|------|--------|-----------|
| GET | /api/v1/... | USED | Component1, Component2 |
| POST | /api/v1/... | UNUSED | - |

[Repeat for each service]

## Unused Endpoints

| Service | Method | Path | Notes |
|---------|--------|------|-------|
| auth-service | GET | /api/v1/users | No frontend consumer |

## Missing Backend APIs

| Frontend Component | Expected API | Notes |
|--------------------|--------------|-------|
| PaymentForm.tsx | POST /api/v1/payments | Payment integration missing |
```

---

## Field Definitions

### Endpoint Entry

```yaml
method: GET | POST | PUT | DELETE | PATCH
path: string           # Full API path including base
status: USED | UNUSED  # Whether frontend consumes this
consumers: string[]    # List of consuming components
service: string        # Owning microservice
controller: string     # Controller class name
lineNumber: number     # Source file line number
```

### Service Summary

```yaml
name: string           # Service name
port: number           # Default port
swaggerStatus: AVAILABLE | MISSING
swaggerUrl: string | null
endpointCount: number
usedCount: number
unusedCount: number
```

---

## Example Output

```markdown
### auth-service (Port 8081)

**Swagger**: MISSING | **Endpoints**: 5

| Method | Path | Status | Consumers |
|--------|------|--------|-----------|
| POST | /api/v1/auth/login | USED | LoginForm.tsx, api.ts |
| GET | /api/v1/auth/me | USED | UserMenu.tsx |
| POST | /api/v1/auth/validate | USED | ProtectedRoute.tsx |
| POST | /api/v1/auth/register | USED | RegisterForm.tsx |
| POST | /api/v1/auth/logout | UNUSED | - |
```
