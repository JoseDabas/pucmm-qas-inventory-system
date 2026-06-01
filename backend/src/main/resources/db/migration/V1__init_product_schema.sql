-- ===================================================================
-- SCRIPT DE MIGRACIÓN: V1__init_product_schema.sql
-- Propósito: Crear el esquema inicial para la entidad Product.
-- Motor de Base de Datos: PostgreSQL.
-- Gestión: Administrado por Flyway.
-- ===================================================================

-- Crear la tabla 'products'
-- Esta tabla mapea de manera precisa a los atributos definidos en la
-- clase de dominio edu.pucmm.cs.inventory.domain.Product.
CREATE TABLE products (
    
    -- Identificador único del producto. Utilizamos UUID como llave primaria.
    -- Es una práctica recomendada en sistemas distribuidos para evitar colisiones.
    id UUID PRIMARY KEY,
    
    -- Nombre del producto. Se define como NOT NULL ya que es obligatorio.
    name VARCHAR(255) NOT NULL,
    
    -- Código SKU (Stock Keeping Unit). 
    -- Se define como NOT NULL y UNIQUE para garantizar que no existan
    -- dos productos distintos con el mismo identificador de stock.
    sku_code VARCHAR(100) NOT NULL UNIQUE,
    
    -- Descripción del producto. Puede ser nula, por ende no se especifica NOT NULL.
    description TEXT,
    
    -- Categoría a la que pertenece el producto.
    category VARCHAR(150),
    
    -- Precio unitario del producto.
    -- Se usa DECIMAL(19,4) para manejar montos monetarios de forma precisa
    -- y evitar problemas de redondeo comunes con tipos float/double.
    -- CHECK constraint asegura que el precio en base de datos nunca sea negativo.
    price DECIMAL(19, 4) NOT NULL CHECK (price >= 0),
    
    -- Cantidad inicial registrada. No puede ser negativa.
    initial_quantity INTEGER NOT NULL CHECK (initial_quantity >= 0),
    
    -- Nivel de stock mínimo para alertas. No puede ser negativo.
    minimum_stock INTEGER NOT NULL CHECK (minimum_stock >= 0),
    
    -- Bandera booleana para determinar si el producto está activo en el sistema.
    -- Se define por defecto en TRUE.
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- ===================================================================
-- COMENTARIOS DE TABLA Y COLUMNAS
-- Para mejorar la observabilidad e introspección de la base de datos,
-- documentamos la tabla y sus columnas.
-- ===================================================================

COMMENT ON TABLE products IS 'Almacena los productos gestionados por el sistema de inventario empresarial.';
COMMENT ON COLUMN products.id IS 'Llave primaria en formato UUID.';
COMMENT ON COLUMN products.name IS 'Nombre descriptivo del producto.';
COMMENT ON COLUMN products.sku_code IS 'Código único de mantenimiento de existencias (SKU).';
COMMENT ON COLUMN products.description IS 'Detalles y características del producto.';
COMMENT ON COLUMN products.category IS 'Categoría de clasificación del producto.';
COMMENT ON COLUMN products.price IS 'Precio unitario con precisión de 4 decimales.';
COMMENT ON COLUMN products.initial_quantity IS 'Cantidad registrada al momento de la creación.';
COMMENT ON COLUMN products.minimum_stock IS 'Umbral de stock para generar alertas de reabastecimiento.';
COMMENT ON COLUMN products.is_active IS 'Indica si el producto está disponible y activo.';
