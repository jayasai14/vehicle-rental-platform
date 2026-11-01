# Domain Model and Physical Database Design Plan (Freeze Baseline)

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh  
**Status:** Database Design Freeze Candidate Baseline (Partner-Direct Payment Model)  
**Confirmed Entity Count:** **47 Proposed Physical Tables** (45 Application-Domain Tables + 2 Infrastructure-Managed Tables) + **8 Future-Phase Tables**

> [!IMPORTANT]
> **Pre-Implementation Notice:** This document defines the **proposed physical database schema** to be created via Flyway migrations in MySQL 8.0+. **No migrations or actual database schema have been generated yet.** Application code, JPA entities, and Flyway migration SQL scripts will only be authored after formal Product Owner database design freeze approval.

---

## 1. Modeling Principles & Invariants

- **RDBMS Engine:** MySQL 8.0+ with InnoDB storage engine, strict foreign key constraints, and utf8mb4 encoding (`utf8mb4_0900_ai_ci`).
- **Identifier Strategy:** `BIGINT UNSIGNED AUTO_INCREMENT` for internal relational primary keys. Secure external identifiers (UUIDv4 or nanoids like `booking_reference`, `demand_reference`) exposed on public APIs to prevent enumeration attacks.
- **Monetary Precision:** `DECIMAL(10,2)` for localized financial tracking. Explicit currency code (default `'INR'`). Floating-point types are strictly forbidden.
- **Timezone Standardization:** All timestamps stored in UTC (`DATETIME` or `TIMESTAMP`). Rendered in `Asia/Kolkata` (IST, UTC+5:30) for presentation.
- **Audit & Deletion Policy:** `created_at` on all records; `updated_at` on mutable entities. Soft deletion (`deleted_at`) used only where legally justified; core operational, contract, and audit entities use `ON DELETE RESTRICT` to preserve immutable audit trails.
- **Separation of Legal Ownership, Operational Management, and Custody:**
  - Individual Owner: `legal_owner_user_id` (FK $\rightarrow$ `users.id`).
  - Partner Business Owner: `legal_owner_partner_id` (FK $\rightarrow$ `partner_profiles.id`).
  - Operational Manager: `manager_partner_id` (FK $\rightarrow$ `partner_profiles.id`).
  - Physical custody tracked via `vehicle_custody_events` with explicit, typed custodian foreign keys.
- **Zero Platform Cash Handling in MVP:** The platform collects, holds, escrows, splits, and transfers zero customer funds. Rental partners collect rental charges and security deposits directly; platform records payment demands, customer submissions (UTR/proof), manual partner verification, deposit custody, and contributor payables.

---

## 2. Module 1: Identity, Access, KYC & Addresses (11 Tables)

> Detailed column specifications, constraints, and indexes for all physical tables are formalized in [FINAL_ENTITY_INVENTORY.md](FINAL_ENTITY_INVENTORY.md) and [ENTITY_RELATIONSHIPS.md](ENTITY_RELATIONSHIPS.md).

### 1. `users`
- `id`, `full_name`, `email`, `phone`, `password_hash`, `account_status` (`PENDING_VERIFICATION`, `ACTIVE`, `SUSPENDED`, `DEACTIVATED`), `email_verified_at`, `phone_verified_at`, `created_at`, `updated_at`. Unique on `email` and `phone`.

### 2. `roles`
- `id`, `code` (`CUSTOMER`, `VEHICLE_CONTRIBUTOR`, `RENTAL_PARTNER`, `ADMIN`), `name`, `description`. Master role definitions.

### 3. `user_roles`
- `id`, `user_id`, `role_id`, `granted_by`, `granted_at`. Additive role assignments. `ADMIN` granted strictly via internal administrative provisioning.

### 4. `refresh_tokens`
- `id`, `user_id`, `token_hash` (SHA-256), `device_info`, `ip_address`, `expires_at`, `revoked_at`, `created_at`. Supports token rotation and revocation.

