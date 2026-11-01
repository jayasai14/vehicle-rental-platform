# Domain Model and Physical Database Design Plan (Freeze Baseline)

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh  
**Status:** Database Design Freeze Candidate Baseline  
**Confirmed Entity Count:** **54 Proposed Physical Tables** (52 Application-Domain Tables + 2 Infrastructure-Managed Tables)

> [!IMPORTANT]
> **Pre-Implementation Notice:** This document defines the **proposed physical database schema** to be created via Flyway migrations in MySQL 8.0+. **No migrations or actual database schema have been generated yet.** Application code, JPA entities, and Flyway migration SQL scripts will only be authored after formal Product Owner database design freeze approval.

---

## 1. Modeling Principles & Invariants

- **RDBMS Engine:** MySQL 8.0+ with InnoDB storage engine, strict foreign key constraints, and utf8mb4 encoding (`utf8mb4_0900_ai_ci`).
- **Identifier Strategy:** `BIGINT UNSIGNED AUTO_INCREMENT` for internal relational primary keys. Secure external identifiers (UUIDv4 or nanoids like `booking_reference`, `provider_order_id`) exposed on public APIs to prevent enumeration attacks.
- **Monetary Precision:** `DECIMAL(10,2)` for localized transactions and `DECIMAL(12,2)` for general ledger journals. Explicit currency code (default `'INR'`). Floating-point types are strictly forbidden.
- **Timezone Standardization:** All timestamps stored in UTC (`DATETIME` or `TIMESTAMP`). Rendered in `Asia/Kolkata` (IST, UTC+5:30) for presentation.
- **Audit & Deletion Policy:** `created_at` on all records; `updated_at` on mutable entities. Soft deletion (`deleted_at`) used only where legally justified; core operational and financial entities use `ON DELETE RESTRICT` to preserve immutable audit trails.
- **Separation of Legal Ownership, Operational Management, and Custody:**
  - Individual Owner: `legal_owner_user_id` (FK $\rightarrow$ `users.id`).
  - Partner Business Owner: `legal_owner_partner_id` (FK $\rightarrow$ `partner_profiles.id`).
  - Operational Manager: `manager_partner_id` (FK $\rightarrow$ `partner_profiles.id`).
  - Physical custody tracked via `vehicle_custody_events` with explicit, typed custodian foreign keys.
- **Double-Entry General Ledger Normalization:** Normalized 3-table ledger architecture (`ledger_accounts`, `ledger_transactions`, `ledger_entries`) guaranteeing $\sum \text{Debits} = \sum \text{Credits}$ per transaction group.

---

## 2. Module 1: Identity, Access, KYC & Addresses (11 Tables)

> Detailed column specifications, constraints, and indexes for all 54 tables are formalized in [FINAL_ENTITY_INVENTORY.md](FINAL_ENTITY_INVENTORY.md) and [ENTITY_RELATIONSHIPS.md](ENTITY_RELATIONSHIPS.md).

### 1. `users`
- `id`, `full_name`, `email`, `phone`, `password_hash`, `account_status` (`PENDING_VERIFICATION`, `ACTIVE`, `SUSPENDED`, `DEACTIVATED`), `email_verified_at`, `phone_verified_at`, `created_at`, `updated_at`. Unique on `email` and `phone`.

### 2. `roles`
- `id`, `code` (`CUSTOMER`, `VEHICLE_CONTRIBUTOR`, `RENTAL_PARTNER`, `ADMIN`), `name`, `description`.

### 3. `user_roles`
- `id`, `user_id`, `role_id`, `granted_by`, `granted_at`. Additive role assignments. `ADMIN` granted strictly via internal administrative provisioning.

### 4. `refresh_tokens`
- `id`, `user_id`, `token_hash` (SHA-256), `device_info`, `ip_address`, `expires_at`, `revoked_at`, `created_at`. Supports rotation and revocation.

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

