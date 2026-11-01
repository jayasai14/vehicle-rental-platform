# Final Physical Database Entity Inventory (Freeze Baseline)

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh (Expandable)  
**Status:** Database Design Freeze Candidate Baseline  
**Total Proposed MVP Tables:** **47 Physical Tables** (45 Application-Domain Tables + 2 Infrastructure-Managed Tables) + **8 Future-Phase Tables** (55 Total Cataloged Entities)  

> [!IMPORTANT]
> **Pre-Implementation Notice:** All tables enumerated below represent the **proposed physical database design** to be created via Flyway migrations in MySQL 8.0+. **No migrations, JPA entities, or actual database schemas have been generated yet.** Application code and Flyway DDL scripts will only be authored after formal Product Owner database design freeze sign-off.

---

## 1. Physical Table Count Reconciliation (Partner-Direct Payment Model)

Following the adoption of the **Partner-Direct Payment Collection** model (ADR-018), the platform handles zero customer funds, no platform general ledger accounts are active, and gateway clearing tables are deferred. The physical database inventory is cleanly categorized into **45 MVP Application-Domain Tables**, **2 Infrastructure-Managed Tables**, and **8 Future-Phase Marketplace Gateway Tables**:

```
┌─────────────────────────────────────────────────────────────┬───────────────┐
│ Functional Domain / Module                                  │ Proposed Count│
├─────────────────────────────────────────────────────────────┼───────────────┤
│ 1. Identity, Access, KYC & Addresses                        │ 11 tables     │
│ 2. Operating Locations, Partner Hubs & Logistics            │ 3 tables      │
│ 3. Vehicle Inventory, Compliance & Pricing (incl. docs)     │ 6 tables      │
│ 4. Generic Vehicle Custody Tracking                         │ 1 table       │
│ 5. Contributor Agreements & Fleet Proposals                 │ 4 tables      │
│ 6. Bookings, Holds, Extensions, Inspections & Incidents     │ 7 tables      │
│ 7. Partner Payment Profiles, Demands, Submissions & Deposits│ 7 tables      │
│ 8. Transactional Outbox, Support, Reviews & Auditing        │ 6 tables      │
├─────────────────────────────────────────────────────────────┼───────────────┤
│ Total Proposed Application Domain Tables                    │ 45 tables     │
│ 9. Infrastructure-Managed Tables (ShedLock & Flyway)        │ 2 tables      │
├─────────────────────────────────────────────────────────────┼───────────────┤
│ TOTAL PROPOSED PHYSICAL MYSQL TABLES FOR MVP                │ 47 tables     │
├─────────────────────────────────────────────────────────────┼───────────────┤
│ Future-Phase Marketplace Gateway & Double-Entry Tables      │ 8 tables      │
├─────────────────────────────────────────────────────────────┼───────────────┤
│ TOTAL CATALOGED DATABASE ENTITIES                           │ 55 tables     │
└─────────────────────────────────────────────────────────────┴───────────────┘
```

---

## 2. Table-by-Table Inventory (All 47 MVP Physical Tables Individually Listed)

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
- **Foreign Keys:** `user_id` $\rightarrow$ `users.id` (`ON DELETE CASCADE`), `role_id` $\rightarrow$ `roles.id` (`ON DELETE RESTRICT`), `granted_by` $\rightarrow$ `users.id` (NULLABLE)
- **Important Columns:** `granted_at` (DATETIME).
- **Unique Constraints:** `uk_user_roles_pair` (`user_id`, `role_id`)

#### 4. `refresh_tokens`
- **Responsibility:** Cryptographically hashed refresh tokens supporting rotation and revocation.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `user_id` $\rightarrow$ `users.id` (`ON DELETE CASCADE`)
- **Important Columns:** `token_hash` (VARCHAR 64 SHA-256), `device_info` (VARCHAR 255), `ip_address` (VARCHAR 45), `expires_at` (DATETIME), `revoked_at` (DATETIME NULL), `created_at`.
- **Unique Constraints:** `uk_refresh_tokens_hash` (`token_hash`)
- **Important Indexes:** `idx_refresh_tokens_user_expires` (`user_id`, `expires_at`)

#### 5. `addresses`
- **Responsibility:** Unified geographic address repository for users, hubs, and pickup/dropoff points.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `city_id` $\rightarrow$ `cities.id` (`ON DELETE RESTRICT`), `user_id` $\rightarrow$ `users.id` (NULLABLE, `ON DELETE CASCADE`)
- **Important Columns:** `address_line1` (VARCHAR 255), `address_line2` (VARCHAR 255 NULL), `landmark` (VARCHAR 100 NULL), `postal_code` (VARCHAR 10), `latitude` (DECIMAL(10,8) NULL), `longitude` (DECIMAL(11,8) NULL), `address_type` (ENUM: `CUSTOMER_HOME`, `PARTNER_HUB`, `PICKUP_POINT`, `DELIVERY_DESTINATION`), `created_at`, `updated_at`.
- **Important Indexes:** `idx_addresses_city` (`city_id`), `idx_addresses_lat_long` (`latitude`, `longitude`)