### 5. `addresses`
- `id`, `user_id` (nullable), `city_id`, `address_line1`, `address_line2`, `landmark`, `postal_code`, `latitude`, `longitude`, `address_type` (`CUSTOMER_HOME`, `PARTNER_HUB`, `PICKUP_POINT`, `DELIVERY_DESTINATION`), `created_at`, `updated_at`. Unified address repository.

### 6. `customer_profiles`
- `id`, `user_id`, `date_of_birth`, `address_id`, `emergency_contact_name`, `emergency_contact_phone`, `created_at`, `updated_at`.

### 7. `partner_profiles`
- `id`, `user_id`, `business_legal_name`, `trade_name`, `gstin_encrypted`, `pan_encrypted`, `contact_email`, `contact_phone`, `verification_status` (`SUBMITTED`, `IN_REVIEW`, `VERIFIED`, `REJECTED`, `SUSPENDED`), `submitted_at`, `verified_at`, `rejection_reason`, `created_at`, `updated_at`.
- Gate: Operational privileges locked until `verification_status = 'VERIFIED'`.

### 8. `partner_documents`
- `id`, `partner_id`, `document_type` (`TRADE_LICENCE`, `GSTIN_CERTIFICATE`, `COMMERCIAL_PERMIT`, `MSME_CERTIFICATE`, `OTHER`), `object_key`, `document_number_encrypted`, `verification_status`, `verified_by_user_id`, `verified_at`, `created_at`.

### 9. `contributor_profiles`
- `id`, `user_id`, `pan_encrypted`, `verification_status` (`PENDING`, `VERIFIED`, `REJECTED`), `verified_by_user_id`, `verified_at`, `rejection_reason`, `created_at`, `updated_at`.

### 10. `driving_licences`
- `id`, `user_id`, `licence_number_encrypted`, `issuing_state`, `vehicle_classes`, `valid_from`, `valid_until`, `verification_status`, `provider_reference`, `verified_at`, `created_at`.

### 11. `kyc_verifications`
- `id`, `user_id`, `verification_type` (`AADHAAR_OKYC`, `PAN`, `PASSPORT`), `provider_name`, `provider_reference`, `verification_status`, `verified_at`, `expires_at`, `created_at`.

---

## 3. Module 2: Operating Locations, Partner Hubs & Logistics (3 Tables)

### 12. `cities`
- `id`, `name`, `state`, `country`, `is_active`, `created_at`. Initial launch: Vijayawada, Andhra Pradesh.

### 13. `partner_hubs`
- `id`, `partner_id`, `address_id`, `city_id`, `hub_name`, `contact_person`, `contact_phone`, `operating_hours_json`, `is_active`, `created_at`, `updated_at`. Verified depots/service locations.

### 14. `pickup_locations`
- `id`, `vehicle_id`, `partner_hub_id` (nullable), `address_id` (nullable), `location_name`, `location_type` (`PARTNER_HUB`, `CONTRIBUTOR_LOCATION`, `DOORSTEP_DELIVERY`), `delivery_fee`, `is_default`, `created_at`.

---

## 4. Module 3: Vehicle Inventory, Compliance & Pricing (6 Tables)

### 15. `vehicles`
- `id`, `ownership_type` (`INDIVIDUAL_OWNER`, `PARTNER_BUSINESS_OWNER`), `legal_owner_user_id` (FK $\rightarrow$ `users.id` nullable), `legal_owner_partner_id` (FK $\rightarrow$ `partner_profiles.id` nullable), `manager_partner_id` (FK $\rightarrow$ `partner_profiles.id`), `city_id`, `primary_hub_id`, `inventory_source` (`PARTNER_OWNED`, `CONTRIBUTOR_OWNED`), `category` (`CAR`, `BIKE`, `SCOOTER`), `make`, `model`, `variant`, `manufacture_year`, `registration_number_encrypted`, `fuel_type`, `transmission`, `seating_capacity`, `current_odometer_km`, `turnaround_buffer_minutes`, `listing_status`, `verification_status`, `created_at`, `updated_at`.
- Check constraint validates that individual owners have `legal_owner_user_id` and business owners have `legal_owner_partner_id`.
- Initial custody rule: Partner-owned vehicles begin in `PARTNER_CUSTODY`; contributor-owned vehicles begin in `CONTRIBUTOR_CUSTODY`.

