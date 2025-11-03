-- enable uuid generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Table warehouse
CREATE TABLE IF NOT EXISTS warehouse (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_name TEXT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    min_quantity INTEGER NOT NULL DEFAULT 0 CHECK (min_quantity >= 0),
    contactor_id UUID, -- optional
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_updated_by VARCHAR(36), -- will be wrote at the lastest update
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT product_name_not_empty CHECK (char_length(btrim(product_name)) > 0)
);

-- index to speed up typical queries (e.g., list active items)
CREATE INDEX IF NOT EXISTS idx_warehouse_is_deleted ON warehouse (is_deleted);
CREATE INDEX IF NOT EXISTS idx_warehouse_product_name ON warehouse (lower(product_name));

-- Table warehouse_history (immutable)
CREATE TABLE IF NOT EXISTS warehouse_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    warehouse_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    type TEXT NOT NULL CHECK (type IN ('import', 'export')),
    updated_by VARCHAR(36) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_warehouse_history_warehouse_id ON warehouse_history (warehouse_id);

-- Prevent updates/deletes on warehouse_history at DB level
CREATE OR REPLACE FUNCTION prevent_modify_warehouse_history() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'warehouse_history is immutable: updates/deletes are not allowed';
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_update_delete_history ON warehouse_history;
CREATE TRIGGER trg_prevent_update_delete_history
    BEFORE UPDATE OR DELETE ON warehouse_history
    FOR EACH ROW EXECUTE FUNCTION prevent_modify_warehouse_history();

-- ============================================
-- TRIGGER: Prevent manual quantity updates
-- ============================================
CREATE OR REPLACE FUNCTION prevent_manual_quantity_update() RETURNS trigger AS $$
BEGIN
    -- Chỉ cho phép thay đổi quantity nếu có flag đặc biệt từ trigger khác
    IF NEW.quantity IS DISTINCT FROM OLD.quantity THEN
        -- Kiểm tra nếu update đến từ system (có thể dùng session variable hoặc check context)
        IF current_setting('app.allow_quantity_update', true) IS NULL 
           OR current_setting('app.allow_quantity_update', true) != 'true' THEN
            RAISE EXCEPTION 'Cannot update quantity directly. Use warehouse_history to track inventory changes.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_prevent_manual_quantity ON warehouse;
CREATE TRIGGER trg_prevent_manual_quantity
    BEFORE UPDATE ON warehouse
    FOR EACH ROW EXECUTE FUNCTION prevent_manual_quantity_update();

-- ============================================
-- TRIGGER: Auto-update quantity from warehouse_history
-- ============================================
CREATE OR REPLACE FUNCTION update_warehouse_quantity() RETURNS trigger AS $$
DECLARE
    v_total_nhap INTEGER;
    v_total_xuat INTEGER;
    v_new_quantity INTEGER;
BEGIN
    -- Sum import and export from warehouse_history
    SELECT 
        COALESCE(SUM(CASE WHEN type = 'import' THEN quantity ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN type = 'export' THEN quantity ELSE 0 END), 0)
    INTO v_total_nhap, v_total_xuat
    FROM warehouse_history
    WHERE warehouse_id = NEW.warehouse_id;
    
    v_new_quantity := v_total_nhap - v_total_xuat;
    
    -- Kiểm tra quantity không âm
    IF v_new_quantity < 0 THEN
        RAISE EXCEPTION 'Insufficient inventory. Cannot process this transaction.';
    END IF;
    
    -- Cho phép update quantity thông qua session variable
    PERFORM set_config('app.allow_quantity_update', 'true', true);
    
    -- Update quantity trong warehouse
    UPDATE warehouse 
    SET quantity = v_new_quantity,
        updated_at = now(),
        last_updated_by = NEW.updated_by
    WHERE id = NEW.warehouse_id;
    
    -- Reset session variable
    PERFORM set_config('app.allow_quantity_update', NULL, true);
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_update_warehouse_quantity ON warehouse_history;
CREATE TRIGGER trg_update_warehouse_quantity
    AFTER INSERT ON warehouse_history
    FOR EACH ROW EXECUTE FUNCTION update_warehouse_quantity();

