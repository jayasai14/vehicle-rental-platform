# Final Architecture Review & Reconciliation Report: Partner-Direct Payment Model & Refunds Correction

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh (Expandable)  
**Lead Roles:** Lead Software Architect & Database Architect  
**Technology Stack:** React + Vite, Tailwind CSS, Spring Boot, Spring Security, MySQL 8.0+, JWT + BCrypt  
**Status:** **Reconciled & Frozen Database Design Baseline (Partner-Direct Payment & Refund Architecture)**  
**Physical Table Baseline:** **48 Physical MySQL Tables for MVP** (46 Application-Domain Tables + 2 Infrastructure-Managed Tables) + **8 Future-Phase Tables** (56 Total Cataloged Entities)  
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

## 1. Exact Changes Made

This architecture correction resolves two critical design gaps identified during final review:

1. **Addition of Rental-Fee Refund Tracking (`rental_fee_refunds`):**
   - Introduced a dedicated MVP application-domain table (`rental_fee_refunds`, Table 36 in Module 7) to track rental-fee refund lifecycles separately from security deposit refunds (`security_deposit_refunds`, Table 39).
   - Covers rental-fee refunds for booking cancellations, payment exceptions, overpayments, late payments on expired booking holds, and Admin-approved dispute resolutions.
   - Enforced the **Partner UTR Non-Equivalence Invariant**: a partner-entered UTR marks status `DISBURSED_BY_PARTNER` but does **not** automatically indicate customer receipt. The customer has a 72-hour window to acknowledge receipt or raise a dispute.

2. **Configurable Refund Policies (No Hardcoded Percentages):**
   - Removed all hardcoded cancellation tiers and percentages.
   - Introduced dynamic rule evaluation with an immutable audit snapshot stored in `rental_fee_refunds.cancellation_policy_snapshot` (JSON) capturing the cancellation timestamp, applicable rule identifier, gross paid amount, fee deducted (if any), and net refundable amount.

3. **Decoupled Payment Demand and Submission State Machines:**
   - Separated the financial obligation owed (`rental_payment_demands`) from individual customer payment attempts (`rental_payment_submissions`).
   - Demand States: `OPEN`, `PARTIALLY_SATISFIED`, `SATISFIED`, `EXPIRED`, `CANCELLED`.
   - Submission States: `SUBMITTED_FOR_VERIFICATION`, `VERIFIED`, `REJECTED`.
   - Supported multi-submission accumulation: verified partial submissions accumulate in `amount_satisfied` until the demand is satisfied.
   - Defined top-up workflows for underpayments and automated refund obligation creation for verified overpayments.

4. **Unified Booking Confirmation Invariant:**
   - Standardized the booking confirmation invariant across all documents: a booking transitions to `CONFIRMED` only when (1) exclusive vehicle lock is acquired; (2) demands are `SATISFIED` through verified submissions, OR an explicit partner-approved pay-at-pickup policy applies; (3) required security deposit conditions are satisfied; and (4) the booking hold is valid (`ACTIVE` or `VERIFICATION_PENDING`) or safely revalidated under the vehicle lock.
   - Expired holds are never silently confirmed. Late payments on expired holds where the vehicle was reallocated are refunded via `rental_fee_refunds`.
   - For pay-at-pickup bookings, vehicle handover is blocked until pickup collections are verified.

5. **Recalculated Table Inventory Baseline:**
   - Module 7 increased from 7 to 8 tables.
   - Application-domain tables increased from 45 to 46.
   - Infrastructure tables remain 2 (`shedlock`, `flyway_schema_history`).
   - Total MVP physical MySQL tables increased from 47 to 48.
   - Future-phase tables remain 8. Total cataloged database entities: 56.

---

## 2. New `rental_fee_refunds` Schema & Relationships

