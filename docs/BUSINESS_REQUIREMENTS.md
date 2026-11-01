# Business Requirements (Pre-Freeze Specification)

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh (Expandable)  
**Status:** Pre-Freeze Corrected Baseline  

---

## 1. Product Scope

A modern vehicle rental marketplace launching initially in Vijayawada, Andhra Pradesh, supporting cars, bikes, and scooters. The platform combines conventional rental partner fleet inventory with an innovative vehicle contribution model allowing individual vehicle owners to monetize idle vehicles through verified rental partners.

---

## 2. Dual Inventory Channels & Ownership Architecture

The platform strictly differentiates between **Legal Ownership**, **Operational Management**, and **Physical Custody**:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              INVENTORY ARCHITECTURE                                    │
│                                                                                        │
│   ┌────────────────────────────────┐         ┌─────────────────────────────────────┐   │
│   │     PARTNER-OWNED FLEET        │         │      CONTRIBUTOR-OWNED FLEET        │   │
│   ├────────────────────────────────┤         ├─────────────────────────────────────┤   │
│   │ Legal Owner: Rental Partner    │         │ Legal Owner: Individual Contributor │   │
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

### 2.1 Partner-Owned Vehicles
- Registered independently by verified rental partners.
- Begins in `PARTNER_CUSTODY` upon verification and assignment to a verified partner hub (`partner_locations`).
- Partner manages listing status, pricing, availability blocks, maintenance, and turnarounds.
- Excluded from contributor price-floor constraints.

### 2.2 Contributor-Owned Vehicles
- Submitted by verified individual owners; reviewed and accepted by a rental partner.
- Begins in `CONTRIBUTOR_CUSTODY` upon owner registration; transitions to `PARTNER_CUSTODY` upon hub onboarding, or remains in `CONTRIBUTOR_CUSTODY` for owner-managed handovers.
- Governed by a legally binding `contribution_agreements` contract specifying:
  - Agreed fixed compensation per eligible completed booking.
  - Settlement payment mode (Approach A baseline).
  - Cancellation compensation rate (`cancellation_compensation_rate`).
  - Permitted availability schedule and withdrawal notice period (recommended 7 days).
  - Inspection and handover protocols (partner-managed vs. owner-managed).
- **Sole Contractual Debtor:** The **rental partner is the sole contractual debtor** for contributor fixed earnings. The platform calculates, records, and audits the obligation but is never the legal debtor.
- **Booking Precedence:** Confirmed customer bookings strictly supersede contributor withdrawal notices or availability blackout blocks. Return to contributor occurs only after the final confirmed booking ends.
- **Single Active Manager Invariant:** A vehicle cannot have more than one active contribution agreement simultaneously.

---

## 3. Vehicle Custody Architecture (Independent of Ownership & Bookings)

Physical vehicle custody is tracked as an independent state machine (`vehicle_custody_events`) supporting both **partner-managed** and **owner-managed** handovers across 8 canonical permutations:

1. `CONTRIBUTOR_TO_PARTNER`: Contributor delivers vehicle to partner hub for fleet onboarding.
2. `PARTNER_TO_CUSTOMER`: Standard customer pickup at partner hub.
3. `CONTRIBUTOR_TO_CUSTOMER`: Direct customer pickup at contributor residence (owner-managed handover).
4. `CUSTOMER_TO_PARTNER`: Standard customer return to partner hub.
5. `CUSTOMER_TO_CONTRIBUTOR`: Direct customer return to contributor residence (owner-managed return).
6. `PARTNER_TO_CONTRIBUTOR`: Partner returns vehicle to contributor upon agreement termination or withdrawal.
7. `INTER_HUB_TRANSFER`: Partner relocates vehicle between two operational depots in the city.
8. `LOGISTICS_RELOCATION`: Authorized representative/logistics driver handles doorstep delivery or recovery.

---

## 4. Roles and Core Capabilities

| Role | Core Capabilities |
|---|---|
| **CUSTOMER** | Search, compare, create 15-min booking holds, confirm bookings via payment, manage own bookings, request extensions, participate in pickup/return inspections, dispute deposit deductions (48h window), view own payments and verification records. |
| **VEHICLE_CONTRIBUTOR** | Submit vehicle onboarding applications, review and sign partner proposals, view vehicle documents and agreements, participate in direct handover/return inspections, acknowledge direct handovers, view earnings and settlement statuses (with UTR references), acknowledge or dispute direct settlements, request vehicle withdrawal. |
| **RENTAL_PARTNER** | Maintain business profile and documents, manage partner hubs and delivery zones, register independently owned fleet, review contributor submissions, propose/amend agreements, manage inventory, control rental listings and prices (subject to price floor), conduct inspections, manage booking lifecycle, report damage incidents, record direct contributor settlements with UTR (Approach A). |
| **ADMIN** | Verify partners, contributors, and vehicles; manage commission rules; arbitrate deposit disputes and damage claims; audit double-entry ledger; oversee compliance and view audit logs. Admin access is permission-controlled and audited. |

---

## 5. Main Operational & Booking Workflows

### 5.1 Booking Hold & Pessimistic Concurrency
1. Customer selects dates, vehicle category, and pickup method (hub pickup, owner direct, or delivery zone).
2. The search engine evaluates availability considering rental intervals plus operational turnaround buffers (`turnaround_buffer_minutes`).
3. When the customer enters checkout, the system acquires an exclusive row lock (`SELECT id FROM vehicles WHERE id = :id FOR UPDATE;`) and creates a **`booking_holds` record (15-minute TTL)**.
4. **Hold Isolation:** Creating a booking hold **does NOT create an incomplete or draft record in the `bookings` table**.
5. If the hold expires or is released, the slot is immediately freed for other customers.

