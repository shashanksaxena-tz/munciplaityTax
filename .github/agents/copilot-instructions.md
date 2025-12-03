# munciplaityTax Development Guidelines

Auto-generated from all feature plans. Last updated: 2025-11-28

## Active Technologies
- TypeScript 5.x (Frontend) + React 18+ with Vite, Tailwind CSS (5-schedule-y-sourcing)
- Java 21 LTS (Backend) + Spring Boot 3.2.3, Spring Cloud (5-schedule-y-sourcing)
- PostgreSQL 16+ with multi-tenant schemas (5-schedule-y-sourcing)
- Jest + React Testing Library (Frontend), JUnit 5 + Mockito (Backend) (5-schedule-y-sourcing)
- Java 21 (backend), TypeScript/React 18+ (frontend) + Spring Boot 3.2.3, React 18, Vite, Tailwind CSS (copilot/add-mock-payment-gateway)
- PostgreSQL 16+ (multi-tenant schemas), Redis 7+ (caching) (copilot/add-mock-payment-gateway)

## Project Structure

```text
backend/
├── src/main/java/com/munitax/
│   ├── taxengine/
│   │   ├── apportionment/      # Schedule Y multi-state sourcing
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── model/
│   │   │   ├── dto/
│   │   │   └── enums/
│   └── test/

frontend/ (or components/ at repo root)
├── src/
│   ├── components/
│   │   ├── apportionment/      # Schedule Y UI components
│   ├── services/
│   ├── types/
│   └── utils/
└── tests/
```

## Commands

### Backend
```bash
cd backend
mvn clean install          # Build backend
mvn test                   # Run tests
mvn spring-boot:run        # Run locally
mvn flyway:migrate         # Database migrations
```

### Frontend
```bash
npm install               # Install dependencies
npm run dev               # Dev server
npm test                  # Run tests
npm run build             # Production build
```

## Code Style

**Java**: 
- Use BigDecimal for all monetary and percentage calculations (avoid double/float)
- Scale=10, RoundingMode.HALF_UP for tax calculations
- Follow Spring Boot best practices
- Use Lombok annotations (@Data, @Builder)

**TypeScript/React**: 
- Functional components with hooks
- TypeScript strict mode enabled
- Tailwind CSS for styling
- React Testing Library for tests

**Database**:
- Multi-tenant schema isolation (schema-per-tenant)
- NUMERIC(20,2) for dollars, NUMERIC(14,10) for percentages
- JSONB for flexible fields (allocations, audit log values)
- Immutable audit logs (no updates/deletes)

## Recent Changes
- copilot/add-mock-payment-gateway: Added Java 21 (backend), TypeScript/React 18+ (frontend) + Spring Boot 3.2.3, React 18, Vite, Tailwind CSS
- 5-schedule-y-sourcing: Added multi-state apportionment (Schedule Y) with Joyce/Finnigan elections, throwback/throwout rules, market-based service sourcing

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
