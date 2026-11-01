# Business Requirements (Freeze Baseline)

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh (Expandable)  
**Status:** Database Design Freeze Candidate Baseline  

---

## 1. Product Scope

A modern vehicle rental marketplace launching initially in Vijayawada, Andhra Pradesh, supporting cars, bikes, and scooters. The platform combines conventional rental partner fleet inventory with an innovative vehicle contribution model allowing individual vehicle owners to monetize idle vehicles through verified rental partners.

---

## 2. Inventory Channels & Legal Ownership Architecture

The platform strictly differentiates between **Legal Ownership**, **Operational Management**, and **Physical Custody**:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              INVENTORY ARCHITECTURE                                    │
│                                                                                        │
│   ┌────────────────────────────────┐         ┌─────────────────────────────────────┐   │
│   │     PARTNER-OWNED FLEET        │         │      CONTRIBUTOR-OWNED FLEET        │   │
│   ├────────────────────────────────┤         ├─────────────────────────────────────┤   │
│   │ Legal Owner: Partner Business  │         │ Legal Owner: Individual Contributor │   │
│   │ (legal_owner_partner_id)       │         │ (legal_owner_user_id)               │   │
│   │ Manager: Rental Partner        │         │ Manager: Assigned Rental Partner    │   │
│   │ Initial Custody: PARTNER_CUSTODY│        │ Initial Custody: CONTRIBUTOR_CUSTODY│   │
│   │ Agreement: None (Internal)     │         │ Agreement: Bilateral Fixed Contract │   │
│   └────────────────────────────────┘         └─────────────────────────────────────┘   │
│                   │                                             │                      │
│                   └──────────────────────┬──────────────────────┘                      │
│                                          ▼                                             │
│                       [UNIFIED CUSTOMER MARKETPLACE]                                   │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### 2.1 Supported Legal Ownership Combinations:
The data model explicitly enforces valid ownership and operational combinations via database foreign keys and check constraints:
1. **Individual Legal Owner (`INDIVIDUAL_OWNER`):**
   - The vehicle is legally owned by an individual citizen/contributor (`vehicles.legal_owner_user_id` FK $\rightarrow$ `users.id`).
   - `legal_owner_partner_id` is strictly NULL.
   - `manager_partner_id` (FK $\rightarrow$ `partner_profiles.id`) references the commercial partner managing the rentals.
   - `inventory_source = 'CONTRIBUTOR_OWNED'`.
2. **Partner Business Legal Owner (`PARTNER_BUSINESS_OWNER`):**
   - The vehicle is legally owned by a commercial rental business entity (`vehicles.legal_owner_partner_id` FK $\rightarrow$ `partner_profiles.id`).
   - `manager_partner_id` references the managing partner (typically the same partner, or another partner under fleet-management agreements).
   - `inventory_source = 'PARTNER_OWNED'`.

### 2.2 Partner Onboarding & Verification Gate:
- **Pending Applicants Blocked:** Submitting a partner application (`partner_profiles.verification_status = 'SUBMITTED'`) does **not** grant partner operational capabilities.
- **Verification Gate:** Only formally approved partners (`verification_status = 'VERIFIED'` and `user_roles` containing `RENTAL_PARTNER`) may:
  - Register and publish fleet vehicles.
  - Review contributor vehicle applications.
  - Propose and execute contribution agreements.
  - Configure partner hubs and delivery zones.
  - Fulfill customer bookings.

---

## 3. Vehicle Custody Architecture (Independent of Ownership & Bookings)

Physical vehicle custody is tracked as an independent state machine (`vehicle_custody_events`) with **explicit typed relational references**, eliminating unvalidated polymorphic foreign keys.