#### 6. `customer_profiles`
- **Responsibility:** Customer-specific verification and emergency contact data.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `user_id` $\rightarrow$ `users.id` (`ON DELETE CASCADE`), `address_id` $\rightarrow$ `addresses.id` (NULLABLE)
- **Important Columns:** `date_of_birth` (DATE NULL), `emergency_contact_name` (VARCHAR 100 NULL), `emergency_contact_phone` (VARCHAR 20 NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_customer_profiles_user` (`user_id`)

#### 7. `partner_profiles`
- **Responsibility:** Verified business profile for rental partner organizations.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`), `verified_by_user_id` $\rightarrow$ `users.id` (NULLABLE)
- **Important Columns:** `business_legal_name` (VARCHAR 150), `trade_name` (VARCHAR 150), `gstin_encrypted` (VARCHAR 255 NULL), `pan_encrypted` (VARCHAR 255 NULL), `contact_email` (VARCHAR 191), `contact_phone` (VARCHAR 20), `verification_status` (ENUM: `SUBMITTED`, `IN_REVIEW`, `VERIFIED`, `REJECTED`, `SUSPENDED`), `submitted_at` (DATETIME), `verified_at` (DATETIME NULL), `rejection_reason` (TEXT NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_partner_profiles_user` (`user_id`)
- **Important Indexes:** `idx_partner_verification_status` (`verification_status`)

#### 8. `partner_documents`
- **Responsibility:** Commercial verification documents (Trade licence, GSTIN, permits).
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `partner_id` $\rightarrow$ `partner_profiles.id` (`ON DELETE CASCADE`), `verified_by_user_id` $\rightarrow$ `users.id` (NULLABLE)
- **Important Columns:** `document_type` (ENUM: `TRADE_LICENCE`, `GSTIN_CERTIFICATE`, `COMMERCIAL_PERMIT`, `MSME_CERTIFICATE`, `OTHER`), `object_key` (VARCHAR 255), `document_number_encrypted` (VARCHAR 255 NULL), `verification_status` (ENUM: `PENDING`, `VERIFIED`, `REJECTED`), `verified_at` (DATETIME NULL), `created_at`.
- **Important Indexes:** `idx_partner_docs_partner` (`partner_id`)

#### 9. `contributor_profiles`
- **Responsibility:** Vehicle contributor profile and tax identification.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `user_id` $\rightarrow$ `users.id` (`ON DELETE CASCADE`), `verified_by_user_id` $\rightarrow$ `users.id` (NULLABLE)
- **Important Columns:** `pan_encrypted` (VARCHAR 255 NULL), `verification_status` (ENUM: `PENDING`, `VERIFIED`, `REJECTED`), `verified_at` (DATETIME NULL), `rejection_reason` (TEXT NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_contributor_profiles_user` (`user_id`)

#### 10. `identity_verifications`
- **Responsibility:** KYC verification logs (Aadhaar OKYC token, PAN check) without storing raw restricted ID numbers.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `user_id` $\rightarrow$ `users.id` (`ON DELETE CASCADE`)
- **Important Columns:** `verification_type` (ENUM: `AADHAAR_OKYC`, `PAN`, `PASSPORT`), `provider_name` (VARCHAR 50), `provider_reference` (VARCHAR 100), `verification_status` (ENUM: `PENDING`, `VERIFIED`, `FAILED`, `EXPIRED`), `verified_at` (DATETIME NULL), `expires_at` (DATETIME NULL), `created_at`.
- **Important Indexes:** `idx_id_verifications_user` (`user_id`)

#### 11. `driver_licences`
- **Responsibility:** Driving licence details for customer rental eligibility.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `user_id` $\rightarrow$ `users.id` (`ON DELETE CASCADE`), `verified_by_user_id` $\rightarrow$ `users.id` (NULLABLE)
- **Important Columns:** `licence_number_encrypted` (VARCHAR 255), `issuing_state` (VARCHAR 50), `vehicle_classes` (VARCHAR 100), `valid_from` (DATE), `valid_until` (DATE), `verification_status` (ENUM: `PENDING`, `VERIFIED`, `REJECTED`, `EXPIRED`), `provider_reference` (VARCHAR 100 NULL), `verified_at` (DATETIME NULL), `created_at`.
- **Unique Constraints:** `uk_driver_licences_user` (`user_id`)
- **Important Indexes:** `idx_driver_licences_validity` (`valid_until`, `verification_status`)

---

### Module 2: Operating Locations, Partner Hubs & Logistics (3 Tables)

#### 12. `cities`
- **Responsibility:** Operational municipalities (launching in Vijayawada).
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** None
- **Important Columns:** `name` (VARCHAR 100), `state` (VARCHAR 100), `country` (VARCHAR 50 DEFAULT 'India'), `is_active` (BOOLEAN DEFAULT TRUE), `created_at`.
- **Unique Constraints:** `uk_cities_name_state` (`name`, `state`)

#### 13. `partner_locations`
- **Responsibility:** Verified physical depots, garages, and hubs operated by rental partners.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `partner_id` $\rightarrow$ `partner_profiles.id` (`ON DELETE RESTRICT`), `address_id` $\rightarrow$ `addresses.id` (`ON DELETE RESTRICT`)
- **Important Columns:** `name` (VARCHAR 100), `contact_person` (VARCHAR 100), `phone` (VARCHAR 20), `operating_hours_json` (JSON), `is_active` (BOOLEAN DEFAULT TRUE), `created_at`, `updated_at`.
- **Important Indexes:** `idx_partner_locations_partner` (`partner_id`)

#### 14. `delivery_zones`
- **Responsibility:** Doorstep delivery service radius and tiered pricing.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `partner_id` $\rightarrow$ `partner_profiles.id` (`ON DELETE CASCADE`)
- **Important Columns:** `zone_name` (VARCHAR 100), `max_radius_km` (DECIMAL(5,2)), `base_delivery_fee` (DECIMAL(10,2)), `extra_per_km_fee` (DECIMAL(10,2)), `is_active` (BOOLEAN DEFAULT TRUE), `created_at`.
- **Important Indexes:** `idx_delivery_zones_partner` (`partner_id`)

---

### Module 3: Vehicle Inventory, Compliance & Pricing (6 Tables)

#### 15. `vehicles`
- **Responsibility:** Core vehicle identity, supporting both individual owners and partner business owners, and separating operational management.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:**
  - `legal_owner_user_id` $\rightarrow$ `users.id` (NULLABLE, `ON DELETE RESTRICT` - for individual person owners)
  - `legal_owner_partner_id` $\rightarrow$ `partner_profiles.id` (NULLABLE, `ON DELETE RESTRICT` - for partner business legal owners)
  - `manager_partner_id` $\rightarrow$ `partner_profiles.id` (`ON DELETE RESTRICT` - operational rental manager)
  - `city_id` $\rightarrow$ `cities.id` (`ON DELETE RESTRICT`)
  - `primary_hub_location_id` $\rightarrow$ `partner_locations.id` (NULLABLE, `ON DELETE RESTRICT`)
- **Important Columns:** `ownership_type` (ENUM: `INDIVIDUAL_OWNER`, `PARTNER_BUSINESS_OWNER`), `inventory_source` (ENUM: `PARTNER_OWNED`, `CONTRIBUTOR_OWNED`), `category` (ENUM: `CAR`, `BIKE`, `SCOOTER`), `make` (VARCHAR 50), `model` (VARCHAR 50), `variant` (VARCHAR 50 NULL), `manufacture_year` (SMALLINT UNSIGNED), `registration_number_encrypted` (VARCHAR 255), `fuel_type` (ENUM: `PETROL`, `DIESEL`, `ELECTRIC`, `CNG`), `transmission` (ENUM: `MANUAL`, `AUTOMATIC`), `seating_capacity` (TINYINT UNSIGNED), `current_odometer_km` (INT UNSIGNED DEFAULT 0), `turnaround_buffer_minutes` (SMALLINT UNSIGNED DEFAULT 60), `current_custody_type` (ENUM: `CONTRIBUTOR`, `PARTNER`, `CUSTOMER`, `LOGISTICS_AGENT`), `current_custodian_id` (BIGINT UNSIGNED), `listing_status` (ENUM: `DRAFT`, `PENDING_VERIFICATION`, `PUBLISHED`, `PAUSED`, `ARCHIVED`), `verification_status` (ENUM: `UNVERIFIED`, `VERIFIED`, `REJECTED`), `created_at`, `updated_at`.
- **Check Constraints:** `chk_vehicle_ownership` (`(ownership_type = 'INDIVIDUAL_OWNER' AND legal_owner_user_id IS NOT NULL AND legal_owner_partner_id IS NULL) OR (ownership_type = 'PARTNER_BUSINESS_OWNER' AND legal_owner_partner_id IS NOT NULL)`).
- **Unique Constraints:** `uk_vehicles_reg_encrypted` (`registration_number_encrypted`)
- **Important Indexes:** `idx_vehicles_search` (`category`, `city_id`, `listing_status`, `verification_status`), `idx_vehicles_manager` (`manager_partner_id`)

#### 16. `vehicle_documents`
- **Responsibility:** Vehicular compliance documents (RC, Insurance, Commercial Permit, PUC).
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` $\rightarrow$ `vehicles.id` (`ON DELETE CASCADE`), `verified_by_user_id` $\rightarrow$ `users.id` (NULLABLE)
- **Important Columns:** `document_type` (ENUM: `RC`, `INSURANCE_POLICY`, `COMMERCIAL_PERMIT`, `PUC`, `FITNESS_CERT`), `object_key` (VARCHAR 255), `document_number_encrypted` (VARCHAR 255 NULL), `issue_date` (DATE NULL), `expiry_date` (DATE NULL), `verification_status` (ENUM: `PENDING`, `VERIFIED`, `REJECTED`), `verified_at` (DATETIME NULL), `created_at`.
- **Important Indexes:** `idx_vehicle_docs_vehicle` (`vehicle_id`), `idx_vehicle_docs_expiry` (`expiry_date`)

#### 17. `vehicle_images`
- **Responsibility:** Vehicle photo assets with ordered display types.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` $\rightarrow$ `vehicles.id` (`ON DELETE CASCADE`)
- **Important Columns:** `object_key` (VARCHAR 255), `image_type` (ENUM: `EXTERIOR_FRONT`, `EXTERIOR_REAR`, `EXTERIOR_SIDE`, `INTERIOR`, `DASHBOARD`, `FEATURE`), `display_order` (SMALLINT UNSIGNED DEFAULT 0), `is_primary` (BOOLEAN DEFAULT FALSE), `created_at`.
- **Important Indexes:** `idx_vehicle_images_vehicle` (`vehicle_id`, `display_order`)

#### 18. `vehicle_pricing`
- **Responsibility:** Rental price schedule, deposit amounts, and minimum durations.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` $\rightarrow$ `vehicles.id` (`ON DELETE CASCADE`), `created_by_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`)
- **Important Columns:** `hourly_rate` (DECIMAL(10,2)), `daily_rate` (DECIMAL(10,2)), `security_deposit` (DECIMAL(10,2)), `extra_hour_rate` (DECIMAL(10,2)), `late_fee_hourly_rate` (DECIMAL(10,2)), `min_booking_hours` (SMALLINT UNSIGNED DEFAULT 4), `effective_from` (DATETIME), `effective_to` (DATETIME NULL), `created_at`.
- **Important Indexes:** `idx_vehicle_pricing_lookup` (`vehicle_id`, `effective_from`, `effective_to`)

#### 19. `pickup_locations`
- **Responsibility:** Configured pickup arrangements per vehicle (hub pickup, owner direct handover, or doorstep delivery).
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` $\rightarrow$ `vehicles.id` (`ON DELETE CASCADE`), `partner_location_id` $\rightarrow$ `partner_locations.id` (NULLABLE), `address_id` $\rightarrow$ `addresses.id` (NULLABLE)
- **Important Columns:** `location_type` (ENUM: `PARTNER_HUB`, `CONTRIBUTOR_LOCATION`, `DOORSTEP_DELIVERY`), `is_default` (BOOLEAN DEFAULT FALSE), `created_at`.
- **Important Indexes:** `idx_pickup_locations_vehicle` (`vehicle_id`)

#### 20. `vehicle_availability_blocks`
- **Responsibility:** Calendar blackout blocks (maintenance, owner personal use, legal holds).
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` $\rightarrow$ `vehicles.id` (`ON DELETE CASCADE`), `created_by_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`)
- **Important Columns:** `starts_at` (DATETIME), `ends_at` (DATETIME), `block_type` (ENUM: `MAINTENANCE`, `OWNER_USE`, `ACCIDENT_REPAIR`, `LEGAL_HOLD`, `BUFFER`), `reason` (VARCHAR 255 NULL), `status` (ENUM: `ACTIVE`, `CANCELLED`), `created_at`.
- **Important Indexes:** `idx_avail_blocks_interval` (`vehicle_id`, `status`, `starts_at`, `ends_at`)

---

### Module 4: Generic Vehicle Custody Tracking (1 Table)

#### 21. `vehicle_custody_events`
- **Responsibility:** Auditable physical custody event ledger supporting all 8 handover permutations. Eliminates unvalidated polymorphic foreign keys by using explicit typed references.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:**
  - `vehicle_id` $\rightarrow$ `vehicles.id` (`ON DELETE RESTRICT`)
  - `booking_id` $\rightarrow$ `bookings.id` (NULLABLE, `ON DELETE RESTRICT`)
  - `inspection_id` $\rightarrow$ `inspections.id` (NULLABLE, `ON DELETE RESTRICT`)
  - `handover_address_id` $\rightarrow` `addresses.id` (`ON DELETE RESTRICT` - exact physical location)
  - `actor_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT` - authenticated actor performing the transfer)
  - `authorized_rep_user_id` $\rightarrow$ `users.id` (NULLABLE, `ON DELETE RESTRICT` - logistics agent or authorized witness)
  - `from_custodian_user_id` $\rightarrow$ `users.id` (NULLABLE, `ON DELETE RESTRICT` - when previous custodian was Contributor, Customer, or Agent)
  - `from_partner_location_id` $\rightarrow$ `partner_locations.id` (NULLABLE, `ON DELETE RESTRICT` - when previous custodian was Partner Hub)
  - `to_custodian_user_id` $\rightarrow$ `users.id` (NULLABLE, `ON DELETE RESTRICT` - when receiving custodian is Contributor, Customer, or Agent)
  - `to_partner_location_id` $\rightarrow$ `partner_locations.id` (NULLABLE, `ON DELETE RESTRICT` - when receiving custodian is Partner Hub)
