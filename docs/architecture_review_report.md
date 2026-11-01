# Final Architecture Review & Freeze Sign-Off Report

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh (Expandable)  
**Lead Roles:** Lead Software Architect & Database Architect  
**Technology Stack:** React + Vite, Tailwind CSS, Spring Boot, Spring Security, MySQL 8.0+, JWT + BCrypt, Razorpay  
**Status:** **Approved Database Design Freeze Baseline (54-Table Architecture)**  
**Governing Document Set:**  
1. [PROJECT_CONSTITUTION.md](PROJECT_CONSTITUTION.md)  
2. [BUSINESS_REQUIREMENTS.md](BUSINESS_REQUIREMENTS.md)  
3. [ARCHITECTURE.md](ARCHITECTURE.md)  
4. [ARCHITECTURE_DECISIONS.md](ARCHITECTURE_DECISIONS.md)  
5. [FINAL_ENTITY_INVENTORY.md](FINAL_ENTITY_INVENTORY.md)  
6. [ENTITY_RELATIONSHIPS.md](ENTITY_RELATIONSHIPS.md)  
7. [STATE_MACHINES.md](STATE_MACHINES.md)  
8. [FINANCIAL_ARCHITECTURE.md](FINANCIAL_ARCHITECTURE.md)  
9. [ROLE_PERMISSION_MATRIX.md](ROLE_PERMISSION_MATRIX.md)  
10. [DATABASE_DESIGN.md](DATABASE_DESIGN.md)  
11. [API_MODULES.md](API_MODULES.md)  
12. [SECURITY.md](SECURITY.md)  
13. [PAGES_AND_ROUTES.md](PAGES_AND_ROUTES.md)  
14. [DEVELOPMENT_WORKFLOW.md](DEVELOPMENT_WORKFLOW.md)  

---

## 1. Executive Summary & Inventory Reconciliation

This final sign-off report validates that all remaining architectural, legal, and accounting boundaries are resolved and reconciled across all 14 governing documentation files. The 54-table normalized database architecture is approved as the final freeze baseline.

### Physical Table Inventory Reconciliation:
- **Prior Count:** 50 domain tables + 2 infrastructure tables = 52 proposed tables.
- **Normalized Baseline:** Normalization of the general ledger replaces the single `ledger_entries` table with three master tables:
  1. `ledger_accounts` (Chart of Accounts Master)
  2. `ledger_transactions` (Journal Headers)
  3. `ledger_entries` (Journal Line Items)
- **Final Entity Count:** Exactly **54 Proposed Physical MySQL Tables**:
  - **52 Application-Domain Tables** (all individually enumerated in [FINAL_ENTITY_INVENTORY.md](FINAL_ENTITY_INVENTORY.md) and connected in [ENTITY_RELATIONSHIPS.md](ENTITY_RELATIONSHIPS.md))
  - **2 Infrastructure-Managed Tables** (`shedlock`, `flyway_schema_history`)
- **Pre-Implementation Notice:** These are proposed physical database tables to be created via Flyway migrations in MySQL 8.0+. **No migrations, JPA entities, or application code have been generated yet.** Implementation begins only upon formal Product Owner sign-off.

---

## 2. Final Sign-Off Architectural Reconciliations