### Custodian Relational Attributes:
- **Site of Handover:** `handover_address_id` (FK $\rightarrow$ `addresses.id`).
- **Authenticated Handover Actor:** `actor_user_id` (FK $\rightarrow$ `users.id`).
- **Authorized Representative / Logistics Witness:** `authorized_rep_user_id` (FK $\rightarrow$ `users.id` nullable).
- **Previous Custodian:** `from_custodian_user_id` (FK $\rightarrow$ `users.id` nullable) OR `from_partner_location_id` (FK $\rightarrow$ `partner_locations.id` nullable).
- **Receiving Custodian:** `to_custodian_user_id` (FK $\rightarrow$ `users.id` nullable) OR `to_partner_location_id` (FK $\rightarrow$ `partner_locations.id` nullable).

### Supported 8 Canonical Permutations:
1. `CONTRIBUTOR_TO_PARTNER`: Contributor delivers vehicle to partner hub for fleet onboarding.
2. `PARTNER_TO_CUSTOMER`: Standard customer pickup at partner hub (partner-managed handover).
3. `CONTRIBUTOR_TO_CUSTOMER`: Customer picks up vehicle directly from contributor residence (**owner-managed direct handover**).
4. `CUSTOMER_TO_PARTNER`: Standard customer return to partner hub (partner-managed return).
5. `CUSTOMER_TO_CONTRIBUTOR`: Customer returns vehicle directly to contributor residence (**owner-managed direct return**).
6. `PARTNER_TO_CONTRIBUTOR`: Partner returns vehicle to contributor upon agreement termination or withdrawal.
7. `INTER_HUB_TRANSFER`: Partner relocates vehicle between two operational depots in the city.
8. `LOGISTICS_RELOCATION`: Authorized representative / logistics driver handles doorstep delivery or recovery.

---

## 4. Roles and Core Capabilities

| Role | Core Capabilities |
|---|---|
| **CUSTOMER** | Search, compare, create 15-min booking holds, confirm bookings via payment, manage own bookings, request extensions, participate in pickup/return inspections, dispute deposit deductions (48h window), view own payments and verification records. |
| **VEHICLE_CONTRIBUTOR** | Submit vehicle onboarding applications, review and sign partner proposals, view vehicle documents and agreements, participate in direct handover/return inspections, acknowledge direct handovers, view earnings and settlement statuses (with UTR references), acknowledge or dispute direct partner settlements, request vehicle withdrawal. |
| **RENTAL_PARTNER (VERIFIED)** | Maintain verified business profile and documents, manage partner hubs and delivery zones, register independently owned fleet, review contributor submissions, propose/amend agreements, manage inventory, control rental listings and prices (subject to price floor), conduct inspections, manage booking lifecycle, report damage incidents, record direct contributor settlements with UTR (Approach A). |
| **ADMIN** | Verify partners, contributors, and vehicles; manage commission rules; arbitrate deposit disputes and damage claims; audit double-entry general ledger; oversee compliance and view audit logs. Admin access is permission-controlled and audited. |

---

## 5. Main Operational & Booking Workflows

### 5.1 Booking Hold & Pessimistic Concurrency
1. Customer selects dates, vehicle category, and pickup method (hub pickup, owner direct, or delivery zone).
2. The search engine evaluates availability considering rental intervals plus operational turnaround buffers (`turnaround_buffer_minutes`).
3. When the customer enters checkout, the system acquires an exclusive row lock (`SELECT id FROM vehicles WHERE id = :id FOR UPDATE;`) and creates a **`booking_holds` record (15-minute TTL)**.
4. **Hold Isolation:** Creating a booking hold **does NOT create an incomplete or draft record in the `bookings` table**.
5. If the hold expires or is released, the slot is immediately freed for other customers.