### 16. `vehicle_documents`
- `id`, `vehicle_id`, `document_type` (`RC`, `INSURANCE_POLICY`, `COMMERCIAL_PERMIT`, `PUC`, `FITNESS_CERT`), `object_key`, `document_number_encrypted`, `issue_date`, `expiry_date`, `verification_status`, `verified_by_user_id`, `created_at`. Required for compliance auditing.

### 17. `vehicle_pricing_profiles`
- `id`, `vehicle_id`, `hourly_rate`, `daily_rate`, `security_deposit`, `extra_hour_rate`, `late_fee_hourly_rate`, `min_booking_hours`, `effective_from`, `effective_to`, `created_at`.

### 18. `vehicle_images`
- `id`, `vehicle_id`, `object_key`, `image_type`, `display_order`, `is_primary`, `created_at`.

### 19. `vehicle_features`
- `id`, `vehicle_id`, `feature_key` (`ABS`, `AIRBAGS`, `BLUETOOTH`, `GPS_TRACKER`, `HELMET_INCLUDED`), `feature_value`, `created_at`.

### 20. `vehicle_maintenance_blocks`
- `id`, `vehicle_id`, `starts_at`, `ends_at`, `block_type` (`MAINTENANCE`, `OWNER_USE`, `ACCIDENT_REPAIR`, `LEGAL_HOLD`), `reason`, `status`, `created_by_user_id`, `created_at`.

---

## 5. Module 4: Generic Vehicle Custody Tracking (1 Table)

### 21. `vehicle_custody_events`
- `id`, `vehicle_id`, `booking_id` (nullable), `inspection_id` (nullable), `event_type` (`CONTRIBUTOR_TO_PARTNER`, `PARTNER_TO_CUSTOMER`, `CONTRIBUTOR_TO_CUSTOMER`, `CUSTOMER_TO_PARTNER`, `CUSTOMER_TO_CONTRIBUTOR`, `PARTNER_TO_CONTRIBUTOR`, `INTER_HUB_TRANSFER`, `LOGISTICS_RELOCATION`), `handover_address_id` (FK $\rightarrow$ `addresses.id`), `actor_user_id` (FK $\rightarrow$ `users.id`), `authorized_rep_user_id` (FK $\rightarrow$ `users.id` nullable), `from_custodian_user_id` (FK $\rightarrow$ `users.id` nullable), `from_partner_hub_id` (FK $\rightarrow$ `partner_hubs.id` nullable), `to_custodian_user_id` (FK $\rightarrow$ `users.id` nullable), `to_partner_hub_id` (FK $\rightarrow$ `partner_hubs.id` nullable), `odometer_km`, `fuel_or_battery_level`, `notes`, `transferred_at`, `acknowledged_by_recipient_at`, `created_at`.
- Tracks all 8 canonical handover permutations without unvalidated polymorphic foreign keys.

---

## 6. Module 5: Contributor Agreements & Fleet Proposals (4 Tables)

### 22. `contributor_vehicle_applications`
- `id`, `vehicle_id`, `contributor_user_id`, `preferred_partner_id` (nullable), `application_status`, `submitted_at`, `reviewed_at`, `rejection_reason`, `created_at`.

### 23. `contribution_agreements`
- `id`, `vehicle_id`, `contributor_user_id`, `partner_id`, `agreement_number`, `agreement_status`, `version`, `start_at`, `end_at`, `fixed_amount_per_booking`, `currency`, `cancellation_compensation_rate`, `payout_due_hours`, `terms_document_object_key`, `termination_notice_days`, `accepted_by_contributor_at`, `accepted_by_partner_at`, `created_at`, `updated_at`.

