CREATE TABLE IF NOT EXISTS customers (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(200) NOT NULL,
    phone_number VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS devices (
    id SERIAL PRIMARY KEY,
    customer_id INT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    device_type VARCHAR(100) NOT NULL,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS repair_jobs (
    id SERIAL PRIMARY KEY,
    customer_id INT NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    device_id INT NOT NULL REFERENCES devices(id) ON DELETE RESTRICT,
    reported_fault TEXT NOT NULL,
    repair_notes TEXT,
    status VARCHAR(40) NOT NULL DEFAULT 'RECEIVED' CHECK (status IN ('RECEIVED','IN_PROGRESS','READY_FOR_COLLECTION','COMPLETED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS repair_records (
    id SERIAL PRIMARY KEY,
    repair_job_id INT NOT NULL REFERENCES repair_jobs(id) ON DELETE CASCADE,
    previous_status VARCHAR(40),
    new_status VARCHAR(40) NOT NULL,
    notes TEXT,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone_number);
CREATE INDEX IF NOT EXISTS idx_devices_customer_id ON devices(customer_id);
CREATE INDEX IF NOT EXISTS idx_repair_jobs_customer_id ON repair_jobs(customer_id);
CREATE INDEX IF NOT EXISTS idx_repair_jobs_status ON repair_jobs(status);
CREATE INDEX IF NOT EXISTS idx_repair_jobs_device_id ON repair_jobs(device_id);
CREATE INDEX IF NOT EXISTS idx_repair_records_job_id ON repair_records(repair_job_id);
