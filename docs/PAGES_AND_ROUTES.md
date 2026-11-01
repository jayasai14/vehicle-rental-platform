# Page and Route Inventory

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh  
**Status:** Database Design Freeze Candidate Baseline (Partner-Direct Payment Model)  

Routes below are proposed for the React + Vite frontend application. Final naming can be adjusted before implementation, but route ownership and access requirements must remain explicit.

---

## 1. Public

| Route | Page | Access |
|---|---|---|
| `/` | Landing page | Public |
| `/vehicles` | Explore vehicles / search results | Public |
| `/vehicles/:vehicleId` | Vehicle details & availability | Public |
| `/how-it-works` | How the platform works | Public |
| `/earn-with-your-vehicle` | Contributor information & onboarding | Public |
| `/about` | About the platform | Public |
| `/help` | Help / FAQ / Contact | Public |
| `/terms` | Terms of service | Public |
| `/privacy` | Privacy policy | Public |
| `/login` | Unified login | Public |
| `/register/customer` | Customer registration | Public |
| `/register/partner` | Partner application | Public |
| `/forgot-password` | Password recovery | Public |
| `/verify-email` | Email OTP verification | Public |

*ADMIN must never be selectable through public registration.*

---

## 2. Customer

| Route | Page | Notes |
|---|---|---|
| `/customer` | Customer dashboard | Recent activity, upcoming rentals |
| `/customer/bookings` | Booking history | Filter by status (`CONFIRMED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`) |
| `/customer/bookings/:bookingId` | Booking details | Rental contract, vehicle details, partner contact, inspection reports |
| `/customer/bookings/:bookingId/deposit` | Security deposit tracking | Held balance, proposed deductions, refund status, dispute button |
| `/customer/profile` | Profile & emergency contacts | Personal details, phone verification |
| `/customer/verification` | Identity & driving licence | OKYC Aadhaar / driving licence upload |
| `/customer/reviews` | My reviews | Submitted vehicle reviews |
| `/customer/support` | Support requests | Open support/dispute tickets |

---

## 3. Booking Flow (Authenticated Customer)

| Route | Page | Notes |
|---|---|---|
| `/checkout/:vehicleId` | Date/time, hub pickup / doorstep selection, price quote | Server acquires 15-minute temporary reservation lock (`booking_holds`) |
| `/checkout/:vehicleId/verification` | Identity & driving licence validation | Required prior to payment instruction display |
| `/checkout/:vehicleId/review` | Rental amount, deposit, and terms review | Displays exact rental charge, security deposit, and partner terms |
| `/checkout/:vehicleId/payment-instructions` | Partner direct payment instructions | Displays managing partner's UPI QR code, UPI ID, payment link, amount due, and hold expiration countdown |
| `/checkout/:vehicleId/submit-payment` | Payment reference submission form | Customer inputs payment method, Bank UTR number, amount, time, and optional receipt screenshot |
| `/checkout/:vehicleId/verification-pending` | Verification pending screen | Hold status `VERIFICATION_PENDING`; displays verification countdown timer (e.g. 60m) while partner checks bank records |
| `/booking-confirmation/:bookingId` | Booking confirmed page | Displayed once managing partner verifies payment (or for authorized pay-at-pickup) |
| `/bookings/:bookingId/extension` | Request rental extension | Extension demand generation |

*Use server-side booking holds and quote validation. Never depend on frontend step order for security or price integrity. Customer upload NEVER auto-confirms booking.*

---

## 4. Vehicle Contributor