### 2.1 Customer Payment, Deposit Custody, and Partner Settlement Demarcation
To eliminate regulatory and commercial ambiguity, the architecture establishes strict legal and operational boundaries:
1. **Merchant of Record (MoR):** The Platform Operating Entity acts as the digital marketplace MoR for customer checkout transactions on the web/app. It collects total customer payments, issues platform facilitation tax invoices, and contracts with RBI-authorized payment aggregators. *(Pending legal validation regarding GST Section 9(5) E-Commerce Operator classification).*
2. **Customer Payment Recipient:** Captured via payment gateway into platform clearing accounts (`GATEWAY_CLEARING`). Rental charges are held in `CUSTOMER_RENTAL_CLEARING` and security deposits in `CUSTOMER_REFUNDABLE_LIABILITY`.
3. **Security Deposit Holder:** Held exclusively by the platform as a **Refundable Balance Sheet Liability** (`CUSTOMER_REFUNDABLE_LIABILITY`). Deposits are never treated as platform revenue, working capital, or an unregulated private "escrow".
4. **Customer Refund Responsibility:** The platform bears sole operational and contractual responsibility for processing customer refunds (cancellations, clean return deposit releases, dispute adjustments, late hold collision refunds) directly to original payment instruments via gateway APIs (`REFUND_PAYABLE`). Partners never refund customers directly.
5. **Partner Rental Settlement:** The platform is contractually obligated to disburse net rental proceeds (`PARTNER_PAYABLE`) to verified partners on a scheduled batch cycle (T+2) via payout disbursement APIs (`SETTLEMENT_CLEARING`). Net proceeds equal gross rental subtotal minus platform commission, applicable gateway fees, partner-funded discounts, and mandated tax withholdings (TDS/TCS).
6. **Payment Provider Responsibilities:** PCI-DSS compliance, UPI intent/collect flows, fraud checks, instant webhook callbacks, automated refund execution, and scheduled bank settlement into platform clearing accounts per RBI guidelines.
7. **Method-Independent Gateway Architecture:** Payment provider integrations are abstracted behind provider-agnostic interfaces (`PaymentGatewayClient`, `PayoutGatewayClient`). The database schema, state machines, and double-entry ledger remain decoupled from any single proprietary gateway mechanism.
8. **Separation from Approach A:** Direct partner-to-contributor settlement under Approach A applies **strictly and exclusively** to the private bilateral commercial contract between the Rental Partner and the Vehicle Contributor. **Approach A does NOT apply to, nor does it establish regulatory compliance for, the separate customer payment collection and partner rental settlement flow.**

### 2.2 Financial Reversals & Post-Payout Partner Receivable Recovery
When an incorrect partner credit has already been disbursed to the partner's external bank account, the platform executes a balanced, double-entry reversal without creating negative liability balances:
1. **Creation of Partner Receivable:** The platform establishes an asset claim by debiting `PARTNER_RECEIVABLE` (Asset) and crediting `REFUND_PAYABLE` (Liability), allowing immediate customer refund execution:
   $$\text{Debit } \texttt{PARTNER\_RECEIVABLE} \quad / \quad \text{Credit } \texttt{REFUND\_PAYABLE}$$
2. **Automated Payout Offset (Option A):** The outstanding receivable is automatically deducted from the partner's net rental proceeds in the subsequent settlement batch:
   $$\text{Debit } \texttt{PARTNER\_PAYABLE} \quad / \quad \text{Credit } \texttt{PARTNER\_RECEIVABLE} \quad / \quad \text{Credit } \texttt{SETTLEMENT\_CLEARING}$$
3. **Direct Repayment (Option B):** If no upcoming bookings exist, the partner repays the platform via direct bank transfer or gateway payment link:
   $$\text{Debit } \texttt{GATEWAY\_CLEARING} \quad / \quad \text{Credit } \texttt{PARTNER\_RECEIVABLE}$$

### 2.3 Contributor Settlement Architecture: Selected Single Lump-Sum Settlement Model
To preserve the approved 54-table baseline and prevent state synchronization defects, the architecture formally adopts:
**One complete direct settlement per contributor payable, with immutable payment evidence (Single Lump-Sum Model).**
- **Indivisible Booking Obligation:** Each completed booking generates exactly one contributor payable record representing the agreed fixed compensation ($K_{contrib}$). Partial disbursements, installment transfers, or split payments are prohibited at the domain and database levels for single vehicle bookings.
- **Immutable Payment Evidence:** When the partner executes the direct bank transfer (via NEFT/IMPS/UPI) outside the platform, the partner submits:
  1. Bank UTR / Transaction Reference Number (`payout_reference`, VARCHAR 100, mandatory, unique per payable)
  2. Exact settlement timestamp (`paid_at`, DATETIME)
  3. Transfer receipt document (`payment_receipt_url`, object key)
