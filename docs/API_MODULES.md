# API Module Plan

This is an endpoint inventory for contract design, not a promise that routes already exist. Use version prefix `/api/v1`. Final request/response schemas, status codes and authorization must be documented before implementation.

## Conventions

- JSON request/response DTOs; never serialize persistence entities.
- Pagination: `page`, `size`, `sort`; stable response metadata.
- Money: decimal strings or safe JSON number conventions documented consistently; never binary floating-point calculations.
- Timestamps: ISO-8601 with timezone; persist UTC.
- Standard error object: timestamp, status, code, message, path, correlationId, fieldErrors.
- Validate all input server-side.
- Use idempotency keys for payment/booking operations that may be retried.
- Never accept role, owner, manager, commission, final price or payout entitlement from an untrusted client without server derivation/authorization.

## Auth and user

- POST /auth/register/customer
- POST /auth/register/partner-application
- POST /auth/login
- POST /auth/refresh
- POST /auth/logout
- POST /auth/verify-email
- POST /auth/forgot-password
- POST /auth/reset-password
- GET /users/me
- PATCH /users/me
- GET /users/me/roles

Role grants and admin bootstrap must be controlled; public signup cannot create ADMIN.

## Partner

- GET /partners/me
- PATCH /partners/me
- POST /partners/me/verification-documents
- GET /partners/me/verification-status
- Admin endpoints to review/approve/reject partner applications.

## Vehicles and marketplace

- GET /vehicles (public search with category, dates, location, price and filters)
- GET /vehicles/{vehicleId}
- GET /vehicles/{vehicleId}/availability
- POST /partner/vehicles (partner-owned vehicle registration)
- PATCH /partner/vehicles/{vehicleId}
- POST /partner/vehicles/{vehicleId}/documents
- POST /partner/vehicles/{vehicleId}/publish
- POST /partner/vehicles/{vehicleId}/pause
- GET /partner/vehicles
- GET /contributor/vehicles

Public details must not reveal private registration, identity or exact owner addresses.

## Contributions and agreements

- POST /contributor/vehicle-applications
- GET /contributor/vehicle-applications
- GET /contributor/vehicle-applications/{applicationId}
- GET /partner/contributions
- POST /partner/contributions/{applicationId}/decision
- POST /partner/contributions/{applicationId}/proposal
- POST /contribution-agreements/{agreementId}/accept
- GET /contributor/agreements
- GET /partner/agreements
- POST /contributor/vehicles/{vehicleId}/availability-blocks
- POST /contributor/vehicles/{vehicleId}/withdrawal-requests
- POST /partner/withdrawal-requests/{requestId}/decision

Agreement versions must preserve accepted terms. Ensure only the named contributor and partner can view/respond.

## Booking

- POST /booking-quotes
- POST /booking-holds
- POST /bookings
- GET /bookings/{bookingId}
- GET /customer/bookings
- GET /partner/bookings
- POST /bookings/{bookingId}/cancel
- POST /bookings/{bookingId}/extension-requests
- POST /partner/bookings/{bookingId}/extension-decision
- POST /partner/bookings/{bookingId}/mark-no-show

Quote, hold and booking confirmation must re-check availability and derive all pricing on backend. Enforce ownership/manager/customer authorization for every resource.

## Inspections and incidents

- POST /partner/bookings/{bookingId}/pickup-inspection
- POST /partner/bookings/{bookingId}/return-inspection
- POST /bookings/{bookingId}/incidents
- GET /bookings/{bookingId}/inspections (role-restricted)
- POST /incidents/{incidentId}/evidence
- POST /admin/incidents/{incidentId}/decision

## Payments and payouts

- POST /payments/orders
- POST /payments/{paymentId}/verify
- POST /payments/webhooks/razorpay
- GET /customer/payments
- POST /admin/bookings/{bookingId}/refunds
- GET /partner/financials
- GET /partner/contributor-payables
- POST /partner/contributor-payables/{payableId}/settlements
- GET /contributor/earnings
- GET /contributor/settlements

Webhook routes are authenticated by provider signature verification, not user JWT. Process events idempotently. Server owns commission and contributor payable calculations.

## Reviews, support and notifications

- POST /bookings/{bookingId}/reviews
- GET /vehicles/{vehicleId}/reviews
- POST /support/tickets
- GET /support/tickets
- POST /support/tickets/{ticketId}/messages
- GET /notifications
- PATCH /notifications/{notificationId}/read

## Admin

Admin endpoints cover user/role moderation, partner and vehicle verification, bookings, financial reconciliation, policy configuration, support/disputes and audit. Prefer explicit admin resource paths and record sensitive actions in audit logs.

## API contract approval checklist

- Request/response DTO and field validation.
- Authentication requirement and permitted roles.
- Resource ownership/manager checks.
- State transition permitted from each current status.
- Idempotency and concurrency behavior.
- Side effects, notifications and audit requirements.
- Error codes and HTTP status.
- Pagination and filtering.
- Test coverage.
