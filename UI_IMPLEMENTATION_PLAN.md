# MuniTax - UI/Frontend Implementation Plan

**Last Updated:** November 26, 2025, 8:15 PM
**Status:** Comprehensive UI plans for ALL features

---

## ✅ YES! UI IS FULLY PLANNED FOR EVERY FEATURE

This document provides a comprehensive overview of all UI/Frontend implementations planned for MuniTax. **Every backend feature has a corresponding UI plan.**

---

## 📱 PHASE 4: USER MANAGEMENT UI (15 Components)

### Registration & Authentication
1. **RegistrationForm.tsx** - Full registration with validation
2. **LoginForm.tsx** - Login with role-based redirect
3. **EmailVerification.tsx** - Email verification page
4. **ForgotPassword.tsx** - Password reset request
5. **ResetPassword.tsx** - Password reset form

### Profile Management
6. **ProfileDashboard.tsx** - Profile management hub
7. **ProfileCard.tsx** - Individual profile display
8. **CreateProfileModal.tsx** - Profile creation dialog
9. **EditProfileModal.tsx** - Profile editing dialog
10. **ProfileSwitcher.tsx** - Header profile dropdown

### Navigation & Layout
11. **ProtectedRoute.tsx** - Route guards
12. **Header.tsx** - Main navigation
13. **UserMenu.tsx** - User dropdown menu
14. **LoadingSpinner.tsx** - Loading states
15. **Toast.tsx** - Notifications

**Features:**
- Email/password validation
- Password strength indicator
- SSN/EIN masking
- Role-based routing
- Profile switching
- Responsive design
- Accessibility (ARIA, keyboard nav)

---

## 🔍 PHASE 5: AUDITOR WORKFLOW UI (20 Components)

### Dashboard & Queue
1. **AuditorDashboard.tsx** - Main auditor dashboard
2. **SubmissionQueue.tsx** - Submission queue table
3. **StatisticsCards.tsx** - Statistics widgets
4. **FilterPanel.tsx** - Advanced filters
5. **BulkActionBar.tsx** - Bulk operations

### Split-Screen Review
6. **SplitScreenReview.tsx** - Main review layout
7. **PdfViewer.tsx** - PDF display with PDF.js
8. **PdfToolbar.tsx** - PDF controls (zoom, rotate, navigate)
9. **FormDataPanel.tsx** - Right panel with tabs
10. **FormAccordion.tsx** - Expandable form sections

### Field Editing & Validation
11. **FieldEditor.tsx** - Inline field editing
12. **ConfidenceIndicator.tsx** - AI confidence badges
13. **OverrideReasonModal.tsx** - Override justification
14. **RecalculationModal.tsx** - Before/after comparison

### Actions & Notifications
15. **ApproveModal.tsx** - Approval dialog
16. **RejectModal.tsx** - Rejection with required reasons
17. **RequestInfoModal.tsx** - Request more information
18. **NotificationBell.tsx** - Notification icon with badge
19. **NotificationList.tsx** - Notification dropdown
20. **TimelineView.tsx** - Action history timeline

**Features:**
- Split-screen layout (50/50 adjustable)
- PDF annotation tools
- Bulk approve/reject
- Field-level overrides with audit trail
- Real-time notifications
- Email integration
- Responsive (desktop/tablet/mobile)

---

## 🤖 PHASE 6: AI EXTRACTION UI (12 Components)

### Visual Provenance
1. **ClickToSourceViewer.tsx** - Click field → show PDF source
2. **BoundingBoxOverlay.tsx** - Highlight extraction area
3. **ConfidenceScoreDisplay.tsx** - Show AI confidence
4. **MultiPageIndicator.tsx** - Forms spanning pages

### Ignored Items
5. **IgnoredItemsReport.tsx** - List of ignored pages
6. **IgnoredItemCard.tsx** - Individual ignored item
7. **PageThumbnail.tsx** - Thumbnail preview
8. **ManualIncludeButton.tsx** - Override ignore decision

