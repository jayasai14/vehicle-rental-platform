# DriveElite — Complete Vehicle Rental Platform

A modern, production-oriented vehicle rental marketplace built for **Vijayawada, Andhra Pradesh, India**, with clean multi-city extensibility. 

The platform supports cars, bikes, scooters, and electric vehicles (EVs) across two distinct supply channels:
1. **Partner-Owned Fleet** — verified rental partners register and manage their own fleet independently.
2. **Contributor-Owned Fleet** — individuals submit idle personal vehicles to a rental partner under legally binding contribution agreements and receive predictable revenue shares.

---

## Architecture Highlights

- **Partner-Direct Payment Collection (ADR-018):** The platform never escrows, holds, or splits customer rental money in the MVP. Customers remit payments directly to the verified managing partner’s UPI ID/QR.
- **Manual Verification Gate:** Submissions require explicit partner verification (comparing against bank statement) before bookings transition to `CONFIRMED`.
- **Financial Safeguard A (Late-Payment Reapplication):** If a customer transfers funds after a 15-minute booking hold expires, the verified submission can be securely reallocated to a new active hold under strict single-use locks, preventing dead loss.
- **Financial Safeguard B (Pessimistic Refund Locking):** Partner rental fee refund obligations for cancelled bookings calculate refundable balances under pessimistic row locks: `total_verified_submitted - cancellation_fee - cumulative_unvoided_refunds`.
- **48 Physical Tables:** 46 application-domain tables + 2 infrastructure tables (`flyway_schema_history` and `shedlock`). Zero placeholder endpoints or dead UI buttons.
- **ShedLock Distributed Tasks:** Automated background tasks for expiring stale holds (every 60s) and transitioning 72-hour unacknowledged refund dispute windows.

---

## Technology Stack

### Backend
- **Core:** Java 21 LTS, Spring Boot 3.3.5
- **Security:** Spring Security 6, JWT with refresh token rotation and database-backed revocation (`refresh_tokens`), BCrypt password hashing
- **Persistence:** Spring Data JPA, Hibernate ORM, MySQL 8.0+
- **Migrations:** Flyway (`V1__initial_schema.sql`, `V2__seed_demo_data.sql`)
- **Concurrency & Locking:** Pessimistic write locks (`PESSIMISTIC_WRITE`), ShedLock for multi-instance distributed background tasks

### Frontend
- **Core:** React 18, Vite, React Router v6, Axios
- **Styling:** Tailwind CSS v3, Glassmorphism design tokens, Google Fonts (Inter & Outfit)
- **Features:** 15-minute live countdown timer, interactive UPI QR modal with UTR entry, 1-click test persona fast-fill, responsive mobile drawers, role-based route guards

---

## Project Structure

```
vehicle-rental-platform/
├── backend/
│   ├── src/main/java/com/vehiclerental/
│   │   ├── config/              # SecurityConfig, ShedLockConfig
│   │   ├── common/              # Global exceptions, ApiResponse, DataSeeder
│   │   └── modules/
│   │       ├── user/            # User, Role, RefreshToken, AuthService, AuthController
│   │       ├── location/        # City, PartnerHub, PickupLocation, LocationService
│   │       ├── partner/         # PartnerProfile, PartnerPaymentProfile, FleetService
│   │       ├── contributor/     # ContributorProfile, Agreement, ContributorService
│   │       ├── vehicle/         # Vehicle, Pricing, Features, Search, Availability
│   │       ├── custody/         # Custody state machine, VehicleCustodyEvent
│   │       ├── booking/         # BookingHold, Booking, Handover, Return, Extension
│   │       ├── payment/         # Demand, Submission, Refund, Deposit, Safeguards
│   │       └── operations/      # SupportTicket, AuditLog, ScheduledTasksService
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/
│   │       ├── V1__initial_schema.sql      # Complete 48-table DDL with late payment fields
│   │       └── V2__seed_demo_data.sql       # Demo hubs, vehicles, agreements & pricing
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── api/                 # Axios client with 401 token refresh rotation & endpoints
│   │   ├── context/             # AuthContext with 1-click persona switcher
│   │   ├── components/          # Navbar, Footer, StatusBadge, HoldTimer, UPIPaymentModal
│   │   └── pages/               # HomePage, VehiclesPage, DetailPage, CheckoutPage,
│   │                            # CustomerDashboard, PartnerDashboard, ContributorDashboard, AdminDashboard
│   ├── package.json
│   ├── tailwind.config.js
│   └── vite.config.js           # Dev server proxying /api to http://localhost:8080
├── docs/                        # Complete frozen architecture documentation (15 files)
└── README.md
```

