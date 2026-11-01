# Final Architecture Review & Reconciliation Report: Partner-Direct Payment Model

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh (Expandable)  
**Lead Roles:** Lead Software Architect & Database Architect  
**Technology Stack:** React + Vite, Tailwind CSS, Spring Boot, Spring Security, MySQL 8.0+, JWT + BCrypt  
**Status:** **Reconciled & Frozen Database Design Baseline (Partner-Direct Payment Architecture)**  
**Physical Table Baseline:** **47 Physical MySQL Tables for MVP** (45 Application-Domain Tables + 2 Infrastructure-Managed Tables) + **8 Future-Phase Tables** (55 Total Cataloged Entities)  
**Governing Document Set:**  
1. [README.md](../README.md)  
2. [docs/PROJECT_CONSTITUTION.md](PROJECT_CONSTITUTION.md)  
3. [docs/BUSINESS_REQUIREMENTS.md](BUSINESS_REQUIREMENTS.md)  
4. [docs/ARCHITECTURE.md](ARCHITECTURE.md)  
5. [docs/ARCHITECTURE_DECISIONS.md](ARCHITECTURE_DECISIONS.md)  
6. [docs/FINAL_ENTITY_INVENTORY.md](FINAL_ENTITY_INVENTORY.md)  
7. [docs/ENTITY_RELATIONSHIPS.md](ENTITY_RELATIONSHIPS.md)  
8. [docs/STATE_MACHINES.md](STATE_MACHINES.md)  
9. [docs/FINANCIAL_ARCHITECTURE.md](FINANCIAL_ARCHITECTURE.md)  
10. [docs/ROLE_PERMISSION_MATRIX.md](ROLE_PERMISSION_MATRIX.md)  
11. [docs/DATABASE_DESIGN.md](DATABASE_DESIGN.md)  
12. [docs/API_MODULES.md](API_MODULES.md)  
13. [docs/SECURITY.md](SECURITY.md)  
14. [docs/PAGES_AND_ROUTES.md](PAGES_AND_ROUTES.md)  
15. [docs/DEVELOPMENT_WORKFLOW.md](DEVELOPMENT_WORKFLOW.md)  

---

## 1. Final Payment Architecture Decision

The MVP adopts **PARTNER-DIRECT PAYMENT COLLECTION** (formalized in ADR-018):
- **Direct Payee:** The rental partner managing a vehicle is the customer-facing rental service provider and direct payee for that vehicle's bookings.
- **Zero Platform Cash Handling:** The platform does NOT collect, hold, escrow, split, or disburse customer rental money or security deposits in the MVP.
- **Accepted Payment Channels:**
  - Partner's UPI QR code (image displayed at checkout).
  - Partner's UPI ID / VPA (displayed with copy button).
  - Partner's external payment link (if configured).
  - Cash at pickup (only when the partner explicitly enables and accepts this method).
- **No Platform Gateway Account:** No platform-owned Razorpay or payment aggregator merchant account is integrated for customer rental checkout in the MVP.
- **No Marketplace Settlement in MVP:** Razorpay Route, marketplace split settlements, automated partner payouts, and platform general ledger double-entry accounting for customer rentals are deferred to Future Phase.
- **Provider-Agnostic Boundary:** Core payment demand, submission, and verification abstractions remain decoupled from any proprietary gateway vendor.
- **Merchant of Record Demarcation:** The platform does not assume Merchant of Record status. The partner-as-service-provider and partner-as-payee model is documented as an architectural decision subject to legal, tax, and insurance validation before production launch.
- **Pre-Implementation Notice:** All specifications represent the **proposed database and system architecture**. No application code, JPA entities, or Flyway SQL migrations have been generated yet.

---

## 2. Customer Rental Payment Lifecycle

The rental payment lifecycle is redesigned for manual partner-direct collection:

### 2.1 The 10-Step Customer Payment & Verification Flow:
1. **Vehicle Selection & Hold Request:** Customer selects a vehicle, dates, and pickup mode, and initiates checkout.
2. **Temporary Availability Hold:** The system acquires a pessimistic row lock (`SELECT id FROM vehicles WHERE id = :id FOR UPDATE`) and creates a 15-minute temporary reservation hold (`booking_holds`, status: `ACTIVE`). Master booking row is **NOT** inserted.
3. **Display Partner Payment Instructions:** System displays the managing partner's payment profile: payee business name, UPI QR image, UPI ID, external link (if configured), amount due, and verification window countdown timer.
4. **Direct Payment Execution:** Customer makes the payment directly to the managing partner outside the platform.
5. **Customer Reference Submission:** Customer submits payment proof: transaction reference (Bank UTR / Cash receipt), payment method used, exact amount transferred, transaction timestamp, and optional receipt image.
6. **Verification Pending & Hold Lock Extension:** Payment submission transitions to `SUBMITTED_FOR_VERIFICATION`. The hold transitions to `VERIFICATION_PENDING` with its TTL extended by the configurable verification window (e.g. 60 minutes), preserving inventory locking.
7. **Partner Independent Verification:** Managing partner inspects their own bank account statement or UPI app to confirm inward fund credit.
8. **Partner Verification / Rejection Action:** Partner clicks "Verify Payment" or "Reject Payment" (with mandatory reason).
9. **Strict Verification Invariant:** **A customer-uploaded screenshot, receipt, or UTR must NEVER automatically mark a payment as verified or confirm a booking.** ONLY an explicit partner API action marks the submission `VERIFIED`.
10. **Booking Confirmation:** Only upon verified payment confirmation (or under a partner-approved pay-at-pickup policy) does the master booking record transition to `CONFIRMED`.

### 2.2 State Transitions & Failure Scenarios:
- **`rental_payment_demands` Lifecycle:** `ISSUED` $\rightarrow$ `PAYMENT_SUBMITTED` $\rightarrow$ `VERIFIED` (Terminal), with failure branches to `EXPIRED` or `CANCELLED`.
- **`rental_payment_submissions` Lifecycle:** `SUBMITTED_FOR_VERIFICATION` $\rightarrow$ `VERIFIED` (Terminal) or `REJECTED` (Customer may re-submit if hold valid).
- **Duplicate Reference Prevention:** Database unique constraint on `(manager_partner_id, transaction_reference)` prevents recycling UTR numbers.
- **Payment Amount Mismatch:** If customer pays ₹1,000 against a ₹1,500 demand, partner rejects with `AMOUNT_MISMATCH`; customer can submit a top-up reference.
- **Expired Hold & Late Payment:** If customer pays after the 15-minute hold expires and inventory is taken by another customer, booking is **NOT** silently confirmed. The system logs a `LATE_PAYMENT_EXCEPTION`, and partner executes an external refund outside the platform.
- **Partner Non-Response:** If partner does not verify within 60 minutes, hold expires to prevent indefinite vehicle calendar locking. Automated reminders trigger at 30m and 45m. Support ticket workflow arbitrates late verifications.

---

## 3. Security Deposit and Refund Lifecycle

- **Direct Partner Custody:** The rental partner directly receives and holds the customer's security deposit.
- **Refundable Liability:** The deposit is tracked in `security_deposits` as a customer refundable liability in partner custody, never as platform revenue or escrow.
- **Evidence-Based Deductions:**
  - Partner proposes deductions (`deposit_deductions`) with mandatory pre-rental vs. post-rental inspection photos, odometer readings, and repair estimates.
  - **No Automatic Approval:** A disputed damage claim is **NEVER** automatically approved merely because a 48-hour response window expires. Disputed claims enter `ADMIN_ARBITRATION`. Non-responsive claims require Admin verification before deduction approval.
- **Immediate Release of Undisputed Balances:** Any uncontested portion of a security deposit must be refunded immediately by the partner outside the platform.
- **Direct Partner Refund Flow (`security_deposit_refunds`):**
  - Partner transfers refund directly to customer via UPI/NEFT outside platform.
  - Partner inputs Bank UTR reference, payment date, and optional receipt proof.
  - Status moves from `PROMISED` $\rightarrow$ `DISBURSED_BY_PARTNER` $\rightarrow$ `CONFIRMED_BY_CUSTOMER`.
  - Customer can confirm or dispute refund within 72 hours.
- **Zero Platform Gateway Transfers:** The platform records, monitors, and audits refunds, but does not execute external gateway refund transactions.

---

