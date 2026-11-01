-- ============================================================================
-- V2: MASTER SEED DATA (ROLES & EXPANSION CITIES)
-- ============================================================================

INSERT INTO roles (id, name, description) VALUES
(1, 'ROLE_CUSTOMER', 'Registered marketplace customer who books vehicles'),
(2, 'ROLE_VEHICLE_CONTRIBUTOR', 'Vehicle owner who contributes vehicles to rental partners'),
(3, 'ROLE_RENTAL_PARTNER', 'Commercial rental partner managing hubs, inventory and fulfillment'),
(4, 'ROLE_ADMIN', 'Platform administrative oversight, arbitration and compliance');

INSERT INTO cities (id, name, state, country, is_active) VALUES
(1, 'Vijayawada', 'Andhra Pradesh', 'India', TRUE),
(2, 'Guntur', 'Andhra Pradesh', 'India', TRUE),
(3, 'Visakhapatnam', 'Andhra Pradesh', 'India', TRUE),
(4, 'Hyderabad', 'Telangana', 'India', TRUE);
