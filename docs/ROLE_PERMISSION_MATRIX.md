# Role-Permission & Resource-Level Security Matrix

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh  
**Status:** Database Design Freeze Candidate Baseline (54-Table Architecture)  
**Enforcement:** Spring Security `@PreAuthorize` method security combined with custom domain security evaluators.

---

## 1. Global Role Capabilities Matrix

Roles are **additive**. A single user account can hold `CUSTOMER` and `VEHICLE_CONTRIBUTOR` capabilities simultaneously. `ADMIN` assignment is strictly controlled by trusted administrative processes; public registration endpoints must never permit self-selection of `ADMIN`.

> [!IMPORTANT]
> **Partner Verification Gate:** Submitting a partner application does **not** grant partner operational privileges. A user whose `partner_profiles.verification_status` is `SUBMITTED`, `IN_REVIEW`, or `REJECTED` is strictly barred from fleet registration, listing management, reviewing contributor proposals, or booking fulfillment. Full partner operational privileges are unlocked **only after an Admin formally approves the application (`verification_status = 'VERIFIED'`)**.

| Functional Domain | Operation | CUSTOMER | VEHICLE_CONTRIBUTOR | PENDING_PARTNER | VERIFIED_PARTNER | ADMIN |
|---|---|:---:|:---:|:---:|:---:|:---:|
| **Auth & Profile** | Register / Login / Reset Password | ✅ | ✅ | ✅ | ✅ | ❌ (Internal) |
| | Manage Personal Profile & Addresses | ✅ | ✅ | ✅ | ✅ | ✅ |
| | Submit Partner Business Application | ❌ | ❌ | ✅ (Submitted) | ❌ (Already Verified) | ❌ |
| | Review / Approve Partner Application | ❌ | ❌ | ❌ | ❌ | ✅ |
| **Marketplace** | Search Vehicles & View Public Details | ✅ | ✅ | ✅ | ✅ | ✅ |
| | View Unmasked Registration & RC | ❌ | ❌ (Own vehicle only) | ❌ | ❌ (Managed fleet only) | ✅ |
| **Partner Fleet** | Register Independently Owned Fleet Vehicle | ❌ | ❌ | ❌ (Blocked) | ✅ | ❌ |
| | Manage Partner Hubs & Delivery Zones | ❌ | ❌ | ❌ (Blocked) | ✅ | ❌ |
| | Set Rental Pricing & Turnaround Buffers | ❌ | ❌ | ❌ (Blocked) | ✅ | ✅ |
| **Contributor Fleet** | Submit Contributor Vehicle Application | ❌ | ✅ | ❌ | ❌ | ❌ |
| | Review Contributor Application | ❌ | ❌ | ❌ (Blocked) | ✅ | ✅ |
| | Propose Contribution Agreement | ❌ | ❌ | ❌ (Blocked) | ✅ | ❌ |
| | Accept / Reject Agreement Terms | ❌ | ✅ (Own vehicle) | ❌ | ✅ | ❌ |
| | View Vehicle Documents & Agreement | ❌ | ✅ (Own vehicle) | ❌ | ✅ (Managed fleet) | ✅ |
| | Request Vehicle Withdrawal | ❌ | ✅ (Own vehicle) | ❌ | ❌ | ✅ |
| **Booking & Holds** | Create 15-Minute Booking Hold | ✅ | ❌ | ❌ | ❌ | ❌ |
| | Confirm Booking via Payment | ✅ | ❌ | ❌ | ❌ | ❌ |
| | View Booking Details | ✅ (Own booking) | ❌ (Earnings view only) | ❌ | ✅ (Managed fleet) | ✅ |
| | Cancel Booking (Pre-Pickup) | ✅ (Own booking) | ❌ | ❌ | ✅ (Managed fleet) | ✅ |
| | Request Booking Extension | ✅ (Own booking) | ❌ | ❌ | ❌ | ❌ |
| | Decide Booking Extension | ❌ | ❌ | ❌ | ✅ (Managed fleet) | ✅ |
| | Mark Booking No-Show | ❌ | ❌ | ❌ | ✅ (Managed fleet) | ✅ |
| **Inspections & Handovers** | Participate in Pre-Rental Handover Inspection | ✅ (Customer handover) | ✅ (Direct owner handover) | ❌ | ✅ (Partner-managed) | ✅ |
| | Acknowledge Direct Vehicle Handover | ✅ (Customer pickup) | ✅ (Direct handover/receipt) | ❌ | ✅ (Hub handover) | ✅ |
| | Participate in Post-Rental Return Inspection | ✅ (Customer return) | ✅ (Agreement requirement) | ❌ | ✅ (Partner-managed) | ✅ |
| | Log Vehicle Custody Transfer Event | ❌ | ✅ (Direct handover/return) | ❌ | ✅ (Hub/Relocation) | ✅ |
| **Incidents & Deposit** | Log Damage Claim / Fuel Deficit | ❌ | ❌ | ❌ | ✅ (Managed fleet) | ✅ |
| | Dispute Deposit Deduction (48h Window) | ✅ (Own booking) | ❌ | ❌ | ❌ | ❌ |
| | Review / Arbitrate Deposit Dispute | ❌ | ❌ | ❌ | ❌ | ✅ (Audited) |
| | Trigger Deposit Release / Gateway Refund | ❌ | ❌ | ❌ | ❌ | ✅ (Auto / Admin) |
| | Retry Failed Gateway Refund | ❌ | ❌ | ❌ | ❌ | ✅ (Admin / Finance) |
| | Settle Permanently Failed Refund Offline (NEFT) | ❌ | ❌ | ❌ | ❌ | ✅ (Admin / Finance) |
| **Payouts & Settlement** | Configure Payout Method (Bank/UPI) | ❌ | ✅ (Own account) | ❌ | ✅ (Own account) | ❌ |
| | View Own Earnings & Settlements | ❌ | ✅ (Own vehicles) | ❌ | ✅ (Managed fleet) | ✅ |
| | Record Direct Contributor Settlement (App A)| ❌ | ❌ | ❌ | ✅ (Responsible partner) | ✅ |
| | Acknowledge / Dispute Settlement (App A) | ❌ | ✅ (Own payable) | ❌ | ❌ | ✅ |
| | Manage Partner Receivables & Recovery | ❌ | ❌ | ❌ | ❌ | ✅ (Admin / Finance) |
| | Audit Double-Entry General Ledger | ❌ | ❌ | ❌ | ❌ | ✅ (Audited) |

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
  1. **Manage Independently Registered Fleet:** Register, maintain, price, and operate partner-owned vehicles independently of contributor submissions.
  2. **Manage Accepted Contributor Fleet:** Accept contributor applications, propose binding agreements, configure vehicle listings, and assign vehicles to hubs.
  3. **Control Rental Listings & Pricing:** Manage listing status (published/paused) and set rental prices subject to the backend contributor price-floor engine.
  4. **Conduct Inspections & Manage Bookings:** Perform pre-rental pickup and post-rental return inspections, manage booking extensions, and report incidents/damages.
  5. **Record Direct Contributor Settlements:** Under Approach A, record direct bank payments (NEFT/IMPS/UPI) made to contributors with mandatory UTR references, payment receipts, and dates in `contributor_payables`.

