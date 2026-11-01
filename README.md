# Vehicle Rental Platform

A modern, production-oriented vehicle rental marketplace launching in Vijayawada, Andhra Pradesh. The platform supports cars, bikes, and scooters through two distinct supply channels:

1. **Partner-Owned Fleet** — verified rental partners register and manage their own vehicles independently.
2. **Contributor-Owned Fleet** — individuals submit vehicles to a rental partner. If accepted, the partner manages rental operations and pays the contributor an agreed fixed compensation per eligible completed booking under a recorded agreement.

Customers use a unified marketplace to discover and book both inventory types.

---

## Technology Stack

- **Frontend:** React + Vite, Tailwind CSS, React Router, Axios
- **Backend:** Java + Spring Boot, Spring Security, Spring Data JPA
- **Database:** MySQL 8.0+
- **Authentication:** BCrypt password hashing, JWT access/refresh token flow with server-side rotation/revocation (`refresh_tokens`), and resource-level authorization
- **External Integrations Planned:** Razorpay payments and refunds, Cloudinary/S3 private media storage, transactional notifications via Transactional Outbox

---

## Core Roles & Capabilities

- `CUSTOMER`: Browse, compare, create 15-minute booking holds, confirm bookings via payment, manage own bookings, request extensions/cancellations, participate in inspections, dispute deposit deductions (48h window), view own payments and verification records.
- `VEHICLE_CONTRIBUTOR`: Submit personally owned vehicles, review partner offers, manage agreements/availability requests, participate in direct handover/return inspections, acknowledge direct handovers, view earnings and settlement statuses (with UTR references), acknowledge or dispute direct settlements, request vehicle withdrawal.
- `RENTAL_PARTNER`: Maintain partner business profile, manage hubs and delivery zones, register independently owned fleet, review contributor vehicle submissions, manage accepted vehicles and bookings, conduct inspections, manage handovers/returns, and record direct contributor settlements with UTR (Approach A).
- `ADMIN`: Verify partners and vehicles, manage commission rules, arbitrate deposit disputes and damage claims, audit double-entry ledger, oversee compliance, and view security audit logs. Admin access is permission-controlled and audited.

---

## Authoritative Pre-Freeze Architecture Documentation

All documentation has been updated to reflect the pre-freeze architecture baseline:

1. [Project Constitution](docs/PROJECT_CONSTITUTION.md) — Mandatory development rules, non-negotiable domain invariants, and configurable operational defaults.
2. [Business Requirements](docs/BUSINESS_REQUIREMENTS.md) — Workflows, dual inventory channels, 8 custody permutations, damage claim review rules, and tax disclaimers.
3. [System Architecture](docs/ARCHITECTURE.md) — Modular monolith design, module ownership, outbox pattern, and distributed scheduling.
4. [Architecture Decision Records](docs/ARCHITECTURE_DECISIONS.md) — Formal ADRs (ADR-001 through ADR-013).
5. [Final Physical Entity Inventory](docs/FINAL_ENTITY_INVENTORY.md) — Authoritative inventory of **52 proposed physical tables** (50 application-domain + 2 infrastructure).
6. [Entity Relationships & Master ERD](docs/ENTITY_RELATIONSHIPS.md) — Referential integrity, cardinality, and comprehensive Mermaid ER diagram connecting all 50 domain tables.
7. [Operational & Financial State Machines](docs/STATE_MACHINES.md) — Decoupled lifecycle state transition specifications for holds, bookings, payments, refunds, deposits, agreements, and custody.
8. [Financial Architecture & Ledger](docs/FINANCIAL_ARCHITECTURE.md) — Double-entry accounting journal scenarios, Approach A (MVP) vs Approach B (Future), and contributor price-floor engine.
9. [Role-Permission Matrix](docs/ROLE_PERMISSION_MATRIX.md) — Global capabilities and fine-grained resource-level authorization rules (BOLA/IDOR defense).
10. [API Module Plan](docs/API_MODULES.md) — REST endpoint inventory under `/api/v1` namespace.
11. [Security Baseline](docs/SECURITY.md) — Authentication, token encryption, application defenses, and dispute arbitration security.
12. [Page and Route Inventory](docs/PAGES_AND_ROUTES.md) — Frontend route map across roles.
13. [Development Workflow](docs/DEVELOPMENT_WORKFLOW.md) — Phase discipline, Git workflow, and quality gates.
14. [Architecture Review Report](docs/architecture_review_report.md) — Pre-freeze validation audit findings and resolution status.

---

## Implementation Status

> [!IMPORTANT]
> This repository is currently in the **Pre-Freeze Architecture Validation** stage. **No application code, Flyway migrations, JPA entities, or SQL DDL scripts have been generated yet.** Implementation will commence only after formal Product Owner database design freeze sign-off.