### 2.1 Schema Definition:
```sql
CREATE TABLE rental_fee_refunds (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    refund_reference VARCHAR(32) NOT NULL,
    booking_id BIGINT UNSIGNED NULL,
    demand_id BIGINT UNSIGNED NULL,
    hold_id BIGINT UNSIGNED NULL,
    manager_partner_id BIGINT UNSIGNED NOT NULL,
    customer_user_id BIGINT UNSIGNED NOT NULL,
    refund_reason ENUM('BOOKING_CANCELLATION', 'OVERPAYMENT_RETURN', 'EXPIRED_HOLD_EXCEPTION', 'PAYMENT_MISMATCH_RETURN', 'ADMIN_ARBITRATION_ORDER', 'OTHER_EXCEPTION') NOT NULL,
    cancellation_policy_snapshot JSON NULL,
    gross_amount_paid DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    cancellation_fee_deducted DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    refund_amount DECIMAL(10,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    refund_status ENUM('PENDING_PARTNER_DISBURSEMENT', 'DISBURSED_BY_PARTNER', 'CONFIRMED_BY_CUSTOMER', 'DISPUTED_BY_CUSTOMER', 'ADMIN_OVERRIDE') NOT NULL DEFAULT 'PENDING_PARTNER_DISBURSEMENT',
    refund_method ENUM('UPI_TRANSFER', 'BANK_NEFT_IMPS', 'CASH_RETURN') NOT NULL,
    partner_transaction_reference VARCHAR(100) NULL,
    receipt_object_key VARCHAR(255) NULL,
    recorded_by_user_id BIGINT UNSIGNED NOT NULL,
    admin_arbitration_id BIGINT UNSIGNED NULL,
    disbursed_at DATETIME NULL,
    acknowledged_at DATETIME NULL,
    dispute_reason VARCHAR(500) NULL,
    dispute_opened_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_rental_refunds_ref UNIQUE (refund_reference),
    CONSTRAINT uk_rental_refunds_partner_utr UNIQUE (manager_partner_id, partner_transaction_reference),
    CONSTRAINT fk_rental_refunds_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rental_refunds_demand FOREIGN KEY (demand_id) REFERENCES rental_payment_demands(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rental_refunds_hold FOREIGN KEY (hold_id) REFERENCES booking_holds(id) ON DELETE SET NULL,
    CONSTRAINT fk_rental_refunds_partner FOREIGN KEY (manager_partner_id) REFERENCES partner_profiles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rental_refunds_customer FOREIGN KEY (customer_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rental_refunds_recorder FOREIGN KEY (recorded_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rental_refunds_arbitrator FOREIGN KEY (admin_arbitration_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_rental_refunds_amounts CHECK (refund_amount > 0 AND refund_amount <= gross_amount_paid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 2.2 Relational Integrity & Non-Polymorphism:
- **Explicit Foreign Keys:** Every relationship uses strongly typed, explicit foreign keys. Zero polymorphic entity references.
- **Relational Flexibility:**
  - When refunding a cancelled confirmed booking: `booking_id` and `demand_id` are populated; `hold_id` is null or historical.
  - When refunding a late payment on an expired hold: `hold_id` and `demand_id` are populated; `booking_id` is null (since no confirmed booking was created).
  - When returning an overpayment on a confirmed booking: `booking_id` and `demand_id` are populated.
- **Anti-Fraud Duplicate Prevention:** `uk_rental_refunds_partner_utr` (`manager_partner_id`, `partner_transaction_reference`) prevents recycling UTRs across different refund records.

---

## 3. Corrected Payment Demand and Submission State Machines

The demand represents the customer's financial obligation; the submission represents an individual payment attempt by the customer.

```
DEMAND LIFECYCLE (rental_payment_demands):
  [OPEN] ──(Verified partial submission: amount_satisfied < demanded)──> [PARTIALLY_SATISFIED]
     │                                                                           │
     │──(Verified submission(s): amount_satisfied >= demanded)───────────────────┤
     │                                                                           ▼
     │                                                                      [SATISFIED] (Terminal)
     │──(Hold expires without sufficient verified payments)─────────────────────> [EXPIRED]   (Terminal)
     └──(Customer cancels hold / checkout aborted)─────────────────────────────> [CANCELLED] (Terminal)

SUBMISSION LIFECYCLE (rental_payment_submissions):
  [SUBMITTED_FOR_VERIFICATION] ──(Partner confirms inward bank credit)──> [VERIFIED] (Terminal)
               │
               └──(Partner rejects invalid UTR / bad receipt)───────────> [REJECTED] (Terminal)