---

## Demo Accounts & Test Personas

The database is pre-seeded with realistic Vijayawada data and test accounts for each role. Every account uses password: `Password@123`

| Role | Email | Password | Primary Capabilities |
| :--- | :--- | :--- | :--- |
| **Customer** | `customer@example.com` | `Password@123` | Browse fleet, create holds, pay partner UPI, cancel bookings, acknowledge/dispute refunds |
| **Rental Partner** | `partner@example.com` | `Password@123` | Fleet management, verify/reject UPI payments, pre-rental handover & return inspections |
| **Car Contributor** | `contributor@example.com` | `Password@123` | Attach cars to hubs, digital agreements, track monthly earnings, confirm/dispute payables |
| **Platform Admin** | `admin@example.com` | `Password@123` | KPI overview, approve/reject partners, arbitrate refund & deposit disputes, audit logs |

> **Fast Testing:** The login screen and navbar contain a **1-click Persona Switcher** allowing instant role transitions without manual credential entry.

---

## Local Setup & Running Instructions

### Prerequisites
- Java 21 or Java 17 LTS
- Node.js (v18+) and npm
- MySQL 8.0+ running on port `3306` with database `vehicle_rental_db` created:
  ```sql
  CREATE DATABASE vehicle_rental_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE USER 'vehicle_user'@'localhost' IDENTIFIED BY 'vehicle_pass';
  GRANT ALL PRIVILEGES ON vehicle_rental_db.* TO 'vehicle_user'@'localhost';
  FLUSH PRIVILEGES;
  ```

### 1. Run Backend Server
```bash
cd backend
# Run unit test suite
./mvnw test

# Start Spring Boot application on port 8080
./mvnw spring-boot:run
```
*(On Windows cmd, invoke `mvnw.cmd spring-boot:run`)*.
Flyway will automatically apply `V1__initial_schema.sql` and `V2__seed_demo_data.sql`.

### 2. Run Frontend Development Server
```bash
cd frontend
npm install
npm run dev
```
Open **[http://localhost:5173](http://localhost:5173)** in your browser. All API calls to `/api/v1` are seamlessly proxied to the backend on `localhost:8080`.

### 3. Build Production Bundle
```bash
cd frontend
npm run build
```
Creates optimized production assets in `frontend/dist/`.

---

## Automated Test Coverage

The backend includes comprehensive JUnit 5 and Mockito test suites validating critical architectural invariants:
- `AvailabilityAndBookingTest`: Calendar overlap detection, hold collisions, and availability queries.
- `FinancialSafeguardsTest`: 
  - **Safeguard A:** Late-payment recovery from expired demands onto new active holds, preventing double reapplication.
  - **Safeguard B:** Pessimistic row locking on refundable rental fee balances (`total_verified - cancellation_fee - cumulative_refunds`).
- `AuthServiceTest`: User registration, BCrypt credential verification, and refresh token rotation with single-use replay protection.

Run tests anytime with:
```bash
./mvnw test
```

---

## Operational Hubs in Vijayawada

1. **Benz Circle Central Hub** (Shop 14, MG Road, Benz Circle, Vijayawada - 520010)
2. **Vijayawada Junction Hub** (Opposite Railway Station Main Gate, Station Road, Vijayawada - 520001)
3. **Gannavaram Airport Express** (Terminal 1 Arrival Dropoff Zone, Gannavaram, Vijayawada - 521102)
4. **PNBS Transit Hub** (Pandit Nehru Bus Station Bay 3, Vijayawada - 520013)
