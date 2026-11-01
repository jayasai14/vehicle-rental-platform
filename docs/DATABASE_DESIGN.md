# Domain Model and Database Design Plan

This is the initial logical schema plan, not yet an approved physical schema or migration. Review constraints and cardinalities before implementation.

## 1. Modeling principles

- MySQL with InnoDB and foreign keys.
- Use BIGINT identifiers (or UUID consistently if a later security/distribution need justifies it); do not mix conventions casually.
- Use DECIMAL for money and explicit currency code (INR initially).
- Store timestamps in UTC; render in Asia/Kolkata for initial operations.
- Add created_at/updated_at to mutable records; deleted_at only where soft deletion is justified.
- Use Flyway migrations.
- Store private file object keys and metadata, not public permanent URLs.
- Enforce unique registration identifiers carefully; encrypt/restrict sensitive document values where appropriate.
- Define indexes from actual access patterns and explain any uniqueness constraints.

## 2. Identity and authorization

### users
- id, full_name, email, phone, password_hash, account_status, email_verified_at, phone_verified_at, created_at, updated_at.
- Unique normalized email and phone where required.

### roles
- id, code (CUSTOMER, VEHICLE_CONTRIBUTOR, RENTAL_PARTNER, ADMIN), name.

### user_roles
- user_id, role_id, granted_at, granted_by.
- Unique (user_id, role_id).
- ADMIN grants are controlled by trusted admin process, never self-registration.

### refresh_tokens / user_sessions
- id, user_id, token_hash or jti, expires_at, revoked_at, created_at, device metadata.
- Store refresh-token hashes, not plaintext refresh tokens.

### customer_profiles
- user_id, date_of_birth where required, address/contact preferences and other minimum required profile data.

### partner_profiles
- id, user_id, legal/business name, contact information, service area, verification_status, submitted_at, verified_at, verified_by, rejection_reason.
- Partner onboarding is an application and approval flow.

### contributor_profiles
- user_id, verification_status, payout preference/reference (sensitive access restricted).

## 3. Vehicle and inventory

### vehicles
- id, legal_owner_user_id, manager_partner_id, inventory_source (PARTNER_OWNED / CONTRIBUTOR_OWNED), category (CAR / BIKE / SCOOTER), make, model, variant, manufacture_year, registration_number (protected), fuel_type, transmission where applicable, seating_capacity, engine_cc where applicable, odometer_km, description, city_id, pickup_address_id, listing_status, verification_status, created_at, updated_at.
- legal_owner_user_id identifies ownership; manager_partner_id identifies the partner currently responsible for rental operations.
- For partner-owned inventory, the partner user is legal owner/authorized owner as defined by verification. For contributor inventory, contributor remains legal owner and partner is manager.
- Prevent multiple active agreements/managers for a contributor vehicle.

### vehicle_documents
- id, vehicle_id, document_type (RC, INSURANCE, PERMIT, POLLUTION_CERTIFICATE, OTHER), object_key, document_number_protected, issue_date, expiry_date, verification_status, verified_by, verified_at.
- Private access only; support document expiry alerts.

### vehicle_images
- id, vehicle_id, object_key, display_order, image_type.

### vehicle_pricing
- id, vehicle_id, hourly_rate, daily_rate, security_deposit, extra_hour_rate, late_fee_rule/config reference, effective_from, effective_to, created_by.
- Store money as DECIMAL and snapshot the accepted quote on booking.

### vehicle_availability_blocks
- id, vehicle_id, starts_at, ends_at, block_type, reason, created_by, status.
- Used for maintenance, owner unavailability and operational buffers; must not conflict with active bookings.

### cities / locations / addresses
- Normalize launch city and service locations. Protect exact home addresses; public listing should expose only approved pickup-area detail until booking confirmation.

## 4. Contribution agreement domain

### vehicle_contribution_applications
- id, vehicle_id, contributor_user_id, preferred_partner_id nullable, application_status, submitted_at, reviewed_at, rejection_reason.
- Separate application from vehicle to retain review history.

### contribution_agreements
- id, vehicle_id, contributor_user_id, partner_id, agreement_status, start_at, end_at, fixed_amount_per_booking, currency, payout_due_hours, eligible_booking_rule, availability_terms, maintenance_terms, damage_terms, custody_terms, termination_notice_days, accepted_by_contributor_at, accepted_by_partner_at, version, created_at.
- Amount and payment terms belong to the agreement, not hardcoded settings.
- Immutable accepted terms. Amendments create a new version or amendment record; retain history.

### contribution_agreement_events
- id, agreement_id, event_type, actor_user_id, occurred_at, details_json.
- Track proposal, acceptance, suspension, termination and return workflow.

### vehicle_withdrawal_requests
- id, vehicle_id, agreement_id, requested_by, requested_at, requested_return_at, status, decision_reason, resolved_at.
- Enforce protection of confirmed bookings and agreement notice requirements.

## 5. Booking and inspection

