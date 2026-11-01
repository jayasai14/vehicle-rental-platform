# Entity Relationships and Referential Integrity Specification

**Project:** Vehicle Rental Platform  
**Market:** Vijayawada, Andhra Pradesh  
**Status:** Pre-Freeze Corrected Baseline  
**Confirmed Physical Tables:** 52 Proposed Physical Tables (50 Application-Domain Tables + 2 Infrastructure Tables)

---

## 1. Domain Modeling Principles & Foreign Key Integrity

To maintain strict relational integrity across all 50 application-domain tables, the platform enforces:

1. **No Untyped Polymorphic Foreign Keys on Business Entities:**  
   Every business entity (`vehicles`, `bookings`, `agreements`, `inspections`, `incidents`, `deposits`, `payables`, `settlements`) is bound via explicit, typed relational foreign keys with strict database constraints (`FOREIGN KEY REFERENCES`). Polymorphic reference identifiers are strictly isolated to append-only audit and integration tables (`ledger_entries`, `outbox_events`, `audit_logs`), where they are constrained by checked `reference_type` or `aggregate_type` enumerations.
2. **Strict Separation of Legal Ownership, Operational Management, and Custody:**
   - **Legal Ownership (`legal_owner_user_id`):** References `users.id` (the registered individual person or entity named on the vehicle RC).
   - **Operational Management (`manager_partner_id`):** References `partner_profiles.id` (the commercial rental partner authorized to list, price, and manage rentals).
   - **Physical Custody (`current_custody_type`, `current_custodian_id`):** Independent physical custody state machine tracking who currently possesses the physical asset.
3. **Pessimistic Concurrency & Hold Independence:**
   - `booking_holds` reserves calendar intervals under `SELECT ... FOR UPDATE` row locks.
   - Creating a hold does **not** insert an incomplete or draft record into `bookings`.
   - `bookings` rows are created only upon verified payment capture.
4. **Referential Deletion Controls:**
   - Dependent leaf assets (`vehicle_images`, `inspection_media`, `refresh_tokens`, `support_ticket_messages`) use `ON DELETE CASCADE`.
   - Core domain entities (`vehicles`, `bookings`, `agreements`, `inspections`, `payment_orders`, `security_deposits`, `settlements`) strictly use `ON DELETE RESTRICT` to guarantee historical and financial audit integrity.

---

## 2. Comprehensive Master Entity Relationship Diagram

The following diagram explicitly models **all 50 application-domain tables** and their relational constraints.