- **Important Columns:** `event_type` (ENUM: `CONTRIBUTOR_TO_PARTNER`, `PARTNER_TO_CUSTOMER`, `CONTRIBUTOR_TO_CUSTOMER`, `CUSTOMER_TO_PARTNER`, `CUSTOMER_TO_CONTRIBUTOR`, `PARTNER_TO_CONTRIBUTOR`, `INTER_HUB_TRANSFER`, `LOGISTICS_RELOCATION`), `odometer_km` (INT UNSIGNED), `fuel_or_battery_level` (TINYINT UNSIGNED: 0-100), `notes` (TEXT NULL), `transferred_at` (DATETIME), `acknowledged_by_recipient_at` (DATETIME NULL), `created_at`.
- **Check Constraints:** `chk_custody_from` (`from_custodian_user_id IS NOT NULL OR from_partner_location_id IS NOT NULL`), `chk_custody_to` (`to_custodian_user_id IS NOT NULL OR to_partner_location_id IS NOT NULL`).
- **Important Indexes:** `idx_custody_vehicle_time` (`vehicle_id`, `transferred_at`), `idx_custody_booking` (`booking_id`)

---

### Module 5: Contributor Agreements & Fleet Proposals (4 Tables)

#### 22. `vehicle_contribution_applications`
- **Responsibility:** Application submissions by vehicle owners to rental partners.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` $\rightarrow$ `vehicles.id` (`ON DELETE RESTRICT`), `contributor_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`), `preferred_partner_id` $\rightarrow$ `partner_profiles.id` (NULLABLE, `ON DELETE RESTRICT`)
- **Important Columns:** `application_status` (ENUM: `SUBMITTED`, `UNDER_REVIEW`, `PROPOSAL_MADE`, `ACCEPTED`, `REJECTED`, `WITHDRAWN`), `submitted_at` (DATETIME), `reviewed_at` (DATETIME NULL), `rejection_reason` (TEXT NULL), `created_at`.
- **Important Indexes:** `idx_contrib_app_status` (`contributor_user_id`, `application_status`)

#### 23. `contribution_agreements`
- **Responsibility:** Legally binding commercial agreements defining fixed payment per booking, notice periods, and terms.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` $\rightarrow$ `vehicles.id` (`ON DELETE RESTRICT`), `contributor_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`), `partner_id` $\rightarrow$ `partner_profiles.id` (`ON DELETE RESTRICT`)
- **Important Columns:** `agreement_number` (VARCHAR 50), `agreement_status` (ENUM: `DRAFT`, `PROPOSED`, `ACCEPTED`, `ACTIVE`, `SUSPENDED`, `TERMINATION_REQUESTED`, `TERMINATED`, `EXPIRED`), `version` (INT UNSIGNED DEFAULT 1), `start_at` (DATETIME), `end_at` (DATETIME), `fixed_amount_per_booking` (DECIMAL(10,2)), `currency` (CHAR(3) DEFAULT 'INR'), `cancellation_compensation_rate` (DECIMAL(5,2) NULL), `payout_due_hours` (INT UNSIGNED DEFAULT 48), `terms_document_object_key` (VARCHAR 255 NULL), `termination_notice_days` (TINYINT UNSIGNED DEFAULT 7), `accepted_by_contributor_at` (DATETIME NULL), `accepted_by_partner_at` (DATETIME NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_agreements_number` (`agreement_number`)
- **Important Indexes:** `idx_agreements_vehicle_status` (`vehicle_id`, `agreement_status`)

#### 24. `contribution_agreement_events`
- **Responsibility:** Immutable audit trail for agreement negotiations, proposals, suspensions, and terminations.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `agreement_id` $\rightarrow$ `contribution_agreements.id` (`ON DELETE CASCADE`), `actor_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`)
- **Important Columns:** `from_status` (VARCHAR 30 NULL), `to_status` (VARCHAR 30), `event_type` (VARCHAR 50), `details_json` (JSON NULL), `occurred_at` (DATETIME).
- **Important Indexes:** `idx_agreement_events_agreement` (`agreement_id`)

#### 25. `vehicle_withdrawal_requests`
- **Responsibility:** Vehicle reclamation requests initiated by contributors (confirmed bookings strictly supersede).
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` $\rightarrow$ `vehicles.id` (`ON DELETE RESTRICT`), `agreement_id` $\rightarrow$ `contribution_agreements.id` (`ON DELETE RESTRICT`), `requested_by_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`)
- **Important Columns:** `requested_at` (DATETIME), `effective_withdrawal_date` (DATETIME), `status` (ENUM: `SUBMITTED`, `ACKNOWLEDGED`, `SETTLING_BOOKINGS`, `READY_FOR_HANDOVER`, `COMPLETED`, `REJECTED`), `decision_reason` (TEXT NULL), `resolved_at` (DATETIME NULL), `created_at`.
- **Important Indexes:** `idx_withdrawal_vehicle_status` (`vehicle_id`, `status`)