### 5.2 Payment Order, Capture & Delayed Webhook Handling
1. A `payment_orders` entity is generated with Razorpay using an idempotency key.
2. Upfront collection presents the taxable rental charge, delivery fee, and refundable security deposit as distinct line items.
3. Upon successful payment transaction capture, a webhook is delivered to `payment_webhook_events`.
4. **Delayed Webhook Recovery:** If a successful payment webhook arrives after a hold has expired:
   - If the vehicle interval remains completely available, the system reactivates the hold and creates the `CONFIRMED` booking.
   - If the requested interval has been reserved by another customer in the interim, the system **automatically triggers a 100% gateway refund**, logs an incident, and alerts the customer and admin.

### 5.3 Operational Booking Fulfillment vs. Financial Completion
1. **Pickup Handover:** Vehicle is inspected (`inspections`), odometer and fuel/battery are recorded with photos (`inspection_media`), dual signatures/OTPs are exchanged, and a `vehicle_custody_events` record is posted (`CUSTOMER_CUSTODY`). Booking transitions to `IN_PROGRESS`.
2. **Return Handover:** Vehicle is returned to partner hub or contributor, post-rental inspection checklist is executed, condition photos are captured, and custody is restored (`PARTNER_CUSTODY` or `CONTRIBUTOR_CUSTODY`).
3. **Operational Completion:** Once return inspection is signed and custody is restored, the booking transitions to **`COMPLETED` immediately**.
4. **Decoupled Financial Lifecycles:** Operational completion triggers contributor payable eligibility. It does **not** depend on whether deposit refunds, damage disputes, or partner bank settlements have completed.

### 5.4 Security Deposit Management & Dispute Arbitration
1. **Liability Segregation:** Security deposits are tracked as a **refundable customer liability**, never as platform revenue or gross turnover.
2. **Clean Return:** If the return inspection logs zero damage, fuel deficit, or late penalties, a 100% gateway refund is automatically triggered.
3. **Damage Deduction Claims:** If damage or excess mileage is reported, the partner submits a `deposit_deductions` claim with mandatory photo evidence and repair estimates.
4. **Immediate Release of Undisputed Balance:** If the deposit is ₹5,000 and the claim is ₹1,500, the uncontested ₹3,500 is **immediately refunded** to the customer.
5. **48-Hour Dispute Window:** The customer is notified and has 48 hours to accept or dispute the claim.
6. **No Automatic Approval:** **A disputed damage claim is NEVER automatically approved merely because the 48-hour response window expires.**
   - If disputed by customer: Transitions to `DISPUTED` and enters `ADMIN_ARBITRATION`. Admin evaluates pre-rental vs. post-rental evidence, GPS logs, and repair estimates before deciding.
   - If customer fails to respond: Transitions to `PENDING_ADMIN_REVIEW`. Admin verifies partner evidence before approving any deduction from the deposit liability.

---

## 6. Financial Architecture & Contributor Settlement

### 6.1 Settlement Approaches
- **Approach A (Direct Partner Settlement - Approved MVP Baseline):**
  - The rental partner is the sole legal debtor for the contributor's fixed payment.
  - The platform calculates the payable, records the obligation against the partner, provides a settlement interface, stores the partner's payment reference/UTR and date, tracks settlement status, allows contributor confirmation or dispute, and provides administrative oversight.
  - The platform does not hold, route, or assume legal liability for contributor funds.
- **Approach B (Razorpay Route - Future Option PENDING Validation):**
  - Gateway automatically splits incoming rental proceeds directly to contributor bank accounts.
  - Marked **PENDING** partner linked-account onboarding, KYC workflows, and RBI payment aggregator regulatory review.

### 6.2 Contributor Price-Floor Engine
For `CONTRIBUTOR_OWNED` vehicles, backend validation enforces a mathematical price floor:

$$\text{Base Rental Price} \ge \frac{\text{Contributor Fixed Fee} + \text{Partner Min Margin} + \text{Allocable Fixed Costs}}{1 - (\text{Platform Commission \%} + \text{Gateway Fee \%} + \text{Effective Tax \%})}$$

### 6.3 Separation of Financial Components & Tax Disclaimer
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

## 7. Proposed Operational Defaults (Configurable, NOT Hardcoded)

The following operational parameters must be stored as configurable database settings and environment properties, **never hardcoded into application logic**:

| Parameter Code | Proposed Initial Default | Description |
|---|---|---|
| `hold_duration_minutes` | **15 minutes** | Temporary reservation lock duration during checkout. |
| `turnaround_car_minutes` | **60 minutes** | Operational cleaning, refueling, and inspection turnaround buffer for cars. |
| `turnaround_bike_minutes` | **30 minutes** | Operational turnaround buffer for motorcycles. |
| `turnaround_scooter_minutes`| **30 minutes** | Operational turnaround buffer for scooters. |
| `contributor_settlement_mode`| **`DIRECT_PARTNER_SETTLED`** | Approach A direct partner settlement with UTR entry. |
| `withdrawal_notice_days` | **7 days** | Recommended advance notice for contributor vehicle reclamation, subject to confirmed bookings. |
| `dispute_window_hours` | **48 hours** | Customer response window for proposed deposit deductions. |
| `grace_period_late_minutes`| **30 minutes** | Grace window before overdue penalties and support escalation apply. |
