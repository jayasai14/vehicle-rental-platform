-- ============================================================================
-- VEHICLE RENTAL PLATFORM — AUTHORITATIVE DATABASE SCHEMA (48 MVP TABLES)
-- Target DBMS: MySQL 8.0+
-- Encoding: utf8mb4 / utf8mb4_unicode_ci
-- ============================================================================

-- MODULE 1: IDENTITY, ACCESS, KYC & ADDRESSES
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    status ENUM('PENDING_VERIFICATION', 'ACTIVE', 'SUSPENDED', 'DEACTIVATED') NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_phone UNIQUE (phone_number),
    INDEX idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE roles (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_roles_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_roles (
    user_id BIGINT UNSIGNED NOT NULL,
    role_id BIGINT UNSIGNED NOT NULL,
    assigned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    family_id VARCHAR(64) NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_refresh_tokens_family (family_id),
    INDEX idx_refresh_tokens_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE addresses (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255) NULL,
    landmark VARCHAR(150) NULL,
    city VARCHAR(100) NOT NULL DEFAULT 'Vijayawada',
    state VARCHAR(100) NOT NULL DEFAULT 'Andhra Pradesh',
    postal_code VARCHAR(10) NOT NULL,
    latitude DECIMAL(10, 8) NULL,
    longitude DECIMAL(11, 8) NULL,
    address_type ENUM('CUSTOMER_RESIDENCE', 'PARTNER_OFFICE', 'PARTNER_HUB', 'PICKUP_POINT', 'DELIVERY_DESTINATION') NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_addresses_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_addresses_city_state (city, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE customer_profiles (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    residential_address_id BIGINT UNSIGNED NULL,
    date_of_birth DATE NULL,
    emergency_contact_name VARCHAR(150) NULL,
    emergency_contact_phone VARCHAR(20) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_customer_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_customer_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_customer_profiles_address FOREIGN KEY (residential_address_id) REFERENCES addresses(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE partner_profiles (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    business_name VARCHAR(200) NOT NULL,
    registered_business_address_id BIGINT UNSIGNED NULL,
    pan_number VARCHAR(10) NOT NULL,
    gstin VARCHAR(15) NULL,
    trade_licence_number VARCHAR(50) NULL,
    commercial_registration_number VARCHAR(50) NULL,
    verification_status ENUM('SUBMITTED', 'IN_REVIEW', 'VERIFIED', 'REJECTED') NOT NULL DEFAULT 'SUBMITTED',
    verified_at DATETIME NULL,
    verified_by_user_id BIGINT UNSIGNED NULL,
    rejection_reason VARCHAR(500) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_partner_profiles_user UNIQUE (user_id),
    CONSTRAINT uk_partner_profiles_pan UNIQUE (pan_number),
    CONSTRAINT uk_partner_profiles_gstin UNIQUE (gstin),
    CONSTRAINT fk_partner_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_partner_profiles_address FOREIGN KEY (registered_business_address_id) REFERENCES addresses(id) ON DELETE SET NULL,
    CONSTRAINT fk_partner_profiles_verifier FOREIGN KEY (verified_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_partner_verification_status (verification_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE partner_documents (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id BIGINT UNSIGNED NOT NULL,
    document_type ENUM('TRADE_LICENCE', 'GSTIN_CERTIFICATE', 'PAN_CARD', 'COMMERCIAL_TAX_RECEIPT', 'OTHER_COMPLIANCE') NOT NULL,
    document_number VARCHAR(100) NULL,
    storage_object_key VARCHAR(255) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT UNSIGNED NOT NULL,
    verification_status ENUM('PENDING', 'VERIFIED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at DATETIME NULL,
    verified_by_user_id BIGINT UNSIGNED NULL,
    rejection_reason VARCHAR(500) NULL,
    CONSTRAINT fk_partner_documents_partner FOREIGN KEY (partner_id) REFERENCES partner_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_partner_documents_verifier FOREIGN KEY (verified_by_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE contributor_profiles (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    residential_address_id BIGINT UNSIGNED NULL,
    pan_number VARCHAR(10) NOT NULL,
    bank_account_holder_name VARCHAR(150) NOT NULL,
    bank_account_number VARCHAR(50) NOT NULL,
    bank_ifsc_code VARCHAR(15) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_contributor_profiles_user UNIQUE (user_id),
    CONSTRAINT uk_contributor_profiles_pan UNIQUE (pan_number),
    CONSTRAINT fk_contributor_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_contributor_profiles_address FOREIGN KEY (residential_address_id) REFERENCES addresses(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE driving_licences (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    licence_number VARCHAR(50) NOT NULL,
    issuing_rto VARCHAR(100) NOT NULL,
    valid_from DATE NOT NULL,
    valid_until DATE NOT NULL,
    vehicle_classes_authorized VARCHAR(100) NOT NULL,
    storage_object_key VARCHAR(255) NOT NULL,
    verification_status ENUM('PENDING', 'VERIFIED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    verified_at DATETIME NULL,
    verified_by_user_id BIGINT UNSIGNED NULL,
    rejection_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_driving_licences_user UNIQUE (user_id),
    CONSTRAINT uk_driving_licences_number UNIQUE (licence_number),
    CONSTRAINT fk_driving_licences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_driving_licences_verifier FOREIGN KEY (verified_by_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE kyc_verifications (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    kyc_type ENUM('AADHAAR_OKYC', 'PASSPORT', 'VOTER_ID') NOT NULL,
    verification_reference VARCHAR(100) NOT NULL,
    status ENUM('INITIATED', 'VERIFIED', 'FAILED') NOT NULL DEFAULT 'INITIATED',
    verified_at DATETIME NULL,
    id_card_storage_key VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_kyc_verifications_user_type UNIQUE (user_id, kyc_type),
    CONSTRAINT fk_kyc_verifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- MODULE 2: OPERATING LOCATIONS & LOGISTICS
-- ----------------------------------------------------------------------------
CREATE TABLE cities (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL DEFAULT 'Andhra Pradesh',
    country VARCHAR(100) NOT NULL DEFAULT 'India',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_cities_name_state UNIQUE (name, state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE partner_hubs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(150) NOT NULL,
    address_id BIGINT UNSIGNED NOT NULL,
    city_id BIGINT UNSIGNED NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_partner_hubs_partner FOREIGN KEY (partner_id) REFERENCES partner_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_partner_hubs_address FOREIGN KEY (address_id) REFERENCES addresses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_partner_hubs_city FOREIGN KEY (city_id) REFERENCES cities(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pickup_locations (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id BIGINT UNSIGNED NOT NULL,
    partner_hub_id BIGINT UNSIGNED NULL,
    address_id BIGINT UNSIGNED NOT NULL,
    location_name VARCHAR(150) NOT NULL,
    location_type ENUM('HUB', 'AIRPORT', 'RAILWAY_STATION', 'BUS_TERMINAL', 'HOTEL_DROPOFF', 'DOORSTEP_DELIVERY_ZONE') NOT NULL,
    doorstep_delivery_surcharge DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_pickup_locations_partner FOREIGN KEY (partner_id) REFERENCES partner_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_pickup_locations_hub FOREIGN KEY (partner_hub_id) REFERENCES partner_hubs(id) ON DELETE SET NULL,
    CONSTRAINT fk_pickup_locations_address FOREIGN KEY (address_id) REFERENCES addresses(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- MODULE 3: VEHICLE INVENTORY, COMPLIANCE & PRICING
-- ----------------------------------------------------------------------------
CREATE TABLE vehicles (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    registration_number VARCHAR(30) NOT NULL,
    vin_chassis_number VARCHAR(50) NOT NULL,
    engine_number VARCHAR(50) NOT NULL,
    make VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    year_of_manufacture INT UNSIGNED NOT NULL,
    category ENUM('CAR', 'BIKE', 'SCOOTER') NOT NULL,
    transmission ENUM('MANUAL', 'AUTOMATIC') NOT NULL DEFAULT 'MANUAL',
    fuel_type ENUM('PETROL', 'DIESEL', 'ELECTRIC', 'HYBRID', 'CNG') NOT NULL DEFAULT 'PETROL',
    seating_capacity INT UNSIGNED NOT NULL DEFAULT 4,
    odometer_reading_km INT UNSIGNED NOT NULL DEFAULT 0,
    current_fuel_or_battery_level_percent INT UNSIGNED NOT NULL DEFAULT 100,
    
    ownership_type ENUM('INDIVIDUAL_OWNER', 'PARTNER_BUSINESS_OWNER') NOT NULL,
    legal_owner_user_id BIGINT UNSIGNED NULL,
    legal_owner_partner_id BIGINT UNSIGNED NULL,
    manager_partner_id BIGINT UNSIGNED NOT NULL,
    assigned_hub_id BIGINT UNSIGNED NULL,
    
    current_custody_status ENUM('CONTRIBUTOR_CUSTODY', 'PARTNER_CUSTODY', 'CUSTOMER_CUSTODY', 'IN_MAINTENANCE', 'IN_TRANSIT') NOT NULL DEFAULT 'PARTNER_CUSTODY',
    listing_status ENUM('DRAFT', 'PENDING_APPROVAL', 'PUBLISHED', 'UNLISTED', 'SUSPENDED') NOT NULL DEFAULT 'DRAFT',
    turnaround_buffer_minutes INT UNSIGNED NOT NULL DEFAULT 60,
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_vehicles_registration UNIQUE (registration_number),
    CONSTRAINT uk_vehicles_vin UNIQUE (vin_chassis_number),
    CONSTRAINT uk_vehicles_engine UNIQUE (engine_number),
    CONSTRAINT fk_vehicles_legal_owner_user FOREIGN KEY (legal_owner_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vehicles_legal_owner_partner FOREIGN KEY (legal_owner_partner_id) REFERENCES partner_profiles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vehicles_manager_partner FOREIGN KEY (manager_partner_id) REFERENCES partner_profiles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vehicles_assigned_hub FOREIGN KEY (assigned_hub_id) REFERENCES partner_hubs(id) ON DELETE SET NULL,
    INDEX idx_vehicles_category_status (category, listing_status),
    INDEX idx_vehicles_custody (current_custody_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vehicle_documents (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    vehicle_id BIGINT UNSIGNED NOT NULL,
    document_type ENUM('REGISTRATION_CERTIFICATE_RC', 'INSURANCE_POLICY', 'POLLUTION_UNDER_CONTROL_PUC', 'COMMERCIAL_PERMIT', 'FITNESS_CERTIFICATE') NOT NULL,
    document_number VARCHAR(100) NOT NULL,
    issued_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    storage_object_key VARCHAR(255) NOT NULL,
    verification_status ENUM('PENDING', 'VERIFIED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    verified_at DATETIME NULL,
    verified_by_user_id BIGINT UNSIGNED NULL,
    rejection_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicle_documents_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    CONSTRAINT fk_vehicle_documents_verifier FOREIGN KEY (verified_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_vehicle_docs_expiry (expiry_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vehicle_pricing_profiles (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    vehicle_id BIGINT UNSIGNED NOT NULL,
    hourly_rate DECIMAL(10,2) NOT NULL,
    daily_rate DECIMAL(10,2) NOT NULL,
    weekly_rate DECIMAL(10,2) NULL,
    security_deposit_amount DECIMAL(10,2) NOT NULL DEFAULT 2000.00,
    free_km_per_day INT UNSIGNED NOT NULL DEFAULT 300,
    extra_km_charge DECIMAL(10,2) NOT NULL DEFAULT 10.00,
    late_return_hourly_penalty DECIMAL(10,2) NOT NULL DEFAULT 200.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_pricing_profiles_vehicle UNIQUE (vehicle_id),
    CONSTRAINT fk_pricing_profiles_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vehicle_images (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    vehicle_id BIGINT UNSIGNED NOT NULL,
    storage_object_key VARCHAR(255) NOT NULL,
    caption VARCHAR(100) NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT UNSIGNED NOT NULL DEFAULT 0,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicle_images_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vehicle_features (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    vehicle_id BIGINT UNSIGNED NOT NULL,
    feature_name VARCHAR(100) NOT NULL,
    feature_category ENUM('SAFETY', 'COMFORT', 'ENTERTAINMENT', 'PERFORMANCE', 'CONVENIENCE') NOT NULL DEFAULT 'CONVENIENCE',
    CONSTRAINT uk_vehicle_features UNIQUE (vehicle_id, feature_name),
    CONSTRAINT fk_vehicle_features_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vehicle_maintenance_blocks (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    vehicle_id BIGINT UNSIGNED NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    reason VARCHAR(255) NOT NULL,
    blocked_by_user_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_maintenance_blocks_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    CONSTRAINT fk_maintenance_blocks_user FOREIGN KEY (blocked_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_maint_times (vehicle_id, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- MODULE 5: CONTRIBUTOR AGREEMENTS & FLEET PROPOSALS
-- ----------------------------------------------------------------------------
CREATE TABLE contributor_vehicle_applications (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    contributor_id BIGINT UNSIGNED NOT NULL,
    target_partner_id BIGINT UNSIGNED NOT NULL,
    make VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    year_of_manufacture INT UNSIGNED NOT NULL,
    category ENUM('CAR', 'BIKE', 'SCOOTER') NOT NULL,
    transmission ENUM('MANUAL', 'AUTOMATIC') NOT NULL DEFAULT 'MANUAL',
    fuel_type ENUM('PETROL', 'DIESEL', 'ELECTRIC', 'HYBRID', 'CNG') NOT NULL DEFAULT 'PETROL',
    registration_number VARCHAR(30) NOT NULL,
    vin_chassis_number VARCHAR(50) NOT NULL,
    engine_number VARCHAR(50) NOT NULL,
    current_odometer_km INT UNSIGNED NOT NULL DEFAULT 0,
    vehicle_pickup_address_id BIGINT UNSIGNED NOT NULL,
    rc_document_storage_key VARCHAR(255) NOT NULL,
    insurance_document_storage_key VARCHAR(255) NOT NULL,
    status ENUM('SUBMITTED', 'IN_REVIEW', 'ACCEPTED', 'REJECTED', 'WITHDRAWN') NOT NULL DEFAULT 'SUBMITTED',
    reviewed_at DATETIME NULL,
    reviewed_by_user_id BIGINT UNSIGNED NULL,
    rejection_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_cva_registration UNIQUE (registration_number),
    CONSTRAINT fk_cva_contributor FOREIGN KEY (contributor_id) REFERENCES contributor_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_cva_partner FOREIGN KEY (target_partner_id) REFERENCES partner_profiles(id) ON DELETE CASCADE,
    CONSTRAINT fk_cva_address FOREIGN KEY (vehicle_pickup_address_id) REFERENCES addresses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_cva_reviewer FOREIGN KEY (reviewed_by_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE contribution_agreements (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT UNSIGNED NOT NULL,
    partner_id BIGINT UNSIGNED NOT NULL,
    contributor_id BIGINT UNSIGNED NOT NULL,
    vehicle_id BIGINT UNSIGNED NULL,
    agreement_code VARCHAR(32) NOT NULL,
    fixed_contributor_cut_per_booking DECIMAL(10,2) NOT NULL,
    effective_start_date DATE NOT NULL,
    effective_end_date DATE NOT NULL,
    notice_period_days INT UNSIGNED NOT NULL DEFAULT 7,
    terms_and_conditions_text TEXT NOT NULL,
    status ENUM('PENDING_PARTNER_SIGNATURE', 'PENDING_CONTRIBUTOR_SIGNATURE', 'ACTIVE', 'TERMINATION_REQUESTED', 'TERMINATED', 'EXPIRED') NOT NULL DEFAULT 'PENDING_CONTRIBUTOR_SIGNATURE',
    partner_signed_at DATETIME NULL,
    contributor_signed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_agreements_code UNIQUE (agreement_code),
    CONSTRAINT uk_agreements_app UNIQUE (application_id),
    CONSTRAINT fk_agreements_app FOREIGN KEY (application_id) REFERENCES contributor_vehicle_applications(id) ON DELETE RESTRICT,
    CONSTRAINT fk_agreements_partner FOREIGN KEY (partner_id) REFERENCES partner_profiles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_agreements_contributor FOREIGN KEY (contributor_id) REFERENCES contributor_profiles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_agreements_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE contribution_agreement_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    agreement_id BIGINT UNSIGNED NOT NULL,
    event_type ENUM('PROPOSED', 'SIGNED_BY_PARTNER', 'SIGNED_BY_CONTRIBUTOR', 'ACTIVATED', 'WITHDRAWAL_NOTICED', 'TERMINATED', 'EXPIRED') NOT NULL,
    actor_user_id BIGINT UNSIGNED NOT NULL,
    event_notes VARCHAR(500) NULL,
    event_timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cae_agreement FOREIGN KEY (agreement_id) REFERENCES contribution_agreements(id) ON DELETE CASCADE,
    CONSTRAINT fk_cae_actor FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE vehicle_withdrawal_requests (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    agreement_id BIGINT UNSIGNED NOT NULL,
    requested_by_user_id BIGINT UNSIGNED NOT NULL,
    requested_withdrawal_date DATE NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status ENUM('SUBMITTED', 'APPROVED', 'VEHICLE_RETURNED', 'REJECTED') NOT NULL DEFAULT 'SUBMITTED',
    approved_by_user_id BIGINT UNSIGNED NULL,
    actual_return_date DATE NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_vwr_agreement FOREIGN KEY (agreement_id) REFERENCES contribution_agreements(id) ON DELETE CASCADE,
    CONSTRAINT fk_vwr_requester FOREIGN KEY (requested_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vwr_approver FOREIGN KEY (approved_by_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- MODULE 6 & 7: HOLDS, DEMANDS, SUBMISSIONS, BOOKINGS
-- ----------------------------------------------------------------------------
CREATE TABLE booking_holds (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    hold_reference VARCHAR(32) NOT NULL,
    vehicle_id BIGINT UNSIGNED NOT NULL,
    customer_user_id BIGINT UNSIGNED NOT NULL,
    pickup_location_id BIGINT UNSIGNED NOT NULL,
    dropoff_location_id BIGINT UNSIGNED NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    quoted_rental_amount DECIMAL(10,2) NOT NULL,
    quoted_deposit_amount DECIMAL(10,2) NOT NULL,
    quoted_surcharge_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_hold_amount DECIMAL(10,2) NOT NULL,
    status ENUM('ACTIVE', 'VERIFICATION_PENDING', 'CONVERTED', 'EXPIRED', 'RELEASED') NOT NULL DEFAULT 'ACTIVE',
    expires_at DATETIME NOT NULL,
    recovered_from_hold_id BIGINT UNSIGNED NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_booking_holds_ref UNIQUE (hold_reference),
    CONSTRAINT fk_booking_holds_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_booking_holds_customer FOREIGN KEY (customer_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_booking_holds_pickup_loc FOREIGN KEY (pickup_location_id) REFERENCES pickup_locations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_booking_holds_dropoff_loc FOREIGN KEY (dropoff_location_id) REFERENCES pickup_locations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_booking_holds_recovery FOREIGN KEY (recovered_from_hold_id) REFERENCES booking_holds(id) ON DELETE SET NULL,
    INDEX idx_holds_interval (vehicle_id, start_time, end_time),
    INDEX idx_holds_status_expires (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rental_payment_demands (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    demand_reference VARCHAR(32) NOT NULL,
    hold_id BIGINT UNSIGNED NULL,
    manager_partner_id BIGINT UNSIGNED NOT NULL,
    customer_user_id BIGINT UNSIGNED NOT NULL,
    demand_type ENUM('BOOKING_CONFIRMATION', 'BOOKING_EXTENSION', 'LATE_RETURN_FEE', 'INCIDENT_CHARGE') NOT NULL DEFAULT 'BOOKING_CONFIRMATION',
    amount_demanded DECIMAL(10,2) NOT NULL,
    total_verified_submitted DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    amount_satisfied DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    excess_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    cancellation_fee_retained DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    status ENUM('OPEN', 'PARTIALLY_SATISFIED', 'SATISFIED', 'EXPIRED', 'CANCELLED') NOT NULL DEFAULT 'OPEN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_payment_demands_ref UNIQUE (demand_reference),
    CONSTRAINT fk_demands_hold FOREIGN KEY (hold_id) REFERENCES booking_holds(id) ON DELETE SET NULL,
    CONSTRAINT fk_demands_partner FOREIGN KEY (manager_partner_id) REFERENCES partner_profiles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_demands_customer FOREIGN KEY (customer_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_demands_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rental_payment_submissions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    submission_reference VARCHAR(32) NOT NULL,
    demand_id BIGINT UNSIGNED NOT NULL,
    manager_partner_id BIGINT UNSIGNED NOT NULL,
    customer_user_id BIGINT UNSIGNED NOT NULL,
    payment_method ENUM('UPI_QR', 'UPI_ID', 'EXTERNAL_LINK', 'CASH_AT_PICKUP') NOT NULL,
    transaction_reference VARCHAR(100) NULL,
    amount_submitted DECIMAL(10,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    receipt_object_key VARCHAR(255) NULL,
    transaction_timestamp DATETIME NOT NULL,
    submission_status ENUM('SUBMITTED_FOR_VERIFICATION', 'VERIFIED', 'REJECTED') NOT NULL DEFAULT 'SUBMITTED_FOR_VERIFICATION',
    verified_at DATETIME NULL,
    verified_by_user_id BIGINT UNSIGNED NULL,
    rejection_reason VARCHAR(500) NULL,
    
    recovered_for_demand_id BIGINT UNSIGNED NULL,
    reapplied_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    reapplied_at DATETIME NULL,
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_submissions_ref UNIQUE (submission_reference),
    CONSTRAINT uk_submissions_partner_ref UNIQUE (manager_partner_id, transaction_reference),
    CONSTRAINT fk_submissions_demand FOREIGN KEY (demand_id) REFERENCES rental_payment_demands(id) ON DELETE RESTRICT,
    CONSTRAINT fk_submissions_partner FOREIGN KEY (manager_partner_id) REFERENCES partner_profiles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_submissions_customer FOREIGN KEY (customer_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_submissions_verifier FOREIGN KEY (verified_by_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_submissions_recovered_demand FOREIGN KEY (recovered_for_demand_id) REFERENCES rental_payment_demands(id) ON DELETE SET NULL,
    INDEX idx_submissions_status (submission_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bookings (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    booking_code VARCHAR(32) NOT NULL,
    hold_id BIGINT UNSIGNED NOT NULL,
    vehicle_id BIGINT UNSIGNED NOT NULL,
    customer_user_id BIGINT UNSIGNED NOT NULL,
    manager_partner_id BIGINT UNSIGNED NOT NULL,
    pickup_location_id BIGINT UNSIGNED NOT NULL,
    dropoff_location_id BIGINT UNSIGNED NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    actual_return_time DATETIME NULL,
    base_rental_amount DECIMAL(10,2) NOT NULL,
    security_deposit_amount DECIMAL(10,2) NOT NULL,
    delivery_surcharge_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total_amount_payable DECIMAL(10,2) NOT NULL,
    initial_demand_id BIGINT UNSIGNED NOT NULL,
    booking_status ENUM('CONFIRMED', 'READY_FOR_PICKUP', 'IN_PROGRESS', 'OVERDUE', 'RETURNED', 'COMPLETED', 'CANCELLED', 'NO_SHOW') NOT NULL DEFAULT 'CONFIRMED',
    cancellation_reason VARCHAR(500) NULL,
    cancelled_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_bookings_code UNIQUE (booking_code),
    CONSTRAINT uk_bookings_hold UNIQUE (hold_id),
    CONSTRAINT fk_bookings_hold FOREIGN KEY (hold_id) REFERENCES booking_holds(id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_customer FOREIGN KEY (customer_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_partner FOREIGN KEY (manager_partner_id) REFERENCES partner_profiles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_pickup_loc FOREIGN KEY (pickup_location_id) REFERENCES pickup_locations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_dropoff_loc FOREIGN KEY (dropoff_location_id) REFERENCES pickup_locations(id) ON DELETE RESTRICT,
    CONSTRAINT fk_bookings_demand FOREIGN KEY (initial_demand_id) REFERENCES rental_payment_demands(id) ON DELETE RESTRICT,
    INDEX idx_bookings_interval (vehicle_id, start_time, end_time),
    INDEX idx_bookings_status (booking_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE booking_status_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT UNSIGNED NOT NULL,
    from_status ENUM('CONFIRMED', 'READY_FOR_PICKUP', 'IN_PROGRESS', 'OVERDUE', 'RETURNED', 'COMPLETED', 'CANCELLED', 'NO_SHOW') NULL,
    to_status ENUM('CONFIRMED', 'READY_FOR_PICKUP', 'IN_PROGRESS', 'OVERDUE', 'RETURNED', 'COMPLETED', 'CANCELLED', 'NO_SHOW') NOT NULL,
    actor_user_id BIGINT UNSIGNED NOT NULL,
    transition_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bse_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_bse_actor FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE booking_extensions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT UNSIGNED NOT NULL,
    previous_end_time DATETIME NOT NULL,
    requested_end_time DATETIME NOT NULL,
    extension_rental_amount DECIMAL(10,2) NOT NULL,
    demand_id BIGINT UNSIGNED NULL,
    status ENUM('REQUESTED', 'APPROVED', 'REJECTED', 'CANCELLED') NOT NULL DEFAULT 'REQUESTED',
    decided_by_user_id BIGINT UNSIGNED NULL,
    decision_reason VARCHAR(500) NULL,
    decided_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_extensions_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_extensions_demand FOREIGN KEY (demand_id) REFERENCES rental_payment_demands(id) ON DELETE SET NULL,
    CONSTRAINT fk_extensions_decider FOREIGN KEY (decided_by_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inspections (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT UNSIGNED NOT NULL,
    vehicle_id BIGINT UNSIGNED NOT NULL,
    inspection_type ENUM('PRE_RENTAL_HANDOVER', 'POST_RENTAL_RETURN', 'MAINTENANCE_INSPECTION') NOT NULL,
    odometer_reading_km INT UNSIGNED NOT NULL,
    fuel_or_battery_level_percent INT UNSIGNED NOT NULL,
    exterior_condition_notes TEXT NULL,
    interior_condition_notes TEXT NULL,
    known_scratches_or_dents TEXT NULL,
    inspector_user_id BIGINT UNSIGNED NOT NULL,
    customer_signature_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    partner_signature_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    conducted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    hub_id BIGINT UNSIGNED NULL,
    CONSTRAINT fk_inspections_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_inspections_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inspections_user FOREIGN KEY (inspector_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_inspections_hub FOREIGN KEY (hub_id) REFERENCES partner_hubs(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inspection_media (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    inspection_id BIGINT UNSIGNED NOT NULL,
    view_angle ENUM('FRONT', 'REAR', 'LEFT_SIDE', 'RIGHT_SIDE', 'ODOMETER_DASH', 'FUEL_GAUGE', 'INTERIOR_CABIN', 'CLOSEUP_DAMAGE', 'SPARE_TYRE') NOT NULL,
    storage_object_key VARCHAR(255) NOT NULL,
    caption VARCHAR(255) NULL,
    captured_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inspection_media_inspection FOREIGN KEY (inspection_id) REFERENCES inspections(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE incidents (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    incident_reference VARCHAR(32) NOT NULL,
    booking_id BIGINT UNSIGNED NOT NULL,
    vehicle_id BIGINT UNSIGNED NOT NULL,
    reported_by_user_id BIGINT UNSIGNED NOT NULL,
    incident_type ENUM('ACCIDENT_MAJOR', 'ACCIDENT_MINOR', 'MECHANICAL_BREAKDOWN', 'THEFT_OR_PILFERAGE', 'TRAFFIC_VIOLATION_FINE', 'INTERIOR_DAMAGE') NOT NULL,
    severity ENUM('LOW', 'MEDIUM', 'HIGH', 'CRITICAL') NOT NULL DEFAULT 'MEDIUM',
    description TEXT NOT NULL,
    police_report_filed BOOLEAN NOT NULL DEFAULT FALSE,
    police_fir_number VARCHAR(100) NULL,
    estimated_damage_cost DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    status ENUM('REPORTED', 'INVESTIGATING', 'INSURANCE_CLAIM_FILED', 'RESOLVED', 'CLOSED') NOT NULL DEFAULT 'REPORTED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_incidents_ref UNIQUE (incident_reference),
    CONSTRAINT fk_incidents_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT,
    CONSTRAINT fk_incidents_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_incidents_reporter FOREIGN KEY (reported_by_user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- MODULE 4: PHYSICAL VEHICLE CUSTODY TRACKING
-- ----------------------------------------------------------------------------
CREATE TABLE vehicle_custody_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    vehicle_id BIGINT UNSIGNED NOT NULL,
    booking_id BIGINT UNSIGNED NULL,
    handover_type ENUM(
        'CONTRIBUTOR_TO_PARTNER',
        'PARTNER_TO_CUSTOMER',
        'CONTRIBUTOR_TO_CUSTOMER',
        'CUSTOMER_TO_PARTNER',
        'CUSTOMER_TO_CONTRIBUTOR',
        'PARTNER_TO_CONTRIBUTOR',
        'PARTNER_HUB_TO_PARTNER_HUB',
        'LOGISTICS_AGENT_RELOCATION'
    ) NOT NULL,
    from_custodian_user_id BIGINT UNSIGNED NULL,
    to_custodian_user_id BIGINT UNSIGNED NULL,
    from_partner_location_id BIGINT UNSIGNED NULL,
    to_partner_location_id BIGINT UNSIGNED NULL,
    handover_address_id BIGINT UNSIGNED NOT NULL,
    actor_user_id BIGINT UNSIGNED NOT NULL,
    authorized_rep_user_id BIGINT UNSIGNED NULL,
    event_timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    odometer_reading_km INT UNSIGNED NOT NULL,
    fuel_or_battery_level_percent INT UNSIGNED NOT NULL,
    custody_notes VARCHAR(500) NULL,
    from_party_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    to_party_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_vce_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vce_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE SET NULL,
    CONSTRAINT fk_vce_from_user FOREIGN KEY (from_custodian_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vce_to_user FOREIGN KEY (to_custodian_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vce_from_hub FOREIGN KEY (from_partner_location_id) REFERENCES partner_hubs(id) ON DELETE SET NULL,
    CONSTRAINT fk_vce_to_hub FOREIGN KEY (to_partner_location_id) REFERENCES partner_hubs(id) ON DELETE SET NULL,
    CONSTRAINT fk_vce_address FOREIGN KEY (handover_address_id) REFERENCES addresses(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vce_actor FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_vce_rep FOREIGN KEY (authorized_rep_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- MODULE 8 (PARTIAL FOR ARBITRATION FKs): SUPPORT TICKETS
-- ----------------------------------------------------------------------------
CREATE TABLE support_tickets (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    ticket_reference VARCHAR(32) NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    booking_id BIGINT UNSIGNED NULL,
    subject VARCHAR(200) NOT NULL,
    category ENUM('BOOKING_ISSUE', 'PAYMENT_ISSUE', 'VEHICLE_CONDITION', 'DEPOSIT_DISPUTE', 'REFUND_DISPUTE', 'CONTRIBUTOR_SETTLEMENT', 'OTHER') NOT NULL,
    status ENUM('OPEN', 'IN_PROGRESS', 'WAITING_ON_USER', 'RESOLVED', 'CLOSED') NOT NULL DEFAULT 'OPEN',
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') NOT NULL DEFAULT 'MEDIUM',
    assigned_admin_user_id BIGINT UNSIGNED NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_support_tickets_ref UNIQUE (ticket_reference),
    CONSTRAINT fk_support_tickets_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_support_tickets_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE SET NULL,
    CONSTRAINT fk_support_tickets_admin FOREIGN KEY (assigned_admin_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE support_ticket_messages (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT UNSIGNED NOT NULL,
    sender_user_id BIGINT UNSIGNED NOT NULL,
    message_text TEXT NOT NULL,
    attachment_object_key VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stm_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE,
    CONSTRAINT fk_stm_sender FOREIGN KEY (sender_user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- MODULE 7: PARTNER PAYMENT PROFILES, REFUNDS & DEPOSITS
-- ----------------------------------------------------------------------------
CREATE TABLE partner_payment_profiles (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    partner_id BIGINT UNSIGNED NOT NULL,
    payee_display_name VARCHAR(150) NOT NULL,
    upi_id VARCHAR(100) NOT NULL,
    upi_qr_object_key VARCHAR(255) NULL,
    external_payment_url VARCHAR(255) NULL,
    accepted_payment_methods JSON NOT NULL,
    is_cash_at_pickup_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_partner_payment_profiles UNIQUE (partner_id),
    CONSTRAINT fk_ppp_partner FOREIGN KEY (partner_id) REFERENCES partner_profiles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rental_fee_refunds (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    refund_reference VARCHAR(32) NOT NULL,
    booking_id BIGINT UNSIGNED NULL,
    demand_id BIGINT UNSIGNED NULL,
    hold_id BIGINT UNSIGNED NULL,
    manager_partner_id BIGINT UNSIGNED NOT NULL,
    customer_user_id BIGINT UNSIGNED NOT NULL,
    refund_reason ENUM('BOOKING_CANCELLATION', 'OVERPAYMENT_RETURN', 'EXPIRED_HOLD_EXCEPTION', 'PAYMENT_MISMATCH_RETURN', 'ADMIN_ARBITRATION_ORDER', 'OTHER_EXCEPTION') NOT NULL,
    cancellation_policy_snapshot JSON NULL,
    gross_amount_paid DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    cancellation_fee_deducted DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    refund_amount DECIMAL(10,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    refund_status ENUM('PENDING_PARTNER_DISBURSEMENT', 'DISBURSED_BY_PARTNER', 'CUSTOMER_ACKNOWLEDGED', 'DISPUTED_BY_CUSTOMER', 'DISPUTE_WINDOW_ELAPSED_UNACKNOWLEDGED', 'ADMIN_RESOLVED', 'CANCELLED') NOT NULL DEFAULT 'PENDING_PARTNER_DISBURSEMENT',
    refund_method ENUM('UPI_TRANSFER', 'BANK_NEFT_IMPS', 'CASH_RETURN') NOT NULL,
    partner_transaction_reference VARCHAR(100) NULL,
    receipt_object_key VARCHAR(255) NULL,
    recorded_by_user_id BIGINT UNSIGNED NOT NULL,
    
    dispute_ticket_id BIGINT UNSIGNED NULL,
    resolved_by_admin_user_id BIGINT UNSIGNED NULL,
    resolution_notes VARCHAR(1000) NULL,
    resolved_at DATETIME NULL,
    
    disbursed_at DATETIME NULL,
    acknowledged_at DATETIME NULL,
    dispute_reason VARCHAR(500) NULL,
    dispute_opened_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_rental_refunds_ref UNIQUE (refund_reference),
    CONSTRAINT uk_rental_refunds_partner_utr UNIQUE (manager_partner_id, partner_transaction_reference),
    CONSTRAINT fk_rental_refunds_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rental_refunds_demand FOREIGN KEY (demand_id) REFERENCES rental_payment_demands(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rental_refunds_hold FOREIGN KEY (hold_id) REFERENCES booking_holds(id) ON DELETE SET NULL,
    CONSTRAINT fk_rental_refunds_partner FOREIGN KEY (manager_partner_id) REFERENCES partner_profiles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rental_refunds_customer FOREIGN KEY (customer_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rental_refunds_recorder FOREIGN KEY (recorded_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rental_refunds_dispute_ticket FOREIGN KEY (dispute_ticket_id) REFERENCES support_tickets(id) ON DELETE SET NULL,
    CONSTRAINT fk_rental_refunds_resolved_admin FOREIGN KEY (resolved_by_admin_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_rental_refunds_status (refund_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE security_deposits (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT UNSIGNED NOT NULL,
    manager_partner_id BIGINT UNSIGNED NOT NULL,
    customer_user_id BIGINT UNSIGNED NOT NULL,
    required_amount DECIMAL(10,2) NOT NULL,
    collected_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    held_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    deducted_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    refunded_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    currency CHAR(3) NOT NULL DEFAULT 'INR',
    collection_method ENUM('UPI_TRANSFER', 'BANK_NEFT_IMPS', 'CASH_AT_PICKUP') NOT NULL,
    collection_transaction_reference VARCHAR(100) NULL,
    status ENUM('PENDING_COLLECTION', 'HELD_BY_PARTNER', 'PARTIALLY_REFUNDED', 'FULLY_REFUNDED', 'FORFEITED_TO_COVER_LOSSES') NOT NULL DEFAULT 'PENDING_COLLECTION',
    collected_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_security_deposits_booking UNIQUE (booking_id),
    CONSTRAINT fk_security_deposits_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT,
    CONSTRAINT fk_security_deposits_partner FOREIGN KEY (manager_partner_id) REFERENCES partner_profiles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_security_deposits_customer FOREIGN KEY (customer_user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE deposit_deductions (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    deposit_id BIGINT UNSIGNED NOT NULL,
    incident_id BIGINT UNSIGNED NULL,
    inspection_id BIGINT UNSIGNED NULL,
    deduction_reason ENUM('VEHICLE_DAMAGE', 'FUEL_DEFICIT', 'LATE_RETURN_PENALTY', 'TRAFFIC_VIOLATION_FINE', 'CLEANING_FEE') NOT NULL,
    proposed_deduction_amount DECIMAL(10,2) NOT NULL,
    approved_deduction_amount DECIMAL(10,2) NULL,
    proposed_by_user_id BIGINT UNSIGNED NOT NULL,
    evidence_document_key VARCHAR(255) NULL,
    status ENUM('PROPOSED', 'DISPUTED', 'APPROVED_BY_ADMIN', 'AGREED_BY_CUSTOMER', 'CANCELLED') NOT NULL DEFAULT 'PROPOSED',
    customer_dispute_reason VARCHAR(500) NULL,
    dispute_opened_at DATETIME NULL,
    
    dispute_ticket_id BIGINT UNSIGNED NULL,
    resolved_by_admin_user_id BIGINT UNSIGNED NULL,
    resolution_notes VARCHAR(1000) NULL,
    resolved_at DATETIME NULL,
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_deductions_deposit FOREIGN KEY (deposit_id) REFERENCES security_deposits(id) ON DELETE CASCADE,
    CONSTRAINT fk_deductions_incident FOREIGN KEY (incident_id) REFERENCES incidents(id) ON DELETE SET NULL,
    CONSTRAINT fk_deductions_inspection FOREIGN KEY (inspection_id) REFERENCES inspections(id) ON DELETE SET NULL,
    CONSTRAINT fk_deductions_proposer FOREIGN KEY (proposed_by_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_deductions_dispute_ticket FOREIGN KEY (dispute_ticket_id) REFERENCES support_tickets(id) ON DELETE SET NULL,
    CONSTRAINT fk_deductions_resolved_admin FOREIGN KEY (resolved_by_admin_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE security_deposit_refunds (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    deposit_id BIGINT UNSIGNED NOT NULL,
    manager_partner_id BIGINT UNSIGNED NOT NULL,
    customer_user_id BIGINT UNSIGNED NOT NULL,
    refund_amount DECIMAL(10,2) NOT NULL,
    refund_method ENUM('UPI_TRANSFER', 'BANK_NEFT_IMPS', 'CASH_RETURN') NOT NULL,
    partner_transaction_reference VARCHAR(100) NULL,
    receipt_object_key VARCHAR(255) NULL,
    refund_status ENUM('PENDING_PARTNER_DISBURSEMENT', 'DISBURSED_BY_PARTNER', 'CUSTOMER_ACKNOWLEDGED', 'DISPUTED_BY_CUSTOMER', 'DISPUTE_WINDOW_ELAPSED_UNACKNOWLEDGED', 'ADMIN_RESOLVED', 'CANCELLED') NOT NULL DEFAULT 'PENDING_PARTNER_DISBURSEMENT',
    disbursed_at DATETIME NULL,
    acknowledged_at DATETIME NULL,
    dispute_reason VARCHAR(500) NULL,
    dispute_opened_at DATETIME NULL,
    
    dispute_ticket_id BIGINT UNSIGNED NULL,
    resolved_by_admin_user_id BIGINT UNSIGNED NULL,
    resolution_notes VARCHAR(1000) NULL,
    resolved_at DATETIME NULL,
    
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_deposit_refunds_deposit FOREIGN KEY (deposit_id) REFERENCES security_deposits(id) ON DELETE CASCADE,
    CONSTRAINT fk_deposit_refunds_partner FOREIGN KEY (manager_partner_id) REFERENCES partner_profiles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_deposit_refunds_customer FOREIGN KEY (customer_user_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_deposit_refunds_dispute_ticket FOREIGN KEY (dispute_ticket_id) REFERENCES support_tickets(id) ON DELETE SET NULL,
    CONSTRAINT fk_deposit_refunds_resolved_admin FOREIGN KEY (resolved_by_admin_user_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE contributor_payables (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT UNSIGNED NOT NULL,
    agreement_id BIGINT UNSIGNED NOT NULL,
    partner_id BIGINT UNSIGNED NOT NULL,
    contributor_user_id BIGINT UNSIGNED NOT NULL,
    agreed_cut_amount DECIMAL(10,2) NOT NULL,
    payable_status ENUM('PENDING_COMPLETION', 'ELIGIBLE', 'SETTLEMENT_RECORDED', 'CONFIRMED_BY_CONTRIBUTOR', 'DISPUTED') NOT NULL DEFAULT 'PENDING_COMPLETION',
    partner_payment_method ENUM('BANK_NEFT_IMPS', 'UPI_TRANSFER') NULL,
    partner_transaction_reference VARCHAR(100) NULL,
    partner_receipt_object_key VARCHAR(255) NULL,
    settled_at DATETIME NULL,
    confirmed_at DATETIME NULL,
    dispute_reason VARCHAR(500) NULL,
    disputed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_contributor_payables_booking UNIQUE (booking_id),
    CONSTRAINT fk_payables_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payables_agreement FOREIGN KEY (agreement_id) REFERENCES contribution_agreements(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payables_partner FOREIGN KEY (partner_id) REFERENCES partner_profiles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_payables_contributor FOREIGN KEY (contributor_user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- MODULE 8: TRANSACTIONAL OUTBOX, REVIEWS, NOTIFICATIONS & AUDIT
-- ----------------------------------------------------------------------------
CREATE TABLE outbox_events (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    payload JSON NOT NULL,
    status ENUM('PENDING', 'PROCESSED', 'FAILED', 'DEAD_LETTER') NOT NULL DEFAULT 'PENDING',
    retry_count INT UNSIGNED NOT NULL DEFAULT 0,
    max_retries INT UNSIGNED NOT NULL DEFAULT 5,
    last_error VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME NULL,
    INDEX idx_outbox_status_retries (status, retry_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reviews (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT UNSIGNED NOT NULL,
    vehicle_id BIGINT UNSIGNED NOT NULL,
    customer_user_id BIGINT UNSIGNED NOT NULL,
    rating_cleanliness INT UNSIGNED NOT NULL,
    rating_vehicle_condition INT UNSIGNED NOT NULL,
    rating_partner_service INT UNSIGNED NOT NULL,
    overall_rating DECIMAL(3,2) NOT NULL,
    review_title VARCHAR(150) NULL,
    review_text TEXT NULL,
    partner_reply_text TEXT NULL,
    partner_replied_at DATETIME NULL,
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_reviews_booking UNIQUE (booking_id),
    CONSTRAINT fk_reviews_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_customer FOREIGN KEY (customer_user_id) REFERENCES users(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    reference_entity_type VARCHAR(50) NULL,
    reference_entity_id VARCHAR(50) NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notifications_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_logs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    actor_user_id BIGINT UNSIGNED NULL,
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    details JSON NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_audit_entity (entity_name, entity_id),
    INDEX idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- MODULE 9: INFRASTRUCTURE TABLES
-- ----------------------------------------------------------------------------
CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP(3) NOT NULL,
    locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
