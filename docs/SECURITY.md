# Security Baseline

Security is part of the initial design, not a final hardening task.

## Authentication

- Spring Security is the backend enforcement point.
- Hash passwords using BCrypt (or a carefully configured Spring PasswordEncoder).
- Use short-lived JWT access tokens and a refresh-token rotation/revocation mechanism.
- Store refresh tokens hashed or use server-side session/token identifiers; never store raw refresh tokens in the database.
- Validate issuer, audience, signature, expiry and allowed algorithms.
- Keep signing secrets outside source control and use strong key management in deployment.
- Define logout, password reset, account disablement and token-revocation behavior.
- Rate-limit login, OTP, password reset and verification endpoints.
- Do not reveal whether an email/phone exists through sensitive recovery responses.

## Token storage and browser

- Prefer secure, HttpOnly, SameSite cookies for refresh tokens where deployment architecture supports them; protect cookie-authenticated state-changing requests against CSRF.
- Keep access tokens short-lived and avoid localStorage for long-lived credentials.
- Use HTTPS everywhere outside local development.
- Apply strict CORS allowlists per environment. CORS is not authorization.

## Authorization

- Use additive roles/capabilities: CUSTOMER, VEHICLE_CONTRIBUTOR, RENTAL_PARTNER, ADMIN.
- Never trust roles supplied in registration payloads.
- Apply method/service-level checks and endpoint-level rules.
- Verify resource relationships on every access: booking customer, vehicle legal owner, assigned manager partner, agreement parties and admin permission.
- A route guard in React is UX only; backend must reject unauthorized access.
- Use least privilege for admin functions and audit sensitive actions.

## Data protection

- Collect only identity/licence/vehicle data needed for lawful operations.
- Restrict access to KYC, licence, RC, insurance and payout data.
- Store private files in private object storage; issue short-lived signed URLs after authorization.
- Encrypt sensitive fields or use provider references where appropriate.
- Do not store Aadhaar numbers/documents unless a lawful, necessary and supported process is established. Prefer authorized verification provider references.
- Redact PII from logs, analytics and error messages.
- Define retention, access, deletion and incident response policies.

## Payments

- Verify gateway signatures server-side.
- Verify payment status and amount against server-created order before confirming booking.
- Verify webhook signatures and process each provider event idempotently.
- Never accept browser redirect or client callback as proof of payment.
- Keep refund, deposit, commission and payout records auditable.
- Do not store card PAN, CVV or bank credentials.

## Application defenses

- DTO validation and safe error handling.
- SQL injection protection through parameterized repository access.
- Output encoding and safe rendering of user content.
- Upload validation: MIME/type, size, file signature, randomized object keys and malware scanning where feasible.
- Rate limits and abuse monitoring.
- Secure headers and appropriate Content Security Policy.
- Prevent booking race conditions and duplicate webhook/financial processing.
- Audit log for partner approval, role grants, vehicle verification, refunds, dispute decisions, agreement changes and payout settlement.
- Dependency updates and vulnerability checks.

## Launch gate

Before public launch, review applicable Indian privacy, payment, consumer, motor vehicle/rental, insurance, tax and local operating requirements with qualified professionals. Technical implementation alone does not establish legal compliance.
