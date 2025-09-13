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

INSERT INTO contactor (
    contactor_type, organization_name, fullname,
    phone_number, email, address, notes
) VALUES
-- Food suppliers
('supplier', 'Fresh Farm Produce', 'John Anderson', '+18005551234', 'john@freshfarm.com',
    '100 Farm Lane, Agritown, CA, 90210',
    'Product categories: vegetables, fruits. Delivery days: tue, thu.'),
('supplier', 'Quality Meats Inc.', 'John Doe', '+18005551235', 'orders@qualitymeats.com',
    '200 Butcher Rd, Carniville, CA, 90211',
    'Product categories: beef, poultry. Payment terms: net 15.'),
('supplier', 'Ocean Fresh Seafood', 'Michael Johnny', '+18005551238', 'orders@oceanfresh.com',
    '500 Harbor Way, Coastal, CA, 90214',
    'Product categories: fish, shellfish. Order minimum: $200.'),
('supplier', 'Golden Grains Bakery', 'Alice Baker', '+18005552101', 'orders@goldengrains.com',
    '101 Flour Mill Rd, Bakerville, CA, 90216',
    'Product categories: bread, pastries. Delivery days: mon, wed, fri.'),
('supplier', 'Dairy Valley Farms', 'John Farmer', '+18005552102', 'sales@dairyvalley.com',
    '202 Creamery Lane, Milkton, CA, 90217',
    'Product categories: milk, cheese, yogurt. Organic products available.'),
('supplier', 'Pasta Primo', 'Maria Rossi', '+18005552103', 'contactor@pastaprimo.com',
    '303 Noodle Blvd, Carbville, CA, 90218',
    'Product categories: pasta, risotto. Imported goods.'),
('supplier', 'Spice World', 'Samir Gupta', '+18005552104', 'info@spiceworld.com',
    '404 Pepper St, Flavortown, CA, 90219',
    'Product categories: spices, herbs. Minimum order: $50.'),
('supplier', 'Catch of the Day Seafood', 'Carlos Santiago', '+18005552105', 'orders@catchoftheday.com',
    '505 Fisherman''s Wharf, Harborville, CA, 90220',
    'Product categories: fish, shellfish. Daily specials available.'),
('supplier', 'Green Leaf Produce', 'Susan Greenfield', '+18005552106', 'sales@greenleaf.com',
    '606 Organic Way, Farmville, CA, 90221',
    'Product categories: vegetables, fruits. Certified organic.'),
('supplier', 'Prime Cuts Butchery', 'David Miller', '+18005552107', 'orders@primecuts.com',
    '707 Steak Ave, Meatville, CA, 90222',
    'Product categories: beef, pork, lamb. Dry aged options available.'),
('supplier', 'Beverage Distributors Inc.', 'Robert King', '+18005552108', 'sales@bevdist.com',
    '808 Soda Blvd, Drinkton, CA, 90223',
    'Product categories: soft drinks, juices. Emergency delivery available.'),

-- Delivery services
('deliverer', 'QuickDrop Delivery', 'Mike Johnson', '+18005551236', 'mike@quickdrop.com',
    '300 Speedy Ave, Fastville, CA, 90212',
    'Vehicle type: refrigerated truck. Coverage area: downtown, westside.'),
('deliverer', 'Bike Couriers LLC', 'Tom Wilson', '+18005551239', 'tom@bikecouriers.com',
    '600 Green St, Eco City, CA, 90215',
    'Vehicle type: cargo bikes. Rush hours: 11:00-13:00, 17:00-19:00.'),
('deliverer', 'Metro Rapid Delivery', 'Mike Swift', '+18005552109', 'dispatch@metrorapid.com',
    '909 Express Lane, Quicktown, CA, 90224',
    'Vehicle type: vans. Service hours: 6am-10pm.'),
('deliverer', 'Eco-Friendly Couriers', 'Lily Chen', '+18005552110', 'contactor@ecocouriers.com',
    '1010 Green Way, Eco City, CA, 90225',
    'Vehicle type: electric bikes. Carbon neutral service.'),
('deliverer', 'Overnight Logistics', 'Frank Night', '+18005552111', 'support@overnightlog.com',
    '1111 Moonlight Dr, Nightville, CA, 90226',
    'Vehicle type: trucks. Overnight service available.'),
('deliverer', 'City Center Couriers', 'Jessica Urban', '+18005552112', 'info@citycentercouriers.com',
    '1212 Downtown Plaza, Metroville, CA, 90227',
    'Vehicle type: scooters. No rush hour surcharge.'),
('deliverer', 'Reliable Runners', 'Tom Trusty', '+18005552113', 'book@reliablerunners.com',
    '1313 Dependable St, Trustville, CA, 90228',
    'Vehicle type: vans. Full insurance coverage.'),
('deliverer', 'Bulk Transport Co.', 'Bill Hauler', '+18005552114', 'sales@bulktransport.com',
    '1414 Heavy Haul Rd, Industrial Park, CA, 90229',
    'Vehicle type: refrigerated trucks. Pallet capacity: 20.'),

-- Local groceries
('grocery', 'Corner Market', 'Lisa Chen', '+18005551237', 'lisa@cornermarket.com',
    '400 Main St, Localville, CA, 90213',
    'Specialty: organic products. Discount: 10% for restaurants.'),
('grocery', 'Sunshine Market', 'Ray Sunshine', '+18005552115', 'manager@sunshinemarket.com',
    '1515 Bright Ave, Sunnyside, CA, 90230',
    'Specialty: local produce. Discount: 5% for restaurants.'),
('grocery', 'Corner Pantry', 'Pat Smith', '+18005552116', 'orders@cornerpantry.com',
    '1616 Cross St, Neighborhood, CA, 90231',
    'Specialty: emergency supplies. Open 24 hours.'),
('grocery', 'Gourmet Grocers', 'Pierre Dubois', '+18005552117', 'sales@gourmetgrocers.com',
    '1717 Fancy Blvd, Upscale, CA, 90232',
    'Specialty: imported foods. Tasting samples available.'),
('grocery', 'Budget Foods', 'Penny Wise', '+18005552118', 'contactor@budgetfoods.com',
    '1818 Savings St, Thrifty, CA, 90233',
    'Specialty: bulk purchases. Case discounts available.'),
('grocery', 'International Market', 'Sofia Garcia', '+18005552119', 'info@intlmarket.com',
    '1919 World Way, Cosmopolitan, CA, 90234',
    'Specialty: ethnic ingredients. Languages spoken: Spanish, Mandarin, Arabic.'),
('grocery', 'Health Nut Grocery', 'Adam Wellness', '+18005552120', 'orders@healthnut.com',
    '2020 Wellness Dr, Vitality, CA, 90235',
    'Specialty: organic supplements. Nutrition consultant available.');