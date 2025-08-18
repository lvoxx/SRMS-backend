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

INSERT INTO customer (first_name, last_name, phone_number, email, dietary_restrictions, allergies, is_regular, notes, created_at) VALUES
('An', 'Nguyen', '+84987654321', 'an.nguyen@example.com', ARRAY['Vegetarian'], ARRAY['Peanuts'], TRUE, 'Prefers window seat.', '2025-07-15 10:00:00+07'),
('Olivia', 'Smith', '+14155552671', 'olivia.smith@email.com', NULL, NULL, FALSE, NULL, '2025-07-12 11:30:00+07'),
('Binh', 'Tran', '+84912345678', 'binh.tran@email.com', ARRAY['Gluten-Free'], ARRAY['Shellfish'], TRUE, 'Celebrated anniversary here last year.', '2025-07-10 19:45:00+07'),
('James', 'Johnson', '+442079460958', 'james.j@webmail.com', NULL, ARRAY['Dairy'], FALSE, NULL, '2025-07-09 14:00:00+07'),
('Chau', 'Le', '+84333444555', 'chau.le@work.com', ARRAY['Vegan'], NULL, TRUE, 'Friend of the chef.', '2025-06-28 18:20:00+07'),
('Sophia', 'Williams', '61491570110', 'sophia.w@mail.com', NULL, NULL, FALSE, 'First time visitor.', '2025-06-25 20:00:00+07'),
('Minh', 'Pham', '+84777888999', 'minh.pham@example.com', ARRAY['Halal'], ARRAY['Tree Nuts'], TRUE, NULL, '2025-06-22 12:15:00+07'),
('Lucas', 'Brown', '+12125552368', NULL, NULL, ARRAY['Soy'], FALSE, NULL, '2025-06-19 13:00:00+07'),
('Lan', 'Huynh', '+84905123456', 'lan.huynh@mail.vn', NULL, NULL, TRUE, 'Always orders the Pho Bo.', '2025-06-15 17:55:00+07'),
('Evelyn', 'Jones', '+6591234567', 'evelyn.jones@email.sg', ARRAY['Vegetarian'], NULL, FALSE, NULL, '2025-06-11 21:10:00+07'),
('Quan', 'Vo', '+84965432109', 'quan.vo@example.com', NULL, ARRAY['Fish'], TRUE, NULL, '2025-06-05 19:00:00+07'),
('Alexander', 'Garcia', '+33612345678', 'alex.garcia@email.fr', ARRAY['Dairy-Free'], NULL, FALSE, NULL, '2025-06-01 15:30:00+07'),
('Thanh', 'Do', '+84868111222', 'thanh.do@personal.com', NULL, NULL, TRUE, 'Likes extra spicy.', '2025-05-28 11:45:00+07'),
('Mia', 'Miller', '+4915123456789', 'mia.miller@mail.de', ARRAY['Gluten-Free'], ARRAY['Gluten'], FALSE, NULL, '2025-05-20 20:30:00+07'),
('Phong', 'Hoang', '+84944333222', NULL, NULL, NULL, FALSE, 'Walk-in customer.', '2025-05-15 12:00:00+07'),
('Harper', 'Davis', '+13105550123', 'harper.davis@email.com', ARRAY['Vegan'], ARRAY['Peanuts', 'Soy'], TRUE, NULL, '2025-05-10 18:00:00+07'),
('Viet', 'Ngo', '+84978901234', 'viet.ngo@example.com', NULL, NULL, FALSE, NULL, '2025-05-02 19:20:00+07'),
('Daniel', 'Rodriguez', '+34612345678', 'daniel.r@email.es', ARRAY['Low-Carb'], NULL, FALSE, NULL, '2025-04-29 22:00:00+07'),
('Nga', 'Bui', '+84918765432', 'nga.bui@work.vn', NULL, ARRAY['Shellfish'], TRUE, 'Enjoys the tasting menu.', '2025-04-25 17:00:00+07'),
('Emily', 'Martinez', '+17135559876', 'emily.m@email.com', NULL, NULL, FALSE, NULL, '2025-04-18 13:45:00+07'),
('Son', 'Dinh', '+84834567890', 'son.dinh@example.com', ARRAY['Halal'], NULL, TRUE, NULL, '2025-04-12 12:30:00+07'),
('Chloe', 'Hernandez', '+525512345678', 'chloe.h@email.mx', NULL, ARRAY['Eggs'], FALSE, NULL, '2025-04-07 20:15:00+07'),
('Dung', 'Ly', '+84909876543', NULL, NULL, NULL, FALSE, NULL, '2025-03-30 19:00:00+07'),
('Christopher', 'Lopez', '+16505553456', 'chris.lopez@email.com', ARRAY['Vegetarian'], NULL, TRUE, 'Requires a high chair for a toddler.', '2025-03-22 18:30:00+07'),
('Trang', 'Phan', '+84932109876', 'trang.phan@mail.com', NULL, ARRAY['Dairy'], FALSE, NULL, '2025-03-16 14:20:00+07'),
('Grace', 'Gonzalez', '+13055557890', 'grace.g@webmail.com', NULL, NULL, FALSE, NULL, '2025-03-10 19:50:00+07'),
('Ha', 'Vu', '+84977666555', 'ha.vu@example.com', ARRAY['Gluten-Free'], NULL, TRUE, 'Loves the outdoor seating area.', '2025-03-01 11:00:00+07'),
('Henry', 'Wilson', '+441614960123', 'henry.w@email.co.uk', NULL, ARRAY['Fish'], FALSE, NULL, '2025-02-24 20:40:00+07'),
('Kieu', 'Dang', '+84988777666', NULL, NULL, NULL, FALSE, 'Referred by An Nguyen.', '2025-02-19 12:45:00+07'),
('Aria', 'Anderson', '+12065558765', 'aria.a@email.com', ARRAY['Vegan'], NULL, TRUE, NULL, '2025-02-14 18:00:00+07'),
('Xuan', 'Ho', '+84922333444', 'xuan.ho@personal.vn', NULL, ARRAY['Peanuts'], FALSE, NULL, '2025-02-08 13:15:00+07'),
('Jacob', 'Thomas', '+61299998888', 'jacob.t@mail.com.au', NULL, NULL, FALSE, NULL, '2025-01-31 19:30:00+07'),
('Phuc', 'Duong', '+84888999000', 'phuc.duong@example.com', ARRAY['Vegetarian'], NULL, TRUE, 'Media/Influencer.', '2025-01-25 20:00:00+07'),
('Penelope', 'Taylor', '+14165551234', 'penelope.t@email.ca', NULL, ARRAY['Shellfish'], FALSE, 'Tourist, asked for local recommendations.', '2025-01-18 17:30:00+07'),
('Hieu', 'Mai', '+84901234567', NULL, NULL, NULL, FALSE, NULL, '2025-01-10 12:00:00+07'),
('Lily', 'Moore', '+441314960567', 'lily.m@webmail.co.uk', NULL, NULL, FALSE, NULL, '2024-12-28 19:00:00+07'),
('Tuan', 'Trinh', '+84812345678', 'tuan.trinh@example.com', NULL, ARRAY['Eggs'], TRUE, NULL, '2024-12-20 18:45:00+07'),
('Zoe', 'Jackson', '+13125557891', 'zoe.j@email.com', ARRAY['Dairy-Free'], ARRAY['Dairy'], FALSE, NULL, '2024-12-12 21:00:00+07'),
('Linh', 'Dao', '+84966555444', NULL, ARRAY['Tree Nuts'], FALSE, 'Company dinner booking.', '2024-12-05 13:30:00+07'),
('Leo', 'Martin', '+33712345678', 'leo.m@email.fr', NULL, NULL, FALSE, NULL, '2024-11-28 20:10:00+07'),
('Yen', 'Vuong', '+84987123456', 'yen.vuong@mail.vn', ARRAY['Gluten-Free'], NULL, TRUE, NULL, '2024-11-21 11:50:00+07'),
('Owen', 'Lee', '+16045556789', 'owen.lee@email.com', NULL, NULL, FALSE, NULL, '2024-11-14 17:00:00+07'),
('My', 'Doan', '+84911222333', NULL, NULL, FALSE, NULL, '2024-11-06 19:25:00+07'),
('David', 'Perez', '+34912345678', 'david.p@email.es', ARRAY['Kosher'], NULL, FALSE, NULL, '2024-10-30 22:15:00+07'),
('Anh', 'Luu', '+84902345678', 'anh.luu@example.com', NULL, ARRAY['Soy'], TRUE, 'Regular for lunch.', '2024-10-22 12:05:00+07'),
('Stella', 'Thompson', '+19175554321', 'stella.t@email.com', ARRAY['Vegetarian'], NULL, FALSE, NULL, '2024-10-15 18:30:00+07'),
('Huy', 'Cao', '+84976543210', NULL, NULL, FALSE, 'Friend of a staff member.', '2024-10-07 14:00:00+07'),
('Nora', 'White', '+64211234567', 'nora.w@email.co.nz', NULL, ARRAY['Gluten'], FALSE, NULL, '2024-09-29 20:50:00+07'),
('Nam', 'Ho', '+84823456789', 'nam.ho@work.com', NULL, NULL, TRUE, NULL, '2024-09-15 19:00:00+07'),
('Isla', 'Harris', '+447700900123', 'isla.h@email.com', ARRAY['Vegan'], NULL, FALSE, NULL, '2024-09-01 19:45:00+07');

