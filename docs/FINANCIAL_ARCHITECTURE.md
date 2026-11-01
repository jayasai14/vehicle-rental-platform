# Financial & Operational Record-Keeping Architecture (Partner-Direct Payment Model)

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh  
**Status:** Database Design Freeze Candidate Baseline (Partner-Direct Model)  
**Governing Principle:** **The platform collects, holds, escrows, splits, and transfers ZERO customer rental money in the MVP.** The rental partner who manages a vehicle is the customer-facing rental service provider and direct payee for all bookings on that vehicle. The platform functions strictly as a digital marketplace, booking workflow engine, and operational record-keeper.

---

## 1. Architectural & Legal Responsibility Demarcation

To eliminate legal, tax, and regulatory ambiguity, the platform clearly defines the operational roles and legal responsibilities of the parties:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                        PARTNER-DIRECT PAYMENT ARCHITECTURE (MVP)                       │
│                                                                                        │
│  1. CUSTOMER RENTAL CHECKOUT (Direct Payment)                                         │
│     [Customer] ────(UPI QR / UPI ID / Payment Link / Cash)────> [Rental Partner]        │
│          │                                                             │               │
│          ▼ (Submits Bank UTR / Receipt)                                ▼ (Verifies)    │
│     [Platform: rental_payment_submissions] ─────────────────────> [Demand SATISFIED]   │
│          │ (Accumulates verified amounts)                               │               │
│          ▼ (Satisfies demand)                                           ▼               │
│     [rental_payment_demands] ───────────────────────────────────> [Booking CONFIRMED]   │
│                                                                                        │
│ ────────────────────────────────────────────────────────────────────────────────────── │
│  2. RENTAL-FEE REFUND EXECUTION (Direct Partner Transfer)                              │
│     Trigger: Booking Cancellation / Overpayment / Expired Hold / Admin Dispute         │
│     Rules: Determined by configurable business rules (cancellation_policy_snapshot)    │
│     Refund: Partner directly transfers rental refund via UPI/Bank outside platform     │
│     [Platform: rental_fee_refunds] (Tracks obligation, UTR proof, 72h dispute window)  │
│                                                                                        │
│ ────────────────────────────────────────────────────────────────────────────────────── │
│  3. SECURITY DEPOSIT CUSTODY & REFUND (Direct Partner Custody)                         │
│     Deposit Custody: Partner directly collects and holds customer deposit              │
│     Deductions: Evidence-based damage/fuel claims (Disputes arbitrated by Admin)       │
│     Refund: Partner directly returns deposit via UPI/Bank outside platform             │
│     [Platform: security_deposits & security_deposit_refunds] (Tracks proof & status)   │
│                                                                                        │
│ ────────────────────────────────────────────────────────────────────────────────────── │
│  4. CONTRIBUTOR COMPENSATION (Approach A Direct Settlement)                            │
│     Contractual Debt: Rental Partner ──(Agreed Fixed Fee)──> Contributor                │
│     Settlement: Partner pays Contributor directly via NEFT/IMPS/UPI outside platform   │
│     [Platform: contributor_payables] (Tracks obligation, UTR, proof & acknowledgement) │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### 1.1 Complete Responsibility Demarcation Matrix:

