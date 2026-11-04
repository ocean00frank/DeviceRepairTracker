# Device Repair Tracker

## Problem

Manual record-keeping can lead to lost records, forgotten repair jobs,
confusion about repair status, and difficulties tracking payments. A simple
computer-based system is needed to help repair shops organise repair records
and manage jobs more efficiently.

## Solution

Device Repair Tracker is a Java web application that helps repair shops manage
customers, devices, repair jobs, repair progress, outcomes, and payments. It
provides a browser dashboard backed by a Javalin REST API and PostgreSQL
database.

## Team

| Name | Student number |
|---|---:|
| Jean Kapalu | 2410497 |
| Faith Sikombwa | 2410056 |
| Muzhinga Chibanda | 2410151 |
| Abraham Lubosi | 2410265 |
| Frank Mwelwa | 2410434 |

## Main features

- Register, update, list, and delete customers and devices.
- Create and search repair jobs.
- Track repair status, repair outcome, and repair history.
- Record repair costs, payments, and outstanding balances.
- View dashboard statistics for repair progress.
- Use a browser interface connected to REST API endpoints.

## Technology

- Java 17
- Javalin REST API
- PostgreSQL
- Maven
- HTML, CSS, and JavaScript frontend
- JUnit 5 tests

## Class responsibilities

### Application and business logic

| Class | Responsibility |
|---|---|
| `Main` | Initializes the database, starts the Javalin server, configures JSON handling, registers API routes, and serves the web interface. |
| `ApiController` | Defines REST endpoints, validates requests, calls the service layer, and returns JSON responses. |
| `RepairManager` | Applies business rules and coordinates customer, device, repair-job, history, and payment operations. |
| `DatabaseConfig` | Loads database settings, creates PostgreSQL connections, and initializes the database schema. |

### Model classes

| Class | Responsibility |
|---|---|
| `Customer` | Stores and validates customer identity and contact details. |
| `Device` | Stores and validates device type, brand, model, and customer ownership. |
| `RepairJob` | Stores repair details, costs, outcomes, payment status, repair status, and status history. |
| `Payment` | Stores and validates payment amount, method, status, and date. |
| `RepairStatus` | Defines the allowed repair progress states. |
| `Trackable` | Defines the `trackStatus()` method implemented by trackable objects. |

### Repository and exception classes

| Class | Responsibility |
|---|---|
| `CustomerRepository` | Saves, finds, updates, and deletes customer records. |
| `DeviceRepository` | Saves, finds, updates, and deletes device records. |
| `RepairJobRepository` | Stores, searches, counts, and updates repair jobs. |
| `PaymentRepository` | Records payments and calculates repair totals and balances. |
| `RepairHistoryRepository` | Retrieves repair status-change history. |
| `InvalidRepairStatusException` | Reports invalid repair-status transitions. |

## Object-oriented concepts demonstrated

- **Encapsulation:** `Customer`, `Device`, `RepairJob`, and `Payment` keep
  their fields private and use validated getters and setters.
- **Inheritance:** The project uses Java's base `Object` class. It mainly uses
  interface implementation instead of class-to-class inheritance.
- **Abstraction:** The `Trackable` interface defines the `trackStatus()` method
  without specifying how each implementing class performs it.
- **Polymorphism:** `RepairJob` and `Payment` implement `Trackable` and provide
  different implementations of `trackStatus()`.
- **Collections:** The project uses `List`, `ArrayList`, and `HashMap` to
  store and manage customers, devices, repair jobs, payments, and responses.
- **Custom exception:** `InvalidRepairStatusException` is thrown when an
  invalid repair-status transition is attempted.

## Requirements

- JDK 17 or newer
- Apache Maven
- PostgreSQL
- Git

## How to run

1. Clone the repository and move into the application directory:

   ```powershell
   git clone <repository-url>
   cd <repository-folder>\tracker
   ```

2. Create the PostgreSQL database:

   ```sql
   CREATE DATABASE "DeviceRepair_db";
   ```

3. Create and configure the local environment file:

   ```powershell
   Copy-Item .env.example .env
   ```

   Update `.env` with the PostgreSQL credentials:

   ```env
   DB_HOST=localhost
   DB_PORT=5432
   DB_NAME=DeviceRepair_db
   DB_USER=your_postgresql_username
   DB_PASSWORD=your_postgresql_password
   ```

   Do not commit `.env` because it contains local database credentials.

4. Build and test the application:

   ```powershell
   mvn clean test
   ```

5. Start the application:

   ```powershell
   mvn exec:java
   ```

6. Open <http://localhost:7000/> in a browser. The API health check is
   available at <http://localhost:7000/api/health>.

To use a different port:

```powershell
mvn exec:java "-Dexec.args=7001"
```

## Project structure

- `tracker/src/main/java/com/devicerepair` contains the Java source code.
- `tracker/src/main/resources/public` contains the browser interface.
- `tracker/database/schema.sql` defines the PostgreSQL tables and indexes.
- `tracker/src/test` contains automated tests.

## Repository collaboration

The team uses one shared GitHub repository. Each member should commit their
completed work from their own GitHub account, use descriptive commit messages,
and push changes so contributions remain visible in the commit history.
