# System Architecture (Freeze Baseline)

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh (Expandable)  
**Status:** Database Design Freeze Candidate Baseline (48 Physical Tables: 46 MVP Domain + 2 Infra, 8 Future-Phase)  

---

## 1. Architectural Style

Start with a **modular monolith**: one Spring Boot deployable, one MySQL database, and a separate React application. Organize backend code by business capability with clear module boundaries. Do not introduce microservices, service discovery, or distributed infrastructure without a demonstrated need.

---

## 2. Repository Layout

```text
vehicle-rental-platform/
├── frontend/
├── backend/
├── docs/
├── .gitignore
└── README.md
```

### Frontend Layout (Target)

```text
frontend/src/
├── app/                 # app providers and bootstrap
├── assets/
├── components/
│   ├── common/
│   ├── navigation/
│   ├── vehicles/
│   ├── booking/
│   └── forms/
├── features/
│   ├── auth/
│   ├── marketplace/
│   ├── customer/
│   ├── contributor/
│   ├── partner/
│   ├── admin/
│   ├── bookings/
│   ├── vehicles/
│   └── notifications/
├── layouts/
├── routes/
├── services/            # shared API client/interceptors only
├── hooks/
├── context/
├── utils/
└── main.jsx
```

Feature-specific components, hooks, and API functions live inside their respective feature folder. Use React Router with route guards for client UX and Spring Security `@PreAuthorize` method security for authoritative enforcement.

### Backend Layout (Target)

Root package: `com.vehiclerental`

```text
backend/src/main/java/com/vehiclerental/
├── VehicleRentalApplication.java
├── config/
│   ├── SchedulingConfig.java
│   └── AsyncConfig.java
├── security/
│   ├── jwt/
│   ├── filter/
│   └── principal/
├── common/
│   ├── api/
│   ├── exception/
│   ├── validation/
│   ├── audit/
│   ├── event/               # Spring Application Event definitions
│   ├── outbox/              # Transactional Outbox entity, repo, and dispatcher
│   └── scheduling/          # ShedLock configuration
├── auth/
├── user/
├── partner/                 # Partner profiles, partner_locations (hubs), delivery_zones
├── vehicle/                 # Vehicles, vehicle_custody_events, documents, pricing, buffers
├── contribution/            # Contributor applications, agreements, withdrawal requests, contributor_payables
├── booking/                 # Bookings, booking_holds, extensions, cancellation
├── inspection/              # Handover/return inspections, incidents, inspection_media
├── payment/                 # Partner payment profiles, rental payment demands, customer submissions, rental fee refunds, verification
├── deposit/                 # Security deposits, damage deductions, security deposit refunds
├── review/
├── support/
└── notification/
```

---

## 3. Core Module Boundaries & Ownership (46 MVP Domain Tables + 2 Infra = 48 Total)

| Module | Physical Tables Owned | Core Domain Responsibilities |
|---|---|---|
| **auth & user** | `users`, `roles`, `user_roles`, `refresh_tokens`, `addresses`, `customer_profiles`, `partner_profiles`, `partner_documents`, `contributor_profiles`, `driving_licences`, `kyc_verifications` (11) | User identity, registration, JWT issuance/rotation, profile management, partner commercial documents, unified addresses, driving licence & KYC verifications. |
| **location** | `cities`, `partner_hubs`, `pickup_locations` (3) | Geographic operating zones, partner physical service hubs, pickup and return points. |
| **vehicle** | `vehicles`, `vehicle_documents`, `vehicle_pricing_profiles`, `vehicle_images`, `vehicle_features`, `vehicle_maintenance_blocks` (6) | Fleet catalog (individual & partner business legal owners, operational manager), compliance documents (RC, insurance), pricing profiles, photos, feature tags, maintenance blocks. |
| **custody** | `vehicle_custody_events` (1) | Generic vehicle custody tracking ledger supporting all 8 canonical physical handover permutations. |
| **contribution** | `contributor_vehicle_applications`, `contribution_agreements`, `contribution_agreement_events`, `vehicle_withdrawal_requests` (4) | Contributor vehicle submissions, bilateral agreements, fixed payout terms, lifecycle audit events, withdrawal requests. |
| **booking & inspection** | `booking_holds`, `bookings`, `booking_status_events`, `booking_extensions`, `inspections`, `inspection_media`, `incidents` (7) | 15-minute temporary reservation locks, master booking contracts, extensions, pre/post inspection logs, condition media, damage incidents. |
| **payment & settlement** | `partner_payment_profiles`, `rental_payment_demands`, `rental_payment_submissions`, `rental_fee_refunds`, `security_deposits`, `deposit_deductions`, `security_deposit_refunds`, `contributor_payables` (8) | Partner payment profiles (UPI QR/ID/Link/Cash), payment demands, customer UTR submissions, manual partner verification, rental fee refunds, deposit custody, evidence-based deductions, direct deposit refunds, contributor payable tracking. |
| **support & communication** | `reviews`, `support_tickets`, `support_ticket_messages`, `notifications` (4) | Booking-bound customer reviews, support ticketing, in-app/SMS/email notifications. |
| **common / infrastructure** | `outbox_events`, `audit_logs`, `shedlock`, `flyway_schema_history` (2 domain + 2 infra = 4) | Transactional outbox engine, security audit logs, ShedLock distributed locks, Flyway schema history. |

