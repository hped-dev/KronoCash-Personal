-- =============================================================================
-- KronoCash - V3: Escenarios de simulación y logros
-- Módulo 3 (Simulador y Mascota IA)
-- Compatible con PostgreSQL 15 y Flyway
-- =============================================================================

-- Escenarios guardados por el usuario en el simulador financiero.
CREATE TABLE simulation_scenarios (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    scenario_name      VARCHAR(100) NOT NULL,
    monthly_income     NUMERIC(12, 2) NOT NULL,
    savings_percentage NUMERIC(5, 2) NOT NULL CHECK (savings_percentage BETWEEN 0 AND 100),
    ant_expenses_cut   NUMERIC(12, 2) NOT NULL DEFAULT 0,
    months_horizon     INTEGER NOT NULL CHECK (months_horizon BETWEEN 1 AND 120),
    projected_total    NUMERIC(14, 2) NOT NULL,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insignias desbloqueadas por el usuario (educación financiera y hábitos).
CREATE TABLE achievements (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    code        VARCHAR(50) NOT NULL,
    title       VARCHAR(100) NOT NULL,
    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
