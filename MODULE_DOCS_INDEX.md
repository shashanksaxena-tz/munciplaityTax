# Module Documentation Index

**Municipality Tax Management System** - Complete module-level documentation index for AI assistants and developers.

---

## 📚 Documentation Overview

This project now has comprehensive per-folder documentation generated using the **per-folder-docs skill**. Each major directory contains:

- **AI-CONTEXT.md** - AI-optimized documentation with detailed APIs, patterns, and usage
- **README.md** - Human-friendly overview with quick starts and examples

---

## 🗂️ Module Index

| Module | Description | AI Context | README |
|--------|-------------|------------|--------|
| **[backend/](backend/README.md)** | 10 Spring Boot microservices for tax management | [📄](backend/AI-CONTEXT.md) | [📖](backend/README.md) |
| **[src/](src/README.md)** | Frontend source code (components, services, hooks, types) | [📄](src/AI-CONTEXT.md) | [📖](src/README.md) |
| **[components/](components/README.md)** | 50+ top-level React components (dashboards, wizards, forms) | [📄](components/AI-CONTEXT.md) | [📖](components/README.md) |
| **[services/](services/README.md)** | Frontend API client layer (5 service modules) | [📄](services/AI-CONTEXT.md) | [📖](services/README.md) |
| **[config/](config/AI-CONTEXT.md)** | Design system and form schemas | [📄](config/AI-CONTEXT.md) | - |
| **[contexts/](contexts/AI-CONTEXT.md)** | React Context providers (Auth, Profile, Toast) | [📄](contexts/AI-CONTEXT.md) | - |
| **[docs/](docs/AI-CONTEXT.md)** | Technical documentation (15 docs) | [📄](docs/AI-CONTEXT.md) | - |

---

## 🏗️ Architecture Quick Reference

### Backend Architecture
```
Discovery Service (Eureka)
    ↓
Gateway Service
    ↓
┌─────────────────────────────────┐
│    Microservices                │
├──────────┬──────────────────────┤
│ Auth     │ Tax Engine (largest) │
│ Extract  │ Ledger               │
│ Submit   │ PDF                  │
│ Rule     │ Tenant               │
└──────────┴──────────────────────┘
```

**Services:** 10 microservices  
**Technology:** Spring Boot 3.x, Java 17, PostgreSQL, Redis  
**Docs:** [backend/AI-CONTEXT.md](backend/AI-CONTEXT.md)

### Frontend Architecture
```
App.tsx / TaxFilingApp.tsx
    ↓
Components/ (50+ page-level components)
    ↓
src/
├── components/ (24 reusable UI components)
├── services/ (5 API clients)
├── hooks/ (8 custom hooks)
├── types/ (TypeScript definitions)
└── utils/ (Helper functions)
```

**Components:** 74+ unique components  
**Technology:** React 18, TypeScript, Vite, Tailwind  
**Docs:** [src/AI-CONTEXT.md](src/AI-CONTEXT.md), [components/AI-CONTEXT.md](components/AI-CONTEXT.md)

---

## 🎯 Quick Navigation

### By Role

**Backend Developer:**
- [Backend Services Overview](backend/README.md)
- [API Documentation](docs/API_STATUS.md)
- [Architecture](docs/ARCHITECTURE.md)

**Frontend Developer:**
- [Frontend Source](src/README.md)
- [Component Library](components/README.md)
- [API Services](services/README.md)

**Full Stack Developer:**
- [Complete Documentation Summary](PER_FOLDER_DOCS_SUMMARY.md)
- [Data Flow](docs/DATA_FLOW.md)
- [Sequence Diagrams](docs/SEQUENCE_DIAGRAMS.md)

