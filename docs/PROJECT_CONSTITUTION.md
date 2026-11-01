# Project Constitution (Freeze Baseline)

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh (Expandable)  
**Status:** Database Design Freeze Candidate Baseline (47 Physical Tables: 45 MVP Domain + 2 Infra, 8 Future-Phase)  

This document is the governing contract for all human contributors, engineering leads, and AI coding agents. Read it before making changes.

---

## 1. Product Principles

- **Production-Oriented Architecture:** Build a robust, enterprise-grade vehicle rental marketplace, not a disposable prototype.
- **Dual Inventory Channels:** Preserve both inventory channels: partner-owned fleet and contributor-owned vehicles.
- **Unified Customer Experience:** Customer search, checkout, booking, and return workflows must remain seamless and identical across both inventory sources.
- **Separation of Concerns:** Model legal ownership, operational manager assignment, physical custody, and financial settlement as independent, decoupled concepts.
- **Marketplace Neutrality:** The platform is a technology facilitator. The rental partner is the sole contractual debtor for contributor fixed earnings. Direct payments by partners to contributors are never represented as platform cash disbursements.
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
- **Partner Verification Gate:** Pending partner applicants (`partner_profiles.verification_status IN ('SUBMITTED', 'IN_REVIEW')`) must **never receive full partner privileges**. Only formally verified partners may manage rental inventory or accept contributed vehicles.
- **Legal Ownership Combinations:** The vehicle catalog explicitly supports:
  1. Individual person legal owner (`legal_owner_user_id` FK $\rightarrow$ `users.id`).
  2. Partner business legal owner (`legal_owner_partner_id` FK $\rightarrow$ `partner_profiles.id`).
  3. Separate operational rental manager (`manager_partner_id` FK $\rightarrow$ `partner_profiles.id`).
  - Database check constraints enforce that an individual-owned vehicle has `legal_owner_user_id` and a partner-business vehicle has `legal_owner_partner_id`.
- A vehicle can have only **one active operational manager partner** at a time.
- Physical vehicle custody is modeled **independently of vehicle legal ownership and booking status**:
  - Partner-owned vehicles begin in `PARTNER_CUSTODY`.
  - Contributor-owned vehicles begin in `CONTRIBUTOR_CUSTODY`.
  - Custody events record previous custodian, receiving custodian, handover actor, authorized representative, and location via **explicit typed foreign keys**, barring unvalidated polymorphic references.
  - The system must explicitly support all 8 canonical custody permutations: contributor-to-partner, partner-to-customer, contributor-to-customer (owner-managed), customer-to-partner, customer-to-contributor (owner-managed return), partner-to-contributor, inter-hub transfer, and logistics agent relocation.

### 3.2 Booking Hold & Concurrency Invariants
- A vehicle cannot have overlapping confirmed bookings or active holds. All operations modifying availability must acquire a pessimistic row lock:
  ```sql
  SELECT id FROM vehicles WHERE id = :id FOR UPDATE;
  ```
### 3.2 Booking Hold & Partner-Direct Payment Invariants
- A vehicle cannot have overlapping confirmed bookings or active holds. All operations modifying availability must acquire a pessimistic row lock:
  ```sql
  SELECT id FROM vehicles WHERE id = :id FOR UPDATE;
  ```
