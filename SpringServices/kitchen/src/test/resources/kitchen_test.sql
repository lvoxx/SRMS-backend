CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =====================================================
-- KITCHEN SERVICE DATABASE SCHEMA
-- =====================================================

-- Table: menu_category
-- Purpose: Hierarchical categorization of kitchen menu items
CREATE TABLE menu_category (
    self_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ctg_parent_id UUID,
    category_name VARCHAR(255) NOT NULL,
    display_order INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_parent_category 
        FOREIGN KEY (ctg_parent_id) 
        REFERENCES menu_category(self_id) 
        ON DELETE SET NULL,
    CONSTRAINT chk_no_self_reference 
        CHECK (self_id != ctg_parent_id),
    CONSTRAINT chk_category_name_not_empty 
        CHECK (TRIM(category_name) != '')
);

-- Index for faster hierarchy queries
CREATE INDEX idx_menu_category_parent ON menu_category(ctg_parent_id);
CREATE INDEX idx_menu_category_active ON menu_category(is_active) WHERE is_active = TRUE;

-- =====================================================

-- Table: kitchen_menu
-- Purpose: Menu items available for ordering
CREATE TABLE kitchen_menu (
    menu_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_name VARCHAR(255) NOT NULL,
    image_url VARCHAR(500),
    min_quantity INT DEFAULT 1,
    unit VARCHAR(50) DEFAULT 'phần',
    max_quantity INT DEFAULT 20,
    menu_ctg_id UUID NOT NULL,
    price DECIMAL(12, 2),
    description VARCHAR(1000),
    is_available BOOLEAN DEFAULT TRUE,
    preparation_time_minutes INT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_menu_category 
        FOREIGN KEY (menu_ctg_id) 
        REFERENCES menu_category(self_id) 
        ON DELETE RESTRICT,
    CONSTRAINT chk_menu_name_not_empty 
        CHECK (TRIM(menu_name) != ''),
    CONSTRAINT chk_min_quantity_positive 
        CHECK (min_quantity > 0),
    CONSTRAINT chk_max_quantity_valid 
        CHECK (max_quantity >= min_quantity),
    CONSTRAINT chk_price_non_negative 
        CHECK (price IS NULL OR price >= 0),
    CONSTRAINT chk_prep_time_positive 
        CHECK (preparation_time_minutes IS NULL OR preparation_time_minutes > 0)
);

-- Indexes for common queries
CREATE INDEX idx_kitchen_menu_category ON kitchen_menu(menu_ctg_id);
CREATE INDEX idx_kitchen_menu_available ON kitchen_menu(is_available) WHERE is_available = TRUE;

-- =====================================================

-- Table: kitchen_orders
-- Purpose: Track orders received from order service
CREATE TABLE kitchen_orders (
    kitchen_order_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    table_location VARCHAR(100) NOT NULL,
    staff_name VARCHAR(100) NOT NULL,
    order_time TIMESTAMPTZ NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'received',
    kitchen_note VARCHAR(500),
    customer_note VARCHAR(500),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_order_status 
        CHECK (status IN (
            'received',           -- Đã nhận
            'kitchen_rejected',   -- Nhà bếp từ chối
            'adjusted',           -- Điều chỉnh
            'preparing',          -- Đang chế biến
            'customer_rejected',  -- Khách từ chối
            'completed'           -- Hoàn thành
        )),
    CONSTRAINT chk_kitchen_note_required 
        CHECK (
            (status NOT IN ('kitchen_rejected', 'adjusted') OR 
            (kitchen_note IS NOT NULL AND TRIM(kitchen_note) != ''))
        ),
    CONSTRAINT chk_customer_note_required 
        CHECK (
            (status != 'customer_rejected' OR 
            (customer_note IS NOT NULL AND TRIM(customer_note) != ''))
        ),
    CONSTRAINT chk_table_location_not_empty 
        CHECK (TRIM(table_location) != ''),
    CONSTRAINT chk_staff_name_not_empty 
        CHECK (TRIM(staff_name) != ''),
    CONSTRAINT chk_order_time_valid 
        CHECK (order_time <= CURRENT_TIMESTAMP)
);

-- Indexes for performance
CREATE INDEX idx_kitchen_orders_order_id ON kitchen_orders(order_id);
CREATE INDEX idx_kitchen_orders_status ON kitchen_orders(status);
CREATE INDEX idx_kitchen_orders_time ON kitchen_orders(order_time DESC);

-- =====================================================