- **Immutability Invariant:** Once transitioned to `SETTLED_BY_PARTNER`, settlement reference fields are locked and immutable.
- **Dispute and Audit Resolution:** If the contributor disputes the transfer (`DISPUTED`), Admin Support arbitrates using the bank UTR evidence. If the transfer was deficient or invalid, the partner executes a corrective transfer under administrative supervision and updates the record via audited administrative events, avoiding the complexity of a 55th entity (`contributor_settlement_events`).

### 2.4 State Machine Completeness & Failed Refund Decoupling
1. **Master Booking Lifecycle:** Explicitly models all states (`CONFIRMED`, `READY_FOR_PICKUP`, `IN_PROGRESS`, `OVERDUE`, `RETURNED`, `COMPLETED`, `CANCELLED`, `NO_SHOW`).
   - `CANCELLED` and `NO_SHOW` are strictly terminal.
   - Pre-pickup cancellation refunds deposit 100% and applies cancellation policy to rental subtotal.
   - No-show retains rental subtotal as liquidated damages while refunding 100% of the security deposit.
   - Ongoing rentals (`IN_PROGRESS`, `OVERDUE`) cannot be cancelled or marked no-show.
2. **Late Payment Webhook Recovery Protocol:**
   - Evaluates slot availability under `SELECT ... FOR UPDATE` on `vehicles`.
   - If slot is still available: recovers booking to `CONFIRMED` and notifies customer.
   - If slot was reserved by another customer: retains hold as `EXPIRED`, enqueues 100% refund (`AUTO_REFUND_EXPIRED_HOLD`), and alerts customer/admin.
3. **Decoupled Failed Refund Recovery:**
   - The refund lifecycle (`refunds`) is **strictly decoupled from booking status**. A failed refund never alters or regresses a `COMPLETED`, `CANCELLED`, or `NO_SHOW` booking.
   - Transient failures transition to `RETRY_QUEUED` with exponential backoff (15m, 1h, 4h; max 3 retries).
   - Permanent failures transition to `MANUAL_REVIEW`, where finance either resolves the gateway error or executes a direct offline bank transfer (`OFFLINE_SETTLED`), recording the Bank UTR in refund metadata.

---

## 3. Final Physical Table Inventory & Module Summary (54 Tables)

| Module | Table Count | Physical Table Names |
|---|:---:|---|
| **Module 1: Identity, Access, KYC & Addresses** | 11 | `users`, `roles`, `user_roles`, `refresh_tokens`, `addresses`, `customer_profiles`, `partner_profiles`, `partner_documents`, `contributor_profiles`, `user_kyc_verifications`, `driving_licences` |
| **Module 2: Locations, Hubs & Delivery Zones** | 3 | `cities`, `partner_locations`, `delivery_zones` |
| **Module 3: Fleet Master, Pricing & Blackouts** | 5 | `vehicle_categories`, `vehicles`, `vehicle_features`, `vehicle_pricing_plans`, `vehicle_blackouts` |
| **Module 4: Contributor Applications & Agreements** | 4 | `contributor_vehicle_applications`, `contribution_agreements`, `vehicle_withdrawal_requests`, `pickup_locations` |
| **Module 5: Custody & Fleet Movements** | 1 | `vehicle_custody_events` (All 8 permutations with typed relational references) |
| **Module 6: Reservation Locks, Bookings & Inspections** | 8 | `booking_holds`, `bookings`, `booking_status_events`, `booking_extensions`, `inspections`, `inspection_media`, `incidents`, `deposit_deductions` |
| **Module 7: Payments, Deposits, Payouts & Double-Entry** | 14 | `payment_orders`, `payment_transactions`, `security_deposits`, `payment_webhook_events`, `refunds`, `partner_commissions`, `contributor_payables`, `payout_methods`, `settlements`, `ledger_accounts`, `ledger_transactions`, `ledger_entries`, `platform_commission_rules` |
| **Module 8: Transactional Outbox, Support & Auditing** | 6 | `outbox_events`, `reviews`, `support_tickets`, `support_ticket_messages`, `notifications`, `audit_logs` |
| **Module 9: Infrastructure-Managed Tables** | 2 | `shedlock`, `flyway_schema_history` |
| **TOTAL PHYSICAL TABLES** | **54** | **52 Application-Domain Tables + 2 Infrastructure-Managed Tables** |

