-- enable uuid generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Table warehouse
CREATE TABLE IF NOT EXISTS warehouse (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    product_name TEXT NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    min_quantity INTEGER NOT NULL DEFAULT 0 CHECK (min_quantity >= 0),
    supplier_id UUID, -- optional
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by UUID,
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
    invoice_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    type TEXT NOT NULL CHECK (type IN ('import', 'export')),
    updated_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_warehouse_history_product_id ON warehouse_history (product_id);

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
    WHERE product_id = NEW.product_id;
    
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
        updated_by = NEW.updated_by
    WHERE id = NEW.product_id;
    
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
INSERT INTO warehouse (product_name, quantity, min_quantity, supplier_id, updated_by) VALUES
-- Flour and baking ingredients
('00 Flour', 500, 100, NULL, NULL),
('Pizza Flour', 300, 80, NULL, NULL),
('Dry Yeast', 50, 10, NULL, NULL),
('Fresh Yeast', 20, 5, NULL, NULL),
('Extra Virgin Olive Oil', 100, 20, NULL, NULL),
('Vegetable Oil', 80, 15, NULL, NULL),
('Sea Salt', 30, 5, NULL, NULL),
('White Sugar', 40, 8, NULL, NULL),

-- Cheeses and dairy
('Mozzarella Cheese', 200, 50, NULL, NULL),
('Parmesan Cheese', 80, 20, NULL, NULL),
('Cheddar Cheese', 60, 15, NULL, NULL),
('Gorgonzola Cheese', 30, 8, NULL, NULL),
('Ricotta Cheese', 40, 10, NULL, NULL),
('Fresh Cream', 25, 5, NULL, NULL),

-- Tomato sauces and seasonings
('Italian Tomato Sauce', 150, 30, NULL, NULL),
('Crushed Tomatoes', 120, 25, NULL, NULL),
('Hot Sauce', 60, 12, NULL, NULL),
('Garlic Sauce', 45, 10, NULL, NULL),
('Honey Mustard', 35, 8, NULL, NULL),
('Mayonnaise', 40, 8, NULL, NULL),
('BBQ Sauce', 55, 12, NULL, NULL),
('Minced Garlic', 25, 5, NULL, NULL),
('Onion', 80, 15, NULL, NULL),
('Green Bell Pepper', 60, 12, NULL, NULL),
('Red Bell Pepper', 55, 12, NULL, NULL),
('Yellow Bell Pepper', 45, 10, NULL, NULL),

-- Meats and seafood
('Pepperoni', 120, 25, NULL, NULL),
('Italian Sausage', 90, 18, NULL, NULL),
('Ham', 70, 15, NULL, NULL),
('Ground Beef', 85, 20, NULL, NULL),
('Shredded Chicken', 75, 15, NULL, NULL),
('Regular Sausage', 65, 12, NULL, NULL),
('Fresh Shrimp', 40, 8, NULL, NULL),
('Fresh Squid', 35, 7, NULL, NULL),
('Smoked Salmon', 25, 5, NULL, NULL),
('Bacon', 50, 10, NULL, NULL),

-- Vegetables and herbs
('Fresh Mushrooms', 45, 10, NULL, NULL),
('Black Olives', 35, 8, NULL, NULL),
('Green Olives', 30, 8, NULL, NULL),
('Sweet Corn', 40, 8, NULL, NULL),
('Canned Pineapple', 35, 7, NULL, NULL),
('Spinach', 25, 5, NULL, NULL),
('Cherry Tomatoes', 30, 6, NULL, NULL),
('Fresh Basil', 20, 4, NULL, NULL),
('Dried Oregano', 15, 3, NULL, NULL),
('Rosemary', 12, 3, NULL, NULL),
('Jalapeno Peppers', 18, 4, NULL, NULL),

-- Pizza bases and accessories
('Pizza Base Size S', 200, 50, NULL, NULL),
('Pizza Base Size M', 180, 45, NULL, NULL),
('Pizza Base Size L', 160, 40, NULL, NULL),
('Pizza Base Size XL', 140, 35, NULL, NULL),
('Thin Crispy Base', 120, 30, NULL, NULL),
('Chicago Deep Dish Base', 80, 20, NULL, NULL),

-- Beverages and accessories
('Coca Cola Can', 300, 60, NULL, NULL),
('Pepsi Can', 280, 55, NULL, NULL),
('Sprite Can', 250, 50, NULL, NULL),
('Bottled Water', 400, 80, NULL, NULL),
('Heineken Beer', 150, 30, NULL, NULL),
('Tiger Beer', 140, 28, NULL, NULL),
('Italian Red Wine', 60, 12, NULL, NULL),
('White Wine', 55, 12, NULL, NULL),

-- Side dishes
('French Fries', 120, 25, NULL, NULL),
('Onion Rings', 80, 16, NULL, NULL),
('Chicken Nuggets', 90, 18, NULL, NULL),
('BBQ Chicken Wings', 70, 14, NULL, NULL),
('Caesar Salad', 40, 8, NULL, NULL),
('Italian Mixed Salad', 35, 7, NULL, NULL),

-- Consumables
('Pizza Box Size S', 500, 100, NULL, NULL),
('Pizza Box Size M', 450, 90, NULL, NULL),
('Pizza Box Size L', 400, 80, NULL, NULL),
('Pizza Box Size XL', 350, 70, NULL, NULL),
('Paper Bags', 800, 150, NULL, NULL),
('Paper Napkins', 1000, 200, NULL, NULL),
('Pizza Cutter', 15, 3, NULL, NULL),
('Pizza Serving Spatula', 12, 3, NULL, NULL);

-- Insert sample warehouse history records
INSERT INTO warehouse_history (invoice_id, product_id, quantity, type, updated_by) VALUES
-- Initial stock intake
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM warehouse WHERE product_name = '00 Flour'), 500, 'import', NULL),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', (SELECT id FROM warehouse WHERE product_name = 'Mozzarella Cheese'), 200, 'import', NULL),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', (SELECT id FROM warehouse WHERE product_name = 'Italian Tomato Sauce'), 150, 'import', NULL),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', (SELECT id FROM warehouse WHERE product_name = 'Pepperoni'), 120, 'import', NULL),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', (SELECT id FROM warehouse WHERE product_name = 'Pizza Base Size M'), 180, 'import', NULL),
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', (SELECT id FROM warehouse WHERE product_name = 'Coca Cola Can'), 300, 'import', NULL),