### 24. `contribution_agreement_events`
- `id`, `agreement_id`, `from_status`, `to_status`, `event_type`, `details_json`, `actor_user_id`, `occurred_at`.

### 25. `vehicle_withdrawal_requests`
- `id`, `vehicle_id`, `agreement_id`, `requested_by_user_id`, `requested_at`, `effective_withdrawal_date`, `status`, `decision_reason`, `resolved_at`, `created_at`. Confirmed customer bookings strictly supersede withdrawal requests.

---

## 7. Module 6: Bookings, Holds, Extensions, Inspections & Incidents (7 Tables)

### 26. `booking_holds`
- `id`, `vehicle_id`, `customer_user_id`, `starts_at`, `ends_at`, `turnaround_buffer_minutes`, `expires_at`, `status` (`ACTIVE`, `VERIFICATION_PENDING`, `CONVERTED`, `EXPIRED`, `RELEASED`, `REJECTED`), `created_at`.
- 15-minute temporary reservation lock acquired under pessimistic `SELECT FOR UPDATE`. **Does NOT insert incomplete booking records.** When customer submits payment proof, status becomes `VERIFICATION_PENDING` with extended TTL to preserve inventory lock during partner verification.

### 27. `bookings`
- `id`, `booking_reference`, `vehicle_id`, `hold_id` (nullable), `customer_user_id`, `manager_partner_id`, `pickup_location_id`, `return_location_id`, `starts_at`, `ends_at`, `turnaround_buffer_minutes`, `booking_status` (`CONFIRMED`, `READY_FOR_PICKUP`, `IN_PROGRESS`, `OVERDUE`, `RETURNED`, `COMPLETED`, `CANCELLED`, `NO_SHOW`), `currency`, `rental_subtotal`, `delivery_fee`, `platform_fee_snapshot`, `discount_total`, `partner_discount_subsidy`, `tax_total`, `total_rental_amount`, `cancellation_policy_snapshot`, `confirmed_at`, `cancelled_at`, `cancellation_reason`, `created_at`, `updated_at`.
- Created directly upon verified partner payment confirmation (or under authorized pay-at-pickup policy). Operational completion (`COMPLETED`) is strictly decoupled from deposit refunds or contributor payouts.

### 28. `booking_status_events`
- `id`, `booking_id`, `from_status`, `to_status`, `actor_user_id`, `reason`, `occurred_at`.

### 29. `booking_extensions`
- `id`, `booking_id`, `requested_end_at`, `additional_rental_amount`, `demand_id` (nullable), `status`, `decided_by_user_id`, `decided_at`, `created_at`.

### 30. `inspections`
- `id`, `booking_id` (nullable), `vehicle_id`, `performed_by_user_id`, `inspection_type` (`PRE_RENTAL_PICKUP`, `POST_RENTAL_RETURN`, `INCIDENT`), `customer_acknowledged_at`, `odometer_km`, `fuel_or_battery_level`, `condition_summary`, `created_at`.

### 31. `inspection_media`
- `id`, `inspection_id`, `object_key`, `media_type`, `caption`, `captured_at`.

### 32. `incidents`
- `id`, `booking_id`, `vehicle_id`, `reported_by_user_id`, `incident_type`, `description`, `status`, `estimated_repair_cost`, `approved_deduction_amount`, `reported_at`, `resolved_at`, `resolution_notes`.

---

## 8. Module 7: Partner Payment Profiles, Demands, Submissions & Deposits (7 Tables)

### 33. `partner_payment_profiles`
- `id`, `partner_id` (FK $\rightarrow$ `partner_profiles.id`), `payee_display_name`, `upi_id`, `upi_qr_object_key`, `external_payment_url`, `is_pay_at_pickup_enabled`, `accepted_methods_json`, `payment_instructions_note`, `is_active`, `created_at`, `updated_at`. Unique on `partner_id`.
- Stores partner direct payment collection coordinates. Does NOT store PINs, passwords, or sensitive bank secrets.