| Operational Domain | Responsible Business Entity | Real-World Money Movement | Architectural & Platform Manifestation |
|---|---|---|---|
| **Rental Service Provider** | **Rental Partner** | Furnishes the vehicle, handles physical custody, inspects vehicle condition. | Partner profile verified by Admin; partner legally contracts rental service to customer. |
| **Customer Rental Payee** | **Rental Partner** | Customer transfers rental fee directly to partner's UPI QR, UPI ID, payment link, or cash at pickup. | Platform displays partner's payment profile; issues `rental_payment_demands`; captures customer `rental_payment_submissions`; partner verifies credit. Zero platform cash handling. |
| **Payment Verification Gate** | **Rental Partner** | Partner independently inspects bank account/UPI app statements for inward credit. | **Strict Invariant:** ONLY partner verification marks payment as verified. Customer screenshot/UTR never auto-confirms payment. Accumulates verified submissions toward demand satisfaction. |
| **Rental Fee Refund Execution** | **Rental Partner** | Partner executes external bank transfer/UPI back to customer for cancellations, overpayments, expired-hold exceptions, or dispute resolutions. | Platform calculates refundable amount via configurable policy snapshot; records refund obligations, partner UTR proof, customer acknowledgements, and dispute resolutions in `rental_fee_refunds`. Partner UTR $\neq$ customer received. Separate states: `PENDING_PARTNER_DISBURSEMENT`, `DISBURSED_BY_PARTNER`, `CUSTOMER_ACKNOWLEDGED`, `DISPUTED_BY_CUSTOMER`, `DISPUTE_WINDOW_ELAPSED_UNACKNOWLEDGED`, `ADMIN_RESOLVED`. Platform never triggers gateway transfers. |
| **Security Deposit Custodian** | **Rental Partner** | Partner receives and holds security deposit directly in partner's bank/UPI account or cash. | Tracked in `security_deposits` as partner-held customer liability. Platform records expected deposit, collected amount, and UTR. Platform does NOT hold escrow. |
| **Security Deposit Refund Execution** | **Rental Partner** | Partner executes external bank transfer/UPI back to customer for deposit release after deduction reconciliation. | Platform records deposit refund obligations, partner UTR proof, customer acknowledgements, and dispute states in `security_deposit_refunds`. Platform never triggers gateway transfers. |
| **Contributor Compensation Debtor** | **Rental Partner** | Partner pays contributor agreed fixed compensation directly outside platform via NEFT/IMPS/UPI. | Obligation tracked in `contributor_payables`. Single lump-sum model per completed booking. Platform never disburses contributor funds. |
| **Dispute Arbitration** | **Platform Admin** | Evaluates inspection photos, damage claims, cancellation appeals, and payment receipts if parties disagree. | Support tickets, incident mediation, refund arbitration, and deduction approval workflows (`deposit_deductions`, `rental_fee_refunds`, `security_deposit_refunds`). Clearly separates dispute case (`dispute_ticket_id` $\rightarrow$ `support_tickets.id`) from deciding Admin (`resolved_by_admin_user_id` $\rightarrow$ `users.id`). |
| **Platform Monetization** | **Platform Operating Entity** | Separate commercial model (e.g., partner subscriptions or listing fees). | **Zero deductions from customer rental payments.** No commission splitting in MVP. |

> [!IMPORTANT]
> **No Merchant of Record Claim:** The platform is **not** the Merchant of Record collecting customer rental money in the MVP. The partner-as-service-provider and partner-as-payee model is an architectural decision that eliminates platform custody risks and regulatory payment aggregator overhead, subject to formal legal, tax, and insurance validation before launch.

---

## 2. Retained MVP Financial & Operational Tables (Module 7: 8 Tables)

Every retained financial table in Module 7 records real-world obligations, submissions, and evidence without representing external transfers as platform cash:

### 2.1 `partner_payment_profiles`
- **What it Records:** Managing partner's direct payment receiving configuration: public payee name, UPI ID (VPA), Cloudinary object key for UPI QR image, external payment URL, accepted payment methods JSON, and whether pay-at-pickup is enabled.
- **Who Moves Money:** Customer transfers money to these partner coordinates.
- **Responsible Entity:** Rental Partner (responsible for accuracy of payment coordinates).
- **Security Control:** NEVER stores sensitive bank credentials, UPI PINs, or OTPs. QR images and payment evidence are protected by secure access controls.