### 10. `identity_verifications`
- `id`, `user_id`, `verification_type` (`AADHAAR_OKYC`, `PAN`, `PASSPORT`), `provider_name`, `provider_reference`, `verification_status`, `verified_at`, `expires_at`, `created_at`.

### 11. `driver_licences`
- `id`, `user_id`, `licence_number_encrypted`, `issuing_state`, `vehicle_classes`, `valid_from`, `valid_until`, `verification_status`, `provider_reference`, `verified_at`, `created_at`.

---

## 3. Module 2: Operating Locations, Partner Hubs & Logistics (3 Tables)

### 12. `cities`
- `id`, `name`, `state`, `country`, `is_active`, `created_at`. Initial launch: Vijayawada, Andhra Pradesh.

### 13. `partner_locations` (Partner Hubs)
- `id`, `partner_id`, `address_id`, `name`, `contact_person`, `phone`, `operating_hours_json`, `is_active`, `created_at`, `updated_at`. Verified depots/hubs.

### 14. `delivery_zones`
- `id`, `partner_id`, `zone_name`, `max_radius_km`, `base_delivery_fee`, `extra_per_km_fee`, `is_active`, `created_at`. Doorstep delivery radius and tiered pricing.

---

## 4. Module 3: Vehicle Inventory, Compliance & Pricing (6 Tables)

### 15. `vehicles`
- `id`, `ownership_type` (`INDIVIDUAL_OWNER`, `PARTNER_BUSINESS_OWNER`), `legal_owner_user_id` (FK $\rightarrow$ `users.id` nullable), `legal_owner_partner_id` (FK $\rightarrow$ `partner_profiles.id` nullable), `manager_partner_id` (FK $\rightarrow$ `partner_profiles.id`), `city_id`, `primary_hub_location_id`, `inventory_source` (`PARTNER_OWNED`, `CONTRIBUTOR_OWNED`), `category` (`CAR`, `BIKE`, `SCOOTER`), `make`, `model`, `variant`, `manufacture_year`, `registration_number_encrypted`, `fuel_type`, `transmission`, `seating_capacity`, `current_odometer_km`, `turnaround_buffer_minutes`, `current_custody_type`, `current_custodian_id`, `listing_status`, `verification_status`, `created_at`, `updated_at`.
- Check constraint validates that individual owners have `legal_owner_user_id` and business owners have `legal_owner_partner_id`.
- Initial custody rule: Partner-owned vehicles begin in `PARTNER_CUSTODY`; contributor-owned vehicles begin in `CONTRIBUTOR_CUSTODY`.

### 16. `vehicle_documents`
- `id`, `vehicle_id`, `document_type` (`RC`, `INSURANCE_POLICY`, `COMMERCIAL_PERMIT`, `PUC`, `FITNESS_CERT`), `object_key`, `document_number_encrypted`, `issue_date`, `expiry_date`, `verification_status`, `verified_by_user_id`, `created_at`.

### 17. `vehicle_images`
- `id`, `vehicle_id`, `object_key`, `image_type`, `display_order`, `is_primary`, `created_at`.

### 18. `vehicle_pricing`
- `id`, `vehicle_id`, `hourly_rate`, `daily_rate`, `security_deposit`, `extra_hour_rate`, `late_fee_hourly_rate`, `min_booking_hours`, `effective_from`, `effective_to`, `created_by_user_id`, `created_at`.

### 19. `pickup_locations`
- `id`, `vehicle_id`, `location_type` (`PARTNER_HUB`, `CONTRIBUTOR_LOCATION`, `DOORSTEP_DELIVERY`), `partner_location_id` (nullable), `address_id` (nullable), `is_default`, `created_at`.

### 20. `vehicle_availability_blocks`
- `id`, `vehicle_id`, `starts_at`, `ends_at`, `block_type` (`MAINTENANCE`, `OWNER_USE`, `ACCIDENT_REPAIR`, `LEGAL_HOLD`, `BUFFER`), `reason`, `status`, `created_by_user_id`, `created_at`.

---

## 5. Module 4: Generic Vehicle Custody Tracking (1 Table)

