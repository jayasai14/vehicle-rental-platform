# Financial Architecture & Double-Entry Ledger Specification

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh  
**Status:** Final Pre-Freeze Baseline (Approved 54-Table Architecture)  
**Governing Principle:** **The platform is never the legal debtor for contributor compensation.** The vehicle contributor receives an agreed fixed compensation per eligible completed booking directly under a commercial contract with the rental partner. Direct payments by partners to contributors must never be represented as cash movements through the platform general ledger, because the platform never receives, holds, or transfers those funds.

---

## 1. Separation of Financial Models: Platform GL vs. Partner Obligations

To ensure legal, regulatory, and accounting integrity, the platform strictly separates three distinct financial domains:

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              MARKETPLACE FINANCIAL SEPARATION                          │
│                                                                                        │
│  1. PLATFORM GENERAL LEDGER                                                            │
│     [Customer] ──(Rental + Deposit)──> [Platform Gateway Clearing]                     │
│                                                      │                                 │
│                        ┌─────────────────────────────┴────────────────────────────┐    │
│                        ▼                                                          ▼    │
│            [Platform Commission]                                          [Partner Net]│
│            (Gross Platform Revenue)                                        (Platform   │
│                                                                             Liability) │
│                                                                                        │
│ ────────────────────────────────────────────────────────────────────────────────────── │
│  2. PARTNER-CONTRIBUTOR OBLIGATION TRACKING (contributor_payables)                     │
│     Contractual Debt: Rental Partner ──(Agreed Fixed Fee)──> Contributor                │
│     (Tracked by platform; NOT a platform cash liability or balance sheet debt)        │
│                                                                                        │
│ ────────────────────────────────────────────────────────────────────────────────────── │
│  3. DIRECT PARTNER SETTLEMENT EVIDENCE (Approach A MVP Baseline)                       │
│     [Partner Bank Account] ──(Direct NEFT/IMPS/UPI)──> [Contributor Bank Account]      │
│     Recorded in Platform: UTR Reference Number, Payment Date, Receipt Attachment       │
│     Verification: Contributor digital acknowledgement or dispute in platform UI        │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### 1.1 The Three Financial Domains Defined:

1. **Platform General Ledger (`ledger_accounts`, `ledger_transactions`, `ledger_entries`):**
   - Tracks **only** actual platform cash flows, platform revenues, and platform legal liabilities.
   - Platform cash incoming via payment gateway: `GATEWAY_CLEARING`.
   - Customer deposits held as a platform liability: `CUSTOMER_REFUNDABLE_LIABILITY`.
   - Customer rental fees held in transit: `CUSTOMER_RENTAL_CLEARING`.
   - Platform marketplace commission: `PLATFORM_COMMISSION_REVENUE`.
   - Net rental proceeds owed by platform to rental partner: `PARTNER_PAYABLE`.
   - Outward customer refunds in transit: `REFUND_PAYABLE`.
   - Outward bank disbursements from platform to partner: `SETTLEMENT_CLEARING`.
2. **Partner-Contributor Contractual Obligation Model (`contributor_payables`):**
   - Represents the **partner's legal debt to the contributor** under their bilateral commercial agreement.
   - Stored in a dedicated domain table (`contributor_payables`), **strictly separate from the platform general ledger**.
   - The platform calculates the payable, tracks eligibility upon completed vehicle return, monitors due dates, and displays the payable in partner and contributor dashboards.
   - It is **never** credited as a liability of the platform in `ledger_entries`.
3. **Direct Partner Settlement Evidence (Approach A MVP):**
   - When the partner pays the contributor directly outside the platform (via bank transfer/UPI), the partner logs the payment reference (Bank UTR number, date, receipt document).
   - This records the operational discharge of the partner's obligation in `contributor_payables`.
   - **It generates NO cash movement entries in the platform general ledger**, because platform bank funds are completely untouched.

### 1.2 Customer Payment, Deposit Custody, and Partner Settlement Demarcation

