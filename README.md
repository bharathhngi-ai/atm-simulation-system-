# Smart ATM Simulation & Online Banking System

A full-stack, enterprise-grade ATM simulation and smart online banking web application built with **Java 21**, **Spring Boot 3.3.4**, **Spring Security**, **Spring Data JPA**, **MySQL 8**, **Thymeleaf**, **Bootstrap 5**, and **Vanilla JavaScript**.

---

## 🏛️ System Overview

The Smart ATM Simulation System provides realistic ATM banking workflows coupled with modern online banking management. Every balance inquiry, cash deposit, withdrawal, fund transfer, and emergency request is persisted atomically to a **MySQL** database with optimistic locking (`@Version`) and pessimistic row locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`) to eliminate race conditions and double-spending.

### Key Highlights
- **100% Real Database Connectivity**: Zero placeholder methods, zero mock data, zero TODO comments. All operations execute real SQL queries against MySQL.
- **Rule-Based Smart Fraud Detection Engine**: Real-time evaluation of transactions for velocity abuse (rapid withdrawals), high-value anomalies, repeated failed logins, and suspicious transfer behavior.
- **Peer-to-Peer Emergency Money Protocol**: Customers can request urgent funds from registered contacts; the approver receives instant notifications and authorizes the transfer with their ATM PIN.
- **Instant Account Freeze & Security Guard**: Immediate locking of account debit capabilities on customer request or administrative intervention, recorded in an immutable audit trail.
- **Enterprise Security**: BCrypt password hashing, PIN hashing, CSRF protection, brute-force login lockouts, and transactional idempotency via UUID tokens.

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Java 21 (LTS) |
| **Framework** | Spring Boot 3.3.4 (Spring MVC, Spring Data JPA, Spring Security, Spring Validation) |
| **Database** | MySQL 8.0 (InnoDB, Port 3307) |
| **Template Engine** | Thymeleaf 3.x with Spring Security Dialect |
| **Frontend Styling** | Bootstrap 5.3.3, Bootstrap Icons 1.11.3, Custom CSS |
| **Client Scripting** | Vanilla JavaScript (ES6+), Interactive ATM Keypad, Live Timestamps |
| **Build Tool** | Apache Maven 3.10+ |
| **Testing** | JUnit 5, Mockito, Spring Boot Test, H2 In-Memory (Test Profile) |

---

## 📂 Project Architecture

```
atm project 2/
├── pom.xml
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/bank/atm/
│   │   │   ├── AtmSimulationApplication.java
│   │   │   ├── config/              # SecurityConfig, WebConfig, DataInitializer
│   │   │   ├── controller/          # 11 Spring MVC Controllers
│   │   │   ├── dto/                 # 11 Validation & Form Transfer DTOs
│   │   │   ├── entity/              # 10 JPA Entities with Auditing & Locking
│   │   │   ├── enums/               # Domain Enums (Role, Status, Severity, etc.)
│   │   │   ├── exception/           # Custom Exceptions & GlobalExceptionHandler
│   │   │   ├── repository/          # 10 Spring Data Repositories with Locking
│   │   │   ├── security/            # UserDetails, Auth Handlers, SecurityUtils
│   │   │   ├── service/             # Business Logic & Fraud Detection Services
│   │   │   └── util/                # AccountNumberGenerator, MoneyUtils
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-test.properties
│   │       ├── static/
│   │       │   ├── css/styles.css   # Navy blue & white corporate banking design
│   │       │   └── js/app.js        # ATM keypad, auto-formatting, chips
│   │       └── templates/
│   │           ├── admin/           # 7 Admin management & monitoring views
│   │           ├── auth/            # Login and Customer Registration views
│   │           ├── customer/        # 14 Customer banking & transaction views
│   │           ├── error/           # Error, 403 Forbidden, 404 views
│   │           └── fragments/       # Navbar, Sidebar, Header, Alerts, Footer
│   └── test/
│       └── java/com/bank/atm/       # Unit & Integration Tests (18 test cases)
```

---

## 🗄️ Database Configuration & Schema

The application connects to MySQL using the active local instance:

- **Database Name**: `atm_simulation`
- **Port**: `3307` (or configured via `application.properties`)
- **Username**: `root`
- **Password**: `root`
- **JDBC URL**: `jdbc:mysql://localhost:3307/atm_simulation?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true`

### Tables Created:
1. `users`: Customer & Admin user credentials, profile information, and account lockout status.
2. `roles`: Role definitions (`ROLE_CUSTOMER`, `ROLE_ADMIN`).
3. `user_roles`: Many-to-many role mappings.
4. `bank_accounts`: Account numbers, `BigDecimal` balances, ATM PIN hashes, account status (`ACTIVE`, `FROZEN`, `CLOSED`), and optimistic locking `@Version`.
5. `transactions`: Complete transaction ledger with reference codes, before/after balances, types, and timestamps.
6. `emergency_requests`: Emergency money workflow records with statuses (`PENDING`, `APPROVED`, `REJECTED`, `EXPIRED`).
7. `trusted_contacts`: Fast-pay and emergency beneficiary links.
8. `fraud_alerts`: Automated anomaly alerts with severity scoring and administrative resolution logs.
9. `notifications`: In-app event alerts for deposits, transfers, approvals, and security notices.
10. `audit_logs`: Immutable compliance audit trail capturing IP addresses, actions, and actors.
11. `idempotency_records`: Client UUID tokens preventing duplicate form submissions.

