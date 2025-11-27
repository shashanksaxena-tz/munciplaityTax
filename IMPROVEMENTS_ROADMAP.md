# MuniTax - Improvements & Feature Roadmap

**Last Updated:** November 26, 2025, 8:01 PM
**Status:** Phase 3 Complete - Planning Phase 4+

---

## 🎯 PRIORITY CLASSIFICATION

### **Priority 0: CRITICAL FOUNDATION** (Must have before production)
Features essential for basic multi-user, multi-tenant operation

### **Priority 1: CORE WORKFLOW** (Next 1-2 months)
Features required for complete user experience and auditor approval

### **Priority 2: ADVANCED FEATURES** (Next 3-6 months)
Features that enhance usability and provide competitive advantage

### **Priority 3: FUTURE ENHANCEMENTS** (6+ months)
Nice-to-have features for long-term growth

---

## ✅ COMPLETED (Phase 1-3)

- ✅ Individual Tax Calculator (Java)
- ✅ Business Tax Calculator (Java)
- ✅ Gemini AI Extraction (Real API via Backend)
- ✅ Session Management (PostgreSQL)
- ✅ PDF Generation (Apache PDFBox via Backend)
- ✅ Address Validation (Frontend Util)
- ✅ Basic microservices architecture
- ✅ Frontend Service Refactoring (Removed TS Logic)
- ✅ Basic Tax Compliance (Refund vs Credit Choice)

---

### 1. User Management & Authentication System ✅ IN PROGRESS
**Timeline:** 2 weeks
**Status:** 🚧 **IMPLEMENTING NOW** (Day 1)
**Progress:** 80% Complete ⬆️

#### 1.1. User Registration & Profiles ✅ IMPLEMENTED
- [x] **User Entity** (170 lines)
- [x] **UserProfile Entity** (150 lines)
- [x] **Repositories** (33 lines)
- [x] **UserManagementService** (240 lines)
- [x] **UserController** (210 lines) ✅ NEW
- [x] **AuthController** (200 lines) ✅ NEW

#### 1.2. Registration Flow ✅ IMPLEMENTED
- [x] Backend models and services
- [x] REST API endpoints ✅ NEW
  - `POST /api/v1/users/register`
  - `GET /api/v1/users/verify-email`
  - `POST /api/v1/users/forgot-password`
  - `POST /api/v1/users/reset-password`
- [ ] Email service integration (NEXT)
- [ ] Frontend registration form
- [ ] Email templates

#### 1.3. Authentication & Login ✅ IMPLEMENTED
- [x] **Login Endpoint** ✅ NEW
  - `POST /auth/login`
  - Email/password authentication
  - JWT token generation
  - Email verification check
  - Account active check
- [x] **Token Management** ✅ NEW
  - `POST /auth/token`
  - `POST /auth/validate`
  - `GET /auth/me`
- [x] **CustomUserDetailsService** (70 lines) ✅ NEW
  - Spring Security integration
  - Role-based authorities
  - Last login tracking

#### 1.4. Role-Based Access Control (RBAC) ✅ IMPLEMENTED
- [x] **Roles Defined**:
  - `ROLE_INDIVIDUAL`: File personal returns
  - `ROLE_BUSINESS`: File business returns
  - `ROLE_AUDITOR`: Review and approve/reject
  - `ROLE_ADMIN`: System configuration
- [x] **Spring Security Configuration** (70 lines) ✅ NEW
  - Public endpoints (register, login, verify, reset)
  - Protected endpoints (profiles, user data)
  - CORS configuration
  - Stateless session management
- [x] **JWT Authentication**
  - HS256 signing
  - 24-hour expiration
  - Role claims in token
- [ ] **Method-level security** (NEXT)
- [ ] **Permission annotations**

#### 1.5. Multi-Profile Support ✅ IMPLEMENTED
- [x] **Profile CRUD Endpoints** ✅ NEW
  - `GET /api/v1/users/profiles`
  - `GET /api/v1/users/profiles/primary`
  - `POST /api/v1/users/profiles`
  - `PUT /api/v1/users/profiles/{id}`
  - `DELETE /api/v1/users/profiles/{id}`
- [x] **Household Filing**:
  - Primary user + additional profiles
  - Relationship tracking
  - Profile switching capability
- [x] **Business Entity Management**:
  - Multiple business profiles per user
  - EIN-based separation
  - Fiscal year tracking

**Files Created (12):** ⬆️
1. ✅ `User.java` (170 lines) - User entity with roles
2. ✅ `UserProfile.java` (150 lines) - Profile entity
3. ✅ `UserRepository.java` (15 lines) - User data access
4. ✅ `UserProfileRepository.java` (18 lines) - Profile data access
5. ✅ `UserManagementService.java` (240 lines) - Business logic
6. ✅ `UserController.java` (210 lines) - User management API ✅ NEW
7. ✅ `AuthController.java` (200 lines) - Authentication API ✅ NEW
8. ✅ `CustomUserDetailsService.java` (70 lines) - Spring Security ✅ NEW
9. ✅ `SecurityConfig.java` (70 lines) - Security configuration ✅ NEW
10. ✅ `application.yml` (30 lines) - Service configuration ✅ NEW
11. ⏳ **NEXT**: Email service
12. ⏳ **NEXT**: Frontend integration

