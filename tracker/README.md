# Device Repair Tracker

A small Java + Javalin application for managing device repair jobs in a local PostgreSQL database.

## Problem and aim

Small repair shops often track jobs using paper notes, WhatsApp messages, and handwritten receipts. That causes lost records, bad status tracking, and confusion over which devices are waiting to be collected. This project provides a lightweight system for registering customers, recording devices, creating repair jobs, updating statuses, and searching records.

## Objectives

- Register customers and their contact details.
- Record devices with type, brand, and model.
- Create, view, search, and manage repair jobs.
- Track status across RECEIVED -> IN_PROGRESS -> READY_FOR_COLLECTION -> COMPLETED.
- Persist data in PostgreSQL using JDBC and PreparedStatement.
- Reject invalid transitions with a custom checked exception.

## Technologies

- Java 17
- Maven
- Javalin 5
- PostgreSQL JDBC driver
- Dotenv Java
- HTML, CSS, and vanilla JavaScript

## Prerequisites

- JDK 17 or later installed
- Maven installed
- PostgreSQL server running locally
- A database named DeviceRepair_db, or adjust the value in your local .env file if your installation uses a different database name

## PostgreSQL configuration

Create a local .env file in the project root using the sample values below:

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=DeviceRepair_db
DB_USER=your_postgresql_username
DB_PASSWORD=change_me
```

The application reads environment variables from .env and uses them to connect to PostgreSQL. Do not commit real credentials. The .env file is ignored by Git.

If your PostgreSQL role does not have permission to create tables or modify data, grant at least:

```sql
GRANT CONNECT ON DATABASE "DeviceRepair_db" TO your_user;
GRANT USAGE ON SCHEMA public TO your_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO your_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO your_user;
```

## Initialising the schema

From the project root:

```bash
mvn compile
```

The app reads and executes the schema script at `database/schema.sql` when it starts. It uses `CREATE TABLE IF NOT EXISTS` and `CREATE INDEX IF NOT EXISTS`, so it does not delete existing records.

## Running the app

```bash
mvn clean compile exec:java -Dexec.mainClass=com.devicerepair.Main
```

If `exec-maven-plugin` is not configured, run the application via:

```bash
mvn package
java -jar target/tracker-1.0-SNAPSHOT.jar
```

The frontend is served at:

- http://localhost:7000/

## API endpoints

### GET /api/health
Returns application/database status.

Example response:

```json
{
  "status": "UP",
  "database": "connected",
  "databaseName": "DeviceRepair_db"
}
```

### POST /api/customers
Registers a new customer.

Request body:

```json
{
  "fullName": "Jane Doe",
  "phoneNumber": "+27721234567"
}
```

### GET /api/customers
Lists all customers.

### POST /api/devices
Registers a device for a customer.

Request body:

```json
{
  "customerId": 1,
  "deviceType": "Phone",
  "brand": "Samsung",
  "model": "Galaxy S23"
}
```

### POST /api/repair-jobs
Creates a repair job.

Request body:

```json
{
  "customerId": 1,
  "deviceId": 1,
  "reportedFault": "Screen cracked",
  "repairNotes": "Customer reported a dropped phone"
}
```

### GET /api/repair-jobs
Lists all repair jobs.

### GET /api/repair-jobs/{id}
Gets a single repair job by ID.

### GET /api/repair-jobs/search?q=term
Searches by job ID, customer name, phone number, brand, model, and reported fault.

### PUT /api/repair-jobs/{id}/status
Updates the status, validating transitions server-side.

Request body:

```json
{
  "status": "IN_PROGRESS"
}
```

### GET /api/repair-jobs/{id}/records
Returns the status history for a repair job.

## Database tables and relationships

- customers: stores customer information
- devices: stores one-to-many relationship with customers
- repair_jobs: stores each repair request and current status
- repair_records: stores historical status changes

The schema uses primary keys, foreign keys, timestamps, and indexes.

## OOP concepts in the project

### Encapsulation
Model classes keep their fields private and expose validation through constructors and setters. Database access is kept out of the model layer.

### Inheritance
`InvalidRepairStatusException` extends `Exception` and carries the error message for invalid status transitions.

### Polymorphism
The `Trackable` interface is implemented by both `RepairJob` and `RepairRecord`. The application stores them in a `List<Trackable>` and calls `trackStatus()`, which resolves to the runtime implementation.

Example:

```java
List<Trackable> records = new ArrayList<>();
records.add(new RepairJob(...));
records.add(new RepairRecord(...));

for (Trackable item : records) {
    System.out.println(item.trackStatus());
}
```

This demonstrates runtime polymorphism because the actual object type determines the output.

### Abstraction
The controller layer exposes JSON endpoints while the service and repository layers hide database and business logic implementation details behind interfaces and focused classes.

## Invalid status handling

The allowed transitions are:

- RECEIVED -> IN_PROGRESS
- IN_PROGRESS -> READY_FOR_COLLECTION
- READY_FOR_COLLECTION -> COMPLETED

A request to move to the same status is treated as a harmless no-op. Any other transition throws `InvalidRepairStatusException`, which is caught in the service/API layer and returned as a 409 JSON error.

## Presentation in under three minutes

1. Start the app and show the dashboard.
2. Register a customer and device.
3. Create a repair job.
4. Change status from RECEIVED to IN_PROGRESS to READY_FOR_COLLECTION to COMPLETED.
5. Show the search feature and repair history.
6. Explain the custom exception and the OOP examples.

## Known limitations

- The project is intentionally small and does not include payments, authentication, SMS notifications, or analytics.
- Local PostgreSQL must be available and configured before the application can connect.

## Project structure

```text
tracker/
├── database/
│   └── schema.sql
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/devicerepair/
│   │   └── resources/
│   │       └── public/
│   └── test/
├── .env
├── .env.example
├── .gitignore
├── pom.xml
├── README.md
└── target/
```
