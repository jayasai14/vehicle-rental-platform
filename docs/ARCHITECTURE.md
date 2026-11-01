# System Architecture (Pre-Freeze Specification)

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh (Expandable)  
**Status:** Pre-Freeze Corrected Baseline  

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
├── payment/                 # Orders, transactions, security_deposits, deposit_deductions, refunds, webhooks
├── payout/                  # Payout_methods, contributor_payables, partner_commissions, settlements, ledger
├── review/
├── support/
└── notification/
```

---

## 3. Core Module Boundaries & Ownership

| Module | Physical Tables Owned | Core Domain Responsibilities |
|---|---|---|
| **auth** | `refresh_tokens` | Registration, login, JWT issuance, token rotation/revocation, password reset. |
| **user** | `users`, `roles`, `user_roles`, `addresses`, `customer_profiles`, `contributor_profiles`, `identity_verifications`, `driver_licences` | User identity, profile management, additive role grants, unified addresses, KYC verification logs. |
| **partner** | `partner_profiles`, `partner_documents`, `partner_locations`, `delivery_zones` | Partner business onboarding, compliance documents, physical partner hubs, doorstep delivery zones. |
| **vehicle** | `vehicles`, `vehicle_documents`, `vehicle_images`, `vehicle_pricing`, `pickup_locations`, `vehicle_availability_blocks`, `vehicle_custody_events`, `cities` | Fleet catalog, compliance documents, pricing schedules, turnaround buffers, blackout blocks, and the 8-permutation custody event ledger. |
| **contribution** | `vehicle_contribution_applications`, `contribution_agreements`, `contribution_agreement_events`, `vehicle_withdrawal_requests` | Contributor onboarding, bilateral agreements, fixed-fee terms, immutable versioning, and withdrawal requests. |
| **booking** | `booking_holds`, `bookings`, `booking_status_events`, `booking_extensions` | 15-minute reservation locks (pessimistic `SELECT FOR UPDATE`), master booking contracts, extensions, cancellations, and operational status events. |
| **inspection** | `inspections`, `inspection_media`, `incidents` | Pre-rental and post-rental vehicle condition reports, photographic evidence, damage and breakdown incident claims. |
| **payment** | `payment_orders`, `payment_transactions`, `security_deposits`, `deposit_deductions`, `payment_webhook_events`, `refunds` | Payment gateway orders, transaction attempts, refundable security deposit liabilities, dispute arbitration, idempotent webhooks, and retryable refunds. |
| **payout** | `contributor_payables`, `partner_commissions`, `payout_methods`, `settlements`, `ledger_entries`, `platform_commission_rules` | Commission rules, contributor payables tracking (Approach A baseline), partner disbursement batches, tokenized payout methods, and append-only double-entry ledger. |
| **review** | `reviews` | Booking-bound customer ratings and reviews. |
| **support** | `support_tickets`, `support_ticket_messages` | Threaded customer, partner, and contributor support and dispute tickets. |
| **notification**| `notifications` | Transactional notifications across in-app, SMS, and email channels. |
| **common/outbox**| `outbox_events`, `audit_logs`, `shedlock`, `flyway_schema_history` | Transactional outbox engine, security audit logs, distributed locks, and migration tracking. |

---

## 4. Decoupled Inter-Module Communication

1. **Spring Application Events:**
   - Modules publish strongly-typed domain events (e.g., `BookingConfirmedEvent`, `InspectionCompletedEvent`, `DamageClaimSubmittedEvent`) via `ApplicationEventPublisher`.
   - Dependent modules consume events using `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` to decouple synchronous dependencies and prevent distributed transaction failure loops.
2. **Transactional Outbox Pattern:**
   - External network interactions (Razorpay payment creation/refunds, Cloudinary media processing, email/SMS dispatch) must NOT execute directly inside database transaction boundaries.
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
2. **Hold Independence & Delayed Webhook Safety:**
   - Creating a hold inserts into `booking_holds`; it **does NOT create an incomplete record in `bookings`**.
   - If a successful payment webhook arrives after a hold has expired and the interval has been reserved by another customer, the platform **automatically triggers a 100% gateway refund**.

---

## 6. Financial Architecture & Contributor Settlement

1. **Approach A (Direct Partner Settlement - Recommended MVP Baseline):**
   - The rental partner is the sole legal debtor for contributor fixed earnings.
   - The platform calculates the payable, records the obligation against the partner, provides an interface for the partner to record payments with bank UTR numbers, tracks settlement status, allows contributor confirmation or dispute, and provides administrative oversight.
   - The platform never holds or routes third-party contributor funds and assumes zero debtor liability.
2. **Approach B (Razorpay Route - Future Option PENDING Validation):**
   - Payment gateway automatically splits incoming rental proceeds directly to contributor bank accounts.
   - Marked **PENDING** partner linked-account onboarding, KYC workflows, and RBI payment aggregator regulatory review.
3. **Double-Entry Append-Only Ledger:**
   - All monetary movements generate balancing debit and credit entries (`ledger_entries`) tied to an identifiable `transaction_group_id`.
   - Account balances are derived projections; corrections use reversal entries.
   - Customer security deposits are tracked as refundable liabilities, never as platform revenue or escrow.

---

## 7. Vehicle Custody Architecture

1. **Ownership-Independent Modeling:**
   - Legal ownership (`legal_owner_user_id`), operational manager (`manager_partner_id`), and physical custody (`vehicle_custody_events`) are modeled independently.
2. **Initial Custody States:**
   - Partner-owned vehicles begin in `PARTNER_CUSTODY`.
   - Contributor-owned vehicles begin in `CONTRIBUTOR_CUSTODY`.
3. **Supported Permutations:**
   - Tracks all 8 canonical handover permutations: contributor-to-partner, partner-to-customer, contributor-to-customer (owner-managed), customer-to-partner, customer-to-contributor (owner-managed return), partner-to-contributor, inter-hub transfer, and logistics agent relocation.

---

## 8. Decoupling Operational Completion from Financial Settlement

- A booking reaches operational completion (`COMPLETED`) as soon as the vehicle is returned and the post-rental inspection is signed.
- Security deposit refund, deduction, or dispute operates independently on `security_deposits` and `deposit_deductions`.
- Contributor payable eligibility, scheduling, and settlement operate independently on `contributor_payables`.
- Delayed or disputed refunds or payouts never block operational booking completion or vehicle re-listing.