**API Endpoints (14):** ✅ NEW
```
Authentication:
POST   /auth/login              - User login
POST   /auth/token              - Generate JWT token
POST   /auth/validate           - Validate token
GET    /auth/me                 - Get current user info

User Management:
POST   /api/v1/users/register   - Register new user
GET    /api/v1/users/verify-email - Verify email
POST   /api/v1/users/forgot-password - Request password reset
POST   /api/v1/users/reset-password - Reset password

Profile Management:
GET    /api/v1/users/profiles   - Get all profiles
GET    /api/v1/users/profiles/primary - Get primary profile
POST   /api/v1/users/profiles   - Create profile
PUT    /api/v1/users/profiles/{id} - Update profile
DELETE /api/v1/users/profiles/{id} - Delete profile
```

**Database Schema:**
```sql
✅ users (id, email, password_hash, first_name, last_name, phone_number, 
         email_verified, verification_token, reset_token, roles, 
         active, tenant_id, created_at, updated_at, last_login_at)
✅ user_roles (user_id, role)
✅ user_profiles (id, user_id, type, name, ssn_or_ein, business_name,
                 fiscal_year_end, address_*, relationship, is_primary,
                 active, created_at, updated_at)
```

**Build Status:** ✅ SUCCESS (12 files compiled)

**Remaining (20%):**
- [ ] Email service integration
- [ ] Frontend registration/login forms
- [ ] Method-level security annotations
- [ ] Integration testing

#### 1.6. Frontend UI Implementation ✅ COMPLETE
**Priority:** HIGH (Part of Phase 4)
**Timeline:** 3-4 days
**Progress:** 100% Complete ✅✅✅

##### Authentication Components ✅ IMPLEMENTED (6 files)
- [x] **AuthContext.tsx** (140 lines) - User state management
- [x] **LoginForm.tsx** (200 lines) - Login page
- [x] **RegistrationForm.tsx** (500 lines) - Multi-step registration
- [x] **EmailVerification.tsx** (100 lines) - Email verification
- [x] **ForgotPassword.tsx** (120 lines) - Password reset request
- [x] **ResetPassword.tsx** (180 lines) - Password reset form

##### Profile Management ✅ IMPLEMENTED (4 files)
- [x] **ProfileContext.tsx** (170 lines) - Profile state management
- [x] **ProtectedRoute.tsx** (50 lines) - Route guards
- [x] **ProfileDashboard.tsx** (120 lines) - Profile hub
- [x] **ProfileCard.tsx** (250 lines) - Profile cards

##### Modals & Dialogs ✅ IMPLEMENTED (2 files)
- [x] **CreateProfileModal.tsx** (280 lines) ✅ NEW
  - Profile type selection (Individual/Business)
  - Form validation
  - Beautiful modal design
  - Loading states
- [x] **EditProfileModal.tsx** (230 lines) ✅ NEW
  - Pre-filled form data
  - Update functionality
  - Color-coded by profile type

##### Navigation & Layout ✅ IMPLEMENTED (3 files)
- [x] **ProfileSwitcher.tsx** (100 lines) ✅ NEW
  - Dropdown with all profiles
  - Active indicator
  - Quick switch functionality
  - Click-outside to close
- [x] **UserMenu.tsx** (130 lines) ✅ NEW
  - User info display
  - Role badges
  - Navigation links
  - Logout button
- [x] **Header.tsx** (100 lines) ✅ NEW
  - Logo and branding
  - Role-based navigation
  - Profile switcher integration
  - User menu integration
  - Notifications icon
  - Responsive design

**Files Created (15/15):** ✅ COMPLETE
1. ✅ AuthContext.tsx
2. ✅ LoginForm.tsx
3. ✅ RegistrationForm.tsx
4. ✅ EmailVerification.tsx
5. ✅ ForgotPassword.tsx
6. ✅ ResetPassword.tsx
7. ✅ ProfileContext.tsx
8. ✅ ProtectedRoute.tsx
9. ✅ ProfileDashboard.tsx
10. ✅ ProfileCard.tsx
11. ✅ CreateProfileModal.tsx ✅ NEW
12. ✅ EditProfileModal.tsx ✅ NEW
13. ✅ ProfileSwitcher.tsx ✅ NEW
14. ✅ UserMenu.tsx ✅ NEW
15. ✅ Header.tsx ✅ NEW

**Total Lines of Code:** ~2,650 lines ⬆️⬆️