---

### Module 6: Bookings, Holds, Extensions, Inspections & Incidents (7 Tables)

#### 26. `booking_holds`
- **Responsibility:** 15-minute temporary reservation locks acquired under pessimistic `SELECT FOR UPDATE` locking. Does NOT insert incomplete booking rows.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` $\rightarrow$ `vehicles.id` (`ON DELETE RESTRICT`), `customer_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`)
- **Important Columns:** `starts_at` (DATETIME), `ends_at` (DATETIME), `turnaround_buffer_minutes` (SMALLINT UNSIGNED), `expires_at` (DATETIME), `status` (ENUM: `ACTIVE`, `CONVERTED`, `EXPIRED`, `RELEASED`), `created_at`.
- **Important Indexes:** `idx_booking_holds_active` (`vehicle_id`, `status`, `expires_at`), `idx_booking_holds_customer` (`customer_user_id`, `status`)

#### 27. `bookings`
- **Responsibility:** Master booking contract created directly upon payment confirmation.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `vehicle_id` $\rightarrow$ `vehicles.id` (`ON DELETE RESTRICT`), `hold_id` $\rightarrow$ `booking_holds.id` (NULLABLE, `ON DELETE SET NULL`), `customer_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`), `manager_partner_id` $\rightarrow$ `partner_profiles.id` (`ON DELETE RESTRICT`), `pickup_location_id` $\rightarrow$ `pickup_locations.id` (`ON DELETE RESTRICT`), `return_location_id` $\rightarrow$ `pickup_locations.id` (`ON DELETE RESTRICT`), `cancellation_actor_user_id` $\rightarrow$ `users.id` (NULLABLE)
- **Important Columns:** `booking_reference` (VARCHAR 32), `starts_at` (DATETIME), `ends_at` (DATETIME), `turnaround_buffer_minutes` (SMALLINT UNSIGNED), `booking_status` (ENUM: `CONFIRMED`, `READY_FOR_PICKUP`, `IN_PROGRESS`, `OVERDUE`, `RETURNED`, `COMPLETED`, `CANCELLED`, `NO_SHOW`), `currency` (CHAR(3) DEFAULT 'INR'), `rental_subtotal` (DECIMAL(10,2)), `delivery_fee` (DECIMAL(10,2) DEFAULT 0.00), `platform_fee_snapshot` (DECIMAL(10,2)), `discount_total` (DECIMAL(10,2) DEFAULT 0.00), `partner_discount_subsidy` (DECIMAL(10,2) DEFAULT 0.00), `tax_total` (DECIMAL(10,2)), `total_rental_amount` (DECIMAL(10,2)), `cancellation_policy_snapshot` (JSON), `confirmed_at` (DATETIME), `cancelled_at` (DATETIME NULL), `cancellation_reason` (VARCHAR 255 NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_bookings_reference` (`booking_reference`)
- **Important Indexes:** `idx_bookings_schedule` (`vehicle_id`, `booking_status`, `starts_at`, `ends_at`), `idx_bookings_customer` (`customer_user_id`), `idx_bookings_manager` (`manager_partner_id`)