To eliminate legal, regulatory, and tax ambiguity, the marketplace explicitly establishes the operational and legal demarcations for customer checkout and partner payouts:

| Responsibility Domain | Responsible Legal Entity | Architectural & Ledger Manifestation |
|---|---|---|
| **Merchant of Record (MoR)** | **Platform Operating Entity** | Acts as the digital marketplace MoR for customer-facing checkout transactions on the web/app. Collects total charges, issues platform facilitation tax invoices, and integrates with RBI-authorized payment aggregators. *(Subject to pending legal validation regarding GST Section 9(5) E-Commerce Operator classification).* |
| **Customer Payment Recipient** | **Platform Payment Gateway Merchant Account** | Gateway captures customer checkout funds (rental fee + security deposit) into the platform's clearing pipeline (`GATEWAY_CLEARING`). Customer funds are segregated on the ledger into rental clearing and deposit liabilities. |
| **Security Deposit Holder** | **Platform Operating Entity** | Holds customer security deposits as a **Refundable Balance Sheet Liability** (`CUSTOMER_REFUNDABLE_LIABILITY`). Deposits are never treated as platform revenue, working capital, or an unregulated private "escrow". |
| **Customer Refund Responsibility** | **Platform Operating Entity** | Bears sole operational and contractual responsibility for processing customer refunds (cancellations, clean return deposit releases, dispute adjustments, or late hold collision refunds) directly to the original payment instrument via payment gateway refund APIs (`REFUND_PAYABLE`). Partners never refund customers directly. |
| **Partner Rental Settlement** | **Platform Operating Entity** | Contractually obligated to disburse net rental proceeds (`PARTNER_PAYABLE`) to verified partners on a scheduled batch cycle (e.g. T+2 post return) via payout gateway disbursement APIs (`SETTLEMENT_CLEARING`). Net proceeds equal gross rental subtotal minus platform commission, applicable gateway fees, partner-funded discounts, and mandated tax withholdings (TDS/TCS). |
| **Payment Provider Responsibilities** | **Payment Aggregator / Gateway Partner** | PCI-DSS compliant payment card handling, UPI intent/collect flows, fraud checks, instant webhook callbacks, automated refund execution, and scheduled bank settlement into platform clearing accounts per RBI Payment Aggregator guidelines. |

> [!IMPORTANT]
> **Method-Independent Gateway Architecture:** Payment provider integrations are abstracted behind vendor-agnostic service contracts (`PaymentGatewayClient`, `PayoutGatewayClient`). The database schema, state machines, and double-entry ledger remain decoupled from any single proprietary gateway mechanism.
> 
> **Separation from Approach A:** Direct partner-to-contributor settlement under Approach A applies **strictly and exclusively** to the private bilateral commercial contract between the Rental Partner and the Vehicle Contributor. **Approach A does NOT apply to, nor does it establish regulatory compliance for, the separate customer payment collection and partner rental settlement flow.** Customer checkout and partner payout compliance is established independently under marketplace facilitator regulations and commercial contracts.

---

## 2. Normalized Double-Entry Database Ledger Structure (54-Table Design)

The platform general ledger uses an immutable, append-only, fully normalized double-entry database design comprising **three physical tables**:

### 2.1 `ledger_accounts` (Chart of Accounts Master)
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Important Columns:**
  - `account_code` (VARCHAR 40, UNIQUE): Unique identifier for the account (e.g. `GATEWAY_CLEARING`).
  - `account_name` (VARCHAR 100): Human-readable name.
  - `account_category` (ENUM: `ASSET`, `LIABILITY`, `EQUITY`, `REVENUE`, `EXPENSE`).
  - `normal_balance` (ENUM: `DEBIT`, `CREDIT`).
  - `currency` (CHAR(3) DEFAULT 'INR').
  - `is_active` (BOOLEAN DEFAULT TRUE).
  - `created_at` (DATETIME).
