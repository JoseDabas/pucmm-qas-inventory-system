-- ===================================================================
-- V4__align_products_aud_category.sql
-- Alinea la tabla de auditoría 'products_aud' con el nuevo modelo
-- normalizado de categorías: la entidad ProductEntity pasó de un
-- 'category' (texto) a un FK 'category_id' (UUID) hacia 'categories'.
-- Envers audita el FK, por lo que la columna de auditoría debe ser
-- 'category_id' en lugar de 'category'.
-- ===================================================================

ALTER TABLE products_aud DROP COLUMN IF EXISTS category;

ALTER TABLE products_aud ADD COLUMN IF NOT EXISTS category_id UUID;

COMMENT ON COLUMN products_aud.category_id IS 'FK auditada hacia la categoría del producto (RelationTargetAuditMode.NOT_AUDITED).';