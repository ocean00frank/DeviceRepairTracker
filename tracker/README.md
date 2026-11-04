# Device Repair Tracker

## Problem

Repair shops need a simple way to record customers and their devices, create repair jobs, track repair progress, record whether a device was fixed, and monitor payment status.

Device Repair Tracker is a Java web application that solves this problem with a Javalin REST API, a browser frontend, and PostgreSQL permanent storage.

## Requirements

- JDK 17 or newer
- Apache Maven
- PostgreSQL
- Git

## How to run

1. Clone the group's GitHub repository:

   ```powershell
   git clone <repository-url>
   cd <repository-folder>\tracker
   ```

2. Create the database in PostgreSQL:

   ```sql
   CREATE DATABASE "DeviceRepair_db";
   ```

3. Create the local environment file:

   ```powershell
   Copy-Item .env.example .env
   ```

4. Edit `.env` with the PostgreSQL credentials for your computer:

   ```env
   DB_HOST=localhost
   DB_PORT=5432
   DB_NAME=DeviceRepair_db
   DB_USER=your_postgresql_username
   DB_PASSWORD=your_postgresql_password
   ```

   Do not commit `.env` because it contains local credentials.

5. Build and test the project:

   ```powershell
   mvn clean test
   ```

6. Start the application:

   ```powershell
   mvn exec:java
   ```

7. Open <http://localhost:7000/> in a browser. The API health check is available at <http://localhost:7000/api/health>.

To use another port:

```powershell
mvn exec:java "-Dexec.args=7001"
```

## Main classes

### Application and API

- `Main` - starts Javalin, configures JSON and serves the frontend.
- `ApiController` - defines the REST API endpoints and handles HTTP errors.
- `DatabaseConfig` - loads database settings and initializes the schema.
- `RepairManager` - coordinates business rules and repository operations.

### Model classes

- `Customer` - stores and validates customer details.
- `Device` - stores and validates device details.
- `RepairJob` - stores repair details and controls valid status transitions.
- `RepairStatus` - defines the available repair statuses.
- `Payment` - stores and validates payment transactions.
- `Trackable` - interface shared by trackable application objects.

### Repository classes

- `CustomerRepository` - saves and retrieves customers.
- `DeviceRepository` - saves and retrieves devices.
- `RepairJobRepository` - saves and retrieves repair jobs.
- `RepairHistoryRepository` - stores and retrieves status history.
- `PaymentRepository` - stores payments and calculates balances.

### Exception class

- `InvalidRepairStatusException` - reports invalid repair-status transitions.

## Object-oriented requirements demonstrated

- **Encapsulation:** model fields are private and accessed through validated methods.
- **Interface:** `RepairJob` and `Payment` implement `Trackable`.
- **Polymorphism:** both classes provide their own `trackStatus()` implementation.
- **Custom exception:** invalid repair transitions throw `InvalidRepairStatusException`.
- **Collections:** the application uses `List`, `ArrayList`, and `HashMap`.

## Frontend and storage

The frontend is in `src/main/resources/public`:

- `index.html` - page structure and forms.
- `css/style.css` - page styling.
- `js/app.js` - browser interactions and REST API calls.

PostgreSQL provides permanent storage for customers, devices, repair jobs, repair history, and payments. The database schema is in `database/schema.sql`.

## Group repository and commit history

The group uses one shared GitHub repository. Every group member must:

1. Commit the code they worked on from their own GitHub account.
2. Use a clear commit message describing the work.
3. Push their commits to the shared repository.

The GitHub commit history records which member contributed each change.