-- ============================================
-- TRIGGER: Maintain updated_at and version
-- ============================================
CREATE OR REPLACE FUNCTION trg_set_updated_at_warehouse() RETURNS trigger AS $$
BEGIN
    -- Chỉ update version, updated_at đã được set trong trigger update_warehouse_quantity
    IF NEW.version IS NOT DISTINCT FROM OLD.version THEN
        NEW.version = OLD.version + 1;
    END IF;
    IF NEW.updated_at IS NOT DISTINCT FROM OLD.updated_at THEN
        NEW.updated_at = now();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_warehouse_updated_at ON warehouse;
CREATE TRIGGER trg_warehouse_updated_at
    BEFORE UPDATE ON warehouse
    FOR EACH ROW EXECUTE FUNCTION trg_set_updated_at_warehouse();

-- ============================================
-- SAMPLE DATA
-- ============================================

-- Insert sample products into warehouse
INSERT INTO warehouse (product_name, quantity, min_quantity, contactor_id) VALUES
-- Flour and baking ingredients
('00 Flour', 500, 100, NULL),
('Pizza Flour', 300, 80, NULL),
('Dry Yeast', 50, 10, NULL),
('Fresh Yeast', 20, 5, NULL),
('Extra Virgin Olive Oil', 100, 20, NULL),
('Vegetable Oil', 80, 15, NULL),
('Sea Salt', 30, 5, NULL),
('White Sugar', 40, 8, NULL),

-- Cheeses and dairy
('Mozzarella Cheese', 200, 50, NULL),
('Parmesan Cheese', 80, 20, NULL),
('Cheddar Cheese', 60, 15, NULL),
('Gorgonzola Cheese', 30, 8, NULL),
('Ricotta Cheese', 40, 10, NULL),
('Fresh Cream', 25, 5, NULL),

-- Tomato sauces and seasonings
('Italian Tomato Sauce', 150, 30, NULL),
('Crushed Tomatoes', 120, 25, NULL),
('Hot Sauce', 60, 12, NULL),
('Garlic Sauce', 45, 10, NULL),
('Honey Mustard', 35, 8, NULL),
('Mayonnaise', 40, 8, NULL),
('BBQ Sauce', 55, 12, NULL),
('Minced Garlic', 25, 5, NULL),
('Onion', 80, 15, NULL),
('Green Bell Pepper', 60, 12, NULL),
('Red Bell Pepper', 55, 12, NULL),
('Yellow Bell Pepper', 45, 10, NULL),

-- Meats and seafood
('Pepperoni', 120, 25, NULL),
('Italian Sausage', 90, 18, NULL),
('Ham', 70, 15, NULL),
('Ground Beef', 85, 20, NULL),
('Shredded Chicken', 75, 15, NULL),
('Regular Sausage', 65, 12, NULL),
('Fresh Shrimp', 40, 8, NULL),
('Fresh Squid', 35, 7, NULL),
('Smoked Salmon', 25, 5, NULL),
('Bacon', 50, 10, NULL),

-- Vegetables and herbs
('Fresh Mushrooms', 45, 10, NULL),
('Black Olives', 35, 8, NULL),
('Green Olives', 30, 8, NULL),
('Sweet Corn', 40, 8, NULL),
('Canned Pineapple', 35, 7, NULL),
('Spinach', 25, 5, NULL),
('Cherry Tomatoes', 30, 6, NULL),
('Fresh Basil', 20, 4, NULL),
('Dried Oregano', 15, 3, NULL),
('Rosemary', 12, 3, NULL),
('Jalapeno Peppers', 18, 4, NULL),

-- Pizza bases and accessories
('Pizza Base Size S', 200, 50, NULL),
('Pizza Base Size M', 180, 45, NULL),
('Pizza Base Size L', 160, 40, NULL),
('Pizza Base Size XL', 140, 35, NULL),
('Thin Crispy Base', 120, 30, NULL),
('Chicago Deep Dish Base', 80, 20, NULL),

-- Beverages and accessories
('Coca Cola Can', 300, 60, NULL),
('Pepsi Can', 280, 55, NULL),
('Sprite Can', 250, 50, NULL),
('Bottled Water', 400, 80, NULL),
('Heineken Beer', 150, 30, NULL),
('Tiger Beer', 140, 28, NULL),
('Italian Red Wine', 60, 12, NULL),
('White Wine', 55, 12, NULL),