-- Stock out for sample orders
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM warehouse WHERE product_name = 'Mozzarella Cheese'), 50, 'export', NULL),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', (SELECT id FROM warehouse WHERE product_name = 'Italian Tomato Sauce'), 30, 'export', NULL),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', (SELECT id FROM warehouse WHERE product_name = 'Pepperoni'), 25, 'export', NULL),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', (SELECT id FROM warehouse WHERE product_name = 'Pizza Base Size M'), 45, 'export', NULL),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', (SELECT id FROM warehouse WHERE product_name = 'Coca Cola Can'), 75, 'export', NULL),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a16', (SELECT id FROM warehouse WHERE product_name = '00 Flour'), 100, 'export', NULL),

-- Additional stock intake
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', (SELECT id FROM warehouse WHERE product_name = 'Mozzarella Cheese'), 80, 'import', NULL),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', (SELECT id FROM warehouse WHERE product_name = 'Red Bell Pepper'), 40, 'import', NULL),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', (SELECT id FROM warehouse WHERE product_name = 'Fresh Mushrooms'), 30, 'import', NULL),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a14', (SELECT id FROM warehouse WHERE product_name = 'Fresh Basil'), 15, 'import', NULL),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a15', (SELECT id FROM warehouse WHERE product_name = 'Pizza Box Size L'), 200, 'import', NULL);