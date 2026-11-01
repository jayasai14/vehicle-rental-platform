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

### 5.2 Partner-Direct Customer Rental Payment & Verification Workflow

The MVP adopts a **Partner-Direct Payment Collection Model**. The platform does not collect, hold, escrow, split, or disburse customer rental money or security deposits.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        PARTNER-DIRECT CUSTOMER PAYMENT WORKFLOW                        │
│                                                                                        │
│  [Customer]                                  [Platform]               [Rental Partner] │
│      │                                            │                          │         │
│      │── 1. Selects vehicle & dates ─────────────>│                          │         │
│      │                                            │ (Acquires 15-min hold)   │         │
│      │<── 2. Displays Partner Payment Info ───────│ (Fetches QR/UPI details) │         │
│      │       (Payee Name, UPI ID, QR, Cash option)│                          │         │
│      │                                            │                          │         │
│      │── 3. Transfers funds directly to Partner ────────────────────────────>│         │
│      │       (Outside platform via UPI/NetBanking/Cash)                      │ (Funds  │
│      │                                            │                          │ received│
│      │── 4. Submits Transaction Reference (UTR) ─>│                          │ in bank)│
│      │       (Method, amount, time, proof)        │                          │         │
│      │                                            │── 5. Notifies Partner ──>│         │
│      │                                            │    (SUBMITTED_FOR_VERIFY)│         │
│      │                                            │                          │── 6. Checks bank/UPI
│      │                                            │<── 7. VERIFIES / REJECTS │      records
│      │                                            │                          │            - Optional payment screenshot or receipt attachment.
5. **Pending Verification Status & Demand Separation:**
   - The platform creates a `rental_payment_submissions` record in `SUBMITTED_FOR_VERIFICATION` status.
   - The parent `rental_payment_demands` remains in `OPEN` (or `PARTIALLY_SATISFIED` if prior verified payments exist).
   - The vehicle hold remains **strictly locked** while an active verification window (configurable default: 60 minutes) is open (`booking_holds.status = VERIFICATION_PENDING`).
   - The managing partner receives an immediate push/SMS/email alert to verify the incoming payment submission.
6. **Independent Partner Audit:** The partner inspects their own UPI app, bank statement, or cash register to verify credit.
7. **Partner Verification Gate (Non-Negotiable Invariant):**
   - **A customer-uploaded screenshot, receipt, or UTR must NEVER automatically mark a payment as successful or confirm a booking.**
   - **ONLY explicit partner verification** can transition the submission to `VERIFIED`.
   - Partner submits verification decision:
     - **Action A: VERIFY** (`POST /api/v1/partner/payment-submissions/{id}/verify`): Confirms receipt of funds.
       * The verified amount is added to `rental_payment_demands.amount_satisfied`.
       * If `amount_satisfied < amount_demanded`, the demand transitions to `PARTIALLY_SATISFIED`. The customer can submit top-up submissions for the outstanding balance.
       * If `amount_satisfied >= amount_demanded`, the demand transitions to `SATISFIED`.
       * If `amount_satisfied > amount_demanded` (overpayment), the demand transitions to `SATISFIED`, and the platform automatically creates an overpayment refund obligation in `rental_fee_refunds` (`refund_reason = OVERPAYMENT_RETURN`).
     - **Action B: REJECT** (`POST /api/v1/partner/payment-submissions/{id}/reject`): Rejects with mandatory reason (`UTR_NOT_FOUND`, `AMOUNT_MISMATCH`, `PAYMENT_DECLINED`). The submission transitions to `REJECTED`. The demand remains `OPEN` or `PARTIALLY_SATISFIED`. Customer is alerted to submit a corrected payment.
