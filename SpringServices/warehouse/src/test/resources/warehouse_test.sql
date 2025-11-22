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