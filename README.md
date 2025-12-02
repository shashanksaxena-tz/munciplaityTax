<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://github.com/user-attachments/assets/0aa67016-6eaf-458a-adb2-6e31a0763ed6" />
</div>

# MuniTax - Municipal Tax Filing System

A comprehensive tax filing and auditing system for Dublin Municipality, supporting both individual and business tax returns with a complete auditor workflow.

View your app in AI Studio: https://ai.studio/apps/drive/1Dqi4Vp_81RqaSKw_9bwsdp5z0tdIwtxM

## Features

### For Taxpayers
- **Individual Tax Filing**: Upload W-2s, 1099s, and other tax forms
- **Business Tax Filing**: File withholding (W-1), net profits (Form 27), and reconciliation (W-3) returns
- **Automated Calculations**: AI-powered form extraction and tax calculation
- **Discrepancy Detection**: Automatic identification of errors and inconsistencies
- **Amendment Support**: File amended returns with tracked changes
- **Payment Integration**: Secure online payment processing

### For Auditors (NEW in Spec 9)
- **Submission Queue**: Prioritized queue with filtering by status, priority, risk score, and more
- **Return Review**: Comprehensive review interface with audit reports and taxpayer history
- **Approval Workflow**: E-signature based approvals with immutable audit trail
- **Rejection Workflow**: Detailed rejection with categorized reasons and resubmission deadlines
- **Document Requests**: Request and track additional documentation from taxpayers
- **Automated Audits**: Risk scoring, anomaly detection, and pattern analysis
- **Audit Trail**: Complete immutable history of all actions for compliance
- **Role-Based Access**: Support for AUDITOR, SENIOR_AUDITOR, SUPERVISOR, MANAGER roles

### Gemini AI Extraction Service (NEW)

The extraction service leverages **Google Gemini 2.5 Flash** for intelligent document analysis:

- **Universal Form Support**: Extracts data from Federal 1040, W-2, 1099-NEC/MISC, W-2G, Schedules C/E/F, and business forms (1120, 1065, Form 27)
- **User-Provided API Keys**: Users can provide their own Gemini API key directly from the UI (never stored, session-only)
- **Real-Time Feedback**: Live extraction progress with detected forms, taxpayer name preview, and confidence scores
- **Confidence Scoring**: Field-level confidence with weighted scoring (Critical > High > Medium > Low)
- **Document Provenance**: Track which page and location each field was extracted from
- **Extraction Summary**: Detailed report of extracted forms, skipped pages, and processing duration

See [docs/EXTRACTION_SERVICE_API.md](./docs/EXTRACTION_SERVICE_API.md) for complete API documentation.

## Architecture

### Frontend
- **Framework**: React 19 with TypeScript
- **Routing**: React Router v7
- **UI**: Tailwind CSS with Lucide icons
- **State Management**: React hooks and context

### Backend (Microservices)
- **Framework**: Spring Boot 3.2.3 with Java 21
- **Architecture**: Microservices with Eureka service discovery
- **Database**: PostgreSQL with JPA/Hibernate
- **Security**: JWT-based authentication with role-based access control

#### Services
1. **Gateway Service**: API gateway and routing (port 8080)
2. **Discovery Service**: Eureka service registry (port 8761)
3. **Auth Service**: User authentication and authorization (port 8081)
4. **Submission Service**: Tax return submissions and auditor workflow (port 8082)
5. **Tax Engine Service**: Tax calculations and rules engine (port 8083)
6. **Extraction Service**: AI-powered document extraction with Gemini 2.5 Flash (port 8084)
7. **PDF Service**: PDF generation and manipulation (port 8085)
8. **Tenant Service**: Multi-tenant management (port 8086)

## Run Locally

### Prerequisites
- Node.js 18+
- Java 21
- Maven 3.8+
- PostgreSQL 15+
- Gemini API Key (optional - users can provide their own via UI)

### Frontend

1. Install dependencies:
   ```bash
   npm install
   ```

2. (Optional) Set the `GEMINI_API_KEY` in `.env.local` for server-default API key. Users can also provide their own key directly in the UI.

3. Run the development server:
   ```bash
   npm run dev
   ```

4. Build for production:
   ```bash
   npm run build
   ```

### Backend

1. Start PostgreSQL database

2. Configure database connection in each service's `application.yml`

3. Build all services:
   ```bash
   cd backend
   mvn clean install
   ```