### 34. `rental_payment_demands`
- `id`, `demand_reference`, `hold_id` (nullable), `booking_id` (nullable), `manager_partner_id`, `customer_user_id`, `demand_type` (`INITIAL_RENTAL`, `EXTENSION`, `DAMAGE_INVOICE`), `amount_demanded`, `security_deposit_demanded`, `currency`, `status` (`ISSUED`, `PAYMENT_SUBMITTED`, `VERIFIED`, `EXPIRED`, `CANCELLED`), `expires_at`, `verified_at`, `created_at`, `updated_at`. Unique on `demand_reference`.
- Records explicit payment obligations generated for reservations, extensions, or invoices.

### 35. `rental_payment_submissions`
- `id`, `demand_id`, `hold_id` (nullable), `booking_id` (nullable), `customer_user_id`, `manager_partner_id`, `payment_method` (`UPI_QR`, `UPI_ID`, `EXTERNAL_LINK`, `CASH_AT_PICKUP`), `amount_submitted`, `currency`, `transaction_reference` (UTR Number / Cash receipt), `transaction_timestamp`, `receipt_object_key`, `status` (`SUBMITTED_FOR_VERIFICATION`, `VERIFIED`, `REJECTED`), `verified_by_user_id` (nullable), `verified_at`, `rejection_reason`, `notes`, `created_at`. Unique on `(manager_partner_id, transaction_reference)`.
- Records customer payment submissions. **Invariant:** Customer upload NEVER auto-verifies; only explicit partner action marks `VERIFIED`.

### 36. `security_deposits`
- `id`, `booking_id` (FK $\rightarrow$ `bookings.id`), `manager_partner_id`, `customer_user_id`, `required_amount`, `collected_amount`, `collection_method`, `collection_reference`, `collection_evidence_object_key`, `total_deducted_amount`, `total_refunded_amount`, `status` (`PENDING_COLLECTION`, `COLLECTED_HELD_BY_PARTNER`, `DEDUCTION_PROPOSED`, `DISPUTED`, `PARTIALLY_REFUNDED`, `FULLY_REFUNDED`, `RETAINED_FOR_DAMAGE`, `CLOSED`), `collected_at`, `closed_at`, `created_at`, `updated_at`. Unique on `booking_id`.
- Tracks security deposits directly held and refunded by the managing partner.

### 37. `deposit_deductions`
- `id`, `security_deposit_id`, `incident_id` (nullable), `proposed_by_user_id`, `resolved_by_admin_id` (nullable), `deduction_type` (`DAMAGE_REPAIR`, `LATE_RETURN_PENALTY`, `FUEL_DEFICIT`, `CLEANING_FEE`, `TRAFFIC_CHALLAN`), `proposed_amount`, `approved_amount`, `evidence_summary`, `status` (`PROPOSED`, `DISPUTED`, `APPROVED_BY_ADMIN`, `AGREED_BY_CUSTOMER`, `CANCELLED`), `dispute_reason`, `dispute_opened_at`, `resolved_at`, `created_at`.
- Granular damage claims against deposit. Disputed claims are never auto-approved upon 48h timeout; require Admin arbitration.

### 38. `security_deposit_refunds`
- `id`, `security_deposit_id`, `booking_id`, `manager_partner_id`, `customer_user_id`, `refund_amount`, `currency`, `refund_method` (`UPI_TRANSFER`, `BANK_NEFT_IMPS`, `CASH`), `refund_transaction_reference` (UTR), `receipt_object_key`, `status` (`PROMISED`, `DISBURSED_BY_PARTNER`, `CONFIRMED_BY_CUSTOMER`, `DISPUTED_BY_CUSTOMER`, `ADMIN_OVERRIDE`), `refunded_at`, `acknowledged_at`, `dispute_notes`, `created_at`, `updated_at`. Unique on `(manager_partner_id, refund_transaction_reference)`.
- Audit trail of deposit refunds executed directly by the partner outside the platform.