**UI Features Implemented:**
✅ Complete authentication flow
✅ Multi-step registration wizard
✅ Email verification
✅ Password reset
✅ Profile CRUD operations
✅ Profile switching
✅ Role-based navigation
✅ Protected routes
✅ Beautiful, modern design
✅ Responsive layouts
✅ Loading states
✅ Error handling
✅ Modal dialogs
✅ Dropdown menus
✅ Password strength indicators
✅ Form validation

---

### 1.7. Phase 4 Summary ✅ COMPLETE
- [ ] **Registration Form Component**:
  - Email input with validation
  - Password input with strength indicator
  - Confirm password
  - First name, last name, phone number
  - User type selection (Individual/Business/Auditor)
  - Profile type selection (Individual/Business)
  - SSN/EIN input (masked)
  - Business name (if business)
  - Address fields
  - Terms & conditions checkbox
  - Submit button with loading state
- [ ] **Validation**:
  - Email format validation
  - Password strength (min 8 chars, uppercase, lowercase, number, special)
  - SSN/EIN format validation
  - Required field validation
  - Real-time error messages
- [ ] **Success Flow**:
  - Success message
  - Redirect to email verification page
  - Resend verification email option

##### Login Page
- [ ] **Login Form Component**:
  - Email input
  - Password input with show/hide toggle
  - Remember me checkbox
  - Login button with loading state
  - "Forgot password?" link
  - "Don't have an account? Register" link
- [ ] **Authentication Flow**:
  - JWT token storage (localStorage/sessionStorage)
  - Automatic redirect after login
  - Role-based redirect (Individual → /filer, Business → /business, Auditor → /auditor)
  - Error handling (invalid credentials, unverified email, inactive account)
- [ ] **Session Management**:
  - Auto-logout on token expiry
  - Token refresh mechanism
  - Persistent login (remember me)

##### Email Verification Page
- [ ] **Verification Component**:
  - Token extraction from URL
  - Automatic verification on load
  - Success/failure message
  - Redirect to login on success
  - Resend verification email button

##### Password Reset Flow
- [ ] **Forgot Password Page**:
  - Email input
  - Submit button
  - Success message
  - Back to login link
- [ ] **Reset Password Page**:
  - Token extraction from URL
  - New password input
  - Confirm password input
  - Password strength indicator
  - Submit button
  - Success redirect to login

##### Profile Management UI
- [ ] **Profile Dashboard**:
  - List of all profiles (cards)
  - Primary profile indicator
  - Add new profile button
  - Edit/delete actions per profile
  - Profile type icons (Individual/Business)
- [ ] **Profile Creation Modal**:
  - Profile type selection
  - Name input
  - SSN/EIN input
  - Relationship dropdown (Self, Spouse, Dependent, Employee)
  - Business fields (if business type)
  - Address fields
  - Save button
- [ ] **Profile Edit Modal**:
  - Pre-filled form
  - Same fields as creation
  - Update button
  - Cancel button
- [ ] **Profile Switching**:
  - Dropdown in header
  - Show current active profile
  - Quick switch between profiles
  - Update context on switch

##### Navigation & Layout
- [ ] **Header Component**:
  - Logo
  - Navigation menu (role-based)
  - Profile switcher dropdown
  - User menu (Profile, Settings, Logout)
  - Notifications icon
- [ ] **Protected Routes**:
  - Route guards based on authentication
  - Role-based route access
  - Redirect to login if not authenticated
  - Redirect to appropriate dashboard based on role
- [ ] **Role-Based Dashboards**:
  - Individual Filer Dashboard
  - Business Filer Dashboard
  - Auditor Dashboard
  - Admin Dashboard

##### UI/UX Enhancements
- [ ] **Loading States**:
  - Skeleton loaders
  - Spinner for async operations
  - Progress indicators
- [ ] **Error Handling**:
  - Toast notifications
  - Inline error messages
  - Error boundary components
- [ ] **Responsive Design**:
  - Mobile-first approach
  - Tablet optimization
  - Desktop layout
- [ ] **Accessibility**:
  - ARIA labels
  - Keyboard navigation
  - Screen reader support
  - Focus management

**UI Components to Create (15):**
1. `RegistrationForm.tsx` - Registration page
2. `LoginForm.tsx` - Login page
3. `EmailVerification.tsx` - Email verification
4. `ForgotPassword.tsx` - Password reset request
5. `ResetPassword.tsx` - Password reset form
6. `ProfileDashboard.tsx` - Profile management
7. `ProfileCard.tsx` - Individual profile card
8. `CreateProfileModal.tsx` - Profile creation
9. `EditProfileModal.tsx` - Profile editing
10. `ProfileSwitcher.tsx` - Profile dropdown
11. `ProtectedRoute.tsx` - Route guard
12. `Header.tsx` - Navigation header
13. `UserMenu.tsx` - User dropdown menu
14. `LoadingSpinner.tsx` - Loading indicator
15. `Toast.tsx` - Notification component