### 2.2 `rental_payment_demands`
- **What it Records:** The explicit financial obligation demanded for a booking hold, booking extension, or late-fee charge (e.g., "Pay ₹1,500 to Partner X for Hold Y"). Tracks `amount_demanded` alongside `amount_satisfied` and independent lifecycle state (`OPEN`, `PARTIALLY_SATISFIED`, `SATISFIED`, `EXPIRED`, `CANCELLED`).
- **Multi-Submission Accumulation:** Multiple payment submissions can be linked to a single demand. The platform aggregates only `VERIFIED` submissions into `amount_satisfied`.
- **Overpayment Accounting (Strict Representation):**
  - If verified submissions total ₹1,800.00 against a ₹1,500.00 demand:
    - `amount_demanded` = ₹1,500.00
    - `total_verified_submitted` = ₹1,800.00 (aggregate sum of all verified submissions)
    - `amount_satisfied` = ₹1,500.00 (**strictly capped at `amount_demanded`**; ₹1,800 is NEVER stored as the amount satisfying a ₹1,500 obligation)
    - `excess_amount` = ₹300.00
  - The demand transitions to `SATISFIED`, and the platform automatically creates a ₹300.00 refund obligation in `rental_fee_refunds` with `refund_reason = 'OVERPAYMENT_RETURN'`.
- **Who Moves Money:** Generated by the platform system upon customer checkout initiation; settled by customer direct payment(s) to partner.
- **Responsible Entity:** Platform generates demand based on vehicle pricing and deposit rules; Partner receives funds and verifies submissions.

### 2.3 `rental_payment_submissions`
- **What it Records:** Individual customer-submitted payment attempts: payment method used (`UPI_QR`, `UPI_ID`, `EXTERNAL_LINK`, `CASH_AT_PICKUP`), transaction reference (Bank UTR / Cash receipt), exact amount transferred, transaction timestamp, receipt image key, and submission status (`SUBMITTED_FOR_VERIFICATION`, `VERIFIED`, `REJECTED`).
- **Top-Up & Overpayment:** If a verified submission is less than `amount_demanded`, customer submits a top-up submission. If total verified submissions exceed `amount_demanded`, the excess amount is recorded in `rental_payment_demands.excess_amount` and routed to `rental_fee_refunds` (`OVERPAYMENT_RETURN`).
- **Late Payment After Hold Expiry (Immutable Recovery Flow):**
  - When a booking hold's TTL expires, both `booking_holds` and `rental_payment_demands` transition to `EXPIRED`. They are **permanently immutable in their terminal state** and must **NEVER** be silently reopened or transitioned back to `SATISFIED`.
  - When an external payment arrives after expiration:
    1. Managing partner inspects their bank/UPI records and confirms receipt.
    2. System marks the submission as `VERIFIED` with an audit note documenting late receipt against an expired demand. The expired demand remains `EXPIRED`.
    3. System checks vehicle availability under pessimistic lock: `SELECT id FROM vehicles WHERE id = :vehicleId FOR UPDATE;`
    4. **Case A (Vehicle Still Available):**
       - Create a fresh `booking_holds` record linked to the expired hold via `recovered_from_hold_id = original_expired_hold.id`.
       - Create a fresh `rental_payment_demands` record with `amount_demanded` equal to the booking obligation.
       - Revalidate all booking conditions, driver eligibility, and turnaround buffers.
       - Apply the previously verified payment amount to the new valid demand (`amount_satisfied = verified_amount`, status = `SATISFIED`) without duplicating the underlying payment.
       - Transition the fresh hold to `CONVERTED` and insert a confirmed booking into `bookings`.
    5. **Case B (Vehicle Unavailable / Conflict):**
       - The booking cannot be recovered safely.
       - Platform creates an immediate refund obligation in `rental_fee_refunds` with `refund_reason = 'EXPIRED_HOLD_EXCEPTION'` for the full verified amount.
       - Partner executes the external bank/UPI refund directly to the customer and records proof.
- **Who Moves Money:** Customer moves money directly to partner outside platform.
- **Responsible Entity:** Customer submits proof; Managing Partner verifies or rejects.
- **Duplicate Prevention:** Unique constraint `uk_submissions_partner_ref` (`manager_partner_id`, `transaction_reference`) prevents recycling references across bookings.

