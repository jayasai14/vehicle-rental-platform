# Architecture Decision Records (ADRs) - Freeze Baseline

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh  
**Status:** Database Design Freeze Candidate Baseline (47 Physical Tables: 45 MVP Domain + 2 Infra, 8 Future-Phase)  

> [!IMPORTANT]
> **Pre-Implementation Notice:** All decisions below govern the **proposed architecture and database design**. No application code, JPA entities, SQL DDL scripts, or Flyway migrations have been generated yet. Formal implementation begins only after final Product Owner approval.

---

## ADR-001: Upfront Rental Payment and Refundable Security Deposit Collection (Partner-Direct Custody)

### Context
Vehicle rentals require security deposits to cover potential damage, fuel discrepancies, traffic fines, and overdue returns. In India, retail payment methods (UPI and domestic debit cards) do not universally support multi-day pre-authorization holds without recurring e-mandates.

### Decision
1. **Partner-Direct Collection:** The rental partner who manages the vehicle directly collects the rental charges and refundable security deposit from the customer via the partner's verified UPI QR code, UPI ID, external payment link, or cash at pickup (where enabled).
2. **Refundable Customer Liability in Partner Custody:** The security deposit is collected and held directly by the rental partner as a **refundable customer liability**. It is never collected, escrowed, or held by the platform.
3. **Non-Escrow Designation:** The platform acts strictly as an operational record-keeper and dispute arbitrator; it does not operate an escrow or trust account.
4. **Documented Deductions & Review Process:** Deductions from security deposits are permitted only with approved inspection evidence (photos, odometer, fuel readings).
5. **No Automatic Claim Approval:** **A disputed damage claim is NEVER automatically approved merely because a 48-hour response window expires.** Disputed claims require Admin arbitration. If a customer is non-responsive after 48 hours, an Admin must verify the evidence before any deduction is approved.
6. **Partner Refund Execution:** The rental partner is solely responsible for executing deposit refunds directly to the customer outside the platform. The platform records the refund amount, transaction reference (UTR), date, and evidence.

---

## ADR-002: Relational Schema, Hold Independence, and Verification Locking

### Context
Previous proposals lacked explicit separation between temporary reservations and confirmed bookings, risking dirty reads and database bloat from abandoned checkout sessions. Furthermore, manual payment verification requires preserving inventory locks while the partner verifies the customer's payment.

### Decision
1. **Hold Independence:** `booking_holds` is an independent 15-minute temporary reservation lock acquired under pessimistic `SELECT FOR UPDATE` locking.
2. **No Incomplete Booking Records:** Creating a hold **does NOT insert an incomplete or draft record into the `bookings` table**. The `bookings` table receives records only upon verified payment confirmation.
3. **Verification Window Extension:** When a customer submits payment proof (UTR / receipt) before the 15-minute hold expires, the hold status transitions to `VERIFICATION_PENDING` and its TTL is extended by a configurable verification window (e.g., 60 minutes), preserving inventory locking while the partner verifies the transfer.
4. **Expired Hold Protocol:** If a hold expires without payment submission, or if the partner rejects the payment submission, the hold transitions to `EXPIRED` or `RELEASED`, instantly freeing inventory. If a customer paid late or unmatched after expiration, an explicit partner exception/refund workflow is initiated without auto-confirming the booking.

---

## ADR-003: Contributor Profitability Price Floor Engine & Partner Discount Model

### Context
In contributor-owned vehicles, the contributor is contractually entitled to an agreed fixed payout per completed eligible booking. If a partner discounts the vehicle below the total of contributor payout + taxes + operational expenses, a financial deficit occurs.

### Decision
1. **Profitability Floor Validation:** The backend enforces a minimum listing price for contributor vehicles:
   $$\text{Base Rental Price} \ge \frac{\text{Contributor Fixed Fee} + \text{Partner Min Margin} + \text{Allocable Fixed Costs}}{1 - (\text{Effective Tax \%})}$$
2. **Clean Separation of Components:** The system cleanly separates rental amount, security deposit, fixed contributor compensation, partner margin, and discount funding.
3. **No Hardcoded GST Deductions:** Tax calculations are **not** hardcoded as a simple percentage deduction from rental revenue; they are configurable and marked pending professional tax validation.
4. **Partner-Funded Discount Subsidies:** If a partner offers a promotional rate below the floor, the partner absorbs the promotional discount (`partner_discount_subsidy`) without reducing contributor compensation.

---

## ADR-004: Spring Event-Driven Architecture and Transactional Outbox Pattern

