# MuniTax Documentation Index

## Overview

Welcome to the MuniTax documentation. This directory contains comprehensive technical documentation for the Municipal Tax Filing System.

---

## Documentation Structure

```
docs/
├── ARCHITECTURE.md           # System architecture and technology stack
├── DATA_FLOW.md              # Data flow diagrams and workflows
├── SEQUENCE_DIAGRAMS.md      # Detailed sequence diagrams for key processes
├── PII_DATA.md               # PII data handling and classification
├── DATA_SECURITY.md          # Security architecture and controls
├── FEATURES_LIST.md          # Complete features list with status
├── MODULES_LIST.md           # Backend and frontend module breakdown
├── RULE_ENGINE.md            # Rule engine architecture and configuration
├── TAX_COMPUTATIONS.md       # Tax calculation algorithms and formulas
├── CONFIGURABLE_DESIGN.md    # Configurable system parameters
├── SCHEDULE_X_README.md      # Schedule X expansion feature details
└── README.md                 # This index file
```

### Related Documents (Repository Root)

```
/RULE_ENGINE_DISCONNECT_ANALYSIS.md   # Critical integration issues analysis
```

---

## Quick Reference Guide

### For Developers

| Document | Purpose | When to Use |
|----------|---------|-------------|
| [ARCHITECTURE.md](./ARCHITECTURE.md) | System design, microservices, tech stack | Starting new development, onboarding |
| [MODULES_LIST.md](./MODULES_LIST.md) | Code organization, service responsibilities | Finding where code lives |
| [SEQUENCE_DIAGRAMS.md](./SEQUENCE_DIAGRAMS.md) | Request/response flows | Understanding interactions |
| [DATA_FLOW.md](./DATA_FLOW.md) | Data transformation pipelines | Debugging data issues |
| [/RULE_ENGINE_DISCONNECT_ANALYSIS.md](../RULE_ENGINE_DISCONNECT_ANALYSIS.md) | Known integration issues | Debugging rule service issues |

### For Tax Domain Understanding

| Document | Purpose | When to Use |
|----------|---------|-------------|
| [TAX_COMPUTATIONS.md](./TAX_COMPUTATIONS.md) | Tax calculation algorithms | Implementing tax logic |
| [RULE_ENGINE.md](./RULE_ENGINE.md) | Dynamic rule configuration | Adding/modifying tax rules |
| [CONFIGURABLE_DESIGN.md](./CONFIGURABLE_DESIGN.md) | System configuration options | Customizing for tenants |
| [FEATURES_LIST.md](./FEATURES_LIST.md) | Feature inventory and status | Planning work |

### For Security & Compliance

| Document | Purpose | When to Use |
|----------|---------|-------------|
| [DATA_SECURITY.md](./DATA_SECURITY.md) | Security controls, authentication | Security reviews |
| [PII_DATA.md](./PII_DATA.md) | Sensitive data handling | Privacy compliance |

---

## Document Summaries

### ARCHITECTURE.md
High-level system architecture including:
- Microservices architecture diagram
- Technology stack (React, Spring Boot, PostgreSQL)
- Service catalog and responsibilities
- Deployment architecture (Docker)
- Communication patterns

### DATA_FLOW.md
Data flow diagrams covering:
- Individual tax filing flow
- Business tax filing flow
- Document extraction pipeline
- Auditor workflow data flow
- Payment processing flow

### SEQUENCE_DIAGRAMS.md
Detailed sequence diagrams for:
- User authentication
- Tax return filing (individual and business)
- Document extraction with Gemini AI
- Auditor review workflow
- Rule configuration
- Payment processing

### PII_DATA.md
PII data documentation including:
- Data classification levels (HIGH, MEDIUM, LOW)
- PII inventory by data type
- Protection measures
- Masking and encryption requirements
- Compliance requirements