### 2.4 `rental_fee_refunds`
- **What it Records:** Audit trail of rental-fee refunds initiated and executed directly by the rental partner when a confirmed booking is cancelled, an overpayment occurs, a payment exception/late payment on an expired hold is returned, or an Admin-approved dispute requires a rental-fee refund.
- **Configurable Policy Snapshot:** Stores the exact cancellation policy rule in `cancellation_policy_snapshot` (JSON) evaluated at cancellation time, gross amount paid, cancellation fee deducted (if any), and net refundable amount. Never hardcodes percentages.
- **Refund State Machine & Acknowledgement Semantics:**
  - `PENDING_PARTNER_DISBURSEMENT`: Refund obligation registered; partner must execute external payment.
  - `DISBURSED_BY_PARTNER`: Partner executed external transfer and logged Bank UTR and receipt.
  - `CUSTOMER_ACKNOWLEDGED`: Explicit customer confirmation via UI action. **This is the ONLY event that establishes customer-confirmed receipt.**
  - `DISPUTED_BY_CUSTOMER`: Customer claims non-receipt within 72 hours of disbursement.
  - `DISPUTE_WINDOW_ELAPSED_UNACKNOWLEDGED`: 72-hour dispute window expired without explicit customer confirmation or dispute. **Expiry must NEVER be represented as customer acknowledgement.**
  - `ADMIN_RESOLVED`: Authorized Admin arbitrated dispute, recording resolution details.
- **Arbitration Foreign Keys & Case Separation:**
  - Case Reference: `dispute_ticket_id` (FK $\rightarrow$ `support_tickets.id` ON DELETE SET NULL) identifies the support/dispute case.
  - Deciding Admin: `resolved_by_admin_user_id` (FK $\rightarrow$ `users.id` ON DELETE SET NULL) identifies the authorized Admin who resolved the dispute.
  - Audit Trail: `resolution_notes` VARCHAR(1000) and `resolved_at` DATETIME capture the determination.
- **Transactional Aggregate Refund Limit Validation:**
  - A row-level `CHECK (refund_amount <= gross_amount_paid)` constraint is **insufficient** across multiple partial refunds (e.g. overpayment return followed by partial cancellation refund).
  - Every refund obligation creation executes inside a database transaction:
    ```sql
    -- Step 1: Lock the parent demand exclusively
    SELECT id, amount_satisfied FROM rental_payment_demands WHERE id = :demandId FOR UPDATE;
    
    -- Step 2: Sum all prior uncancelled refunds
    SELECT COALESCE(SUM(refund_amount), 0.00) FROM rental_fee_refunds 
    WHERE demand_id = :demandId AND refund_status != 'CANCELLED';
    
    -- Step 3: Enforce aggregate limit
    -- remaining_refundable = amount_satisfied - prior_refunds;
    -- IF new_refund_amount > remaining_refundable THEN REJECT;
    ```
  - **Effect of Canceled / Failed Refunds:** If a refund is voided or cancelled (`CANCELLED`), its amount is excluded from cumulative refunds, restoring the refundable balance.
  - Pessimistic locking prevents concurrent refund requests from both passing based on stale balances.
- **Who Moves Money:** Rental Partner transfers funds directly to customer bank/UPI outside the platform.
- **Responsible Entity:** Rental Partner executes refund; Platform monitors, audits, and arbitrates disputes.
- **Duplicate Prevention:** Unique constraint `uk_rental_refunds_partner_utr` (`manager_partner_id`, `partner_transaction_reference`).

### 2.5 `security_deposits`
- **What it Records:** Tracking of customer security deposits held directly by the rental partner: required amount, collected amount, collection method, collection reference (UTR), held amount, deducted amount, refunded amount, and lifecycle status.
- **Who Moves Money:** Customer deposits with partner; partner holds funds.
- **Responsible Entity:** Rental Partner is the legal and physical custodian.

