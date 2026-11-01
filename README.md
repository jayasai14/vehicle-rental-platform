# Vehicle Rental Platform

A real-world vehicle rental marketplace launching in Vijayawada, Andhra Pradesh. The platform supports cars, bikes, and scooters through two supply channels:

1. **Partner-owned inventory** — verified rental partners register and manage their own vehicles independently.
2. **Contributor-owned inventory** — individuals submit vehicles to a rental partner. If accepted, the partner manages rental operations and pays the contributor a fixed amount per completed booking under a recorded agreement.

Customers use one marketplace to discover and book both types of vehicles.

## Technology

- Frontend: React + Vite, Tailwind CSS, React Router, Axios
- Backend: Java + Spring Boot, Spring Security, Spring Data JPA
- Database: MySQL
- Authentication: BCrypt password hashing, JWT access/refresh token flow, server-side role and resource authorization
- Integrations planned: Razorpay payments, Cloudinary/S3 media storage, email notifications

## Core roles

- `CUSTOMER`: browse, book, pay, manage bookings, request extensions/cancellations, review completed bookings.
- `VEHICLE_CONTRIBUTOR`: submit personally owned vehicles, review partner offers, manage agreements/availability requests, track booking-based earnings.
- `RENTAL_PARTNER`: manage partner profile, register owned inventory, review contributor vehicle submissions, manage accepted vehicles and bookings, inspections, handovers, returns and contributor settlements.
- `ADMIN`: verify partners and vehicles, manage users/listings/bookings/payments/commission/policies/disputes and platform configuration.

Accounts support multiple roles/capabilities. A customer may also become a vehicle contributor. A partner may list partner-owned inventory without relying on contributors.

## Key business rules

- Launch market: Vijayawada initially; location model must not prevent future expansion.
- Vehicle types: car, bike, scooter.
- Rental duration: hourly and daily.
- A vehicle has one active rental manager/partner at a time.
- Partner-owned vehicles are controlled by their partner.
- Contributor-owned vehicles remain legally owned by the contributor. The contributor and partner coordinate availability and the handover arrangement. The partner manages listing and rental operations after acceptance.
- A contribution agreement is mandatory before a contributor vehicle is published. It records fixed payment per eligible completed booking, payment timing, term, responsibilities, damage/maintenance rules, availability, termination and return procedure.
- Contributor payout is created according to the agreement after a completed booking and successful return process; cancelled bookings do not automatically create a payout unless the agreement explicitly defines cancellation compensation.
- Partner controls rental prices subject to platform policy and admin intervention.
- Pickup/delivery methods are configured by vehicle and partner.
- Licence and identity eligibility checks are required before a booking is confirmed.
- Security deposits are configurable by vehicle and customer verification policy.
- Booking availability must be checked transactionally to prevent overlapping confirmed bookings.
- Handover/return inspections record timestamp, odometer, fuel/charge level, photos and condition.
- Payments, refunds, commissions and contributor payouts must have auditable transaction records. Never trust frontend payment success; verify gateway events server-side.

## Initial documentation

- [Project Constitution](docs/PROJECT_CONSTITUTION.md)
- [Business Requirements](docs/BUSINESS_REQUIREMENTS.md)
- [System Architecture](docs/ARCHITECTURE.md)
- [Page and Route Inventory](docs/PAGES_AND_ROUTES.md)
- [Domain Model and Database Plan](docs/DATABASE_DESIGN.md)
- [API Module Plan](docs/API_MODULES.md)
- [Security Baseline](docs/SECURITY.md)
- [Development Workflow](docs/DEVELOPMENT_WORKFLOW.md)

## Implementation status

This repository currently starts as a documentation-first project. Do not generate application code until the architecture, domain model, database relationships and API contracts have been reviewed and approved.
