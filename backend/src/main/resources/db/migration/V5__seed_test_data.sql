-- ===================================================================
-- SCRIPT DE MIGRACIÓN FLYWAY: V5__seed_test_data.sql
-- Propósito: Precargar datos de prueba realistas para pruebas de 
--            integración, en particular para el endpoint de alertas
--            de stock bajo (Ticket QA-4 y BACK-10).
-- ===================================================================

-- 1. Insertar Categorías
INSERT INTO categories (id, name, description) VALUES
('11111111-1111-1111-1111-111111111111', 'Electrónica', 'Dispositivos electrónicos, computadoras y accesorios.'),
('22222222-2222-2222-2222-222222222222', 'Hogar', 'Muebles, electrodomésticos y decoración para el hogar.'),
('33333333-3333-3333-3333-333333333333', 'Oficina', 'Insumos, papelería y mobiliario de oficina.');

-- 2. Insertar Productos
-- Productos Saludables
INSERT INTO products (id, name, sku_code, description, category_id, price, initial_quantity, minimum_stock, is_active) VALUES
('aaaa1111-aaaa-1111-aaaa-111111111111', 'Laptop Pro 15', 'LAP-001', 'Laptop de alto rendimiento para desarrolladores', '11111111-1111-1111-1111-111111111111', 1250.00, 100, 20, true),
('aaaa2222-aaaa-2222-aaaa-222222222222', 'Licuadora Smart', 'LIC-001', 'Licuadora inteligente con conexión WiFi', '22222222-2222-2222-2222-222222222222', 120.00, 50, 10, true),
('aaaa3333-aaaa-3333-aaaa-333333333333', 'Silla Ergonómica', 'SIL-001', 'Silla ergonómica de malla transpirable', '33333333-3333-3333-3333-333333333333', 200.00, 30, 5, true);

-- Productos Críticos (Stock <= mínimo para disparar alertas BACK-10)
INSERT INTO products (id, name, sku_code, description, category_id, price, initial_quantity, minimum_stock, is_active) VALUES
('aaaa4444-aaaa-4444-aaaa-444444444444', 'Mouse Inalámbrico', 'MOU-001', 'Mouse recargable para oficina', '11111111-1111-1111-1111-111111111111', 25.00, 40, 15, true),
('aaaa5555-aaaa-5555-aaaa-555555555555', 'Lámpara LED', 'LAM-001', 'Lámpara de escritorio LED', '22222222-2222-2222-2222-222222222222', 15.00, 20, 20, true);

-- 3. Insertar Movimientos de Stock (Histórico)
-- Formula: Stock Actual = initial_quantity + SUM(IN) - SUM(OUT)

-- Movimientos para Producto 1 (Saludable: 100 + 50 - 20 = 130 > 20)
INSERT INTO stock_movements (id, product_id, movement_type, quantity, movement_date, username, observations) VALUES
('bbbb1111-bbbb-1111-bbbb-111111111111', 'aaaa1111-aaaa-1111-aaaa-111111111111', 'IN', 50, CURRENT_TIMESTAMP - INTERVAL '10 days', 'system_seed', 'Reabastecimiento inicial extra'),
('bbbb1111-bbbb-1111-bbbb-222222222222', 'aaaa1111-aaaa-1111-aaaa-111111111111', 'OUT', 20, CURRENT_TIMESTAMP - INTERVAL '5 days', 'ventas_usr', 'Despacho corporativo');

-- Movimientos para Producto 2 (Saludable: 50 + 10 - 5 = 55 > 10)
INSERT INTO stock_movements (id, product_id, movement_type, quantity, movement_date, username, observations) VALUES
('bbbb2222-bbbb-2222-bbbb-111111111111', 'aaaa2222-aaaa-2222-aaaa-222222222222', 'IN', 10, CURRENT_TIMESTAMP - INTERVAL '15 days', 'system_seed', 'Lote recibido con retraso'),
('bbbb2222-bbbb-2222-bbbb-222222222222', 'aaaa2222-aaaa-2222-aaaa-222222222222', 'OUT', 5, CURRENT_TIMESTAMP - INTERVAL '2 days', 'ventas_usr', 'Venta a cliente final');

-- Movimientos para Producto 3 (Saludable: 30 + 0 - 5 = 25 > 5)
INSERT INTO stock_movements (id, product_id, movement_type, quantity, movement_date, username, observations) VALUES
('bbbb3333-bbbb-3333-bbbb-111111111111', 'aaaa3333-aaaa-3333-aaaa-333333333333', 'OUT', 5, CURRENT_TIMESTAMP - INTERVAL '1 days', 'ventas_usr', 'Venta minorista');

-- Movimientos para Producto 4 (CRÍTICO: 40 + 5 - 35 = 10 <= 15)
INSERT INTO stock_movements (id, product_id, movement_type, quantity, movement_date, username, observations) VALUES
('bbbb4444-bbbb-4444-bbbb-111111111111', 'aaaa4444-aaaa-4444-aaaa-444444444444', 'IN', 5, CURRENT_TIMESTAMP - INTERVAL '20 days', 'system_seed', 'Corrección de inventario'),
('bbbb4444-bbbb-4444-bbbb-222222222222', 'aaaa4444-aaaa-4444-aaaa-444444444444', 'OUT', 25, CURRENT_TIMESTAMP - INTERVAL '8 days', 'ventas_usr', 'Despacho mayorista (cliente A)'),
('bbbb4444-bbbb-4444-bbbb-333333333333', 'aaaa4444-aaaa-4444-aaaa-444444444444', 'OUT', 10, CURRENT_TIMESTAMP - INTERVAL '1 days', 'ventas_usr', 'Venta masiva de urgencia');

-- Movimientos para Producto 5 (CRÍTICO: 20 + 0 - 15 = 5 <= 20)
INSERT INTO stock_movements (id, product_id, movement_type, quantity, movement_date, username, observations) VALUES
('bbbb5555-bbbb-5555-bbbb-111111111111', 'aaaa5555-aaaa-5555-aaaa-555555555555', 'OUT', 10, CURRENT_TIMESTAMP - INTERVAL '12 days', 'ventas_usr', 'Suministro oficinas internas'),
('bbbb5555-bbbb-5555-bbbb-222222222222', 'aaaa5555-aaaa-5555-aaaa-555555555555', 'OUT', 5, CURRENT_TIMESTAMP - INTERVAL '3 days', 'ventas_usr', 'Venta estándar');