- **Constraints:** `uk_ledger_accounts_code` (`account_code`).

### 2.2 `ledger_transactions` (Journal Transaction Headers)
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Important Columns:**
  - `transaction_group_id` (VARCHAR 64, UNIQUE): Unique correlation identifier grouping balancing entries (e.g. `TXG-CHK-1001`).
  - `reference_type` (ENUM: `BOOKING`, `PAYMENT_ORDER`, `SECURITY_DEPOSIT`, `REFUND`, `PARTNER_SETTLEMENT`, `REVERSAL_ADJUSTMENT`).
  - `reference_id` (BIGINT UNSIGNED): Foreign key ID of the underlying operational entity.
  - `narrative` (VARCHAR 255): Descriptive context for the accounting entry.
  - `posted_at` (DATETIME): Accounting effective timestamp.
  - `created_at` (DATETIME): Immutable record creation timestamp.
- **Constraints:** `uk_ledger_tx_group` (`transaction_group_id`).
- **Indexes:** `idx_ledger_tx_ref` (`reference_type`, `reference_id`), `idx_ledger_tx_posted` (`posted_at`).

### 2.3 `ledger_entries` (Journal Transaction Line Items)
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:**
  - `transaction_id` $\rightarrow$ `ledger_transactions.id` (`ON DELETE RESTRICT`)
  - `account_id` $\rightarrow$ `ledger_accounts.id` (`ON DELETE RESTRICT`)
- **Important Columns:**
  - `sequence_number` (SMALLINT UNSIGNED): 1-indexed line number within transaction.
  - `debit_amount` (DECIMAL(12,2) DEFAULT 0.00): Must be $\ge 0.00$.
  - `credit_amount` (DECIMAL(12,2) DEFAULT 0.00): Must be $\ge 0.00$.
  - `created_at` (DATETIME).
- **Check Constraints:** `chk_ledger_entry_amounts` (`debit_amount >= 0.00 AND credit_amount >= 0.00 AND (debit_amount > 0.00 OR credit_amount > 0.00)`).
- **Indexes:** `idx_ledger_entries_account` (`account_id`, `created_at`), `idx_ledger_entries_tx` (`transaction_id`).

### 2.4 Journal Balancing Invariant Rule:
Every transaction header must have at least two entries whose sums strictly balance:
$$\sum_{i=1}^{n} \text{ledger\_entries.debit\_amount} = \sum_{i=1}^{n} \text{ledger\_entries.credit\_amount}$$
Balances are derived projections. Mutable balance columns are strictly forbidden. Corrections are executed **exclusively via offsetting reversal entries**.

---

## 3. Platform Chart of Accounts (Master Records in `ledger_accounts`)

| Account Code | Category | Normal Balance | Description & Legal Ownership |
|---|---|---|---|
| `GATEWAY_CLEARING` | Asset | Debit | Funds captured by payment gateway awaiting bank settlement to platform bank account. |
| `CUSTOMER_REFUNDABLE_LIABILITY` | Liability | Credit | Customer security deposits held by the platform as refundable liabilities. (Never platform revenue or escrow). |
| `CUSTOMER_RENTAL_CLEARING` | Liability | Credit | Unearned customer rental payments held in transit awaiting booking confirmation. |
| `PLATFORM_COMMISSION_REVENUE` | Revenue | Credit | Earned marketplace commission recognized upon booking confirmation. |
| `PARTNER_PAYABLE` | Liability | Credit | Net rental proceeds owed by platform to rental partner after platform commission. |
| `PARTNER_RECEIVABLE` | Asset | Debit | Amounts owed by rental partners to the platform arising from post-payout reversals, disputed damage claim clawbacks, overpayments, or unpaid operational penalties. |
| `REFUND_PAYABLE` | Liability | Credit | Outward customer refunds approved and queued for payment gateway execution. |
| `SETTLEMENT_CLEARING` | Asset / Clearing | Debit | Outgoing disbursement funds in transit from platform bank account to partner bank account. |