8. **Booking Confirmation Invariant:**
   A booking can be confirmed ONLY when all of the following conditions are met simultaneously:
   1. **Valid Calendar Availability & Vehicle Assignment:** Re-verified under an exclusive vehicle row lock (`SELECT id FROM vehicles WHERE id = :id FOR UPDATE`).
   2. **Demand Satisfaction:** Applicable rental payment demands are `SATISFIED` through accumulated verified submissions, OR an explicit partner-approved pay-at-pickup policy permits the required unpaid balance.
   3. **Deposit Policy Satisfied:** Any required security-deposit policy is satisfied (either pre-collected or flagged for pickup verification).
   4. **Active Lock Window:** The booking hold is still valid (`status IN ('ACTIVE', 'VERIFICATION_PENDING')`) or the booking is otherwise safely revalidated under the vehicle lock.
   - **Strict Concurrency Rule:** An expired hold must **NEVER** be silently converted to a confirmed booking.
   - **Pay-at-Pickup Handover Gate:** For pay-at-pickup bookings, the booking is confirmed with payment status `PENDING_AT_PICKUP`, but **physical vehicle handover is strictly blocked** in the system until the partner logs verified collection of both rental charges and security deposit at vehicle handover.

#### Exception & Edge Scenario Handling:
- **Hold Expiration without Submission:** If customer does not submit payment within hold TTL (15m), hold transitions to `EXPIRED`. Vehicle calendar is freed immediately.
- **Hold Expiration with Partial Payments:** If a hold expires while a demand is `PARTIALLY_SATISFIED`, the hold is released, the demand transitions to `EXPIRED`, and the partner is legally and operationally obligated to refund the accumulated verified partial payments via `rental_fee_refunds` (`refund_reason = LATE_PAYMENT_EXPIRED_HOLD`).
- **Late Payment (Payment submitted after hold expiry or verified after expiry):**
  - If a customer transfers money externally but submits UTR after the hold expired, or if the partner verifies payment after the hold expired:
  - The system acquires the vehicle row lock and checks calendar availability.
  - If the vehicle interval was booked by another customer in the interim, the booking **cannot be confirmed**. The system strictly forbids silent conversion.
  - The platform creates an exception record in `rental_fee_refunds` with reason `LATE_PAYMENT_EXPIRED_HOLD`, binding the managing partner to return 100% of the funds directly to the customer.
- **Partner Verification Timeout / Non-Response:** If partner does not verify within the verification window (60m):
  - System does NOT auto-confirm.
  - Escalates alert to Partner and Support/Admin.
  - If timeout elapses without action, hold is released and customer is flagged for partner direct refund via `rental_fee_refunds`.
- **Duplicate Reference Prevention:** Database constraint `uk_submissions_partner_ref` on `(manager_partner_id, transaction_reference)` prevents the same UTR from being submitted to the same partner twice.
- **Cash-at-Pickup Evidence:** When cash payment is accepted at pickup, the partner must enter the cash receipt voucher number and cash collection timestamp; the platform validates the managing partner's identity and records the transaction in `rental_payment_submissions` and `audit_logs`.

---

### 5.3 Operational Booking Fulfillment vs. Financial Completion
1. **Pickup Handover:** Vehicle is inspected (`inspections`), odometer and fuel/battery are recorded with photos (`inspection_media`), dual signatures/OTPs are exchanged, and a `vehicle_custody_events` record is posted (`CUSTOMER_CUSTODY`). Booking transitions to `IN_PROGRESS`.
   - *Pay-at-Pickup Check:* Handover cannot proceed unless payment demand and deposit collection are verified by the partner.
2. **Return Handover:** Vehicle is returned to partner hub or contributor, post-rental return inspection checklist is executed, condition photos are captured, and custody is restored (`PARTNER_CUSTODY` or `CONTRIBUTOR_CUSTODY`).
3. **Operational Completion:** Once return inspection is signed and custody is restored, the booking transitions to **`COMPLETED` immediately**.
4. **Decoupled Financial Lifecycles:** Operational completion triggers contributor payable eligibility in `contributor_payables`. It does **not** depend on whether deposit refunds, damage disputes, or customer claims have settled.

---

### 5.4 Security Deposit Management, Deductions & Partner Refunds