INSERT INTO customer (first_name, last_name, phone_number, email, dietary_restrictions, allergies, is_regular, notes, created_at) VALUES
('Tien', 'Dinh', '+84978111222', 'tien.dinh@example.com', NULL, ARRAY['Shellfish'], TRUE, 'Celebrates birthday in October.', '2025-07-20 09:30:00+07'),
('Charlotte', 'Martin', '+15125550182', 'charlotte.m@email.com', ARRAY['Vegetarian'], NULL, FALSE, NULL, '2025-07-18 14:00:00+07'),
('Khanh', 'Ngo', '+84913456789', 'khanh.ngo@work.vn', NULL, NULL, TRUE, 'Prefers a table near the entrance.', '2025-07-16 18:00:00+07'),
('Matthew', 'Robinson', '+441214960333', 'matthew.r@webmail.co.uk', ARRAY['Vegan'], ARRAY['Peanuts'], FALSE, 'Inquiring about private event hosting.', '2025-07-14 11:25:00+07'),
('Oanh', 'Ly', '+84345678901', 'oanh.ly@personal.com', ARRAY['Gluten-Free'], NULL, TRUE, NULL, '2025-07-11 20:15:00+07'),
('Benjamin', 'Clark', '+18325553434', NULL, NULL, ARRAY['Tree Nuts'], FALSE, NULL, '2025-07-08 19:00:00+07'),
('Giang', 'Bui', '+84789012345', 'giang.bui@mail.vn', NULL, NULL, TRUE, 'Always orders the Bun Cha.', '2025-07-05 12:45:00+07'),
('Isabella', 'Lewis', '+61399998888', 'isabella.l@mail.com.au', ARRAY['Dairy-Free'], NULL, FALSE, NULL, '2025-07-01 21:30:00+07'),
('Thang', 'Hoang', '+84908765432', 'thang.h@example.com', ARRAY['Halal'], ARRAY['Soy'], TRUE, NULL, '2025-06-27 13:00:00+07'),
('Amelia', 'Walker', '+6581234567', 'amelia.w@email.sg', NULL, NULL, FALSE, 'Tourist.', '2025-06-24 20:00:00+07'),
('Cuong', 'Phan', '+84967890123', NULL, ARRAY['Fish', 'Shellfish'], FALSE, 'Booked for a large family gathering.', '2025-06-20 19:30:00+07'),
('Joseph', 'Hall', '+33787654321', 'joseph.h@email.fr', NULL, NULL, FALSE, NULL, '2025-06-17 15:00:00+07'),
('Diep', 'Do', '+84867112233', 'diep.do@work.com', ARRAY['Low-Carb'], ARRAY['Gluten'], TRUE, NULL, '2025-06-13 11:10:00+07'),
('Scarlett', 'Allen', '+4917612345678', 'scarlett.a@mail.de', ARRAY['Vegetarian'], NULL, FALSE, NULL, '2025-06-10 20:45:00+07'),
('Hung', 'Tran', '+84945678901', NULL, NULL, FALSE, 'Walk-in, paid with cash.', '2025-06-06 12:20:00+07'),
('Victoria', 'Young', '+14085550155', 'victoria.y@email.com', ARRAY['Vegan'], NULL, TRUE, 'Works nearby.', '2025-06-02 18:15:00+07'),
('Mai', 'Nguyen', '+84971234567', 'mai.nguyen@example.com', NULL, ARRAY['Dairy'], FALSE, NULL, '2025-05-29 19:00:00+07'),
('Samuel', 'King', '+34698765432', 'samuel.k@email.es', NULL, NULL, FALSE, NULL, '2025-05-26 22:30:00+07'),
('Thao', 'Le', '+84917654321', 'thao.le@personal.vn', NULL, ARRAY['Eggs'], TRUE, 'Loves the fresh spring rolls.', '2025-05-21 17:30:00+07'),
('Hazel', 'Wright', '+12135558787', 'hazel.w@email.com', NULL, NULL, FALSE, NULL, '2025-05-17 13:00:00+07'),
('Khoa', 'Pham', '+84835678901', 'khoa.pham@example.com', ARRAY['Halal'], ARRAY['Peanuts'], TRUE, NULL, '2025-05-12 12:10:00+07'),
('Eleanor', 'Scott', '+525587654321', 'eleanor.s@email.mx', NULL, NULL, FALSE, NULL, '2025-05-08 20:00:00+07'),
('Hanh', 'Vu', '+84908765432', NULL, NULL, FALSE, NULL, '2025-05-03 19:40:00+07'),
('Anthony', 'Green', '+19175551122', 'anthony.g@email.com', ARRAY['Vegetarian'], ARRAY['Tree Nuts'], TRUE, 'Customer since 2023.', '2025-04-28 18:00:00+07'),
('Lien', 'Ho', '+84931098765', 'lien.ho@mail.com', NULL, NULL, FALSE, NULL, '2025-04-24 14:00:00+07'),
('Aurora', 'Adams', '+14045555566', 'aurora.a@webmail.com', ARRAY['Dairy-Free'], NULL, FALSE, NULL, '2025-04-19 19:20:00+07'),
('Duc', 'Dang', '+84975556667', 'duc.dang@example.com', NULL, NULL, TRUE, 'Owner of the nearby bookshop.', '2025-04-15 11:30:00+07'),
('William', 'Baker', '+441414960789', 'william.b@email.co.uk', NULL, ARRAY['Shellfish'], FALSE, NULL, '2025-04-10 20:00:00+07'),
('Nhung', 'Trinh', '+84986667778', NULL, NULL, FALSE, 'First visit, very impressed.', '2025-04-05 12:15:00+07'),
('Luna', 'Nelson', '+13235559900', 'luna.n@email.com', ARRAY['Vegan'], NULL, TRUE, 'Asks for the quietest table available.', '2025-03-31 18:45:00+07'),
('Hieu', 'Ly', '+84921112223', 'hieu.ly@personal.vn', NULL, ARRAY['Gluten'], FALSE, NULL, '2025-03-27 13:30:00+07'),
('Elijah', 'Carter', '+61412345678', 'elijah.c@mail.com.au', NULL, NULL, FALSE, NULL, '2025-03-22 19:00:00+07'),
('Thu', 'Doan', '+84887778889', 'thu.doan@example.com', ARRAY['Vegetarian'], NULL, TRUE, 'Comes for lunch every Friday.', '2025-03-18 20:30:00+07'),
('Sofia', 'Mitchell', '+16475551212', 'sofia.m@email.ca', NULL, ARRAY['Dairy'], FALSE, 'Celebrating graduation.', '2025-03-14 17:00:00+07'),
('Long', 'Vo', '+84902345678', NULL, NULL, FALSE, NULL, '2025-03-10 12:30:00+07'),
('Camila', 'Perez', '+441174960444', 'camila.p@webmail.co.uk', NULL, NULL, FALSE, NULL, '2025-03-05 19:15:00+07'),
('Bao', 'Vuong', '+84811223344', 'bao.vuong@example.com', NULL, ARRAY['Fish'], TRUE, NULL, '2025-02-28 18:00:00+07'),
('Abigail', 'Roberts', '+13125558899', 'abigail.r@email.com', ARRAY['Gluten-Free'], ARRAY['Gluten', 'Soy'], FALSE, NULL, '2025-02-24 21:20:00+07'),
('Ngoc', 'Phan', '+84965554443', NULL, ARRAY['Peanuts'], FALSE, 'Corporate account booking.', '2025-02-20 13:00:00+07'),
('Jayden', 'Turner', '+33654321876', 'jayden.t@email.fr', NULL, NULL, FALSE, NULL, '2025-02-15 20:40:00+07'),
('Vy', 'Dinh', '+84981234567', 'vy.dinh@mail.vn', NULL, NULL, TRUE, NULL, '2025-02-11 11:00:00+07'),
('Ethan', 'Phillips', '+12065553344', 'ethan.p@email.com', NULL, NULL, FALSE, NULL, '2025-02-06 17:30:00+07'),
('Quynh', 'Tran', '+84912223334', NULL, NULL, FALSE, NULL, '2025-02-01 19:00:00+07'),
('Madison', 'Campbell', '+34911223344', 'madison.c@email.es', ARRAY['Kosher'], NULL, FALSE, 'Visiting from Spain.', '2025-01-28 22:00:00+07'),
('Trung', 'Bui', '+84903456789', 'trung.bui@example.com', NULL, ARRAY['Shellfish'], TRUE, NULL, '2025-01-23 12:40:00+07'),
('Avery', 'Parker', '+19175556677', 'avery.p@email.com', ARRAY['Vegetarian'], NULL, FALSE, NULL, '2025-01-19 18:00:00+07'),
('Tung', 'Le', '+84975432109', NULL, NULL, FALSE, 'Requested a menu in English.', '2025-01-14 14:30:00+07'),
('Layla', 'Evans', '+64223456789', 'layla.e@email.co.nz', NULL, ARRAY['Tree Nuts'], FALSE, NULL, '2025-01-09 20:10:00+07'),
('Dat', 'Nguyen', '+84822345678', 'dat.nguyen@work.com', NULL, NULL, TRUE, 'Regular for business lunches.', '2025-01-04 19:00:00+07'),
('Riley', 'Edwards', '+447700900456', 'riley.e@email.com', ARRAY['Vegan', 'Gluten-Free'], NULL, FALSE, NULL, '2024-12-29 19:30:00+07');