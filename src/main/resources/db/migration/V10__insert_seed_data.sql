-- Sample categories
INSERT INTO categories (id, name, description) VALUES
    (gen_random_uuid(), 'Electronics', 'Electronic gadgets and accessories'),
    (gen_random_uuid(), 'Clothing', 'Fashion and apparel'),
    (gen_random_uuid(), 'Books', 'Books and educational material')
ON CONFLICT (name) DO NOTHING;

-- Sample coupon
INSERT INTO coupons (code, discount_type, discount_value, min_order_value, max_uses, is_active)
VALUES ('WELCOME10', 'PERCENTAGE', 10.00, 50.00, 100, true)
ON CONFLICT (code) DO NOTHING;