**State Management:**
- [ ] Auth context (user, token, roles)
- [ ] Profile context (active profile, all profiles)
- [ ] API client with auth interceptor
- [ ] Token refresh logic

---

### 2. Multi-Tenancy & Municipality Management

#### 1.1. User Registration & Profiles ✅ IMPLEMENTED
- [x] **User Entity**:
  - Email/password fields
  - Email verification token & expiry
  - Password reset token & expiry
  - Role-based access (INDIVIDUAL, BUSINESS, AUDITOR, ADMIN)
  - Active status
  - Tenant association
- [x] **UserProfile Entity**:
  - Multi-profile support
  - Individual vs Business profiles
  - SSN/EIN storage
  - Address embedding
  - Primary profile flag
  - Relationship tracking (Self, Spouse, Dependent)
- [x] **Repositories**:
  - UserRepository with email queries
  - UserProfileRepository with user queries
- [x] **UserManagementService**:
  - User registration with email verification
  - Email verification flow
  - Password reset flow
  - Profile creation/update/delete
  - Primary profile management

#### 1.2. Registration Flow 🚧 PARTIAL
- [x] Backend models and services
- [ ] REST API endpoints (NEXT)
- [ ] Email service integration
- [ ] Frontend registration form
- [ ] Email templates

#### 1.3. Role-Based Access Control (RBAC) 🚧 PARTIAL
- [x] **Roles Defined**:
  - `ROLE_INDIVIDUAL`: File personal returns
  - `ROLE_BUSINESS`: File business returns
  - `ROLE_AUDITOR`: Review and approve/reject
  - `ROLE_ADMIN`: System configuration
- [ ] **Spring Security Configuration** (NEXT)
- [ ] **Method-level security**
- [ ] **Permission checking**

#### 1.4. Multi-Profile Support ✅ IMPLEMENTED
- [x] **Household Filing**:
  - Primary user + additional profiles
  - Relationship tracking
  - Profile switching capability
- [x] **Business Entity Management**:
  - Multiple business profiles per user
  - EIN-based separation
  - Fiscal year tracking
- [x] **Profile History**:
  - Active/inactive status
  - Creation/update timestamps

**Files Created (8):**
1. `User.java` (170 lines) - User entity with roles
2. `UserProfile.java` (150 lines) - Profile entity
3. `UserRepository.java` (15 lines) - User data access
4. `UserProfileRepository.java` (18 lines) - Profile data access
5. `UserManagementService.java` (240 lines) - Business logic
6. ✅ **NEXT**: REST controllers
7. ✅ **NEXT**: Security configuration
8. ✅ **NEXT**: Email service

**Database Schema:**
```sql
✅ users (id, email, password_hash, first_name, last_name, phone_number, 
         email_verified, verification_token, reset_token, roles, 
         active, tenant_id, created_at, updated_at, last_login_at)
✅ user_roles (user_id, role)
✅ user_profiles (id, user_id, type, name, ssn_or_ein, business_name,
                 fiscal_year_end, address_*, relationship, is_primary,
                 active, created_at, updated_at)
```

---

### 2. Multi-Tenancy & Municipality Management
**Timeline:** 2 weeks
**Status:** CRITICAL - MUST IMPLEMENT FIRST

#### 1.1. User Registration & Profiles
- [ ] **Registration Flow**:
  - Email/password signup
  - Email verification
  - SSN/EIN validation
  - Profile type selection (Individual/Business/Auditor)
- [ ] **User Types**:
  - **Individual Filer**: Personal tax returns
  - **Business Filer**: Business returns (can manage multiple businesses)
  - **Auditor**: County-level review access
- [ ] **Profile Management**:
  - User can create multiple profiles (self + family members)
  - Business user can manage multiple EINs
  - Profile switching within same account
  - Historical data per profile

#### 1.2. Role-Based Access Control (RBAC)
- [ ] **Roles**:
  - `ROLE_INDIVIDUAL`: File personal returns
  - `ROLE_BUSINESS`: File business returns
  - `ROLE_AUDITOR`: Review and approve/reject
  - `ROLE_ADMIN`: System configuration
- [ ] **Permissions**:
  - View own returns
  - Edit draft returns
  - Submit returns
  - View all returns (auditor)
  - Approve/reject returns (auditor)
  - Configure rules (admin)

#### 1.3. Multi-Profile Support
- [ ] **Household Filing**:
  - Primary user + spouse
  - Dependents
  - Multiple family members
- [ ] **Business Entity Management**:
  - Single login for multiple businesses
  - EIN-based separation
  - Consolidated dashboard
- [ ] **Profile History**:
  - View past returns per profile
  - Year-over-year comparison
  - Amendment history