```

### 3.1 State Transition Invariants:
1. **Demand Creation:** A demand is generated upon checkout hold initiation (`OPEN`, `amount_satisfied = 0.00`).
2. **Accumulation Rule:** Only submissions in `VERIFIED` status contribute to `amount_satisfied`. Submissions in `SUBMITTED_FOR_VERIFICATION` or `REJECTED` have zero impact on `amount_satisfied`.
3. **Transition to `PARTIALLY_SATISFIED`:** Occurs when a verified submission yields `0 < amount_satisfied < amount_demanded`.
4. **Transition to `SATISFIED`:** Occurs immediately when `amount_satisfied >= amount_demanded`.
5. **No Auto-Verification:** Screenshots, customer notes, or bank SMS texts never transition submissions to `VERIFIED`. Only explicit partner API action marks a submission `VERIFIED`.

---

## 4. Rental-Fee Refund Lifecycle

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        RENTAL-FEE REFUND STATE MACHINE                                 │
│                                                                                        │
│  [PENDING_PARTNER_DISBURSEMENT]                                                        │
│           │                                                                            │
│           ▼ (Partner executes external bank/UPI transfer & enters UTR + receipt)       │
│  [DISBURSED_BY_PARTNER] ◄─── Partner UTR Non-Equivalence: Customer NOT yet confirmed   │
│     │               │                                                                  │
│     │ (72h Timeout /│ Customer confirms receipt)                                       │
│     ▼               ▼                                                                  │
│  [CONFIRMED_BY_CUSTOMER] (Terminal)                                                    │
│                     │                                                                  │
│                     └──(Customer disputes non-receipt within 72h)                      │
│                                     │                                                  │
│                                     ▼                                                  │
│                          [DISPUTED_BY_CUSTOMER]                                        │
│                                     │                                                  │
│                                     ▼ (Admin arbitrates bank proof / statement)        │
│                             [ADMIN_OVERRIDE] (Terminal)                                │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### 4.1 Rental-Fee Refund Conditions:
1. **Confirmed Booking Cancellation:** Customer cancels pre-pickup. The system evaluates the configurable cancellation rule, calculates the cancellation fee and net refundable amount, snapshots the policy in `cancellation_policy_snapshot`, and inserts a `PENDING_PARTNER_DISBURSEMENT` refund record.
2. **Overpayment Return:** Customer verified submissions exceed `amount_demanded`. The system flags `excess_amount` and automatically creates an `OVERPAYMENT_RETURN` refund record for the difference.
3. **Expired-Hold Late Payment Exception:** Customer transfers funds after hold expiration and the vehicle is reallocated. Partner verifies inward credit but cannot confirm booking; partner initiates an `EXPIRED_HOLD_EXCEPTION` refund.
4. **Admin Dispute Resolution:** Admin arbitrates a customer dispute in favor of a customer refund, generating an `ADMIN_ARBITRATION_ORDER` refund obligation.

---

## 5. Security-Deposit Refund Lifecycle

Security deposits remain tracked in `security_deposits` (custody) and `security_deposit_refunds` (disbursements):
1. **Direct Partner Custody:** Partner collects and holds customer deposit directly; tracked as a refundable customer liability.
2. **Post-Rental Inspection:** Customer and partner sign return inspection (`bookings.booking_status = COMPLETED`).
3. **Evidence-Based Deductions (`deposit_deductions`):** Partner proposes damage/fuel/late deductions with photographic evidence. Customer has 48 hours to agree or dispute. **Disputed deductions are never auto-approved upon timeout; Admin arbitration is required.**
4. **Undisputed Balance Disbursement:** Partner transfers uncontested deposit balance directly to customer via UPI/Bank outside platform.
5. **Audit Trail (`security_deposit_refunds`):** Partner inputs UTR and receipt; customer confirms or disputes within 72 hours.

---

## 6. Partial-Payment and Top-Up Handling

The system supports multiple customer submissions against a single demand:
1. **Scenario:** Demand amount due: ₹1,500.
2. **Initial Submission:** Customer submits ₹1,000 with Bank UTR `UTR-001`.
3. **Partner Verification:** Partner inspects bank statement, verifies ₹1,000 credit, and marks submission `VERIFIED`.
4. **Demand Accumulation:** Demand `amount_satisfied` updates to ₹1,000. Because ₹1,000 < ₹1,500, demand transitions from `OPEN` to `PARTIALLY_SATISFIED`. Hold remains in `VERIFICATION_PENDING`.
5. **Customer Top-Up:** UI prompts customer: *"₹1,000 verified. Remaining balance due: ₹500."* Customer transfers ₹500 and submits second submission with `UTR-002`.
6. **Second Verification:** Partner verifies ₹500 credit. Demand `amount_satisfied` becomes ₹1,500.
7. **Demand Satisfaction & Confirmation:** Demand transitions to `SATISFIED`. The booking confirmation invariant evaluates to true, converting hold to `CONVERTED` and booking to `CONFIRMED`.
8. **Overpayment Handling:** If customer accidentally transferred ₹1,800 across submissions, `amount_satisfied` records ₹1,800. Demand transitions to `SATISFIED`, booking confirms, and an automated refund record of ₹300 is created in `rental_fee_refunds` (`refund_reason = 'OVERPAYMENT_RETURN'`).

---

## 7. Late-Payment Exception Handling

When a customer pays after the 15-minute hold expires or while a verification was pending:
1. **Hold Expiration:** If the hold TTL (initial 15m or verification extension 60m) expires before partner verification, the hold transitions to `EXPIRED`, demand transitions to `EXPIRED`, and the vehicle calendar is freed.
2. **Partner Verification of Expired Hold:** If customer already sent money and partner confirms inward credit on an expired hold:
   - **Case A (Vehicle Still Available):** Under pessimistic row lock (`SELECT ... FOR UPDATE`), system checks if vehicle remains unreserved. If available, partner revalidates and confirms booking.
   - **Case B (Vehicle Reallocated or Unavailable):** If vehicle is booked by another customer or partner has blocked it, **the expired hold is NEVER silently confirmed**. The partner marks submission `VERIFIED`, system flags a `LATE_PAYMENT_EXCEPTION`, and automatically creates an `EXPIRED_HOLD_EXCEPTION` refund record in `rental_fee_refunds`.
   - The partner transfers the full paid amount back to the customer outside the platform.

---

## 8. Updated Module-by-Module Physical Table Inventory

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
│ Module 7: Partner Payment Profiles, Demands, Submissions & Refunds│ 8 tables      │
│ Module 8: Transactional Outbox, Support, Reviews & Auditing       │ 6 tables      │
├───────────────────────────────────────────────────────────────────┼───────────────┤
│ TOTAL PROPOSED APPLICATION-DOMAIN TABLES FOR MVP                  │ 46 tables     │
│ Module 9: Infrastructure-Managed Tables (ShedLock & Flyway)       │ 2 tables      │
├───────────────────────────────────────────────────────────────────┼───────────────┤
│ TOTAL PROPOSED PHYSICAL MYSQL TABLES FOR MVP                      │ 48 tables     │
├───────────────────────────────────────────────────────────────────┼───────────────┤
│ Future-Phase Marketplace Gateway & Double-Entry Tables            │ 8 tables      │
├───────────────────────────────────────────────────────────────────┼───────────────┤
│ TOTAL CATALOGED DATABASE ENTITIES                                 │ 56 tables     │
└───────────────────────────────────────────────────────────────────┴───────────────┘
```

