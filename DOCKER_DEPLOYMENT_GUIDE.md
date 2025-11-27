# MuniTax - Docker Deployment & Testing Guide

**Last Updated:** November 26, 2025, 8:42 PM
**Status:** Deployment in Progress

---

## 🐳 Docker Deployment

### Services Deployed:

#### **Infrastructure (3 services)**
1. **PostgreSQL** - Database (port 5432)
2. **Redis** - Caching (port 6379)
3. **Zipkin** - Distributed tracing (port 9411)

#### **Backend Microservices (8 services)**
1. **discovery-service** - Eureka Service Registry (port 8761)
2. **gateway-service** - API Gateway (port 8080)
3. **auth-service** - Authentication & User Management
4. **tenant-service** - Tenant & Session Management
5. **tax-engine-service** - Tax Calculations
6. **extraction-service** - AI Document Extraction (with Gemini API)
7. **submission-service** - Tax Return Submissions
8. **pdf-service** - PDF Generation

#### **Frontend (1 service)**
1. **frontend** - React Application (port 3000)

---

## 🚀 Deployment Commands

### Quick Start:
```bash
# Make deployment script executable
chmod +x deploy.sh

# Deploy all services
./deploy.sh
```

### Manual Deployment:
```bash
# Build backend
cd backend
mvn clean package -DskipTests
cd ..

# Start all services
docker-compose up -d --build

# Check status
docker-compose ps

# View logs
docker-compose logs -f
```

### Stop Services:
```bash
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

---

## 🔧 Environment Variables

### Required in `.env`:
```
GEMINI_API_KEY=your-api-key-here
```

### Docker Compose Variables:
- `SPRING_DATASOURCE_URL` - PostgreSQL connection
- `SPRING_DATASOURCE_USERNAME` - Database user
- `SPRING_DATASOURCE_PASSWORD` - Database password
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` - Service discovery URL
- `JWT_SECRET` - JWT signing key
- `GEMINI_API_KEY` - Gemini AI API key

---

## 🧪 Testing the Complete Flow

### 1. **Verify Services are Running**

```bash
# Check all containers
docker-compose ps

# Should show all services as "Up"
```

**Expected Services:**
- ✅ munitax-postgres (healthy)
- ✅ munitax-redis
- ✅ munitax-zipkin
- ✅ discovery-service
- ✅ gateway-service
- ✅ auth-service
- ✅ tenant-service
- ✅ tax-engine-service
- ✅ extraction-service
- ✅ submission-service
- ✅ pdf-service
- ✅ munitax-frontend

### 2. **Access Service Dashboards**

Open in browser:
- **Frontend:** http://localhost:3000
- **Eureka Dashboard:** http://localhost:8761
- **Zipkin Tracing:** http://localhost:9411
- **API Gateway:** http://localhost:8080

### 3. **Test User Registration Flow**

#### Step 1: Register New User
```bash
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test@1234",
    "firstName": "John",
    "lastName": "Doe",
    "phoneNumber": "5551234567",
    "userRole": "ROLE_INDIVIDUAL",
    "profileType": "INDIVIDUAL",
    "ssnOrEin": "123-45-6789",
    "address": {
      "street": "123 Main St",
      "city": "Dublin",
      "state": "OH",
      "zip": "43016",
      "country": "USA"
    },
    "tenantId": "dublin"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Registration successful. Please check your email to verify your account.",
  "userId": "uuid-here"
}
```

#### Step 2: Login (Skip email verification for testing)
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test@1234"
  }'
```

**Expected Response:**
```json
{
  "token": "jwt-token-here",
  "userId": "uuid-here",
  "email": "test@example.com",
  "roles": "ROLE_INDIVIDUAL",
  "message": "Login successful"
}
```

### 4. **Test Profile Management**

#### Get User Profiles
```bash
curl -X GET http://localhost:8080/api/v1/users/profiles \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

#### Create New Profile
```bash
curl -X POST http://localhost:8080/api/v1/users/profiles \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "type": "INDIVIDUAL",
    "name": "Jane Doe",
    "ssnOrEin": "987-65-4321",
    "relationshipToUser": "Spouse",
    "address": {
      "street": "123 Main St",
      "city": "Dublin",
      "state": "OH",
      "zip": "43016",
      "country": "USA"
    }
  }'
```

### 5. **Test Tax Calculation**

```bash
curl -X POST http://localhost:8080/api/tax/calculate/individual \
  -H "Content-Type: application/json" \
  -d '{
    "taxYear": 2024,
    "filingStatus": "Single",
    "federalAGI": 75000,
    "w2Income": 75000,
    "locality": "dublin"
  }'
```

**Expected Response:**
```json
{
  "totalTaxDue": 1687.50,
  "effectiveRate": 2.25,
  "breakdown": [...]
}
```

