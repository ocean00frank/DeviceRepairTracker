# RepairTrack: Device Repair Tracker

RepairTrack is a Java web app for repair shops. It keeps customer and device details, tracks each repair from **In progress** to **Ready for collection** and **Completed**, records whether the repair was fixed, and tracks the repair price and whether it has been paid.

## Requirements

- JDK 27
- Apache Maven
- PostgreSQL running locally
- Git

## Clone the project

Replace `<repository-url>` with the clone URL for your Git repository:

```powershell
git clone <repository-url>
cd <repository-folder>
cd tracker
```

The Maven project (`pom.xml`) is inside the `tracker` folder.

## Create the database

Create the database once in pgAdmin or `psql`:

```sql
CREATE DATABASE "DeviceRepair_db";
```

The app does not create the database itself. On startup, it runs `database/schema.sql` to create or update the tables and indexes. Its additive migrations preserve existing records.

The PostgreSQL user configured below needs permission to connect to the database and create or update objects in the `public` schema. If needed, grant access as a database administrator:

```sql
GRANT CONNECT ON DATABASE "DeviceRepair_db" TO your_user;
GRANT USAGE, CREATE ON SCHEMA public TO your_user;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO your_user;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO your_user;
```

## Configure the connection

From the `tracker` directory, create your local environment file:

```powershell
Copy-Item .env.example .env
```

Edit `.env` and enter the PostgreSQL username and password for your machine:

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=DeviceRepair_db
DB_USER=your_postgresql_username
DB_PASSWORD=your_postgresql_password
```

Keep `.env` private. It is ignored by Git; do not commit database credentials.

## Build and test

Run these commands from the `tracker` directory:

```powershell
mvn clean test
```

## Start the server

```powershell
mvn exec:java
```

When startup succeeds, open <http://localhost:7000/>. Check the database connection at <http://localhost:7000/api/health>.

If port 7000 is already in use, start the app on another port, for example:

```powershell
mvn exec:java "-Dexec.args=7001"
```

Then open <http://localhost:7001/> instead. Stop the server with `Ctrl+C` in the terminal where Maven is running.

## Using the app

1. Register a customer and their device.
2. Create a repair job, entering its price and choosing **Paid** or **Not paid**.
3. Select a job in the repair list to update its progress or payment status.
4. Update the repair result to **Fixed** or **Not fixed** when appropriate.
5. Search for repairs by customer, device, fault, or job ID.

Payment status on a repair job is `PAID` or `NOT_PAID`. Detailed payment transaction endpoints are also available for integrations.

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
Saves whether the device was fixed. The result can be updated at any repair stage.

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
4. Set the repair price and initial payment state when creating the job.
5. Change status from IN_PROGRESS to READY_FOR_COLLECTION to COMPLETED.
6. Update payment state between Paid and Not paid from the selected repair panel.
7. Show search, fixed/not-fixed result, and repair history.
8. Explain the custom exception and the OOP examples.

## Known limitations

- The project is intentionally small and does not include authentication, SMS notifications, or analytics.
- Local PostgreSQL must be available and configured before the application can connect.

## Project structure

```text
tracker/
├── .vscode/                           # Editor-specific workspace configuration
│   └── settings.json                  # VS Code Java runtime and project settings
├── database/                          # Database schema and migration scripts
│   └── schema.sql                     # PostgreSQL tables, constraints, indexes, and migrations
├── src/                               # Application source code and tests
│   ├── main/                           # Files required to run the application
│   │   ├── java/com/devicerepair/      # Java application packages
│   │   │   ├── api/                    # HTTP endpoints
│   │   │   │   └── ApiController.java # HTTP routes and request/response handling
│   │   │   ├── database/               # Database connection setup
│   │   │   │   └── DatabaseConfig.java # PostgreSQL connection and schema startup
│   │   │   ├── exception/              # Application-specific errors
│   │   │   │   └── InvalidRepairStatusException.java # Invalid repair transition error
│   │   │   ├── model/                  # Domain objects and shared types
│   │   │   │   ├── Customer.java      # Customer details and validation
│   │   │   │   ├── Device.java        # Device details and owner
│   │   │   │   ├── Payment.java       # Individual payment transaction
│   │   │   │   ├── RepairJob.java     # Repair details, price, payment state, and progress
│   │   │   │   ├── RepairStatus.java  # Allowed repair workflow statuses
│   │   │   │   └── Trackable.java     # Shared status-tracking interface
│   │   │   ├── repository/             # SQL data access classes
│   │   │   │   ├── CustomerRepository.java # Customer SQL operations
│   │   │   │   ├── DeviceRepository.java   # Device SQL operations
│   │   │   │   ├── PaymentRepository.java  # Payment SQL operations and balances
│   │   │   │   ├── RepairHistoryRepository.java # Reads repair status history
│   │   │   │   └── RepairJobRepository.java # Repair-job SQL operations
│   │   │   ├── service/                # Repair workflow and business rules
│   │   │   │   └── RepairManager.java # Validates and coordinates repair operations
│   │   │   └── Main.java              # App entry point; configures and starts Javalin
│   │   └── resources/public/           # Browser interface files
│   │       ├── css/style.css          # Web app layout and visual styling
│   │       ├── index.html             # Dashboard and repair forms
│   │       └── js/app.js              # Browser logic and backend API calls
│   └── test/java/com/devicerepair/     # Automated Java tests
│       ├── PaymentTest.java           # Payment and payment-state validation tests
│       └── RepairJobTest.java          # Repair model, progress, and serialization tests
├── .env                               # Local PostgreSQL credentials; do not commit
├── .env.example                       # Safe template for creating .env
├── .gitignore                         # Files and local settings excluded from Git
├── pom.xml                            # Maven dependencies, Java version, and build plugins
├── README.md                          # Setup, run, API, and project documentation
└── target/                            # Generated Maven build output; recreated by Maven
```

The main code is under `src/main`; automated tests are under `src/test`. The `target` directory is generated when Maven builds the project and does not need to be edited. The `.vscode/settings.json` Java runtime path may need adjusting to match the JDK installation on another computer.
