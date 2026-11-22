-- =============================================================================
-- KronoCash - V2: Transacciones, metas de ahorro y hábitos
-- Módulo 2 (Gestión Financiera y Hábitos)
-- Compatible con PostgreSQL 15 y Flyway
-- =============================================================================

-- Ingresos y egresos del usuario. amount siempre positivo; el tipo define
-- el signo econónico en la capa de negocio.
CREATE TABLE transactions (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type             VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    category         VARCHAR(50) NOT NULL,
    amount           NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    description      VARCHAR(255),
    transaction_date DATE NOT NULL DEFAULT CURRENT_DATE
);

-- Metas de ahorro: current_amount puede ser menor, igual o mayor que el
-- objetivo (no se restringe aquí; se valida en la lógica de negocio).
CREATE TABLE savings_goals (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title          VARCHAR(100) NOT NULL,
    target_amount  NUMERIC(12, 2) NOT NULL CHECK (target_amount > 0),
    current_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    deadline       DATE
);

-- Hábitos diarios con control de cumplimiento y racha.
CREATE TABLE habits (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(100) NOT NULL,
    frequency       VARCHAR(20) NOT NULL DEFAULT 'DAILY',
    completed_today BOOLEAN NOT NULL DEFAULT FALSE,
    streak_count    INTEGER NOT NULL DEFAULT 0
);

-- Índices: PostgreSQL no indexa automáticamente las claves foráneas.
CREATE INDEX idx_transactions_user_id ON transactions (user_id);
CREATE INDEX idx_transactions_date ON transactions (transaction_date);
-- Compuesto para el historial filtrado por usuario y rango de fechas.
CREATE INDEX idx_transactions_user_date ON transactions (user_id, transaction_date);
CREATE INDEX idx_savings_goals_user_id ON savings_goals (user_id);
CREATE INDEX idx_habits_user_id ON habits (user_id);