### Context
Direct synchronous inter-module method calls create tight circular coupling and distributed transaction failure loops.

### Decision
1. **Spring Application Events:** Cross-module communication uses strongly-typed domain events published via `ApplicationEventPublisher`.
2. **Transactional Event Listeners:** Downstream modules consume events via `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
3. **Transactional Outbox Pattern:** All external network interactions (Cloudinary uploads, notification dispatch, SMS/Email) write an event record into `outbox_events` in the same local transaction as the domain state change.
4. **Outbox Dispatcher:** A background worker polls pending outbox events, dispatches them with exponential backoff retries, and records processing status.

---

## ADR-005: Pessimistic Concurrency Control for Booking and Hold Allocation

### Context
Under concurrent checkout requests, multiple transactions checking availability simultaneously can see zero conflicts and insert overlapping reservations.

### Decision
1. **Pessimistic Row-Level Lock:** All operations altering vehicle availability MUST acquire an exclusive row lock on the target vehicle:
   ```sql
   SELECT id FROM vehicles WHERE id = :vehicleId FOR UPDATE;
   ```
2. **Composite Availability Verification:** Within the locked transaction, verify zero overlapping intervals across confirmed bookings, active unexpired holds (`booking_holds.status IN ('ACTIVE', 'VERIFICATION_PENDING') AND expires_at > NOW()`), maintenance blocks, and operational turnaround buffers.

---

## ADR-006: Background Task Scheduling and Distributed Locking with ShedLock

### Context
Clustered deployments must not execute duplicate background jobs (expiring stale holds, dispatching outbox events, checking overdue returns).

### Decision
1. **Spring Task Scheduling:** Use `@EnableScheduling` with fixed-delay and cron tasks.
2. **ShedLock Integration:** Multi-instance deployments synchronize scheduled tasks using ShedLock backed by the MySQL `shedlock` table (`lock_until`, `locked_at`, `locked_by`).
3. **Idempotency:** All scheduled tasks are designed to be idempotent and safe against repeat executions.

---

## ADR-007: Normalized Append-Only Double-Entry General Ledger [STATUS: DEFERRED TO FUTURE PHASE]

### Context
In earlier platform-collection designs, a 3-table double-entry ledger (`ledger_accounts`, `ledger_transactions`, `ledger_entries`) was proposed to track platform cash flows, gateway clearing, and partner payables. Under the MVP Partner-Direct collection model, customer funds do not flow through platform accounts.

### Decision
1. **Deferred to Future Phase:** Because the platform does not collect, hold, or disburse customer rental money or security deposits in the MVP, the 3-table platform double-entry general ledger is **deferred to Future Phase** (Marketplace Payment Gateway Integration).
2. **Operational Audit Trail for MVP:** The platform maintains an operational audit trail via `rental_payment_demands`, `rental_payment_submissions`, `security_deposits`, `security_deposit_refunds`, `contributor_payables`, and `audit_logs`.
3. **Zero Platform Cash Guarantee:** External partner-to-customer and partner-to-contributor transfers are **never** represented as platform cash movements.

---

## ADR-008: Marketplace Payments & Contributor Contractual Settlement Architecture (Approach A)

### Context
Marketplace platforms in India cannot arbitrarily collect and distribute third-party funds without regulatory compliance. The contributor receives an agreed fixed payment per eligible completed booking under a commercial contract with the rental partner. The platform must not silently become the contributor's legal debtor.

### Decision
1. **Contractual Principle:** The rental partner is the sole legal debtor for the contributor's fixed payment.
2. **Approach A (Direct Partner Settlement - Confirmed MVP Baseline):**
   - The partner settles the contributor directly via bank transfer (NEFT/IMPS/UPI) outside the platform.
   - The platform tracks the obligation in `contributor_payables`, stores the partner's payment reference/UTR, date, and receipt attachment, tracks settlement status, allows contributor confirmation or dispute, and provides administrative oversight.
   - Direct payments are **never recorded as platform cash disbursements**. The platform assumes zero debtor liability.
3. **Single Lump-Sum Model:** Enforce one complete direct settlement per eligible booking. Partial payments are prohibited.
4. **Approach B (Platform-Facilitated Settlement via Marketplace Gateway):** Deferred to Future Phase pending partner linked-account onboarding and regulatory validation.

---

## ADR-009: Physical Turnaround Buffers and Configurable Cancellation Terms

### Context
Vehicles require inspection, cleaning, and transit between rentals. Contributor withdrawal requests must not disrupt confirmed customer rentals.

### Decision
1. **Turnaround Buffers:** Every vehicle listing enforces configurable `turnaround_buffer_minutes` (e.g. 60 min for cars, 30 min for bikes/scooters) before and after bookings.
2. **Strict Booking Precedence:** Confirmed customer bookings strictly supersede contributor withdrawal notices. Vehicle return back to the contributor is scheduled only after the completion of the latest confirmed booking.
3. **Agreement-Specific Cancellation:** Cancelled bookings do not automatically generate contributor payouts unless the signed agreement explicitly defines cancellation compensation (`cancellation_compensation_rate`).

---

## ADR-010: Generic Physical Vehicle Custody Event Model (Zero Unvalidated Polymorphic FKs)

### Context
Previous custody models used untyped polymorphic ID fields (`custodian_id`), which compromised referential integrity.

### Decision
1. **Initial Custody Rules:**
   - Partner-owned vehicles begin in `PARTNER_CUSTODY`.
   - Contributor-owned vehicles begin in `CONTRIBUTOR_CUSTODY`.
2. **Explicit Relational Foreign Keys:** Implement `vehicle_custody_events` capturing:
   - `handover_address_id` (FK $\rightarrow$ `addresses.id`)
   - `actor_user_id` (FK $\rightarrow$ `users.id`)
   - `authorized_rep_user_id` (FK $\rightarrow$ `users.id` nullable)
   - `from_custodian_user_id` / `from_partner_location_id`
   - `to_custodian_user_id` / `to_partner_location_id`
3. **All 8 Canonical Permutations Supported:** Contributor-to-partner, partner-to-customer, contributor-to-customer (owner-managed), customer-to-partner, customer-to-contributor (owner-managed return), partner-to-contributor, inter-hub transfer, and logistics agent relocation.

---

## ADR-011: Strict Decoupling of Operational and Financial Lifecycles

### Context
Connecting operational booking completion with deposit refunds, damage claims, and contributor payouts introduces severe operational deadlock.

### Decision
1. **Independent State Machines:**
   - A booking transitions to `COMPLETED` once the vehicle is returned and the return inspection is signed.
   - Security deposit refund, deduction, or dispute operates independently on `security_deposits` and `deposit_deductions`.
   - Contributor payable eligibility, scheduling, and settlement operate independently on `contributor_payables`.
2. **Failure Isolation:** Delayed or disputed refunds or payouts never block operational booking completion or vehicle re-listing.

---

## ADR-012: Database Design Freeze Baseline (48 Proposed Physical Tables)

### Context
Following the transition to Partner-Direct payment collection and the explicit modeling of rental-fee refund tracking (`rental_fee_refunds`), the physical schema baseline is finalized before implementation.

### Decision
1. **Authoritative Proposed Count:** Freeze the physical database schema at exactly **48 physical tables**:
   - **46 Application-Domain Tables** across Modules 1 through 8 (including `rental_fee_refunds`).
   - **2 Infrastructure-Managed Tables** (`shedlock`, `flyway_schema_history`).
   - **8 Future-Phase Tables** cataloged for future marketplace gateway integration (56 total cataloged entities).
2. **Pre-Implementation Status:** No database tables or migrations have been created yet.

---

## ADR-013: Configurable Operational Defaults

### Context
Hardcoding operational parameters into application code prevents business tuning and regional flexibility.

### Decision
1. **Configurable Defaults:** The following operational parameters are externalized in configuration/database settings:
   - Booking hold TTL: 15 minutes.
   - Payment verification window: 60 minutes.
   - Car turnaround buffer: 60 minutes.
   - Bike turnaround buffer: 30 minutes.
   - Scooter turnaround buffer: 30 minutes.
   - Contributor settlement mode: Direct partner payment (Approach A).
   - Vehicle withdrawal notice: Recommended 7 days, subject to confirmed bookings.
   - Deposit dispute response window: 48 hours.
   - Customer refund acknowledgement window: 72 hours.
   - Overdue return grace period: 30 minutes.

---

## ADR-014: Legal Ownership Modeling & Partner Business Fleet Support

### Context
The catalog must support individual vehicle owners, partner business legal owners, and independent operational managers.

### Decision
1. **Explicit Ownership Columns in `vehicles`:**
   - `ownership_type` ENUM: `INDIVIDUAL_OWNER`, `PARTNER_BUSINESS_OWNER`.
   - `legal_owner_user_id` (FK $\rightarrow$ `users.id` nullable) for individual citizens.
   - `legal_owner_partner_id` (FK $\rightarrow$ `partner_profiles.id` nullable) for partner businesses.
   - `manager_partner_id` (FK $\rightarrow$ `partner_profiles.id` NOT NULL) for the operational manager.
2. **Check Constraint:** `CHECK ((ownership_type = 'INDIVIDUAL_OWNER' AND legal_owner_user_id IS NOT NULL AND legal_owner_partner_id IS NULL) OR (ownership_type = 'PARTNER_BUSINESS_OWNER' AND legal_owner_partner_id IS NOT NULL))`.

---

## ADR-015: Partner Registration Verification Gating & Manual Verification Protocol

### Context
Unverified partner applicants must not access fleet management or customer rentals. In addition, payments submitted by customers must be verified before booking confirmation.

### Decision
1. **Partner Verification Gate:** Users with `partner_profiles.verification_status != 'VERIFIED'` are blocked by `@PreAuthorize` evaluators from registering fleet inventory, configuring payment profiles, accepting contributed vehicles, or fulfilling bookings.
2. **Manual Payment Verification Gate:** Only verified partners managing the vehicle can verify or reject customer payment submissions. A customer-uploaded screenshot or UTR never auto-confirms a payment.

---

## ADR-016: Platform Merchant-of-Record Model [STATUS: SUPERSEDED BY ADR-018]

### Context & Status
*This ADR previously proposed the platform acting as Merchant of Record with a centralized Razorpay account, upfront platform deposit custody, and T+2 partner payouts. It has been **SUPERSEDED** by ADR-018 (Partner-Direct Payment Collection) for the MVP release.*

---

## ADR-017: Contributor Settlement Model & Bilateral Dispute Mechanics (Approach A)

### Context
A decision is required between a single immutable lump-sum settlement model vs. introducing multi-event contributor settlement history tables. Additionally, bilateral settlement rules govern partner-contributor obligations.

### Decision
1. **Selected Contributor Settlement Model:** Enforce **One complete direct settlement per contributor payable, with immutable payment evidence (Single Lump-Sum Model)**. Each completed booking generates exactly one indivisible contributor payable. Partial payments or installment disbursements are prohibited.
2. **Direct Bilateral Settlement:** The rental partner pays the contributor directly outside the platform. The platform records the UTR, receipt, and date in `contributor_payables`.
3. **Contributor Confirmation & Dispute:** The contributor has an explicit window to confirm receipt or raise a dispute. Disputed settlements trigger Admin arbitration.
4. **Reversal & Overpayment Recovery:** When an incorrect contributor payment or damage adjustment occurs, the partner recovers the overpayment directly from the contributor outside the platform, with evidence recorded in the payable audit trail.

---

## ADR-018: Partner-Direct Payment Collection, Decoupled State Machines & Refund Tracking (MVP Baseline)

### Context
Launching a vehicle rental marketplace in Vijayawada requires a legally sound, operationally robust, and low-friction payment architecture. Operating a platform-owned payment gateway with centralized collection, escrow-like deposit holding, and split payouts introduces regulatory complexity (RBI Payment Aggregator regulations, GST Section 9(5) E-Commerce Operator compliance, escrow bank accounts). Furthermore, payment demand state must not overlap with individual submission state, and rental-fee refunds must have a distinct, auditable lifecycle from deposit refunds.

### Decision
The platform adopts **PARTNER-DIRECT PAYMENT COLLECTION WITH DECOUPLED STATE MACHINES AND RENTAL-FEE REFUND TRACKING** for the MVP:

1. **Direct Payee Role:** The rental partner managing the vehicle is the customer-facing rental service provider and direct payee for all bookings on that vehicle.
2. **Zero Platform Cash Handling:** The platform does NOT collect, hold, escrow, split, or disburse customer rental money or security deposits. No platform-owned payment gateway account is integrated for customer rental checkout.
3. **Partner Payment Methods Supported:**
   - Partner's UPI QR Code (uploaded image displayed at checkout).
   - Partner's UPI ID (VPA displayed with copy button).
   - Partner's external payment link (if configured).
   - Cash at pickup (only if explicitly enabled by the partner).
4. **Decoupled Payment Demands and Submissions:**
   - `rental_payment_demands`: Models the total financial obligation (`OPEN`, `PARTIALLY_SATISFIED`, `SATISFIED`, `EXPIRED`, `CANCELLED`).
   - `rental_payment_submissions`: Models individual customer payments/proof (`SUBMITTED_FOR_VERIFICATION`, `VERIFIED`, `REJECTED`).
   - Multiple submissions accumulate verified amounts in `rental_payment_demands.amount_satisfied`. Top-up payments are supported.
   - Overpayments (verified amounts > demanded) transition the demand to `SATISFIED` and trigger an overpayment refund in `rental_fee_refunds`.
5. **Strict Partner Verification Gate (Invariant):**
   - **A customer-uploaded screenshot, receipt, or UTR must NEVER automatically mark a payment as verified or confirm a booking.**
   - The partner independently verifies their own UPI, bank, or cash records.
   - ONLY an explicit action by the managing partner marks a submission `VERIFIED`.
6. **Booking Confirmation Invariant:**
   A booking can be confirmed ONLY when:
   - Availability and vehicle assignment are valid under row lock (`SELECT ... FOR UPDATE`).
   - Demands are `SATISFIED` through verified submissions, OR partner-approved pay-at-pickup policy permits unpaid balance.
   - Any required security-deposit policy is satisfied.
   - Hold is still valid or safely revalidated under vehicle lock. An expired hold is **NEVER** silently confirmed.
   - For pay-at-pickup, vehicle handover is blocked until required collections are verified at pickup.
7. **Rental-Fee Refund Tracking (`rental_fee_refunds`):**
   - Independent application table tracking partner-executed rental fee refunds for booking cancellations, overpayments, expired-hold late payments, and dispute resolutions.
   - Cancellation refund amounts are determined dynamically from configurable business rules snapshotted in `cancellation_policy_snapshot` (never hardcoded).
   - Partner-entered UTR transitions status to `DISBURSED_BY_PARTNER`. Customer confirmation or 72h dispute window required (`CONFIRMED_BY_CUSTOMER` or `DISPUTED_BY_CUSTOMER`).
8. **Security Deposit Direct Custody & Refunds (`security_deposits`, `deposit_deductions`, `security_deposit_refunds`):**
   - Partner directly collects, holds, and refunds security deposits.
   - Disputed damage claims require evidence and never auto-approve on timeout; require Admin arbitration.
9. **Contributor Compensation (Approach A Preserved):**
   - Partner owes contributor an agreed fixed fee per completed booking.
   - Partner pays contributor directly outside the platform and logs UTR in `contributor_payables`.

### Alternatives Considered
1. **Platform as Merchant of Record with Razorpay Route:** Centralized collection, marketplace split settlements, automated payouts. *Rejected for MVP due to regulatory complexity, escrow licensing requirements, and partner linked-account onboarding friction.*
2. **Automated Screenshot OCR / Webhook Parsing:** Auto-confirming bookings on receipt upload. *Rejected due to extreme fraud vulnerability (tampered images, recycled UTRs).*
3. **Combined Deposit & Rental Refund Table:** Overloading `security_deposit_refunds` to handle rental cancellations. *Rejected to prevent conflating distinct accounting categories, distinct tax obligations, and distinct dispute rules.*
4. **Partner-Direct Collection with Manual Verification Gate & Separate Refund Tracking:** *Selected for MVP for speed, regulatory simplicity, complete auditability, zero platform cash liability, and clear separation of concerns.*

### Responsibilities Matrix
- **Platform Responsibilities:** Operational record-keeping, booking state management, inventory locking, displaying partner payment profiles, capturing customer submission metadata, notifying parties, tracking verification deadlines, recording rental fee refunds, deposit refunds and contributor payables, providing Admin dispute resolution.
- **Rental Partner Responsibilities:** Maintaining accurate UPI QR/ID in payment profile, collecting rental charges and deposits directly, verifying transactions against bank/UPI statements, executing rental-fee refunds, executing deposit refunds, paying contributors directly outside the platform.

### Future Phase Migration Path
When the business scales and regulatory/commercial validation is completed:
- Integrate an approved marketplace payment provider (e.g., Razorpay Route) using provider-agnostic interfaces.
- Activate the **8 Future-Phase Tables** (`payment_webhook_events`, `partner_commissions`, `payout_methods`, `settlements`, `ledger_accounts`, `ledger_transactions`, `ledger_entries`, `platform_commission_rules`).
- Onboard partners to linked merchant accounts with automated KYC.

### Outstanding Validation Gates
Before production launch:
- **Legal Validation:** Confirm partner-as-service-provider and partner-as-payee contract terms.
- **Tax Validation:** Confirm GST compliance (pure tech intermediary vs. e-commerce operator).
- **Insurance Validation:** Verify partner commercial rental insurance policies.
- **Payment Provider Pre-Approval:** Maintain provider-agnostic design for future marketplace gateway integration.

