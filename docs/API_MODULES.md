# API Module Plan (Freeze Baseline)

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh (Expandable)  
**Status:** Database Design Freeze Candidate Baseline (54-Table Architecture)  

> [!NOTE]
> This document defines the **target REST API contract specification** under the `/api/v1` namespace. No application controllers, routes, or endpoints have been implemented yet.

---

## 1. Architectural & Protocol Conventions

- **Data Serialization:** Strictly JSON request/response DTOs; persistence entities are never serialized directly.
- **Pagination & Sorting:** Standard query parameters (`page`, `size`, `sort`) with uniform envelope metadata (`totalElements`, `totalPages`, `isLast`).
- **Monetary Values:** Represented as standard formatted string or decimal numbers with explicit currency code (e.g. `{"amount": "5000.00", "currency": "INR"}`); floating-point math is barred.
- **Timestamps:** ISO-8601 with timezone offset (`2026-11-01T14:30:00+05:30`); persisted internally in UTC.
- **Error Format:** Consistent error response schema: `timestamp`, `status`, `code`, `message`, `path`, `correlationId`, `fieldErrors`.
- **Idempotency:** Unique `Idempotency-Key` header mandated on payment creation, hold conversion, refund dispatch, and settlement recording.
- **Zero-Trust Pricing:** Never accept prices, commission rates, fixed fees, or deposit amounts from clients; all financial figures are derived authoritatively on the backend.
- **Partner Verification Gate:** All endpoints under `/partner/**` enforce that the authenticated partner's profile is in `VERIFIED` status (`verification_status = 'VERIFIED'`). Pending applicants are rejected with `403 Forbidden`.

---

## 2. Authentication, User Profile & Addresses

- `POST /api/v1/auth/register/customer` - Public customer registration (cannot assign `ADMIN` or `RENTAL_PARTNER`).
- `POST /api/v1/auth/register/partner-application` - Rental partner onboarding application submission (creates profile in `SUBMITTED` status).
- `POST /api/v1/auth/login` - Authenticate via email/phone and password; returns JWT access token and HTTP-only refresh token.
- `POST /api/v1/auth/refresh` - Rotate refresh token and issue new access token.
- `POST /api/v1/auth/logout` - Revoke active refresh token.
- `POST /api/v1/auth/verify-email` - Complete email verification via OTP.
- `POST /api/v1/auth/forgot-password` - Request password reset OTP/link.
- `POST /api/v1/auth/reset-password` - Set new password with valid token.
- `GET /api/v1/users/me` - Retrieve authenticated user profile, roles, and partner verification status.
- `PATCH /api/v1/users/me` - Update profile details.
- `GET /api/v1/users/me/addresses` - List registered user addresses.
- `POST /api/v1/users/me/addresses` - Register new address (customer home, pickup point).
- `PATCH /api/v1/users/me/addresses/{addressId}` - Update address.

---

## 3. Partner Hubs, Locations & Delivery Zones (Verified Partners Only)

- `GET /api/v1/partners/me` - Retrieve partner business profile and verification status.
- `PATCH /api/v1/partners/me` - Update business details.
- `POST /api/v1/partners/me/verification-documents` - Upload commercial permits, GSTIN, trade licence.
- `GET /api/v1/partners/me/verification-status` - Check partner verification status (`SUBMITTED`, `IN_REVIEW`, `VERIFIED`, `REJECTED`).
- `GET /api/v1/partner/locations` - List partner depots/hubs (`partner_locations`).
- `POST /api/v1/partner/locations` - Register new verified partner hub with operating hours.
- `PATCH /api/v1/partner/locations/{locationId}` - Update hub operating details.
- `GET /api/v1/partner/delivery-zones` - List configured delivery service radiuses.
- `POST /api/v1/partner/delivery-zones` - Configure doorstep delivery radius and tiered fees.
- `PATCH /api/v1/partner/delivery-zones/{zoneId}` - Update delivery zone pricing.

---

## 4. Vehicle Inventory & Marketplace

- `GET /api/v1/vehicles` - Public search endpoint with category, dates, hub/delivery filters, and turnaround buffer validation.
- `GET /api/v1/vehicles/{vehicleId}` - Public vehicle listing details (masked registration, verified badges).
- `GET /api/v1/vehicles/{vehicleId}/availability` - Query available calendar intervals.
- `POST /api/v1/partner/vehicles` - Register fleet vehicle (supports individual owner or partner business legal owner; begins in `PARTNER_CUSTODY`).
- `PATCH /api/v1/partner/vehicles/{vehicleId}` - Update vehicle specifications.
- `POST /api/v1/partner/vehicles/{vehicleId}/documents` - Upload RC, insurance, PUC documents.
- `POST /api/v1/partner/vehicles/{vehicleId}/pricing` - Set rental rates, turnaround buffer, and security deposit.
- `POST /api/v1/partner/vehicles/{vehicleId}/publish` - Publish verified listing.
- `POST /api/v1/partner/vehicles/{vehicleId}/pause` - Temporarily pause listing.
- `GET /api/v1/partner/vehicles/{vehicleId}/minimum-price-quote` - Query backend contributor price floor derivation.
- `POST /api/v1/vehicles/{vehicleId}/custody-events` - Record physical vehicle custody handover across all 8 permutations (typed custodians, actor, and location).
- `GET /api/v1/vehicles/{vehicleId}/custody-events` - Auditable custody transfer history.
- `GET /api/v1/partner/vehicles` - List partner-managed fleet (both partner-owned and accepted contributor-owned).
- `GET /api/v1/contributor/vehicles` - List contributor's owned vehicles with status and current custodian.