### 5.2 Payment Order, Capture & Late Webhook Protocol
1. A `payment_orders` entity is generated with Razorpay using an idempotency key.
2. Upfront collection presents the taxable rental charge, delivery fee, and refundable security deposit as distinct line items.
3. Upon successful payment transaction capture, a webhook is delivered to `payment_webhook_events`.
4. **Late Webhook Protocol (Payment Arriving After Hold Expiration):**
   - Webhook handler verifies signature and deduplicates on `provider_event_id`.
   - Acquires fresh lock: `SELECT id FROM vehicles WHERE id = :id FOR UPDATE;`.
   - **Booking Recovery:** If the requested interval plus turnaround buffer remains completely available, the system reactivates the hold, creates the `bookings` record in `CONFIRMED` status, and sends a booking confirmation notification to the customer.
   - **Automatic Full Refund:** If the requested interval has been reserved by another customer in the interim, the system **automatically triggers a 100% gateway refund**, logs an incident, and alerts the customer and admin.

### 5.3 Operational Booking Fulfillment vs. Financial Completion
1. **Pickup Handover:** Vehicle is inspected (`inspections`), odometer and fuel/battery are recorded with photos (`inspection_media`), dual signatures/OTPs are exchanged, and a `vehicle_custody_events` record is posted (`CUSTOMER_CUSTODY`). Booking transitions to `IN_PROGRESS`.
2. **Return Handover:** Vehicle is returned to partner hub or contributor, post-rental inspection checklist is executed, condition photos are captured, and custody is restored (`PARTNER_CUSTODY` or `CONTRIBUTOR_CUSTODY`).
3. **Operational Completion:** Once return inspection is signed and custody is restored, the booking transitions to **`COMPLETED` immediately**.
4. **Decoupled Financial Lifecycles:** Operational completion triggers contributor payable eligibility in `contributor_payables`. It does **not** depend on whether deposit refunds, damage disputes, or partner bank disbursements have completed.

### 5.4 Security Deposit Management & Dispute Arbitration
1. **Liability Segregation:** Security deposits are tracked as a **refundable customer liability**, never as platform revenue or gross turnover.
2. **Clean Return:** If the return inspection logs zero damage, fuel deficit, or late penalties, a 100% gateway refund is automatically triggered.
3. **Damage Deduction Claims:** If damage or excess mileage is reported, the partner submits a `deposit_deductions` claim with mandatory photo evidence and repair estimates.
4. **Immediate Release of Undisputed Balance:** If the deposit is ₹5,000 and the claim is ₹1,500, the uncontested ₹3,500 is **immediately refunded** to the customer.
5. **48-Hour Dispute Window:** The customer is notified and has 48 hours to accept or dispute the claim.
6. **No Automatic Approval:** **A disputed damage claim is NEVER automatically approved merely because the 48-hour response window expires.**
   - If disputed by customer $\rightarrow$ Transitions to `DISPUTED` and enters `ADMIN_ARBITRATION`. Admin evaluates pre-rental vs. post-rental evidence, GPS logs, and repair estimates before deciding.
   - If customer fails to respond $\rightarrow$ Transitions to `PENDING_ADMIN_REVIEW`. Admin verifies partner evidence before approving any deduction from the deposit liability.

### 5.5 Cancellation and No-Show Policies
1. **Pre-Pickup Cancellation:**
   - Customers may cancel prior to key handover. The system applies the policy snapshotted in `bookings.cancellation_policy_snapshot`.
   - Security deposits are **100% refunded** upon any cancellation because physical custody was never transferred.
   - If cancelled by the rental partner or platform (e.g. unexpected vehicle breakdown), the customer receives a **100% full refund** (rental fee + deposit) plus a customer inconvenience credit where applicable.
2. **Customer No-Show:**
   - If a customer fails to arrive for vehicle pickup within the 2-hour grace period, the partner or admin marks the booking as `NO_SHOW`.
   - The vehicle calendar is immediately freed for new reservations.
   - The rental fee is retained as liquidated damages per cancellation policy snapshot, while the **security deposit is refunded in full (100%)** via the refund pipeline.

---

## 6. Financial Architecture & Contributor Settlement