### 2.6 `deposit_deductions`
- **What it Records:** Itemized damage, fuel deficit, late return, or traffic fine claims against a security deposit, with mandatory inspection evidence and status (`PROPOSED`, `DISPUTED`, `APPROVED_BY_ADMIN`, `AGREED_BY_CUSTOMER`, `CANCELLED`).
- **Dispute Arbitration Separation:** Disputed claims link to `dispute_ticket_id` (FK $\rightarrow$ `support_tickets.id`) for the case and `resolved_by_admin_user_id` (FK $\rightarrow$ `users.id`) for the deciding Admin, alongside `resolution_notes` and `resolved_at`.
- **Who Moves Money:** Executed via deposit retention by partner; no cash moves if settled from deposit.
- **Responsible Entity:** Rental Partner proposes; Customer agrees or disputes; Admin arbitrates unresolved claims.

### 2.7 `security_deposit_refunds`
- **What it Records:** Audit trail of deposit refunds executed directly by the partner: refund amount, refund method, transaction reference (Bank UTR), proof document, refund timestamp, customer confirmation, and dispute notes.
- **State Machine Alignment:** Governed by identical lifecycle states (`PENDING_PARTNER_DISBURSEMENT`, `DISBURSED_BY_PARTNER`, `CUSTOMER_ACKNOWLEDGED`, `DISPUTED_BY_CUSTOMER`, `DISPUTE_WINDOW_ELAPSED_UNACKNOWLEDGED`, `ADMIN_RESOLVED`).
- **Arbitration Foreign Keys:** Links to `dispute_ticket_id` (`support_tickets.id`) and `resolved_by_admin_user_id` (`users.id`).
- **Who Moves Money:** Rental Partner transfers funds directly to customer bank/UPI outside the platform.
- **Responsible Entity:** Rental Partner executes refund; Platform monitors and audits.

### 2.8 `contributor_payables`
- **What it Records:** Partner's contractual obligation to pay the vehicle contributor a fixed agreed amount per completed booking under their bilateral contribution agreement. Tracks payable status, Bank UTR reference, proof attachment, partner settlement date, and contributor digital acknowledgement.
- **Who Moves Money:** Rental Partner pays Contributor directly outside the platform via NEFT/IMPS/UPI.
- **Responsible Entity:** Rental Partner is the sole legal debtor; Contributor is the payee.

---

## 3. Contributor Compensation Model (Approach A Direct Partner Settlement)

The platform preserves the core business model for contributor-owned vehicles:

1. **Bilateral Agreement:** An individual contributor submits a vehicle; a rental partner accepts it under a signed contribution agreement (`contribution_agreements`) specifying an agreed fixed fee per completed booking (e.g., ₹800 per booking).
2. **Single Lump-Sum Model:** Each eligible completed booking generates exactly one indivisible contributor payable (`contributor_payables`). Partial payments or installment disbursements are prohibited at domain and database levels.
3. **Operational Completion Trigger:** A payable transitions to `ELIGIBLE` strictly upon physical vehicle return and signed post-rental inspection (`bookings.booking_status = COMPLETED`).
4. **Direct External Transfer:** The partner transfers the fixed amount directly to the contributor's external bank account or UPI ID.
5. **Platform Audit Trail:** The partner inputs the Bank UTR number, payment date, and optional transfer receipt into the platform.
6. **Contributor Confirmation & Dispute:** The contributor confirms receipt in the UI. If funds are not received within 7 days, the contributor can dispute the record, triggering Admin review.
7. **Zero Platform Cash Entry:** Contributor settlements are **never posted to platform financial accounts** because platform bank accounts are never touched.

---

## 4. Platform Monetization Independence

1. **No Rental Commission Deductions in MVP:**
   - The platform does NOT deduct a commission percentage from customer rental payments in the MVP.
   - All customer rental payments and deposits go 100% directly to the rental partner.
2. **Independent Future Monetization:**
   - Platform monetization (e.g. partner monthly software subscription, per-listing fee, or feature placement fee) will be designed as an independent billing flow where the partner pays the platform.
   - It will never be conflated with customer rental checkout flows.

---

## 5. Security & Audit Requirements for Direct Payments

1. **Partner Authorization Gating:**
   - A partner can only view and verify payment submissions belonging to vehicles/bookings they manage (`manager_partner_id = :partnerId`).
   - Partner users cannot view or verify other partners' payment submissions.