### 39. `contributor_payables`
- `id`, `booking_id`, `agreement_id`, `contributor_user_id`, `partner_id`, `fixed_amount`, `currency`, `payable_status` (`PENDING_RETURN`, `ELIGIBLE`, `SCHEDULED`, `SETTLED_BY_PARTNER`, `ACKNOWLEDGED`, `DISPUTED`, `CANCELLED`), `settlement_mode` (`DIRECT_PARTNER_SETTLED`), `bank_transaction_reference` (UTR), `receipt_document_object_key`, `partner_settled_at`, `contributor_acknowledged_at`, `created_at`, `updated_at`. Unique on `(booking_id, agreement_id)`.
- Partner-specific obligation tracking model (NOT a platform general ledger liability). Enforces single, complete direct partner settlement with immutable payment evidence.

---

## 9. Module 8: Transactional Outbox, Support, Reviews & Auditing (6 Tables)

### 40. `outbox_events`
- `id`, `aggregate_type`, `aggregate_id`, `event_type`, `payload_json`, `status` (`PENDING`, `PROCESSING`, `PROCESSED`, `FAILED`, `DEAD_LETTER`), `retry_count`, `next_retry_at`, `failure_reason`, `created_at`, `processed_at`. Resilient outbox for async media processing and notification delivery.

### 41. `reviews`
- `id`, `booking_id`, `reviewer_user_id`, `vehicle_id`, `partner_id`, `rating_score`, `rating_cleanliness`, `rating_maintenance`, `review_text`, `moderation_status`, `created_at`. Unique on `booking_id`.

### 42. `support_tickets`
- `id`, `ticket_number`, `user_id`, `booking_id` (nullable), `vehicle_id` (nullable), `assigned_admin_id` (nullable), `category`, `priority`, `status`, `subject`, `resolved_at`, `created_at`, `updated_at`. Unique on `ticket_number`.

### 43. `support_ticket_messages`
- `id`, `ticket_id`, `sender_user_id`, `message_body`, `attachment_object_key`, `is_internal_note`, `sent_at`.

### 44. `notifications`
- `id`, `user_id`, `channel` (`IN_APP`, `EMAIL`, `SMS`), `template_code`, `title`, `message_content`, `payload_json`, `delivery_status`, `read_at`, `created_at`.

### 45. `audit_logs`
- `id`, `actor_user_id` (nullable), `action`, `entity_type`, `entity_id`, `ip_address`, `correlation_id`, `metadata_json_redacted`, `occurred_at`. Immutable security, payment verification, and administrative audit log.

---

## 10. Module 9: Infrastructure-Managed Tables (2 Tables)

### 46. `shedlock`
- Managed directly by the ShedLock Spring library (`name` PK, `lock_until`, `locked_at`, `locked_by`). Distributed lock for scheduled jobs.

### 47. `flyway_schema_history`
- Managed directly by Flyway migration tool (`installed_rank` PK, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`).

---

## 11. Future-Phase Marketplace Gateway & Double-Entry Tables (8 Tables)

The following 8 tables are preserved and deferred for future activation upon onboarding an approved marketplace payment provider (e.g., Razorpay Route) with automated split disbursements:
1. `payment_webhook_events` (Inward gateway webhook idempotency ledger)
2. `partner_commissions` (Platform commission calculations per booking)
3. `payout_methods` (Tokenized partner linked payout accounts)
4. `settlements` (Outward automated marketplace settlement batches)
5. `ledger_accounts` (Platform Chart of Accounts Master)
6. `ledger_transactions` (Platform double-entry journal headers)
7. `ledger_entries` (Platform append-only double-entry lines)
8. `platform_commission_rules` (Configurable percentage and fee rules)
