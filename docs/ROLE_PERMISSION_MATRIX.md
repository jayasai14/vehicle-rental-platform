# Role-Permission & Resource-Level Security Matrix

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh  
**Status:** Database Design Freeze Candidate Baseline (Partner-Direct Payment Model)  
**Enforcement:** Spring Security `@PreAuthorize` method security combined with custom domain security evaluators.

---

## 1. Global Role Capabilities Matrix

Roles are **additive**. A single user account can hold `CUSTOMER` and `VEHICLE_CONTRIBUTOR` capabilities simultaneously. `ADMIN` assignment is strictly controlled by trusted administrative processes; public registration endpoints must never permit self-selection of `ADMIN`.

> [!IMPORTANT]
> **Partner Verification Gate:** Submitting a partner application does **not** grant partner operational privileges. A user whose `partner_profiles.verification_status` is `SUBMITTED`, `IN_REVIEW`, or `REJECTED` is strictly barred from fleet registration, configuring payment profiles, reviewing contributor proposals, or fulfilling bookings. Full partner operational privileges are unlocked **only after an Admin formally approves the application (`verification_status = 'VERIFIED'`)**.

| Functional Domain | Operation | CUSTOMER | VEHICLE_CONTRIBUTOR | PENDING_PARTNER | VERIFIED_PARTNER | ADMIN |
|---|---|:---:|:---:|:---:|:---:|:---:|
| **Auth & Profile** | Register / Login / Reset Password | ✅ | ✅ | ✅ | ✅ | ❌ (Internal) |
| | Manage Personal Profile & Addresses | ✅ | ✅ | ✅ | ✅ | ✅ |
| | Submit Partner Business Application | ❌ | ❌ | ✅ (Submitted) | ❌ (Already Verified) | ❌ |
| | Review / Approve Partner Application | ❌ | ❌ | ❌ | ❌ | ✅ |
| **Marketplace** | Search Vehicles & View Public Details | ✅ | ✅ | ✅ | ✅ | ✅ |
| | View Unmasked Registration & RC | ❌ | ❌ (Own vehicle only) | ❌ | ❌ (Managed fleet only) | ✅ |
| **Partner Fleet** | Register Independently Owned Fleet Vehicle | ❌ | ❌ | ❌ (Blocked) | ✅ | ❌ |
| | Manage Partner Hubs & Pickup Locations | ❌ | ❌ | ❌ (Blocked) | ✅ | ❌ |
| | Set Rental Pricing & Turnaround Buffers | ❌ | ❌ | ❌ (Blocked) | ✅ | ✅ |
| **Contributor Fleet** | Submit Contributor Vehicle Application | ❌ | ✅ | ❌ | ❌ | ❌ |
| | Review Contributor Application | ❌ | ❌ | ❌ (Blocked) | ✅ | ✅ |
| | Propose Contribution Agreement | ❌ | ❌ | ❌ (Blocked) | ✅ | ❌ |
| | Accept / Reject Agreement Terms | ❌ | ✅ (Own vehicle) | ❌ | ✅ | ❌ |
| | View Vehicle Documents & Agreement | ❌ | ✅ (Own vehicle) | ❌ | ✅ (Managed fleet) | ✅ |
| | Request Vehicle Withdrawal | ❌ | ✅ (Own vehicle) | ❌ | ❌ | ✅ |
| **Booking & Holds** | Create 15-Minute Booking Hold | ✅ | ❌ | ❌ | ❌ | ❌ |
| | View Booking Details | ✅ (Own booking) | ❌ (Earnings view only) | ❌ | ✅ (Managed fleet) | ✅ |
| | Cancel Booking (Pre-Pickup) | ✅ (Own booking) | ❌ | ❌ | ✅ (Managed fleet) | ✅ |
| | Request Booking Extension | ✅ (Own booking) | ❌ | ❌ | ❌ | ❌ |
| | Decide Booking Extension | ❌ | ❌ | ❌ | ✅ (Managed fleet) | ✅ |
| | Mark Booking No-Show | ❌ | ❌ | ❌ | ✅ (Managed fleet) | ✅ |
| **Partner Direct Payments**| Configure Partner Payment Profile (UPI QR/ID/Link) | ❌ | ❌ | ❌ (Blocked) | ✅ | ❌ |
| | View Managing Partner Payment Instructions | ✅ (Checkout hold) | ❌ | ❌ | ✅ (Own profile) | ✅ |
| | Submit Direct Payment Proof (UTR, Receipt) | ✅ (Own hold) | ❌ | ❌ | ❌ | ❌ |
| | View Incoming Payment Submissions | ❌ | ❌ | ❌ | ✅ (Managed fleet) | ✅ |
| | Verify Customer Payment Submission | ❌ | ❌ | ❌ | ✅ (Managed fleet) | ❌ |
| | Reject Customer Payment Submission | ❌ | ❌ | ❌ | ✅ (Managed fleet) | ❌ |
| **Inspections & Handovers** | Participate in Pre-Rental Handover Inspection | ✅ (Customer handover) | ✅ (Direct owner handover) | ❌ | ✅ (Partner-managed) | ✅ |
| | Acknowledge Direct Vehicle Handover | ✅ (Customer pickup) | ✅ (Direct handover/receipt) | ❌ | ✅ (Hub handover) | ✅ |
| | Participate in Post-Rental Return Inspection | ✅ (Customer return) | ✅ (Agreement requirement) | ❌ | ✅ (Partner-managed) | ✅ |
| | Log Vehicle Custody Transfer Event | ❌ | ✅ (Direct handover/return) | ❌ | ✅ (Hub/Relocation) | ✅ |
| **Rental Fee Refunds** | Record Direct Rental Fee Refund (Cancellation/Overpayment) | ❌ | ❌ | ❌ | ✅ (Managed fleet) | ✅ |
| | Explicitly Acknowledge Receipt (`CUSTOMER_ACKNOWLEDGED`) | ✅ (Own booking/hold) | ❌ | ❌ | ❌ | ❌ |
| | Dispute Rental Fee Refund (72h Window) | ✅ (Own booking/hold) | ❌ | ❌ | ❌ | ❌ |
| | Arbitrate Dispute (`ADMIN_RESOLVED` with Case & Admin FK) | ❌ | ❌ | ❌ | ❌ | ✅ (Audited) |
| **Deposit & Refunds** | Log Damage Claim / Fuel Deficit | ❌ | ❌ | ❌ | ✅ (Managed fleet) | ✅ |
| | Dispute Deposit Deduction (48h Window) | ✅ (Own booking) | ❌ | ❌ | ❌ | ❌ |
| | Review / Arbitrate Deposit Dispute (Case & Admin FK) | ❌ | ❌ | ❌ | ❌ | ✅ (Audited) |
| | Record Direct Deposit Refund (UTR & Proof) | ❌ | ❌ | ❌ | ✅ (Managed fleet) | ✅ |
| | Explicitly Acknowledge Deposit Refund (`CUSTOMER_ACKNOWLEDGED`) | ✅ (Own booking) | ❌ | ❌ | ❌ | ❌ |
| | Dispute Deposit Refund (72h Window) | ✅ (Own booking) | ❌ | ❌ | ❌ | ❌ |
| | Arbitrate Deposit Refund Dispute | ❌ | ❌ | ❌ | ❌ | ✅ (Audited) |
| **Contributor Compensation**| View Own Earnings & Payables | ❌ | ✅ (Own vehicles) | ❌ | ✅ (Managed fleet) | ✅ |
| | Record Direct Contributor Settlement (App A)| ❌ | ❌ | ❌ | ✅ (Responsible partner) | ✅ |
| | Acknowledge / Dispute Settlement (App A) | ❌ | ✅ (Own payable) | ❌ | ❌ | ✅ |
| **System & Audit** | Query Audit Logs & Security Trails | ❌ | ❌ | ❌ | ❌ | ✅ (Audited) |

