# Design and Implementation consideration

## 0. Ticketing System Task breakdown
The following is an overview of all the steps taken in order to complete the task assigned.

Research:
- Gather non-functional requirements
- Design high-level system architecture
- Model core domain entities and relationships (data model design)
- Derive design decision and impacts for the task.

Application development:
- Generate project scaffolding [(Spring Initializr)](https://start.spring.io/)
- Create API contract [(OpenAPI Spec)](https://swagger.io/specification/)
- Generate REST controllers and models [(Swagger Codegen)](https://swagger.io/tools/swagger-codegen/)
- Implement cross-cutting concerns (exception handling,security,logging filter,audit etc.) [(Claude 4.0)](https://www.anthropic.com/news/claude-4)
- Refactor code:
    - separate layers
    - apply best practices
    - fix/debug issues
- Create custom metrics

Testing/tuning phase:
- Apply performance tuning
- Create integration test for business logic validation.
- Create load tests to tweak and validate performance metrics.
- Create mock tests for failure scenarios.

Cloud DevOps:
- Create multi-stage dockerfile:
  - Tune alpine base image
  - Apply JVM tuning
  - Enforce runtime security constraints

- Create docker compose.yaml ( K8S is superior for container orchestration )

Breakdown into sub-tickets:
- PLS-1 : Research
- PLS-2 : Application development
- PLS-3 : Testing/tuning phase
- PLS-4 : Cloud DevOps