---

## 4. Platform Double-Entry Accounting Journal Scenarios

### Scenario 1: Initial Customer Checkout Payment
*Customer pays ₹5,000 Rental Charge + ₹3,000 Security Deposit = ₹8,000 total via Razorpay.*

```text
Transaction Header: TXG-CHK-1001 | Ref: BOOKING #BK-9021 | Posted: 2026-11-01 10:00:00
Narrative: Upfront checkout payment captured via Razorpay
-----------------------------------------------------------------------------------------
Account Code                     | Debit (₹)   | Credit (₹)  | Line Item Narrative
-----------------------------------------------------------------------------------------
GATEWAY_CLEARING                 | 8,000.00    | 0.00        | Total customer payment captured
CUSTOMER_RENTAL_CLEARING         | 0.00        | 5,000.00    | Rental fee held in clearing
CUSTOMER_REFUNDABLE_LIABILITY    | 0.00        | 3,000.00    | Security deposit held as liability
-----------------------------------------------------------------------------------------
TOTALS                           | 8,000.00    | 8,000.00    | BALANCED
```

---

### Scenario 2: Booking Confirmation & Commission Recognition
*Booking confirmed. Platform earns 15% commission (₹750); Net Partner entitlement is ₹4,250.*

```text
Transaction Header: TXG-CNF-1001 | Ref: BOOKING #BK-9021 | Posted: 2026-11-01 10:00:05
Narrative: Rental fee earned and net partner entitlement recognized
-----------------------------------------------------------------------------------------
Account Code                     | Debit (₹)   | Credit (₹)  | Line Item Narrative
-----------------------------------------------------------------------------------------
CUSTOMER_RENTAL_CLEARING         | 5,000.00    | 0.00        | Rental clearing earned
PLATFORM_COMMISSION_REVENUE      | 0.00        | 750.00      | 15% marketplace commission
PARTNER_PAYABLE                  | 0.00        | 4,250.00    | Net partner rental proceeds owed
-----------------------------------------------------------------------------------------
TOTALS                           | 5,000.00    | 5,000.00    | BALANCED
```

---

### Scenario 3: Clean Vehicle Return & 100% Deposit Refund
*Vehicle returned in pristine condition. Full ₹3,000 security deposit refunded via Razorpay refund API.*

```text
Transaction Header: TXG-DEP-REF-1001 | Ref: SECURITY_DEPOSIT #DEP-501 | Posted: 2026-11-03 18:00:00
Narrative: Security deposit released in full to customer
-----------------------------------------------------------------------------------------
Account Code                     | Debit (₹)   | Credit (₹)  | Line Item Narrative
-----------------------------------------------------------------------------------------
CUSTOMER_REFUNDABLE_LIABILITY    | 3,000.00    | 0.00        | Deposit liability extinguished
REFUND_PAYABLE                   | 0.00        | 3,000.00    | Outward refund queued
-----------------------------------------------------------------------------------------
REFUND_PAYABLE                   | 3,000.00    | 0.00        | Gateway refund executed
GATEWAY_CLEARING                 | 0.00        | 3,000.00    | Gateway clearing adjusted
-----------------------------------------------------------------------------------------
TOTALS                           | 6,000.00    | 6,000.00    | BALANCED
```

---

### Scenario 4: Return Inspection with Approved Damage Claim & Partial Deposit Refund
*Return inspection logs scratch damage: ₹1,000 approved deduction credited to partner. Undisputed ₹2,000 refunded immediately to customer.*