-- Side dishes
('French Fries', 120, 25, NULL),
('Onion Rings', 80, 16, NULL),
('Chicken Nuggets', 90, 18, NULL),
('BBQ Chicken Wings', 70, 14, NULL),
('Caesar Salad', 40, 8, NULL),
('Italian Mixed Salad', 35, 7, NULL),

-- Consumables
('Pizza Box Size S', 500, 100, NULL),
('Pizza Box Size M', 450, 90, NULL),
('Pizza Box Size L', 400, 80, NULL),
('Pizza Box Size XL', 350, 70, NULL),
('Paper Bags', 800, 150, NULL),
('Paper Napkins', 1000, 200, NULL),
('Pizza Cutter', 15, 3, NULL),
('Pizza Serving Spatula', 12, 3, NULL);

-- Insert sample warehouse history records
INSERT INTO warehouse_history (warehouse_id, quantity, type, updated_by) VALUES
-- Initial stock intake for flour
((SELECT id FROM warehouse WHERE product_name = '00 Flour'), 500, 'import', '2c0615e4-1e0c-43f1-b9d9-2e652a22544a'),
((SELECT id FROM warehouse WHERE product_name = 'Pizza Flour'), 300, 'import', 'c54b2a8f-8d9e-4e8c-85f0-6c9b3d07e2c1'),
((SELECT id FROM warehouse WHERE product_name = 'Dry Yeast'), 50, 'import', 'b6a8e3d2-5c4a-4f6b-871d-1e3c2f0b9a78'),

-- Initial stock intake for cheeses
((SELECT id FROM warehouse WHERE product_name = 'Mozzarella Cheese'), 200, 'import', '7f1d4b6c-3a9e-4d5f-9e8c-0a2b1c3d4e5f'),
((SELECT id FROM warehouse WHERE product_name = 'Parmesan Cheese'), 80, 'import', 'a3e7f9b1-6d4c-4e8a-b0f2-5d1c9e8b7a6f'),
((SELECT id FROM warehouse WHERE product_name = 'Cheddar Cheese'), 60, 'import', 'd1c9e8b7-a6f0-4c3e-9b2a-1d5f4e8c3b7a'),

-- Initial stock intake for sauces
((SELECT id FROM warehouse WHERE product_name = 'Italian Tomato Sauce'), 150, 'import', 'e4f0a2b1-c3d4-4e5f-9e8c-7f1d4b6c3a9e'),
((SELECT id FROM warehouse WHERE product_name = 'Crushed Tomatoes'), 120, 'import', '1c3d4e5f-9e8c-4b6c-3a9e-7f1d4b6c3a9e'),
((SELECT id FROM warehouse WHERE product_name = 'BBQ Sauce'), 55, 'import', '9b2a1d5f-4e8c-4b7a-6f0a-3e7f9b16d4c3'),

-- Initial stock intake for meats
((SELECT id FROM warehouse WHERE product_name = 'Pepperoni'), 120, 'import', '3f5e9d2a-1c8b-4a7f-9d4e-6b0c5a1f2e3d'),
((SELECT id FROM warehouse WHERE product_name = 'Italian Sausage'), 90, 'import', '6b0c5a1f-2e3d-4c8b-9a7f-3f5e9d2a1c8b'),
((SELECT id FROM warehouse WHERE product_name = 'Ham'), 70, 'import', '9a7f3f5e-9d2a-4c8b-6b0c-5a1f2e3d4c8b'),

-- Initial stock intake for vegetables
((SELECT id FROM warehouse WHERE product_name = 'Fresh Mushrooms'), 45, 'import', '8e6a1f3d-5c7b-4d9a-b0e2-1c4f9b8d2a5e'),
((SELECT id FROM warehouse WHERE product_name = 'Black Olives'), 35, 'import', '1c4f9b8d-2a5e-4d9a-8e6a-1f3d5c7b4d9a'),
((SELECT id FROM warehouse WHERE product_name = 'Green Bell Pepper'), 60, 'import', '5c7b4d9a-8e6a-4f3d-1c4f-9b8d2a5e1f3d'),

