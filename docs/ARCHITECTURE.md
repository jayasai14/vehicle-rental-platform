# System Architecture (Freeze Baseline)

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh (Expandable)  
**Status:** Database Design Freeze Candidate Baseline (47 Physical Tables: 45 MVP Domain + 2 Infra, 8 Future-Phase)  

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
├── contribution/            # Contributor applications, agreements, withdrawal requests
├── booking/                 # Bookings, booking_holds, extensions, cancellation
├── inspection/              # Handover/return inspections, incidents, inspection_media
├── payment/                 # Partner payment profiles, rental payment demands, customer submissions, verification
├── deposit/                 # Security deposits, damage deductions, partner direct refunds
├── contribution/            # Contributor applications, agreements, withdrawal requests, contributor_payables
├── booking/                 # Bookings, booking_holds, extensions, cancellation
├── inspection/              # Handover/return inspections, incidents, inspection_media
├── review/
├── support/
└── notification/
```

---

## 3. Core Module Boundaries & Ownership (45 MVP Domain Tables + 2 Infra = 47 Total)

| Module | Physical Tables Owned | Core Domain Responsibilities |
|---|---|---|
| **auth & user** | `users`, `roles`, `user_roles`, `refresh_tokens`, `addresses`, `customer_profiles`, `partner_profiles`, `partner_documents`, `contributor_profiles`, `driving_licences`, `kyc_verifications` (11) | User identity, registration, JWT issuance/rotation, profile management, partner commercial documents, unified addresses, driving licence & KYC verifications. |
| **location** | `cities`, `partner_hubs`, `pickup_locations` (3) | Geographic operating zones, partner physical service hubs, pickup and return points. |
| **vehicle** | `vehicles`, `vehicle_documents`, `vehicle_pricing_profiles`, `vehicle_images`, `vehicle_features`, `vehicle_maintenance_blocks` (6) | Fleet catalog (individual & partner business legal owners, operational manager), compliance documents (RC, insurance), pricing profiles, photos, feature tags, maintenance blocks. |
| **custody** | `vehicle_custody_events` (1) | Generic vehicle custody tracking ledger supporting all 8 canonical physical handover permutations. |
| **contribution** | `contributor_vehicle_applications`, `contribution_agreements`, `contribution_agreement_events`, `vehicle_withdrawal_requests` (4) | Contributor vehicle submissions, bilateral agreements, fixed payout terms, lifecycle audit events, withdrawal requests. |
| **booking & inspection** | `booking_holds`, `bookings`, `booking_status_events`, `booking_extensions`, `inspections`, `inspection_media`, `incidents` (7) | 15-minute temporary reservation locks, master booking contracts, extensions, pre/post inspection logs, condition media, damage incidents. |
| **payment & settlement** | `partner_payment_profiles`, `rental_payment_demands`, `rental_payment_submissions`, `security_deposits`, `deposit_deductions`, `security_deposit_refunds`, `contributor_payables` (7) | Partner payment profiles (UPI QR/ID/Link/Cash), payment demands, customer UTR submissions, manual partner verification, deposit custody, evidence-based deductions, direct partner refunds, contributor payable tracking. |
| **support & communication** | `reviews`, `support_tickets`, `support_ticket_messages`, `notifications` (4) | Booking-bound customer reviews, support ticketing, in-app/SMS/email notifications. |
| **common / infrastructure** | `outbox_events`, `audit_logs`, `shedlock`, `flyway_schema_history` (2 domain + 2 infra = 4) | Transactional outbox engine, security audit logs, ShedLock distributed locks, Flyway schema history. |

> [!NOTE]
> **Future-Phase Marketplace Gateway Module (8 Tables):** `payment_webhook_events`, `partner_commissions`, `payout_methods`, `settlements`, `ledger_accounts`, `ledger_transactions`, `ledger_entries`, `platform_commission_rules` are formally documented as Future-Phase specifications to be activated upon commercial marketplace gateway onboarding.

---

## 4. Decoupled Inter-Module Communication

1. **Spring Application Events:**
   - Modules publish strongly-typed domain events (e.g., `BookingConfirmedEvent`, `PaymentSubmittedEvent`, `PaymentVerifiedEvent`, `InspectionCompletedEvent`) via `ApplicationEventPublisher`.
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
2. **Hold Isolation & Partner Verification Protocol:**
   - Creating a hold inserts into `booking_holds`; it **does NOT create an incomplete record in `bookings`**.
   - Submitting payment details inserts into `rental_payment_submissions` (`SUBMITTED_FOR_VERIFICATION`) and preserves the hold lock during the verification window.
   - Only partner verification converts the hold and creates a `CONFIRMED` booking. Unverified submissions never confirm bookings.
     - If the vehicle interval remains completely available, the system recovers and confirms the booking.
     - If the interval has been reserved by another customer in the interim, the platform **automatically triggers a 100% gateway refund**.

---

## 6. Financial Architecture & Contributor Settlement

1. **Approach A (Direct Partner Settlement - Recommended MVP Baseline):**
   - The rental partner is the sole legal debtor for contributor fixed earnings.
   - The platform calculates the payable, records the obligation against the partner in `contributor_payables`, provides an interface for the partner to record payments with bank UTR numbers, tracks settlement status, allows contributor confirmation or dispute, and provides administrative oversight.
   - Direct payments by partners to contributors are **never represented as cash movements through the platform general ledger**.
   - The platform never holds or routes third-party contributor funds and assumes zero debtor liability.
2. **Approach B (Razorpay Route - Future Option PENDING Validation):**
   - Payment gateway automatically splits incoming rental proceeds directly to contributor bank accounts.
   - Marked **PENDING** partner linked-account onboarding, KYC workflows, and RBI payment aggregator regulatory review.
3. **Double-Entry Append-Only General Ledger:**
   - Master accounts (`ledger_accounts`), journal headers (`ledger_transactions`), and line items (`ledger_entries`) record actual platform cash, customer refundable liabilities, platform commission revenue, and net partner payables.
   - Every transaction group strictly balances ($\sum \text{Debits} = \sum \text{Credits}$).
   - Balances are derived projections; corrections use reversal entries.

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
- Security deposit refund, deduction, or dispute operates independently on `security_deposits` and `deposit_deductions`.
- Contributor payable eligibility, scheduling, and settlement operate independently on `contributor_payables`.
- Delayed or disputed refunds or payouts never block operational booking completion or vehicle re-listing.