In the partner-direct model, **the rental partner directly collects, holds, and returns the customer's security deposit**. The platform does not hold or escrow deposit funds.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        PARTNER-DIRECT SECURITY DEPOSIT LIFECYCLE                       │
│                                                                                        │
│  [Customer Checkout] ──> Paid directly to Partner along with rental fee                │
│                                │                                                       │
│                                ▼                                                       │
│                     Status: HELD_BY_PARTNER                                            │
│                                │                                                       │
│           ┌────────────────────┴────────────────────┐                                  │
│           ▼                                         ▼                                  │
│   [Clean Vehicle Return]                   [Damage / Late / Fuel Claim]                │
│           │                                         │                                  │
│           ▼                                         ▼                                  │
│  Partner sends UPI/Bank                Partner submits deposit_deductions              │
│  refund directly to Customer           with photos and repair estimate                 │
│           │                                         │                                  │
│           ▼                                         ├────────────────────────┐         │
│  Partner records refund reference                   ▼                        ▼         │
│  (UTR, date, proof) in Platform              [Customer Accepts]     [Customer Disputes]│
│           │                                         │                 (48h window)     │
│           ▼                                         ▼                        ▼         │
│  Status: REFUNDED_IN_FULL              Partner deducts agreed       Admin Arbitration  │
│  Customer acknowledges or disputes     amount & refunds residual    (Evaluates media)  │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Deposit Tracking Responsibilities:
1. **Platform Tracking Records:** The platform records expected deposit amount, amount collected, collection method, transaction reference, current status, deductions, refund amounts, and evidence documents.
2. **Clean Return & Immediate Direct Refund:** If post-rental inspection confirms clean vehicle condition, the partner is obligated to execute a direct refund (UPI/IMPS) within 24 hours and log the Bank UTR in `security_deposit_refunds`.
3. **Evidence-Based Damage Claims:** If damage or fuel deficit is reported, the partner submits a `deposit_deductions` claim with mandatory pre/post inspection photos and repair quotes.
4. **Immediate Release of Undisputed Balance:** If the deposit is ₹5,000 and the claim is ₹1,500, the partner must immediately refund the uncontested ₹3,500 and log the payment reference.
5. **48-Hour Dispute Window:** The customer has 48 hours to accept or dispute the proposed deduction.
6. **Non-Negotiable Dispute Invariant:** **A missing customer response must NEVER automatically approve a damage deduction claim.**
   - If disputed by customer $\rightarrow$ Transitions to `CUSTOMER_DISPUTED` and enters `ADMIN_ARBITRATION`. Admin evaluates pre-rental vs. post-rental evidence, GPS logs, and repair estimates before deciding.
   - If customer does not respond $\rightarrow$ Transitions to `PENDING_ADMIN_REVIEW`. Admin verifies partner evidence before approving deduction.
7. **Platform Refund Boundary:** The platform records and monitors refunds, but **does not execute or claim to execute an external bank refund**.

---

### 5.5 Rental-Fee Refund Tracking & Configurable Cancellation Rules

Rental fee refunds are tracked independently in `rental_fee_refunds`. The rental partner directly executes all rental fee refunds outside the platform.

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        PARTNER-DIRECT RENTAL-FEE REFUND LIFECYCLE                      │
│                                                                                        │
│  [Trigger: Cancellation / Overpayment / Expired-Hold Late Payment / Dispute Decision]  │
│                                │                                                       │
│                                ▼                                                       │
│                 Status: PENDING_PARTNER_DISBURSEMENT                                   │
│                                │                                                       │
│                                ▼                                                       │
│  Partner executes direct external transfer (UPI/NEFT/IMPS) and records UTR + receipt   │
│                                │                                                       │
│                                ▼                                                       │
│                     Status: DISBURSED_BY_PARTNER                                       │
│          (Partner UTR entry does NOT mean customer has received funds!)                │
│                                │                                                       │
│           ┌────────────────────┼─────────────────────────┐                             │
│           ▼                    ▼                         ▼                             │
│  [Customer Acknowledges]  [72h Window Expires]   [Customer Disputes (within 72h)]       │
│  (Clicks in UI)           (Zero dispute/ack)     (Funds not received / Short amount)   │
│           │                    │                         │                             │
│           ▼                    ▼                         ▼                             │
│  CUSTOMER_ACKNOWLEDGED   DISPUTE_WINDOW_ELAPSED    Status: DISPUTED_BY_CUSTOMER        │
│                          _UNACKNOWLEDGED                 │                             │
│                                                          ▼                             │
│                                                   Admin Dispute Arbitration            │
│                                                   (support_tickets & bank evidence)     │
│                                                          │                             │
│                                                          ▼                             │
│                                                   Status: ADMIN_RESOLVED               │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Configurable Cancellation & Refund Rules:
1. **Configurable Business Rules (NOT Hardcoded):**
   - Booking cancellation refund calculations are determined dynamically from configurable policy parameters snapshotted in `bookings.cancellation_policy_snapshot` or `rental_fee_refunds.cancellation_policy_snapshot`.
   - Percentage tiers, flat fee deductions, and cancellation notice windows are maintained in system configuration and never hardcoded in source code.