**Database Schema:**
```sql
users (id, email, password_hash, role, created_at)
profiles (id, user_id, type, name, ssn_ein, address, created_at)
user_businesses (id, user_id, business_name, ein, fiscal_year)
```

---

### 2. Multi-Tenancy & Municipality Management
**Timeline:** 2 weeks
**Status:** CRITICAL

#### 2.1. Tenant Configuration
- [ ] **Municipality Setup**:
  - Tenant registration (Dublin, Columbus, Westerville, etc.)
  - Tenant-specific branding (logo, colors)
  - Contact information
  - Tax year configuration
- [ ] **Data Isolation**:
  - Schema-per-tenant (PostgreSQL)
  - Complete data separation
  - Tenant-aware queries
  - Cross-tenant prevention

#### 2.2. Rule Engine 2.0 (Tenant-Specific)
- [ ] **Dynamic Rule Loading**:
  - Load rules by tenant ID
  - Override default rules
  - Tenant-specific rates
- [ ] **Rule Versioning**:
  - Version tracking (e.g., "2024 Rules v1", "2024 Rules v2")
  - Effective date ranges
  - Retroactive calculation support
  - Audit trail of rule changes
- [ ] **Rule Testing Sandbox**:
  - Test new rules against historical data
  - Preview impact before publishing
  - Rollback capability
  - A/B testing support
- [ ] **Rule Application**:
  - Apply rules to specific tenant
  - Publish/unpublish rules
  - Schedule rule activation
  - Notification on rule changes

**Database Schema:**
```sql
tenants (id, name, municipality, tax_rate, logo_url, created_at)
tax_rules (id, tenant_id, version, effective_date, rules_json, status)
rule_history (id, rule_id, changed_by, change_type, old_value, new_value)
```

---

### 3. Comprehensive Database Layer
**Timeline:** 2 weeks
**Status:** CRITICAL

#### 3.1. Core Schemas
- [ ] **Users & Tenants**:
  - Multi-tenant isolation
  - User-tenant relationships
  - Profile management
- [ ] **Submissions**:
  - Return state (Draft, Submitted, Under Review, Approved, Rejected, Amended)
  - Submission timestamp
  - Submitter information
  - Approval/rejection metadata
- [ ] **Audit Logs** (Immutable):
  - Every user action
  - Every system action
  - Login/logout events
  - Data changes (before/after)
  - Extraction events
  - Approval/rejection events
- [ ] **Extraction Logs**:
  - Raw extraction results
  - Confidence scores per field
  - Ignored files/pages with reasons
  - Source page numbers
  - Extraction timestamp
  - AI model version

#### 3.2. Ledger Management
- [ ] **Financial Ledger** (Double-entry):
  - Tax liability entries
  - Payment entries
  - Withholding entries
  - Refund entries
  - Credit entries
  - Balance tracking
- [ ] **Withholding Tracking**:
  - Form W-1 entries
  - Quarterly/monthly tracking
  - Reconciliation with annual return
- [ ] **Payment Tracking**:
  - Estimated payments
  - Final payments
  - Refunds issued
  - Credits applied
- [ ] **Carryforward Management**:
  - NOL carryforward
  - Overpayment credits
  - Multi-year tracking

**Database Schema:**
```sql
submissions (id, tenant_id, user_id, profile_id, type, status, submitted_at, reviewed_at, reviewer_id)
audit_logs (id, tenant_id, user_id, action, entity_type, entity_id, old_value, new_value, timestamp)
extraction_logs (id, submission_id, file_name, page_number, extracted_data, confidence, ignored_reason, timestamp)
ledger_entries (id, tenant_id, profile_id, entry_type, debit, credit, balance, description, transaction_date)
withholdings (id, tenant_id, business_id, period, gross_wages, tax_withheld, filed_date)
payments (id, tenant_id, submission_id, amount, payment_type, payment_date, confirmation_number)
```

---

## 🟠 PRIORITY 1: CORE WORKFLOW ENHANCEMENTS

### 4. Auditor Workflow & Review System
**Timeline:** 3 weeks
**Status:** HIGH PRIORITY

#### 4.1. Auditor Dashboard
- [ ] **Submission Queue**:
  - List of pending returns
  - Sort by: date, amount, taxpayer, status
  - Filter by: type (individual/business), date range, amount range
  - Bulk actions (approve/reject multiple)
  - Priority flagging
- [ ] **Statistics Panel**:
  - Pending count
  - Approved today/week/month
  - Rejected today/week/month
  - Average review time
  - Revenue collected

#### 4.2. Split-Screen Review Interface
- [ ] **Left Panel: PDF Viewer**:
  - Display full uploaded PDF
  - Page navigation
  - Zoom controls
  - Annotation tools
  - Highlight discrepancies
- [ ] **Right Panel: Extracted Data**:
  - Editable form fields
  - Confidence indicators
  - Calculated values
  - Comparison with reported values
  - Override capability

