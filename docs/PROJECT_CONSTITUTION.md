# Project Constitution

This document is the governing contract for all contributors and AI coding agents, including Antigravity. Read it before making changes.

## 1. Product principles

- Build a production-oriented vehicle rental marketplace, not a disposable demo.
- Preserve the two inventory sources: partner-owned vehicles and contributor-owned vehicles.
- Keep customer booking workflows unified across both inventory sources.
- Model legal ownership, operational management, custody and financial entitlement as separate concepts.
- Design for Vijayawada launch while keeping cities/locations configurable for expansion.

## 2. Non-negotiable engineering rules

1. Inspect the existing repository and relevant files before editing.
2. Do not replace, delete or restructure working code without explaining the reason and impact.
3. Do not create duplicate entities, services, controllers, components or API endpoints.
4. Keep frontend and backend independent under the repository root.
5. Backend uses layered, feature-oriented packages: controller, service, repository, model/entity, dto, mapper where needed.
6. Controllers handle HTTP concerns only. Business rules belong in services. Persistence belongs in repositories.
7. Never expose JPA entities directly from API controllers; use request/response DTOs.
8. Use database migrations (Flyway recommended) as the source of truth. Do not rely on Hibernate schema auto-update in production.
9. Validate input at API boundaries and enforce authorization in backend service/API layers.
10. Frontend route guards improve UX but are never a substitute for backend authorization.
11. No secrets, API keys, database passwords, signing keys or private documents in source control.
12. Use environment variables and documented example environment files with placeholders only.
13. Use consistent API response/error formats and global exception handling.
14. Add tests for business-critical paths and regression fixes.
15. Update documentation when routes, entities, statuses, permissions, environment variables or business rules change.

## 3. Domain invariants

- A user may hold multiple roles.
- A partner can create and manage its own vehicles without a contributor.
- A contributor vehicle must have a verified owner relationship and an accepted agreement before it becomes bookable.
- A vehicle must have one active operational manager at a time.
- A vehicle cannot have overlapping confirmed/active bookings.
- A confirmed booking must preserve the agreed price, deposit, cancellation terms and vehicle snapshot needed for later accounting.
- Contributor compensation is the fixed amount defined in the active agreement for an eligible completed booking, not a hardcoded global amount.
- Payments, refunds, platform commissions and contributor payables are separate ledger events.
- Booking lifecycle, payment lifecycle, inspection lifecycle and payout lifecycle must not be collapsed into one status field.
- Every custody handover and return is auditable.
- Only authorized users can see identity, licence, registration, insurance and agreement documents.

## 4. Required implementation discipline

For each phase:
1. State the scope and assumptions.
2. Inspect current code and dependencies.
3. Present files to be added/changed and important design decisions.
4. Implement only the approved scope.
5. Run relevant formatting, build and tests.
6. Report commands run and actual results; never claim an unrun test passed.
7. Summarize API/schema/UI changes and remaining risks.
8. Update docs and provide a commit-ready summary.

## 5. Architecture change control

If a requirement conflicts with this constitution or current architecture:
- Stop before making a broad structural change.
- Explain the conflict and propose options.
- Preserve backwards compatibility where practical.
- Update this document only after the product owner approves the change.

## 6. Code quality

- Prefer readable, cohesive classes and components over oversized files.
- Avoid premature microservices; start with a modular monolith.
- Keep business statuses as explicit enums/constants with documented transitions.
- Use decimal types for money (Java BigDecimal; SQL DECIMAL), never floating point.
- Store instants consistently (UTC in persistence; present in local timezone, Asia/Kolkata for initial operations).
- Use pagination for potentially large collections.
- Make state-changing operations safe against retries where appropriate (idempotency).
- Log correlation identifiers and useful operational context; never log credentials, tokens or sensitive identity data.
