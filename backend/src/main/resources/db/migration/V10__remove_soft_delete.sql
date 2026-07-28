-- ===================================================================
-- SCRIPT DE MIGRACIÓN FLYWAY: V10__remove_soft_delete.sql
-- Propósito: Revertir el borrado lógico (Soft Delete) introducido en
--            V9__add_soft_delete.sql. Se eliminan las columnas 'deleted'
--            de products, categories y products_aud, volviendo al esquema
--            previo en el que la eliminación es física (hard delete).
-- Nota: Flyway no ejecuta un rollback de V9; esta migración hacia adelante
--       (V10) deshace de forma explícita los cambios de esquema de V9 para
--       mantener consistentes las bases de datos donde V9 ya se aplicó.
-- ===================================================================

-- Columna de auditoría de Envers (se permite NULL en products_aud):
ALTER TABLE products_aud
    DROP COLUMN deleted;

ALTER TABLE categories
    DROP COLUMN deleted;

ALTER TABLE products
    DROP COLUMN deleted;
