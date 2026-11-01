# Vehicle Rental Platform

A modern, production-oriented vehicle rental marketplace launching in Vijayawada, Andhra Pradesh. The platform supports cars, bikes, and scooters through two distinct supply channels:

1. **Partner-Owned Fleet** — verified rental partners register and manage their own vehicles independently.
2. **Contributor-Owned Fleet** — individuals submit vehicles to a rental partner. If accepted, the partner manages rental operations and pays the contributor an agreed fixed compensation per eligible completed booking under a recorded agreement.

Customers use a unified marketplace to discover and book both inventory types.

---

## MVP Payment Architecture: Partner-Direct Collection

The MVP adopts a **Partner-Direct Payment Collection** model (ADR-018):
- **Direct Payee:** The rental partner managing a vehicle is the customer-facing rental service provider and direct payee for all bookings on that vehicle.
- **Zero Platform Cash Handling:** The platform does NOT collect, hold, escrow, split, or disburse customer rental money or security deposits in the MVP.
- **Payment Methods Supported:** Partner's UPI QR code, Partner's UPI ID, Partner's external payment link (if configured), and Cash at pickup (where enabled by the partner).
- **Manual Verification Gate (Strict Invariant):** A customer-uploaded screenshot or UTR never automatically marks a payment as verified. ONLY explicit partner confirmation marks the payment `VERIFIED` and confirms the booking.
- **Security Deposit Custody & Refunds:** The partner directly receives, holds, and refunds security deposits outside the platform. The platform records demands, collection references, evidence-based deductions, and refund audit trails.
- **Contributor Compensation (Approach A):** The partner pays the contributor an agreed fixed fee per eligible completed booking directly outside the platform via NEFT/IMPS/UPI. Tracked in `contributor_payables`.
- **Future-Phase Marketplace Gateway:** The architecture remains provider-agnostic. Integration with an approved marketplace payment provider (e.g. Razorpay Route) for automated split settlements and platform general ledger tracking is deferred to a future phase.

---

## Technology Stack

- **Frontend:** React + Vite, Tailwind CSS, React Router, Axios
- **Backend:** Java + Spring Boot, Spring Security, Spring Data JPA
- **Database:** MySQL 8.0+
- **Authentication:** BCrypt password hashing, JWT access/refresh token flow with server-side rotation/revocation (`refresh_tokens`), and resource-level authorization
- **External Integrations:** Cloudinary/S3 private media storage, transactional notifications via Transactional Outbox, provider-agnostic payment interfaces

---

## Core Roles & Capabilities

- `CUSTOMER`: Browse, compare, create 15-minute booking holds, view managing partner payment instructions, submit payment references (UTR / receipt), track verification status, participate in inspections, dispute deposit deductions (48h window), view own payments and verification records.
- `VEHICLE_CONTRIBUTOR`: Submit personally owned vehicles, review partner offers, manage agreements/availability requests, participate in direct handover/return inspections, acknowledge direct handovers, view earnings and settlement statuses (with UTR references), acknowledge or dispute direct settlements, request vehicle withdrawal.
- `RENTAL_PARTNER (VERIFIED)`: Maintain partner business profile, configure direct payment profile (UPI QR/ID/Link), verify or reject customer payment submissions, manage hubs and pickup locations, register independently owned fleet, review contributor vehicle submissions, manage accepted vehicles and bookings, conduct inspections, manage direct deposit custody and refunds, and record direct contributor settlements with UTR (Approach A).
- `ADMIN`: Verify partners and vehicles, arbitrate deposit disputes and damage claims, oversee compliance, and view security audit logs. Admin access is permission-controlled and audited.

---

## Authoritative Database Design Freeze Documentation (47 Physical Tables for MVP)

All documentation reflects the reconciled database design freeze candidate baseline:

1. [Project Constitution](docs/PROJECT_CONSTITUTION.md) — Mandatory development rules, non-negotiable domain invariants, partner-direct payment rules, and configurable operational defaults.
2. [Business Requirements](docs/BUSINESS_REQUIREMENTS.md) — Workflows, dual inventory channels, 8 custody permutations, 10-step partner-direct payment workflow, and tax disclaimers.
3. [System Architecture](docs/ARCHITECTURE.md) — Modular monolith design, 45 MVP domain tables + 2 infra tables, outbox pattern, and distributed scheduling.
4. [Architecture Decision Records](docs/ARCHITECTURE_DECISIONS.md) — Formal ADRs (ADR-001 through ADR-018), including ADR-018 Partner-Direct Payment Collection.
5. [Final Physical Entity Inventory](docs/FINAL_ENTITY_INVENTORY.md) — Authoritative inventory of **47 proposed physical tables for MVP** (45 application-domain + 2 infrastructure) + **8 Future-Phase tables**.
6. [Entity Relationships & Master ERD](docs/ENTITY_RELATIONSHIPS.md) — Referential integrity, cardinality, and comprehensive Mermaid ER diagram connecting all 45 MVP domain tables.
7. [Operational & Financial State Machines](docs/STATE_MACHINES.md) — Decoupled lifecycle state transition specifications for holds, bookings, payment demands, submissions, deposit refunds, agreements, and custody.
8. [Financial & Operational Record-Keeping Architecture](docs/FINANCIAL_ARCHITECTURE.md) — Zero platform cash handling, partner direct collection, deposit direct custody, contributor compensation tracking (Approach A), and future-phase general ledger roadmap.
9. [Role-Permission Matrix](docs/ROLE_PERMISSION_MATRIX.md) — Global capabilities, verified partner gates, payment verification authorization, and fine-grained resource-level security (BOLA/IDOR defense).
10. [API Module Plan](docs/API_MODULES.md) — REST endpoint inventory under `/api/v1` namespace.
11. [Security Baseline](docs/SECURITY.md) — Authentication, token encryption, payment submission verification security, and dispute arbitration controls.
12. [Page and Route Inventory](docs/PAGES_AND_ROUTES.md) — Frontend route map across roles, including payment instruction and partner verification screens.
13. [Development Workflow](docs/DEVELOPMENT_WORKFLOW.md) — Phase discipline, Git workflow, and quality gates.
14. [Architecture Review Report](docs/architecture_review_report.md) — Pre-freeze validation audit findings, final reconciliation report, and sign-off status.

---

## Implementation Status

> [!IMPORTANT]
> This repository is currently in the **Database Design Freeze & Pre-Implementation** stage. **No application code, Flyway migrations, JPA entities, or SQL DDL scripts have been generated yet.** Implementation will commence only after formal Product Owner database design freeze sign-off.