2. **Refund Calculation Data Elements:**
   - **Cancellation Timestamp:** Time cancellation was requested.
   - **Applicable Policy Tier:** Rule applied based on lead time before scheduled pickup.
   - **Gross Amount Paid:** Total verified rental fee received by partner (`amount_satisfied`).
   - **Cancellation Charge Deducted:** Retained fee calculated under the policy snapshot.
   - **Refundable Rental Amount:** Net balance due to customer (`gross_amount_paid - cancellation_charge_deducted`).
3. **Refund Responsibility:**
   - The rental partner who received customer funds is solely responsible for transferring the refundable amount directly to the customer's bank account or UPI ID.
   - The platform never holds customer rental funds and does not execute gateway refunds.
4. **Evidence & Record-Keeping:**
   - The partner logs the external Bank UTR, disbursement timestamp, and receipt image into `rental_fee_refunds`.
   - Unique constraint `uk_rental_refunds_partner_utr` prevents reference recycling across disbursements.
5. **Customer Acknowledgement & Dispute Workflow:**
   - **Strict Acknowledgement Semantics:** Explicit customer acknowledgement in the UI is the ONLY event establishing customer-confirmed receipt (`CUSTOMER_ACKNOWLEDGED`).
   - **Dispute Window Expiry:** If 72 hours elapse with no dispute and no explicit customer acknowledgement, status becomes `DISPUTE_WINDOW_ELAPSED_UNACKNOWLEDGED`. Expiry of the dispute window is NEVER represented as customer acknowledgement.
   - If the customer reports non-receipt or short amount within 72 hours, a support ticket (`support_tickets`) is opened, and status transitions to `DISPUTED_BY_CUSTOMER`.
   - An authorized Admin inspects customer and partner bank proofs before issuing an administrative resolution, recording `dispute_ticket_id`, `resolved_by_admin_user_id`, `resolution_notes`, and `resolved_at` upon transitioning to `ADMIN_RESOLVED`.
6. **Correct Late-Payment Recovery After Hold Expiry:**
   - An expired hold and expired payment demand remain permanently immutable in `EXPIRED`.
   - When a payment arrives or is verified on an expired hold:
     1. Partner confirms the external payment actually arrived.
     2. System records the submission as `VERIFIED` with audit notes. The expired demand is **never reopened**.
     3. System checks vehicle availability under `SELECT ... FOR UPDATE`.
     4. If available, the system creates a fresh hold (`recovered_from_hold_id = original_expired_hold.id`), generates a fresh payment demand, applies the verified payment (`amount_satisfied = verified_amount`), revalidates booking conditions, and confirms the booking.
     5. If the vehicle is unavailable or booking cannot be safely recovered, the system creates a `rental_fee_refunds` obligation (`refund_reason = EXPIRED_HOLD_EXCEPTION`). The partner executes an external refund and records proof.