---

## 4. Explicit Remaining Production-Launch Legal and Provider Validations

The technical architecture is complete, normalized, and internally consistent. The following business, commercial, and legal items remain **pending final Product Owner and legal counsel determination**:

1. **GST Classification & E-Commerce Operator Status:**
   - *Action:* Obtain formal tax legal opinion on whether the platform operates as an E-Commerce Operator under Section 9(5) of the CGST Act for passenger transport / motor cab rental, or strictly as an IT marketplace facilitator.
2. **Payment Provider Commercial Terms & Settlement Schedule:**
   - *Action:* Formalize merchant agreement with Razorpay (or alternative RBI-authorized aggregator) specifying nodal account settlement schedules (T+1/T+2), refund processing limits, and payment method enablement.
3. **Identity & Driving Licence Verification Provider Selection:**
   - *Action:* Execute vendor agreement with identity verification provider (Digilocker, Signzy, IDfy, or Karza) for automated Aadhaar OKYC and driving licence Sarathi API verifications.
4. **Commercial Self-Drive Insurance & Liability Alignment:**
   - *Action:* Align with commercial insurance underwriters regarding self-drive commercial policies (Rent-a-Cab Scheme 1989) and subrogation protocols between partner business fleet policies and contributor personal insurance policies.
5. **Local Municipal Permit Requirements (Vijayawada):**
   - *Action:* Confirm municipal trade licences and transport department operating permits required for commercial rental hubs and vehicle depots in Vijayawada, Andhra Pradesh.

---

## 5. Final Architecture Sign-Off Checklist

```
[X] Physical Table Inventory: Exactly 54 physical MySQL tables (52 domain + 2 infra).
[X] Master ER Diagram: Models all 52 application-domain tables with typed foreign keys.
[X] Legal Ownership: Explicitly models individual and partner business owners with check constraints.
[X] Custody Tracking: All 8 permutations modeled with explicit typed custodians, actor, and location.
[X] Hold Independence: booking_holds separated from bookings; zero incomplete booking rows.
[X] Late Webhook Handling: Re-check availability recovery or automatic full refund.
[X] General Ledger Normalization: ledger_accounts, ledger_transactions, ledger_entries balanced.
[X] Financial Model Separation: Contributor payables tracked separately from platform cash GL.
[X] Customer Payment & Settlement Demarcation: MoR, deposit holder, refund liability documented.
[X] Post-Payout Reversal Recovery: PARTNER_RECEIVABLE asset account and double-entry reversal scenarios.
[X] Contributor Settlement Model: Single lump-sum direct settlement with immutable payment evidence.
[X] Master Booking State Machine: Explicit cancellation, no-show, and terminal invariants.
[X] Failed Refund Recovery: Decoupled from booking status; retry queue and offline settlement.
[X] Partner Verification Gate: Unverified partner applicants strictly blocked from operational actions.
[X] Pricing Engine: Clean separation of rental, commission, gateway fee, contributor fee, and partner margin.
[X] Role-Permission Matrix: Contributor rights formalized; resource evaluators documented.
[X] API Inventory: Endpoint inventory under /api/v1 aligned with data model and security evaluators.
[X] Documentation Consistency: All 14 documents completely synchronized.
[X] Pre-Implementation Constraint: Zero application code, JPA entities, or migrations created.
```