```text
Transaction Header: TXG-DEP-CLM-1001 | Ref: SECURITY_DEPOSIT #DEP-502 | Posted: 2026-11-03 18:30:00
Narrative: Approved damage claim credited to partner; residual deposit refunded
-----------------------------------------------------------------------------------------
Account Code                     | Debit (₹)   | Credit (₹)  | Line Item Narrative
-----------------------------------------------------------------------------------------
CUSTOMER_REFUNDABLE_LIABILITY    | 3,000.00    | 0.00        | Deposit liability extinguished
PARTNER_PAYABLE                  | 0.00        | 1,000.00    | Damage repair funds allocated to partner
REFUND_PAYABLE                   | 0.00        | 2,000.00    | Residual deposit refund queued
-----------------------------------------------------------------------------------------
REFUND_PAYABLE                   | 2,000.00    | 0.00        | Gateway refund executed
GATEWAY_CLEARING                 | 0.00        | 2,000.00    | Gateway clearing adjusted
-----------------------------------------------------------------------------------------
TOTALS                           | 5,000.00    | 5,000.00    | BALANCED
```

---

### Scenario 5: Platform Disbursement to Rental Partner
*Platform disburses net rental proceeds of ₹4,250 to Rental Partner bank account via RazorpayX payout.*

```text
Transaction Header: TXG-SET-PRT-1001 | Ref: PARTNER_SETTLEMENT #SET-901 | Posted: 2026-11-05 11:00:00
Narrative: Outward net rental payout disbursed to partner bank account
-----------------------------------------------------------------------------------------
Account Code                     | Debit (₹)   | Credit (₹)  | Line Item Narrative
-----------------------------------------------------------------------------------------
PARTNER_PAYABLE                  | 4,250.00    | 0.00        | Partner liability discharged
SETTLEMENT_CLEARING              | 0.00        | 4,250.00    | Outgoing platform bank disbursement
-----------------------------------------------------------------------------------------
TOTALS                           | 4,250.00    | 4,250.00    | BALANCED
```

---

### Scenario 6: Financial Correction BEFORE Partner Payout (Intact Partner Payable)
*An erroneous damage deduction of ₹1,000 credited to a partner is reversed before the partner payout batch runs. The existing `PARTNER_PAYABLE` balance is sufficient to absorb the debit.*

```text
Transaction Header: TXG-REV-PREPAY-1001 | Ref: REVERSAL_ADJUSTMENT #REV-001 | Posted: 2026-11-04 14:00:00
Narrative: Pre-payout reversal of erroneous partner damage credit on #DEP-502
-----------------------------------------------------------------------------------------
Account Code                     | Debit (₹)   | Credit (₹)  | Line Item Narrative
-----------------------------------------------------------------------------------------
PARTNER_PAYABLE                  | 1,000.00    | 0.00        | Reversal of unremitted partner credit
REFUND_PAYABLE                   | 0.00        | 1,000.00    | Reversal credit queued for customer refund
-----------------------------------------------------------------------------------------
REFUND_PAYABLE                   | 1,000.00    | 0.00        | Gateway refund executed to customer
GATEWAY_CLEARING                 | 0.00        | 1,000.00    | Gateway clearing adjusted
-----------------------------------------------------------------------------------------
TOTALS                           | 2,000.00    | 2,000.00    | BALANCED
```

---

### Scenario 7: Financial Reversal AFTER Partner Payout (Creation of Partner Receivable)
*A ₹1,000 damage deduction previously credited and already paid out to the partner's external bank account (Scenario 5) is reversed after Admin dispute arbitration. The customer must be refunded immediately, but the partner has already received the payout funds.*

```text
Transaction Header: TXG-REV-POSTPAY-1001 | Ref: REVERSAL_ADJUSTMENT #REV-002 | Posted: 2026-11-06 14:00:00
Narrative: Post-payout reversal of damage credit on #DEP-502; customer refund queued and partner receivable established
-----------------------------------------------------------------------------------------
Account Code                     | Debit (₹)   | Credit (₹)  | Line Item Narrative
-----------------------------------------------------------------------------------------
PARTNER_RECEIVABLE               | 1,000.00    | 0.00        | Legal claim / receivable owed by partner
REFUND_PAYABLE                   | 0.00        | 1,000.00    | Reversal credit queued for customer refund
-----------------------------------------------------------------------------------------
REFUND_PAYABLE                   | 1,000.00    | 0.00        | Gateway refund executed to customer
GATEWAY_CLEARING                 | 0.00        | 1,000.00    | Gateway clearing adjusted
-----------------------------------------------------------------------------------------
TOTALS                           | 2,000.00    | 2,000.00    | BALANCED
```