### 2.3 Customer Boundary & Privacy Rights
- A customer must **only** access their own bookings, payment orders, security deposit records, refund statuses, and submitted verification documents.
- Customers must never see contributor fixed fees, partner commission rates, internal ledger journal entries, or private unmasked RC/PAN data of vehicle owners.

### 2.4 Administrative Access Controls
- Admin access to sensitive capabilities (manual refund overrides, dispute arbitration, commission rate modifications, user role alterations) is strictly restricted to authorized administrative personnel.
- All administrative actions must be logged in `audit_logs` with actor ID, timestamp, IP address, correlation ID, and redacted metadata.

---

## 3. Resource-Level Authorization Rules (BOLA / IDOR Defense)

Every secure API endpoint enforces resource-level evaluators via Spring Security:

### A. Booking Access Security (`@bookingSecurity.canAccess(bookingId, principal)`)
- **CUSTOMER:** Permitted ONLY if `bookings.customer_user_id == principal.id`.
- **RENTAL_PARTNER:** Permitted ONLY if `bookings.manager_partner_id == principal.partner_id` AND partner is `VERIFIED`.
- **VEHICLE_CONTRIBUTOR:** Permitted ONLY if the booking was executed on a vehicle owned by the contributor (`vehicles.legal_owner_user_id == principal.id`), restricted strictly to read-only earnings/inspection summaries with customer PII redacted.
- **ADMIN:** Permitted across all bookings with audit logging.

### B. Vehicle Document Access (`@vehicleSecurity.canAccessDocuments(vehicleId, principal)`)
- Unmasked RC documents, insurance policies, and commercial permits are restricted:
  - **Individual Owner (`legal_owner_user_id`):** Full access to own vehicle documents.
  - **Partner Business Owner (`legal_owner_partner_id`):** Full access to fleet documents.
  - **Manager Partner (`manager_partner_id`):** Full access to managed fleet documents if `VERIFIED`.
  - **Customer / Public:** Access denied. Public marketplace responses return only verified badges and masked identifiers (`AP-16-**-1234`).
  - **ADMIN:** Full audit access.

### C. Inspection Media & Damage Evidence (`@inspectionSecurity.canAccess(inspectionId, principal)`)
- Short-lived presigned storage URLs (5-minute TTL) are issued only to:
  - The customer assigned to the booking.
  - The verified partner managing the vehicle.
  - The vehicle owner (contributor) if damage is reported or if owner-managed return applies.
  - Admin/Support arbitrators.

### D. Agreement Security (`@agreementSecurity.canAccess(agreementId, principal)`)
- Access restricted exclusively to:
  - The named contributor (`contributor_user_id == principal.id`).
  - The named verified partner (`partner_id == principal.partner_id`).
  - System administrators.

### E. Settlement & Payout Security (`@settlementSecurity.canAccess(settlementId, principal)`)
- Contributor can view only their own payables and settlements (`contributor_payables.contributor_user_id == principal.id`).
- Partner can view only settlements and payables associated with their managed bookings (`partner_id == principal.partner_id`).