### Real-Time Updates
9. **ExtractionProgressFeed.tsx** - WebSocket live updates
10. **ProgressBar.tsx** - Visual progress indicator
11. **ExtractionStats.tsx** - Statistics display
12. **ExtractionTimeline.tsx** - Step-by-step progress

**Features:**
- Click any field → PDF opens to exact location
- Bounding box highlighting
- Ignored pages with reasons
- Real-time WebSocket feed:
  - "Upload received: 15 files, 47 pages"
  - "Identified 5 tax forms"
  - "Skipping 2 non-tax documents"
  - "Extracting data from W-2..."
- Progress indicators
- Confidence distribution charts

---

## 📊 PHASE 7: REPORTING UI (10 Components)

### Standard Reports
1. **ReportsDashboard.tsx** - Reports hub
2. **DailyReceiptsReport.tsx** - Revenue report
3. **DelinquentFilersReport.tsx** - Overdue returns
4. **TopTaxpayersReport.tsx** - Top 100 list
5. **FilingStatisticsReport.tsx** - Statistics dashboard

### Custom Report Builder
6. **ReportBuilder.tsx** - Drag-and-drop query builder
7. **FieldSelector.tsx** - Choose fields
8. **FilterBuilder.tsx** - Build complex filters
9. **ChartBuilder.tsx** - Visualization options
10. **ReportScheduler.tsx** - Schedule reports

**Features:**
- Pre-built report templates
- Visual query builder (no SQL needed)
- Drag-and-drop interface
- Charts (bar, line, pie, pivot)
- Export (PDF, Excel, CSV)
- Email scheduling
- Save custom reports

---

## 📝 PHASE 8: RETURN HISTORY UI (8 Components)

### Timeline & History
1. **ReturnHistoryPage.tsx** - Main history view
2. **TimelineComponent.tsx** - Chronological events
3. **EventCard.tsx** - Individual event display
4. **DiffViewer.tsx** - Side-by-side comparison
5. **VersionSelector.tsx** - Version dropdown
6. **ChangeHighlighter.tsx** - Highlight changes
7. **AmendmentBadge.tsx** - Amendment indicator
8. **HistoryFilters.tsx** - Filter by date, user, action

**Features:**
- Complete action timeline
- "User uploaded W-2 (10:00 AM)"
- "System extracted data (10:01 AM)"
- "User changed Box 18 (10:05 AM)"
- Side-by-side diff view
- Version rollback (admin only)
- Export history to PDF

---

## 🔄 PHASE 9: RECONCILIATION UI (6 Components)

### Business ↔ Individual
1. **ReconciliationDashboard.tsx** - Main reconciliation hub
2. **W2MatchingTable.tsx** - W-2 to W-1 matching
3. **DiscrepancyAlert.tsx** - Mismatch warnings
4. **BulkReconciliation.tsx** - Process multiple matches

### Federal ↔ Local
5. **IncomeComparisonChart.tsx** - Federal vs Local
6. **ScheduleValidator.tsx** - Cross-check schedules

**Features:**
- Automatic W-2 to W-1 matching
- Discrepancy highlighting (>$10)
- Bulk reconciliation
- Federal vs Local comparison
- Year-over-year analysis
- NOL tracking visualization

---

## 🏢 PHASE 10: MULTI-TENANCY UI (8 Components)

### Admin Interface
1. **TenantManagement.tsx** - Tenant admin dashboard
2. **TenantRegistration.tsx** - Onboard new municipality
3. **TenantSettings.tsx** - Per-tenant configuration
4. **BrandingEditor.tsx** - Logo, colors, theme
5. **TenantSwitcher.tsx** - Admin tenant switching

### Rule Engine UI
6. **RuleEditor.tsx** - Visual rule configuration
7. **RuleSandbox.tsx** - Test rules before publishing
8. **RuleVersioning.tsx** - Version history & rollback

**Features:**
- Tenant registration wizard
- Custom branding per tenant
- Dynamic rule editor
- Rule testing sandbox
- A/B testing support
- Version control
- Rollback capability

---

