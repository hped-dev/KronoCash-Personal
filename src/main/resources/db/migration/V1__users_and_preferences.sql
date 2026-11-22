-- =============================================================================
-- KronoCash - V1: Identidad y Preferencias de Accesibilidad
-- Módulo 1 (Identidad, Skins y Accesibilidad)
-- Compatible con PostgreSQL 15 y Flyway
-- =============================================================================

-- Tabla de usuarios: credenciales de autenticación y datos básicos de cuenta.
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  UNIQUE NOT NULL,
    email         VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de preferencias: configuración de tema y accesibilidad (WCAG 2.2 AAA)
-- por usuario. Relación 1:1 con users mediante clave primaria foránea.
CREATE TABLE user_preferences (
    user_id       BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    theme_style   VARCHAR(20) NOT NULL DEFAULT 'minimal'
                  CHECK (theme_style IN ('retro', 'minimal', 'cyberpunk')),
    dark_mode     BOOLEAN NOT NULL DEFAULT FALSE,
    reduce_motion BOOLEAN NOT NULL DEFAULT FALSE,
    font_scale    INTEGER NOT NULL DEFAULT 100
                  CHECK (font_scale BETWEEN 100 AND 200)
);
