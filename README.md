# Device Repair Tracker

## Problem

Manual record-keeping can lead to lost records, forgotten repair jobs,
confusion about repair status, and difficulties tracking payments. A simple
computer-based system is needed to help repair shops organise repair records
and manage jobs more efficiently.

## Solution

Device Repair Tracker is a Java web application for managing repair-shop
operations. It stores customers and their devices, creates repair jobs, tracks
repair progress and outcomes, and records payment information in PostgreSQL.
The system provides a browser dashboard backed by a Javalin REST API.

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
- View dashboard statistics for jobs and repair progress.
- Use a responsive browser interface connected to REST endpoints.

## Technology

- Java 17
- Javalin REST API
- PostgreSQL
- Maven
- HTML, CSS, and JavaScript frontend
- JUnit 5 tests

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

   Update `.env` with the PostgreSQL credentials for the local computer:

   ```env
   DB_HOST=localhost
   DB_PORT=5432
   DB_NAME=DeviceRepair_db
   DB_USER=your_postgresql_username
   DB_PASSWORD=your_postgresql_password
   ```

   Do not commit `.env`; it contains local database credentials.

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



## Main Classes and Their Responsibilities

1. **Customer:** Stores customer details and contact information.
2. **Device:** Stores device details, including type, brand, and model.
3. **RepairJob:** Records device faults, repair details, costs, and repair
   status.
4. **Payment:** Records payments made for repair jobs.
5. **RepairManager:** Manages repair jobs and updates their status.
6. **InvalidRepairStatusException:** Handles invalid repair-status changes.

## Main classes and their functions

### Application and business logic

| Class | Function |
|---|---|
| `Main` | Initializes the database, configures the Javalin server and JSON handling, registers API routes, and serves the web interface. |
| `ApiController` | Defines the REST API endpoints, validates incoming requests, calls the service layer, and returns JSON responses and appropriate HTTP status codes. |
| `RepairManager` | Applies business rules and coordinates customer, device, repair-job, history, and payment operations with the repositories. |
| `DatabaseConfig` | Reads database settings from environment variables or `.env`, creates PostgreSQL connections, and initializes the schema. |

### Model classes

| Class | Function |
|---|---|
| `Customer` | Holds a customer's ID, name, and phone number, and validates the contact details. |
| `Device` | Holds a device's type, brand, model, and the ID of its owner. |
| `RepairJob` | Holds repair details, cost, outcome, and payment and repair statuses; validates status changes and keeps an in-memory status history. |
| `Payment` | Represents a payment for a repair job, including amount, payment method, status, and date; validates payment information. |
| `RepairStatus` | Lists the allowed repair progress states and converts status text to a valid state. |
| `Trackable` | Defines the `trackStatus()` method used by objects that provide a status summary. `RepairJob` and `Payment` implement it. |

### Database and error-handling classes

| Class | Function |
|---|---|
| `CustomerRepository` | Saves, finds, updates, and deletes customer records in PostgreSQL. |
| `DeviceRepository` | Saves, finds, updates, and deletes devices, including looking up devices by customer. |
| `RepairJobRepository` | Stores and retrieves repair jobs, searches and counts jobs, and updates repair details and statuses. |
| `PaymentRepository` | Records payments, updates repair costs, and calculates totals and balances for a repair job. |
| `RepairHistoryRepository` | Retrieves the recorded status-change history for a repair job. |
| `InvalidRepairStatusException` | Reports an attempted repair-status change that is not allowed. |

## Object-oriented concepts demonstrated

- **Encapsulation:** model fields are private and validated through methods.
- **Interface and polymorphism:** trackable objects implement `Trackable` and
  provide their own status-tracking behavior.
- **Custom exceptions:** invalid repair-status transitions raise
  `InvalidRepairStatusException`.
- **Collections:** the application uses Java collections such as `List`,
  `ArrayList`, and `HashMap`.