#### 4.3. Approval/Rejection Workflow
- [ ] **Approve Action**:
  - One-click approval
  - Optional approval notes
  - Automatic status update
  - Email notification to taxpayer
  - Move to "Approved" queue
- [ ] **Reject Action**:
  - **Required rejection reason** (dropdown + text):
    - "W-2 image is blurry"
    - "Missing required form"
    - "Calculation discrepancy"
    - "Invalid SSN/EIN"
    - "Other (specify)"
  - Detailed comments field
  - Specific field highlighting
  - Email notification to taxpayer
  - Return to taxpayer for correction
- [ ] **Request More Information**:
  - Send back with specific questions
  - Taxpayer can respond
  - Conversation thread
  - Re-submit capability

#### 4.4. Auditor Override & Corrections
- [ ] **Manual Field Editing**:
  - Override any extracted value
  - Reason required for override
  - Logged in audit trail
  - Highlight overridden fields
- [ ] **Recalculation**:
  - Trigger recalculation after override
  - Show before/after comparison
  - Validate new values

#### 4.5. User Notification System
- [ ] **Rejection Notifications**:
  - Email with rejection reason
  - In-app notification
  - Specific field issues highlighted
  - Guidance on how to correct
- [ ] **Approval Notifications**:
  - Email confirmation
  - Payment instructions (if balance due)
  - Refund timeline (if overpayment)

**Database Schema:**
##### Responsive Design
- [ ] **Desktop** (1920x1080+):
  - Full split-screen layout
  - All features visible
- [ ] **Tablet** (768x1024):
  - Tabbed interface (PDF/Forms)
  - Collapsible panels
- [ ] **Mobile** (375x667):
  - Stacked layout
  - Simplified table view
  - Swipe actions

**UI Components to Create (20):**
1. `AuditorDashboard.tsx` - Main dashboard
2. `SubmissionQueue.tsx` - Queue table
3. `StatisticsCards.tsx` - Stats widgets
4. `FilterPanel.tsx` - Filters sidebar
5. `SplitScreenReview.tsx` - Review page layout
6. `PdfViewer.tsx` - PDF display component
7. `PdfToolbar.tsx` - PDF controls
8. `FormDataPanel.tsx` - Right panel
9. `FormAccordion.tsx` - Expandable forms
10. `FieldEditor.tsx` - Inline field editing
11. `ConfidenceIndicator.tsx` - Confidence badges
12. `ApproveModal.tsx` - Approval dialog
13. `RejectModal.tsx` - Rejection dialog
14. `RequestInfoModal.tsx` - Info request dialog
15. `OverrideReasonModal.tsx` - Override reason
16. `RecalculationModal.tsx` - Before/after comparison
17. `NotificationBell.tsx` - Notification icon
18. `NotificationList.tsx` - Notification dropdown
19. `TimelineView.tsx` - Action history
20. `BulkActionBar.tsx` - Bulk operations

**State Management:**
- [ ] Submission queue state
- [ ] Active submission state
- [ ] PDF viewer state (page, zoom, annotations)
- [ ] Form data state (with undo/redo)
- [ ] Notification state
- [ ] WebSocket connection for real-time updates

---

### 5. Advanced AI Extraction & Transparency
**Timeline:** 3 weeks
**Status:** HIGH PRIORITY

#### 5.1. Visual Provenance (Click-to-Source)
- [ ] **Field-to-PDF Linking**:
  - Click any form field → PDF opens to source page
  - Highlight exact text/box in PDF
  - Bounding box overlay
  - Confidence score display
- [ ] **Multi-page Support**:
  - Handle forms spanning multiple pages
  - Show all source locations
  - Page thumbnails
- [ ] **Implementation**:
  - Store bounding box coordinates in extraction log
  - PDF.js for rendering
  - Overlay drawing on canvas

#### 5.2. Ignored Items Report
- [ ] **Comprehensive Listing**:
  - All pages/files that were NOT used
  - Explicit reasons for each:
    - "Page 4 ignored: Identified as Instructions"
    - "Page 5 ignored: Not relevant to Tax Year 2024"
    - "File 2 ignored: Not a tax form (bank statement)"
    - "Page 3 ignored: Duplicate of Page 1"
- [ ] **Visual Preview**:
  - Thumbnail of ignored pages
  - Click to view full page
  - Option to manually include
- [ ] **Statistics**:
  - Total pages uploaded
  - Pages used
  - Pages ignored
  - Confidence distribution

#### 5.3. Real-Time Extraction Updates
- [ ] **WebSocket Feed** (Granular Progress):
  - "Upload received: 15 files, 47 pages total"
  - "Scanning file 1 of 15..."
  - "Identified 5 tax forms across 12 pages"
  - "Found: W-2 (Page 1), Schedule C (Page 3-4), 1099-NEC (Page 7)"
  - "Skipping 2 non-tax documents (bank statements)"
  - "Extracting data from W-2..."
  - "Extracting data from Schedule C..."
  - "Extraction complete: 5 forms processed, 35 pages ignored"