### DATA_SECURITY.md
Security architecture covering:
- Authentication (JWT-based)
- Authorization (RBAC)
- Encryption (at rest and in transit)
- Network security
- Audit logging
- Incident response

### FEATURES_LIST.md
Complete feature inventory:
- Tax filing features
- Document processing
- Validation rules
- Auditor workflow
- Rule engine
- Payment features
- PDF generation

### MODULES_LIST.md
Module breakdown including:
- Frontend components and services
- Backend microservices
- Package structures
- API endpoints per service
- Module dependencies

### RULE_ENGINE.md
Rule engine documentation:
- Rule architecture and components
- Rule categories and value types
- Rule lifecycle and approval workflow
- Temporal rule management
- Built-in rules
- Adding custom rules

### TAX_COMPUTATIONS.md
Tax calculation algorithms:
- Individual tax calculation flow
- W-2 qualifying wages rules
- Schedule X/Y processing
- Business tax calculation
- NOL carryforward logic
- Penalty and interest calculations
- Discrepancy detection

### CONFIGURABLE_DESIGN.md
Configuration documentation:
- Tax rate configuration
- Income inclusion settings
- Penalty/interest parameters
- Validation thresholds
- Multi-tenant configuration
- Temporal configuration

### RULE_ENGINE_DISCONNECT_ANALYSIS.md (Root)
Critical analysis document covering:
- Database connection disconnects between services
- Missing schema migrations
- Enum value mismatches
- Service integration issues
- Recommended fix strategy

---

## External Documentation

| Document | Location | Purpose |
|----------|----------|---------|
| Disconnect Analysis | [/RULE_ENGINE_DISCONNECT_ANALYSIS.md](../RULE_ENGINE_DISCONNECT_ANALYSIS.md) | Critical integration issues |
| API Samples | [/API_SAMPLES.md](/API_SAMPLES.md) | API request/response examples |
| Docker Guide | [/DOCKER_DEPLOYMENT_GUIDE.md](/DOCKER_DEPLOYMENT_GUIDE.md) | Container deployment |
| Current Features | [/CURRENT_FEATURES.md](/CURRENT_FEATURES.md) | Implementation status |
| Specifications | [/specs/README.md](/specs/README.md) | Feature specifications |

---

## Diagram Types

The documentation uses [Mermaid](https://mermaid.js.org/) diagrams extensively. Here are the diagram types used:

| Type | Used For | Example |
|------|----------|---------|
| `flowchart` | Process flows, decision trees | Tax calculation flow |
| `sequenceDiagram` | Request/response interactions | API call sequences |
| `graph` | Architecture, relationships | Microservices diagram |
| `erDiagram` | Data models | Rule entity relationships |
| `stateDiagram` | State machines | Session lifecycle |
| `gantt` | Timelines | Rule effective dates |
| `classDiagram` | Object relationships | Service dependencies |

### Viewing Diagrams

Mermaid diagrams render automatically in:
- GitHub (markdown files)
- VS Code (with Markdown Preview Mermaid Support extension)
- IntelliJ IDEA (with Markdown plugin)

---

## Contributing to Documentation

### Guidelines

1. **Keep current**: Update docs when code changes
2. **Use diagrams**: Mermaid diagrams preferred
3. **Be specific**: Include code examples where helpful
4. **Version history**: Update version table at bottom

### Document Template

```markdown
# Title

## Overview
Brief description of what this document covers.

---

## Section 1
Content with diagrams and tables.

---

## Section 2
More content.

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | YYYY-MM-DD | Initial version |

---

**Document Owner:** Team Name  
**Last Updated:** Date
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.2 | 2025-12-10 | Referenced updated FEATURES_LIST.md with corrected feature statuses |
| 1.1 | 2025-12-01 | Updated with latest main branch changes, added disconnect analysis reference |
| 1.0 | 2025-12-01 | Initial documentation creation |

---

**Document Owner:** Development Team  
**Last Updated:** December 10, 2025