- Turnaround buffers (`turnaround_buffer_minutes`) must be enforced before and after every booking and maintenance blackout block.
- **Hold Independence:** A booking hold (`booking_holds`) is a temporary reservation lock (configurable default: 15 minutes). **Creating a booking hold must NOT create an incomplete or draft record in the `bookings` table.**
- **Partner-Direct Payment Verification Gate:**
  - In the MVP, the customer pays the managing rental partner directly (via partner's UPI QR, UPI ID, external payment link, or cash at pickup if enabled).
  - Customer submits payment reference details creating a `rental_payment_submissions` record in `SUBMITTED_FOR_VERIFICATION` status.
  - **A customer-uploaded screenshot, receipt, or UTR must NEVER automatically mark a payment as successful or confirm a booking.**
  - **ONLY verified partner confirmation** (`POST /api/v1/partner/payment-submissions/{id}/verify`) can transition payment status to `VERIFIED` and convert the hold to a `CONFIRMED` master booking.
  - If the partner rejects the submission or the verification window expires without submission, the hold expires and inventory is freed. Late or unmatched payments are handled through an explicit partner refund exception workflow; the system never silently confirms bookings or retains indefinite inventory locks.
  - If pay-at-pickup is authorized by the partner, the booking may transition to `CONFIRMED` with payment marked `PENDING_AT_PICKUP`, but vehicle handover is strictly barred until payment and deposit collection are verified by the partner.

### 3.3 Financial & Contributor Settlement Invariants (Partner-Direct Model)
- **Zero Platform Cash Handling in MVP:**
  - The platform does **not** collect, hold, escrow, split, or disburse customer rental money or security deposits.
  - The rental partner is the customer-facing rental service provider and direct payee for all bookings on vehicles they manage.
  - The platform does not deduct commission from customer payments in the MVP. Any future platform monetization or subscription model is a separate business flow.
- **Partner Payment Profiles:** Each verified partner maintains a `partner_payment_profiles` record detailing their public-facing payee name, UPI ID, UPI QR code image, accepted payment methods, and pay-at-pickup configuration.
- **Security Deposit Direct Custody:**
  - The rental partner directly receives, holds, and refunds customer security deposits.
  - Security deposits are tracked in `security_deposits` as distinct records from rental payments.
  - The platform monitors deposit obligations, partner damage deduction claims (`deposit_deductions`), and partner-executed direct refunds (`security_deposit_refunds`), but does not execute external bank transfers.
  - **No Automatic Approval:** A customer non-response does **never** automatically approve a partner's damage deduction claim. Unresolved disputes require Admin review.
- **Contributor Compensation (Approach A Direct Settlement):**
  - The rental partner is the sole legal debtor for the contributor's agreed fixed compensation per eligible completed booking.
  - The partner pays the contributor directly outside the platform via external bank transfer (NEFT/IMPS/UPI).
  - The platform tracks the obligation in `contributor_payables` (lump-sum per booking) with mandatory Bank UTR, date, and receipt proof.
  - Contributor payments generate **zero entries** in any platform general ledger.
- **Future-Phase Marketplace Gateway Boundary:**
  - Integration with an approved marketplace payment provider (e.g. Razorpay Route) for automated collection, linked partner accounts, split disbursements, and platform general ledger accounting is deferred to a future phase, subject to commercial, legal, and compliance gates.

### 3.4 Operational vs. Financial Decoupling
- Operational booking fulfillment (vehicle pickup, rental execution, return inspection, operational completion) is strictly decoupled from financial settlement (deposit refund, damage dispute resolution, contributor payout).
- An operationally returned vehicle must transition to **`COMPLETED` immediately** upon return inspection sign-off to unblock fleet operations and next rentals.

### 3.5 Architecture & Physical Schema Baseline
- Cross-module side effects must use Spring Application Events (`@TransactionalEventListener(AFTER_COMMIT)`) and the Transactional Outbox Pattern (`outbox_events`).
- **Physical Schema Baseline:** Exactly **47 physical MySQL tables for MVP** (45 application-domain tables + 2 infrastructure-managed tables: `shedlock`, `flyway_schema_history`). An additional 8 tables are designated as Future-Phase Marketplace Gateway specifications (55 total cataloged entities).
- No migrations, JPA entities, or actual database schemas have been generated yet.

---

## 4. Configurable Operational Defaults (NOT Hardcoded)

The following operational values are configurable defaults, never hardcoded into application code:
- Booking hold duration: **15 minutes**.
- Payment verification window: **60 minutes** (configurable period for partner to verify submitted UTR).
- Car turnaround buffer: **60 minutes**.
- Bike turnaround buffer: **30 minutes**.
- Scooter turnaround buffer: **30 minutes**.
- Contributor settlement mode: **Direct partner payment (Approach A)**.
- Vehicle withdrawal notice: **Recommended 7 days**, subject to confirmed customer bookings.
- Deposit dispute response window: **48 hours**.
- Overdue return grace period: **30 minutes**.
- Overdue return grace period: **30 minutes**.