- [ ] **Progress Indicators**:
  - Overall progress bar
  - Per-file progress
  - Estimated time remaining
  - Current activity
- [ ] **User Engagement**:
  - Keep user informed without overwhelming
  - Show value being created
  - Build trust in AI process

**Database Schema:**
```sql
extraction_details (id, extraction_log_id, field_name, value, confidence, page_number, bbox_x, bbox_y, bbox_width, bbox_height)
ignored_items (id, extraction_log_id, page_number, file_name, ignore_reason, thumbnail_url)
```

---

### 6. Return History & Audit Trail
**Timeline:** 2 weeks
**Status:** HIGH PRIORITY

#### 6.1. Timeline View
- [ ] **Chronological Events**:
  - "User uploaded W-2 (10:00 AM)"
  - "System extracted data (10:01 AM)"
  - "User changed Box 18 from $50,000 to $52,000 (10:05 AM)"
  - "User submitted return (10:10 AM)"
  - "Auditor Jane Smith reviewed (2:00 PM)"
  - "Auditor approved return (2:15 PM)"
- [ ] **Event Details**:
  - Timestamp
  - User/system actor
  - Action type
  - Before/after values
  - Reason (if applicable)

#### 6.2. Diff View (Amendments)
- [ ] **Side-by-side Comparison**:
  - Original submitted version
  - Amended version
  - Highlight changes
  - Reason for amendment
- [ ] **Field-level Changes**:
  - Show exactly what changed
  - Who made the change
  - When it was changed
  - Why it was changed

#### 6.3. Version Control
- [ ] **Return Versions**:
  - V1: Original submission
  - V2: After auditor corrections
  - V3: Amended by taxpayer
- [ ] **Rollback Capability**:
  - View any previous version
  - Compare versions
  - Restore previous version (admin only)

**Database Schema:**
```sql
return_versions (id, submission_id, version_number, data_snapshot, created_by, created_at, change_reason)
timeline_events (id, submission_id, event_type, actor_id, actor_type, description, metadata, timestamp)
```

---

### 7. Reconciliation Engine
**Timeline:** 2 weeks
**Status:** MEDIUM-HIGH PRIORITY

#### 7.1. Business ↔ Individual Reconciliation
- [ ] **W-2 Withholding Matching**:
  - Match individual's W-2 to employer's W-1 filing
  - Verify withholding amounts match
  - Flag discrepancies > $10
  - Automatic matching by EIN + SSN
- [ ] **Bulk Reconciliation**:
  - Process all W-2s for a business
  - Generate reconciliation report
  - Identify missing W-2s
  - Identify excess W-2s

#### 7.2. Federal ↔ Local Reconciliation
- [ ] **Income Comparison**:
  - Compare Federal AFTI to Local Net Profits
  - Validate Schedule X add-backs/deductions
  - Flag unusual differences
- [ ] **Schedule Validation**:
  - Cross-check Schedule C, E, F
  - Verify consistency
  - Suggest corrections

#### 7.3. Historical Reconciliation
- [ ] **Year-over-year Analysis**:
  - Compare current year to prior year
  - Identify significant changes (>20%)
  - Flag for auditor review
- [ ] **NOL Tracking**:
  - Verify carryforward amounts
  - Track across years
  - Automatic application
- [ ] **Overpayment Tracking**:
  - Track credits across years
  - Ensure proper application
  - Prevent duplicate credits

**Database Schema:**
```sql
reconciliations (id, tenant_id, type, entity1_id, entity2_id, status, discrepancies, created_at)
discrepancy_items (id, reconciliation_id, field_name, expected_value, actual_value, difference, severity)
```

---

## 🟡 PRIORITY 2: ADVANCED FEATURES

### 8. Reporting Engine
**Timeline:** 3 weeks
**Status:** MEDIUM PRIORITY

#### 8.1. Standard Reports (Pre-built)
- [ ] **Daily Receipts**:
  - Revenue by day/week/month
  - Payment method breakdown
  - Refunds issued
- [ ] **Delinquent Filers**:
  - Overdue returns
  - Amount owed
  - Contact information
- [ ] **Top Taxpayers**:
  - Top 100 by liability
  - Top 100 by withholding
  - Top 100 by refund
- [ ] **Filing Statistics**:
  - Total returns filed
  - By type (individual/business)
  - By status (approved/rejected/pending)
  - Average processing time
- [ ] **Revenue Projections**:
  - Based on historical trends
  - Seasonal adjustments
  - Forecast accuracy

#### 8.2. Custom Report Builder
- [ ] **Visual Query Builder**:
  - Drag-and-drop interface
  - Select fields from any table
  - Join multiple tables
  - No SQL knowledge required