### bookings
- id, booking_reference, vehicle_id, customer_user_id, manager_partner_id, starts_at, ends_at, pickup_location_id, return_location_id, booking_status, currency, rental_subtotal, delivery_fee, platform_fee_snapshot, discount_total, tax_total, deposit_amount, total_due, cancellation_policy_snapshot, quote_expires_at, confirmed_at, cancelled_at, cancellation_reason, created_at.
- Snapshot price and policy values at confirmation.
- Add database-safe overlap prevention strategy (transactional lock/guard row and conflict check; MySQL lacks a simple general exclusion constraint).

### booking_status_events
- id, booking_id, from_status, to_status, actor_user_id, reason, occurred_at.

### booking_extensions
- id, booking_id, requested_end_at, additional_amount, status, decided_by, decided_at.

### inspections
- id, booking_id, inspection_type (PICKUP / RETURN / INCIDENT), performed_by, performed_at, odometer_km, fuel_or_charge_level, condition_summary, acknowledgement_status.

### inspection_media
- id, inspection_id, object_key, caption, captured_at.

### incidents / damage_claims
- id, booking_id, reported_by, incident_type, description, status, estimated_amount, approved_amount, resolution, reported_at, resolved_at.
- Store evidence and decision history separately; disputed amount must not be silently deducted.

## 6. Driver and identity verification

### identity_verifications
- id, user_id, verification_type, status, provider_reference, verified_at, expires_at, restricted_data_reference.
- Avoid storing unnecessary Aadhaar data. Prefer authorized verification providers and token/reference storage where possible.

### driver_licences
- id, user_id, licence_number_protected, issuing_region, vehicle_class, valid_from, valid_until, verification_status, provider_reference.
- Restrict access and retention; verify applicable categories and validity.

## 7. Financial records

### payment_orders
- id, booking_id, provider, provider_order_id, amount, currency, status, idempotency_key, created_at, paid_at.

### payment_transactions
- id, payment_order_id, provider_payment_id, transaction_type (CAPTURE, REFUND, DEPOSIT_AUTH, DEPOSIT_CAPTURE, DEPOSIT_RELEASE), amount, currency, status, provider_reference, created_at.
- Unique provider identifiers to prevent duplicate processing.

### payment_webhook_events
- id, provider_event_id, event_type, signature_verified, processing_status, payload_reference, received_at, processed_at.
- Unique provider_event_id; verify signatures and process idempotently.

### refunds
- id, booking_id, payment_transaction_id, amount, reason, status, provider_refund_id, requested_at, completed_at.

### partner_commissions
- id, booking_id, partner_id, gross_amount, commission_rate_snapshot, commission_amount, status.

### contributor_payables
- id, booking_id, agreement_id, contributor_user_id, partner_id, amount, currency, payable_status, eligible_at, due_at, settled_at.
- Unique eligible booking/agreement combination to prevent duplicate payable.

### settlements
- id, payee_user_id, payee_type, amount, currency, status, payment_reference, initiated_at, completed_at, failure_reason.

### ledger_entries
- id, transaction_group_id, account_type, account_reference, debit_amount, credit_amount, currency, reference_type, reference_id, created_at.
- Append-only accounting events and reversals; do not edit history to fix a financial mistake.

## 8. Supporting modules

### reviews
- id, booking_id, reviewer_user_id, vehicle_id, rating, comment, moderation_status, created_at.
- Only completed bookings can be reviewed; enforce one review per eligible reviewer/booking.

### support_tickets / ticket_messages
- Track customer, partner and contributor complaints, evidence references, assignment, status and resolution.

### notifications
- id, user_id, channel, template_code, payload, delivery_status, read_at, created_at.

### audit_logs
- id, actor_user_id, action, entity_type, entity_id, correlation_id, metadata_redacted, occurred_at.
- Never store passwords, raw tokens or unnecessary sensitive document data in logs.

### platform_settings / cancellation_policies
- Versioned configurable policies. Bookings snapshot the exact policy applied at confirmation.

## 9. Relationships to validate in ERD

- User 1—many user_roles; User 1—1 customer/contributor profile as applicable; User 1—0/1 partner profile.
- User 1—many vehicles as legal owner; Partner 1—many managed vehicles.
- Vehicle 1—many documents/images/pricing/availability blocks.
- Vehicle 1—many contribution applications over time; only one active agreement/manager at a time.
- Agreement 1—many agreement events and contributor payables.
- Vehicle 1—many bookings over time; booking belongs to one customer and one manager partner.
- Booking 1—many status events, inspections, payment transactions, refunds, incidents and support cases.
- Booking/agreement pair can create at most one contributor payable for the same eligible completion.
- Booking has at most one eligible review per reviewer under policy.

## 10. Before physical schema

Review:
1. Whether partner ownership should reference a partner entity rather than user directly.
2. Exact vehicle registration uniqueness and data protection approach.
3. Pricing units, minimum duration, buffers and tax treatment.
4. Booking overlap locking strategy under concurrent requests.
5. Payment capture/deposit authorization feasibility for selected payment provider.
6. Ledger account design and reconciliation.
7. Retention/deletion policy for KYC and licence data.

Do not generate Flyway migrations or JPA entities until these points and the ERD are approved.