#### 28. `booking_status_events`
- **Responsibility:** Immutable audit history of booking operational status transitions.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `booking_id` $\rightarrow$ `bookings.id` (`ON DELETE CASCADE`), `actor_user_id` $\rightarrow$ `users.id` (NULLABLE)
- **Important Columns:** `from_status` (VARCHAR 30 NULL), `to_status` (VARCHAR 30), `reason` (VARCHAR 255 NULL), `occurred_at` (DATETIME).
- **Important Indexes:** `idx_booking_status_events_booking` (`booking_id`)

#### 29. `booking_extensions`
- **Responsibility:** Customer requests to extend ongoing rentals.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `booking_id` $\rightarrow$ `bookings.id` (`ON DELETE RESTRICT`), `decided_by_user_id` $\rightarrow$ `users.id` (NULLABLE)
- **Important Columns:** `requested_end_at` (DATETIME), `additional_rental_amount` (DECIMAL(10,2)), `payment_order_id` (BIGINT UNSIGNED NULL), `status` (ENUM: `PENDING_APPROVAL`, `PAYMENT_PENDING`, `APPROVED`, `REJECTED`, `EXPIRED`), `decided_at` (DATETIME NULL), `created_at`.
- **Important Indexes:** `idx_booking_extensions_booking` (`booking_id`)

#### 30. `inspections`
- **Responsibility:** Formal vehicle condition reports (pre-rental pickup and post-rental return).
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `booking_id` $\rightarrow$ `bookings.id` (NULLABLE, `ON DELETE RESTRICT`), `vehicle_id` $\rightarrow$ `vehicles.id` (`ON DELETE RESTRICT`), `performed_by_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`)
- **Important Columns:** `inspection_type` (ENUM: `PRE_RENTAL_PICKUP`, `POST_RENTAL_RETURN`, `INCIDENT`), `customer_acknowledged_at` (DATETIME NULL), `odometer_km` (INT UNSIGNED), `fuel_or_battery_level` (TINYINT UNSIGNED: 0-100), `condition_summary` (TEXT NULL), `created_at`.
- **Important Indexes:** `idx_inspections_booking` (`booking_id`), `idx_inspections_vehicle` (`vehicle_id`)

#### 31. `inspection_media`
- **Responsibility:** Photos and visual evidence captured during vehicle inspections.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `inspection_id` $\rightarrow$ `inspections.id` (`ON DELETE CASCADE`)
- **Important Columns:** `object_key` (VARCHAR 255), `media_type` (ENUM: `PHOTO_FRONT`, `PHOTO_REAR`, `PHOTO_LEFT`, `PHOTO_RIGHT`, `ODOMETER_PHOTO`, `FUEL_DASH_PHOTO`, `DAMAGE_DETAIL`), `caption` (VARCHAR 255 NULL), `captured_at` (DATETIME).
- **Important Indexes:** `idx_inspection_media_inspection` (`inspection_id`)

#### 32. `incidents`
- **Responsibility:** Damage, mechanical breakdown, traffic violation, or theft claim records.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `booking_id` $\rightarrow$ `bookings.id` (`ON DELETE RESTRICT`), `vehicle_id` $\rightarrow$ `vehicles.id` (`ON DELETE RESTRICT`), `reported_by_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`)
- **Important Columns:** `incident_type` (ENUM: `ACCIDENT`, `SCRATCH_DENT`, `MECHANICAL_BREAKDOWN`, `TRAFFIC_FINE`, `FUEL_DEFICIT`, `THEFT`), `description` (TEXT), `status` (ENUM: `REPORTED`, `UNDER_REVIEW`, `ESTIMATED`, `DISPUTED`, `SETTLED`, `REJECTED`), `estimated_repair_cost` (DECIMAL(10,2) NULL), `approved_deduction_amount` (DECIMAL(10,2) NULL), `reported_at` (DATETIME), `resolved_at` (DATETIME NULL), `resolution_notes` (TEXT NULL).
- **Important Indexes:** `idx_incidents_booking` (`booking_id`), `idx_incidents_vehicle` (`vehicle_id`)

