-- ===================================================================
-- V8__add_created_at_timestamps.sql
-- Añade una marca de tiempo de creación a 'products' y 'categories' para
-- poder listar "lo más reciente primero" de forma determinista (incluso en
-- los listados paginados en el servidor).
--
-- El esquema lo valida Hibernate (ddl-auto=validate); la columna se puebla en
-- inserciones nuevas mediante @CreationTimestamp en la entidad, y el
-- DEFAULT now() cubre las filas ya existentes al aplicar esta migración.
-- ===================================================================

ALTER TABLE products
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();

ALTER TABLE categories
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();

-- Backfill de productos ya existentes: aproximamos su fecha de creación con la
-- fecha de su primer movimiento de stock (cuando exista), para darles un orden
-- cronológico realista en lugar de dejarlos todos con el instante de la migración.
UPDATE products p
SET created_at = m.first_movement
FROM (
    SELECT product_id, MIN(movement_date) AS first_movement
    FROM stock_movements
    GROUP BY product_id
) m
WHERE m.product_id = p.id;

COMMENT ON COLUMN products.created_at IS 'Fecha de creación del producto (orden de listado: más reciente primero).';
COMMENT ON COLUMN categories.created_at IS 'Fecha de creación de la categoría (orden de listado: más reciente primero).';
