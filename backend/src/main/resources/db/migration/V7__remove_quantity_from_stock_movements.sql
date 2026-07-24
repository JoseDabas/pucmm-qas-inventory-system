-- ===================================================================
-- SCRIPT DE MIGRACIÓN FLYWAY: V7__remove_quantity_from_stock_movements.sql
-- Propósito: Eliminar la columna 'quantity' de 'stock_movements' y su tabla
--            de auditoría. La semántica de "cuánto se movió" queda capturada
--            implícitamente por previousQuantity y newQuantity (diferencia).
--            La columna 'quantity' del request (delta del cliente) vive solo
--            en el DTO de entrada y nunca se persiste.
-- ===================================================================

-- 1. Asegurar backfill de previous_quantity / new_quantity en filas del seed
--    (V5) que se insertaron sin esas columnas y cuyo valor quedó null.
--    Formula: las filas del seed son movimientos IN/OUT iniciales, así que
--    se reconstruyen desde la propia columna quantity antes de borrarla.
UPDATE stock_movements
   SET previous_quantity = 0,
       new_quantity      = quantity
 WHERE previous_quantity IS NULL
   AND new_quantity      IS NULL
   AND movement_type     = 'IN';

-- Para movimientos OUT del seed, la aproximación conservadora es
-- previous = quantity (venía de algún stock positivo) y new = 0.
-- Esto no ocurre en datos reales nuevos (la V6 ya los rellena en tiempo real),
-- pero sí en las filas de OUT del seed de V5.
UPDATE stock_movements
   SET previous_quantity = quantity,
       new_quantity      = 0
 WHERE previous_quantity IS NULL
   AND new_quantity      IS NULL
   AND movement_type     = 'OUT';

-- 2. Ahora que todas las filas tienen previous_quantity y new_quantity,
--    eliminamos la columna redundante.
ALTER TABLE stock_movements DROP COLUMN IF EXISTS quantity;

-- 3. Eliminar también la columna de la tabla de auditoría de Envers.
ALTER TABLE stock_movements_aud DROP COLUMN IF EXISTS quantity;

COMMENT ON TABLE stock_movements IS 'Registro histórico de entradas y salidas de los productos en inventario. El stock de cada movimiento se representa mediante previous_quantity (antes) y new_quantity (después).';