> [!NOTE]
> **Future-Phase Marketplace Gateway Module (8 Tables):** `payment_webhook_events`, `partner_commissions`, `payout_methods`, `settlements`, `ledger_accounts`, `ledger_transactions`, `ledger_entries`, `platform_commission_rules` are formally documented as Future-Phase specifications to be activated upon commercial marketplace gateway onboarding.

---

## 4. Decoupled Inter-Module Communication

1. **Spring Application Events:**
   - Modules publish strongly-typed domain events (e.g., `BookingConfirmedEvent`, `PaymentSubmittedEvent`, `PaymentVerifiedEvent`, `RentalFeeRefundRecordedEvent`, `InspectionCompletedEvent`) via `ApplicationEventPublisher`.
   - Dependent modules consume events using `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` to decouple synchronous dependencies and prevent distributed transaction failure loops.
2. **Transactional Outbox Pattern:**
   - External network interactions (Cloudinary media processing, email/SMS dispatch, external notifications) must NOT execute directly inside database transaction boundaries.
   - The originating service inserts an `outbox_events` record in the same local database transaction as the domain state change.
   - An asynchronous scheduled outbox dispatcher polls pending events, executes the external call with exponential backoff retries, and marks events `PROCESSED` or `FAILED`.
   - Guarantees at-least-once delivery and resilience against external API downtime.

---

## 5. Concurrency Control & State Consistency

1. **Pessimistic Row-Level Locking:**
   - All operations altering vehicle availability (booking hold creation, booking confirmation, extension, maintenance blocking, and custody handover) MUST acquire an exclusive row lock on the target vehicle:
     ```sql
     SELECT id FROM vehicles WHERE id = :vehicleId FOR UPDATE;
     ```
   - Availability validation executes within this locked transaction, verifying zero overlap across confirmed bookings, active unexpired booking holds (`status = 'ACTIVE' AND expires_at > NOW()`), maintenance blocks, and turnaround buffers (`turnaround_buffer_minutes`).
2. **Hold Isolation & Decoupled Payment Verification Protocol:**
   - Creating a hold inserts into `booking_holds`; it **does NOT create an incomplete record in `bookings`**.
   - A payment demand (`rental_payment_demands`) tracks the required financial amount (`OPEN`, `PARTIALLY_SATISFIED`, `SATISFIED`, `EXPIRED`, `CANCELLED`).
   - Customer submissions (`rental_payment_submissions`) record external transfer attempts (`SUBMITTED_FOR_VERIFICATION`, `VERIFIED`, `REJECTED`).
   - Multiple verified submissions accumulate in `rental_payment_demands.total_verified_submitted` and apply to `amount_satisfied` (strictly capped at `amount_demanded`).
   - **Overpayment Accounting:** If verified submissions total ₹1,800 against a ₹1,500 demand, `total_verified_submitted = 1800.00`, `amount_satisfied = 1500.00`, and `excess_amount = 300.00`. The system never stores ₹1,800 as satisfying a ₹1,500 demand.
   - **Aggregate Refund Limit Validation:** Enforced transactionally under `SELECT ... FOR UPDATE` on the parent demand. Active refunds cannot exceed the gross refundable balance.
   - Customer uploads never auto-verify payments; **only verified partner confirmation** transitions submissions to `VERIFIED`.
   - **Booking Confirmation Invariant:** A booking can be confirmed ONLY when:
     1. Vehicle calendar availability is valid under the exclusive row lock.
     2. Applicable payment demands are `SATISFIED` through accumulated verified submissions, OR an explicit partner-approved pay-at-pickup policy permits the required unpaid balance.
     3. Any required security-deposit policy is satisfied.
     4. The booking hold is still valid or safely revalidated under the vehicle lock.
   - **Late Payment Recovery After Hold Expiry:**
     - The original hold and demand remain permanently immutable in `EXPIRED`.
     - When payment arrives after expiry and is confirmed by the partner, the submission is recorded as `VERIFIED`. The expired demand is **never reopened**.
     - If the vehicle is available under `SELECT FOR UPDATE`, a fresh hold (`recovered_from_hold_id`) and fresh demand are generated, applying the verified payment, and the booking is confirmed.
     - If unavailable or recovery cannot be completed, a `rental_fee_refunds` obligation (`EXPIRED_HOLD_EXCEPTION`) is created for partner external return.
   - For pay-at-pickup bookings, physical handover is strictly blocked until the partner verifies required collections at pickup.

