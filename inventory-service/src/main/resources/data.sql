INSERT INTO inventory (product_id, stock, version) VALUES ('PROD-999', 100, 0) ON CONFLICT DO NOTHING;