---

## 2. Granular Contributor & Partner Capabilities

### 2.1 Vehicle Contributor Rights
A verified vehicle contributor has the explicit capability to:
1. **Participate in Handover Inspections:** For direct handovers (`CONTRIBUTOR_TO_CUSTOMER` or `CONTRIBUTOR_TO_PARTNER`), contributors can inspect the vehicle, log initial condition/mileage/fuel, and submit digital signatures/OTPs.
2. **Acknowledge Direct Handovers:** Confirm physical transfer of custody directly to a customer or partner.
3. **Participate in Return Inspections:** When an agreement specifies owner-managed returns (`CUSTOMER_TO_CONTRIBUTOR`), participate in return inspection checklists and verify vehicle return condition.
4. **View Vehicle Documents & Agreements:** Access unmasked copies of their own registration certificates, insurance policies, and active/historical contribution agreements.
5. **View Earnings & Settlements:** Access a dedicated dashboard tracking fixed earnings per eligible completed booking, payable status, settlement mode, payment reference/UTR, and payment timestamps.
6. **Acknowledge or Dispute Direct Settlements:** Acknowledge receipt of direct partner payments or dispute invalid UTR references in `contributor_payables`.
7. **Request Withdrawal:** Initiate vehicle withdrawal with recommended 7-day notice, subject to agreement terms and confirmed customer booking completion.

