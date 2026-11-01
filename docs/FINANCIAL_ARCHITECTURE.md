# Financial Architecture & Double-Entry Ledger Specification

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh  
**Status:** Pre-Freeze Corrected Specification  
**Governing Principle:** **The platform is never the legal debtor for contributor compensation.** The vehicle contributor receives an agreed fixed compensation per eligible completed booking directly under a commercial contract with the rental partner. The platform acts as a neutral technology provider that calculates, tracks, and audits this obligation.

---

## 1. Contributor Settlement Architecture: Approach A (MVP) vs. Approach B (Future)

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                              MARKETPLACE FINANCIAL FLOWS                               │
│                                                                                        │
│     [CUSTOMER] ──(Rental + Deposit)──> [PAYMENT GATEWAY / PLATFORM CLEARING]           │
│                                                   │                                    │
│                     ┌─────────────────────────────┴────────────────────────────┐       │
│                     ▼                                                          ▼       │
│         [PLATFORM COMMISSION]                                          [PARTNER REVENUE]│
│       (Gross Platform Revenue)                                                │        │
│                                                                               │        │
│                                              Contractual Obligation Between    │        │
│                                              Partner & Contributor             │        │
│                                                       │                        ▼        │
│                                                       └───────────────> [CONTRIBUTOR]  │
│                                                                         (Fixed Payout) │
└────────────────────────────────────────────────────────────────────────────────────────┘
```

### 1.1 Comparative Analysis & Decision

| Dimension | Approach A: Direct Partner Settlement (RECOMMENDED MVP) | Approach B: Platform-Facilitated Settlement (Razorpay Route) |
|---|---|---|
| **Contractual Debtor** | **Rental Partner is the sole legal debtor.** | Rental Partner remains legal debtor; authorizes platform to split. |
| **Payment Execution** | Partner pays Contributor directly via net banking (NEFT/IMPS/UPI) outside the platform. | Payment gateway automatically splits incoming rental funds to Contributor linked account. |
| **Platform Function** | Platform: 1) Calculates payable, 2) Records obligation, 3) Allows partner to record payment + UTR, 4) Tracks settlement status, 5) Allows contributor confirmation/dispute, 6) Provides admin oversight. | Platform instructs payment gateway Route API to disburse funds from Partner's linked balance. |
| **Legal/Regulatory Risk**| **Zero escrow/aggregator liability.** Platform never holds or routes third-party contributor funds. | Requires Partner linked-account onboarding, KYC, and RBI payment aggregator compliance review. |
| **Audit Trail** | Banking UTR, transaction timestamp, payment receipt attachment, and contributor digital acknowledgement stored in platform. | Gateway transfer ID and webhook reconciliation. |
| **Implementation Status** | **RECOMMENDED BASELINE FOR INITIAL LAUNCH (MVP)** | **FUTURE ENHANCEMENT (PENDING Onboarding & Legal Review)** |

> [!IMPORTANT]
> **No Platform Debt Assumption:** Under Approach A, the platform never records contributor compensation as a direct liability of the platform. Instead, the platform tracks the partner's contractual obligation (`CONTRIBUTOR_PAYABLE`) against the partner's earned rental entitlement (`PARTNER_PAYABLE`), providing full transparency without assuming debtor liability.

---

## 2. Double-Entry Accounting Ledger Structure

The platform uses an append-only, double-entry financial journal. Mutable balance columns are strictly prohibited; account balances are derived projections:
$$\text{Account Balance} = \sum (\text{Credits} - \text{Debits}) \quad \text{or} \quad \sum (\text{Debits} - \text{Credits})$$

### Core Accounting Guarantees:
1. **Transaction Balance:** Every journal transaction group must balance to zero: $\sum \text{Debits} = \sum \text{Credits}$.
2. **Strict Valid Accounts:** Every entry must reference an approved, formal account from the Chart of Accounts.
3. **Transaction Grouping:** Every balanced entry set shares a mandatory, unique `transaction_group_id`.
4. **Immutable Corrections:** Mistakes or financial adjustments are corrected strictly through **reversal entries**, never by modifying or deleting historical rows.
5. **Liability Segregation:** Platform liabilities (`CUSTOMER_REFUNDABLE_LIABILITY`) are strictly segregated from partner contractual obligations (`CONTRIBUTOR_PAYABLE`) and platform revenue (`PLATFORM_COMMISSION_REVENUE`).
6. **Separation of Operational Events from Accounting Recognition:** A booking transitioning to `COMPLETED` operationally does not automatically settle accounts; financial recognition and disbursement follow distinct accounting lifecycles.

---

## 3. Physical Table Design: Confirmed 52-Table Baseline vs. Normalized 54-Table Option

### Confirmed Pre-Freeze Baseline: 50 Application-Domain Tables + 2 Infra = 52 Physical Tables
In the confirmed baseline, the double-entry journal is modeled via `ledger_entries`:
- **`ledger_entries`**: Stores balancing debits and credits with:
  - `transaction_group_id` (VARCHAR 64): Identifiable group tying balancing entries.
  - `sequence_number` (SMALLINT UNSIGNED): Sequence within the group.
  - `account_code` (ENUM): Strict enum representing the approved Chart of Accounts.
  - `debit_amount` (DECIMAL(12,2)): Must be $\ge 0.00$.
  - `credit_amount` (DECIMAL(12,2)): Must be $\ge 0.00$.
  - `reference_type` (VARCHAR 30): `BOOKING`, `PAYMENT`, `DEPOSIT`, `REFUND`, `SETTLEMENT`, `ADJUSTMENT`.
  - `reference_id` (BIGINT UNSIGNED): Foreign entity ID.
  - `narrative` (VARCHAR 255): Explanatory posting text.
  - `created_at` (DATETIME): Immutable timestamp.

### Alternative Normalized Option: 52 Application-Domain Tables + 2 Infra = 54 Physical Tables
If the Product Owner requires relational foreign-key normalization for accounts and transaction headers:
1. **`ledger_accounts`**: Master chart of accounts (`id`, `account_code`, `account_name`, `account_category`, `is_active`, `created_at`).
2. **`ledger_transactions`**: Journal transaction header (`id`, `transaction_group_id`, `reference_type`, `reference_id`, `narrative`, `posted_at`, `created_at`).
3. **`ledger_entries`**: Relational line items (`id`, `transaction_id FK`, `account_id FK`, `debit_amount`, `credit_amount`, `sequence_number`, `created_at`).

> [!NOTE]
> The primary specification retains the confirmed 52 physical tables count. The 54-table normalized option is documented here as an explicit architectural alternative pending final Product Owner database design freeze sign-off.

---

## 4. Formal Chart of Accounts

| Account Code | Account Category | Balance Nature | Description & Legal Ownership |
|---|---|---|---|
| `GATEWAY_CLEARING` | Asset | Debit-normal | Funds captured by Razorpay awaiting bank settlement to the platform. |
| `CUSTOMER_REFUNDABLE_LIABILITY` | Liability | Credit-normal | Customer security deposits held as refundable liabilities. (Never platform revenue or escrow). |
| `CUSTOMER_RENTAL_CLEARING` | Liability | Credit-normal | Unearned customer rental payments awaiting booking confirmation and delivery. |
| `PLATFORM_COMMISSION_REVENUE` | Revenue | Credit-normal | Earned marketplace commission recognized upon booking confirmation. |
| `PARTNER_PAYABLE` | Liability | Credit-normal | Net rental proceeds owed to the rental partner after commission. |
| `CONTRIBUTOR_PAYABLE` | Contractual Tracking | Credit-normal | Fixed fee owed by the partner to the contributor, tracked by the platform. |
| `REFUND_PAYABLE` | Liability | Credit-normal | Outward customer refunds queued for payment gateway execution. |
| `SETTLEMENT_CLEARING` | Asset / Clearing | Debit-normal | Outgoing disbursement funds in transit through banking channels. |

---

## 5. Detailed Accounting Journal Scenarios

### Scenario 1: Upfront Customer Checkout Payment
*Customer pays ₹5,000 Rental Charge + ₹3,000 Security Deposit = ₹8,000 total via Razorpay.*

```text
Transaction Group: TXG-CHK-1001
Reference: Booking #BK-9021 | Event: Payment Captured
-----------------------------------------------------------------------------------------
Account Code                     | Debit (₹)   | Credit (₹)  | Narrative
-----------------------------------------------------------------------------------------
GATEWAY_CLEARING                 | 8,000.00    | 0.00        | Gateway captured total payment
CUSTOMER_RENTAL_CLEARING         | 0.00        | 5,000.00    | Rental fee held in clearing
CUSTOMER_REFUNDABLE_LIABILITY    | 0.00        | 3,000.00    | Security deposit held as liability
-----------------------------------------------------------------------------------------
TOTALS                           | 8,000.00    | 8,000.00    | BALANCED (Zero Discrepancy)
```

---

### Scenario 2: Booking Confirmation & Commission Recognition
*Booking confirmed. Platform recognizes 15% commission (₹750); Net Partner entitlement is ₹4,250.*

```text
Transaction Group: TXG-CNF-1001
Reference: Booking #BK-9021 | Event: Booking Confirmed
-----------------------------------------------------------------------------------------
Account Code                     | Debit (₹)   | Credit (₹)  | Narrative
-----------------------------------------------------------------------------------------
CUSTOMER_RENTAL_CLEARING         | 5,000.00    | 0.00        | Rental clearing earned
PLATFORM_COMMISSION_REVENUE      | 0.00        | 750.00      | 15% marketplace commission
PARTNER_PAYABLE                  | 0.00        | 4,250.00    | Net partner rental proceeds
-----------------------------------------------------------------------------------------
TOTALS                           | 5,000.00    | 5,000.00    | BALANCED (Zero Discrepancy)
```

---

### Scenario 3: Clean Vehicle Return & 100% Deposit Refund
*Vehicle returned in pristine condition. Full ₹3,000 security deposit refunded via Razorpay refund API.*

```text
Transaction Group: TXG-DEP-REF-1001
Reference: Security Deposit #DEP-501 | Event: Deposit Released
-----------------------------------------------------------------------------------------
Account Code                     | Debit (₹)   | Credit (₹)  | Narrative
-----------------------------------------------------------------------------------------
CUSTOMER_REFUNDABLE_LIABILITY    | 3,000.00    | 0.00        | Deposit liability extinguished
REFUND_PAYABLE                   | 0.00        | 3,000.00    | Outward refund queued
-----------------------------------------------------------------------------------------
REFUND_PAYABLE                   | 3,000.00    | 0.00        | Gateway refund executed
GATEWAY_CLEARING                 | 0.00        | 3,000.00    | Gateway clearing adjusted
-----------------------------------------------------------------------------------------
TOTALS                           | 6,000.00    | 6,000.00    | BALANCED (Zero Discrepancy)
```

---

### Scenario 4: Return Inspection with Damage Claim & Partial Deposit Refund
*Return inspection logs scratch damage: ₹1,000 approved deduction. Undisputed ₹2,000 refunded immediately.*

```text
Transaction Group: TXG-DEP-CLM-1001
Reference: Security Deposit #DEP-502 | Event: Deduction Approved & Partial Refund
-----------------------------------------------------------------------------------------
Account Code                     | Debit (₹)   | Credit (₹)  | Narrative
-----------------------------------------------------------------------------------------
CUSTOMER_REFUNDABLE_LIABILITY    | 3,000.00    | 0.00        | Deposit liability extinguished
PARTNER_PAYABLE                  | 0.00        | 1,000.00    | Damage repair credit to partner
REFUND_PAYABLE                   | 0.00        | 2,000.00    | Residual deposit refund to customer
-----------------------------------------------------------------------------------------
REFUND_PAYABLE                   | 2,000.00    | 0.00        | Gateway refund executed
GATEWAY_CLEARING                 | 0.00        | 2,000.00    | Gateway clearing adjusted
-----------------------------------------------------------------------------------------
TOTALS                           | 5,000.00    | 5,000.00    | BALANCED (Zero Discrepancy)
```

---

### Scenario 5: Contributor Obligation Recognition (Contributor-Owned Vehicle)
*Booking completed operationally. Contributor agreement specifies ₹2,000 fixed payment per completed rental. Platform records partner's contractual obligation.*

```text
Transaction Group: TXG-CON-OBL-1001
Reference: Contributor Payable #CP-301 | Event: Booking Completed
-----------------------------------------------------------------------------------------
Account Code                     | Debit (₹)   | Credit (₹)  | Narrative
-----------------------------------------------------------------------------------------
PARTNER_PAYABLE                  | 2,000.00    | 0.00        | Partner entitlement allocated to contributor fee
CONTRIBUTOR_PAYABLE              | 0.00        | 2,000.00    | Contributor contractual entitlement recorded
-----------------------------------------------------------------------------------------
TOTALS                           | 2,000.00    | 2,000.00    | BALANCED (Zero Discrepancy)
```

---

### Scenario 6: Approach A Settlement Execution (Direct Partner Payment)
*Partner settles ₹2,000 directly to Contributor bank account via NEFT/UPI outside the platform and enters UTR #UTR998877.*

```text
Transaction Group: TXG-CON-SET-1001
Reference: Contributor Payable #CP-301 | Event: Partner Direct Payment Recorded (UTR: UTR998877)
-----------------------------------------------------------------------------------------
Account Code                     | Debit (₹)   | Credit (₹)  | Narrative
-----------------------------------------------------------------------------------------
CONTRIBUTOR_PAYABLE              | 2,000.00    | 0.00        | Obligation discharged via partner direct transfer
SETTLEMENT_CLEARING              | 0.00        | 2,000.00    | Direct settlement confirmed with UTR
-----------------------------------------------------------------------------------------
TOTALS                           | 2,000.00    | 2,000.00    | BALANCED (Zero Discrepancy)
```

---

### Scenario 7: Financial Reversal (Correction of Incorrect Charge / Dispute)
*An incorrect late return penalty of ₹500 is reversed after admin dispute arbitration.*

```text
Transaction Group: TXG-REV-1001
Reference: Incident #INC-204 | Event: Penalty Reversal
-----------------------------------------------------------------------------------------
Account Code                     | Debit (₹)   | Credit (₹)  | Narrative
-----------------------------------------------------------------------------------------
PARTNER_PAYABLE                  | 500.00      | 0.00        | Reversal of disputed partner credit
REFUND_PAYABLE                   | 0.00        | 500.00      | Reversal credit queued for customer refund
-----------------------------------------------------------------------------------------
TOTALS                           | 500.00      | 500.00      | BALANCED (Zero Discrepancy)
```

---

## 6. Contributor Price-Floor Engine & Pricing Architecture

For `CONTRIBUTOR_OWNED` vehicles, the platform enforces a pricing floor to ensure that rental revenue covers all fixed obligations and variable costs:

### 6.1 Pricing Components (Separated)
1. **Taxable Rental Amount ($R_{tax}$):** Base rental price before platform fee and taxes.
2. **Platform Commission ($C_{plat}$):** Percentage or fixed fee earned by platform.
3. **Gateway Fees ($F_{gw}$):** Transaction processing fee charged by payment provider (e.g. 2%).
4. **Fixed Contributor Compensation ($K_{contrib}$):** Agreed fixed payment per booking.
5. **Partner Minimum Margin ($M_{part}$):** Minimum profit required by partner.
6. **Discount Funding ($D_{fund}$):** Explicit attribution of discounts (platform-funded vs. partner-funded).

### 6.2 Price-Floor Mathematical Invariant

$$\text{Base Rental Price} \ge \frac{K_{contrib} + M_{part} + \text{Allocable Fixed Costs}}{1 - (\text{Commission Rate} + \text{Gateway Fee Rate} + \text{Effective Tax Rate})}$$

> [!CAUTION]
> **No Hardcoded GST Deductions:** Tax calculations must **never** be hardcoded as a simple percentage deduction from rental revenue. Indian GST laws differentiate between Goods Transport, Rent-a-Cab schemes, and Marketplace Facilitation (Section 9(5) CGST Act). Tax rates, HSN/SAC codes (e.g., 9966 or 9973), and GST application rules are **fully configurable** in `platform_commission_rules` and marked **PENDING professional tax validation**.

### 6.3 Discount Attribution Rules:
- If a partner wishes to offer a promotional discount that drops the listing price below the calculated floor, the platform requires an **authorized override**.
- The override explicitly debits the deficit (`partner_discount_subsidy`) directly from `PARTNER_PAYABLE`, leaving the contributor's fixed fee ($K_{contrib}$) 100% protected.