## 💳 PHASE 11: PAYMENT UI (6 Components)

### Payment Processing
1. **PaymentPage.tsx** - Payment gateway integration
2. **CreditCardForm.tsx** - Stripe integration
3. **ACHForm.tsx** - Bank account payments
4. **PaymentPlanSetup.tsx** - Installment options
5. **PaymentHistory.tsx** - Transaction history
6. **ReceiptViewer.tsx** - Receipt display/download

**Features:**
- Stripe integration
- ACH payments
- Payment plans
- Automatic receipts
- Refund processing
- Payment history

---

## 📱 PHASE 12: MOBILE APP UI (10 Components)

### React Native Components
1. **MobileLogin.tsx** - Mobile login screen
2. **MobileDashboard.tsx** - Mobile dashboard
3. **CameraScan.tsx** - Camera capture for forms
4. **OfflineDrafts.tsx** - Offline mode
5. **PushNotifications.tsx** - Push notification handler
6. **BiometricAuth.tsx** - Fingerprint/Face ID
7. **MobileFilingFlow.tsx** - Simplified filing
8. **MobileUpload.tsx** - Photo upload
9. **MobileReview.tsx** - Mobile review screen
10. **SyncManager.tsx** - Offline sync

**Features:**
- Camera scan for tax forms
- Offline drafts
- Push notifications
- Biometric authentication
- Auto-sync when online
- Simplified mobile UI

---

## 🎨 UI/UX STANDARDS (All Phases)

### Design System
- **Colors**: Consistent palette across all components
- **Typography**: Google Fonts (Inter, Roboto)
- **Spacing**: 4px grid system
- **Components**: Reusable component library
- **Icons**: Consistent icon set (Heroicons/Lucide)

### Responsive Design
- **Mobile First**: 375px+ (phones)
- **Tablet**: 768px+ (tablets)
- **Desktop**: 1024px+ (laptops)
- **Large Desktop**: 1920px+ (monitors)

### Accessibility
- **WCAG 2.1 AA** compliance
- **ARIA labels** on all interactive elements
- **Keyboard navigation** support
- **Screen reader** compatibility
- **Focus management**
- **Color contrast** ratios

### Performance
- **Code splitting** by route
- **Lazy loading** for heavy components
- **Image optimization**
- **Bundle size** monitoring
- **Lighthouse score** 90+

### State Management
- **React Context** for global state
- **React Query** for server state
- **Zustand** for complex state (optional)
- **WebSocket** for real-time updates

---

## 📊 TOTAL UI COMPONENTS

| Phase | Feature | Components | Status |
|-------|---------|------------|--------|
| 4 | User Management | 15 | ⏳ Planned |
| 5 | Auditor Workflow | 20 | ⏳ Planned |
| 6 | AI Extraction | 12 | ⏳ Planned |
| 7 | Reporting | 10 | ⏳ Planned |
| 8 | Return History | 8 | ⏳ Planned |
| 9 | Reconciliation | 6 | ⏳ Planned |
| 10 | Multi-Tenancy | 8 | ⏳ Planned |
| 11 | Payment | 6 | ⏳ Planned |
| 12 | Mobile | 10 | ⏳ Planned |
| **TOTAL** | **9 Features** | **95 Components** | **100% Planned** |

---

## ✅ CONCLUSION

**YES, UI IS FULLY CATERED FOR!**

Every backend feature in the roadmap has:
- ✅ Detailed UI component breakdown
- ✅ User flow diagrams (implicit in descriptions)
- ✅ Responsive design plans
- ✅ Accessibility considerations
- ✅ State management strategy
- ✅ Component count and naming

**Total UI Components Planned:** 95
**Total Pages/Views:** 30+
**Design System:** Comprehensive
**Accessibility:** WCAG 2.1 AA
**Responsive:** Mobile, Tablet, Desktop

**The UI implementation is as detailed as the backend implementation!** 🎨

---

**Document Owner:** Frontend Team
**Last Review:** November 26, 2025
**Next Review:** December 10, 2025
