# Architecture Decision Records (ADRs) - Freeze Baseline

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh  
**Status:** Database Design Freeze Candidate Baseline (54-Table Architecture)  

> [!IMPORTANT]
> **Pre-Implementation Notice:** All decisions below govern the **proposed architecture and database design**. No application code, JPA entities, SQL DDL scripts, or Flyway migrations have been generated yet. Formal implementation begins only after final Product Owner approval.

---

## ADR-001: Upfront Rental Payment and Refundable Security Deposit Collection

### Context
Vehicle rentals require security deposits to cover potential damage, fuel discrepancies, traffic fines, and overdue returns. In India, retail payment methods (UPI and domestic debit cards) do not universally support multi-day pre-authorization holds without recurring e-mandates.

### Decision
1. **Method-Independent Upfront Collection:** The platform collects rental charges and security deposits upfront in a single payment order while keeping the underlying payment architecture method-independent.
2. **Refundable Customer Liability Accounting:** The collected security deposit is recorded as a **refundable customer liability**, never as platform revenue, partner turnover, or taxable income.
3. **Non-Escrow Designation:** The platform's internal double-entry ledger tracks deposit liabilities and must not be described as a third-party escrow account.
4. **Documented Deductions & Review Process:** Deductions from security deposits are permitted only with approved inspection evidence (photos, odometer, fuel readings).
5. **No Automatic Claim Approval:** **A disputed damage claim is NEVER automatically approved merely because a 48-hour response window expires.** Disputed claims require Admin arbitration. If a customer is non-responsive after 48 hours, an Admin must verify the evidence before any deduction is processed.
6. **Immediate Release of Undisputed Balances:** Any uncontested portion of a security deposit is refunded immediately to the customer via payment gateway refund APIs.

---

## ADR-002: Relational Schema & Hold Independence

### Context
Previous proposals lacked explicit separation between temporary reservations and confirmed bookings, risking dirty reads and database bloat from abandoned checkout sessions.

### Decision
1. **Hold Independence:** `booking_holds` is an independent 15-minute temporary reservation lock acquired under pessimistic `SELECT FOR UPDATE` locking.
2. **No Incomplete Booking Records:** Creating a hold **does NOT insert an incomplete or draft record into the `bookings` table**. The `bookings` table receives records only upon verified payment capture.
3. **Delayed Webhook Protocol:** If a payment webhook arrives after hold expiration, the system executes an availability check under `SELECT FOR UPDATE`. It recovers the booking if available, or automatically triggers a 100% gateway refund if the interval was reserved by another customer.

---

## ADR-003: Contributor Profitability Price Floor Engine & Partner Discount Model

### Context
In contributor-owned vehicles, the contributor is contractually entitled to an agreed fixed payout per completed eligible booking. If a partner discounts the vehicle below the total of contributor payout + platform commission + taxes + gateway fees, a financial deficit occurs.

### Decision
1. **Profitability Floor Validation:** The backend enforces a minimum listing price for contributor vehicles:
   $$\text{Base Rental Price} \ge \frac{\text{Contributor Fixed Fee} + \text{Partner Min Margin} + \text{Allocable Fixed Costs}}{1 - (\text{Platform Commission \%} + \text{Gateway Fee \%} + \text{Effective Tax \%})}$$
2. **Clean Separation of Components:** The system cleanly separates taxable rental amount, platform commission, gateway fees, fixed contributor compensation, partner margin, and discount funding.
3. **No Hardcoded GST Deductions:** Tax calculations are **not** hardcoded as a simple percentage deduction from rental revenue; they are configurable and marked pending professional tax validation.
4. **Partner-Funded Discount Subsidies:** If a partner offers a promotional rate below the floor, an authorized override debits the partner's account balance (`partner_discount_subsidy`) without reducing contributor compensation.

---

## ADR-004: Spring Event-Driven Architecture and Transactional Outbox Pattern

### Context
Direct synchronous inter-module method calls create tight circular coupling and distributed transaction failure loops.