```mermaid
erDiagram
    users ||--o{ user_roles : "grants"
    roles ||--o{ user_roles : "defines"
    users ||--o| customer_profiles : "has"
    users ||--o| contributor_profiles : "has"
    users ||--o| partner_profiles : "operates"
    partner_profiles ||--o{ partner_documents : "verifies_with"
    users ||--o{ identity_verifications : "undergoes"
    users ||--o| driver_licences : "holds"
    users ||--o{ addresses : "registers"
    users ||--o{ refresh_tokens : "authenticates_with"
    users ||--o{ payout_methods : "configures"

    cities ||--o{ addresses : "contains"
    cities ||--o{ vehicles : "operates_in"
    cities ||--o{ platform_commission_rules : "applies_to"
    partner_profiles ||--o{ partner_locations : "runs"
    addresses ||--o| partner_locations : "locates"
    partner_profiles ||--o{ delivery_zones : "services"

    users ||--o{ vehicles : "owns_legally (legal_owner_user_id)"
    partner_profiles ||--o{ vehicles : "manages_operationally (manager_partner_id)"
    partner_locations ||--o{ vehicles : "stations_at (primary_hub_location_id)"
    vehicles ||--o{ vehicle_documents : "complies_with"
    vehicles ||--o{ vehicle_images : "showcases"
    vehicles ||--o{ vehicle_pricing : "schedules_rates"
    vehicles ||--o{ pickup_locations : "offers"
    partner_locations ||--o{ pickup_locations : "supplies_hub"
    addresses ||--o{ pickup_locations : "supplies_address"
    vehicles ||--o{ vehicle_availability_blocks : "blackouts"

    vehicles ||--o{ vehicle_custody_events : "tracks_custody"
    addresses ||--o{ vehicle_custody_events : "handover_site"
    users ||--o{ vehicle_custody_events : "actor"

    vehicles ||--o{ vehicle_contribution_applications : "applied_for"
    users ||--o{ vehicle_contribution_applications : "applies"
    partner_profiles ||--o{ vehicle_contribution_applications : "reviews"

    vehicles ||--o{ contribution_agreements : "contracted_under"
    users ||--o{ contribution_agreements : "contributor_party"
    partner_profiles ||--o{ contribution_agreements : "partner_party"
    contribution_agreements ||--o{ contribution_agreement_events : "audits"
    vehicles ||--o{ vehicle_withdrawal_requests : "requests_withdrawal"
    contribution_agreements ||--o{ vehicle_withdrawal_requests : "governed_by"
    users ||--o{ vehicle_withdrawal_requests : "initiated_by"

    vehicles ||--o{ booking_holds : "locks_interval"
    users ||--o{ booking_holds : "held_by_customer"
    booking_holds ||--o| bookings : "converts_to"

    vehicles ||--o{ bookings : "reserves"
    users ||--o{ bookings : "rents_as_customer"
    partner_profiles ||--o{ bookings : "manages_fulfillment"
    pickup_locations ||--o{ bookings : "pickup_point"
    pickup_locations ||--o{ bookings : "return_point"
    bookings ||--o{ booking_status_events : "status_history"
    bookings ||--o{ booking_extensions : "extends"
    bookings ||--o{ inspections : "condition_logged_by"
    vehicles ||--o{ inspections : "inspected"
    users ||--o{ inspections : "conducted_by"
    inspections ||--o{ inspection_media : "evidenced_by"
    bookings ||--o{ incidents : "damage_reported"
    vehicles ||--o{ incidents : "damage_sustained"
    users ||--o{ incidents : "reported_by"

    bookings ||--|| security_deposits : "secured_by"
    security_deposits ||--o{ deposit_deductions : "claims"
    incidents ||--o{ deposit_deductions : "causes"
    users ||--o{ deposit_deductions : "resolved_by"

    bookings ||--o{ payment_orders : "billed_by"
    booking_holds ||--o{ payment_orders : "deposits_for_checkout"
    payment_orders ||--o{ payment_transactions : "attempts"
    payment_webhook_events ||--o{ payment_transactions : "reconciles"
    bookings ||--o{ refunds : "refunded_via"
    security_deposits ||--o{ refunds : "releases_to"
    payment_transactions ||--o{ refunds : "reverses_transaction"

    bookings ||--o| partner_commissions : "calculates_cut"
    partner_profiles ||--o{ partner_commissions : "billed_to"
    bookings ||--o| contributor_payables : "obligates_partner"
    contribution_agreements ||--o{ contributor_payables : "terms_set_by"
    users ||--o{ contributor_payables : "owed_to_contributor"
    partner_profiles ||--o{ contributor_payables : "debtor_partner"

    users ||--o{ settlements : "paid_out_to"
    payout_methods ||--o{ settlements : "routed_via"

    bookings ||--o| reviews : "rated_by"
    users ||--o{ reviews : "reviewer"
    vehicles ||--o{ reviews : "reviewed_vehicle"
    partner_profiles ||--o{ reviews : "reviewed_partner"

    users ||--o{ support_tickets : "opened_by"
    bookings ||--o{ support_tickets : "disputes_booking"
    vehicles ||--o{ support_tickets : "disputes_vehicle"
    users ||--o{ support_tickets : "assigned_admin"
    support_tickets ||--o{ support_ticket_messages : "messages"
    users ||--o{ support_ticket_messages : "message_sender"

    users ||--o{ notifications : "receives"
    users ||--o{ audit_logs : "actor_audit"

    bookings ||--o{ ledger_entries : "journalizes_booking"
    settlements ||--o{ ledger_entries : "journalizes_payout"
    payment_orders ||--o{ ledger_entries : "journalizes_payment"
```

---

## 3. Detailed Entity Relationship Matrix (All 50 Domain Tables)