### Complete Physical Table Inventory (48 MVP Tables):
- **Module 1: Identity, Access, KYC & Addresses (11):** Table 1 `users`, Table 2 `roles`, Table 3 `user_roles`, Table 4 `refresh_tokens`, Table 5 `addresses`, Table 6 `customer_profiles`, Table 7 `partner_profiles`, Table 8 `partner_documents`, Table 9 `contributor_profiles`, Table 10 `driving_licences`, Table 11 `kyc_verifications`.
- **Module 2: Operating Locations, Partner Hubs & Logistics (3):** Table 12 `cities`, Table 13 `partner_hubs`, Table 14 `pickup_locations`.
- **Module 3: Vehicle Inventory, Compliance & Pricing (6):** Table 15 `vehicles`, Table 16 `vehicle_documents`, Table 17 `vehicle_pricing_profiles`, Table 18 `vehicle_images`, Table 19 `vehicle_features`, Table 20 `vehicle_maintenance_blocks`.
- **Module 4: Generic Vehicle Custody Tracking (1):** Table 21 `vehicle_custody_events`.
- **Module 5: Contributor Agreements & Fleet Proposals (4):** Table 22 `contributor_vehicle_applications`, Table 23 `contribution_agreements`, Table 24 `contribution_agreement_events`, Table 25 `vehicle_withdrawal_requests`.
- **Module 6: Bookings, Holds, Extensions, Inspections & Incidents (7):** Table 26 `booking_holds`, Table 27 `bookings`, Table 28 `booking_status_events`, Table 29 `booking_extensions`, Table 30 `inspections`, Table 31 `inspection_media`, Table 32 `incidents`.
- **Module 7: Partner Payment Profiles, Demands, Submissions, Refunds & Deposits (8):** Table 33 `partner_payment_profiles`, Table 34 `rental_payment_demands`, Table 35 `rental_payment_submissions`, **Table 36 `rental_fee_refunds` (NEW)**, Table 37 `security_deposits`, Table 38 `deposit_deductions`, Table 39 `security_deposit_refunds`, Table 40 `contributor_payables`.
- **Module 8: Transactional Outbox, Support, Reviews & Auditing (6):** Table 41 `outbox_events`, Table 42 `reviews`, Table 43 `support_tickets`, Table 44 `support_ticket_messages`, Table 45 `notifications`, Table 46 `audit_logs`.
- **Module 9: Infrastructure-Managed Tables (2):** Table 47 `shedlock`, Table 48 `flyway_schema_history`.