---

### Scenario 8: Recovery of Partner Receivable

#### Option 8A: Automatic Offset Against Subsequent Partner Rental Settlement Batch
*Partner completes subsequent booking #BK-9040 earning net rental proceeds of ₹3,000. The platform payout engine automatically deducts the ₹1,000 outstanding `PARTNER_RECEIVABLE` and disburses the net ₹2,000 to the partner's bank account.*

```text
Transaction Header: TXG-SET-OFFSET-1001 | Ref: PARTNER_SETTLEMENT #SET-905 | Posted: 2026-11-08 11:00:00
Narrative: Partner net settlement for #BK-9040 with automated clawback of outstanding PARTNER_RECEIVABLE
-----------------------------------------------------------------------------------------
Account Code                     | Debit (₹)   | Credit (₹)  | Line Item Narrative
-----------------------------------------------------------------------------------------
PARTNER_PAYABLE                  | 3,000.00    | 0.00        | Gross partner entitlement on #BK-9040 discharged
PARTNER_RECEIVABLE               | 0.00        | 1,000.00    | Outstanding receivable recovered / extinguished
SETTLEMENT_CLEARING              | 0.00        | 2,000.00    | Net disbursement to partner bank account
-----------------------------------------------------------------------------------------
TOTALS                           | 3,000.00    | 3,000.00    | BALANCED
```

#### Option 8B: Direct Partner Repayment (Bank Transfer / Payment Link)
*Partner has no upcoming bookings and pays ₹1,000 directly back to the platform operating bank account.*

```text
Transaction Header: TXG-RCV-DIRECT-1001 | Ref: REVERSAL_ADJUSTMENT #REV-003 | Posted: 2026-11-09 10:00:00
Narrative: Direct repayment received from partner to clear outstanding PARTNER_RECEIVABLE
-----------------------------------------------------------------------------------------
Account Code                     | Debit (₹)   | Credit (₹)  | Line Item Narrative
-----------------------------------------------------------------------------------------
GATEWAY_CLEARING                 | 1,000.00    | 0.00        | Repayment funds captured into platform clearing
PARTNER_RECEIVABLE               | 0.00        | 1,000.00    | Partner receivable extinguished
-----------------------------------------------------------------------------------------
TOTALS                           | 1,000.00    | 1,000.00    | BALANCED
```

---

## 5. Partner-to-Contributor Settlement Model (`contributor_payables`)

Under **Approach A (Recommended MVP Baseline)**, contributor fixed fees are **never recorded as platform cash disbursements**. The platform manages the workflow without assuming debtor liability:

### 5.1 Selected Settlement Approach: Single Complete Settlement with Immutable Evidence
To preserve the 54-table baseline and prevent state synchronization defects, the architecture formally adopts:
**One complete direct settlement per contributor payable, with immutable payment evidence (Single Lump-Sum Model).**

- **Indivisible Booking Obligation:** Each completed booking generates exactly one contributor payable record representing the agreed fixed compensation ($K_{contrib}$). Partial disbursements, installment transfers, or split payments are prohibited at the domain and database levels for single vehicle bookings.
- **Immutable Payment Evidence:** When the partner executes the direct bank transfer (via NEFT/IMPS/UPI) outside the platform, the partner submits:
  1. Bank UTR / Transaction Reference Number (`payout_reference`, VARCHAR 100, mandatory, unique per payable)
  2. Exact settlement timestamp (`paid_at`, DATETIME)
  3. Transfer receipt document (`payment_receipt_url`, object key)