### 21. `vehicle_custody_events`
- `id`, `vehicle_id`, `booking_id` (nullable), `inspection_id` (nullable), `event_type` (`CONTRIBUTOR_TO_PARTNER`, `PARTNER_TO_CUSTOMER`, `CONTRIBUTOR_TO_CUSTOMER`, `CUSTOMER_TO_PARTNER`, `CUSTOMER_TO_CONTRIBUTOR`, `PARTNER_TO_CONTRIBUTOR`, `INTER_HUB_TRANSFER`, `LOGISTICS_RELOCATION`), `handover_address_id` (FK $\rightarrow$ `addresses.id`), `actor_user_id` (FK $\rightarrow$ `users.id`), `authorized_rep_user_id` (FK $\rightarrow$ `users.id` nullable), `from_custodian_user_id` (FK $\rightarrow$ `users.id` nullable), `from_partner_location_id` (FK $\rightarrow$ `partner_locations.id` nullable), `to_custodian_user_id` (FK $\rightarrow$ `users.id` nullable), `to_partner_location_id` (FK $\rightarrow$ `partner_locations.id` nullable), `odometer_km`, `fuel_or_battery_level`, `notes`, `transferred_at`, `acknowledged_by_recipient_at`, `created_at`.
- Tracks all 8 canonical handover permutations without unvalidated polymorphic foreign keys.

---

## 6. Module 5: Contributor Agreements & Fleet Proposals (4 Tables)

### 22. `vehicle_contribution_applications`
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
- `id`, `vehicle_id`, `customer_user_id`, `starts_at`, `ends_at`, `turnaround_buffer_minutes`, `expires_at`, `status` (`ACTIVE`, `CONVERTED`, `EXPIRED`, `RELEASED`), `created_at`.
- 15-minute temporary reservation acquired under `SELECT FOR UPDATE`. **Does NOT insert incomplete booking records.**

### 27. `bookings`
- `id`, `booking_reference`, `vehicle_id`, `hold_id` (nullable), `customer_user_id`, `manager_partner_id`, `pickup_location_id`, `return_location_id`, `starts_at`, `ends_at`, `turnaround_buffer_minutes`, `booking_status` (`CONFIRMED`, `READY_FOR_PICKUP`, `IN_PROGRESS`, `OVERDUE`, `RETURNED`, `COMPLETED`, `CANCELLED`, `NO_SHOW`), `currency`, `rental_subtotal`, `delivery_fee`, `platform_fee_snapshot`, `discount_total`, `partner_discount_subsidy`, `tax_total`, `total_rental_amount`, `cancellation_policy_snapshot`, `confirmed_at`, `cancelled_at`, `cancellation_reason`, `created_at`, `updated_at`.
- Created directly upon verified payment capture. Operational completion (`COMPLETED`) is strictly decoupled from deposit refunds or partner settlements.

### 28. `booking_status_events`
- `id`, `booking_id`, `from_status`, `to_status`, `actor_user_id`, `reason`, `occurred_at`.

### 29. `booking_extensions`
- `id`, `booking_id`, `requested_end_at`, `additional_rental_amount`, `payment_order_id`, `status`, `decided_by_user_id`, `decided_at`, `created_at`.

### 30. `inspections`
- `id`, `booking_id` (nullable), `vehicle_id`, `performed_by_user_id`, `inspection_type` (`PRE_RENTAL_PICKUP`, `POST_RENTAL_RETURN`, `INCIDENT`), `customer_acknowledged_at`, `odometer_km`, `fuel_or_battery_level`, `condition_summary`, `created_at`.

### 31. `inspection_media`
- `id`, `inspection_id`, `object_key`, `media_type`, `caption`, `captured_at`.

### 32. `incidents`
- `id`, `booking_id`, `vehicle_id`, `reported_by_user_id`, `incident_type`, `description`, `status`, `estimated_repair_cost`, `approved_deduction_amount`, `reported_at`, `resolved_at`, `resolution_notes`.