### 2.2 Rental Partner Rights & Verification Gate
- **Pending Applicants:** Users who submit a partner application receive `verification_status = 'SUBMITTED'`. They have zero operational partner privileges until verified.
- **Verified Partners (`verification_status = 'VERIFIED'`):**
  1. **Configure Direct Payment Profile:** Upload UPI QR code image, register UPI ID, external payment link, and toggle cash at pickup.
  2. **Manage Independently Registered Fleet:** Register, maintain, price, and operate partner-owned vehicles independently of contributor submissions.
  3. **Manage Accepted Contributor Fleet:** Accept contributor applications, propose binding agreements, configure vehicle listings, and assign vehicles to hubs.
  4. **Verify / Reject Customer Payments:** Independently inspect bank/UPI statements and verify or reject customer payment submissions. **Only partner action can verify a payment.**
  5. **Direct Deposit Custody & Refunds:** Receive security deposits directly, propose evidence-based deductions, and execute direct deposit refunds to customers outside the platform.
  6. **Direct Rental-Fee Refund Execution:** Execute direct rental fee refunds outside the platform for cancellations, overpayment returns, expired-hold exceptions, or dispute resolutions, recording UTR references, receipts, and policy snapshots in `rental_fee_refunds`.
  7. **Record Direct Contributor Settlements:** Under Approach A, record direct bank payments (NEFT/IMPS/UPI) made to contributors with mandatory UTR references, payment receipts, and dates in `contributor_payables`.

### 2.3 Customer Boundary & Privacy Rights
- A customer must **only** access their own bookings, payment demands, payment submissions, security deposit records, rental-fee refund records (`rental_fee_refunds`), refund statuses, and submitted verification documents.
- Customers have a 72-hour window following partner disbursement of a rental-fee refund or security deposit refund to explicitly acknowledge receipt (`CUSTOMER_ACKNOWLEDGED`) or raise a dispute (`DISPUTED_BY_CUSTOMER`).
- An explicit customer UI action is the **only** event that establishes customer acknowledgement; expiry of the 72-hour window transitions status to `DISPUTE_WINDOW_ELAPSED_UNACKNOWLEDGED` and is never treated as customer confirmation.
- Customers must never see contributor fixed fees, partner internal financials, or private unmasked RC/PAN data of vehicle owners.

### 2.4 Administrative Access Controls
- Admin access to sensitive capabilities (dispute arbitration, partner verification, vehicle compliance approval, user role alterations) is strictly restricted to authorized administrative personnel.
- All administrative actions must be logged in `audit_logs` with actor ID, timestamp, IP address, correlation ID, and redacted metadata.

---

## 3. Resource-Level Authorization Rules (BOLA / IDOR Defense)

Every secure API endpoint enforces resource-level evaluators via Spring Security:

### A. Booking Access Security (`@bookingSecurity.canAccess(bookingId, principal)`)
- **CUSTOMER:** Permitted ONLY if `bookings.customer_user_id == principal.id`.
- **RENTAL_PARTNER:** Permitted ONLY if `bookings.manager_partner_id == principal.partner_id` AND partner is `VERIFIED`.
- **VEHICLE_CONTRIBUTOR:** Permitted ONLY if the booking was executed on a vehicle owned by the contributor (`vehicles.legal_owner_user_id == principal.id`), restricted strictly to read-only earnings/inspection summaries with customer PII redacted.
- **ADMIN:** Permitted across all bookings with audit logging.

### B. Payment Submission & Verification Security (`@paymentSecurity.canVerifySubmission(submissionId, principal)`)
- **CUSTOMER:** Can submit payment proof only for holds owned by them (`booking_holds.customer_user_id == principal.id`). Can view only their own payment submissions.
- **RENTAL_PARTNER:** Can view, verify, or reject payment submissions ONLY for demands/bookings where `manager_partner_id == principal.partner_id`. Partner must be in `VERIFIED` status. Cross-partner verification attempts are blocked with `403 Forbidden`.
- **VEHICLE_CONTRIBUTOR:** Access denied (zero access to customer payment submissions).
- **ADMIN:** Audit view only; cannot substitute for partner bank verification.

### C. Security Deposit & Refund Security (`@depositSecurity.canManageDeposit(depositId, principal)`)
- **CUSTOMER:** View only own deposit records; can dispute deductions within 48h; can acknowledge or dispute deposit refunds.
- **RENTAL_PARTNER:** Manage deposit deductions and record direct refunds ONLY for bookings where `manager_partner_id == principal.partner_id`.
- **ADMIN:** Full dispute arbitration and audit access.

### D. Vehicle Document Access (`@vehicleSecurity.canAccessDocuments(vehicleId, principal)`)
- Unmasked RC documents, insurance policies, and commercial permits are restricted:
  - **Individual Owner (`legal_owner_user_id`):** Full access to own vehicle documents.
  - **Partner Business Owner (`legal_owner_partner_id`):** Full access to fleet documents.
  - **Manager Partner (`manager_partner_id`):** Full access to managed fleet documents if `VERIFIED`.
  - **Customer / Public:** Access denied. Public marketplace responses return only verified badges and masked identifiers (`AP-16-**-1234`).
  - **ADMIN:** Full audit access.

### E. Inspection Media & Damage Evidence (`@inspectionSecurity.canAccess(inspectionId, principal)`)
- Short-lived presigned storage URLs (5-minute TTL) are issued only to:
  - The customer assigned to the booking.
  - The verified partner managing the vehicle.
  - The vehicle owner (contributor) if damage is reported or if owner-managed return applies.
  - Admin/Support arbitrators.

### F. Contributor Payable Security (`@payableSecurity.canAccessPayable(payableId, principal)`)
- Contributor can view only their own payables (`contributor_payables.contributor_user_id == principal.id`).
- Partner can view and record settlements only for payables where `contributor_payables.partner_id == principal.partner_id`.

### G. Rental Fee Refund Security (`@rentalRefundSecurity.canManageRefund(refundId, principal)`)
- **CUSTOMER:** Can view only their own rental-fee refunds (`rental_fee_refunds.customer_user_id == principal.id`); can explicitly acknowledge receipt (`CUSTOMER_ACKNOWLEDGED`) or dispute within 72 hours of disbursement (`DISPUTED_BY_CUSTOMER`).
- **RENTAL_PARTNER:** Can view, initiate, and disburse rental-fee refunds ONLY for bookings/holds where `rental_fee_refunds.manager_partner_id == principal.partner_id`. Partner must be in `VERIFIED` status.
- **ADMIN:** Full dispute arbitration, review of expired-hold exceptions, and administrative resolution (`ADMIN_RESOLVED`), recording `dispute_ticket_id` (FK $\rightarrow$ `support_tickets.id`) and `resolved_by_admin_user_id` (FK $\rightarrow$ `users.id`).