---

## 6. Financial Architecture & Contributor Settlement (Partner-Direct Model)

1. **Partner-Direct Collection & Zero Platform Cash:**
   - The managing rental partner receives customer rental payments and security deposits directly outside the platform.
   - The platform never holds, escrows, or transfers customer funds in the MVP.
2. **Approach A (Direct Partner Contributor Settlement - Baseline):**
   - The rental partner is the sole legal debtor for contributor fixed earnings.
   - The platform calculates the payable, records the obligation against the partner in `contributor_payables`, provides an interface for the partner to record payments with bank UTR numbers, tracks settlement status, allows contributor confirmation or dispute, and provides administrative oversight.
   - Direct payments by partners to contributors are **never represented as cash movements through any platform general ledger**.
   - The platform never holds or routes third-party contributor funds and assumes zero debtor liability.
3. **Rental-Fee Refund Tracking (`rental_fee_refunds`):**
   - Direct partner-executed rental fee refunds are audited for cancellations, overpayments, expired-hold exceptions, and disputes.
   - Cancellation refund amounts are determined dynamically from configurable business rules snapshotted at calculation time.
   - **Refund Acknowledgement Semantics:** Separate states enforce that an explicit customer action is the ONLY event establishing customer-confirmed receipt (`CUSTOMER_ACKNOWLEDGED`). Expiry of the 72-hour dispute window transitions to `DISPUTE_WINDOW_ELAPSED_UNACKNOWLEDGED`, never customer acknowledgement. Disputed refunds escalate to Admin arbitration via `support_tickets` (`dispute_ticket_id`) and record deciding Admin (`resolved_by_admin_user_id`), transitioning to `ADMIN_RESOLVED`. Applies identically to `security_deposit_refunds`.
4. **Future-Phase Marketplace Gateway & Append-Only General Ledger:**
   - Automated split settlements (e.g. Razorpay Route), webhook idempotency, platform commission rules, and double-entry general ledger accounts are deferred to a subsequent release following regulatory and compliance approvals.

---

## 7. Vehicle Custody Architecture

1. **Ownership-Independent Modeling:**
   - Legal ownership (individual or partner business), operational manager (`manager_partner_id`), and physical custody (`vehicle_custody_events`) are modeled independently.
2. **Initial Custody States:**
   - Partner-owned vehicles begin in `PARTNER_CUSTODY`.
   - Contributor-owned vehicles begin in `CONTRIBUTOR_CUSTODY`.
3. **Explicit Typed Custodians:**
   - Custody events record previous custodian, receiving custodian, handover actor, authorized representative, and location using explicit relational foreign keys, eliminating unvalidated polymorphic references.
   - Tracks all 8 canonical handover permutations across partner-managed, owner-managed, and logistics representative flows.

---

## 8. Decoupling Operational Completion from Financial Settlement

- A booking reaches operational completion (`COMPLETED`) as soon as the vehicle is returned and the post-rental inspection is signed.
- Security deposit refund, deduction, or dispute operates independently on `security_deposits`, `deposit_deductions`, and `security_deposit_refunds`.
- Rental fee refunds operate independently on `rental_fee_refunds`.
- Contributor payable eligibility, scheduling, and settlement operate independently on `contributor_payables`.
- Delayed or disputed refunds or payouts never block operational booking completion or vehicle re-listing.