**DevOps/SRE:**
- [Backend Setup](backend/README.md#development-setup)
- [Service Architecture](backend/AI-CONTEXT.md#service-architecture)

---

## 📊 Project Statistics

### Backend
- **Services:** 10 microservices
- **Java Files:** 100+ files
- **Largest Service:** tax-engine-service (241 files)

### Frontend
- **Components:** 74+ components
- **Largest Component:** RuleManagementDashboard.tsx (85KB)
- **Service Modules:** 5 specialized clients
- **Custom Hooks:** 8 hooks

### Documentation
- **AI-CONTEXT.md files:** 7
- **README.md files:** 4
- **Technical Docs:** 15 files
- **Total Pages:** 11 module docs + 15 technical docs

---

## 🔍 Finding What You Need

### Search by Feature
| Feature | Location |
|---------|----------|
| **Authentication** | [backend/auth-service/](backend/), [contexts/AuthContext.tsx](contexts/) |
| **Tax Calculations** | [backend/tax-engine-service/](backend/), [components/](components/) |
| **Document Extraction** | [backend/extraction-service/](backend/), [services/extractionMapper.ts](services/) |
| **Forms** | [config/form-schemas/](config/), [components/forms/](components/) |
| **Payments** | [components/payment/](components/), [components/PaymentGateway.tsx](components/) |
| **Ledger** | [backend/ledger-service/](backend/), [components/LedgerDashboard.tsx](components/) |
| **Business Rules** | [backend/rule-service/](backend/), [services/ruleService.ts](services/) |

### Search by Technology
| Technology | Location |
|------------|----------|
| **Spring Boot** | [backend/](backend/) |
| **React** | [src/](src/), [components/](components/) |
| **TypeScript** | [src/types/](src/) |
| **Axios** | [services/api.ts](services/) |
| **React Query** | [src/hooks/](src/) |
| **Zod** | [services/formSchemaService.ts](services/) |
| **Tailwind** | [config/design-system.ts](config/) |

---

## 📖 Documentation Types

### Module Documentation (This Index)
- **AI-CONTEXT.md** - Detailed, AI-optimized documentation
- **README.md** - Human-friendly quick starts

### Technical Documentation
Located in `docs/`:
- Architecture and design
- API specifications
- Security and compliance
- Feature descriptions
- Sequence diagrams

### Code Documentation
- Inline code comments
- JSDoc/JavaDoc
- Type definitions

---

## 🎓 Getting Started Guides

### For AI Assistants
1. Start with [PER_FOLDER_DOCS_SUMMARY.md](PER_FOLDER_DOCS_SUMMARY.md)
2. Read module-specific AI-CONTEXT.md files
3. Reference technical docs in `docs/`

### For Developers
1. Read [README.md](README.md) in project root
2. Review architecture: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
3. Explore module READMEs for your area of focus

### For New Contributors
1. Start with [docs/README.md](docs/README.md)
2. Read [backend/README.md](backend/README.md) for backend
3. Read [src/README.md](src/README.md) for frontend
4. Review [components/README.md](components/README.md) for UI

---

## 🔄 Keeping Documentation Updated

Documentation is marked with section markers:

```html
<!-- AUTO-GENERATED: SectionName -->
This content will be regenerated
<!-- END AUTO-GENERATED -->

<!-- CUSTOM SECTION: SectionName -->
User content preserved during regeneration
<!-- END CUSTOM SECTION -->
```

To regenerate documentation:
```bash
/per-folder-docs [folder-path]
```

---

## 📝 Documentation Standards

All documentation follows consistent structure:

### AI-CONTEXT.md Sections
1. Purpose
2. Key Files
3. Public API
4. Dependencies
5. Used By
6. Patterns & Conventions
7. Usage Examples

### README.md Sections
1. Overview
2. Structure
3. Quick Start
4. Key Features
5. Related Modules

---

## 🌟 Next Steps

### Expand Documentation Coverage
```bash
# Document individual backend services
/per-folder-docs backend/auth-service
/per-folder-docs backend/tax-engine-service

# Document component subdirectories
/per-folder-docs components/auth
/per-folder-docs components/payment

# Document src subdirectories
/per-folder-docs src/hooks
/per-folder-docs src/types
```

### Enhance Existing Documentation
- Add custom sections to AI-CONTEXT.md files
- Include deployment procedures
- Add troubleshooting guides

---

## 📞 Support & Resources

- **Full Documentation Summary:** [PER_FOLDER_DOCS_SUMMARY.md](PER_FOLDER_DOCS_SUMMARY.md)
- **Technical Docs Index:** [docs/README.md](docs/README.md)
- **Architecture Overview:** [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- **API Reference:** [docs/API_STATUS.md](docs/API_STATUS.md)

---

**Generated:** 2026-02-03T17:03:50+05:30  
**Last Updated:** 2026-02-03T17:03:50+05:30  
**Documentation Version:** 1.0