### Decision
1. **Spring Application Events:** Cross-module communication uses strongly-typed domain events published via `ApplicationEventPublisher`.
2. **Transactional Event Listeners:** Downstream modules consume events via `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
3. **Transactional Outbox Pattern:** All external network interactions (Razorpay payment creation/refunds, Cloudinary uploads, notification dispatch) write an event record into `outbox_events` in the same local transaction as the domain state change.
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
2. **Composite Availability Verification:** Within the locked transaction, verify zero overlapping intervals across confirmed bookings, active unexpired holds (`booking_holds.status = 'ACTIVE' AND expires_at > NOW()`), maintenance blocks, and operational turnaround buffers.

---

## ADR-006: Background Task Scheduling and Distributed Locking with ShedLock

### Context
Clustered deployments must not execute duplicate background jobs (expiring stale holds, dispatching outbox events, checking overdue returns).

### Decision
1. **Spring Task Scheduling:** Use `@EnableScheduling` with fixed-delay and cron tasks.
2. **ShedLock Integration:** Multi-instance deployments synchronize scheduled tasks using ShedLock backed by the MySQL `shedlock` table (`lock_until`, `locked_at`, `locked_by`).
3. **Idempotency:** All scheduled tasks are designed to be idempotent and safe against repeat executions.

---

## ADR-007: Normalized Append-Only Double-Entry General Ledger

### Context
Mutable balance columns (`wallet_balance += amount`) cause lost updates, race conditions, and an un-auditable paper trail. Furthermore, mixing partner-to-contributor obligations into the platform's general ledger falsely represents external bank transfers as platform cash disbursements.

### Decision
1. **Normalized 3-Table Ledger:** The platform general ledger is implemented across three physical tables:
   - `ledger_accounts`: Chart of Accounts Master (`account_code`, `category`, `normal_balance`).
   - `ledger_transactions`: Journal headers (`transaction_group_id`, `reference_type`, `reference_id`, `posted_at`).
   - `ledger_entries`: Journal lines (`transaction_id FK`, `account_id FK`, `debit_amount`, `credit_amount`).
2. **Balance Guarantees:** Every transaction header strictly balances ($\sum \text{Debits} = \sum \text{Credits}$). Corrections execute exclusively via reversal entries.
3. **Platform Scope Only:** The platform general ledger records **only** actual platform cash and liabilities (`GATEWAY_CLEARING`, `CUSTOMER_REFUNDABLE_LIABILITY`, `CUSTOMER_RENTAL_CLEARING`, `PLATFORM_COMMISSION_REVENUE`, `PARTNER_PAYABLE`, `REFUND_PAYABLE`, `SETTLEMENT_CLEARING`). Direct partner-to-contributor payments are completely excluded from platform cash accounting.

---

## ADR-008: Marketplace Payments & Contributor Contractual Settlement Architecture

### Context
Marketplace platforms in India cannot arbitrarily collect and distribute third-party funds without regulatory compliance. The contributor receives an agreed fixed payment per eligible completed booking under a commercial contract with the rental partner. The platform must not silently become the contributor's legal debtor.

### Decision
1. **Contractual Principle:** The rental partner is the sole legal debtor for the contributor's fixed payment.
2. **Approach A (Direct Partner Settlement - Recommended MVP Baseline):**
   - The partner settles the contributor directly via bank transfer (NEFT/IMPS/UPI) outside the platform.
   - The platform tracks the obligation in `contributor_payables`, stores the partner's payment reference/UTR, date, and receipt attachment, tracks settlement status, allows contributor confirmation or dispute, and provides administrative oversight.
   - Direct payments are **never recorded as platform cash disbursements**. The platform assumes zero debtor liability.
3. **Approach B (Platform-Facilitated Settlement via Razorpay Route - PENDING Legal/Provider Validation):**
   - Remains a future enhancement subject to partner linked-account onboarding, KYC, and RBI payment aggregator compliance review.

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

## ADR-012: Database Design Freeze Baseline (54 Proposed Tables)

### Context
Full normalization of the double-entry general ledger (`ledger_accounts`, `ledger_transactions`, `ledger_entries`) requires expanding the physical schema count.

### Decision
1. **Authoritative Proposed Count:** Freeze the physical database schema at exactly **54 physical tables**:
   - 52 Application-Domain Tables
   - 2 Infrastructure-Managed Tables (`shedlock`, `flyway_schema_history`)
2. **Pre-Implementation Status:** No database tables or migrations have been created yet.

---

## ADR-013: Configurable Operational Defaults

### Context
Hardcoding operational parameters into application code prevents business tuning and regional flexibility.

### Decision
1. **Configurable Defaults:** The following operational parameters are externalized in configuration/database settings:
   - Booking hold TTL: 15 minutes.
   - Car turnaround buffer: 60 minutes.
   - Bike turnaround buffer: 30 minutes.
   - Scooter turnaround buffer: 30 minutes.
   - Contributor settlement mode: Direct partner payment (Approach A).
   - Vehicle withdrawal notice: Recommended 7 days, subject to confirmed bookings.
   - Deposit dispute response window: 48 hours.
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

## ADR-015: Partner Registration Verification Gating & Late Webhook Protocol

### Context
Unverified partner applicants must not access fleet management or customer rentals. In addition, payments arriving after hold expiration must be safely reconciled.

### Decision
1. **Partner Verification Gate:** Users with `partner_profiles.verification_status != 'VERIFIED'` are blocked by `@PreAuthorize` evaluators from registering fleet inventory, accepting contributed vehicles, or fulfilling bookings.
2. **Late Webhook Protocol:** If a payment webhook arrives after hold expiration:
   - Fresh availability check is executed under `SELECT ... FOR UPDATE`.
   - Recovers booking as `CONFIRMED` if the requested interval plus turnaround buffer remains completely available.
   - Automatically triggers a 100% gateway refund if the interval was reserved by another customer in the interim.
   - All operations are completely idempotent against duplicate webhook delivery (`payment_webhook_events`).

---

## ADR-016: Customer Payment Collection, Deposit Custody, and Partner Settlement Liability Architecture

### Context
Clear legal and financial demarcations are required for customer checkout collections, refundable deposit custody, customer refund obligations, and partner rental proceeds disbursements. Direct partner-to-contributor settlement under Approach A must not be conflated with customer-facing payment flows.

### Decision
1. **Merchant of Record (MoR):** The Platform Operating Entity acts as the digital marketplace MoR for customer transactions, contracting with RBI-authorized payment aggregators, collecting total checkout payments, and issuing platform service tax invoices. *(Subject to pending legal validation regarding GST Section 9(5) E-Commerce Operator classification).*
2. **Deposit Custody & Liability:** Security deposits are held by the Platform Operating Entity strictly as a **Refundable Balance Sheet Liability** (`CUSTOMER_REFUNDABLE_LIABILITY`). Deposits are never treated as platform revenue or working capital.
3. **Refund Responsibility:** The Platform Operating Entity bears sole operational responsibility for issuing customer refunds directly to the original payment instrument via payment gateway APIs. Partners never refund customers directly.
4. **Partner Rental Proceeds Settlement:** The Platform Operating Entity is contractually liable to disburse net rental proceeds (`PARTNER_PAYABLE`) to verified partners on a scheduled batch cycle (T+2) via payout APIs (`SETTLEMENT_CLEARING`).
5. **Method-Independent Gateway Architecture:** Payment gateway integrations are abstracted behind provider-agnostic interfaces (`PaymentGatewayClient`, `PayoutGatewayClient`). The schema, state machines, and double-entry ledger remain decoupled from proprietary vendor mechanisms.
6. **Separation from Approach A:** Direct partner-to-contributor settlement under Approach A applies strictly to the bilateral partner-contributor agreement and does NOT establish compliance for the customer payment collection and partner payout flow.

---

## ADR-017: Contributor Settlement Model & Post-Payout Reversal Recovery via Partner Receivables

### Context
A decision is required between a single immutable lump-sum settlement model vs. introducing multi-event contributor settlement history tables. Additionally, balanced double-entry accounting mechanics are required when an incorrect partner credit has already been paid out.

### Decision
1. **Selected Contributor Settlement Model:** Enforce **One complete direct settlement per contributor payable, with immutable payment evidence (Single Lump-Sum Model)**. Each completed booking generates exactly one indivisible contributor payable. Partial payments or installment disbursements are prohibited at the domain and database levels. This avoids introducing a 55th table (`contributor_settlement_events`) and preserves the 54-table freeze.
2. **Post-Payout Reversal & Partner Receivable (`PARTNER_RECEIVABLE`):**
   - When an incorrect credit or damage deduction is reversed after payout funds have already been disbursed to the partner's external bank account, the platform establishes an asset claim:
     $$\text{Debit } \texttt{PARTNER\_RECEIVABLE} \quad / \quad \text{Credit } \texttt{REFUND\_PAYABLE}$$
   - This allows immediate gateway refund execution to the customer without creating negative liability balances or untraceable ledger states.
3. **Recovery Execution:**
   - **Option A (Automated Offset):** Deducted automatically from the partner's net rental proceeds in the subsequent settlement batch.
   - **Option B (Direct Repayment):** Settled via direct bank transfer or payment link from partner to platform operating clearing.