- [ ] **Filters & Criteria**:
  - Date range
  - Amount range
  - Status
  - User type
  - Municipality
- [ ] **Grouping & Aggregation**:
  - Group by any field
  - Sum, average, count, min, max
  - Subtotals and totals
- [ ] **Visualization**:
  - Charts (bar, line, pie)
  - Tables
  - Pivot tables

#### 8.3. Export & Scheduling
- [ ] **Export Formats**:
  - PDF (formatted)
  - Excel (with formulas)
  - CSV (raw data)
- [ ] **Scheduled Reports**:
  - Daily/weekly/monthly
  - Email delivery
  - Automatic generation
- [ ] **Report Templates**:
  - Save custom reports
  - Share with team
  - Clone and modify

**Database Schema:**
```sql
reports (id, tenant_id, name, type, query_json, schedule, created_by, created_at)
report_executions (id, report_id, executed_at, result_url, status)
```

---

### 9. Expanded Tax Research & Rule Engine
**Timeline:** 4 weeks (ongoing research)
**Status:** MEDIUM PRIORITY

#### 9.1. Missing Filing Types Research
- [ ] **JEDD Zones** (Joint Economic Development Districts):
  - Special tax zones
  - Different rates
  - Allocation rules
- [ ] **Specific Local Forms**:
  - Form 37 (Estimated payments)
  - Form W-3 (Annual reconciliation)
  - Form 1099-R (Retirement distributions)
- [ ] **Special Cases**:
  - Military personnel
  - Clergy
  - Non-residents
  - Part-year residents

#### 9.2. Business Filing Types
- [ ] **Withholding Returns**:
  - Form W-1 (quarterly/monthly)
  - Form W-2 (annual)
  - Form W-3 (reconciliation)
- [ ] **Net Profits**:
  - Form 27 (annual)
  - Estimated payments
  - NOL carryforward
- [ ] **Special Business Types**:
  - Partnerships (Form 1065)
  - S-Corps (Form 1120S)
  - C-Corps (Form 1120)
  - LLCs (various)

#### 9.3. Rule Engine Enhancements
- [ ] **Income Inclusion Rules**:
  - Schedule C: Yes/No toggle
  - Schedule E: Yes/No toggle
  - Schedule F: Yes/No toggle
  - W-2G: Yes/No toggle
  - Form 1099: Yes/No toggle
- [ ] **Credit Rules**:
  - Credit limit rate
  - Qualifying cities
  - Reciprocity agreements
- [ ] **Allocation Rules**:
  - 3-factor formula
  - Sales factor weighting
  - Throwback rules

---

## 🟢 PRIORITY 3: FUTURE ENHANCEMENTS

### 10. Mobile & Offline Support
**Timeline:** 4 weeks
**Status:** LOW PRIORITY

- [ ] React Native mobile app
- [ ] Camera scan for tax forms
- [ ] Offline draft capability
- [ ] Push notifications
- [ ] Biometric authentication

### 11. Gamification & Engagement
**Timeline:** 2 weeks
**Status:** LOW PRIORITY

- [ ] Tax filing streak rewards
- [ ] Civic badges
- [ ] On-time filing leaderboard
- [ ] Community forum

### 12. Predictive Analytics
**Timeline:** 3 weeks
**Status:** LOW PRIORITY

- [ ] What-if scenarios
- [ ] Tax planning suggestions
- [ ] Optimization recommendations

---

## 📊 IMPLEMENTATION TIMELINE

### **Phase 4: Foundation (Weeks 1-6)**
- User Management & Authentication (2 weeks)
- Multi-Tenancy & Rule Engine 2.0 (2 weeks)
- Comprehensive Database Layer (2 weeks)

### **Phase 5: Core Workflow (Weeks 7-13)**
- Auditor Workflow (3 weeks)
- Advanced AI Extraction (3 weeks)
- Return History & Audit Trail (2 weeks)

### **Phase 6: Advanced Features (Weeks 14-20)**
- Reconciliation Engine (2 weeks)
- Reporting Engine (3 weeks)
- Tax Research & Rule Expansion (4 weeks, ongoing)

### **Phase 7: Future (Weeks 21+)**
- Mobile app
- Gamification
- Predictive analytics

---

## 🎯 SUCCESS METRICS

### User Management:
- [ ] Support 1000+ users
- [ ] < 2 second login time
- [ ] 99.9% authentication uptime

### Auditor Workflow:
- [ ] 50% reduction in review time
- [ ] 90% auditor satisfaction
- [ ] < 5% rejection rate

### AI Extraction:
- [ ] 95% extraction accuracy
- [ ] < 30 seconds per document
- [ ] 100% source traceability

### Reconciliation:
- [ ] 90% automatic matching
- [ ] < 1% false positives
- [ ] 100% discrepancy detection

---

**Document Owner:** Product Team
**Last Review:** November 26, 2025
**Next Review:** December 10, 2025
