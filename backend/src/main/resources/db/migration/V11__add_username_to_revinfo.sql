-- V11__add_username_to_revinfo.sql
-- Agrega la columna de username a la tabla revinfo de Hibernate Envers
-- para rastrear qué usuario ejecutó la acción.

ALTER TABLE revinfo ADD COLUMN username VARCHAR(255);
