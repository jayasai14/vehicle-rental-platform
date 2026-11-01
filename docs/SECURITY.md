# Security Baseline (Pre-Freeze Specification)

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh  
**Status:** Pre-Freeze Corrected Baseline  

Security is an architectural baseline enforced at the data, transport, and application layers, not a post-launch hardening activity.

---

## 1. Authentication & Session Security

- **Enforcement Point:** Spring Security 6+ filter chain with stateless JWT authentication.
- **Password Protection:** BCrypt hashing (`work_factor = 12`) applied to all user credentials; plaintext passwords never touch logs or persistent storage.
- **Token Strategy:** Short-lived JWT access tokens (15-minute expiration) paired with cryptographically hashed refresh tokens (SHA-256) stored in `refresh_tokens`.
- **Refresh Token Rotation & Revocation:** Every token refresh operation rotates the token family; reuse of a revoked token invalidates all descendant sessions immediately.
- **Endpoint Protection:** Rate limiting (Bucket4j) applied to `/auth/login`, `/auth/verify-email`, `/auth/forgot-password`, and `/payments/orders` to defend against brute-force and enumeration attacks.
- **Timing Defense:** Password verification and recovery responses return constant-time generic messages to prevent user enumeration.

---

## 2. Authorization, Role-Based Access & IDOR Defense

- **Additive Roles:** `CUSTOMER`, `VEHICLE_CONTRIBUTOR`, `RENTAL_PARTNER`, `ADMIN`.
- **Administrative Bootstrap:** The `ADMIN` role is strictly restricted to trusted internal administrative provisioning; public registration endpoints reject self-assignment of administrative roles.
- **Contributor Rights:** A verified contributor has explicit capabilities to:
  - Participate in pre-rental handover inspections for their own vehicles.
  - Acknowledge direct physical vehicle handovers.
  - Participate in post-rental return inspections when mandated by agreement.
  - View their own vehicle registration documents, insurance policies, and agreements.
  - View their own fixed earnings, payable statuses, settlement UTR references, and dates.
  - Request vehicle withdrawal with recommended 7-day notice (subject to active bookings).
- **Partner Boundaries:** A verified partner manages independently registered fleet, manages accepted contributor fleet, controls listing pricing subject to price floors, conducts inspections, and records direct contributor settlements under Approach A.
- **Customer Privacy:** A customer can access **only** their own bookings, payment orders, deposit statuses, and submitted KYC documents.
- **Method Security:** Spring Security `@PreAuthorize` method security applied across all service boundaries.
- **BOLA / IDOR Defense:** Every endpoint containing an entity identifier (e.g. `bookingId`, `vehicleId`, `agreementId`, `deductionId`) validates relational ownership:
  - `@bookingSecurity.canAccess(bookingId, principal)`
  - `@vehicleSecurity.canAccessDocuments(vehicleId, principal)`
  - `@inspectionSecurity.canAccess(inspectionId, principal)`
  - `@agreementSecurity.canAccess(agreementId, principal)`
  - `@settlementSecurity.canAccess(settlementId, principal)`

---

## 3. Data Protection, PII & Banking Security

- **Sensitive Data Minimization:** Collect only identity, licence, and vehicle data legally mandated for commercial rental operations.
- **Zero Raw Bank Storage:** The platform never stores raw bank account numbers, credit/debit card numbers, CVVs, or bank net banking credentials.
- **Tokenized Disbursement Credentials:** Partner and contributor disbursement accounts are tokenized via payment gateway fund accounts (`provider_fund_account_id` in `payout_methods`). Operational metadata (masked account last-4, encrypted IFSC) is encrypted at rest using AES-256 GCM.
- **Private Object Storage:** Identity verification photos, RC documents, commercial permits, and inspection media are stored in private object storage buckets and accessed exclusively via short-lived presigned URLs (5-minute maximum TTL).
- **KYC & Identity Data:** Raw Aadhaar numbers are never stored; authorized third-party verification references (e.g. Digilocker / OKYC transaction tokens) are preserved in `identity_verifications`.
- **Log Redaction:** Logback filters automatically mask emails, phone numbers, auth tokens, and financial figures from application logs.

---

## 4. Payment, Security Deposit & Webhook Security

- **Gateway Signature Verification:** All payment callbacks and webhook events require cryptographic HMAC SHA-256 signature verification against the webhook secret before processing.
- **Inward Idempotency Ledger:** Webhook events are deduplicated via unique database constraints on `(provider, provider_event_id)` in `payment_webhook_events`. Duplicate deliveries return `200 OK` immediately without duplicate processing.
- **Refundable Deposit Liability:** Security deposits are tracked as a **refundable customer liability**, never as platform revenue, taxable turnover, or escrow.
- **Damage Claim Dispute Protection:**
  - Deductions from security deposits require mandatory photographic inspection evidence and approved reason codes.
  - Undisputed deposit balances are refunded immediately.
  - **A disputed damage claim is NEVER automatically approved merely because a 48-hour response window expires.** Disputed claims require Admin arbitration; non-responsive claims require Admin verification before deduction.
- **Append-Only Double-Entry Journal:** All monetary flows are recorded in `ledger_entries` under balanced transaction groups ($\sum \text{Debits} = \sum \text{Credits}$). Mutable balances are strictly barred; adjustments execute via reversal entries.

---

## 5. Application Defenses, Outbox & Concurrency

- **Pessimistic Concurrency Defense:** All operations altering vehicle availability (hold creation, booking confirmation, extension, maintenance blocking) acquire a pessimistic row lock (`SELECT id FROM vehicles WHERE id = :id FOR UPDATE;`) to eliminate double-booking race conditions.
- **Hold Isolation & Webhook Safety:** Creating a booking hold reserves the calendar slot for 15 minutes and **does NOT insert an incomplete record into `bookings`**. If a delayed successful payment webhook arrives after hold expiration and the interval is reserved by another customer, the platform automatically triggers a 100% gateway refund.
- **Transactional Outbox Engine:** All external side-effects (payment gateway calls, media processing, SMS/email notices) write to `outbox_events` in the primary database transaction, preventing distributed zombie transactions.
- **Distributed Lock Security:** Multi-instance cron jobs synchronize via ShedLock backed by the MySQL `shedlock` table, preventing concurrent execution of scheduled tasks.
- **Security Audit Logging:** All administrative actions, role modifications, partner approvals, dispute arbitration decisions, manual refunds, and agreement changes are permanently logged in `audit_logs` with actor ID, IP address, correlation ID, and timestamp.