## 4. Contributor Compensation Lifecycle (Approach A)

The platform preserves the established business model:
- **Bilateral Agreement:** Contributor submits vehicle; partner accepts under a signed agreement (`contribution_agreements`) with an agreed fixed fee per eligible booking (e.g. ₹800).
- **Single Lump-Sum Model:** Each completed booking generates exactly one indivisible contributor payable (`contributor_payables`). Partial payments or installment disbursements are prohibited at domain and database levels.
- **Operational Trigger:** Payable transitions to `ELIGIBLE` strictly upon vehicle return and signed return inspection (`bookings.booking_status = COMPLETED`).
- **Direct Settlement:** Partner pays contributor directly outside the platform via NEFT/IMPS/UPI.
- **Platform Tracking:** Partner logs Bank UTR number, settlement date, and receipt attachment. Contributor confirms receipt or disputes invalid UTR within 7 days.
- **Zero Platform Cash Entry:** Partner-to-contributor payments are **never represented as platform cash movements or platform general ledger transactions** because platform funds are completely untouched.

---

## 5. Final Responsibility Matrix

| Operational & Financial Domain | Responsible Entity | Real-World Money Movement | Platform Manifestation |
|---|---|---|---|
| **Rental Service Provider** | **Rental Partner** | Furnishes vehicle, manages physical handover, executes return inspection. | Partner profile verified by Admin; partner legally contracts rental service. |
| **Customer Rental Payee** | **Rental Partner** | Directly receives rental charges via UPI QR, UPI ID, payment link, or cash at pickup. | Displays partner payment profile, issues demands, captures submissions. Zero platform cash handling. |
| **Security Deposit Custodian** | **Rental Partner** | Receives and holds customer deposit directly in partner bank account or cash. | Tracked in `security_deposits` as partner-held liability. Platform holds zero escrow. |
| **Customer Refund Execution** | **Rental Partner** | Directly transfers funds back to customer via UPI/NEFT outside platform. | Records refund obligations, partner UTR proof, and customer confirmations in `security_deposit_refunds`. |
| **Payment Verification Gate** | **Rental Partner** | Independently inspects bank statements for inward credit. | **Strict Invariant:** ONLY partner verification confirms payment. Customer upload never auto-verifies. |
| **Contributor Compensation Debtor** | **Rental Partner** | Pays contributor agreed fixed compensation directly outside platform. | Obligation tracked in `contributor_payables`. Single lump-sum model. Zero platform debt. |
| **Dispute Arbitration** | **Platform Admin** | Evaluates inspection photos, damage claims, and payment receipts if parties dispute. | Admin mediation workflows in `deposit_deductions` and support ticketing. |
| **Platform Monetization** | **Platform Operating Entity** | Independent commercial model (e.g., partner subscriptions or listing fees). | **Zero deductions from customer rental payments.** No commission splitting in MVP. |

---

## 6. Database Table Inventory Reconciliation

### 6.1 Tables Added (1 Table):
1. `partner_payment_profiles`: Managing partner's direct payment receiving configuration (UPI QR image key, UPI ID, external payment link, cash-at-pickup toggle, accepted methods).

### 6.2 Tables Renamed (3 Tables):
1. `payment_orders` $\rightarrow$ `rental_payment_demands`: Clarifies that this records explicit payment obligations demanded for reservations/extensions, not platform gateway orders.
2. `payment_transactions` $\rightarrow$ `rental_payment_submissions`: Clarifies that this records customer-submitted direct payment proof (Bank UTR, amount, timestamp, receipt).
3. `refunds` $\rightarrow$ `security_deposit_refunds`: Clarifies that this records partner direct deposit refunds outside platform, distinct from gateway refund API dispatches.

### 6.3 Tables Moved to Future-Phase Marketplace Gateway (8 Tables):
The following tables are formally preserved and deferred for future activation upon onboarding an approved marketplace payment provider (e.g., Razorpay Route):
1. `payment_webhook_events`: Inward gateway webhook idempotency ledger.
2. `partner_commissions`: Automated platform commission calculations.
3. `payout_methods`: Tokenized partner linked payout accounts.
4. `settlements`: Automated marketplace split disbursement batches.
5. `ledger_accounts`: Platform general ledger Chart of Accounts Master.
6. `ledger_transactions`: Platform general ledger journal transaction headers.
7. `ledger_entries`: Platform append-only double-entry lines.
8. `platform_commission_rules`: Configurable percentage and fee commission rules.