### 6.1 Customer Payment, Deposit Custody, and Partner Settlement Demarcation
To eliminate legal and regulatory ambiguity, operational roles and financial responsibilities are clearly demarcated:
- **Merchant of Record (MoR):** The Platform Operating Entity acts as the digital marketplace MoR for customer checkout transactions on the web/app. It collects total customer payments, issues platform facilitation tax invoices, and contracts with RBI-authorized payment aggregators. *(Pending legal validation regarding GST Section 9(5) E-Commerce Operator classification).*
- **Customer Payment Recipient:** Captured via payment gateway into platform clearing (`GATEWAY_CLEARING`). Rental charges are held in `CUSTOMER_RENTAL_CLEARING` and security deposits in `CUSTOMER_REFUNDABLE_LIABILITY`.
- **Security Deposit Holder:** Held exclusively by the platform as a **Refundable Balance Sheet Liability**. Deposits are never treated as platform revenue, working capital, or an unregulated private "escrow".
- **Customer Refund Responsibility:** The platform bears sole operational and contractual responsibility for processing customer refunds (cancellations, clean return deposit releases, dispute adjustments, late hold collision refunds) directly to original payment instruments via gateway APIs (`REFUND_PAYABLE`). Partners never refund customers directly.
- **Partner Rental Settlement:** The platform is contractually obligated to disburse net rental proceeds (`PARTNER_PAYABLE`) to verified partners on a scheduled batch cycle (e.g. T+2 post return) via payout disbursement APIs (`SETTLEMENT_CLEARING`). Net proceeds equal gross rental subtotal minus platform commission, applicable gateway fees, partner-funded discounts, and mandated tax withholdings (TDS/TCS).
- **Payment Provider Responsibilities:** PCI-DSS compliance, UPI intent/collect flows, fraud checks, instant webhook callbacks, automated refund execution, and scheduled bank settlement into platform clearing accounts per RBI guidelines.
- **Method-Independent Gateway Architecture:** Payment provider integrations are abstracted behind provider-agnostic interfaces (`PaymentGatewayClient`, `PayoutGatewayClient`). The database schema, state machines, and double-entry ledger remain decoupled from any single proprietary gateway mechanism.
- **Independence from Approach A:** Direct partner-to-contributor settlement under Approach A applies **strictly and exclusively** to the private bilateral commercial contract between the Rental Partner and the Vehicle Contributor. **Approach A does NOT apply to, nor does it establish regulatory compliance for, the separate customer payment collection and partner rental settlement flow.**

### 6.2 Strict Separation of Financial Domains
1. **Platform General Ledger (`ledger_accounts`, `ledger_transactions`, `ledger_entries`):**
   - Tracks actual platform cash, customer refundable liabilities, platform commission revenue, net partner payables, and partner receivables.
   - Direct payments by partners to contributors are **never represented as platform cash disbursements**.
2. **Partner-Contributor Contractual Obligation Model (`contributor_payables`):**
   - The rental partner is the sole contractual debtor for contributor fixed earnings.
   - The platform calculates the payable, tracks eligibility, records due dates, and stores direct partner settlement evidence (UTR number, date, receipt).
   - This obligation is **not** a platform legal liability and does not affect the platform general ledger.
3. **Approach B (Razorpay Route):**
   - Remains a future enhancement marked **PENDING** partner linked-account onboarding, KYC workflows, and RBI payment aggregator regulatory review.

### 6.3 Contributor Settlement Architecture: Selected Single Lump-Sum Settlement Model
To preserve the approved 54-table baseline and prevent state synchronization defects, the architecture formally adopts:
**One complete direct settlement per contributor payable, with immutable payment evidence (Single Lump-Sum Model).**
- **Indivisible Booking Obligation:** Each completed booking generates exactly one contributor payable record representing the agreed fixed compensation ($K_{contrib}$). Partial disbursements, installment transfers, or split payments are prohibited at the domain and database levels for single vehicle bookings.
- **Immutable Payment Evidence:** When the partner executes the direct bank transfer (via NEFT/IMPS/UPI) outside the platform, the partner submits:
  1. Bank UTR / Transaction Reference Number (`payout_reference`, VARCHAR 100, mandatory, unique per payable)
  2. Exact settlement timestamp (`paid_at`, DATETIME)
  3. Transfer receipt document (`payment_receipt_url`, object key)
