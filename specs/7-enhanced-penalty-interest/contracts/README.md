# API Contracts - Enhanced Penalty & Interest Calculation

This directory contains OpenAPI 3.0 specifications for the Enhanced Penalty & Interest Calculation APIs.

## Contract Files

- `penalty-api.yaml` - Penalty calculation endpoints
- `interest-api.yaml` - Interest calculation endpoints
- `abatement-api.yaml` - Penalty abatement endpoints
- `payment-allocation-api.yaml` - Payment allocation tracking endpoints

## Base URL

All APIs are served through the API Gateway:

- **Development**: `http://localhost:8080/api/v1`
- **Production**: `https://api.munitax.gov/v1`

## Authentication

All endpoints require JWT Bearer token authentication:

```bash
Authorization: Bearer <jwt_token>
```

Tokens obtained from `/api/v1/auth/login` endpoint.

## Tenant Context

All requests are automatically scoped to the tenant specified in the JWT token's `tenantId` claim.

## Documentation

Full API documentation available at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Contract Testing

Contract tests are located in:
- Backend: `/backend/tax-engine-service/src/test/java/com/munitax/contract/`
- Uses Spring Cloud Contract framework

## Status

🚧 **In Development** - Contract specifications will be generated in Phase 1 of implementation.