2. **Customer & Contributor Data Isolation:**
   - Customers can only view their own payment demands, submissions, rental-fee refund records (`rental_fee_refunds`), and deposit refund records.
   - Contributors can only view their own contribution payables and agreement terms.
3. **Anti-Fraud Controls:**
   - **No Auto-Verification:** Customer uploads NEVER automatically verify payments.
   - **Unique Reference Constraints:** Duplicate UTRs for the same partner are rejected by database unique constraints on both inbound payments (`uk_submissions_partner_ref`) and refunds (`uk_rental_refunds_partner_utr`).
   - **Partner UTR Non-Equivalence Invariant:** Partner entry of a refund UTR marks status `DISBURSED_BY_PARTNER` but does NOT constitute proof of customer receipt. Explicit customer UI confirmation is strictly required for `CUSTOMER_ACKNOWLEDGED`. If 72 hours elapse without dispute or confirmation, status transitions to `DISPUTE_WINDOW_ELAPSED_UNACKNOWLEDGED`. Expiry is never represented as customer acknowledgement. Disputed refunds (`DISPUTED_BY_CUSTOMER`) are resolved by Admin (`ADMIN_RESOLVED`).
   - **Verification Audit Trail:** Every verification or rejection records the acting user ID, timestamp, previous status, new status, and reason in `audit_logs`.
4. **Evidence Storage Security:**
   - Uploaded UPI QR images, payment receipts, and inspection media are stored in Cloudinary/S3 using secure, non-public object keys accessed via short-lived presigned URLs.

---

## 6. Future-Phase Marketplace Payment Provider Integration

The platform architecture remains strictly provider-agnostic. A clean boundary separates MVP manual collection from future automated marketplace payments:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│               FUTURE-PHASE MARKETPLACE PAYMENT GATEWAY ARCHITECTURE                    │
│                                                                                        │
│  [Customer] ────(Online Checkout)────> [Marketplace Payment Provider (e.g. Razorpay)]  │
│                                                   │                                    │
│                     ┌─────────────────────────────┴──────────────────────────────┐     │
│                     ▼                                                            ▼     │
│        [Split to Partner Linked Account]                           [Platform Commission]│
│        (Automated Marketplace Transfer)                            (Platform Revenue)  │
│                                                                                        │
│  FUTURE-PHASE SCHEMA (8 Tables Activated Upon Provider Onboarding):                   │
│  1. payment_webhook_events      - Inward gateway webhook idempotency ledger            │
│  2. partner_commissions         - Automated platform commission calculations           │
│  3. payout_methods              - Tokenized partner linked bank/UPI accounts           │
│  4. settlements                 - Outward automated settlement batches                 │
│  5. ledger_accounts             - Master Chart of Accounts (Platform double-entry)     │
│  6. ledger_transactions         - Double-entry journal transaction headers             │
│  7. ledger_entries              - Append-only double-entry journal line items          │
│  8. platform_commission_rules   - Configurable category/tier commission rules          │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### Future Migration Capabilities (Activated Only After Legal/Commercial Gates):
- Partner linked merchant account onboarding (KYC verification with provider).
- Online checkout with automated split settlements (e.g., Razorpay Route).
- Inward payment webhook listeners and signature verification.
- Automated refund APIs.
- Platform double-entry general ledger tracking platform commission revenue and settlement clearing.

---

## 7. Outstanding Validation Gates Before Production

Before launching production operations in Vijayawada, the following legal and operational gates must be formally cleared:

1. **Legal Validation:** Confirm the bilateral customer-partner rental agreement and the partner-as-payee model under Indian contract and consumer protection laws.
2. **Tax Validation:** Confirm GST classification (pure technology marketplace intermediary vs. E-Commerce Operator under CGST Act Section 9(5)).
3. **Insurance Validation:** Verify that rental partners hold valid commercial rental vehicle insurance policies covering self-drive rentals and third-party liabilities.
4. **Provider Validation:** Review terms and eligibility prerequisites with RBI-authorized payment aggregators before initiating future marketplace gateway onboarding.