---

## 7. Corrected Table Count & Module Breakdown

Every table is accounted for once and only once across the 9 modules:

```
┌───────────────────────────────────────────────────────────────────┬───────────────┐
│ Functional Domain / Module                                        │ Physical Count│
├───────────────────────────────────────────────────────────────────┼───────────────┤
│ Module 1: Identity, Access, KYC & Addresses                       │ 11 tables     │
│ Module 2: Operating Locations, Partner Hubs & Logistics           │ 3 tables      │
│ Module 3: Vehicle Inventory, Compliance & Pricing (incl. docs)    │ 6 tables      │
│ Module 4: Generic Vehicle Custody Tracking                        │ 1 table       │
│ Module 5: Contributor Agreements & Fleet Proposals                │ 4 tables      │
│ Module 6: Bookings, Holds, Extensions, Inspections & Incidents    │ 7 tables      │
│ Module 7: Partner Payment Profiles, Demands, Submissions & Deposits│ 7 tables     │
│ Module 8: Transactional Outbox, Support, Reviews & Auditing       │ 6 tables      │
├───────────────────────────────────────────────────────────────────┼───────────────┤
│ TOTAL PROPOSED APPLICATION-DOMAIN TABLES FOR MVP                  │ 45 tables     │
│ Module 9: Infrastructure-Managed Tables (ShedLock & Flyway)       │ 2 tables      │
├───────────────────────────────────────────────────────────────────┼───────────────┤
│ TOTAL PROPOSED PHYSICAL MYSQL TABLES FOR MVP                      │ 47 tables     │
├───────────────────────────────────────────────────────────────────┼───────────────┤
│ Future-Phase Marketplace Gateway & Double-Entry Tables            │ 8 tables      │
├───────────────────────────────────────────────────────────────────┼───────────────┤
│ TOTAL CATALOGED DATABASE ENTITIES                                 │ 55 tables     │
└───────────────────────────────────────────────────────────────────┴───────────────┘
```

### Module-by-Module Physical Table List:
- **Module 1 (11):** `users`, `roles`, `user_roles`, `refresh_tokens`, `addresses`, `customer_profiles`, `partner_profiles`, `partner_documents`, `contributor_profiles`, `driving_licences`, `kyc_verifications`.
- **Module 2 (3):** `cities`, `partner_hubs`, `pickup_locations`.
- **Module 3 (6):** `vehicles`, `vehicle_documents`, `vehicle_pricing_profiles`, `vehicle_images`, `vehicle_features`, `vehicle_maintenance_blocks`.
- **Module 4 (1):** `vehicle_custody_events`.
- **Module 5 (4):** `contributor_vehicle_applications`, `contribution_agreements`, `contribution_agreement_events`, `vehicle_withdrawal_requests`.
- **Module 6 (7):** `booking_holds`, `bookings`, `booking_status_events`, `booking_extensions`, `inspections`, `inspection_media`, `incidents`.
- **Module 7 (7):** `partner_payment_profiles`, `rental_payment_demands`, `rental_payment_submissions`, `security_deposits`, `deposit_deductions`, `security_deposit_refunds`, `contributor_payables`.
- **Module 8 (6):** `outbox_events`, `reviews`, `support_tickets`, `support_ticket_messages`, `notifications`, `audit_logs`.
- **Module 9 (2):** `shedlock`, `flyway_schema_history`.

---

## 8. ERD Corrections & Missing Relationships Resolved

