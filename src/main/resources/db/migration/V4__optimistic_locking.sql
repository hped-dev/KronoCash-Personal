-- =============================================================================
-- KronoCash - V4: Control optimista de concurrencia
-- Permite detectar escrituras simultáneas sobre importes y rachas.
-- =============================================================================

ALTER TABLE transactions
    ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE savings_goals
    ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

ALTER TABLE habits
    ADD COLUMN version INTEGER NOT NULL DEFAULT 0;