| # | Entity Name | Parent Entities (FK) | Child Entities (Referenced By) | Cardinality & Deletion Rules |
|---|---|---|---|---|
| 1 | `users` | None | `user_roles`, `customer_profiles`, `partner_profiles`, `contributor_profiles`, `identity_verifications`, `driver_licences`, `addresses`, `refresh_tokens`, `payout_methods`, `vehicles`, `bookings`, `inspections`, `settlements`, `reviews`, `support_tickets`, `notifications`, `audit_logs` | 1-to-N (`CASCADE` for tokens/addresses; `RESTRICT` for business entities) |
| 2 | `roles` | None | `user_roles` | 1-to-N (`ON DELETE RESTRICT`) |
| 3 | `user_roles` | `users`, `roles` | None | N-to-1 (`CASCADE` on user, `RESTRICT` on role) |
| 4 | `refresh_tokens` | `users` | None | N-to-1 (`CASCADE` on user) |
| 5 | `addresses` | `cities`, `users` (nullable) | `customer_profiles`, `partner_locations`, `pickup_locations`, `vehicle_custody_events` | 1-to-N (`RESTRICT` on cities/locations) |
| 6 | `customer_profiles`| `users`, `addresses` | None | 1-to-1 with `users` (`CASCADE`) |
| 7 | `partner_profiles` | `users` | `partner_documents`, `partner_locations`, `delivery_zones`, `vehicles`, `contribution_agreements`, `bookings`, `partner_commissions`, `contributor_payables` | 1-to-1 with `users` (`RESTRICT`) |
| 8 | `partner_documents`| `partner_profiles` | None | N-to-1 (`CASCADE` on partner) |
| 9 | `contributor_profiles`| `users` | None | 1-to-1 with `users` (`CASCADE`) |
| 10| `identity_verifications`| `users` | None | N-to-1 (`CASCADE` on user) |
| 11| `driver_licences` | `users` | None | 1-to-1 with `users` (`CASCADE`) |
| 12| `cities` | None | `addresses`, `vehicles`, `platform_commission_rules` | 1-to-N (`RESTRICT`) |
| 13| `partner_locations`| `partner_profiles`, `addresses`| `vehicles`, `pickup_locations` | N-to-1 (`RESTRICT`) |
| 14| `delivery_zones` | `partner_profiles` | None | N-to-1 (`CASCADE` on partner) |
| 15| `vehicles` | `users` (owner), `partner_profiles` (manager), `cities`, `partner_locations` (hub) | `vehicle_documents`, `vehicle_images`, `vehicle_pricing`, `pickup_locations`, `vehicle_availability_blocks`, `vehicle_custody_events`, `contribution_agreements`, `booking_holds`, `bookings`, `inspections`, `incidents`, `reviews` | N-to-1 (`RESTRICT` on core parents; `CASCADE` on images/pricing) |
| 16| `vehicle_documents`| `vehicles` | None | N-to-1 (`CASCADE` on vehicle) |
| 17| `vehicle_images` | `vehicles` | None | N-to-1 (`CASCADE` on vehicle) |
| 18| `vehicle_pricing` | `vehicles`, `users` | None | N-to-1 (`CASCADE` on vehicle) |
| 19| `pickup_locations` | `vehicles`, `partner_locations` (null), `addresses` (null) | `bookings` (as pickup / return point) | N-to-1 (`CASCADE` on vehicle, `RESTRICT` on bookings) |
| 20| `vehicle_availability_blocks`| `vehicles`, `users`| None | N-to-1 (`CASCADE` on vehicle) |
| 21| `vehicle_custody_events`| `vehicles`, `bookings` (null), `inspections` (null), `addresses`, `users` | None | N-to-1 (`RESTRICT`) |
| 22| `vehicle_contribution_applications`| `vehicles`, `users`, `partner_profiles` (null) | None | N-to-1 (`RESTRICT`) |
| 23| `contribution_agreements`| `vehicles`, `users`, `partner_profiles` | `contribution_agreement_events`, `vehicle_withdrawal_requests`, `contributor_payables` | N-to-1 (`RESTRICT`) |
| 24| `contribution_agreement_events`| `contribution_agreements`, `users` | None | N-to-1 (`CASCADE` on agreement) |
| 25| `vehicle_withdrawal_requests`| `vehicles`, `contribution_agreements`, `users` | None | N-to-1 (`RESTRICT`) |
| 26| `booking_holds` | `vehicles`, `users` | `bookings` (optional convert link), `payment_orders` | N-to-1 (`RESTRICT`) |
| 27| `bookings` | `vehicles`, `booking_holds` (null), `users`, `partner_profiles`, `pickup_locations` | `booking_status_events`, `booking_extensions`, `inspections`, `incidents`, `security_deposits`, `payment_orders`, `refunds`, `partner_commissions`, `contributor_payables`, `reviews`, `support_tickets`, `ledger_entries` | N-to-1 (`RESTRICT`) |
| 28| `booking_status_events`| `bookings`, `users` (null) | None | N-to-1 (`CASCADE` on booking) |
| 29| `booking_extensions`| `bookings`, `users` (null) | None | N-to-1 (`RESTRICT`) |
| 30| `inspections` | `bookings` (null), `vehicles`, `users` | `inspection_media`, `vehicle_custody_events` | N-to-1 (`RESTRICT`) |
| 31| `inspection_media`| `inspections` | None | N-to-1 (`CASCADE` on inspection) |
| 32| `incidents` | `bookings`, `vehicles`, `users` | `deposit_deductions` | N-to-1 (`RESTRICT`) |
| 33| `payment_orders` | `bookings` (null), `booking_holds` (null) | `payment_transactions`, `ledger_entries` | N-to-1 (`RESTRICT`) |
| 34| `payment_transactions`| `payment_orders` | `refunds` | N-to-1 (`RESTRICT`) |
| 35| `security_deposits`| `bookings` | `deposit_deductions`, `refunds` | 1-to-1 with `bookings` (`RESTRICT`) |
| 36| `deposit_deductions`| `security_deposits`, `incidents` (null), `users` (null) | None | N-to-1 (`RESTRICT`) |
| 37| `payment_webhook_events`| None (Independent idempotency ledger) | Reconciles `payment_orders` & `refunds` | Standalone audit ledger |
| 38| `refunds` | `bookings`, `security_deposits` (null), `payment_transactions` (null) | `ledger_entries` | N-to-1 (`RESTRICT`) |
| 39| `partner_commissions`| `bookings`, `partner_profiles` | None | 1-to-1 with `bookings` (`RESTRICT`) |
| 40| `contributor_payables`| `bookings`, `contribution_agreements`, `users`, `partner_profiles` | None | 1-to-1 with `bookings` (`RESTRICT`) |
| 41| `payout_methods` | `users` | `settlements` | N-to-1 (`RESTRICT`) |
| 42| `settlements` | `users`, `payout_methods` (null) | `ledger_entries` | N-to-1 (`RESTRICT`) |
| 43| `ledger_entries` | Typed references (`bookings`, `payment_orders`, `settlements`) | None | Append-only double-entry ledger |
| 44| `platform_commission_rules`| `cities` (null) | None | N-to-1 |
| 45| `outbox_events` | Typed aggregate references | None | Append-only integration queue |
| 46| `reviews` | `bookings`, `users`, `vehicles`, `partner_profiles` | None | 1-to-1 with `bookings` (`RESTRICT`) |
| 47| `support_tickets`| `users`, `bookings` (null), `vehicles` (null), `users` (admin) | `support_ticket_messages` | N-to-1 (`RESTRICT`) |
| 48| `support_ticket_messages`| `support_tickets`, `users` | None | N-to-1 (`CASCADE` on ticket) |
| 49| `notifications` | `users` | None | N-to-1 (`CASCADE` on user) |
| 50| `audit_logs` | `users` (actor, null) | None | Append-only security audit log |

---

## 4. Infrastructure Tables (Excluded from Domain Relationships)

The remaining 2 physical tables in the confirmed 52-table count are strictly framework-managed:
- **`shedlock`**: Managed directly by the ShedLock Spring library (`name` PK, `lock_until`, `locked_at`, `locked_by`).
- **`flyway_schema_history`**: Managed directly by Flyway migration engine (`installed_rank` PK, `version`, `script`, `checksum`, `success`).
Neither table participates in application-domain relational foreign keys.
