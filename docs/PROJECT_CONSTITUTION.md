# Project Constitution (Pre-Freeze Specification)

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh (Expandable)  
**Status:** Pre-Freeze Corrected Baseline  

This document is the governing contract for all human contributors, engineering leads, and AI coding agents. Read it before making changes.

---

## 1. Product Principles

- **Production-Oriented Architecture:** Build a robust, enterprise-grade vehicle rental marketplace, not a disposable prototype.
- **Dual Inventory Channels:** Preserve both inventory channels: partner-owned fleet and contributor-owned vehicles.
- **Unified Customer Experience:** Customer search, checkout, booking, and return workflows must remain seamless and identical across both inventory sources.
- **Separation of Concerns:** Model legal ownership, operational manager assignment, physical custody, and financial settlement as independent, decoupled concepts.
- **Marketplace Neutrality:** The platform is a technology facilitator. The rental partner is the sole contractual debtor for contributor fixed earnings.
- **Configurability:** Launch in Vijayawada, AP, with configurable city, turnaround, pricing, and tax parameters to support rapid regional expansion.

---

## 2. Non-Negotiable Engineering Rules

1. Inspect the existing repository and all relevant documentation before proposing changes or writing code.
2. **Pre-Freeze Discipline:** Do not generate application code, JPA entities, SQL DDL scripts, or Flyway migrations until formal Product Owner database design freeze approval is granted.
3. Do not replace, delete, or restructure working documents without explaining the technical rationale.
4. Do not create duplicate entities, services, controllers, components, or API endpoints.
5. Keep frontend (`frontend/`) and backend (`backend/`) completely independent under the repository root.
6. Backend uses layered, feature-oriented packages: `controller`, `service`, `repository`, `model/entity`, `dto`, and `mapper`.
7. Controllers handle HTTP and protocol concerns only; domain logic belongs in services; data persistence belongs in repositories.
8. Never expose JPA entities directly from API controllers; use typed request/response DTOs.
9. Use Flyway database migrations as the single source of truth for physical database schemas. Hibernate auto-ddl (`ddl-auto: update`) is strictly barred in production.
10. Validate all input at API boundaries; enforce authorization in backend service/security layers.
11. Frontend route guards improve UX but are never a substitute for backend authorization.
12. Zero secrets in source control: API keys, database credentials, JWT secrets, and payment keys must use environment variables.
13. Maintain consistent API response envelopes and global exception handling with correlation IDs.
14. Maintain documentation integrity: Preserve all existing comments and docstrings that are unrelated to your code changes.

---

## 3. Mandatory Domain Invariants

### 3.1 Inventory, Ownership & Custody Invariants
- A user may hold multiple additive roles (`CUSTOMER`, `VEHICLE_CONTRIBUTOR`, `RENTAL_PARTNER`). `ADMIN` assignment is strictly controlled by trusted internal administrative processes.
- A partner can independently register and operate its own fleet without contributor submissions.
- A contributor vehicle requires verified owner documentation and an accepted `contribution_agreements` contract before publication.
- A vehicle can have only **one active operational manager partner** at a time.
- Physical vehicle custody is modeled **independently of vehicle legal ownership and booking status**:
  - Partner-owned vehicles begin in `PARTNER_CUSTODY`.
  - Contributor-owned vehicles begin in `CONTRIBUTOR_CUSTODY`.
  - The system must explicitly support all 8 canonical custody permutations: contributor-to-partner, partner-to-customer, contributor-to-customer (owner-managed), customer-to-partner, customer-to-contributor (owner-managed return), partner-to-contributor, inter-hub transfer, and logistics agent relocation.

### 3.2 Booking Hold & Concurrency Invariants
- A vehicle cannot have overlapping confirmed bookings or active holds. All operations modifying availability must acquire a pessimistic row lock:
  ```sql
  SELECT id FROM vehicles WHERE id = :id FOR UPDATE;
  ```
- Turnaround buffers (`turnaround_buffer_minutes`) must be enforced before and after every booking and maintenance blackout block.
- **Hold Independence:** A booking hold (`booking_holds`) is a temporary 15-minute slot reservation. **Creating a booking hold must NOT create an incomplete or draft record in the `bookings` table.**
- **Delayed Webhook Recovery:** If a successful payment webhook arrives after a hold has expired and the requested interval has been reserved by another customer, the platform must **automatically trigger a 100% gateway refund**.

