# RepairTrack: Device Repair Tracker

A Java + Javalin application for managing repair customers, devices, jobs, costs, and payments.

## Problem and aim

Small repair shops often track jobs using paper notes, WhatsApp messages, and handwritten receipts. That causes lost records, bad status tracking, and confusion over which devices are waiting to be collected or paid for. This project provides a lightweight system for registering customers, recording devices, creating repair jobs, updating statuses, tracking repair costs and payments, and searching records.

## Objectives

- Register customers and their contact details.
- Record devices with type, brand, and model.
- Create, view, search, and manage repair jobs.
- Track the repair outcome as pending, fixed, or not fixed.
- Set a repair price and payment status when creating a job, then update it to paid or not paid from the selected repair panel.
- Track status across IN_PROGRESS -> READY_FOR_COLLECTION -> COMPLETED.
- Persist customers, devices, repair jobs, and status history in PostgreSQL using JDBC and PreparedStatement.
- Persist repair costs and payment records in PostgreSQL.
- Reject invalid transitions with a custom checked exception.

## Technologies

- Java 27
- Maven
- Javalin 5
- PostgreSQL JDBC driver
- Jackson JSON libraries
- Dotenv Java
- HTML, CSS, and vanilla JavaScript

## Prerequisites

- JDK 27 installed
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

PostgreSQL stores customers, devices, repair jobs, status history, repair costs, and payment records. Startup applies additive `IF NOT EXISTS` schema changes and does not drop existing tables or records.

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
Creates a repair job with its price and initial payment status.

Request body:

```json
{
  "customerId": 1,
  "deviceId": 1,
  "reportedFault": "Screen cracked",
  "repairNotes": "Customer reported a dropped phone",
  "repairCost": 120.00,
  "paymentStatus": "NOT_PAID"
}
```

### GET /api/repair-jobs
Lists all repair jobs.

### GET /api/repair-jobs/{id}
Gets a single repair job, status history, and payment summary by ID.

### PUT /api/repair-jobs/{id}/payment-status
Updates the selected job's payment status to `PAID` or `NOT_PAID`.

Request body:

```json
{
  "paymentStatus": "PAID"
}
```

### PUT /api/repair-jobs/{id}
Updates the reported fault and repair notes for the selected repair job.

Request body:

```json
{
  "reportedFault": "Charging port not working",
  "repairNotes": "Replace the charging port"
}
```

### PUT /api/repair-jobs/{id}/outcome
Saves whether the device was fixed. Final outcomes are available once the job is ready for collection.

Request body:

```json
{
  "repairOutcome": "FIXED"
}
```

### PUT /api/repair-jobs/{id}/cost
Sets or updates the repair cost. The cost cannot be set below the amount already paid.

Request body:

```json
{
  "repairCost": 120.00
}
```

### GET /api/repair-jobs/{id}/payments
Returns the repair cost, total paid, outstanding balance, and payment history.

### POST /api/repair-jobs/{id}/payments
Records a detailed payment transaction for integrations that need amount-level receipts. The main UI uses the repair job's `PAID` or `NOT_PAID` status.

Request body:

```json
{
  "amount": 45.50,
  "paymentMethod": "Mobile money",
  "paymentStatus": "PAID"
}
```

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
- payments: stores payment amount, method, status, and date linked to a repair job

The `payments` table links each payment to its repair job. The existing `repair_records` table continues to store status history; the API maps those rows directly instead of requiring an additional `RepairRecord` model class.

The schema uses primary keys, foreign keys, timestamps, and indexes.

## OOP concepts in the project

The six main domain classes and their responsibilities are:

- `Customer`: validates and stores customer name and phone number.
- `Device`: stores a device and its owning customer.
- `RepairJob`: stores the reported fault, repair notes, cost, and current status.
- `Payment`: validates and stores each payment amount, date, method, and status.
- `RepairManager`: coordinates repair operations and repository calls.
- `InvalidRepairStatusException`: reports status changes that break the allowed progression.

### Encapsulation
Model classes keep their fields private and expose validation through constructors and setters. Database access is kept out of the model layer.

### Inheritance
`InvalidRepairStatusException` extends `Exception` and carries the error message for invalid status transitions.

### Polymorphism
The `Trackable` interface is implemented by `RepairJob` and `Payment`. The application stores both in a `List<Trackable>` and calls `trackStatus()`, which resolves to the runtime implementation.

Example:

```java
List<Trackable> records = new ArrayList<>();
records.add(new RepairJob(...));
records.add(new Payment(...));

for (Trackable item : records) {
    System.out.println(item.trackStatus());
}
```

This demonstrates runtime polymorphism because the actual object type determines the output.

### Abstraction
The controller layer exposes JSON endpoints while the service and repository layers hide database and business logic implementation details behind interfaces and focused classes.

## Invalid status handling

The allowed transitions are:

- IN_PROGRESS -> READY_FOR_COLLECTION
- READY_FOR_COLLECTION -> COMPLETED

A request to move to the same status is treated as a harmless no-op. Any other transition throws `InvalidRepairStatusException`, which is caught in the service/API layer and returned as a 409 JSON error.

## Presentation in under three minutes

1. Start the app and show the dashboard.
2. Register a customer and device.
3. Create a repair job.
4. Set a repair cost and record a partial or full payment.
5. Change status from IN_PROGRESS to READY_FOR_COLLECTION to COMPLETED.
6. Show search, payment history, the outstanding balance, and repair history.
7. Explain the custom exception and the OOP examples.

## Known limitations

- The project is intentionally small and does not include authentication, SMS notifications, or analytics.
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
