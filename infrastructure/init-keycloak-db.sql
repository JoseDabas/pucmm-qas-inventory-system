-- Crea la base dedicada de Keycloak, separada de la base de la aplicación.
-- Se ejecuta automáticamente al primer arranque de Postgres (volumen vacío).
CREATE DATABASE keycloak_db;