---

### Module 7: Partner Payment Profiles, Demands, Submissions & Deposits (7 Tables)

#### 33. `partner_payment_profiles`
- **Responsibility:** Managing partner's direct payment receiving configuration (UPI QR image, UPI ID, external payment link, cash-at-pickup toggle). Does NOT store sensitive credentials (PINs, passwords, OTPs).
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `partner_id` $\rightarrow$ `partner_profiles.id` (`ON DELETE CASCADE`)
- **Important Columns:** `payee_display_name` (VARCHAR 150), `upi_id` (VARCHAR 100), `upi_qr_object_key` (VARCHAR 255 NULL), `external_payment_url` (VARCHAR 255 NULL), `is_pay_at_pickup_enabled` (BOOLEAN DEFAULT FALSE), `accepted_methods_json` (JSON), `payment_instructions_note` (TEXT NULL), `is_active` (BOOLEAN DEFAULT TRUE), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_partner_pay_profile_partner` (`partner_id`)
- **Important Indexes:** `idx_partner_pay_profile_active` (`partner_id`, `is_active`)

#### 34. `rental_payment_demands`
- **Responsibility:** Explicit payment obligations demanded for an active booking hold, booking extension, or approved damage claim. Replaces platform-clearing payment orders.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `hold_id` $\rightarrow$ `booking_holds.id` (NULLABLE, `ON DELETE SET NULL`), `booking_id` $\rightarrow$ `bookings.id` (NULLABLE, `ON DELETE RESTRICT`), `manager_partner_id` $\rightarrow$ `partner_profiles.id` (`ON DELETE RESTRICT`), `customer_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`)
- **Important Columns:** `demand_reference` (VARCHAR 32), `demand_type` (ENUM: `INITIAL_RENTAL`, `EXTENSION`, `DAMAGE_INVOICE`), `amount_demanded` (DECIMAL(10,2)), `security_deposit_demanded` (DECIMAL(10,2) DEFAULT 0.00), `currency` (CHAR(3) DEFAULT 'INR'), `status` (ENUM: `ISSUED`, `PAYMENT_SUBMITTED`, `VERIFIED`, `EXPIRED`, `CANCELLED`), `expires_at` (DATETIME), `verified_at` (DATETIME NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_payment_demands_reference` (`demand_reference`)
- **Important Indexes:** `idx_demands_hold` (`hold_id`), `idx_demands_booking` (`booking_id`), `idx_demands_partner_status` (`manager_partner_id`, `status`)

#### 35. `rental_payment_submissions`
- **Responsibility:** Customer-submitted evidence of direct payment to partner. Customer upload NEVER auto-verifies payment. Only explicit partner verification transitions status.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `demand_id` $\rightarrow$ `rental_payment_demands.id` (`ON DELETE RESTRICT`), `hold_id` $\rightarrow$ `booking_holds.id` (NULLABLE, `ON DELETE SET NULL`), `booking_id` $\rightarrow$ `bookings.id` (NULLABLE, `ON DELETE SET NULL`), `customer_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`), `manager_partner_id` $\rightarrow$ `partner_profiles.id` (`ON DELETE RESTRICT`), `verified_by_user_id` $\rightarrow$ `users.id` (NULLABLE)
- **Important Columns:** `payment_method` (ENUM: `UPI_QR`, `UPI_ID`, `EXTERNAL_LINK`, `CASH_AT_PICKUP`), `amount_submitted` (DECIMAL(10,2)), `currency` (CHAR(3) DEFAULT 'INR'), `transaction_reference` (VARCHAR 100: UTR/Transaction ID/Cash Receipt), `transaction_timestamp` (DATETIME), `receipt_object_key` (VARCHAR 255 NULL), `status` (ENUM: `SUBMITTED_FOR_VERIFICATION`, `VERIFIED`, `REJECTED`), `verified_at` (DATETIME NULL), `rejection_reason` (TEXT NULL), `notes` (TEXT NULL), `created_at`.
- **Unique Constraints:** `uk_submissions_partner_ref` (`manager_partner_id`, `transaction_reference`)
- **Important Indexes:** `idx_submissions_demand` (`demand_id`), `idx_submissions_partner_status` (`manager_partner_id`, `status`)