---

## 8. Module 7: Payments, Deposits, Deductions, Payouts & Double-Entry (14 Tables)

### 33. `payment_orders`
- `id`, `booking_id` (nullable), `hold_id` (nullable), `order_type` (`INITIAL_BOOKING`, `EXTENSION`, `DAMAGE_INVOICE`), `provider`, `provider_order_id`, `amount`, `currency`, `status`, `idempotency_key`, `created_at`, `paid_at`.

### 34. `payment_transactions`
- `id`, `payment_order_id`, `provider_payment_id`, `payment_method`, `transaction_type` (`CAPTURE`, `AUTHORIZE`), `amount`, `currency`, `status`, `provider_signature`, `created_at`.

### 35. `security_deposits`
- `id`, `booking_id`, `initial_amount`, `current_held_amount`, `total_deducted_amount`, `total_refunded_amount`, `status`, `collected_at`, `closed_at`, `created_at`, `updated_at`. Refundable customer liability, never platform revenue.

### 36. `deposit_deductions`
- `id`, `security_deposit_id`, `incident_id` (nullable), `resolved_by_user_id` (nullable), `deduction_type`, `proposed_amount`, `approved_amount`, `evidence_summary`, `status` (`PROPOSED`, `DISPUTED`, `APPROVED`, `CANCELLED`), `dispute_reason`, `dispute_opened_at`, `dispute_resolved_at`, `created_at`. Never auto-approved upon 48h expiration.

### 37. `payment_webhook_events`
- `id`, `provider`, `provider_event_id`, `event_type`, `payload_json`, `signature_verified`, `processing_status`, `failure_reason`, `received_at`, `processed_at`. Inward idempotency ledger.

### 38. `refunds`
- `id`, `booking_id`, `security_deposit_id` (nullable), `payment_transaction_id` (nullable), `refund_type` (`CANCELLATION_REFUND`, `DEPOSIT_RELEASE`, `DISPUTE_ADJUSTMENT`, `AUTO_REFUND_EXPIRED_HOLD`), `amount`, `currency`, `status` (`INITIATED`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `RETRY_QUEUED`, `MANUAL_REVIEW`, `OFFLINE_SETTLED`), `provider_refund_id`, `idempotency_key`, `offline_utr_reference`, `requested_at`, `processed_at`, `failure_reason`. Decoupled from booking status; failure triggers retry or manual offline settlement.

### 39. `partner_commissions`
- `id`, `booking_id`, `partner_id`, `gross_rental_amount`, `commission_rate_percentage`, `platform_commission_amount`, `tax_on_commission`, `net_partner_entitlement`, `status`, `created_at`.

### 40. `contributor_payables`
- `id`, `booking_id`, `agreement_id`, `contributor_user_id`, `partner_id`, `fixed_amount`, `currency`, `payable_status` (`PENDING_RETURN`, `ELIGIBLE`, `SCHEDULED`, `SETTLED_BY_PARTNER`, `ACKNOWLEDGED`, `DISPUTED`, `CANCELLED`), `settlement_mode` (`DIRECT_PARTNER_SETTLED`, `PLATFORM_FACILITATED`), `bank_transaction_reference` (UTR), `receipt_document_object_key`, `partner_settled_at`, `contributor_acknowledged_at`, `created_at`, `updated_at`.
- Partner-specific obligation tracking model, strictly separated from platform general ledger. Enforces single, indivisible direct partner settlement with immutable payment evidence.

### 41. `payout_methods`
- `id`, `user_id`, `payee_role` (`RENTAL_PARTNER`, `VEHICLE_CONTRIBUTOR`), `provider` (`RAZORPAYX`), `provider_contact_id`, `provider_fund_account_id`, `account_holder_name`, `account_number_last4`, `ifsc_code_encrypted`, `upi_id_masked`, `verification_status`, `is_primary`, `created_at`, `updated_at`.

