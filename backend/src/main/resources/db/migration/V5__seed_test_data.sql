-- ===================================================================
-- 1. CATEGORÍAS
-- ===================================================================
INSERT INTO categories (id, name, description) VALUES
('11111111-1111-1111-1111-111111111111', 'Electrónica', 'Dispositivos electrónicos, computadoras y accesorios.'),
('22222222-2222-2222-2222-222222222222', 'Hogar',       'Electrodomésticos, iluminación y artículos para el hogar.'),
('33333333-3333-3333-3333-333333333333', 'Oficina',     'Mobiliario y suministros de oficina.'),
('44444444-4444-4444-4444-444444444444', 'Deportes',    'Equipamiento deportivo y fitness.'),
('55555555-5555-5555-5555-555555555555', 'Herramientas','Herramientas eléctricas y manuales.');

-- ===================================================================
-- 2. PRODUCTOS (3 por categoría)
--    id | name | sku | descripción | category_id | price | init | min | activo
-- ===================================================================

-- Electrónica
INSERT INTO products (id, name, sku_code, description, category_id, price, initial_quantity, minimum_stock, is_active) VALUES
('a0000000-0000-0000-0000-000000000001', 'Laptop Pro 15',      'LAP-001', 'Laptop de alto rendimiento para desarrolladores', '11111111-1111-1111-1111-111111111111', 1250.00, 100, 20, true),
('a0000000-0000-0000-0000-000000000002', 'Mouse Inalámbrico',  'MOU-001', 'Mouse recargable para oficina',                   '11111111-1111-1111-1111-111111111111',   25.00,  40, 15, true),
('a0000000-0000-0000-0000-000000000003', 'Teclado Mecánico',   'TEC-001', 'Teclado mecánico retroiluminado',                 '11111111-1111-1111-1111-111111111111',   80.00,  60, 25, true);

-- Hogar
INSERT INTO products (id, name, sku_code, description, category_id, price, initial_quantity, minimum_stock, is_active) VALUES
('a0000000-0000-0000-0000-000000000004', 'Licuadora Smart',    'LIC-001', 'Licuadora inteligente con conexión WiFi',         '22222222-2222-2222-2222-222222222222',  120.00,  50, 10, true),
('a0000000-0000-0000-0000-000000000005', 'Lámpara LED',        'LAM-001', 'Lámpara de escritorio LED regulable',             '22222222-2222-2222-2222-222222222222',   15.00,  20, 20, true),
('a0000000-0000-0000-0000-000000000006', 'Aspiradora Robot',   'ASP-001', 'Aspiradora robótica con mapeo láser',             '22222222-2222-2222-2222-222222222222',  300.00,  25,  8, true);

-- Oficina
INSERT INTO products (id, name, sku_code, description, category_id, price, initial_quantity, minimum_stock, is_active) VALUES
('a0000000-0000-0000-0000-000000000007', 'Silla Ergonómica',   'SIL-001', 'Silla ergonómica de malla transpirable',          '33333333-3333-3333-3333-333333333333',  200.00,  30,  5, true),
('a0000000-0000-0000-0000-000000000008', 'Escritorio Ajustable','ESC-001','Escritorio con altura ajustable eléctrica',       '33333333-3333-3333-3333-333333333333',  350.00,  15,  5, true),
('a0000000-0000-0000-0000-000000000009', 'Archivador Metálico','ARC-001', 'Archivador de 4 gavetas con cerradura',           '33333333-3333-3333-3333-333333333333',   90.00,  40, 12, true);

-- Deportes
INSERT INTO products (id, name, sku_code, description, category_id, price, initial_quantity, minimum_stock, is_active) VALUES
('a0000000-0000-0000-0000-00000000000a', 'Balón de Fútbol',    'BAL-001', 'Balón profesional cosido a mano',                 '44444444-4444-4444-4444-444444444444',   35.00,  80, 30, true),
('a0000000-0000-0000-0000-00000000000b', 'Mancuernas 10kg',    'MAN-001', 'Par de mancuernas recubiertas de neopreno',       '44444444-4444-4444-4444-444444444444',   45.00,  50, 20, true),
('a0000000-0000-0000-0000-00000000000c', 'Bicicleta Estática', 'BIC-001', 'Bicicleta estática con monitor de pulso',         '44444444-4444-4444-4444-444444444444',  400.00,  12,  4, true);

-- Herramientas
INSERT INTO products (id, name, sku_code, description, category_id, price, initial_quantity, minimum_stock, is_active) VALUES
('a0000000-0000-0000-0000-00000000000d', 'Taladro Percutor',   'TAL-001', 'Taladro percutor inalámbrico 20V',                '55555555-5555-5555-5555-555555555555',  150.00,  35, 10, true),
('a0000000-0000-0000-0000-00000000000e', 'Set Destornilladores','DES-001','Juego de 32 destornilladores de precisión',       '55555555-5555-5555-5555-555555555555',   30.00, 100, 40, true),
('a0000000-0000-0000-0000-00000000000f', 'Sierra Circular',    'SIE-001', 'Sierra circular 1500W con guía láser',            '55555555-5555-5555-5555-555555555555',  220.00,  18,  6, true);

