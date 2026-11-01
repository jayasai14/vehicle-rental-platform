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
│     [Platform: rental_payment_submissions] ─────────────────────> [Booking CONFIRMED]   │
│                                                                                        │
│ ────────────────────────────────────────────────────────────────────────────────────── │
│  2. SECURITY DEPOSIT CUSTODY & REFUND (Direct Partner Custody)                         │
│     Deposit Custody: Partner directly collects and holds customer deposit              │
│     Deductions: Evidence-based damage/fuel claims (Disputes arbitrated by Admin)       │
│     Refund: Partner directly returns deposit via UPI/Bank outside platform             │
│     [Platform: security_deposits & security_deposit_refunds] (Tracks proof & status)   │
│                                                                                        │
│ ────────────────────────────────────────────────────────────────────────────────────── │
│  3. CONTRIBUTOR COMPENSATION (Approach A Direct Settlement)                            │
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
| **Security Deposit Custodian** | **Rental Partner** | Partner receives and holds security deposit directly in partner's bank/UPI account or cash. | Tracked in `security_deposits` as partner-held customer liability. Platform records expected deposit, collected amount, and UTR. Platform does NOT hold escrow. |
| **Customer Refund Execution** | **Rental Partner** | Partner executes external bank transfer/UPI back to customer for cancellations or deposit release. | Platform records refund obligations, partner UTR proof, and customer confirmations in `security_deposit_refunds`. Platform never triggers gateway transfers. |
| **Payment Verification Gate** | **Rental Partner** | Partner independently inspects bank account/UPI app statements for inward credit. | **Strict Invariant:** ONLY partner verification marks payment as verified. Customer screenshot/UTR never auto-confirms payment. |
| **Contributor Compensation Debtor** | **Rental Partner** | Partner pays contributor agreed fixed compensation directly outside platform via NEFT/IMPS/UPI. | Obligation tracked in `contributor_payables`. Single lump-sum model per completed booking. Platform never disburses contributor funds. |
| **Dispute Arbitration** | **Platform Admin** | Evaluates inspection photos, damage claims, and payment receipts if parties disagree. | Support tickets, incident mediation, and deduction approval workflows (`deposit_deductions`). |
| **Platform Monetization** | **Platform Operating Entity** | Separate commercial model (e.g., partner subscriptions or listing fees). | **Zero deductions from customer rental payments.** No commission splitting in MVP. |

> [!IMPORTANT]
> **No Merchant of Record Claim:** The platform is **not** the Merchant of Record collecting customer rental money in the MVP. The partner-as-service-provider and partner-as-payee model is an architectural decision that eliminates platform custody risks and regulatory payment aggregator overhead, subject to formal legal, tax, and insurance validation before launch.

---

## 2. Retained MVP Financial & Operational Tables (Module 7: 7 Tables)

Every retained financial table in Module 7 records real-world obligations, submissions, and evidence without representing external transfers as platform cash:

### 2.1 `partner_payment_profiles`
- **What it Records:** Managing partner's direct payment receiving configuration: public payee name, UPI ID (VPA), Cloudinary object key for UPI QR image, external payment URL, accepted payment methods JSON, and whether pay-at-pickup is enabled.
- **Who Moves Money:** Customer transfers money to these partner coordinates.
- **Responsible Entity:** Rental Partner (responsible for accuracy of payment coordinates).
- **Security Control:** NEVER stores sensitive bank credentials, UPI PINs, or OTPs. QR images and payment evidence are protected by secure access controls.

### 2.2 `rental_payment_demands`
- **What it Records:** The explicit payment obligation demanded for a booking hold, booking extension, or damage invoice (e.g., "Pay ₹2,400 to Partner X for Hold Y").
- **Who Moves Money:** Generated by the platform system upon customer checkout initiation; settled by customer direct payment.
- **Responsible Entity:** Platform generates demand based on vehicle pricing and deposit rules; Partner receives funds.

### 2.3 `rental_payment_submissions`
- **What it Records:** Customer-submitted evidence of payment: payment method used (`UPI_QR`, `UPI_ID`, `EXTERNAL_LINK`, `CASH_AT_PICKUP`), transaction reference (Bank UTR / Cash receipt), exact amount transferred, transaction timestamp, receipt image key, and status (`SUBMITTED_FOR_VERIFICATION`, `VERIFIED`, `REJECTED`).
- **Who Moves Money:** Customer moves money directly to partner outside platform.
- **Responsible Entity:** Customer submits proof; Managing Partner verifies or rejects.
- **Duplicate Prevention:** Unique constraint `uk_submissions_partner_ref` (`manager_partner_id`, `transaction_reference`) prevents recycling references across bookings.

### 2.4 `security_deposits`
- **What it Records:** Tracking of customer security deposits held directly by the rental partner: required amount, collected amount, collection method, collection reference (UTR), held amount, deducted amount, refunded amount, and lifecycle status.
- **Who Moves Money:** Customer deposits with partner; partner holds funds.
- **Responsible Entity:** Rental Partner is the legal and physical custodian.

### 2.5 `deposit_deductions`
- **What it Records:** Itemized damage, fuel deficit, late return, or traffic fine claims against a security deposit, with mandatory inspection evidence and status (`PROPOSED`, `DISPUTED`, `APPROVED_BY_ADMIN`, `AGREED_BY_CUSTOMER`, `CANCELLED`).
- **Who Moves Money:** Executed via deposit retention by partner; no cash moves if settled from deposit.
- **Responsible Entity:** Rental Partner proposes; Customer agrees or disputes; Admin arbitrates unresolved claims.

### 2.6 `security_deposit_refunds`
- **What it Records:** Audit trail of deposit refunds executed directly by the partner: refund amount, refund method, transaction reference (Bank UTR), proof document, refund timestamp, customer confirmation, and dispute notes.
- **Who Moves Money:** Rental Partner transfers funds directly to customer bank/UPI outside the platform.
- **Responsible Entity:** Rental Partner executes refund; Platform monitors and audits.

### 2.7 `contributor_payables`
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
   - Customers can only view their own payment demands, submissions, and deposit refund records.
   - Contributors can only view their own contribution payables and agreement terms.
3. **Anti-Fraud Controls:**
   - **No Auto-Verification:** Customer uploads NEVER automatically verify payments.
   - **Unique Reference Constraints:** Duplicate UTRs for the same partner are rejected by database unique constraints.
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