### 42. `settlements`
- `id`, `payee_user_id`, `payout_method_id` (nullable), `payee_type`, `settlement_type` (`PARTNER_RENTAL_PROCEEDS`), `amount`, `currency`, `status`, `provider_payout_id`, `failure_reason`, `initiated_at`, `completed_at`, `created_at`. Outward partner net payouts.

### 43. `ledger_accounts` (NEW Normalized Domain Table)
- `id`, `account_code` (`GATEWAY_CLEARING`, `CUSTOMER_REFUNDABLE_LIABILITY`, `CUSTOMER_RENTAL_CLEARING`, `PLATFORM_COMMISSION_REVENUE`, `PARTNER_PAYABLE`, `PARTNER_RECEIVABLE`, `REFUND_PAYABLE`, `SETTLEMENT_CLEARING`), `account_name`, `account_category` (`ASSET`, `LIABILITY`, `EQUITY`, `REVENUE`, `EXPENSE`), `normal_balance` (`DEBIT`, `CREDIT`), `currency`, `is_active`, `created_at`. Master Chart of Accounts.

### 44. `ledger_transactions` (NEW Normalized Domain Table)
- `id`, `transaction_group_id` (unique), `reference_type` (`BOOKING`, `PAYMENT_ORDER`, `SECURITY_DEPOSIT`, `REFUND`, `PARTNER_SETTLEMENT`, `REVERSAL_ADJUSTMENT`), `reference_id`, `narrative`, `posted_at`, `created_at`. Journal transaction headers.

### 45. `ledger_entries` (Updated Domain Table)
- `id`, `transaction_id` (FK $\rightarrow$ `ledger_transactions.id`), `account_id` (FK $\rightarrow$ `ledger_accounts.id`), `sequence_number`, `debit_amount`, `credit_amount`, `created_at`. Double-entry journal lines. Every transaction balances ($\sum \text{Debits} = \sum \text{Credits}$).

### 46. `platform_commission_rules`
- `id`, `vehicle_category`, `city_id` (nullable), `partner_tier`, `commission_percentage`, `fixed_fee`, `min_commission`, `effective_from`, `effective_to`, `is_active`, `created_at`.

---

## 9. Module 8: Transactional Outbox, Support, Reviews & Auditing (6 Tables)

### 47. `outbox_events`
- `id`, `aggregate_type`, `aggregate_id`, `event_type`, `payload_json`, `status`, `retry_count`, `next_retry_at`, `failure_reason`, `created_at`, `processed_at`.

### 48. `reviews`
- `id`, `booking_id`, `reviewer_user_id`, `vehicle_id`, `partner_id`, `rating_score`, `rating_cleanliness`, `rating_maintenance`, `review_text`, `moderation_status`, `created_at`.

### 49. `support_tickets`
- `id`, `ticket_number`, `user_id`, `booking_id` (nullable), `vehicle_id` (nullable), `assigned_admin_id` (nullable), `category`, `priority`, `status`, `subject`, `resolved_at`, `created_at`, `updated_at`.

### 50. `support_ticket_messages`
- `id`, `ticket_id`, `sender_user_id`, `message_body`, `attachment_object_key`, `is_internal_note`, `sent_at`.

### 51. `notifications`
- `id`, `user_id`, `channel`, `template_code`, `title`, `message_content`, `payload_json`, `delivery_status`, `read_at`, `created_at`.

### 52. `audit_logs`
- `id`, `actor_user_id`, `action`, `entity_type`, `entity_id`, `ip_address`, `correlation_id`, `metadata_json_redacted`, `occurred_at`.

---

## 10. Module 9: Infrastructure-Managed Tables (2 Tables)

### 53. `shedlock`
- Managed directly by the ShedLock Spring library (`name` PK, `lock_until`, `locked_at`, `locked_by`). Distributed lock for scheduled jobs.

### 54. `flyway_schema_history`
- Managed directly by Flyway migration tool (`installed_rank` PK, `version`, `description`, `type`, `script`, `checksum`, `installed_by`, `installed_on`, `execution_time`, `success`).
