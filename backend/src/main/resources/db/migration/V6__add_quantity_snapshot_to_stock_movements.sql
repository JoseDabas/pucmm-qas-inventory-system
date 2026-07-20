-- ===================================================================
-- SCRIPT DE MIGRACIÓN FLYWAY: V6__add_quantity_snapshot_to_stock_movements.sql
-- Propósito: Agregar a 'stock_movements' la foto del stock ANTES y DESPUÉS
--            de cada movimiento (cantidad anterior / cantidad nueva), para
--            poder mostrarlas directamente en el Historial de Movimientos.
-- ===================================================================

-- Nuevas columnas (se agregan nullable para poder rellenar las filas existentes)
ALTER TABLE stock_movements ADD COLUMN previous_quantity INTEGER;
ALTER TABLE stock_movements ADD COLUMN new_quantity INTEGER;

-- Backfill de las filas existentes. Hasta ahora la aplicación solo generaba el
-- movimiento inicial (IN) al crear un producto, por lo que el stock pasó de 0
-- a 'quantity' en cada registro histórico previo.
UPDATE stock_movements
   SET previous_quantity = 0,
       new_quantity = quantity
 WHERE previous_quantity IS NULL;

-- Una vez rellenadas, se vuelven obligatorias para garantizar la integridad.
ALTER TABLE stock_movements ALTER COLUMN previous_quantity SET NOT NULL;
ALTER TABLE stock_movements ALTER COLUMN new_quantity SET NOT NULL;

COMMENT ON COLUMN stock_movements.previous_quantity IS 'Cantidad en stock del producto ANTES de aplicar este movimiento.';
COMMENT ON COLUMN stock_movements.new_quantity IS 'Cantidad en stock del producto DESPUÉS de aplicar este movimiento.';

-- La entidad StockMovement está auditada con Hibernate Envers (@Audited), por lo que la
-- tabla de auditoría también debe reflejar las nuevas columnas. Se dejan nullable, igual
-- que el resto de columnas auditadas, para permitir auditorías parciales de Envers.
ALTER TABLE stock_movements_aud ADD COLUMN previous_quantity INTEGER;
ALTER TABLE stock_movements_aud ADD COLUMN new_quantity INTEGER;