-- ===================================================================
-- 3. MOVIMIENTOS DE STOCK (15)
--    'quantity' es la magnitud del movimiento (delta positivo).
--    Stock resultante y estado indicado en el comentario de cada fila.
-- ===================================================================
INSERT INTO stock_movements (id, product_id, movement_type, quantity, movement_date, username, observations) VALUES
-- Laptop Pro 15: 100 + 50 - 20 = 130  (sano)
('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000001', 'IN',  50, CURRENT_TIMESTAMP - INTERVAL '12 days', 'system_seed', 'Reabastecimiento inicial'),
('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000001', 'OUT', 20, CURRENT_TIMESTAMP - INTERVAL '4 days',  'ventas_usr',  'Despacho corporativo'),
-- Mouse Inalámbrico: 40 - 30 = 10  (CRÍTICO, min 15)
('b0000000-0000-0000-0000-000000000003', 'a0000000-0000-0000-0000-000000000002', 'OUT', 30, CURRENT_TIMESTAMP - INTERVAL '6 days',  'ventas_usr',  'Venta mayorista cliente A'),
-- Teclado Mecánico: 60 - 10 = 50  (sano)
('b0000000-0000-0000-0000-000000000004', 'a0000000-0000-0000-0000-000000000003', 'OUT', 10, CURRENT_TIMESTAMP - INTERVAL '3 days',  'ventas_usr',  'Venta minorista'),
-- Licuadora Smart: 50 - 45 = 5  (CRÍTICO, min 10)
('b0000000-0000-0000-0000-000000000005', 'a0000000-0000-0000-0000-000000000004', 'OUT', 45, CURRENT_TIMESTAMP - INTERVAL '2 days',  'ventas_usr',  'Promoción de temporada'),
-- Lámpara LED: 20 - 15 = 5  (CRÍTICO, min 20)
('b0000000-0000-0000-0000-000000000006', 'a0000000-0000-0000-0000-000000000005', 'OUT', 15, CURRENT_TIMESTAMP - INTERVAL '5 days',  'ventas_usr',  'Suministro oficinas internas'),
-- Aspiradora Robot: 25 + 5 = 30  (sano)
('b0000000-0000-0000-0000-000000000007', 'a0000000-0000-0000-0000-000000000006', 'IN',   5, CURRENT_TIMESTAMP - INTERVAL '9 days',  'system_seed', 'Lote adicional recibido'),
-- Silla Ergonómica: 30 - 5 = 25  (sano)
('b0000000-0000-0000-0000-000000000008', 'a0000000-0000-0000-0000-000000000007', 'OUT',  5, CURRENT_TIMESTAMP - INTERVAL '1 days',  'ventas_usr',  'Venta minorista'),
-- Escritorio Ajustable: 15 - 11 = 4  (CRÍTICO, min 5)
('b0000000-0000-0000-0000-000000000009', 'a0000000-0000-0000-0000-000000000008', 'OUT', 11, CURRENT_TIMESTAMP - INTERVAL '7 days',  'ventas_usr',  'Pedido corporativo grande'),
-- Archivador Metálico: 40 + 10 = 50  (sano)
('b0000000-0000-0000-0000-00000000000a', 'a0000000-0000-0000-0000-000000000009', 'IN',  10, CURRENT_TIMESTAMP - INTERVAL '10 days', 'system_seed', 'Corrección de inventario'),
-- Balón de Fútbol: 80 - 60 = 20  (CRÍTICO, min 30)
('b0000000-0000-0000-0000-00000000000b', 'a0000000-0000-0000-0000-00000000000a', 'OUT', 60, CURRENT_TIMESTAMP - INTERVAL '8 days',  'ventas_usr',  'Venta a club deportivo'),
-- Mancuernas 10kg: 50 - 20 = 30  (sano)
('b0000000-0000-0000-0000-00000000000c', 'a0000000-0000-0000-0000-00000000000b', 'OUT', 20, CURRENT_TIMESTAMP - INTERVAL '3 days',  'ventas_usr',  'Venta a gimnasio'),
-- Bicicleta Estática: 12 - 9 = 3  (CRÍTICO, min 4)
('b0000000-0000-0000-0000-00000000000d', 'a0000000-0000-0000-0000-00000000000c', 'OUT',  9, CURRENT_TIMESTAMP - INTERVAL '6 days',  'ventas_usr',  'Liquidación de stock'),
-- Taladro Percutor: 35 - 5 = 30  (sano)
('b0000000-0000-0000-0000-00000000000e', 'a0000000-0000-0000-0000-00000000000d', 'OUT',  5, CURRENT_TIMESTAMP - INTERVAL '2 days',  'ventas_usr',  'Venta a ferretería'),
-- Sierra Circular: 18 + 4 = 22  (sano)
('b0000000-0000-0000-0000-00000000000f', 'a0000000-0000-0000-0000-00000000000f', 'IN',   4, CURRENT_TIMESTAMP - INTERVAL '11 days', 'system_seed', 'Reposición de proveedor');