4. Run services (start discovery service first):
   ```bash
   # Terminal 1: Discovery Service
   cd discovery-service
   mvn spring-boot:run
   
   # Terminal 2: Gateway Service
   cd gateway-service
   mvn spring-boot:run
   
   # Terminal 3: Auth Service
   cd auth-service
   mvn spring-boot:run
   
   # Continue for other services...
   ```

## User Roles

### Taxpayer Roles
- **ROLE_INDIVIDUAL**: Can file personal tax returns
- **ROLE_BUSINESS**: Can file business tax returns

### Auditor Roles
- **ROLE_AUDITOR**: Can review returns, request documents, recommend approval/rejection
- **ROLE_SENIOR_AUDITOR**: Can approve/reject returns under $50K, all auditor permissions
- **ROLE_SUPERVISOR**: Can approve/reject any return, override priority, reassign returns
- **ROLE_MANAGER**: All permissions, generate compliance reports, configure audit rules
- **ROLE_ADMIN**: System configuration and user management

## API Documentation

See [API_SAMPLES.md](./API_SAMPLES.md) for complete API documentation including:
- Withholding (W-1) filing endpoints
- Auditor workflow endpoints
- Authentication endpoints
- Request/response examples
- Error handling

## Auditor Workflow

### Queue Management
1. View prioritized submission queue at `/auditor`
2. Filter by status (PENDING, IN_REVIEW, APPROVED, REJECTED)
3. Filter by priority (HIGH, MEDIUM, LOW)
4. Sort by submission date, tax due, risk score, or days in queue

### Return Review Process
1. **Assign**: Claim return from queue or have supervisor assign
2. **Review**: View return details, supporting documents, and audit report
3. **Actions**:
   - **Approve**: Enter e-signature to approve return
   - **Reject**: Provide detailed reason and resubmission deadline
   - **Request Docs**: Request additional documentation with deadline

### Audit Trail
Every action is logged in an immutable audit trail including:
- Return submission and assignment
- Review start and completion
- Approvals and rejections
- Document requests and responses
- Priority changes and status updates
- Digital signatures for approvals

### Risk Scoring
Automated audit checks calculate a risk score (0-100) based on:
- Year-over-year variance analysis
- Ratio analysis vs industry benchmarks
- Peer comparison with similar businesses
- Pattern analysis (unusual timing, round numbers)
- Discrepancy detection (W-2 box mismatches, NOL errors)

**Risk Levels:**
- LOW (0-20): Routine return, standard review
- MEDIUM (21-60): Minor discrepancies, detailed review
- HIGH (61-100): Major issues, field audit recommended

## Development

### Project Structure
```
munciplaityTax/
├── backend/                 # Spring Boot microservices
│   ├── auth-service/
│   ├── submission-service/  # Includes auditor workflow
│   ├── tax-engine-service/
│   └── ...
├── components/              # React components
│   ├── AuditorDashboard.tsx
│   ├── ReturnReviewPanel.tsx
│   └── ...
├── types.ts                 # TypeScript type definitions
├── App.tsx                  # Main application
└── ...
```

### Adding New Features
1. Define data models in backend entity classes
2. Create repository interfaces with query methods
3. Implement service layer with business logic
4. Create REST controllers with endpoints
5. Add TypeScript types in `types.ts`
6. Create React components
7. Update routing in `App.tsx`
8. Document APIs in `API_SAMPLES.md`

## Testing

### Frontend
```bash
npm run build  # Builds and validates TypeScript
```

### Backend
```bash
cd backend
mvn test       # Run all tests
mvn verify     # Run integration tests
```

## Deployment

See [DOCKER_DEPLOYMENT_GUIDE.md](./DOCKER_DEPLOYMENT_GUIDE.md) for Docker deployment instructions.

## Compliance & Security

- **Audit Trail Retention**: 7+ years (IRS requirement)
- **E-Signatures**: Digital signature hashing for non-repudiation
- **Immutable Logs**: Append-only audit trail, cannot be edited or deleted
- **Role-Based Access**: Enforced at API and UI levels
- **JWT Authentication**: Secure token-based authentication
- **Data Encryption**: TLS/SSL for data in transit

## Contributing

1. Create a feature branch
2. Make your changes
3. Build and test locally
4. Submit a pull request
5. Request code review

## License

Proprietary - Dublin Municipality

## Support

For issues or questions, contact the development team or file an issue in the repository.
