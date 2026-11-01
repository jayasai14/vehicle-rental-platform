# Page and Route Inventory

Routes below are proposed. Final naming can be adjusted before implementation, but route ownership and access requirements must remain explicit.

## Public

| Route | Page | Access |
|---|---|---|
| / | Landing page | Public |
| /vehicles | Explore vehicles/search results | Public |
| /vehicles/:vehicleId | Vehicle details | Public |
| /how-it-works | How the platform works | Public |
| /earn-with-your-vehicle | Contributor information/onboarding | Public |
| /about | About | Public |
| /help | Help/FAQ/contact | Public |
| /terms | Terms | Public |
| /privacy | Privacy policy | Public |
| /login | Unified login | Public |
| /register/customer | Customer registration | Public |
| /register/partner | Partner application | Public |
| /forgot-password | Password recovery | Public |
| /verify-email | Email/OTP verification | Public |

ADMIN must never be selectable through public registration.

## Customer

| Route | Page |
|---|---|
| /customer | Customer dashboard |
| /customer/bookings | Booking history |
| /customer/bookings/:bookingId | Booking details |
| /customer/profile | Profile and contact details |
| /customer/verification | Identity/licence verification |
| /customer/favorites | Saved vehicles (optional) |
| /customer/reviews | My reviews |
| /customer/support | Support requests |

## Booking flow (authenticated customer)

| Route | Page |
|---|---|
| /checkout/:vehicleId | Rental dates, location and booking summary |
| /checkout/:vehicleId/verification | Identity/licence completion |
| /checkout/:vehicleId/review | Price, terms, deposit and cancellation review |
| /checkout/:vehicleId/payment | Payment |
| /booking-confirmation/:bookingId | Confirmation |
| /bookings/:bookingId/extension | Request extension |

Use server-side booking holds and quote validation. Never depend on frontend step order for security or price integrity.

## Vehicle contributor

| Route | Page |
|---|---|
| /contributor | Contributor dashboard |
| /contributor/vehicles/new | Submit vehicle |
| /contributor/vehicles | Submitted/active vehicles |
| /contributor/vehicles/:vehicleId | Application and vehicle status |
| /contributor/agreements | Agreements and terms |
| /contributor/earnings | Earnings/payables |
| /contributor/settlements | Payment history |
| /contributor/availability | Availability and withdrawal requests |

## Rental partner

| Route | Page |
|---|---|
| /partner | Partner dashboard |
| /partner/profile | Business profile and verification |
| /partner/vehicles | Partner-managed inventory |
| /partner/vehicles/new | Add partner-owned vehicle |
| /partner/vehicles/:vehicleId | Manage vehicle/listing |
| /partner/contributions | Contributor applications |
| /partner/contributions/:applicationId | Review application and propose terms |
| /partner/agreements | Active/expired agreements |
| /partner/bookings | Booking management |
| /partner/bookings/:bookingId | Booking and customer details |
| /partner/inspections | Handover/return inspections |
| /partner/earnings | Partner revenue/commission |
| /partner/contributor-payables | Contributor payable/settlement workflow |

## Admin

| Route | Page |
|---|---|
| /admin | Platform dashboard |
| /admin/users | User and role management |
| /admin/partners | Partner applications and verification |
| /admin/vehicles | Vehicle verification and listing moderation |
| /admin/contributions | Contribution and agreement oversight |
| /admin/bookings | Booking oversight |
| /admin/payments | Payments, deposits and refunds |
| /admin/payouts | Partner/contributor settlement oversight |
| /admin/commissions | Commission configuration |
| /admin/disputes | Complaints, damage claims and incidents |
| /admin/reviews | Review moderation |
| /admin/reports | Operational and financial reports |
| /admin/settings | Policies, cancellation, deposits and platform settings |
| /admin/audit | Sensitive action/audit log |

## Navigation principles

- Separate layouts and navigation for public, customer, partner, contributor and admin portals.
- A user with multiple roles can switch between available workspaces without creating another account.
- Hide inaccessible navigation items for usability, but enforce all access on backend endpoints.
- Add loading, empty, error, forbidden and not-found states for every data-driven page.
- Do not create separate pages where a coherent tab/detail view is clearer.