7. **Overpayment Accounting & Aggregate Refund Limits:**
   - For a verified payment of ₹1,800 against a ₹1,500 demand: `total_verified_submitted = 1800.00`, `amount_satisfied = 1500.00` (capped), and `excess_amount = 300.00`. The system never stores ₹1,800 as satisfying a ₹1,500 obligation. The excess ₹300 triggers an automated refund obligation in `rental_fee_refunds`.
   - Cumulative rental-fee refunds are enforced transactionally under a pessimistic row lock on the parent demand (`SELECT ... FOR UPDATE`). Total active refunds (`PENDING_PARTNER_DISBURSEMENT`, `DISBURSED_BY_PARTNER`, `CUSTOMER_ACKNOWLEDGED`, `DISPUTED_BY_CUSTOMER`, `DISPUTE_WINDOW_ELAPSED_UNACKNOWLEDGED`, `ADMIN_RESOLVED`) cannot exceed the gross refundable balance. Canceled refund records do not consume balance.

---

### 5.6 Pre-Pickup Cancellation & No-Show Policies
1. **Pre-Pickup Cancellation:**
   - Customers may cancel prior to key handover. The system applies the snapshotted cancellation policy.
   - Security deposits are **100% refunded by the partner** via `security_deposit_refunds` because physical custody was never transferred.
   - If cancelled by the rental partner (e.g. vehicle breakdown), the partner must issue a **100% full refund** of both rental fee and security deposit.
2. **Customer No-Show:**
   - If a customer fails to arrive within the 2-hour grace period, the partner marks the booking as `NO_SHOW`.
   - Vehicle calendar is immediately freed for new reservations.
   - Rental fees are retained as liquidated damages per cancellation policy snapshot, while the **security deposit must be refunded in full (100%) by the partner**.

---

## 6. Financial Architecture & Responsibilities (Partner-Direct Model)

### 6.1 Legal & Operational Responsibility Demarcation

In the MVP, the marketplace operates as an operational record-keeping and booking facilitator:

| Responsibility Domain | Responsible Entity | Operational & Platform Implementation |
|---|---|---|
| **Rental Service Provider & Payee** | **Rental Partner** | Customer-facing rental contractor and direct payee for all bookings on managed vehicles. Receives rental fees and deposits directly via partner UPI QR/ID/Cash. |
| **Payment Verification** | **Rental Partner** | Independently checks bank/UPI credits and confirms or rejects submitted payment references. |
| **Rental Fee Refunds** | **Rental Partner** | Executes rental-fee refunds directly to customer bank/UPI outside platform for cancellations, overpayments, expired-hold exceptions, and disputes. Logs Bank UTR and proof on platform. |
| **Deposit Custody & Refunds** | **Rental Partner** | Holds security deposits and executes refunds directly to customer bank accounts. Logs Bank UTR and proof on platform. |
| **Booking & Record Keeping** | **Platform** | Manages calendar availability, temporary reservation holds, payment instructions, submission workflows, and immutable audit logs. |
| **Dispute Resolution** | **Platform Admin** | Arbitrates contested damage claims, payment disputes, rental fee refund disputes, and deposit deduction discrepancies based on inspection evidence. |
| **Contributor Compensation** | **Rental Partner** | Sole contractual debtor for contributor fixed earnings. Pays contributor directly outside platform via external transfer. |

### 6.2 Contributor Compensation (Approach A Baseline)
- Unchanged business model: Rental partner manages vehicle and owes contributor an agreed fixed compensation per eligible completed booking.
- Contributor compensation is an obligation between partner and contributor, tracked in `contributor_payables`.
- Partner pays contributor directly via external NEFT/IMPS/UPI and logs Bank UTR and receipt proof.
- Single lump-sum settlement per eligible booking.
- Direct partner-to-contributor payments **generate zero entries in any platform cash ledger**.

### 6.3 Future Payment Provider Integration (Future Phase)
A future approved marketplace payment integration (e.g. Razorpay Route or Cashfree Easy Split) may be introduced in a subsequent release, subject to:
1. Platform corporate merchant onboarding.
2. Formal legal and tax validation regarding GST Section 9(5) E-Commerce Operator status.
3. Partner sub-merchant linked-account KYC workflows.
4. Automated webhook-driven payment capture and split payouts.
*In the MVP, all customer payments and security deposits remain 100% partner-direct.*

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
