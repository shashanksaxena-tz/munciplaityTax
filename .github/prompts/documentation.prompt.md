---
agent: documentation
description: Generate comprehensive system documentation for any repository
---

# Documentation Generator

This prompt invokes the documentation agent to generate comprehensive system documentation.

## Usage

Simply invoke this prompt to analyze the current repository and generate documentation including:
- Architecture documentation with diagrams
- Data flow documentation
- Sequence diagrams for key workflows
- PII data classification and handling
- Security documentation
- Features list
- Modules list
- Configurable design documentation

## Examples

```
/documentation
```

Or with specific context:

```
/documentation Focus on the API layer and microservices architecture
```

The agent will analyze the repository structure, identify technologies, and generate comprehensive documentation in the `docs/` folder.