---

## 5. Contributor Applications, Agreements & Withdrawals

- `POST /api/v1/contributor/vehicle-applications` - Contributor submits vehicle for partner onboarding (begins in `CONTRIBUTOR_CUSTODY`).
- `GET /api/v1/contributor/vehicle-applications` - List contributor's submitted applications.
- `GET /api/v1/contributor/vehicle-applications/{applicationId}` - View application details.
- `GET /api/v1/partner/contributions` - Verified partner lists incoming contributor applications.
- `POST /api/v1/partner/contributions/{applicationId}/proposal` - Partner proposes bilateral agreement terms (fixed fee per booking, notice period, handover mode).
- `POST /api/v1/contribution-agreements/{agreementId}/accept` - Contributor digitally signs and accepts agreement terms.
- `POST /api/v1/contribution-agreements/{agreementId}/reject` - Contributor declines proposal.
- `GET /api/v1/contributor/agreements` - Contributor views active/past agreements and terms documents.
- `GET /api/v1/partner/agreements` - Partner views active/past contributor agreements.
- `POST /api/v1/contributor/vehicles/{vehicleId}/availability-blocks` - Owner schedules permitted maintenance/personal blackout.
- `POST /api/v1/contributor/vehicles/{vehicleId}/withdrawal-requests` - Contributor initiates vehicle reclamation (recommended 7-day notice; active bookings retain precedence).
- `POST /api/v1/partner/withdrawal-requests/{requestId}/decision` - Partner acknowledges withdrawal and schedules de-fleeting handover.

---

## 6. Booking Holds & Master Booking Lifecycle

- `POST /api/v1/booking-quotes` - Calculate derived rental charges, delivery fee, turnaround buffer, and refundable security deposit.
- `POST /api/v1/booking-holds` - Acquire 15-minute temporary reservation lock under `SELECT ... FOR UPDATE`. **Does NOT create an incomplete booking record.**
- `DELETE /api/v1/booking-holds/{holdId}` - Manually release hold early, freeing vehicle calendar.
- `POST /api/v1/bookings` - Convert active hold to `CONFIRMED` master booking upon verified payment capture.
- `GET /api/v1/bookings/{bookingId}` - Retrieve booking details (scoped by role: customer, partner, or owner).
- `GET /api/v1/customer/bookings` - List customer's booking history.
- `GET /api/v1/partner/bookings` - List partner's managed bookings with filter by status.
- `POST /api/v1/bookings/{bookingId}/cancel` - Customer or partner cancels pre-pickup under cancellation policy.
- `POST /api/v1/bookings/{bookingId}/extension-requests` - Customer requests rental extension.
- `POST /api/v1/partner/bookings/{bookingId}/extension-decision` - Partner approves or rejects extension.
- `POST /api/v1/partner/bookings/{bookingId}/mark-no-show` - Partner marks customer no-show after grace period (deposit 100% refunded; rental retained).

---

## 7. Inspections, Incidents & Custody Handovers

- `POST /api/v1/partner/bookings/{bookingId}/pickup-inspection` - Partner completes pre-rental checklist, photos, and odometer.
- `POST /api/v1/customer/bookings/{bookingId}/acknowledge-pickup` - Customer signs pre-rental inspection, triggering custody shift to customer (`CUSTOMER_CUSTODY`).
- `POST /api/v1/contributor/bookings/{bookingId}/handover-inspection` - Contributor participates in owner-managed direct handover inspection.
- `POST /api/v1/contributor/bookings/{bookingId}/acknowledge-handover` - Contributor acknowledges physical handover directly to customer.
- `POST /api/v1/partner/bookings/{bookingId}/return-inspection` - Partner completes post-rental return checklist and condition photos.
- `POST /api/v1/customer/bookings/{bookingId}/acknowledge-return` - Customer signs return inspection, transitioning booking to `COMPLETED` immediately.
- `POST /api/v1/contributor/bookings/{bookingId}/return-inspection` - Contributor participates in owner-managed return inspection when agreement specifies it.
- `POST /api/v1/bookings/{bookingId}/incidents` - Report damage, mechanical breakdown, or traffic violation with evidence.
- `GET /api/v1/bookings/{bookingId}/inspections` - View inspection reports and presigned media URLs (role-restricted).
- `POST /api/v1/admin/incidents/{incidentId}/decision` - Admin arbitrates incident liability.

---

## 8. Payments, Security Deposits, Refunds & Contributor Settlements

