# Business Requirements

## 1. Product scope

A vehicle rental marketplace launching in Vijayawada, supporting cars, bikes and scooters. The product combines conventional rental partner inventory with a distinctive vehicle contribution model.

## 2. Inventory channels

### 2.1 Partner-owned vehicles

- A verified rental partner can register vehicles it owns or is authorized to commercially operate.
- Registration is independent of contributor submissions.
- Partner manages vehicle details, pricing, availability, bookings and inspections.
- Admin verifies partner and vehicle eligibility before publication.

### 2.2 Contributor-owned vehicles

- A user submits a vehicle and required ownership/eligibility evidence.
- A rental partner reviews it and may accept or reject it.
- The contributor and partner execute a written/digitally acknowledged agreement before publication.
- The contributor remains legal owner. Operational management is assigned to the accepting partner.
- Partner and contributor agree how physical handover/pickup/return will work. The platform records custody events; it does not assume the partner permanently stores the vehicle.
- Agreement compensation is a fixed amount per eligible completed booking. Amount and payment deadline are agreement-specific.
- Contributor availability blocks/return requests must respect existing confirmed bookings and the contract.
- One vehicle has only one active management agreement at a time.

## 3. Roles and capabilities

| Role | Core capabilities |
|---|---|
| CUSTOMER | Explore, compare, book, pay, view bookings, cancel/request changes, raise support cases, review completed rentals |
| VEHICLE_CONTRIBUTOR | Submit vehicles, respond to partner proposals, view agreements, manage permitted availability/return requests, view booking-based earnings and settlements |
| RENTAL_PARTNER | Maintain business profile, register own vehicles, review contribution applications, manage inventory, pricing, bookings, inspections, handovers/returns and contributor payables |
| ADMIN | Verify partners/vehicles, manage users, bookings, refunds, commissions, disputes, policies, audit and reports |

Roles are additive. CUSTOMER and VEHICLE_CONTRIBUTOR may belong to one user account. A partner's own inventory does not depend on contributed vehicles. ADMIN assignment is restricted to trusted administrative processes; public registration must never allow self-selection of ADMIN.

## 4. Main workflows

### 4.1 Customer booking

1. Select category, pickup/drop-off location and rental start/end date/time.
2. Search only vehicles available for the full requested interval.
3. Inspect details, price, deposit, included distance/fuel policy, pickup method, rules and cancellation terms.
4. Sign in/register.
5. Complete required identity and driving-licence verification.
6. Confirm price breakdown and accept rental terms.
7. Create a time-limited booking hold to prevent race-condition double bookings.
8. Complete payment/deposit authorization or payment according to configured gateway flow.
9. Verify payment server-side and confirm booking.
10. Complete pickup handover with timestamp, odometer, fuel/charge and condition photos.
11. Track booking; request extension only if the vehicle remains available and partner approves.
12. Return vehicle and record inspection.
13. Settle extra time, damage or other documented charges; release/refund deposit as policy allows.
14. Mark booking completed and enable review.

### 4.2 Contributor submission and agreement

1. Contributor completes profile and identity checks.
2. Submit vehicle category, specifications, registration details, location, photos and required documents.
3. Admin performs initial platform eligibility review; partner can review eligible applications.
4. Partner accepts, rejects or proposes terms.
5. Contributor and partner agree on fixed amount per eligible completed booking, payment deadline, availability, maintenance, insurance, damage responsibilities, custody arrangements and term.
6. Both parties acknowledge the agreement; preserve an immutable version.
7. Record initial vehicle condition and handover arrangement.
8. Partner publishes vehicle and controls approved rental listing/pricing.
9. Each completed eligible booking creates a payable using the agreement snapshot.
10. Partner/platform records settlement with reference and date.
11. Agreement can be suspended/terminated under its terms; vehicle return process respects active bookings.

### 4.3 Partner-owned inventory

1. Verified partner registers its own vehicle.
2. Upload documents, images, registration data, pricing, pickup options and availability.
3. Admin verifies vehicle and required documentation.
4. Partner publishes/pauses listing and manages its availability.
5. Vehicle participates in the same booking and inspection lifecycle as contributor vehicles.

### 4.4 Cancellation and no-show

- Customer can request cancellation subject to the booking's persisted cancellation policy snapshot.
- Initial proposed default (subject to validation and admin configuration): more than 48 hours before pickup: 100% rental refund; 24–48 hours: 75%; less than 24 hours: 50%; no-show: 0% rental refund. Gateway charges and deposit handling must be stated transparently.
- Partner/owner/platform cancellation before pickup should normally provide a full customer refund and support alternative vehicle where feasible.
- A cancellation after pickup is handled as early return/termination, not ordinary pre-pickup cancellation.
- Refunds are separate payment transactions and must be reconciled.
- Contributor payouts are not created for cancelled bookings unless the agreement explicitly defines compensation.

### 4.5 Extension and late return

- Extension is a request, not automatic.
- Check availability against all later confirmed bookings and operational buffers.
- Partner must approve and collect any additional amount before the extension is accepted.
- Late fees follow disclosed rental terms and applicable law; avoid unlimited or hidden charges.
- Escalate overdue return through partner/support procedures.

### 4.6 Inspection, incident and dispute

- Pre-rental and post-rental inspections capture timestamp, mileage, fuel/charge, photos, condition checklist and both-party acknowledgement where possible.
- Damage claims require evidence, review and an opportunity for the customer/owner/partner to respond.
- Disputed amounts must not be silently deducted; record decision, evidence, amount and settlement.
- Breakdown, accident, theft, failure to return and safety incidents need urgent support/escalation workflows.
- Insurance coverage and liability rules must be validated before launch and reflected in agreements and customer terms.

## 5. Financial model

- Partner sets rental pricing within platform policy; admin can suspend or override listings under documented controls.
- Platform commission is configurable and transparently disclosed.
- Contributor fixed payment is contract-specific per eligible completed booking.
- Track customer payment, security deposit, refund, partner gross revenue, platform commission, taxes/fees where applicable, contributor payable and actual settlement separately.
- Use a ledger/transaction history with immutable references rather than deriving balances from a single editable number.
- Razorpay is the planned payment gateway. Start with test mode; production onboarding and settlement flows require verification.
- Security deposit policy is configurable by vehicle and customer verification. Support authorization/capture where gateway and business flow permit; otherwise model collection and refund explicitly.

## 6. Operating policies to configure

- Hourly and daily rentals.
- Pickup at partner location, owner handover or partner-arranged delivery, depending on listing.
- Seven-day suggested notice for contributor vehicle withdrawal, subject to agreement and active confirmed bookings.
- Customer cancellation windows and refund percentages.
- Late return, extension and no-show charges.
- Security deposit amount and release rules.
- Commission and taxes/fees.
- Partner service area and delivery charges.
- Minimum/maximum rental duration and buffer time.
- Document expiry and re-verification rules.

All values above are product defaults to validate, not legal advice or a statement of statutory requirements.
