# Pre-Freeze Architecture Validation Report

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh (Expandable)  
**Lead Roles:** Lead Software Architect & Database Architect  
**Technology Stack:** React + Vite, Tailwind CSS, Spring Boot, Spring Security, MySQL 8.0+, JWT + BCrypt, Razorpay  
**Status:** **Pre-Freeze Candidate Baseline (Pending Final Product-Owner Sign-Off)**  
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

This pre-freeze validation report details all structural corrections made across the architecture document set prior to database design freeze.

### Physical Table Count Audit:
- **Confirmed Proposed Count:** **52 Proposed Physical Tables**
  - **50 Application-Domain Tables** (all enumerated individually in [FINAL_ENTITY_INVENTORY.md](FINAL_ENTITY_INVENTORY.md))
  - **2 Infrastructure-Managed Tables** (`shedlock`, `flyway_schema_history`)
- **Pre-Implementation Status:** These are proposed physical database tables to be created via Flyway migrations in MySQL. **No migrations, JPA entities, or actual database schemas have been generated yet.**
- **Ledger Normalization Option:** The primary baseline models the double-entry journal using `ledger_entries` (with an identifiable `transaction_group_id` and strict ENUM Chart of Accounts). An alternative 3-table normalized option (`ledger_accounts`, `ledger_transactions`, `ledger_entries`) that expands the domain count from 50 to 52 tables (54 tables total) is fully documented in [FINANCIAL_ARCHITECTURE.md](FINANCIAL_ARCHITECTURE.md#3-physical-table-design-confirmed-52-table-baseline-vs-normalized-54-table-option) for Product Owner consideration.

---

## 2. Detailed Summary of Architectural Corrections

### 2.1 Vehicle Custody State Machine Corrections
- **Initial Custody States:** Partner-owned vehicles begin in `PARTNER_CUSTODY`. Contributor-owned vehicles begin in `CONTRIBUTOR_CUSTODY`.
- **All 8 Handover Permutations Supported:**
  1. Contributor to partner (`CONTRIBUTOR_TO_PARTNER`): Onboarding at partner hub.
  2. Partner to customer (`PARTNER_TO_CUSTOMER`): Standard pickup at hub.
  3. Contributor to customer (`CONTRIBUTOR_TO_CUSTOMER`): Owner-managed direct pickup.
  4. Customer to partner (`CUSTOMER_TO_PARTNER`): Hub return.
  5. Customer to contributor (`CUSTOMER_TO_CONTRIBUTOR`): Owner-managed direct return.
  6. Partner to contributor (`PARTNER_TO_CONTRIBUTOR`): Return upon contract termination or withdrawal.
  7. Partner hub to partner hub (`INTER_HUB_TRANSFER`): Fleet rebalancing.
  8. Authorized representative handovers (`LOGISTICS_RELOCATION`): Doorstep delivery/recovery.
- **Model Independence:** Physical custody is modeled in `vehicle_custody_events` independently of vehicle legal ownership and booking status.

### 2.2 Booking & Hold Separation
- **Hold Isolation:** Creating a 15-minute booking hold in `booking_holds` does **not** insert an incomplete or draft record into `bookings`.
- **Concurrency Defense:** Pessimistic row locking (`SELECT id FROM vehicles WHERE id = :id FOR UPDATE;`) prevents concurrent checkout double-bookings.
- **Delayed Webhook Recovery:** If a successful payment webhook arrives after a hold has expired:
  - If the vehicle interval remains completely available, the system recovers and confirms the booking.
  - If the requested interval has been reserved by another customer in the interim, the system **automatically triggers a 100% gateway refund**, logs an incident, and alerts the customer and admin.
- **Operational Decoupling:** Operational booking completion (`COMPLETED`) occurs immediately upon return inspection sign-off and does not depend on financial settlements or deposit refund completion.

### 2.3 Payment, Refund & Security Deposit Lifecycles
- **Lifecycle Separation:** Distinct state machines for payment orders, payment transaction attempts, refunds, security deposits, contributor payables, and partner settlements.
- **Authorization vs. Capture:** Payment attempts explicitly distinguish between authorization and capture.
- **Resilience:** System supports partial refunds, failed refunds, retry/reconciliation queues, failed transactions, and duplicate webhook deduplication (`payment_webhook_events`).
- **Deposit Dispute Arbitration:**
  - Undisputed deposit balances are refunded immediately.
  - **A disputed damage claim is NEVER automatically approved merely because a 48-hour response window expires.** Disputed claims require Admin arbitration; non-responsive claims require Admin verification before deduction.

### 2.4 Financial Accounting & Contributor Settlement
- **Approach A (Direct Partner Settlement) is the Recommended MVP Baseline:**
  - The rental partner is the sole contractual debtor for the contributor's fixed earnings.
  - The platform calculates payables, records obligations against the partner, provides a settlement interface, stores the partner's payment reference/UTR and date, tracks settlement status, allows contributor confirmation or dispute, and provides administrative oversight.
  - The platform never holds or routes third-party contributor funds and assumes zero debtor liability.
- **Approach B (Razorpay Route) Remains Future Option:** Marked **PENDING** partner onboarding, KYC, and RBI payment aggregator regulatory review.
- **Double-Entry Guarantees:** Every journal transaction group balances to zero ($\sum \text{Debits} = \sum \text{Credits}$). Every entry references an approved account code. Financial adjustments use reversal entries. Platform liabilities (`CUSTOMER_REFUNDABLE_LIABILITY`) are strictly segregated from partner contractual obligations (`CONTRIBUTOR_PAYABLE`).

### 2.5 Role-Permission Matrix Corrections
- **Contributor Capabilities Formalized:**
  - Participate in handover inspections for their own vehicles.
  - Acknowledge direct vehicle handovers.
  - Participate in return inspections when the agreement requires it.
  - View their own vehicle documents and agreements.
  - View their own earnings and settlements with UTR references.
  - Request vehicle withdrawal with recommended 7-day notice.
- **Partner & Customer Boundaries:** Partners manage independent fleet, manage accepted contributor fleet, control pricing subject to price floors, conduct inspections, and record direct settlements. Customers access only their own bookings, payments, and verification records. Admin access is permission-controlled and audited.

### 2.6 Pricing & Contributor Price-Floor Engine
- **No Hardcoded GST Deductions:** Tax calculations are not hardcoded as a simple percentage deduction from rental revenue.
- **Component Separation:** Taxable rental amount, platform commission, gateway fees, fixed contributor compensation, partner margin, and discount funding are cleanly separated.
- **Tax Validation Status:** Tax calculations are configurable and marked **PENDING professional tax validation**.

### 2.7 Configurable Operational Defaults (NOT Hardcoded)
- Booking hold TTL: **15 minutes**.
- Car turnaround buffer: **60 minutes**.
- Bike turnaround buffer: **30 minutes**.
- Scooter turnaround buffer: **30 minutes**.
- Contributor settlement mode: **Direct partner payment (Approach A)**.
- Vehicle withdrawal notice: **Recommended 7 days**, subject to confirmed customer bookings.
- Deposit dispute response window: **48 hours**.
- Overdue return grace period: **30 minutes**.

---

## 3. Explicit List of Remaining Product-Owner & Legal Decisions

The following items are deliberately **not frozen** and require explicit Product Owner and legal counsel determination prior to production launch:

1. **Physical Ledger Schema Variant Sign-Off:**
   - *Decision:* Confirm whether to freeze the primary **52-table schema** (with `ledger_entries` using `transaction_group_id` and strict ENUM Chart of Accounts) OR approve the fully normalized **54-table schema** (introducing separate `ledger_accounts` and `ledger_transactions` tables).
2. **GST & Tax Classification Sign-Off:**
   - *Decision:* Professional tax counsel must determine the exact GST rate and HSN/SAC classification (e.g., SAC 9966 for passenger transport / vehicle rental vs. SAC 9973 for leasing/licensing) and whether Section 9(5) E-Commerce Operator tax liability applies to the platform.
3. **Approach B (Razorpay Route) Onboarding Feasibility:**
   - *Decision:* Commercial approval from Razorpay for Route linked-account disbursements to rental partners and contributors, including sub-merchant KYC requirements and payment aggregator guidelines under RBI directives.
4. **Driver Licence & Identity Verification Provider Selection:**
   - *Decision:* Finalize commercial contracts with identity verification providers (e.g., Digilocker, Signzy, IDfy, or Karza) for automated Aadhaar OKYC and driving licence Sarathi API verifications.
5. **Commercial Insurance & Subrogation Protocol:**
   - *Decision:* Legal alignment with commercial vehicle insurance partners regarding liability during customer rental periods, customer self-drive permits (Rent-a-Cab Scheme 1989), and claim subrogation between partner and contributor policies.
