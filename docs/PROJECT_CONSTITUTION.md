# Project Constitution (Freeze Baseline)

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh (Expandable)  
**Status:** Database Design Freeze Candidate Baseline (48 Physical Tables: 46 MVP Domain + 2 Infra, 8 Future-Phase)  

This document is the governing contract for all human contributors, engineering leads, and AI coding agents. Read it before making changes.

---

## 1. Product Principles

- **Production-Oriented Architecture:** Build a robust, enterprise-grade vehicle rental marketplace, not a disposable prototype.
- **Dual Inventory Channels:** Preserve both inventory channels: partner-owned fleet and contributor-owned vehicles.
- **Unified Customer Experience:** Customer search, checkout, booking, and return workflows must remain seamless and identical across both inventory sources.
- **Separation of Concerns:** Model legal ownership, operational manager assignment, physical custody, and financial settlement as independent, decoupled concepts.
- **Marketplace Neutrality:** The platform is a technology facilitator. The rental partner is the sole contractual debtor for contributor fixed earnings. Direct payments by partners to contributors are never represented as platform cash disbursements.
- **Configurability:** Launch in Vijayawada, AP, with configurable city, turnaround, pricing, cancellation, and tax parameters to support rapid regional expansion.

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

### 3.2 Booking Hold, Concurrency & Decoupled Payment State Machines
- A vehicle cannot have overlapping confirmed bookings or active holds. All operations modifying availability must acquire a pessimistic row lock:
  ```sql
  SELECT id FROM vehicles WHERE id = :id FOR UPDATE;
  ```
- Turnaround buffers (`turnaround_buffer_minutes`) must be enforced before and after every booking and maintenance blackout block.
- **Hold Independence:** A booking hold (`booking_holds`) is a temporary reservation lock (configurable default: 15 minutes). **Creating a booking hold must NOT create an incomplete or draft record in the `bookings` table.**
- **Decoupled Payment Demand & Submission Lifecycles:**
  - A payment demand (`rental_payment_demands`) represents the total amount owed by the customer for a hold, extension, or invoice. Its lifecycle is governed by financial demand states: `OPEN`, `PARTIALLY_SATISFIED`, `SATISFIED`, `EXPIRED`, and `CANCELLED`.
  - A payment submission (`rental_payment_submissions`) represents an individual external transfer or cash transaction submitted by the customer. Its lifecycle is: `SUBMITTED_FOR_VERIFICATION`, `VERIFIED`, and `REJECTED`.
  - Multiple submissions against a single demand are fully supported. Verified partial payments accumulate in `rental_payment_demands.total_verified_submitted` and apply to `amount_satisfied` (strictly capped at `amount_demanded`). Top-up payments can be submitted by the customer while the hold is active until the demand is `SATISFIED`.
  - **Overpayment Accounting:** For a ₹1,800 verified payment against a ₹1,500 demand, the system records `total_verified_submitted = 1800.00`, `amount_satisfied = 1500.00`, and `excess_amount = 300.00`. The system **never** stores ₹1,800 as satisfying a ₹1,500 obligation. The excess ₹300 automatically triggers an `OVERPAYMENT_RETURN` refund record in `rental_fee_refunds`.
  - **Transactional Aggregate Refund Limit Validation:** Cumulative rental-fee refunds are validated transactionally under a pessimistic row lock (`SELECT ... FOR UPDATE` on parent demand). A new refund is rejected if `new_refund_amount > (gross_refundable_pool - cumulative_committed_refunds)`. Canceled/rejected refunds do not consume balance.
- **Anti-Auto-Verification Invariant:**
  - **A customer-uploaded screenshot, receipt, or UTR must NEVER automatically mark a payment as successful or confirm a booking.**
  - **ONLY verified partner confirmation** (`POST /api/v1/partner/payment-submissions/{id}/verify`) can transition submission status to `VERIFIED` and accumulate toward demand satisfaction.
