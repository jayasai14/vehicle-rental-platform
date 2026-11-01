# Architecture Decision Records (ADRs) - Pre-Freeze Candidate Baseline

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh  
**Status:** Pre-Freeze Corrected Baseline  

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
3. **Delayed Webhook Auto-Refund:** If a successful payment webhook arrives after a hold has expired and the interval has been reserved by another customer, the platform automatically triggers a 100% gateway refund.

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
3. **Transactional Outbox Pattern:** All external network interactions (Razorpay APIs, Cloudinary uploads, notification dispatch) write an event record into `outbox_events` in the same local transaction as the domain state change.
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

## ADR-007: Append-Only Double-Entry Financial Ledger

### Context
Mutable balance columns (`wallet_balance += amount`) cause lost updates, race conditions, and an un-auditable paper trail.

### Decision
1. **Double-Entry Ledger:** All monetary movements are recorded in `ledger_entries` under a `transaction_group_id`, where total debits equal total credits ($\sum \text{Debits} = \sum \text{Credits}$).
2. **Strict Valid Accounts:** Every entry references an approved account code from the Chart of Accounts.
3. **Immutable History:** Balances are derived projections (`SUM(credit - debit)`). Financial corrections use reversal entries, never updates or deletes.
4. **Liability Segregation:** Platform liabilities (`CUSTOMER_REFUNDABLE_LIABILITY`) are strictly segregated from partner contractual obligations (`CONTRIBUTOR_PAYABLE`).

---

## ADR-008: Marketplace Payments & Contributor Contractual Settlement Architecture

### Context
Marketplace platforms in India cannot arbitrarily collect and distribute third-party funds without regulatory compliance. The contributor receives an agreed fixed payment per eligible completed booking under a commercial contract with the rental partner. The platform must not silently become the contributor's legal debtor.

### Decision
1. **Contractual Principle:** The rental partner is the sole legal debtor for the contributor's fixed payment.
2. **Approach A (Direct Partner Settlement - Recommended MVP Baseline):**
   - The partner settles the contributor directly via bank transfer (NEFT/IMPS/UPI) outside the platform.
   - The platform calculates the payable, records the obligation, allows the partner to record payment and store the UTR reference, tracks settlement status, allows contributor confirmation or dispute, and provides administrative oversight.
   - The platform never holds or routes third-party contributor funds and assumes zero debtor liability.
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

## ADR-010: Generic Physical Vehicle Custody Event Model

### Context
Previous custody models did not model direct contributor-to-customer handovers or distinguish partner-owned from contributor-owned initial states.

### Decision
1. **Initial Custody Rules:**
   - Partner-owned vehicles begin in `PARTNER_CUSTODY`.
   - Contributor-owned vehicles begin in `CONTRIBUTOR_CUSTODY`.
2. **Generic Event Model:** Implement `vehicle_custody_events` tracking all 8 canonical permutations:
   - `CONTRIBUTOR_TO_PARTNER`
   - `PARTNER_TO_CUSTOMER`
   - `CONTRIBUTOR_TO_CUSTOMER` (owner-managed handover)
   - `CUSTOMER_TO_PARTNER`
   - `CUSTOMER_TO_CONTRIBUTOR` (owner-managed return)
   - `PARTNER_TO_CONTRIBUTOR`
   - `INTER_HUB_TRANSFER`
   - `LOGISTICS_RELOCATION` (authorized representative)
3. **Independence:** Custody events are tracked independently of vehicle legal ownership and booking status.

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

## ADR-012: Pre-Freeze Physical Database Inventory (52 Proposed Tables)

### Context
Previous documentation had ambiguities in entity counts due to grouping alternative tables.

### Decision
1. **Authoritative Proposed Count:** Confirm the proposed physical database schema at exactly **52 physical tables**:
   - 50 Application-Domain Tables
   - 2 Infrastructure-Managed Tables (`shedlock`, `flyway_schema_history`)
2. **Pre-Implementation Status:** No database tables or migrations have been created yet.
3. **Ledger Normalization Option:** The confirmed baseline uses `ledger_entries` with an identifiable `transaction_group_id` and strict ENUM Chart of Accounts. If the Product Owner mandates full relational normalization into `ledger_accounts`, `ledger_transactions`, and `ledger_entries`, the count expands to 52 domain + 2 infra = 54 tables.

---

## ADR-013: Configurable Operational Defaults

### Context
Hardcoding operational parameters (hold duration, turnaround buffers, withdrawal notice periods) into application code prevents business tuning and cross-category flexibility.

### Decision
1. **Configurable Defaults:** The following operational parameters are defined as database-driven / externalized configuration defaults, never hardcoded:
   - Booking hold TTL: 15 minutes.
   - Car turnaround buffer: 60 minutes.
   - Bike turnaround buffer: 30 minutes.
   - Scooter turnaround buffer: 30 minutes.
   - Contributor settlement mode: Direct partner payment (Approach A).
   - Vehicle withdrawal notice: Recommended 7 days, subject to confirmed bookings.
   - Deposit dispute response window: 48 hours.
   - Overdue return grace period: 30 minutes.
