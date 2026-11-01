# Security Baseline (Freeze Baseline)

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh  
**Status:** Database Design Freeze Candidate Baseline (Partner-Direct Payment Model)  
**Schema Scope:** 47 Physical MySQL Tables (45 Application-Domain + 2 Infrastructure-Managed) + 8 Future-Phase Tables  

Security is an architectural baseline enforced at the data, transport, and application layers, not a post-launch hardening activity.

---

## 1. Authentication & Session Security

- **Enforcement Point:** Spring Security 6+ filter chain with stateless JWT authentication.
- **Password Protection:** BCrypt hashing (`work_factor = 12`) applied to all user credentials; plaintext passwords never touch logs or persistent storage.
- **Token Strategy:** Short-lived JWT access tokens (15-minute expiration) paired with cryptographically hashed refresh tokens (SHA-256) stored in `refresh_tokens`.
- **Refresh Token Rotation & Revocation:** Every token refresh operation rotates the token family; reuse of a revoked token invalidates all descendant sessions immediately.
- **Endpoint Protection:** Rate limiting (Bucket4j) applied to `/auth/login`, `/auth/verify-email`, `/auth/forgot-password`, and `/booking-holds/{holdId}/submit-payment` to defend against brute-force, spam, and enumeration attacks.
- **Timing Defense:** Password verification and recovery responses return constant-time generic messages to prevent user enumeration.

---

## 2. Authorization, Role-Based Access & IDOR Defense

- **Additive Roles:** `CUSTOMER`, `VEHICLE_CONTRIBUTOR`, `RENTAL_PARTNER`, `ADMIN`.
- **Administrative Bootstrap:** The `ADMIN` role is strictly restricted to trusted internal administrative provisioning; public registration endpoints reject self-assignment of administrative roles.
- **Partner Verification Gate:** Submitting a partner application does not grant partner operational privileges. Users whose `partner_profiles.verification_status` is not `VERIFIED` are blocked by Spring Security `@PreAuthorize("@partnerSecurity.isVerifiedPartner(principal)")` from fleet registration, configuring payment profiles, reviewing contributor proposals, or managing rentals.
- **Contributor Rights:** A verified contributor has explicit capabilities to:
  - Participate in pre-rental handover inspections for their own vehicles.
  - Acknowledge direct physical vehicle handovers.
  - Participate in post-rental return inspections when mandated by agreement.
  - View their own vehicle registration documents, insurance policies, and agreements.
  - View their own fixed earnings, payable statuses, settlement UTR references, and dates.
  - Acknowledge or dispute partner direct settlements with UTR evidence.
  - Request vehicle withdrawal with recommended 7-day notice (subject to active bookings).
- **Partner Boundaries:** A verified partner configures their direct payment profile, manages independently registered fleet, manages accepted contributor fleet, controls listing pricing subject to price floors, conducts inspections, verifies customer payment submissions, manages deposit custody, and records direct contributor settlements under Approach A.
- **Customer Privacy:** A customer can access **only** their own bookings, payment demands, payment submissions, deposit statuses, and submitted KYC documents.
- **Method Security:** Spring Security `@PreAuthorize` method security applied across all service boundaries.
- **BOLA / IDOR Defense:** Every endpoint containing an entity identifier (e.g. `bookingId`, `vehicleId`, `agreementId`, `submissionId`, `payableId`) validates relational ownership:
  - `@bookingSecurity.canAccess(bookingId, principal)`
  - `@paymentSecurity.canVerifySubmission(submissionId, principal)`
  - `@vehicleSecurity.canAccessDocuments(vehicleId, principal)`
  - `@inspectionSecurity.canAccess(inspectionId, principal)`
  - `@agreementSecurity.canAccess(agreementId, principal)`
  - `@payableSecurity.canAccessPayable(payableId, principal)`

---

## 3. Data Protection, PII & Payment Credentials Security

- **Sensitive Data Minimization:** Collect only identity, licence, and vehicle data legally mandated for commercial rental operations.
- **Zero Payment Secrets Storage:** The platform NEVER collects or stores UPI PINs, OTPs, netbanking passwords, credit/debit card credentials, or raw CVVs.
- **Partner Payment Profile Security:** Partner UPI IDs, payee display names, and external payment URLs are validated for format and stored in `partner_payment_profiles`.
- **Private Object Storage for Evidence & QR Codes:** Partner UPI QR codes, customer payment receipts, identity documents, RC files, and inspection media are stored in private Cloudinary/S3 object storage, accessed strictly via short-lived presigned URLs (5-minute maximum TTL).
- **KYC & Identity Data:** Raw Aadhaar numbers are never stored; authorized third-party verification references (e.g. Digilocker / OKYC transaction tokens) are preserved in `kyc_verifications`.
- **Log Redaction:** Logback filters automatically mask emails, phone numbers, auth tokens, and financial references from application logs.

---

## 4. Partner-Direct Payment, Verification & Anti-Fraud Security

- **Strict Verification Invariant:** A customer-uploaded screenshot, receipt, or UTR must **NEVER** automatically mark a payment as verified or confirm a booking. ONLY an explicit API call by an authenticated user belonging to the managing partner organization can mark the submission `VERIFIED`.
- **Duplicate Reference Prevention:** Database unique constraint `uk_submissions_partner_ref` on `(manager_partner_id, transaction_reference)` prevents malicious customers from recycling older bank UTRs or receipts across multiple bookings.
- **Verification Window & Inventory Protection:** Customer submission transitions the hold to `VERIFICATION_PENDING` and extends the lock TTL by a configurable window (e.g. 60 minutes). If the partner fails to act within the window, the hold expires to prevent indefinite inventory locking.
- **Refundable Deposit Custody:** Security deposits are tracked as a **refundable customer liability in partner custody**, never as platform revenue, taxable turnover, or escrow.
- **Damage Claim Dispute Protection:**
  - Deductions from security deposits require mandatory photographic inspection evidence and approved reason codes.
  - Undisputed deposit balances must be refunded immediately by the partner outside the platform.
  - **A disputed damage claim is NEVER automatically approved merely because a 48-hour response window expires.** Disputed claims require Admin arbitration; non-responsive claims require Admin verification before deduction approval.
- **Verification Audit Trail:** Every verification or rejection records the acting partner user ID, timestamp, previous status, new status, IP address, and supplied reason in `audit_logs`.

---

## 5. Application Defenses, Outbox & Concurrency

- **Pessimistic Concurrency Defense:** All operations altering vehicle availability (hold creation, booking confirmation, extension, maintenance blocking) acquire a pessimistic row lock (`SELECT id FROM vehicles WHERE id = :id FOR UPDATE;`) to eliminate double-booking race conditions.
- **Hold Isolation:** Creating a booking hold reserves the calendar slot for 15 minutes and **does NOT insert an incomplete record into `bookings`**.
- **Transactional Outbox Engine:** All external side-effects (media processing, SMS/email notices) write to `outbox_events` in the primary database transaction, preventing distributed zombie transactions.
- **Distributed Lock Security:** Multi-instance cron jobs synchronize via ShedLock backed by the MySQL `shedlock` table, preventing concurrent execution of scheduled tasks.
- **Security Audit Logging:** All administrative actions, role modifications, partner approvals, payment verifications/rejections, dispute arbitration decisions, and agreement changes are permanently logged in `audit_logs` with actor ID, IP address, correlation ID, and timestamp.