- `POST /api/v1/payments/orders` - Generate Razorpay payment order for rental charges + security deposit with idempotency key.
- `POST /api/v1/payments/{paymentId}/verify` - Client submits gateway signature for verification.
- `POST /api/v1/payments/webhooks/razorpay` - Idempotent webhook receiver (HMAC SHA-256 verified; executes late webhook protocol: booking recovery if available, or automatic full refund if reserved).
- `GET /api/v1/customer/payments` - Customer views payment transaction receipts.
- `GET /api/v1/bookings/{bookingId}/deposit` - View deposit status, held balance, and deductions.
- `POST /api/v1/partner/bookings/{bookingId}/deposit/deductions` - Partner proposes deduction with mandatory photos and quotes.
- `POST /api/v1/bookings/{bookingId}/deposit/release-undisputed` - Immediately refunds uncontested deposit balance to customer.
- `POST /api/v1/customer/deposit-deductions/{deductionId}/dispute` - Customer disputes proposed deduction within 48-hour window.
- `POST /api/v1/admin/deposit-deductions/{deductionId}/resolve` - Admin arbitrates deposit dispute (**never auto-approved upon 48h expiration**).
- `POST /api/v1/admin/bookings/{bookingId}/deposit/release` - Trigger release of residual deposit liability.
- `POST /api/v1/admin/bookings/{bookingId}/refunds` - Trigger outward customer refund via gateway refund API.
- `POST /api/v1/admin/refunds/{refundId}/retry` - Finance/Admin re-dispatches failed refund to gateway after error resolution.
- `POST /api/v1/admin/refunds/{refundId}/settle-offline` - Finance logs manual direct bank transfer (NEFT/IMPS) with UTR for permanently declined refunds.
- `GET /api/v1/admin/partner-receivables` - Query outstanding partner receivables resulting from post-payout reversals or clawbacks.
- `POST /api/v1/admin/partner-receivables/{receivableId}/record-repayment` - Record direct partner bank repayment extinguishing receivable.
- `GET /api/v1/payout-methods/me` - List user's registered disbursement accounts.
- `POST /api/v1/payout-methods/me` - Register tokenized bank account / UPI ID (RazorpayX fund account).
- `DELETE /api/v1/payout-methods/me/{methodId}` - Remove disbursement account.
- `GET /api/v1/partner/financials` - Partner views gross rental proceeds, platform commission cut, and net earnings.
- `GET /api/v1/partner/contributor-payables` - Partner lists contractual fixed fee obligations owed to contributors (`contributor_payables`).
- `POST /api/v1/partner/contributor-payables/{payableId}/record-settlement` - **Approach A Baseline:** Partner records single complete direct bank transfer with mandatory UTR reference, payment date, and receipt attachment. (Does NOT affect platform general ledger cash).
- `POST /api/v1/contributor/contributor-payables/{payableId}/acknowledge` - Contributor confirms receipt of direct partner settlement.
- `POST /api/v1/contributor/contributor-payables/{payableId}/dispute` - Contributor disputes settlement amount or invalid UTR reference.
- `GET /api/v1/contributor/earnings` - Contributor dashboard tracking fixed compensation per eligible completed booking.
- `GET /api/v1/contributor/settlements` - Contributor views history of recorded direct settlements and UTRs.
- `POST /api/v1/admin/settlements/route-batch` - **Approach B (PENDING Validation):** Platform-facilitated Route split execution.

---

## 9. General Ledger & Administrative Audit Endpoints

- `GET /api/v1/admin/ledger/accounts` - List Master Chart of Accounts (`ledger_accounts`).
- `GET /api/v1/admin/ledger/transactions` - Query journal transaction headers (`ledger_transactions`) with date range and reference filters.
- `GET /api/v1/admin/ledger/transactions/{transactionGroupId}/entries` - View balanced journal line items (`ledger_entries`).
- `GET /api/v1/admin/audit-logs` - Query security, authentication, and financial audit logs with correlation IDs.
- `POST /api/v1/admin/partners/{partnerId}/verify` - Approve partner onboarding application (unlocks partner operational capabilities).
- `POST /api/v1/admin/partners/{partnerId}/reject` - Reject partner application with reason.
- `POST /api/v1/admin/vehicles/{vehicleId}/verify` - Approve or reject vehicle listing compliance.
- `GET /api/v1/admin/disputes` - Queue of open deposit and settlement disputes requiring arbitration.
- `POST /api/v1/admin/commission-rules` - Configure platform commission percentages, fixed fees, and minimum thresholds.

---

## 10. Reviews, Support Tickets & Notifications

- `POST /api/v1/bookings/{bookingId}/reviews` - Customer submits rating and review for completed rental.
- `GET /api/v1/vehicles/{vehicleId}/reviews` - Public vehicle reviews.
- `POST /api/v1/support/tickets` - Create support or dispute ticket.
- `GET /api/v1/support/tickets` - List user's open and resolved tickets.
- `POST /api/v1/support/tickets/{ticketId}/messages` - Add message or evidence attachment to support ticket.
- `GET /api/v1/notifications` - List in-app notifications.
- `PATCH /api/v1/notifications/{notificationId}/read` - Mark notification as read.
