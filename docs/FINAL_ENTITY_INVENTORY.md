# Final Physical Database Entity Inventory (Pre-Freeze Candidate)

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh (Expandable)  
**Status:** Pre-Freeze Corrected Baseline  
**Confirmed Entity Count:** **52 Proposed Physical Tables** (50 Application-Domain Tables + 2 Infrastructure-Managed Tables)  

> [!IMPORTANT]
> **Pre-Implementation Notice:** All tables enumerated below represent the **proposed physical database design** to be created via Flyway migrations in MySQL. **No migrations or actual database schema have been generated yet.** Application code, JPA entities, and SQL DDL scripts will only be authored after formal Product Owner freeze approval.

---

## 1. Physical Table Count Summary

```
┌─────────────────────────────────────────────────────────────┬───────────────┐
│ Functional Domain / Module                                  │ Proposed      │
│                                                             │ Physical Count│
├─────────────────────────────────────────────────────────────┼───────────────┤
│ 1. Identity, Access, KYC & Addresses                        │ 11 tables     │
│ 2. Operating Locations, Partner Hubs & Logistics            │ 3 tables      │
│ 3. Vehicle Inventory, Compliance & Pricing                  │ 6 tables      │
│ 4. Generic Vehicle Custody Tracking                         │ 1 table       │
│ 5. Contributor Agreements & Fleet Proposals                 │ 4 tables      │
│ 6. Bookings, Holds, Extensions, Inspections & Incidents     │ 7 tables      │
│ 7. Payments, Deposits, Deductions, Payouts & Double-Entry   │ 12 tables     │
│ 8. Transactional Outbox, Support, Reviews & Auditing        │ 6 tables      │
├─────────────────────────────────────────────────────────────┼───────────────┤
│ Total Proposed Application Domain Tables                    │ 50 tables     │
│ 9. Infrastructure-Managed Tables (ShedLock & Flyway)        │ 2 tables      │
├─────────────────────────────────────────────────────────────┼───────────────┤
│ TOTAL PROPOSED PHYSICAL MYSQL TABLES                        │ 52 tables     │
└─────────────────────────────────────────────────────────────┴───────────────┘
```