- **Immutability Invariant:** Once transitioned to `SETTLED_BY_PARTNER`, settlement reference fields are locked and immutable.
- **Dispute and Audit Resolution:** If the contributor disputes the transfer (`DISPUTED`), Admin Support arbitrates using the bank UTR evidence. If the transfer was deficient or invalid, the partner executes a corrective transfer under administrative supervision and updates the record via audited administrative events, avoiding the complexity of a 55th entity (`contributor_settlement_events`).

### 6.4 Financial Reversals and Partner Receivable Recovery
When an incorrect partner credit has already been disbursed to the partner's external bank account, the platform executes a balanced, double-entry reversal:
1. **Creation of Partner Receivable:** The platform creates a balanced transaction debiting `PARTNER_RECEIVABLE` (Asset) and crediting `REFUND_PAYABLE` (Liability), allowing immediate customer refund execution without negative balances.
2. **Recovery Option A (Automated Payout Offset):** The outstanding receivable is automatically deducted from the partner's net rental proceeds in the subsequent settlement batch.
3. **Recovery Option B (Direct Repayment):** If no upcoming bookings exist, the partner repays the platform via direct bank transfer or gateway payment link.

### 6.5 Contributor Price-Floor Engine
For `CONTRIBUTOR_OWNED` vehicles, backend validation enforces a mathematical price floor:

$$\text{Base Rental Price} \ge \frac{\text{Contributor Fixed Fee} + \text{Partner Min Margin} + \text{Allocable Fixed Costs}}{1 - (\text{Platform Commission \%} + \text{Gateway Fee \%} + \text{Effective Tax \%})}$$

### 6.6 Separation of Financial Components & Tax Disclaimer
The platform cleanly separates:
1. **Taxable Rental Amount:** Base rental fee subject to GST.
2. **Platform Commission:** Marketplace facilitation fee earned by platform.
3. **Gateway Fees:** Payment processing fee charged by provider.
4. **Fixed Contributor Compensation:** Contractual obligation owed by partner to owner.
5. **Partner Margin:** Residual gross profit earned by partner.
6. **Discount Funding:** Explicit tracking of whether promotions are platform-funded or partner-funded.

> [!CAUTION]
> **No Hardcoded GST Percentage:** Tax calculations must **never** be hardcoded as a simple deduction from rental revenue. Indian GST rules differentiate between Goods Transport, Rent-a-Cab, and E-commerce Marketplace Facilitation (Section 9(5) CGST Act). Tax rates, HSN/SAC codes, and GST splits are fully configurable and marked **PENDING professional tax validation**.

---

## 7. Configurable Operational Defaults (NOT Hardcoded)

The following operational parameters are stored as configurable database settings and environment properties, **never hardcoded into application logic**:

| Parameter Code | Initial Default | Description |
|---|---|---|
| `hold_duration_minutes` | **15 minutes** | Temporary reservation lock duration during checkout. |
| `turnaround_car_minutes` | **60 minutes** | Operational cleaning, refueling, and inspection turnaround buffer for cars. |
| `turnaround_bike_minutes` | **30 minutes** | Operational turnaround buffer for motorcycles. |
| `turnaround_scooter_minutes`| **30 minutes** | Operational turnaround buffer for scooters. |
| `contributor_settlement_mode`| **`DIRECT_PARTNER_SETTLED`** | Approach A direct partner settlement with UTR entry. |
| `withdrawal_notice_days` | **7 days** | Recommended advance notice for contributor vehicle reclamation (confirmed bookings retain strict precedence). |
| `dispute_window_hours` | **48 hours** | Customer response window for proposed deposit deductions. |
| `grace_period_late_minutes`| **30 minutes** | Grace window before overdue penalties and support escalation apply. |