---

## 🔑 Default Seed Credentials

The system automatically initializes default test accounts upon first startup:

### 1. Banking Administrator
- **Username**: `admin`
- **Password**: `admin123`
- **Role**: `ROLE_ADMIN`
- **Access**: Full access to `/admin/**` dashboards, customer management, fraud resolution, and audit logs.

### 2. Primary Customer (Johnathan Doe)
- **Username**: `john_doe`
- **Password**: `password123`
- **ATM PIN**: `1234`
- **Account Number**: `100110001001`
- **Starting Balance**: `₹15,450.00`
- **Role**: `ROLE_CUSTOMER`

### 3. Beneficiary Customer (Jane Smith)
- **Username**: `jane_smith`
- **Password**: `password123`
- **ATM PIN**: `1234`
- **Account Number**: `100110001002`
- **Starting Balance**: `₹8,200.00`
- **Role**: `ROLE_CUSTOMER`

---

## 🚀 How to Run the Application

### Prerequisites
- **Java 21 (or higher)**: Verify via `java -version`.
- **Maven 3.8+**: Verify via `mvn -version`.
- **MySQL 8.0**: Active on port `3307` with database `atm_simulation`.

### 1. Compile and Package
```powershell
mvn clean package -DskipTests
```
Or run the full test suite:
```powershell
mvn test
```

### 2. Launch the Application
Run via Maven:
```powershell
mvn spring-boot:run
```
Or execute the packaged executable JAR:
```powershell
java -jar target/atm-simulation-system-1.0.0.jar
```

### 3. Access in Web Browser
Open your browser and navigate to:
```
http://localhost:8080/
```
You will be directed to the login portal. Use the test credentials listed above to explore both Customer and Administrator capabilities.

---

## 💡 Comprehensive Feature List

### 1. Customer Banking Operations
- **Registration & Onboarding**: Automatically assigns a unique 12-digit account number, hashes the ATM PIN, and credits a welcome opening deposit.
- **Interactive ATM Dashboard**: Displays account balance, quick action cards, recent transactions, virtual ATM keypad, and quick amount selection chips (₹50, ₹100, ₹200, ₹500, ₹1000, ₹5000).
- **Balance Inquiry**: View real-time available and ledger balance with printable mini-statement advice.
- **Cash Deposit**: Deposit funds with ATM location tracking, instant balance recalculation, and printable transaction advice slip.
- **Cash Withdrawal**: ATM PIN-verified withdrawal with automatic daily limit checks and balance sufficiency validation.
- **Money Transfer**: Inter-account fund transfers using pessimistic row-locking (`SELECT ... FOR UPDATE`) in deterministic account ID order to prevent database deadlocks.
- **Transaction History**: Comprehensive filterable ledger (by transaction type, start date, and end date) with pagination.
- **Transaction Receipt**: Detailed printable ATM receipt with reference number, terminal code, timestamp, and mask-protected account details.
- **ATM PIN Change**: Secure PIN modification requiring old PIN verification and confirmation matching.
- **Profile Management**: Update personal contact information and manage security preferences.
- **Notifications**: Live notification feed with mark-as-read tracking for debit/credit events.
- **Trusted Contacts**: Manage approved transfer contacts with custom nicknames for one-click transfers.
- **Emergency Money Protocol**: Request instant emergency funds from contacts; approvers review and approve transfers seamlessly.
- **Instant Account Freeze**: Self-service security freeze to lock account from unauthorized withdrawals or transfers.

### 2. Administrator Operations
- **Executive Dashboard**: System-wide statistics, total registered accounts, cumulative transaction volume, and open security alerts.
- **Customer & Account Management**: Complete user roster with account freeze/unfreeze toggles and status management.
- **Transaction Monitoring**: Global transaction ledger across all banking users with search and filter controls.
- **Fraud Alert Management**: Review rule-triggered security flags, inspect severity levels, and resolve or dismiss alerts with notes.
- **Emergency Request Oversight**: Monitor all inter-customer emergency money requests and their resolution status.
- **System Audit Trail**: Complete immutable audit log of logins, logouts, transactions, and security modifications with IP address tracking.

---

## 🛡️ Security Implementation

- **Row-Level Pessimistic Locking**: `AccountRepository.findByIdForUpdate()` locks database rows during transactions to ensure atomic operations in concurrent environments.
- **Deadlock Avoidance Strategy**: Transfers sort account IDs numerically before acquiring locks (`firstLockId = min(srcId, tgtId)`).
- **Idempotency Protection**: Every transaction submission requires a UUID token stored in `idempotency_records` to prevent double-submitting forms.
- **Brute Force Protection**: Failed login attempts increment counter in `users`; exceeding thresholds automatically locks the user and generates a `CRITICAL` fraud alert.
- **CSRF Token Validation**: Enforced across all state-altering POST requests.
- **Role-Based Authorization**: URL authorization enforced via Spring Security (`/admin/**` restricted to `ROLE_ADMIN`).