---

## 9. Corrected Total Table Count

- **Previous Baseline:** 47 physical MySQL tables (45 application-domain + 2 infrastructure).
- **Added Entity:** 1 application-domain table (`rental_fee_refunds`).
- **Updated Baseline:** **48 physical MySQL tables for MVP** (46 application-domain + 2 infrastructure).
- **Future-Phase Entities:** 8 tables (`payment_webhook_events`, `partner_commissions`, `payout_methods`, `settlements`, `ledger_accounts`, `ledger_transactions`, `ledger_entries`, `platform_commission_rules`).
- **Total Cataloged Entities:** **56 tables**.

---

## 10. Documents Updated

Every governing document in the repository has been updated and synchronized:

1. [README.md](file:///d:/vehicle-rental-platform/README.md): Baseline updated to 48 physical tables (56 cataloged entities); updated core payment architecture and documentation index.
2. [PROJECT_CONSTITUTION.md](file:///d:/vehicle-rental-platform/docs/PROJECT_CONSTITUTION.md): Updated Sections 3.2, 3.3, 3.5, and 4 with decoupled state machines, booking confirmation invariant, `rental_fee_refunds`, configurable rules, and 48-table baseline.
3. [BUSINESS_REQUIREMENTS.md](file:///d:/vehicle-rental-platform/docs/BUSINESS_REQUIREMENTS.md): Updated Steps 5, 7, 8 in payment workflow, Section 5.5 (`rental_fee_refunds` lifecycle, configurable cancellation rules), and Section 6.1 responsibility matrix.
4. [ARCHITECTURE.md](file:///d:/vehicle-rental-platform/docs/ARCHITECTURE.md): Updated Section 3 table counts (Module 7: 8 tables, 46 domain + 2 infra = 48 MVP physical tables), Section 4 domain events, and Section 5 booking confirmation invariant.
5. [ARCHITECTURE_DECISIONS.md](file:///d:/vehicle-rental-platform/docs/ARCHITECTURE_DECISIONS.md): Updated ADR-012 (48 physical tables baseline), ADR-013 (72h refund dispute window), and ADR-018 (decoupled payment state machines, `rental_fee_refunds`, overpayment handling).
6. [FINAL_ENTITY_INVENTORY.md](file:///d:/vehicle-rental-platform/docs/FINAL_ENTITY_INVENTORY.md): Reconciled summary table to 48 physical tables (56 cataloged); Module 7 updated to 8 tables including Table 36 `rental_fee_refunds`; Tables 37-48 renumbered.
7. [ENTITY_RELATIONSHIPS.md](file:///d:/vehicle-rental-platform/docs/ENTITY_RELATIONSHIPS.md): Updated Mermaid ERD with `rental_fee_refunds` relationships, and referential integrity matrix with Table 36 and renumbered Tables 37-46.
8. [DATABASE_DESIGN.md](file:///d:/vehicle-rental-platform/docs/DATABASE_DESIGN.md): Updated Section 8 Module 7 with Table 36 `rental_fee_refunds` DDL and constraints; renumbered Tables 37-48 across Modules 7, 8, 9.
9. [STATE_MACHINES.md](file:///d:/vehicle-rental-platform/docs/STATE_MACHINES.md): Decoupled Demand (`OPEN`, `PARTIALLY_SATISFIED`, `SATISFIED`, `EXPIRED`, `CANCELLED`) and Submission (`SUBMITTED_FOR_VERIFICATION`, `VERIFIED`, `REJECTED`) state machines; added `rental_fee_refunds` state machine; updated booking confirmation invariant.
10. [FINANCIAL_ARCHITECTURE.md](file:///d:/vehicle-rental-platform/docs/FINANCIAL_ARCHITECTURE.md): Updated Module 7 count to 8 tables, added Section 2.4 `rental_fee_refunds`, updated demand/submission separation, accumulation, and configurable policy snapshots.
11. [ROLE_PERMISSION_MATRIX.md](file:///d:/vehicle-rental-platform/docs/ROLE_PERMISSION_MATRIX.md): Added rental-fee refund permissions to global matrix, partner rights, customer 72h dispute rights, and `@rentalRefundSecurity` evaluator.
12. [API_MODULES.md](file:///d:/vehicle-rental-platform/docs/API_MODULES.md): Added rental-fee refund endpoints (`POST/GET /api/v1/partner/rental-fee-refunds`, `GET /api/v1/customer/rental-fee-refunds`, acknowledge, dispute, resolve), and top-up submission endpoint.
13. [SECURITY.md](file:///d:/vehicle-rental-platform/docs/SECURITY.md): Updated scope to 48 physical tables, added `@rentalRefundSecurity` evaluator, duplicate reference constraint checks, partner UTR non-equivalence invariant, and booking confirmation invariant.
14. [PAGES_AND_ROUTES.md](file:///d:/vehicle-rental-platform/docs/PAGES_AND_ROUTES.md): Added `/customer/refunds`, `/partner/rental-refunds`, and updated `/admin/disputes`.
15. [DEVELOPMENT_WORKFLOW.md](file:///d:/vehicle-rental-platform/docs/DEVELOPMENT_WORKFLOW.md): Validated phase sequencing and quality gates.

---

## 11. Remaining Contradictions or Unresolved Decisions

A comprehensive audit was performed across all 15 documents:
- **Contradictions Remaining:** **ZERO.**
- There are no remaining references to the platform holding customer money, customer screenshot auto-verification, or hardcoded cancellation fee percentages.
- The distinction between `rental_fee_refunds` and `security_deposit_refunds` is cleanly maintained across all entities, state machines, and API endpoints.
- Table names, counts, entity numbers, and foreign keys match identically across all documents.

---

## 12. Items Requiring Product Owner Approval

The following business rules and operational parameters require formal Product Owner sign-off before implementation:

1. **Cancellation Fee Tier Schedule:** Sign off on the initial configurable cancellation rule tiers (e.g. 100% refund >24h before pickup, 50% refund 12-24h before pickup, 0% refund <12h before pickup).
2. **Payment Verification Window Duration:** Formal confirmation of the default verification window duration (recommended default: 60 minutes) during which a vehicle hold is preserved in `VERIFICATION_PENDING`.
3. **Refund Dispute Window Duration:** Confirmation of the 72-hour customer dispute window for both rental-fee and security-deposit refunds.
4. **Platform Monetization Model:** Final selection of commercial monetization mechanism (e.g., partner monthly subscription fee, per-listing fee, or fixed software billing per completed booking).

---

## 13. Confirmation of Zero Application Code Generation

**Explicit Confirmation:**
- **Zero application code has been generated.**
- No Java source files, Spring Boot components, Spring Security configurations, JPA entities, or repository interfaces were created.
- No React components, JSX/TSX files, CSS stylesheets, or frontend routes were created.
- No Flyway SQL migration scripts or database DDL files were generated or executed against any database.
- No unit, integration, or end-to-end tests were generated.
- All modifications are strictly documentation-only architectural corrections.