-- Table: kitchen_order_items
-- Purpose: Individual items within each kitchen order
CREATE TABLE kitchen_order_items (
    item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kitchen_order_id UUID NOT NULL,
    menu_id UUID NOT NULL,
    menu_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit VARCHAR(50) DEFAULT 'phần',
    special_request VARCHAR(500),
    item_status VARCHAR(50) DEFAULT 'pending',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT fk_kitchen_order 
        FOREIGN KEY (kitchen_order_id) 
        REFERENCES kitchen_orders(kitchen_order_id) 
        ON DELETE CASCADE,
    CONSTRAINT chk_quantity_positive 
        CHECK (quantity > 0),
    CONSTRAINT chk_menu_name_not_empty 
        CHECK (TRIM(menu_name) != ''),
    CONSTRAINT chk_item_status 
        CHECK (item_status IN ('pending', 'preparing', 'ready', 'rejected'))
);

-- Indexes
CREATE INDEX idx_order_items_kitchen_order ON kitchen_order_items(kitchen_order_id);
CREATE INDEX idx_order_items_menu ON kitchen_order_items(menu_id);

-- =====================================================

-- Table: kitchen_inside_history
-- Purpose: Track internal kitchen inventory movements
CREATE TABLE kitchen_inside_history (
    history_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_type VARCHAR(50) NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    item_category VARCHAR(100),
    quantity DECIMAL(12, 3) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    source_reference VARCHAR(100),
    staff_name VARCHAR(100) NOT NULL,
    transaction_date TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(1000),
    cost_amount DECIMAL(12, 2),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT chk_transaction_type 
        CHECK (transaction_type IN (
            'warehouse_receive',    -- Nhận từ warehouse
            'external_purchase',    -- Mua từ bên ngoài
            'damage_disposal',      -- Huỷ hàng hư hỏng
            'daily_check',          -- Kiểm đồ cuối ngày
            'adjustment',           -- Điều chỉnh
            'transfer_out',         -- Chuyển ra
            'transfer_in'           -- Chuyển vào
        )),
    CONSTRAINT chk_item_name_not_empty 
        CHECK (TRIM(item_name) != ''),
    CONSTRAINT chk_staff_name_not_empty_history 
        CHECK (TRIM(staff_name) != ''),
    CONSTRAINT chk_unit_not_empty 
        CHECK (TRIM(unit) != ''),
    CONSTRAINT chk_transaction_date_valid 
        CHECK (transaction_date <= CURRENT_TIMESTAMP),
    CONSTRAINT chk_cost_non_negative 
        CHECK (cost_amount IS NULL OR cost_amount >= 0),
    CONSTRAINT chk_damage_note_required 
        CHECK (
            (transaction_type != 'damage_disposal' OR 
            (notes IS NOT NULL AND TRIM(notes) != ''))
        )
);

-- Indexes for reporting and queries
CREATE INDEX idx_kitchen_history_type ON kitchen_inside_history(transaction_type);
CREATE INDEX idx_kitchen_history_date ON kitchen_inside_history(transaction_date DESC);
CREATE INDEX idx_kitchen_history_item ON kitchen_inside_history(item_name);
CREATE INDEX idx_kitchen_history_category ON kitchen_inside_history(item_category);

-- =====================================================

-- Table: kitchen_inventory_current
-- Purpose: Current inventory levels (optional, for quick reference)
CREATE TABLE kitchen_inventory_current (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_name VARCHAR(255) NOT NULL UNIQUE,
    item_category VARCHAR(100),
    current_quantity DECIMAL(12, 3) NOT NULL DEFAULT 0,
    unit VARCHAR(50) NOT NULL,
    min_threshold DECIMAL(12, 3),
    max_threshold DECIMAL(12, 3),
    last_updated TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100),
    
    -- Constraints
    CONSTRAINT chk_current_quantity_non_negative 
        CHECK (current_quantity >= 0),
    CONSTRAINT chk_thresholds_valid 
        CHECK (
            min_threshold IS NULL OR 
            max_threshold IS NULL OR 
            min_threshold <= max_threshold
        ),
    CONSTRAINT chk_inventory_item_name_not_empty 
        CHECK (TRIM(item_name) != ''),
    CONSTRAINT chk_inventory_unit_not_empty 
        CHECK (TRIM(unit) != '')
);

-- Index for low stock alerts
CREATE INDEX idx_inventory_low_stock 
    ON kitchen_inventory_current(current_quantity) 
    WHERE min_threshold IS NOT NULL AND current_quantity <= min_threshold;

-- =====================================================

-- Trigger function to update timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply triggers
CREATE TRIGGER update_menu_category_updated_at 
    BEFORE UPDATE ON menu_category 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_kitchen_menu_updated_at 
    BEFORE UPDATE ON kitchen_menu 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_kitchen_orders_updated_at 
    BEFORE UPDATE ON kitchen_orders 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();