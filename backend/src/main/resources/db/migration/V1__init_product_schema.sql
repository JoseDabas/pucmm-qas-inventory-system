-- ===================================================================
-- V1__init_product_schema.sql
-- Crea el esquema inicial: categorías y productos.
-- Motor: PostgreSQL. Gestión: Flyway.
-- ===================================================================

-- Tabla de categorías (referenciada por products vía FK).
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL UNIQUE,
    description TEXT
);

-- Tabla de productos.
-- Mapea a la entidad edu.pucmm.cs.inventory.domain.Product.
CREATE TABLE products (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    sku_code VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    category_id UUID REFERENCES categories (id),
    price DECIMAL(19, 4) NOT NULL CHECK (price >= 0),
    initial_quantity INTEGER NOT NULL CHECK (initial_quantity >= 0),
    minimum_stock INTEGER NOT NULL CHECK (minimum_stock >= 0),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_products_category ON products (category_id);

-- Comentarios para introspección de la base de datos.
COMMENT ON
TABLE categories IS 'Categorías de clasificación de productos.';

COMMENT ON
TABLE products IS 'Productos gestionados por el sistema de inventario.';

COMMENT ON COLUMN products.sku_code IS 'Código único de existencias (SKU).';

COMMENT ON COLUMN products.category_id IS 'FK a la categoría del producto.';

COMMENT ON COLUMN products.initial_quantity IS 'Cantidad registrada al crear el producto.';

COMMENT ON COLUMN products.minimum_stock IS 'Umbral de stock para alertas.';