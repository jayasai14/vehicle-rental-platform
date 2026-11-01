# System Architecture

## 1. Architectural style

Start with a **modular monolith**: one Spring Boot deployable, one MySQL database, and a separate React application. Organize backend code by business capability with clear module boundaries. Do not introduce microservices, service discovery or distributed infrastructure without a demonstrated need.

## 2. Repository layout

```text
vehicle-rental-platform/
├── frontend/
├── backend/
├── docs/
├── .gitignore
└── README.md
```

### Frontend layout (target)

```text
frontend/src/
├── app/                 # app providers and bootstrap
├── assets/
├── components/
│   ├── common/
│   ├── navigation/
│   ├── vehicles/
│   ├── booking/
│   └── forms/
├── features/
│   ├── auth/
│   ├── marketplace/
│   ├── customer/
│   ├── contributor/
│   ├── partner/
│   ├── admin/
│   ├── bookings/
│   ├── vehicles/
│   └── notifications/
├── layouts/
├── routes/
├── services/            # shared API client/interceptors only
├── hooks/
├── context/
├── utils/
└── main.jsx
```

Feature-specific components, hooks and API functions should live inside the feature, not in a giant global folder. Use React Router. Use route guards for UX and backend authorization for actual security.

### Backend layout (target)

Root package: `com.vehiclerental` (confirm before generation; keep it consistent).

```text
backend/src/main/java/com/vehiclerental/
├── VehicleRentalApplication.java
├── config/
├── security/
│   ├── jwt/
│   ├── filter/
│   └── principal/
├── common/
│   ├── api/
│   ├── exception/
│   ├── validation/
│   └── audit/
├── auth/
├── user/
├── partner/
├── vehicle/
├── contribution/
├── booking/
├── inspection/
├── payment/
├── payout/
├── review/
├── support/
└── notification/
```

Each business module owns its controller, service, repository, entity/model, dto and mapper packages as needed. Avoid empty placeholder files. Do not create a generic package that becomes a dumping ground.

## 3. Logical components

- React SPA: public browsing, role-aware portal layouts, forms and dashboard workflows.
- REST API: authentication, validation, authorization, orchestration and domain rules.
- MySQL: normalized transactional records and auditable financial data.
- Object storage: vehicle photos and private verification documents. Store object keys/metadata in MySQL, not raw image blobs.
- Payment provider: hosted/secure payment collection and verified webhooks.
- Email provider: verification and transactional notices.
- Observability: structured logs, health endpoint, correlation IDs and error reporting.

## 4. Core module boundaries

| Module | Owns |
|---|---|
| auth | Registration, login, OTP/email verification, password reset, refresh/revoke sessions |
| user | User profile, role grants and account status |
| partner | Partner profile, verification and service area |
| vehicle | Vehicle identity/specifications, owner, manager, listing, documents, pricing and availability |
| contribution | Contributor application, partner decision, agreement versions, availability/withdrawal requests |
| booking | Search availability, holds, booking lifecycle, cancellation, extension and no-show |
| inspection | Handover/return checklists, media evidence, incidents and condition reports |
| payment | Payment intents/orders, gateway verification, refunds, deposit transactions and webhooks |
| payout | Partner settlements, contributor payables, payout batches/status and ledger |
| review | Booking-bound ratings and moderation |
| support | Complaints, disputes, claims and escalations |
| notification | In-app/email templates, delivery status and preferences |

## 5. Key design boundaries

- Separate a vehicle's legal owner from its operational manager.
- Separate a vehicle's listing state from its physical availability and booking state.
- A partner-owned vehicle has owner=partner and manager=partner.
- A contributor-owned vehicle has owner=contributor and manager=accepting partner while the agreement is active.
- Use explicit booking, payment, agreement, inspection and payout state machines.
- The booking record snapshots price, fee, deposit and cancellation policy at confirmation.
- Financial movements are recorded as append-only transactions/ledger entries with reversal entries for corrections.
- Use database transactions and appropriate locking/constraints for booking confirmation and payment webhook idempotency.
- Never trust a client-supplied price, commission, payout amount, role or ownership claim.

## 6. Deployment topology (future)

- Frontend static hosting/CDN with HTTPS.
- Spring Boot API behind HTTPS reverse proxy/load balancer.
- Managed MySQL with private networking, backups and migrations.
- Private object storage with short-lived signed access URLs.
- Secrets supplied by managed environment/secret store.
- Separate development, test/staging and production configurations.
- Automated build/test pipeline before deployment.