| Route | Page | Notes |
|---|---|---|
| `/contributor` | Contributor dashboard | Fleet overview, active rentals, monthly earnings |
| `/contributor/vehicles/new` | Submit vehicle for onboarding | Vehicle specs, RC upload, preferred partner selection |
| `/contributor/vehicles` | Submitted & active vehicles | List of contributor-owned vehicles |
| `/contributor/vehicles/:vehicleId` | Application & vehicle status | Approval status, current custodian (`CONTRIBUTOR_CUSTODY` / `PARTNER_CUSTODY`) |
| `/contributor/agreements` | Bilateral contribution agreements | View signed terms, fixed fee per booking, cancellation terms |
| `/contributor/earnings` | Earnings & payables dashboard | Track fixed compensation per eligible completed booking |
| `/contributor/settlements` | Direct settlement records | History of partner direct transfers, Bank UTRs, dates, and acknowledge/dispute actions |
| `/contributor/availability` | Availability & withdrawal requests | Schedule personal blackout blocks or initiate 7-day vehicle withdrawal |

---

## 5. Rental Partner (Verified Partners Only)

| Route | Page | Notes |
|---|---|---|
| `/partner` | Partner dashboard | Fleet utilization, pending verifications, active rentals |
| `/partner/profile` | Business profile & verification | Legal business name, GSTIN, PAN, trade licence documents |
| `/partner/payment-profile` | Direct payment configuration | Set public payee name, UPI ID, upload UPI QR image, external payment link, toggle cash-at-pickup |
| `/partner/payment-verification` | Inward payment verification queue | Review pending customer submissions (`SUBMITTED_FOR_VERIFICATION`), inspect UTR/proof, click "Verify" or "Reject" |
| `/partner/vehicles` | Partner-managed inventory | Both partner-owned and accepted contributor-owned vehicles |
| `/partner/vehicles/new` | Register partner-owned vehicle | Add new vehicle to fleet |
| `/partner/vehicles/:vehicleId` | Manage listing & pricing | Rates, turnaround buffer, listing status (published/paused) |
| `/partner/hubs` | Service hubs & depots | Manage physical operating locations and pickup points |
| `/partner/contributions` | Incoming contributor applications | Review vehicle specs and documents |
| `/partner/contributions/:applicationId` | Review & propose agreement | Set fixed fee per booking, notice period, and handover terms |
| `/partner/agreements` | Active contributor agreements | Contractual agreement management |
| `/partner/bookings` | Booking management | Active, upcoming, overdue, and completed rentals |
| `/partner/bookings/:bookingId` | Booking details & fulfillment | Customer contact, pickup/return checklists, payment records |
| `/partner/inspections` | Inspection records | Pre-rental handover and post-rental return photo logs |
| `/partner/deposits` | Security deposit management | Track deposits held, propose damage deductions, record direct refunds with UTR |
| `/partner/contributor-payables` | Contributor settlement workflow | Schedule and record direct bank transfers (NEFT/IMPS/UPI) to contributors with UTR and receipt upload |
| `/partner/financials` | Financial record-keeping | Overview of direct rental proceeds received and contributor settlements recorded |

---

## 6. Admin

| Route | Page | Notes |
|---|---|---|
| `/admin` | Platform operational dashboard | Marketplace metrics, active rentals, platform health |
| `/admin/users` | User & role management | Manage user accounts, role revocations |
| `/admin/partners` | Partner verification queue | Review commercial documents, approve or reject partner onboarding |
| `/admin/vehicles` | Vehicle verification | Audit RC, commercial permits, insurance documents before listing activation |
| `/admin/contributions` | Contribution agreement oversight | Monitor bilateral partner-contributor agreements |
| `/admin/bookings` | Master booking oversight | Global booking audit and operational monitoring |
| `/admin/payment-submissions` | Payment submissions & audit | Audit trail of customer payment submissions and partner verifications |
| `/admin/deposits` | Deposit & refund monitoring | Oversee deposit liabilities held by partners and refund compliance |
| `/admin/disputes` | Dispute arbitration queue | Arbitrate contested damage deductions and deposit refund disputes |
| `/admin/reviews` | Review moderation | Moderate flagged customer reviews |
| `/admin/reports` | Operational analytics | Fleet utilization, turnaround times, regional metrics |
| `/admin/settings` | Platform policy configuration | Configure default hold TTL, turnaround buffers, verification windows |
| `/admin/audit` | Security audit trail | Query immutable `audit_logs` by user, entity, and correlation ID |
