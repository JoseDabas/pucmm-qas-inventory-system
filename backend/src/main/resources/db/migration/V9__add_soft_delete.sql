-- ===================================================================
-- SCRIPT DE MIGRACIÓN FLYWAY: V9__add_soft_delete.sql
-- Propósito: Habilitar el borrado lógico (Soft Delete) de productos y
--            categorías. En lugar de eliminar físicamente la fila, se marca
--            la columna 'deleted'. La anotación @SoftDelete de Hibernate
--            reescribe los DELETE como UPDATE deleted = true y filtra todas
--            las consultas con WHERE deleted = false.
-- ===================================================================

-- Columna indicadora usada por @SoftDelete (estrategia DELETED por defecto):
--   false = activo (visible)   |   true = borrado (oculto)
-- Las filas existentes quedan activas gracias al DEFAULT FALSE.
ALTER TABLE products
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE categories
    ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- 'products' está auditada con Hibernate Envers (@Audited). Añadimos la columna
-- también a la tabla de auditoría para que Envers pueda registrar la marca en
-- cada revisión sin fallar por columna inexistente. En el histórico se permite
-- NULL (auditorías parciales), igual que el resto de columnas de products_aud.
ALTER TABLE products_aud
    ADD COLUMN deleted BOOLEAN;