1. **`vehicle_documents` Table Represented:** Formally cataloged as Table 16 in Module 3 and fully mapped to `vehicles` (`ON DELETE CASCADE`) in [ENTITY_RELATIONSHIPS.md](ENTITY_RELATIONSHIPS.md).
2. **`outbox_events` Table Represented:** Formally cataloged as Table 40 in Module 8, functioning as an append-only transactional queue using typed aggregate references (`aggregate_type`, `aggregate_id`) without unvalidated foreign keys.
3. **`partner_payment_profiles` Relationship:** Connected 1:1 to `partner_profiles.id` (`ON DELETE CASCADE`).
4. **`rental_payment_demands` Relationships:** Connected to `booking_holds.id` (nullable), `bookings.id` (nullable), `manager_partner_id`, and `customer_user_id`.
5. **`rental_payment_submissions` Relationships:** Connected to `rental_payment_demands.id`, `manager_partner_id`, `customer_user_id`, and `verified_by_user_id`.
6. **`security_deposits` & `security_deposit_refunds` Relationships:** `security_deposits` connected 1:1 to `bookings.id`; `security_deposit_refunds` connected to `security_deposits.id` and `bookings.id`.
7. **Zero Unvalidated Polymorphic Foreign Keys:** Every relationship uses explicit, typed relational foreign keys with database constraints.

---

## 9. Updated Documents & Exact Changed Sections

All 15 governing repository documents have been updated and synchronized:

1. **`README.md`**: Added MVP Payment Architecture summary, updated Technology Stack, Core Roles, and documentation directory linking to the 47-table physical baseline.
2. **`docs/PROJECT_CONSTITUTION.md`**: Updated Section 3.2, 3.3, 3.4, 3.5, and 4 to reflect partner-direct payment collection, zero platform cash handling, manual partner verification invariant, and 47-table physical schema baseline.
3. **`docs/BUSINESS_REQUIREMENTS.md`**: Updated Section 5.2 (10-step partner-direct payment workflow, verification window, failure handling), Section 5.3 (handover blocked until payment verified), Section 5.4 (partner-direct deposit custody and refunds), Section 6 (responsibility matrix), and Section 7.2 (deferred marketplace gateway).
4. **`docs/ARCHITECTURE.md`**: Updated module boundaries and package structure (`payment` and `deposit` packages; 45 MVP domain tables, 8 future-phase tables, transactional outbox pattern, and pessimistic concurrency).
5. **`docs/ARCHITECTURE_DECISIONS.md`**: Added ADR-018 (Partner-Direct Payment Collection & Manual Verification Architecture), marked ADR-016 as superseded, marked ADR-007 as deferred to future phase, reconciled ADR-001, ADR-002, ADR-012, ADR-015, and ADR-017.
6. **`docs/FINAL_ENTITY_INVENTORY.md`**: Reconciled table count to 47 physical MySQL tables (45 domain + 2 infra) + 8 future-phase tables. Renamed payment tables, added `partner_payment_profiles`, and added Section 3 for future-phase tables.
7. **`docs/ENTITY_RELATIONSHIPS.md`**: Updated master Mermaid ERD and 45-table referential integrity matrix to reflect partner-direct payment entities, `vehicle_documents`, and `outbox_events`.
8. **`docs/STATE_MACHINES.md`**: Redesigned booking hold state machine with `VERIFICATION_PENDING` window, detailed `rental_payment_demands` and `rental_payment_submissions` lifecycles, added partner-direct deposit refund state machine, and documented failure/edge scenarios.
9. **`docs/FINANCIAL_ARCHITECTURE.md`**: Complete overhaul to Partner-Direct Financial & Record-Keeping Architecture. Documented zero platform cash handling, partner direct collection, deposit direct custody, contributor compensation tracking (Approach A), and future-phase general ledger archive.
10. **`docs/ROLE_PERMISSION_MATRIX.md`**: Added partner payment profile configuration, payment submission, and verification permissions; updated BOLA/IDOR rules (`@paymentSecurity.canVerifySubmission`).
11. **`docs/DATABASE_DESIGN.md`**: Updated physical schema specifications for all 47 physical MySQL tables and 8 future-phase tables.
12. **`docs/API_MODULES.md`**: Overhauled Section 8 to partner payment profile endpoints, payment submission endpoints, partner verification/rejection endpoints, and deposit refund endpoints; moved gateway webhooks and platform ledger to Section 11 (Future Phase).
13. **`docs/SECURITY.md`**: Updated security baseline for partner payment profiles, private storage for UPI QR images and receipts, anti-fraud duplicate reference constraints, and verification audit trails.
14. **`docs/PAGES_AND_ROUTES.md`**: Updated frontend routes for payment instruction display, customer UTR submission, and partner verification dashboard (`/partner/payment-verification`).
15. **`docs/DEVELOPMENT_WORKFLOW.md`**: Validated phase sequence and quality gates.

