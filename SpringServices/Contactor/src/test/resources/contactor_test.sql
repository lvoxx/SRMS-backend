CREATE TABLE contactor (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contactor_type VARCHAR(50) NOT NULL,
    organization_name VARCHAR(100),
    fullname VARCHAR(150),
    
    -- Contact information
    phone_number VARCHAR(20) NOT NULL CHECK (phone_number ~ '^\+?[0-9]{7,15}$'),
    email VARCHAR(100) CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    
    address VARCHAR(200),
    rating VARCHAR(100),
    
    -- Common metadata
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

-- Add indexes for quick lookups
CREATE INDEX idx_contact_email ON contactor(email) WHERE email IS NOT NULL;
CREATE INDEX idx_contactor_type ON contactor(contactor_type);
CREATE INDEX idx_contactor_phone ON contactor(phone_number);
CREATE INDEX idx_contactor_organization ON contactor(organization_name) WHERE organization_name IS NOT NULL;
CREATE INDEX idx_contactor_name ON contactor(fullname) WHERE fullname IS NOT NULL;

-- Update trigger
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_contactor_modtime
BEFORE UPDATE ON contactor
FOR EACH ROW
EXECUTE FUNCTION update_modified_column();