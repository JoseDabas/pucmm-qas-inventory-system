-- ===================================================================
-- SCRIPT DE MIGRACIÓN FLYWAY: V2__create_stock_movement_schema.sql
-- Propósito: Crear la tabla 'stock_movements' para almacenar el registro 
--            histórico de entradas y salidas de inventario.
-- ===================================================================

CREATE TABLE stock_movements (
    
    -- Clave primaria del movimiento
    id UUID PRIMARY KEY,
    
    -- Clave foránea que referencia al producto afectado en la tabla 'products'
    product_id UUID NOT NULL,
    
    -- Tipo de movimiento (IN o OUT) almacenado como cadena de texto
    movement_type VARCHAR(50) NOT NULL,
    
    -- Cantidad movida, que por regla de negocio debe ser estrictamente positiva
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    
    -- Fecha y hora en la que se generó la transacción de stock
    movement_date TIMESTAMP NOT NULL,
    
    -- Nombre o identificador del usuario que realizó la acción
    username VARCHAR(150) NOT NULL,
    
    -- Comentarios o justificaciones adicionales para el movimiento
    observations TEXT,

    -- Restricción de integridad referencial para evitar eliminar un producto
    -- que tenga historial de movimientos asociado.
    CONSTRAINT fk_stock_movement_product FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT
);

-- ===================================================================
-- COMENTARIOS DE BASE DE DATOS
-- Proveen contexto directamente en el esquema para administradores o analistas
-- ===================================================================

COMMENT ON TABLE stock_movements IS 'Registro histórico de entradas y salidas de los productos en inventario.';
COMMENT ON COLUMN stock_movements.id IS 'Identificador único del movimiento de stock.';
COMMENT ON COLUMN stock_movements.product_id IS 'Referencia FK hacia la tabla de productos.';
COMMENT ON COLUMN stock_movements.movement_type IS 'Define si es una entrada (IN) o una salida (OUT).';
COMMENT ON COLUMN stock_movements.quantity IS 'Cantidad de unidades afectadas en la transacción.';
COMMENT ON COLUMN stock_movements.movement_date IS 'Fecha y hora exacta de registro del movimiento.';
COMMENT ON COLUMN stock_movements.username IS 'Usuario que solicitó u operó la transacción.';
COMMENT ON COLUMN stock_movements.observations IS 'Detalles y notas aclaratorias opcionales.';