- **Immutability Invariant:** Once transitioned to `SETTLED_BY_PARTNER`, settlement reference fields are locked and immutable.
- **Dispute and Audit Resolution:** If the contributor disputes the transfer (`DISPUTED`), Admin Support arbitrates using the bank UTR evidence. If the transfer was deficient or invalid, the partner executes a corrective transfer under administrative supervision and updates the record via audited administrative events, avoiding the complexity of a 55th entity (`contributor_settlement_events`).

### 5.2 Lifecycle & Evidence Storage in `contributor_payables`:

```text
[Booking COMPLETED]
       │
       ▼
Status: ELIGIBLE ──> Platform calculates fixed fee (e.g. ₹2,000) based on contribution agreement snapshot
       │
       ▼
Status: SCHEDULED ──> Partner notified of due payment date (e.g. within 48h)
       │
       ▼
[Partner Bank Transfer] ──> Partner executes direct NEFT/IMPS/UPI transfer outside platform
       │
       ▼
Status: SETTLED_BY_PARTNER ──> Partner inputs:
                               1. Bank UTR / Transaction Reference (VARCHAR 100)
                               2. Payment Date / Timestamp
                               3. Payment Receipt Document (Object Key)
       │
       ├────────────────────────────────────────┐
       ▼                                        ▼
Status: ACKNOWLEDGED                     Status: DISPUTED
(Contributor verifies bank receipt)      (Contributor challenges invalid UTR / amount)
                                                │
                                                ▼
                                         Admin Support reviews UTR & bank evidence
```

### 5.3 Contributor Payout Accounting Rule:
Because the partner pays the contributor from the partner's own external bank account, **no entries are posted to the platform's general ledger**. The platform simply updates `contributor_payables.payable_status = 'SETTLED_BY_PARTNER'` and stores the UTR audit trail. This prevents false accounting claims that the platform handled or disbursed contributor funds.

---

## 6. Contributor Price-Floor Engine & Pricing Architecture

For `CONTRIBUTOR_OWNED` vehicles, backend validation enforces a mathematical price floor to guarantee that rental revenue covers all contractual and variable costs:

### 6.1 Pricing Components (Separated)
1. **Taxable Rental Amount ($R_{tax}$):** The base rental charge before platform fee and taxes.
2. **Platform Commission ($C_{plat}$):** Percentage or fixed fee earned by the platform.
3. **Gateway Fees ($F_{gw}$):** Transaction processing fee charged by Razorpay (e.g. 2%).
4. **Fixed Contributor Compensation ($K_{contrib}$):** Agreed fixed fee owed by the partner to the owner.
5. **Partner Minimum Margin ($M_{part}$):** Minimum commercial margin required by the partner.
6. **Discount Funding ($D_{fund}$):** Explicit attribution of discounts (platform-funded vs. partner-funded).

### 6.2 Price-Floor Mathematical Invariant

$$\text{Base Rental Price} \ge \frac{K_{contrib} + M_{part} + \text{Allocable Fixed Costs}}{1 - (\text{Commission Rate} + \text{Gateway Fee Rate} + \text{Effective Tax Rate})}$$

> [!CAUTION]
> **No Hardcoded GST Deductions:** Tax calculations must **never** be hardcoded as a simple percentage deduction from rental revenue. Indian GST rules differentiate between Goods Transport, Rent-a-Cab, and E-commerce Marketplace Facilitation (Section 9(5) CGST Act). Tax rates, HSN/SAC codes (e.g. SAC 9966 vs SAC 9973), and GST application rules are **fully configurable** in `platform_commission_rules` and marked **PENDING professional tax validation**.

### 6.3 Discount Attribution Rules:
- If a partner wishes to offer a promotional discount that drops the listing price below the calculated floor, the platform requires an **authorized override**.
- The override explicitly debits the deficit (`partner_discount_subsidy`) directly from `PARTNER_PAYABLE`, leaving the contributor's fixed fee ($K_{contrib}$) 100% protected.