#### 36. `security_deposits`
- **Responsibility:** Explicit tracking of customer security deposits collected, held, and refunded directly by the rental partner.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `booking_id` $\rightarrow$ `bookings.id` (`ON DELETE RESTRICT`), `manager_partner_id` $\rightarrow$ `partner_profiles.id` (`ON DELETE RESTRICT`), `customer_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`)
- **Important Columns:** `required_amount` (DECIMAL(10,2)), `collected_amount` (DECIMAL(10,2) DEFAULT 0.00), `collection_method` (ENUM: `UPI_QR`, `UPI_ID`, `EXTERNAL_LINK`, `CASH_AT_PICKUP` NULL), `collection_reference` (VARCHAR 100 NULL: UTR Number), `collection_evidence_object_key` (VARCHAR 255 NULL), `total_deducted_amount` (DECIMAL(10,2) DEFAULT 0.00), `total_refunded_amount` (DECIMAL(10,2) DEFAULT 0.00), `status` (ENUM: `PENDING_COLLECTION`, `COLLECTED_HELD_BY_PARTNER`, `DEDUCTION_PROPOSED`, `DISPUTED`, `PARTIALLY_REFUNDED`, `FULLY_REFUNDED`, `RETAINED_FOR_DAMAGE`, `CLOSED`), `collected_at` (DATETIME NULL), `closed_at` (DATETIME NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_security_deposits_booking` (`booking_id`)
- **Important Indexes:** `idx_security_deposits_partner_status` (`manager_partner_id`, `status`)

#### 37. `deposit_deductions`
- **Responsibility:** Granular damage, fuel, cleaning, or challan deduction claims proposed by partner against deposit. Never auto-approved upon 48h timeout; requires customer agreement or Admin resolution.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `security_deposit_id` $\rightarrow$ `security_deposits.id` (`ON DELETE RESTRICT`), `incident_id` $\rightarrow$ `incidents.id` (NULLABLE, `ON DELETE RESTRICT`), `proposed_by_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`), `resolved_by_admin_id` $\rightarrow$ `users.id` (NULLABLE)
- **Important Columns:** `deduction_type` (ENUM: `DAMAGE_REPAIR`, `LATE_RETURN_PENALTY`, `FUEL_DEFICIT`, `CLEANING_FEE`, `TRAFFIC_CHALLAN`), `proposed_amount` (DECIMAL(10,2)), `approved_amount` (DECIMAL(10,2) NULL), `evidence_summary` (TEXT), `status` (ENUM: `PROPOSED`, `DISPUTED`, `APPROVED_BY_ADMIN`, `AGREED_BY_CUSTOMER`, `CANCELLED`), `dispute_reason` (TEXT NULL), `dispute_opened_at` (DATETIME NULL), `resolved_at` (DATETIME NULL), `created_at`.
- **Important Indexes:** `idx_deposit_deductions_deposit` (`security_deposit_id`)

#### 38. `security_deposit_refunds`
- **Responsibility:** Direct deposit refunds executed by partner outside platform. Platform monitors and records refund reference, proof, date, and customer confirmation/dispute. Platform never executes gateway transfers.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `security_deposit_id` $\rightarrow$ `security_deposits.id` (`ON DELETE RESTRICT`), `booking_id` $\rightarrow$ `bookings.id` (`ON DELETE RESTRICT`), `manager_partner_id` $\rightarrow$ `partner_profiles.id` (`ON DELETE RESTRICT`), `customer_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`)
- **Important Columns:** `refund_amount` (DECIMAL(10,2)), `currency` (CHAR(3) DEFAULT 'INR'), `refund_method` (ENUM: `UPI_TRANSFER`, `BANK_NEFT_IMPS`, `CASH`), `refund_transaction_reference` (VARCHAR 100: UTR Number), `receipt_object_key` (VARCHAR 255 NULL), `status` (ENUM: `PROMISED`, `DISBURSED_BY_PARTNER`, `CONFIRMED_BY_CUSTOMER`, `DISPUTED_BY_CUSTOMER`, `ADMIN_OVERRIDE`), `refunded_at` (DATETIME NULL), `acknowledged_at` (DATETIME NULL), `dispute_notes` (TEXT NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_dep_refunds_ref` (`manager_partner_id`, `refund_transaction_reference`)
- **Important Indexes:** `idx_deposit_refunds_booking` (`booking_id`), `idx_deposit_refunds_deposit` (`security_deposit_id`)

#### 39. `contributor_payables`
- **Responsibility:** Partner-contributor contractual obligation tracking model (NOT a platform general ledger liability). Enforces single, complete direct partner settlement with immutable payment evidence.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `booking_id` $\rightarrow$ `bookings.id` (`ON DELETE RESTRICT`), `agreement_id` $\rightarrow$ `contribution_agreements.id` (`ON DELETE RESTRICT`), `contributor_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`), `partner_id` $\rightarrow$ `partner_profiles.id` (`ON DELETE RESTRICT`)
- **Important Columns:** `fixed_amount` (DECIMAL(10,2)), `currency` (CHAR(3) DEFAULT 'INR'), `payable_status` (ENUM: `PENDING_RETURN`, `ELIGIBLE`, `SCHEDULED`, `SETTLED_BY_PARTNER`, `ACKNOWLEDGED`, `DISPUTED`, `CANCELLED`), `settlement_mode` (ENUM: `DIRECT_PARTNER_SETTLED`), `bank_transaction_reference` (VARCHAR 100 NULL: UTR Number), `receipt_document_object_key` (VARCHAR 255 NULL), `partner_settled_at` (DATETIME NULL), `contributor_acknowledged_at` (DATETIME NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_contrib_payables_booking` (`booking_id`, `agreement_id`)
- **Important Indexes:** `idx_contrib_payables_contributor` (`contributor_user_id`, `payable_status`), `idx_contrib_payables_partner` (`partner_id`, `payable_status`)

---

### Module 8: Transactional Outbox, Support, Reviews & Auditing (6 Tables)

#### 40. `outbox_events`
- **Responsibility:** Transactional outbox guaranteeing resilient, idempotent delivery of external side-effects (Cloudinary media processing, notifications, email/SMS dispatch).
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** None (intentionally generic typed aggregate reference: `aggregate_type`, `aggregate_id`)
- **Important Columns:** `aggregate_type` (VARCHAR 50), `aggregate_id` (BIGINT UNSIGNED), `event_type` (VARCHAR 50), `payload_json` (JSON), `status` (ENUM: `PENDING`, `PROCESSING`, `PROCESSED`, `FAILED`, `DEAD_LETTER`), `retry_count` (TINYINT UNSIGNED DEFAULT 0), `next_retry_at` (DATETIME NULL), `failure_reason` (TEXT NULL), `created_at`, `processed_at` (DATETIME NULL).
- **Important Indexes:** `idx_outbox_polling` (`status`, `next_retry_at`)

#### 41. `reviews`
- **Responsibility:** Customer ratings and reviews bound to completed bookings.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `booking_id` $\rightarrow$ `bookings.id` (`ON DELETE RESTRICT`), `reviewer_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`), `vehicle_id` $\rightarrow$ `vehicles.id` (`ON DELETE RESTRICT`), `partner_id` $\rightarrow$ `partner_profiles.id` (`ON DELETE RESTRICT`)
- **Important Columns:** `rating_score` (TINYINT UNSIGNED: 1-5), `rating_cleanliness` (TINYINT UNSIGNED: 1-5), `rating_maintenance` (TINYINT UNSIGNED: 1-5), `review_text` (TEXT NULL), `moderation_status` (ENUM: `PUBLISHED`, `FLAGGED`, `REJECTED`), `created_at`.
- **Unique Constraints:** `uk_reviews_booking` (`booking_id`)
- **Important Indexes:** `idx_reviews_vehicle` (`vehicle_id`, `moderation_status`)

#### 42. `support_tickets`
- **Responsibility:** Customer, partner, and contributor support and dispute tickets.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`), `booking_id` $\rightarrow$ `bookings.id` (NULLABLE, `ON DELETE SET NULL`), `vehicle_id` $\rightarrow$ `vehicles.id` (NULLABLE, `ON DELETE SET NULL`), `assigned_admin_id` $\rightarrow$ `users.id` (NULLABLE)
- **Important Columns:** `ticket_number` (VARCHAR 32), `category` (ENUM: `BOOKING`, `PAYMENT`, `DAMAGE`, `CUSTODY`, `AGREEMENT`, `GENERAL`), `priority` (ENUM: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`), `status` (ENUM: `OPEN`, `ASSIGNED`, `IN_PROGRESS`, `WAITING_FOR_USER`, `RESOLVED`, `CLOSED`), `subject` (VARCHAR 200), `resolved_at` (DATETIME NULL), `created_at`, `updated_at`.
- **Unique Constraints:** `uk_support_tickets_number` (`ticket_number`)
- **Important Indexes:** `idx_tickets_user_status` (`user_id`, `status`)

#### 43. `support_ticket_messages`
- **Responsibility:** Threaded discussion messages inside support tickets.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `ticket_id` $\rightarrow$ `support_tickets.id` (`ON DELETE CASCADE`), `sender_user_id` $\rightarrow$ `users.id` (`ON DELETE RESTRICT`)
- **Important Columns:** `message_body` (TEXT), `attachment_object_key` (VARCHAR 255 NULL), `is_internal_note` (BOOLEAN DEFAULT FALSE), `sent_at` (DATETIME).
- **Important Indexes:** `idx_ticket_messages_ticket` (`ticket_id`, `sent_at`)

#### 44. `notifications`
- **Responsibility:** In-app, SMS, and email notification delivery tracking.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `user_id` $\rightarrow$ `users.id` (`ON DELETE CASCADE`)
- **Important Columns:** `channel` (ENUM: `IN_APP`, `EMAIL`, `SMS`), `template_code` (VARCHAR 50), `title` (VARCHAR 200), `message_content` (TEXT), `payload_json` (JSON NULL), `delivery_status` (ENUM: `PENDING`, `SENT`, `FAILED`, `READ`), `read_at` (DATETIME NULL), `created_at`.
- **Important Indexes:** `idx_notifications_user_status` (`user_id`, `delivery_status`, `created_at`)

#### 45. `audit_logs`
- **Responsibility:** Immutable security, authorization, payment verification, and administrative action audit log.
- **Primary Key:** `id` (BIGINT UNSIGNED AUTO_INCREMENT)
- **Foreign Keys:** `actor_user_id` $\rightarrow$ `users.id` (NULLABLE)
- **Important Columns:** `action` (VARCHAR 100), `entity_type` (VARCHAR 50), `entity_id` (BIGINT UNSIGNED), `ip_address` (VARCHAR 45 NULL), `correlation_id` (VARCHAR 64 NULL), `metadata_json_redacted` (JSON NULL), `occurred_at` (DATETIME).
- **Important Indexes:** `idx_audit_logs_actor` (`actor_user_id`, `occurred_at`), `idx_audit_logs_entity` (`entity_type`, `entity_id`)

---

### Module 9: Infrastructure-Managed Tables (2 Tables)

#### 46. `shedlock`
- **Responsibility:** Distributed task lock table managed directly by the ShedLock Spring library.
- **Primary Key:** `name` (VARCHAR 64)
- **Important Columns:** `lock_until` (TIMESTAMP(3)), `locked_at` (TIMESTAMP(3)), `locked_by` (VARCHAR 255).

#### 47. `flyway_schema_history`
- **Responsibility:** Database migration audit table managed directly by Flyway.
- **Primary Key:** `installed_rank` (INT)
- **Important Columns:** `version` (VARCHAR 50), `description` (VARCHAR 200), `type` (VARCHAR 20), `script` (VARCHAR 1000), `checksum` (INT), `installed_by` (VARCHAR 100), `installed_on` (TIMESTAMP), `execution_time` (INT), `success` (BOOLEAN).

---

## 3. Future-Phase Marketplace Gateway & Double-Entry Tables (8 Tables)

The following 8 tables are formally preserved and specified for future activation upon onboarding an approved marketplace payment provider (e.g., Razorpay Route) with automated split settlements and platform general ledger tracking:

#### FP1. `payment_webhook_events`
- **Responsibility:** Inward gateway webhook idempotency ledger guaranteeing once-and-only-once processing.
- **Key Columns:** `provider`, `provider_event_id`, `event_type`, `payload_json`, `signature_verified`, `processing_status`.

#### FP2. `partner_commissions`
- **Responsibility:** Automated platform commission fee calculation and settlement deductions.
- **Key Columns:** `booking_id`, `partner_id`, `gross_rental_amount`, `commission_rate_percentage`, `platform_commission_amount`, `tax_on_commission`, `net_partner_entitlement`.

#### FP3. `payout_methods`
- **Responsibility:** Tokenized disbursement recipient bank/UPI accounts for automated partner payouts.
- **Key Columns:** `user_id`, `payee_role`, `provider`, `provider_fund_account_id`, `account_holder_name`, `account_number_last4`, `ifsc_code_encrypted`.

#### FP4. `settlements`
- **Responsibility:** Outward automated marketplace settlement batches and partner disbursements.
- **Key Columns:** `payee_user_id`, `payout_method_id`, `amount`, `status`, `provider_payout_id`, `initiated_at`, `completed_at`.

#### FP5. `ledger_accounts`
- **Responsibility:** Master Chart of Accounts for platform double-entry general ledger.
- **Key Columns:** `account_code`, `account_name`, `account_category`, `normal_balance`, `is_active`.

#### FP6. `ledger_transactions`
- **Responsibility:** Journal transaction header records grouping balancing debits and credits.
- **Key Columns:** `transaction_group_id`, `reference_type`, `reference_id`, `narrative`, `posted_at`.

#### FP7. `ledger_entries`
- **Responsibility:** Append-only double-entry journal line items.
- **Key Columns:** `transaction_id`, `account_id`, `sequence_number`, `debit_amount`, `credit_amount`.

#### FP8. `platform_commission_rules`
- **Responsibility:** Configurable percentage and fixed fee commission rules by category and tier.
- **Key Columns:** `vehicle_category`, `partner_tier`, `commission_percentage`, `fixed_fee`, `min_commission`, `effective_from`.

