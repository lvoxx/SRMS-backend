CREATE TABLE customer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(50) NOT NULL CHECK (trim(first_name) != ''),
    last_name VARCHAR(50) NOT NULL CHECK (trim(last_name) != ''),
    phone_number VARCHAR(20) NOT NULL CHECK (
        phone_number ~ '^\+?[0-9]{7,15}$'
    ),
    email VARCHAR(100) CHECK (
        email IS NULL OR 
        email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
    ),
    dietary_restrictions TEXT[],
    allergies TEXT[],
    is_regular BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Add index for quick phone lookups (common for reservations)
CREATE INDEX idx_customer_phone ON customer(phone_number);

-- Add trigger to automatically update updated_at
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_customer_modtime
BEFORE UPDATE ON customer
FOR EACH ROW
EXECUTE FUNCTION update_modified_column();