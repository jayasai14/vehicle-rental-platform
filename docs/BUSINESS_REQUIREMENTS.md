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
│      │                                            │                          │         │
│      │<── 8. Booking CONFIRMED (if verified) ─────│ (Converts hold)          │         │
│      │       (OR Rejection notice with reason)    │                          │         │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

#### Detailed Step-by-Step Flow:
1. **Booking Hold Request:** Customer selects dates and pickup options. The system acquires a pessimistic lock (`SELECT ... FOR UPDATE`) and creates a temporary `booking_holds` record (15-minute TTL).
2. **Display Partner Payment Instructions:** The checkout interface displays the managing partner's payment profile (`partner_payment_profiles`):
   - Legal trade / payee name.
   - Partner's UPI ID.
   - Dynamic/static UPI QR code image (retrieved via secure presigned URL).
   - Optional external payment URL (if configured).
   - Pay-at-pickup option (only if explicitly enabled by the partner).
   - Exact itemized demand: Rental Subtotal + Security Deposit.
3. **Direct External Transfer:** The customer transfers the exact amount directly to the partner using their UPI app, net banking, or external payment link.
4. **Payment Submission:** The customer submits the transaction reference via `POST /api/v1/bookings/holds/{holdId}/submit-payment`:
   - Payment method (`UPI_QR`, `UPI_ID`, `PAYMENT_LINK`, `CASH_AT_PICKUP`).
   - Transaction Reference / Bank UTR number (mandatory for electronic transfers).
   - Amount submitted.
   - Transaction timestamp.
   - Optional payment screenshot or receipt attachment.
5. **Pending Verification Status:**
   - The platform creates a `rental_payment_submissions` record in `SUBMITTED_FOR_VERIFICATION` status.
   - The underlying `rental_payment_demands` updates to `SUBMITTED`.
   - The vehicle hold remains **strictly locked** while an active verification window (configurable default: 60 minutes) is open.
   - The managing partner receives an immediate push/SMS/email alert to verify the incoming payment.
6. **Independent Partner Audit:** The partner inspects their own UPI app, bank statement, or cash register to verify credit.
7. **Partner Verification Gate (Non-Negotiable Invariant):**
   - **A customer-uploaded screenshot, receipt, or UTR must NEVER automatically mark a payment as successful or confirm a booking.**
   - **ONLY explicit partner verification** can transition the submission to `VERIFIED`.
   - Partner submits verification decision:
     - **Action A: VERIFY** (`POST /api/v1/partner/payment-submissions/{id}/verify`): Confirms receipt of required funds.
     - **Action B: REJECT** (`POST /api/v1/partner/payment-submissions/{id}/reject`): Rejects with mandatory reason (`UTR_NOT_FOUND`, `AMOUNT_MISMATCH`, `PAYMENT_DECLINED`).
8. **Confirmation / Failure Outcomes:**
   - **Upon Verification:** `rental_payment_demands` transitions to `VERIFIED`. `booking_holds` transitions to `CONVERTED`. A master `bookings` contract is created in `CONFIRMED` status. A `security_deposits` record is created in `HELD_BY_PARTNER` status. Customer is notified immediately.
   - **Upon Rejection:** `rental_payment_submissions` transitions to `REJECTED`. Customer is alerted with the rejection reason. Customer may submit a corrected UTR within the remaining hold window, or the hold expires.
   - **Pay-at-Pickup Authorized Flow:** If the partner allows cash/UPI at vehicle pickup, the hold converts to `CONFIRMED` with payment marked `PENDING_AT_PICKUP`. **Vehicle handover inspection is strictly blocked** until the partner logs verified collection of rental charges and security deposit at pickup.

#### Exception & Edge Scenario Handling:
- **Hold Expiration without Submission:** If customer does not submit payment within hold TTL (15m), hold transitions to `EXPIRED`. Vehicle calendar is freed immediately.
- **Partner Verification Timeout / Non-Response:** If partner does not verify within the verification window (60m):
  - System does NOT auto-confirm.
  - Escalates alert to Partner and Support/Admin.
  - If timeout elapses without action, hold is released and customer is flagged for partner direct refund.
- **Duplicate Reference Prevention:** Database constraint `uk_pay_submissions_ref` on (`payee_partner_id`, `transaction_reference`) prevents the same UTR from being submitted to the same partner twice.
- **Payment Amount Mismatch:** If the customer transfers less than required, partner rejects submission with reason `INSUFFICIENT_AMOUNT`.
- **Late Payment (Payment submitted after hold expiry):** If customer pays externally but submits UTR after hold expired:
  - System re-checks vehicle availability. If available, customer can request hold reactivation and submit.
  - If vehicle interval was booked by another customer in the interim, booking cannot be confirmed. System flags a payment mismatch exception, instructs customer to contact partner, and prompts partner to refund the transfer directly.

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

### 5.5 Cancellation and No-Show Policies
1. **Pre-Pickup Cancellation:**
   - Customers may cancel prior to key handover. The system applies the policy snapshotted in `bookings.cancellation_policy_snapshot`.
   - Security deposits are **100% refunded by the partner** because physical custody was never transferred.
   - If cancelled by the rental partner (e.g. vehicle breakdown), the partner must issue a **100% full refund** (rental fee + deposit).
2. **Customer No-Show:**
   - If a customer fails to arrive for vehicle pickup within the 2-hour grace period, the partner marks the booking as `NO_SHOW`.
   - The vehicle calendar is immediately freed for new reservations.
   - Rental fees are retained as liquidated damages per cancellation policy snapshot, while the **security deposit must be refunded in full (100%) by the partner**.

---

## 6. Financial Architecture & Responsibilities (Partner-Direct Model)

### 6.1 Legal & Operational Responsibility Demarcation

In the MVP, the marketplace operates as an operational record-keeping and booking facilitator:

| Responsibility Domain | Responsible Entity | Operational & Platform Implementation |
|---|---|---|
| **Rental Service Provider & Payee** | **Rental Partner** | Customer-facing rental contractor and direct payee for all bookings on managed vehicles. Receives rental fees and deposits directly via partner UPI QR/ID/Cash. |
| **Payment Verification** | **Rental Partner** | Independently checks bank/UPI credits and confirms or rejects submitted payment references. |
| **Deposit Custody & Refunds** | **Rental Partner** | Holds security deposits and executes refunds directly to customer bank accounts. Logs Bank UTR and proof on platform. |
| **Booking & Record Keeping** | **Platform** | Manages calendar availability, temporary reservation holds, payment instructions, submission workflows, and immutable audit logs. |
| **Dispute Resolution** | **Platform Admin** | Arbitrates contested damage claims, payment disputes, and deposit deduction discrepancies based on inspection evidence. |
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