### 6. **Test AI Document Extraction**

```bash
curl -X POST http://localhost:8080/api/extraction/extract \
  -H "Content-Type: multipart/form-data" \
  -F "file=@/path/to/w2.pdf" \
  -F "taxYear=2024"
```

### 7. **Test PDF Generation**

```bash
curl -X POST http://localhost:8080/api/pdf/generate \
  -H "Content-Type: application/json" \
  -d '{
    "taxYear": 2024,
    "taxpayerName": "John Doe",
    "ssn": "123-45-6789",
    "totalTaxDue": 1687.50
  }' \
  --output tax-return.pdf
```

---

## 🎨 Frontend Testing

### 1. **Open Frontend**
Navigate to: http://localhost:3000

### 2. **Test Registration Flow**
1. Click "Register"
2. Fill in 3-step registration form:
   - Step 1: Email, password, account type
   - Step 2: Personal information
   - Step 3: Profile & address
3. Submit and verify success message

### 3. **Test Login Flow**
1. Click "Sign In"
2. Enter email and password
3. Verify redirect to dashboard

### 4. **Test Profile Management**
1. Navigate to "Manage Profiles"
2. View existing profiles
3. Click "Add New Profile"
4. Fill in profile form
5. Submit and verify profile appears in list
6. Test "Set as Active" functionality
7. Test edit profile
8. Test delete profile (non-primary)

### 5. **Test Profile Switching**
1. Click profile switcher in header
2. Select different profile
3. Verify active profile changes

### 6. **Test Navigation**
1. Verify role-based navigation appears
2. Test all navigation links
3. Verify protected routes redirect to login

---

## 📊 Monitoring & Debugging

### View Service Logs:
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f auth-service
docker-compose logs -f extraction-service
docker-compose logs -f frontend
```

### Check Service Health:
```bash
# Eureka Dashboard
curl http://localhost:8761/eureka/apps

# Gateway Health
curl http://localhost:8080/actuator/health

# Auth Service (via Gateway)
curl http://localhost:8080/actuator/health
```

### Database Access:
```bash
# Connect to PostgreSQL
docker exec -it munitax-postgres psql -U postgres -d munitax_db

# List tables
\dt

# View users
SELECT * FROM users;

# View profiles
SELECT * FROM user_profiles;
```

### Zipkin Tracing:
1. Open http://localhost:9411
2. Click "Run Query" to see recent traces
3. Click on a trace to see detailed timing
4. Verify all microservices are reporting

---

## ✅ Success Criteria

### Backend:
- ✅ All 8 microservices running
- ✅ PostgreSQL healthy
- ✅ All services registered in Eureka
- ✅ API Gateway routing correctly
- ✅ User registration working
- ✅ Login returning JWT token
- ✅ Profile CRUD operations working
- ✅ Tax calculations working
- ✅ AI extraction working (with Gemini API)
- ✅ PDF generation working

### Frontend:
- ✅ React app loading
- ✅ Registration form working (3 steps)
- ✅ Login form working
- ✅ Profile dashboard displaying
- ✅ Profile switching working
- ✅ Role-based navigation showing
- ✅ Protected routes working
- ✅ Modals opening/closing
- ✅ Forms validating
- ✅ API calls succeeding

### Integration:
- ✅ Frontend → Gateway → Microservices
- ✅ JWT authentication end-to-end
- ✅ Database persistence
- ✅ Distributed tracing in Zipkin
- ✅ Service discovery working

---

## 🐛 Troubleshooting

### Services Not Starting:
```bash
# Check logs
docker-compose logs

# Rebuild specific service
docker-compose up -d --build auth-service

# Restart all services
docker-compose restart
```

### Database Connection Issues:
```bash
# Check PostgreSQL logs
docker-compose logs postgres

# Verify database exists
docker exec -it munitax-postgres psql -U postgres -l
```

### Frontend Not Loading:
```bash
# Check frontend logs
docker-compose logs frontend

# Rebuild frontend
docker-compose up -d --build frontend
```

### API Calls Failing:
1. Check Eureka dashboard - all services registered?
2. Check Gateway logs - routing correctly?
3. Check specific service logs
4. Verify JWT token is valid
5. Check CORS configuration

---

## 📝 Next Steps After Testing

1. ✅ Verify all services are healthy
2. ✅ Test complete user registration flow
3. ✅ Test login and JWT authentication
4. ✅ Test profile management (CRUD)
5. ✅ Test tax calculations
6. ✅ Test AI document extraction
7. ✅ Test PDF generation
8. ✅ Verify distributed tracing
9. ✅ Test frontend UI components
10. ✅ Document any issues found

---

**Deployment Status:** 🚧 In Progress
**Expected Completion:** ~5 minutes
**Next Phase:** Integration Testing & Bug Fixes
