# Staff Savings and Loan Management System

A practical full-stack application for a staff association. The project uses a reusable ReactJS frontend and a Spring Boot REST API.

## Included modules

- Secure sign-in and four roles: Member, Initiator, Approver, Administrator
- Member registration, membership status, risk/watchlist status, and statements
- Individual and monthly/bulk savings with approval workflow
- Savings withdrawals with balance validation
- Loan application, approval, disbursement, repayment schedule, and balance tracking
- Individual and monthly/bulk loan repayments
- Income, expenses, configurable categories, and approval workflow
- Management dashboard, filters, CSV report export, and audit trail
- Embedded H2 database for an easy first run and an optional MySQL profile

## Requirements

- Java 17 or newer
- Node.js 20 or newer
- Maven 3.9+ (or use the included Maven wrapper scripts)

## 1. Start the backend

On Windows PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

On macOS/Linux:

```bash
cd backend
./mvnw spring-boot:run
```

The API runs at `http://localhost:8081`. Swagger UI is at `http://localhost:8081/swagger-ui.html`.

The default H2 data is stored under `backend/data/`, so it remains after a restart.

## 2. Start the frontend

Open a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`.

## Demo accounts

| Role | Username | Password |
| --- | --- | --- |
| Administrator | `admin` | `Admin@123` |
| Initiator | `initiator` | `Initiator@123` |
| Approver | `approver` | `Approver@123` |
| Member | `member` | `Member@123` |

Change these passwords before any real deployment. Demo records are created only when the database is empty.

## MySQL configuration

Create a database named `staff_finance`, then run:

Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE="mysql"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_password"
$env:SEED_DEMO="true" # first demonstration only; change all demo passwords immediately
.\mvnw.cmd spring-boot:run
```

The default MySQL URL is:

```text
jdbc:mysql://localhost:3306/staff_finance?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Africa/Kigali
```

Override it with the `DB_URL` environment variable when required.

The MySQL profile does not create demo accounts unless `SEED_DEMO=true`. For an initial demonstration, enable it once, sign in, change all demo passwords, and restart without that variable.

## Approval flow

Financial records use `DRAFT → PENDING_APPROVAL → APPROVED/REJECTED`. A user cannot approve a transaction they created. Approved entries cannot be silently edited or deleted; corrections remain visible through the audit trail.

## Project structure

```text
backend/   Spring Boot REST API, business rules, security and persistence
frontend/  ReactJS interface, reusable components and API client
```

See `docs/API-QUICK-REFERENCE.md` for the main endpoints and `docs/TESTING-GUIDE.md` for a guided acceptance test.