- **Booking Confirmation Invariant:**
  A booking can be confirmed ONLY when:
  1. Availability and vehicle assignment are valid under an exclusive vehicle row lock.
  2. The applicable rental payment demands are `SATISFIED` through verified submissions, OR an explicit partner-approved pay-at-pickup policy permits the required unpaid amount.
  3. Any required security-deposit policy is satisfied.
  4. The hold is still valid (`status = 'ACTIVE'` or `status = 'VERIFICATION_PENDING'`) or safely revalidated under the vehicle lock.
  - **An expired hold must NEVER be silently converted to a confirmed booking.**
  - For pay-at-pickup bookings, the outstanding obligation is recorded in `rental_payment_demands`, and **physical vehicle handover is strictly blocked** until required collections are verified by the partner at pickup.
- **Correct Late Payment Recovery After Hold Expiry:**
  - The original hold and demand become permanently immutable in `EXPIRED`.
  - If a payment arrives after expiry and is confirmed by the partner, the submission is recorded as `VERIFIED` with audit notes. The expired demand is **never reopened**.
  - Availability is checked under `SELECT FOR UPDATE`: if available, a fresh hold (`recovered_from_hold_id`) and fresh demand are generated, the verified payment is applied to the new demand, and the booking is confirmed; if unavailable, a `rental_fee_refunds` obligation (`EXPIRED_HOLD_EXCEPTION`) is created for partner external return.

### 3.3 Financial, Refund & Contributor Settlement Invariants (Partner-Direct Model)
- **Zero Platform Cash Handling in MVP:**
  - The platform does **not** collect, hold, escrow, split, or disburse customer rental money or security deposits.
  - The rental partner is the customer-facing rental service provider and direct payee for all bookings on vehicles they manage.
  - The platform does not deduct commission from customer payments in the MVP. Any future platform monetization or subscription model is a separate business flow.
- **Partner Payment Profiles:** Each verified partner maintains a `partner_payment_profiles` record detailing their public-facing payee name, UPI ID, UPI QR code image, accepted payment methods, and pay-at-pickup configuration.
- **Refund Acknowledgement Semantics & States (`rental_fee_refunds`, `security_deposit_refunds`):**
  - Both refund state machines enforce distinct states:
    1. `PENDING_PARTNER_DISBURSEMENT`
    2. `DISBURSED_BY_PARTNER`
    3. `CUSTOMER_ACKNOWLEDGED`
    4. `DISPUTED_BY_CUSTOMER`
    5. `DISPUTE_WINDOW_ELAPSED_UNACKNOWLEDGED`
    6. `ADMIN_RESOLVED`
  - **Explicit Acknowledgement Rule:** An explicit customer acknowledgement in the UI is the **ONLY** event that establishes customer-confirmed receipt (`CUSTOMER_ACKNOWLEDGED`).
  - **Non-Equivalence of Window Expiry:** Expiry of the 72-hour dispute window transitions the refund to `DISPUTE_WINDOW_ELAPSED_UNACKNOWLEDGED`. It must **never** be represented as customer acknowledgement.
  - **Dispute Arbitration:** Disputed refunds escalate to Admin arbitration via an explicit support ticket reference (`dispute_ticket_id` FK $\rightarrow$ `support_tickets.id`) and record the deciding Admin (`resolved_by_admin_user_id` FK $\rightarrow$ `users.id`), resolution notes, and timestamp upon transitioning to `ADMIN_RESOLVED`.
- **Configurable Cancellation Policies:** Cancellation refund amounts are determined strictly from configurable business rules snapshot in `cancellation_policy_snapshot`—percentages and fee tiers are never hardcoded in application logic.
- **Direct External Transfer:** The partner transfers refund money directly outside the platform via UPI/IMPS/NEFT. The platform records transaction references, proof, and timestamps.
- **Security Deposit Direct Custody & Evidence-Based Deductions:**
  - The rental partner directly receives, holds, and refunds customer security deposits as distinct records from rental fees.
  - Granular damage deduction claims require photographic inspection evidence and reference `support_tickets` and deciding Admin upon arbitration.
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
- **Physical Schema Baseline:** Exactly **48 physical MySQL tables for MVP** (46 application-domain tables + 2 infrastructure-managed tables: `shedlock`, `flyway_schema_history`). An additional 8 tables are designated as Future-Phase Marketplace Gateway specifications (56 total cataloged entities).
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
- Customer refund acknowledgement window: **72 hours**.
- Overdue return grace period: **30 minutes**.