### 3.3 Financial & Contributor Settlement Invariants
- **Contributor Settlement (Approach A Baseline):** The rental partner is the sole contractual debtor for contributor fixed earnings. Under Approach A, the partner settles the contributor directly via bank transfer (NEFT/IMPS/UPI) outside the platform; the platform calculates payables, records obligations, allows partner UTR recording, tracks status, enables contributor confirmation or dispute, and provides admin audit.
- The platform must **never silently assume debtor liability** for partner obligations.
- **Approach B (Razorpay Route):** Remains a future enhancement marked **PENDING** partner onboarding, KYC workflows, and RBI payment aggregator regulatory review.
- **Contributor Price Floor:** Partner-configured listing rates for contributor vehicles must enforce the mathematical profitability floor:
  $$\text{Base Rental Price} \ge \frac{\text{Contributor Fixed Fee} + \text{Partner Min Margin} + \text{Allocable Fixed Costs}}{1 - (\text{Commission Rate} + \text{Gateway Fee Rate} + \text{Effective Tax Rate})}$$
- **No Hardcoded GST Deductions:** Tax calculations must never be hardcoded as a simple percentage deduction from rental revenue. Taxable rental amount, platform commission, gateway fees, contributor compensation, partner margin, and discount funding must be cleanly separated. Tax rules are configurable and marked pending professional validation.
- Promotional discounts below the floor require an authorized override that explicitly debits `partner_discount_subsidy` from partner earnings, keeping contributor compensation intact.
- Confirmed customer bookings strictly supersede contributor withdrawal notices. Vehicle handover back to the contributor occurs only after the final confirmed booking ends.
- Cancelled bookings do not generate contributor payouts unless the signed agreement explicitly specifies a `cancellation_compensation_rate`.

### 3.4 Security Deposit & Dispute Invariants
- Security deposits are collected upfront as separate line items and recorded as **refundable customer liabilities**, never as platform revenue, taxable turnover, or escrow.
- Deductions from security deposits require documented inspection evidence (photos, odometer, fuel readings) and approved reason codes.
- Undisputed portions of a security deposit must be refunded to the customer immediately.
- **No Automatic Approval:** **A disputed damage claim is NEVER automatically approved merely because a 48-hour response window expires.** Disputed claims require Admin arbitration; non-responsive claims require Admin verification before deduction.

### 3.5 Operational vs. Financial Decoupling
- Operational booking fulfillment (vehicle pickup, rental execution, return inspection, operational completion) is strictly decoupled from financial settlement (deposit release, damage disputes, partner disbursements, contributor payouts).
- An operationally returned vehicle must transition to **`COMPLETED` immediately** upon return inspection sign-off to unblock fleet operations and next rentals.

### 3.6 Architecture & Accounting Invariants
- Cross-module side effects must use Spring Application Events (`@TransactionalEventListener(AFTER_COMMIT)`) and the Transactional Outbox Pattern (`outbox_events`).
- Financial movements must be recorded in an append-only double-entry ledger (`ledger_entries`) where every transaction group balances to zero ($\sum \text{Debits} = \sum \text{Credits}$). Corrections must use reversal entries.
- The confirmed pre-freeze baseline consists of **52 proposed physical tables** (50 application-domain + 2 infrastructure). No migrations or actual database schemas have been generated yet.

---

## 4. Configurable Operational Defaults (NOT Hardcoded)

The following operational values are configurable defaults, never hardcoded into application code:
- Booking hold duration: **15 minutes**.
- Car turnaround buffer: **60 minutes**.
- Bike turnaround buffer: **30 minutes**.
- Scooter turnaround buffer: **30 minutes**.
- Contributor settlement mode: **Direct partner payment (Approach A)**.
- Vehicle withdrawal notice: **Recommended 7 days**, subject to confirmed customer bookings.
- Deposit dispute response window: **48 hours**.
- Overdue return grace period: **30 minutes**.