---

## 10. Remaining Contradictions or Unresolved Architecture Decisions

A comprehensive cross-document reconciliation was performed across all 15 files.
- **Contradictions Remaining:** **ZERO.**
- All references to platform Merchant of Record status, centralized Razorpay checkout for rentals, automated T+2 settlement disbursements, platform clearing double-entry accounts, and customer screenshot auto-verification have been completely eradicated from active MVP specifications and cleanly moved to Future Phase.
- Every physical table name, entity number, module count, and relationship edge matches identically across `FINAL_ENTITY_INVENTORY.md`, `ENTITY_RELATIONSHIPS.md`, `DATABASE_DESIGN.md`, and `ARCHITECTURE.md`.

---

## 11. Legal, Tax, Insurance and Payment-Provider Validation Gates

Before production deployment and commercial launch in Vijayawada, the following formal validation gates must be cleared:

1. **Legal Validation Gate:**
   - Review and execute bilateral rental agreements establishing that the rental partner is the independent rental service provider and direct payee.
   - Confirm platform terms of service defining the platform strictly as an intermediary technology provider and operational record-keeper.
2. **Tax Validation Gate:**
   - Professional tax opinion on Goods and Services Tax (GST) applicability under Section 9(5) of the CGST Act (determining whether passenger transport / rental service without operator classifies the platform as an E-Commerce Operator liable to pay GST, or as a pure technological intermediary where the partner discharges GST).
3. **Insurance Validation Gate:**
   - Verify that rental partners maintain valid commercial self-drive rental vehicle insurance policies covering vehicle damage, theft, and third-party liabilities during active customer rental intervals.
4. **Future Payment Provider Pre-Approval Gate:**
   - When preparing for Future-Phase marketplace gateway integration, formalize commercial and regulatory onboarding with an RBI-authorized Payment Aggregator (e.g. Razorpay Route or Cashfree) for partner linked-account settlement.

---

## 12. Architecture Freeze Division: Frozen vs. Pending Items

### 12.1 Frozen Architecture Items (Ready for Implementation):
- **Core Technology Stack:** React + Vite, Tailwind CSS, Spring Boot 3+, Spring Security 6+, MySQL 8.0+, JWT + BCrypt.
- **Physical Schema Design:** Exactly 47 physical MySQL tables for MVP (45 application-domain tables + 2 infrastructure tables).
- **Payment Collection Model:** Partner-Direct Payment Collection (UPI QR, UPI ID, Link, Cash at pickup).
- **Verification Gate Invariant:** Manual partner verification strictly required; customer uploads never auto-verify.
- **Deposit Architecture:** Direct partner custody, evidence-based deductions, no auto-approval on timeout, direct partner refund tracking.
- **Contributor Model:** Approach A direct partner settlement; single lump-sum payable per completed booking; tracked in `contributor_payables`.
- **Custody Ledger:** 8 canonical handover permutations without unvalidated polymorphic foreign keys.
- **Hold Concurrency:** Pessimistic locking (`SELECT FOR UPDATE`) on `vehicles` with 15-minute initial hold TTL and 60-minute verification extension.
- **Modular Monolith & Outbox:** Transactional Outbox Pattern (`outbox_events`) and ShedLock distributed scheduling.

### 12.2 Items Requiring Product Owner Approval / Commercial Closure:
- **Commercial Monetization Model:** Selection of platform monetization mechanism (e.g. partner monthly software subscription, fixed listing fee, or per-booking software fee billed to partner).
- **Verification Window Duration:** Final sign-off on operational verification window (recommended default: 60 minutes).
- **Turnaround Buffer Defaults:** Final sign-off on category defaults (recommended: 60 min cars, 30 min bikes/scooters).
- **Production Infrastructure Provider:** Selection of cloud provider (AWS / GCP / DigitalOcean) and managed MySQL instance tier.

---

## Conclusion

The architecture documentation, database design, ERD, financial flows, and state machines are now **100% reconciled, internally consistent, and implementation-ready**. This complete specification serves as the authoritative baseline for the upcoming implementation phase.