-- Initial stock intake for pizza bases
((SELECT id FROM warehouse WHERE product_name = 'Pizza Base Size M'), 180, 'import', 'd4e0f1a2-b3c4-4d5e-9f0a-1b2c3d4e5f6a'),
((SELECT id FROM warehouse WHERE product_name = 'Pizza Base Size L'), 160, 'import', '1b2c3d4e-5f6a-4d5e-d4e0-f1a2b3c4d5e9'),
((SELECT id FROM warehouse WHERE product_name = 'Thin Crispy Base'), 120, 'import', '9f0a1b2c-3d4e-4d5e-d4e0-f1a2b3c4d5e9'),

-- Initial stock intake for beverages
((SELECT id FROM warehouse WHERE product_name = 'Coca Cola Can'), 300, 'import', '6c5a4d3e-2b1a-4f0e-8d7c-9b6a5e4d3c2b'),
((SELECT id FROM warehouse WHERE product_name = 'Bottled Water'), 400, 'import', '9b6a5e4d-3c2b-4f0e-6c5a-4d3e2b1a4f0e'),
((SELECT id FROM warehouse WHERE product_name = 'Heineken Beer'), 150, 'import', '2b1a4f0e-8d7c-4d3e-9b6a-5e4d3c2b1a4f'),

-- Export transactions for daily operations
((SELECT id FROM warehouse WHERE product_name = 'Mozzarella Cheese'), 30, 'export', '8a2c1d4e-5f6b-4c7d-9e0f-1a3b5c7d9e2f'),
((SELECT id FROM warehouse WHERE product_name = 'Italian Tomato Sauce'), 20, 'export', '1a3b5c7d-9e2f-4c7d-8a2c-1d4e5f6b4c7d'),
((SELECT id FROM warehouse WHERE product_name = 'Pepperoni'), 15, 'export', '5f6b4c7d-9e0f-4e1a-3b5c-7d9e2f8a2c1d'),
((SELECT id FROM warehouse WHERE product_name = 'Pizza Base Size M'), 25, 'export', 'c3d4e5f6-a7b8-4c9d-9e0f-1a2b3c4d5e6f'),
((SELECT id FROM warehouse WHERE product_name = 'Fresh Mushrooms'), 10, 'export', '1a2b3c4d-5e6f-4c9d-c3d4-e5f6a7b84c9d'),
((SELECT id FROM warehouse WHERE product_name = 'Green Bell Pepper'), 12, 'export', 'a7b84c9d-9e0f-4e1a-2b3c-4d5e6fc3d4e5'),
((SELECT id FROM warehouse WHERE product_name = 'Coca Cola Can'), 45, 'export', '7b8c9d0e-1f2a-4b3c-5d4e-6f7a8b9c0d1e'),
((SELECT id FROM warehouse WHERE product_name = '00 Flour'), 80, 'export', '6f7a8b9c-0d1e-4b3c-7b8c-9d0e1f2a4b3c'),

-- Additional import transactions for restocking
((SELECT id FROM warehouse WHERE product_name = 'Mozzarella Cheese'), 50, 'import', 'f0e1d2c3-b4a5-4f6e-9d8c-7b6a5f4e3d2c'),
((SELECT id FROM warehouse WHERE product_name = 'Pepperoni'), 40, 'import', '7b6a5f4e-3d2c-4f6e-f0e1-d2c3b4a54f6e'),
((SELECT id FROM warehouse WHERE product_name = 'Pizza Base Size M'), 60, 'import', '4d5e6f7a-8b9c-4e1d-3c2b-1a0f9e8d7c6b'),
((SELECT id FROM warehouse WHERE product_name = 'Coca Cola Can'), 100, 'import', '1a0f9e8d-7c6b-4e1d-4d5e-6f7a8b9c4e1d'),
((SELECT id FROM warehouse WHERE product_name = 'Fresh Mushrooms'), 25, 'import', '6f7a8b9c-0d1e-4b3c-7b8c-9d0e1f2a4b3c'),
((SELECT id FROM warehouse WHERE product_name = 'Pizza Box Size L'), 150, 'import', '3c2b1a0f-9e8d-4f7a-8b9c-0d1e1f2a4b3c');