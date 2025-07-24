CREATE TYPE contact_type AS ENUM (
    'customer',
    'supplier',
    'deliverer',
    'grocery',
    'other'
);

CREATE TABLE contact (
    contact_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contact_type contact_type NOT NULL,
    organization_name VARCHAR(100),
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    
    -- Contact information
    phone_number VARCHAR(20) NOT NULL CHECK (phone_number ~ '^\+?[0-9]{7,15}$'),
    email VARCHAR(100) CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    address JSONB,
    
    -- Type-specific fields (using JSONB for flexibility)
    attributes JSONB,
    
    -- Common metadata
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for quick lookups
CREATE INDEX idx_contact_type ON contact(contact_type);
CREATE INDEX idx_contact_phone ON contact(phone_number);
CREATE INDEX idx_contact_organization ON contact(organization_name) WHERE organization_name IS NOT NULL;
CREATE INDEX idx_contact_name ON contact(last_name, first_name) WHERE last_name IS NOT NULL;

-- Update trigger
CREATE OR REPLACE FUNCTION update_modified_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_contact_modtime
BEFORE UPDATE ON contact
FOR EACH ROW
EXECUTE FUNCTION update_modified_column();

-- Insert suppliers
INSERT INTO contact (
    contact_type, organization_name, first_name, last_name, 
    phone_number, email, address, attributes
) VALUES
-- Food suppliers
('supplier', 'Fresh Farm Produce', 'John', 'Anderson', 
    '+18005551234', 'john@freshfarm.com', 
    '{"street": "100 Farm Lane", "city": "Agritown", "state": "CA", "zip": "90210"}', 
    '{"product_categories": ["vegetables", "fruits"], "delivery_days": ["tue", "thu"]}'),
    
('supplier', 'Quality Meats Inc.', NULL, NULL, 
    '+18005551235', 'orders@qualitymeats.com', 
    '{"street": "200 Butcher Rd", "city": "Carniville", "state": "CA", "zip": "90211"}', 
    '{"product_categories": ["beef", "poultry"], "payment_terms": "net 15"}'),

-- Delivery services
('deliverer', 'QuickDrop Delivery', 'Mike', 'Johnson', 
    '+18005551236', 'mike@quickdrop.com', 
    '{"street": "300 Speedy Ave", "city": "Fastville", "state": "CA", "zip": "90212"}', 
    '{"vehicle_type": "refrigerated truck", "coverage_area": ["downtown", "westside"]}'),

-- Local groceries
('grocery', 'Corner Market', 'Lisa', 'Chen', 
    '+18005551237', 'lisa@cornermarket.com', 
    '{"street": "400 Main St", "city": "Localville", "state": "CA", "zip": "90213"}', 
    '{"specialty": "organic products", "discount": "10% for restaurants"}'),

('supplier', 'Ocean Fresh Seafood', NULL, NULL, 
    '+18005551238', 'orders@oceanfresh.com', 
    '{"street": "500 Harbor Way", "city": "Coastal", "state": "CA", "zip": "90214"}', 
    '{"product_categories": ["fish", "shellfish"], "order_minimum": 200}'),

('deliverer', 'Bike Couriers LLC', 'Tom', 'Wilson', 
    '+18005551239', 'tom@bikecouriers.com', 
    '{"street": "600 Green St", "city": "Eco City", "state": "CA", "zip": "90215"}', 
    '{"vehicle_type": "cargo bikes", "rush_hours": ["11:00-13:00", "17:00-19:00"]}');

-- Additional Suppliers (8 records)
INSERT INTO contact (contact_type, organization_name, phone_number, email, address, attributes) VALUES
('supplier', 'Golden Grains Bakery', '+18005552101', 'orders@goldengrains.com', 
 '{"street": "101 Flour Mill Rd", "city": "Bakerville", "state": "CA", "zip": "90216"}', 
 '{"product_categories": ["bread", "pastries"], "delivery_days": ["mon", "wed", "fri"]}'),

('supplier', 'Dairy Valley Farms', '+18005552102', 'sales@dairyvalley.com', 
 '{"street": "202 Creamery Lane", "city": "Milkton", "state": "CA", "zip": "90217"}', 
 '{"product_categories": ["milk", "cheese", "yogurt"], "organic": true}'),

('supplier', 'Pasta Primo', '+18005552103', 'contact@pastaprimo.com', 
 '{"street": "303 Noodle Blvd", "city": "Carbville", "state": "CA", "zip": "90218"}', 
 '{"product_categories": ["pasta", "risotto"], "imported": true}'),

('supplier', 'Spice World', '+18005552104', 'info@spiceworld.com', 
 '{"street": "404 Pepper St", "city": "Flavortown", "state": "CA", "zip": "90219"}', 
 '{"product_categories": ["spices", "herbs"], "minimum_order": 50}'),

('supplier', 'Catch of the Day Seafood', '+18005552105', 'orders@catchoftheday.com', 
 '{"street": "505 Fisherman''s Wharf", "city": "Harborville", "state": "CA", "zip": "90220"}', 
 '{"product_categories": ["fish", "shellfish"], "daily_specials": true}'),

('supplier', 'Green Leaf Produce', '+18005552106', 'sales@greenleaf.com', 
 '{"street": "606 Organic Way", "city": "Farmville", "state": "CA", "zip": "90221"}', 
 '{"product_categories": ["vegetables", "fruits"], "certified_organic": true}'),

('supplier', 'Prime Cuts Butchery', '+18005552107', 'orders@primecuts.com', 
 '{"street": "707 Steak Ave", "city": "Meatville", "state": "CA", "zip": "90222"}', 
 '{"product_categories": ["beef", "pork", "lamb"], "dry_aged": true}'),

('supplier', 'Beverage Distributors Inc.', '+18005552108', 'sales@bevdist.com', 
 '{"street": "808 Soda Blvd", "city": "Drinkton", "state": "CA", "zip": "90223"}', 
 '{"product_categories": ["soft drinks", "juices"], "emergency_delivery": true}'),

-- Delivery Services (6 records)
('deliverer', 'Metro Rapid Delivery', '+18005552109', 'dispatch@metrorapid.com', 
 '{"street": "909 Express Lane", "city": "Quicktown", "state": "CA", "zip": "90224"}', 
 '{"vehicle_type": "vans", "service_hours": "6am-10pm"}'),

('deliverer', 'Eco-Friendly Couriers', '+18005552110', 'contact@ecocouriers.com', 
 '{"street": "1010 Green Way", "city": "Eco City", "state": "CA", "zip": "90225"}', 
 '{"vehicle_type": "electric bikes", "carbon_neutral": true}'),

('deliverer', 'Overnight Logistics', '+18005552111', 'support@overnightlog.com', 
 '{"street": "1111 Moonlight Dr", "city": "Nightville", "state": "CA", "zip": "90226"}', 
 '{"vehicle_type": "trucks", "overnight_service": true}'),

('deliverer', 'City Center Couriers', '+18005552112', 'info@citycentercouriers.com', 
 '{"street": "1212 Downtown Plaza", "city": "Metroville", "state": "CA", "zip": "90227"}', 
 '{"vehicle_type": "scooters", "rush_hour_surcharge": false}'),

('deliverer', 'Reliable Runners', '+18005552113', 'book@reliablerunners.com', 
 '{"street": "1313 Dependable St", "city": "Trustville", "state": "CA", "zip": "90228"}', 
 '{"vehicle_type": "vans", "insurance_coverage": "full"}'),

('deliverer', 'Bulk Transport Co.', '+18005552114', 'sales@bulktransport.com', 
 '{"street": "1414 Heavy Haul Rd", "city": "Industrial Park", "state": "CA", "zip": "90229"}', 
 '{"vehicle_type": "refrigerated trucks", "pallet_capacity": 20}'),

-- Local Groceries (6 records)
('grocery', 'Sunshine Market', '+18005552115', 'manager@sunshinemarket.com', 
 '{"street": "1515 Bright Ave", "city": "Sunnyside", "state": "CA", "zip": "90230"}', 
 '{"specialty": "local produce", "discount": "5% for restaurants"}'),

('grocery', 'Corner Pantry', '+18005552116', 'orders@cornerpantry.com', 
 '{"street": "1616 Cross St", "city": "Neighborhood", "state": "CA", "zip": "90231"}', 
 '{"specialty": "emergency supplies", "open_24_hours": true}'),

('grocery', 'Gourmet Grocers', '+18005552117', 'sales@gourmetgrocers.com', 
 '{"street": "1717 Fancy Blvd", "city": "Upscale", "state": "CA", "zip": "90232"}', 
 '{"specialty": "imported foods", "tasting_samples": true}'),

('grocery', 'Budget Foods', '+18005552118', 'contact@budgetfoods.com', 
 '{"street": "1818 Savings St", "city": "Thrifty", "state": "CA", "zip": "90233"}', 
 '{"specialty": "bulk purchases", "case_discounts": true}'),

('grocery', 'International Market', '+18005552119', 'info@intlmarket.com', 
 '{"street": "1919 World Way", "city": "Cosmopolitan", "state": "CA", "zip": "90234"}', 
 '{"specialty": "ethnic ingredients", "languages_spoken": ["Spanish", "Mandarin", "Arabic"]}'),

('grocery', 'Health Nut Grocery', '+18005552120', 'orders@healthnut.com', 
 '{"street": "2020 Wellness Dr", "city": "Vitality", "state": "CA", "zip": "90235"}', 
 '{"specialty": "organic supplements", "nutrition_consultant": true}');