*Note on Ledger Normalization:* In this confirmed 52-table baseline, the double-entry financial journal is modeled via `ledger_entries` using an identifiable `transaction_group_id` and strict ENUM Chart of Accounts. If the Product Owner mandates full relational normalization into separate master tables (`ledger_accounts` and `ledger_transactions`), the domain count expands to 52 tables (54 physical tables total). This alternative is fully documented in [FINANCIAL_ARCHITECTURE.md](file:///d:/vehicle-rental-platform/docs/FINANCIAL_ARCHITECTURE.md#3-physical-table-design-confirmed-52-table-baseline-vs-normalized-54-table-option).

---

## 2. Table-by-Table Inventory (All 52 Tables Individually Listed)

### Module 1: Identity, Access, KYC & Addresses (11 Tables)

#### 1. `users`
- **Responsibility:** Central authentication and account entity.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** None
- **Important Columns:** `full_name` (VARCHAR 100), `email` (VARCHAR 191), `phone` (VARCHAR 20), `password_hash` (VARCHAR 255), `account_status` (ENUM: `PENDING_VERIFICATION`, `ACTIVE`, `SUSPENDED`, `DEACTIVATED`), `email_verified_at` (DATETIME NULL), `phone_verified_at` (DATETIME NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_users_email` (`email`), `uk_users_phone` (`phone`)
- **Important Indexes:** `idx_users_account_status` (`account_status`)

#### 2. `roles`
- **Responsibility:** Master list of platform roles.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** None
- **Important Columns:** `code` (VARCHAR 30: `CUSTOMER`, `VEHICLE_CONTRIBUTOR`, `RENTAL_PARTNER`, `ADMIN`), `name` (VARCHAR 50), `description` (VARCHAR 255).
- **Unique Constraints:** `uk_roles_code` (`code`)

#### 3. `user_roles`
- **Responsibility:** Additive assignment of roles to users.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `user_id` -> `users.id` (ON DELETE CASCADE), `role_id` -> `roles.id` (ON DELETE RESTRICT), `granted_by` -> `users.id` (NULLABLE)
- **Important Columns:** `granted_at` (DATETIME).
- **Unique Constraints:** `uk_user_roles_pair` (`user_id`, `role_id`)

#### 4. `refresh_tokens`
- **Responsibility:** Cryptographically hashed refresh tokens supporting rotation and revocation. Standardized physical table.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `user_id` -> `users.id` (ON DELETE CASCADE)
- **Important Columns:** `token_hash` (VARCHAR 64 SHA-256), `device_info` (VARCHAR 255), `ip_address` (VARCHAR 45), `expires_at` (DATETIME), `revoked_at` (DATETIME NULL), `created_at`.
- **Unique Constraints:** `uk_refresh_tokens_hash` (`token_hash`)
- **Important Indexes:** `idx_refresh_tokens_user_expires` (`user_id`, `expires_at`)

#### 5. `addresses`
- **Responsibility:** Unified geographic address table for users, partner hubs, and pickup/dropoff points.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `city_id` -> `cities.id` (ON DELETE RESTRICT), `user_id` -> `users.id` (NULLABLE, ON DELETE CASCADE)
- **Important Columns:** `address_line1` (VARCHAR 255), `address_line2` (VARCHAR 255 NULL), `landmark` (VARCHAR 100 NULL), `postal_code` (VARCHAR 10), `latitude` (DECIMAL(10,8) NULL), `longitude` (DECIMAL(11,8) NULL), `address_type` (ENUM: `CUSTOMER_HOME`, `PARTNER_HUB`, `PICKUP_POINT`, `DELIVERY_DESTINATION`), `created_at`, `updated_at`.
- **Important Indexes:** `idx_addresses_city` (`city_id`), `idx_addresses_lat_long` (`latitude`, `longitude`)

#### 6. `customer_profiles`
- **Responsibility:** Customer-specific verification and profile data.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `user_id` -> `users.id` (ON DELETE CASCADE), `address_id` -> `addresses.id` (NULLABLE)
- **Important Columns:** `date_of_birth` (DATE NULL), `emergency_contact_name` (VARCHAR 100 NULL), `emergency_contact_phone` (VARCHAR 20 NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_customer_profiles_user` (`user_id`)

#### 7. `partner_profiles`
- **Responsibility:** Verified business profile for rental partner organizations.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `user_id` -> `users.id` (ON DELETE RESTRICT), `verified_by_user_id` -> `users.id` (NULLABLE)
- **Important Columns:** `business_legal_name` (VARCHAR 150), `trade_name` (VARCHAR 150), `gstin_encrypted` (VARCHAR 255 NULL), `pan_encrypted` (VARCHAR 255 NULL), `contact_email` (VARCHAR 191), `contact_phone` (VARCHAR 20), `verification_status` (ENUM: `SUBMITTED`, `IN_REVIEW`, `VERIFIED`, `REJECTED`, `SUSPENDED`), `submitted_at` (DATETIME), `verified_at` (DATETIME NULL), `rejection_reason` (TEXT NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_partner_profiles_user` (`user_id`)
- **Important Indexes:** `idx_partner_verification_status` (`verification_status`)

#### 8. `partner_documents`
- **Responsibility:** Business verification compliance documents (Trade licence, GSTIN, commercial permits).
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `partner_id` -> `partner_profiles.id` (ON DELETE CASCADE), `verified_by_user_id` -> `users.id` (NULLABLE)
- **Important Columns:** `document_type` (ENUM: `TRADE_LICENCE`, `GSTIN_CERTIFICATE`, `COMMERCIAL_PERMIT`, `MSME_CERTIFICATE`, `OTHER`), `object_key` (VARCHAR 255), `document_number_encrypted` (VARCHAR 255 NULL), `verification_status` (ENUM: `PENDING`, `VERIFIED`, `REJECTED`), `rejection_reason` (TEXT NULL), `verified_at` (DATETIME NULL), `created_at`.
- **Important Indexes:** `idx_partner_docs_partner` (`partner_id`)

#### 9. `contributor_profiles`
- **Responsibility:** Vehicle contributor profile and tax identification.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `user_id` -> `users.id` (ON DELETE CASCADE), `verified_by_user_id` -> `users.id` (NULLABLE)
- **Important Columns:** `pan_encrypted` (VARCHAR 255 NULL), `verification_status` (ENUM: `PENDING`, `VERIFIED`, `REJECTED`), `verified_at` (DATETIME NULL), `rejection_reason` (TEXT NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_contributor_profiles_user` (`user_id`)

#### 10. `identity_verifications`
- **Responsibility:** KYC verification logs (Aadhaar OKYC token, PAN check) without storing raw restricted ID numbers.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `user_id` -> `users.id` (ON DELETE CASCADE)
- **Important Columns:** `verification_type` (ENUM: `AADHAAR_OKYC`, `PAN`, `PASSPORT`), `provider_name` (VARCHAR 50), `provider_reference` (VARCHAR 100), `verification_status` (ENUM: `PENDING`, `VERIFIED`, `FAILED`, `EXPIRED`), `verified_at` (DATETIME NULL), `expires_at` (DATETIME NULL), `created_at`.
- **Important Indexes:** `idx_id_verifications_user` (`user_id`)

#### 11. `driver_licences`
- **Responsibility:** Driving licence details for customer rental eligibility.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `user_id` -> `users.id` (ON DELETE CASCADE), `verified_by_user_id` -> `users.id` (NULLABLE)
- **Important Columns:** `licence_number_encrypted` (VARCHAR 255), `issuing_state` (VARCHAR 50), `vehicle_classes` (VARCHAR 100), `valid_from` (DATE), `valid_until` (DATE), `verification_status` (ENUM: `PENDING`, `VERIFIED`, `REJECTED`, `EXPIRED`), `provider_reference` (VARCHAR 100 NULL), `verified_at` (DATETIME NULL), `created_at`.
- **Unique Constraints:** `uk_driver_licences_user` (`user_id`)
- **Important Indexes:** `idx_driver_licences_validity` (`valid_until`, `verification_status`)

---

### Module 2: Operating Locations, Partner Hubs & Logistics (3 Tables)

#### 12. `cities`
- **Responsibility:** Operational cities/municipalities.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** None
- **Important Columns:** `name` (VARCHAR 100), `state` (VARCHAR 100), `country` (VARCHAR 50 DEFAULT 'India'), `is_active` (BOOLEAN DEFAULT TRUE), `created_at`.
- **Unique Constraints:** `uk_cities_name_state` (`name`, `state`)

#### 13. `partner_locations`
- **Responsibility:** Verified physical depots, garages, and hubs operated by rental partners.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `partner_id` -> `partner_profiles.id` (ON DELETE RESTRICT), `address_id` -> `addresses.id` (ON DELETE RESTRICT)
- **Important Columns:** `name` (VARCHAR 100), `contact_person` (VARCHAR 100), `phone` (VARCHAR 20), `operating_hours_json` (JSON), `is_active` (BOOLEAN DEFAULT TRUE), `created_at`, `updated_at`.
- **Important Indexes:** `idx_partner_locations_partner` (`partner_id`)

#### 14. `delivery_zones`
- **Responsibility:** Doorstep delivery service radius and tiered delivery pricing.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `partner_id` -> `partner_profiles.id` (ON DELETE CASCADE)
- **Important Columns:** `zone_name` (VARCHAR 100), `max_radius_km` (DECIMAL(5,2)), `base_delivery_fee` (DECIMAL(10,2)), `extra_per_km_fee` (DECIMAL(10,2)), `is_active` (BOOLEAN DEFAULT TRUE), `created_at`.
- **Important Indexes:** `idx_delivery_zones_partner` (`partner_id`)

---

### Module 3: Vehicle Inventory, Compliance & Pricing (6 Tables)

#### 15. `vehicles`
- **Responsibility:** Core vehicle identity, legal ownership, operational manager assignment, and status.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `legal_owner_user_id` -> `users.id` (ON DELETE RESTRICT), `manager_partner_id` -> `partner_profiles.id` (ON DELETE RESTRICT), `city_id` -> `cities.id` (ON DELETE RESTRICT), `primary_hub_location_id` -> `partner_locations.id` (NULLABLE, ON DELETE RESTRICT)
- **Important Columns:** `inventory_source` (ENUM: `PARTNER_OWNED`, `CONTRIBUTOR_OWNED`), `category` (ENUM: `CAR`, `BIKE`, `SCOOTER`), `make` (VARCHAR 50), `model` (VARCHAR 50), `variant` (VARCHAR 50 NULL), `manufacture_year` (SMALLINT UNSIGNED), `registration_number_encrypted` (VARCHAR 255), `fuel_type` (ENUM: `PETROL`, `DIESEL`, `ELECTRIC`, `CNG`), `transmission` (ENUM: `MANUAL`, `AUTOMATIC`), `seating_capacity` (TINYINT UNSIGNED), `current_odometer_km` (INT UNSIGNED DEFAULT 0), `turnaround_buffer_minutes` (SMALLINT UNSIGNED DEFAULT 60), `current_custody_type` (ENUM: `CONTRIBUTOR`, `PARTNER`, `CUSTOMER`, `LOGISTICS_AGENT`), `current_custodian_id` (BIGINT UNSIGNED), `listing_status` (ENUM: `DRAFT`, `PENDING_VERIFICATION`, `PUBLISHED`, `PAUSED`, `ARCHIVED`), `verification_status` (ENUM: `UNVERIFIED`, `VERIFIED`, `REJECTED`), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_vehicles_reg_encrypted` (`registration_number_encrypted`)
- **Important Indexes:** `idx_vehicles_search` (`category`, `city_id`, `listing_status`, `verification_status`), `idx_vehicles_manager` (`manager_partner_id`), `idx_vehicles_owner` (`legal_owner_user_id`)

#### 16. `vehicle_documents`
- **Responsibility:** Regulatory vehicular documents (RC, Insurance, Commercial Permit, PUC).
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` -> `vehicles.id` (ON DELETE CASCADE), `verified_by_user_id` -> `users.id` (NULLABLE)
- **Important Columns:** `document_type` (ENUM: `RC`, `INSURANCE_POLICY`, `COMMERCIAL_PERMIT`, `PUC`, `FITNESS_CERT`), `object_key` (VARCHAR 255), `document_number_encrypted` (VARCHAR 255 NULL), `issue_date` (DATE NULL), `expiry_date` (DATE NULL), `verification_status` (ENUM: `PENDING`, `VERIFIED`, `REJECTED`), `rejection_reason` (TEXT NULL), `verified_at` (DATETIME NULL), `created_at`.
- **Important Indexes:** `idx_vehicle_docs_vehicle` (`vehicle_id`), `idx_vehicle_docs_expiry` (`expiry_date`)

#### 17. `vehicle_images`
- **Responsibility:** Vehicle photo assets with ordered display types.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` -> `vehicles.id` (ON DELETE CASCADE)
- **Important Columns:** `object_key` (VARCHAR 255), `image_type` (ENUM: `EXTERIOR_FRONT`, `EXTERIOR_REAR`, `EXTERIOR_SIDE`, `INTERIOR`, `DASHBOARD`, `FEATURE`), `display_order` (SMALLINT UNSIGNED DEFAULT 0), `is_primary` (BOOLEAN DEFAULT FALSE), `created_at`.
- **Important Indexes:** `idx_vehicle_images_vehicle` (`vehicle_id`, `display_order`)

#### 18. `vehicle_pricing`
- **Responsibility:** Rental price schedule, deposit amounts, and minimum durations.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` -> `vehicles.id` (ON DELETE CASCADE), `created_by_user_id` -> `users.id` (ON DELETE RESTRICT)
- **Important Columns:** `hourly_rate` (DECIMAL(10,2)), `daily_rate` (DECIMAL(10,2)), `security_deposit` (DECIMAL(10,2)), `extra_hour_rate` (DECIMAL(10,2)), `late_fee_hourly_rate` (DECIMAL(10,2)), `min_booking_hours` (SMALLINT UNSIGNED DEFAULT 4), `effective_from` (DATETIME), `effective_to` (DATETIME NULL), `created_at`.
- **Important Indexes:** `idx_vehicle_pricing_lookup` (`vehicle_id`, `effective_from`, `effective_to`)

#### 19. `pickup_locations`
- **Responsibility:** Configured pickup arrangements per vehicle (hub pickup, owner direct handover, or doorstep delivery).
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` -> `vehicles.id` (ON DELETE CASCADE), `partner_location_id` -> `partner_locations.id` (NULLABLE), `address_id` -> `addresses.id` (NULLABLE)
- **Important Columns:** `location_type` (ENUM: `PARTNER_HUB`, `CONTRIBUTOR_LOCATION`, `DOORSTEP_DELIVERY`), `is_default` (BOOLEAN DEFAULT FALSE), `created_at`.
- **Important Indexes:** `idx_pickup_locations_vehicle` (`vehicle_id`)

#### 20. `vehicle_availability_blocks`
- **Responsibility:** Calendar blackout blocks (maintenance, owner personal use, legal holds).
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` -> `vehicles.id` (ON DELETE CASCADE), `created_by_user_id` -> `users.id` (ON DELETE RESTRICT)
- **Important Columns:** `starts_at` (DATETIME), `ends_at` (DATETIME), `block_type` (ENUM: `MAINTENANCE`, `OWNER_USE`, `ACCIDENT_REPAIR`, `LEGAL_HOLD`, `BUFFER`), `reason` (VARCHAR 255 NULL), `status` (ENUM: `ACTIVE`, `CANCELLED`), `created_at`.
- **Important Indexes:** `idx_avail_blocks_interval` (`vehicle_id`, `status`, `starts_at`, `ends_at`)

---

### Module 4: Generic Vehicle Custody Tracking (1 Table)

#### 21. `vehicle_custody_events`
- **Responsibility:** Auditable physical custody event ledger supporting all 8 handover permutations. Custody is independent of legal ownership and booking status.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` -> `vehicles.id` (ON DELETE RESTRICT), `booking_id` -> `bookings.id` (NULLABLE, ON DELETE RESTRICT), `inspection_id` -> `inspections.id` (NULLABLE, ON DELETE RESTRICT), `handover_address_id` -> `addresses.id` (ON DELETE RESTRICT), `actor_user_id` -> `users.id` (ON DELETE RESTRICT)
- **Important Columns:** `event_type` (ENUM: `CONTRIBUTOR_TO_PARTNER`, `PARTNER_TO_CUSTOMER`, `CONTRIBUTOR_TO_CUSTOMER`, `CUSTOMER_TO_PARTNER`, `CUSTOMER_TO_CONTRIBUTOR`, `PARTNER_TO_CONTRIBUTOR`, `INTER_HUB_TRANSFER`, `LOGISTICS_RELOCATION`), `from_custodian_type` (ENUM: `CONTRIBUTOR`, `PARTNER`, `CUSTOMER`, `LOGISTICS_AGENT`), `from_custodian_id` (BIGINT UNSIGNED), `to_custodian_type` (ENUM: `CONTRIBUTOR`, `PARTNER`, `CUSTOMER`, `LOGISTICS_AGENT`), `to_custodian_id` (BIGINT UNSIGNED), `odometer_km` (INT UNSIGNED), `fuel_or_battery_level` (TINYINT UNSIGNED: 0-100), `notes` (TEXT NULL), `transferred_at` (DATETIME), `acknowledged_by_recipient_at` (DATETIME NULL), `created_at`.
- **Important Indexes:** `idx_custody_vehicle_time` (`vehicle_id`, `transferred_at`), `idx_custody_booking` (`booking_id`)

---

### Module 5: Contributor Agreements & Fleet Proposals (4 Tables)

#### 22. `vehicle_contribution_applications`
- **Responsibility:** Application submissions by vehicle owners to rental partners.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` -> `vehicles.id` (ON DELETE RESTRICT), `contributor_user_id` -> `users.id` (ON DELETE RESTRICT), `preferred_partner_id` -> `partner_profiles.id` (NULLABLE, ON DELETE RESTRICT)
- **Important Columns:** `application_status` (ENUM: `SUBMITTED`, `UNDER_REVIEW`, `PROPOSAL_MADE`, `ACCEPTED`, `REJECTED`, `WITHDRAWN`), `submitted_at` (DATETIME), `reviewed_at` (DATETIME NULL), `rejection_reason` (TEXT NULL), `created_at`.
- **Important Indexes:** `idx_contrib_app_status` (`contributor_user_id`, `application_status`)

#### 23. `contribution_agreements`
- **Responsibility:** Legally binding commercial agreements defining fixed payment per booking, notice periods, and terms.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` -> `vehicles.id` (ON DELETE RESTRICT), `contributor_user_id` -> `users.id` (ON DELETE RESTRICT), `partner_id` -> `partner_profiles.id` (ON DELETE RESTRICT)
- **Important Columns:** `agreement_number` (VARCHAR 50), `agreement_status` (ENUM: `DRAFT`, `PROPOSED`, `ACCEPTED`, `ACTIVE`, `SUSPENDED`, `TERMINATION_REQUESTED`, `TERMINATED`, `EXPIRED`), `version` (INT UNSIGNED DEFAULT 1), `start_at` (DATETIME), `end_at` (DATETIME), `fixed_amount_per_booking` (DECIMAL(10,2)), `currency` (CHAR(3) DEFAULT 'INR'), `cancellation_compensation_rate` (DECIMAL(5,2) NULL), `payout_due_hours` (INT UNSIGNED DEFAULT 48), `terms_document_object_key` (VARCHAR 255 NULL), `termination_notice_days` (TINYINT UNSIGNED DEFAULT 7), `accepted_by_contributor_at` (DATETIME NULL), `accepted_by_partner_at` (DATETIME NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_agreements_number` (`agreement_number`)
- **Important Indexes:** `idx_agreements_vehicle_status` (`vehicle_id`, `agreement_status`)

#### 24. `contribution_agreement_events`
- **Responsibility:** Immutable audit trail for agreement negotiations, proposals, suspensions, and terminations.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `agreement_id` -> `contribution_agreements.id` (ON DELETE CASCADE), `actor_user_id` -> `users.id` (ON DELETE RESTRICT)
- **Important Columns:** `from_status` (VARCHAR 30 NULL), `to_status` (VARCHAR 30), `event_type` (VARCHAR 50), `details_json` (JSON NULL), `occurred_at` (DATETIME).
- **Important Indexes:** `idx_agreement_events_agreement` (`agreement_id`)

#### 25. `vehicle_withdrawal_requests`
- **Responsibility:** Vehicle reclamation requests initiated by contributors, protecting active customer bookings.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` -> `vehicles.id` (ON DELETE RESTRICT), `agreement_id` -> `contribution_agreements.id` (ON DELETE RESTRICT), `requested_by_user_id` -> `users.id` (ON DELETE RESTRICT)
- **Important Columns:** `requested_at` (DATETIME), `effective_withdrawal_date` (DATETIME), `status` (ENUM: `SUBMITTED`, `ACKNOWLEDGED`, `SETTLING_BOOKINGS`, `READY_FOR_HANDOVER`, `COMPLETED`, `REJECTED`), `decision_reason` (TEXT NULL), `resolved_at` (DATETIME NULL), `created_at`.
- **Important Indexes:** `idx_withdrawal_vehicle_status` (`vehicle_id`, `status`)

---

### Module 6: Bookings, Holds, Extensions, Inspections & Incidents (7 Tables)

#### 26. `booking_holds`
- **Responsibility:** 15-minute temporary reservation locks acquired under pessimistic `SELECT FOR UPDATE` locking. Does NOT insert incomplete booking rows.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` -> `vehicles.id` (ON DELETE RESTRICT), `customer_user_id` -> `users.id` (ON DELETE RESTRICT)
- **Important Columns:** `starts_at` (DATETIME), `ends_at` (DATETIME), `turnaround_buffer_minutes` (SMALLINT UNSIGNED), `expires_at` (DATETIME), `status` (ENUM: `ACTIVE`, `CONVERTED`, `EXPIRED`, `RELEASED`), `created_at`.
- **Important Indexes:** `idx_booking_holds_active` (`vehicle_id`, `status`, `expires_at`), `idx_booking_holds_customer` (`customer_user_id`, `status`)

#### 27. `bookings`
- **Responsibility:** Master booking contract created directly upon payment confirmation.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` -> `vehicles.id` (ON DELETE RESTRICT), `hold_id` -> `booking_holds.id` (NULLABLE, ON DELETE SET NULL), `customer_user_id` -> `users.id` (ON DELETE RESTRICT), `manager_partner_id` -> `partner_profiles.id` (ON DELETE RESTRICT), `pickup_location_id` -> `pickup_locations.id` (ON DELETE RESTRICT), `return_location_id` -> `pickup_locations.id` (ON DELETE RESTRICT), `cancellation_actor_user_id` -> `users.id` (NULLABLE)
- **Important Columns:** `booking_reference` (VARCHAR 32), `starts_at` (DATETIME), `ends_at` (DATETIME), `turnaround_buffer_minutes` (SMALLINT UNSIGNED), `booking_status` (ENUM: `CONFIRMED`, `READY_FOR_PICKUP`, `IN_PROGRESS`, `OVERDUE`, `RETURNED`, `COMPLETED`, `CANCELLED`, `NO_SHOW`), `currency` (CHAR(3) DEFAULT 'INR'), `rental_subtotal` (DECIMAL(10,2)), `delivery_fee` (DECIMAL(10,2) DEFAULT 0.00), `platform_fee_snapshot` (DECIMAL(10,2)), `discount_total` (DECIMAL(10,2) DEFAULT 0.00), `partner_discount_subsidy` (DECIMAL(10,2) DEFAULT 0.00), `tax_total` (DECIMAL(10,2)), `total_rental_amount` (DECIMAL(10,2)), `cancellation_policy_snapshot` (JSON), `confirmed_at` (DATETIME), `cancelled_at` (DATETIME NULL), `cancellation_reason` (VARCHAR 255 NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_bookings_reference` (`booking_reference`)
- **Important Indexes:** `idx_bookings_schedule` (`vehicle_id`, `booking_status`, `starts_at`, `ends_at`), `idx_bookings_customer` (`customer_user_id`), `idx_bookings_manager` (`manager_partner_id`)

#### 28. `booking_status_events`
- **Responsibility:** Immutable audit history of booking operational status transitions.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `booking_id` -> `bookings.id` (ON DELETE CASCADE), `actor_user_id` -> `users.id` (NULLABLE)
- **Important Columns:** `from_status` (VARCHAR 30 NULL), `to_status` (VARCHAR 30), `reason` (VARCHAR 255 NULL), `occurred_at` (DATETIME).
- **Important Indexes:** `idx_booking_status_events_booking` (`booking_id`)

#### 29. `booking_extensions`
- **Responsibility:** Customer requests to extend ongoing rentals.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `booking_id` -> `bookings.id` (ON DELETE RESTRICT), `decided_by_user_id` -> `users.id` (NULLABLE)
- **Important Columns:** `requested_end_at` (DATETIME), `additional_rental_amount` (DECIMAL(10,2)), `payment_order_id` (BIGINT UNSIGNED NULL), `status` (ENUM: `PENDING_APPROVAL`, `PAYMENT_PENDING`, `APPROVED`, `REJECTED`, `EXPIRED`), `decided_at` (DATETIME NULL), `created_at`.
- **Important Indexes:** `idx_booking_extensions_booking` (`booking_id`)

#### 30. `inspections`
- **Responsibility:** Formal vehicle condition reports (pre-rental pickup and post-rental return).
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `booking_id` -> `bookings.id` (NULLABLE, ON DELETE RESTRICT), `vehicle_id` -> `vehicles.id` (ON DELETE RESTRICT), `performed_by_user_id` -> `users.id` (ON DELETE RESTRICT)
- **Important Columns:** `inspection_type` (ENUM: `PRE_RENTAL_PICKUP`, `POST_RENTAL_RETURN`, `INCIDENT`), `customer_acknowledged_at` (DATETIME NULL), `odometer_km` (INT UNSIGNED), `fuel_or_battery_level` (TINYINT UNSIGNED: 0-100), `condition_summary` (TEXT NULL), `created_at`.
- **Important Indexes:** `idx_inspections_booking` (`booking_id`), `idx_inspections_vehicle` (`vehicle_id`)

#### 31. `inspection_media`
- **Responsibility:** Photos and visual evidence captured during vehicle inspections.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `inspection_id` -> `inspections.id` (ON DELETE CASCADE)
- **Important Columns:** `object_key` (VARCHAR 255), `media_type` (ENUM: `PHOTO_FRONT`, `PHOTO_REAR`, `PHOTO_LEFT`, `PHOTO_RIGHT`, `ODOMETER_PHOTO`, `FUEL_DASH_PHOTO`, `DAMAGE_DETAIL`), `caption` (VARCHAR 255 NULL), `captured_at` (DATETIME).
- **Important Indexes:** `idx_inspection_media_inspection` (`inspection_id`)

#### 32. `incidents`
- **Responsibility:** Damage, mechanical breakdown, traffic violation, or theft claim records. Standardized physical table.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `booking_id` -> `bookings.id` (ON DELETE RESTRICT), `vehicle_id` -> `vehicles.id` (ON DELETE RESTRICT), `reported_by_user_id` -> `users.id` (ON DELETE RESTRICT)
- **Important Columns:** `incident_type` (ENUM: `ACCIDENT`, `SCRATCH_DENT`, `MECHANICAL_BREAKDOWN`, `TRAFFIC_FINE`, `FUEL_DEFICIT`, `THEFT`), `description` (TEXT), `status` (ENUM: `REPORTED`, `UNDER_REVIEW`, `ESTIMATED`, `DISPUTED`, `SETTLED`, `REJECTED`), `estimated_repair_cost` (DECIMAL(10,2) NULL), `approved_deduction_amount` (DECIMAL(10,2) NULL), `reported_at` (DATETIME), `resolved_at` (DATETIME NULL), `resolution_notes` (TEXT NULL).
- **Important Indexes:** `idx_incidents_booking` (`booking_id`), `idx_incidents_vehicle` (`vehicle_id`)

---

### Module 7: Payments, Deposits, Deductions, Payouts & Double-Entry (12 Tables)

#### 33. `payment_orders`
- **Responsibility:** Payment orders created with payment gateway for initial rentals, extensions, or invoices.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `booking_id` -> `bookings.id` (NULLABLE, ON DELETE RESTRICT), `hold_id` -> `booking_holds.id` (NULLABLE, ON DELETE SET NULL)
- **Important Columns:** `order_type` (ENUM: `INITIAL_BOOKING`, `EXTENSION`, `DAMAGE_INVOICE`), `provider` (VARCHAR 30: `RAZORPAY`), `provider_order_id` (VARCHAR 100), `amount` (DECIMAL(10,2)), `currency` (CHAR(3) DEFAULT 'INR'), `status` (ENUM: `CREATED`, `ATTEMPTED`, `PAID`, `FAILED`, `EXPIRED`), `idempotency_key` (VARCHAR 64), `created_at`, `paid_at` (DATETIME NULL).
- **Unique Constraints:** `uk_payment_orders_provider_id` (`provider_order_id`), `uk_payment_orders_idempotency` (`idempotency_key`)
- **Important Indexes:** `idx_payment_orders_booking` (`booking_id`)

#### 34. `payment_transactions`
- **Responsibility:** Inward transaction records and attempts confirmed by payment gateway.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `payment_order_id` -> `payment_orders.id` (ON DELETE RESTRICT)
- **Important Columns:** `provider_payment_id` (VARCHAR 100), `payment_method` (ENUM: `UPI`, `CARD`, `NETBANKING`, `WALLET`), `transaction_type` (ENUM: `CAPTURE`, `AUTHORIZE`), `amount` (DECIMAL(10,2)), `currency` (CHAR(3) DEFAULT 'INR'), `status` (ENUM: `SUCCESS`, `FAILED`), `provider_signature` (VARCHAR 255 NULL), `created_at`.
- **Unique Constraints:** `uk_payment_transactions_provider_id` (`provider_payment_id`)
- **Important Indexes:** `idx_payment_tx_order` (`payment_order_id`)

#### 35. `security_deposits`
- **Responsibility:** Explicit tracking of customer security deposits held as refundable liabilities.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `booking_id` -> `bookings.id` (ON DELETE RESTRICT)
- **Important Columns:** `initial_amount` (DECIMAL(10,2)), `current_held_amount` (DECIMAL(10,2)), `total_deducted_amount` (DECIMAL(10,2) DEFAULT 0.00), `total_refunded_amount` (DECIMAL(10,2) DEFAULT 0.00), `status` (ENUM: `COLLECTED`, `HELD`, `DEDUCTION_PROPOSED`, `DISPUTED`, `PARTIALLY_DEDUCTED`, `PARTIALLY_REFUNDED`, `FULLY_REFUNDED`, `CLOSED`), `collected_at` (DATETIME), `closed_at` (DATETIME NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_security_deposits_booking` (`booking_id`)
- **Important Indexes:** `idx_security_deposits_status` (`status`)

#### 36. `deposit_deductions`
- **Responsibility:** Granular damage, fuel, or penalty deduction claims against a deposit. Never auto-approved upon 48h expiration.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `security_deposit_id` -> `security_deposits.id` (ON DELETE RESTRICT), `incident_id` -> `incidents.id` (NULLABLE, ON DELETE RESTRICT), `resolved_by_user_id` -> `users.id` (NULLABLE)
- **Important Columns:** `deduction_type` (ENUM: `DAMAGE_REPAIR`, `LATE_RETURN_PENALTY`, `FUEL_DEFICIT`, `CLEANING_FEE`, `TRAFFIC_CHALLAN`), `proposed_amount` (DECIMAL(10,2)), `approved_amount` (DECIMAL(10,2) NULL), `evidence_summary` (TEXT), `status` (ENUM: `PROPOSED`, `DISPUTED`, `APPROVED`, `CANCELLED`), `dispute_reason` (TEXT NULL), `dispute_opened_at` (DATETIME NULL), `dispute_resolved_at` (DATETIME NULL), `created_at`.
- **Important Indexes:** `idx_deposit_deductions_deposit` (`security_deposit_id`)

#### 37. `payment_webhook_events`
- **Responsibility:** Inward webhook idempotency ledger guaranteeing once-and-only-once processing.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** None
- **Important Columns:** `provider` (VARCHAR 30: `RAZORPAY`), `provider_event_id` (VARCHAR 100), `event_type` (VARCHAR 50), `payload_json` (JSON), `signature_verified` (BOOLEAN DEFAULT FALSE), `processing_status` (ENUM: `PENDING`, `PROCESSED`, `IGNORED`, `FAILED`), `failure_reason` (TEXT NULL), `received_at` (DATETIME), `processed_at` (DATETIME NULL).
- **Unique Constraints:** `uk_webhook_provider_event` (`provider`, `provider_event_id`)
- **Important Indexes:** `idx_webhook_processing_status` (`processing_status`)

#### 38. `refunds`
- **Responsibility:** Auditable customer refunds (cancellations, deposit releases, and dispute adjustments) with retry tracking.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `booking_id` -> `bookings.id` (ON DELETE RESTRICT), `security_deposit_id` -> `security_deposits.id` (NULLABLE, ON DELETE RESTRICT), `payment_transaction_id` -> `payment_transactions.id` (NULLABLE)
- **Important Columns:** `refund_type` (ENUM: `CANCELLATION_REFUND`, `DEPOSIT_RELEASE`, `DISPUTE_ADJUSTMENT`, `AUTO_REFUND_EXPIRED_HOLD`), `amount` (DECIMAL(10,2)), `currency` (CHAR(3) DEFAULT 'INR'), `status` (ENUM: `INITIATED`, `PROCESSING`, `SUCCEEDED`, `FAILED`, `RETRY_QUEUED`, `MANUAL_REVIEW`), `provider_refund_id` (VARCHAR 100 NULL), `idempotency_key` (VARCHAR 64), `requested_at` (DATETIME), `processed_at` (DATETIME NULL), `failure_reason` (VARCHAR 255 NULL).
- **Unique Constraints:** `uk_refunds_idempotency` (`idempotency_key`), `uk_refunds_provider_id` (`provider_refund_id`)
- **Important Indexes:** `idx_refunds_booking` (`booking_id`)

#### 39. `partner_commissions`
- **Responsibility:** Platform commission calculation per booking.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `booking_id` -> `bookings.id` (ON DELETE RESTRICT), `partner_id` -> `partner_profiles.id` (ON DELETE RESTRICT)
- **Important Columns:** `gross_rental_amount` (DECIMAL(10,2)), `commission_rate_percentage` (DECIMAL(5,2)), `platform_commission_amount` (DECIMAL(10,2)), `tax_on_commission` (DECIMAL(10,2)), `net_partner_entitlement` (DECIMAL(10,2)), `status` (ENUM: `CALCULATED`, `RECONCILED`), `created_at`.
- **Unique Constraints:** `uk_partner_commissions_booking` (`booking_id`)

#### 40. `contributor_payables`
- **Responsibility:** Contractual payable owed by the rental partner to the contributor. Platform records payable, partner enters UTR, contributor confirms/disputes.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `booking_id` -> `bookings.id` (ON DELETE RESTRICT), `agreement_id` -> `contribution_agreements.id` (ON DELETE RESTRICT), `contributor_user_id` -> `users.id` (ON DELETE RESTRICT), `partner_id` -> `partner_profiles.id` (ON DELETE RESTRICT)
- **Important Columns:** `fixed_amount` (DECIMAL(10,2)), `currency` (CHAR(3) DEFAULT 'INR'), `payable_status` (ENUM: `PENDING_RETURN`, `ELIGIBLE`, `SCHEDULED`, `PROCESSING`, `SETTLED`, `CANCELLED`, `DISPUTED`), `settlement_mode` (ENUM: `DIRECT_PARTNER_SETTLED`, `PLATFORM_FACILITATED`), `bank_transaction_reference` (VARCHAR 100 NULL), `settled_at` (DATETIME NULL), `contributor_acknowledged_at` (DATETIME NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_contrib_payables_booking` (`booking_id`, `agreement_id`)
- **Important Indexes:** `idx_contrib_payables_contributor` (`contributor_user_id`, `payable_status`), `idx_contrib_payables_partner` (`partner_id`, `payable_status`)

#### 41. `payout_methods`
- **Responsibility:** Tokenized disbursement recipient bank/UPI accounts.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `user_id` -> `users.id` (ON DELETE RESTRICT)
- **Important Columns:** `payee_role` (ENUM: `RENTAL_PARTNER`, `VEHICLE_CONTRIBUTOR`), `provider` (VARCHAR 30: `RAZORPAYX`), `provider_contact_id` (VARCHAR 100), `provider_fund_account_id` (VARCHAR 100), `account_holder_name` (VARCHAR 150), `account_number_last4` (CHAR(4)), `ifsc_code_encrypted` (VARCHAR 255), `upi_id_masked` (VARCHAR 100 NULL), `verification_status` (ENUM: `UNVERIFIED`, `VERIFIED`, `REJECTED`), `is_primary` (BOOLEAN DEFAULT FALSE), `created_at`, `updated_at`.
- **Important Indexes:** `idx_payout_methods_user` (`user_id`)

#### 42. `settlements`
- **Responsibility:** Outward partner disbursement batches and payout execution records.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `payee_user_id` -> `users.id` (ON DELETE RESTRICT), `payout_method_id` -> `payout_methods.id` (NULLABLE, ON DELETE RESTRICT)
- **Important Columns:** `payee_type` (ENUM: `RENTAL_PARTNER`, `VEHICLE_CONTRIBUTOR`), `settlement_type` (ENUM: `PARTNER_RENTAL_PROCEEDS`, `CONTRIBUTOR_FIXED_FEE`), `amount` (DECIMAL(10,2)), `currency` (CHAR(3) DEFAULT 'INR'), `status` (ENUM: `INITIATED`, `PROCESSING`, `COMPLETED`, `FAILED`, `REVERSED`), `provider_payout_id` (VARCHAR 100 NULL), `failure_reason` (VARCHAR 255 NULL), `initiated_at` (DATETIME), `completed_at` (DATETIME NULL), `created_at`.
- **Important Indexes:** `idx_settlements_payee` (`payee_user_id`, `status`)

#### 43. `ledger_entries`
- **Responsibility:** Append-only double-entry financial journal. Balancing debits and credits grouped by unique transaction group ID.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** None (references audited entities via typed reference columns)
- **Important Columns:** `transaction_group_id` (VARCHAR 64: groups balancing entries), `sequence_number` (SMALLINT UNSIGNED), `account_code` (ENUM: `GATEWAY_CLEARING`, `CUSTOMER_REFUNDABLE_LIABILITY`, `CUSTOMER_RENTAL_CLEARING`, `PLATFORM_COMMISSION_REVENUE`, `PARTNER_PAYABLE`, `CONTRIBUTOR_PAYABLE`, `REFUND_PAYABLE`, `SETTLEMENT_CLEARING`), `debit_amount` (DECIMAL(12,2) DEFAULT 0.00), `credit_amount` (DECIMAL(12,2) DEFAULT 0.00), `currency` (CHAR(3) DEFAULT 'INR'), `reference_type` (VARCHAR 30: `BOOKING`, `PAYMENT`, `DEPOSIT`, `REFUND`, `SETTLEMENT`, `ADJUSTMENT`), `reference_id` (BIGINT UNSIGNED), `narrative` (VARCHAR 255), `created_at` (DATETIME).
- **Important Indexes:** `idx_ledger_group` (`transaction_group_id`), `idx_ledger_account_created` (`account_code`, `created_at`), `idx_ledger_reference` (`reference_type`, `reference_id`)

#### 44. `platform_commission_rules`
- **Responsibility:** Configurable rules for platform commission percentages and minimum fees.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `city_id` -> `cities.id` (NULLABLE)
- **Important Columns:** `vehicle_category` (ENUM: `CAR`, `BIKE`, `SCOOTER`), `partner_tier` (VARCHAR 30 NULL), `commission_percentage` (DECIMAL(5,2)), `fixed_fee` (DECIMAL(10,2) DEFAULT 0.00), `min_commission` (DECIMAL(10,2) DEFAULT 0.00), `effective_from` (DATETIME), `effective_to` (DATETIME NULL), `is_active` (BOOLEAN DEFAULT TRUE), `created_at`.
- **Important Indexes:** `idx_comm_rules_eval` (`vehicle_category`, `is_active`, `effective_from`)

---

### Module 8: Transactional Outbox, Support, Reviews & Auditing (6 Tables)

#### 45. `outbox_events`
- **Responsibility:** Transactional outbox guaranteeing resilient, idempotent delivery of external side-effects (payment gateway calls, notifications).
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** None
- **Important Columns:** `aggregate_type` (VARCHAR 50), `aggregate_id` (BIGINT UNSIGNED), `event_type` (VARCHAR 50), `payload_json` (JSON), `status` (ENUM: `PENDING`, `PROCESSING`, `PROCESSED`, `FAILED`, `DEAD_LETTER`), `retry_count` (TINYINT UNSIGNED DEFAULT 0), `next_retry_at` (DATETIME NULL), `failure_reason` (TEXT NULL), `created_at`, `processed_at` (DATETIME NULL).
- **Important Indexes:** `idx_outbox_polling` (`status`, `next_retry_at`)

#### 46. `reviews`
- **Responsibility:** Customer ratings and reviews bound to completed bookings.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `booking_id` -> `bookings.id` (ON DELETE RESTRICT), `reviewer_user_id` -> `users.id` (ON DELETE RESTRICT), `vehicle_id` -> `vehicles.id` (ON DELETE RESTRICT), `partner_id` -> `partner_profiles.id` (ON DELETE RESTRICT)
- **Important Columns:** `rating_score` (TINYINT UNSIGNED: 1-5), `rating_cleanliness` (TINYINT UNSIGNED: 1-5), `rating_maintenance` (TINYINT UNSIGNED: 1-5), `review_text` (TEXT NULL), `moderation_status` (ENUM: `PUBLISHED`, `FLAGGED`, `REJECTED`), `created_at`.
- **Unique Constraints:** `uk_reviews_booking` (`booking_id`)
- **Important Indexes:** `idx_reviews_vehicle` (`vehicle_id`, `moderation_status`)

#### 47. `support_tickets`
- **Responsibility:** Customer, partner, and contributor support and dispute tickets. Standardized physical table.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `user_id` -> `users.id` (ON DELETE RESTRICT), `booking_id` -> `bookings.id` (NULLABLE, ON DELETE SET NULL), `vehicle_id` -> `vehicles.id` (NULLABLE, ON DELETE SET NULL), `assigned_admin_id` -> `users.id` (NULLABLE)
- **Important Columns:** `ticket_number` (VARCHAR 32), `category` (ENUM: `BOOKING`, `PAYMENT`, `DAMAGE`, `CUSTODY`, `AGREEMENT`, `GENERAL`), `priority` (ENUM: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), `status` (ENUM: `OPEN`, `ASSIGNED`, `IN_PROGRESS`, `WAITING_FOR_USER`, `RESOLVED`, `CLOSED`), `subject` (VARCHAR 200), `resolved_at` (DATETIME NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_support_tickets_number` (`ticket_number`)
- **Important Indexes:** `idx_tickets_user_status` (`user_id`, `status`)

#### 48. `support_ticket_messages`
- **Responsibility:** Threaded discussion messages inside support tickets. Standardized separate physical table.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `ticket_id` -> `support_tickets.id` (ON DELETE CASCADE), `sender_user_id` -> `users.id` (ON DELETE RESTRICT)
- **Important Columns:** `message_body` (TEXT), `attachment_object_key` (VARCHAR 255 NULL), `is_internal_note` (BOOLEAN DEFAULT FALSE), `sent_at` (DATETIME).
- **Important Indexes:** `idx_ticket_messages_ticket` (`ticket_id`, `sent_at`)

#### 49. `notifications`
- **Responsibility:** In-app, SMS, and email notification delivery tracking.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `user_id` -> `users.id` (ON DELETE CASCADE)
- **Important Columns:** `channel` (ENUM: `IN_APP`, `EMAIL`, `SMS`), `template_code` (VARCHAR 50), `title` (VARCHAR 200), `message_content` (TEXT), `payload_json` (JSON NULL), `delivery_status` (ENUM: `PENDING`, `SENT`, `FAILED`, `READ`), `read_at` (DATETIME NULL), `created_at`.
- **Important Indexes:** `idx_notifications_user_status` (`user_id`, `delivery_status`, `created_at`)

#### 50. `audit_logs`
- **Responsibility:** Immutable security, authorization, and administrative action audit log.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `actor_user_id` -> `users.id` (NULLABLE)
- **Important Columns:** `action` (VARCHAR 100), `entity_type` (VARCHAR 50), `entity_id` (BIGINT UNSIGNED), `ip_address` (VARCHAR 45 NULL), `correlation_id` (VARCHAR 64 NULL), `metadata_json_redacted` (JSON NULL), `occurred_at` (DATETIME).
- **Important Indexes:** `idx_audit_logs_actor` (`actor_user_id`, `occurred_at`), `idx_audit_logs_entity` (`entity_type`, `entity_id`)

---

### Module 9: Infrastructure-Managed Tables (2 Tables)

#### 51. `shedlock`
- **Responsibility:** Distributed task lock table managed directly by the ShedLock Spring library.
- **Primary Key:** `name` (VARCHAR 64)
- **Important Columns:** `lock_until` (TIMESTAMP(3)), `locked_at` (TIMESTAMP(3)), `locked_by` (VARCHAR 255).

#### 52. `flyway_schema_history`
- **Responsibility:** Database migration audit table managed directly by Flyway.
- **Primary Key:** `installed_rank` (INT)
- **Important Columns:** `version` (VARCHAR 50), `description` (VARCHAR 200), `type` (VARCHAR 20), `script` (VARCHAR 1000), `checksum` (INT), `installed_by` (VARCHAR 100), `installed_on` (TIMESTAMP), `execution_time` (INT), `success` (BOOLEAN).
