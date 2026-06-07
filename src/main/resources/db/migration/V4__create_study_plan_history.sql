-- ============================================================
-- V4: Study Plan Versioning — History Tables
-- ============================================================

-- 1. History snapshot of plan metadata at each version
CREATE TABLE study_plan_history (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id             UUID NOT NULL REFERENCES study_plans(id) ON DELETE CASCADE,
    version_number      INT  NOT NULL,
    title               VARCHAR(200) NOT NULL,
    goal                TEXT,
    difficulty          VARCHAR(20) NOT NULL,
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    daily_hours         INT  NOT NULL,
    status              VARCHAR(20) NOT NULL,
    total_sessions      INT  NOT NULL DEFAULT 0,
    completed_sessions  INT  NOT NULL DEFAULT 0,
    change_reason       VARCHAR(500),
    snapshot_at         TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_plan_version UNIQUE (plan_id, version_number)
);

-- 2. History snapshot of sessions tied to a plan version
CREATE TABLE study_session_history (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_history_id         UUID NOT NULL REFERENCES study_plan_history(id) ON DELETE CASCADE,
    original_session_id     UUID,
    scheduled_date          DATE NOT NULL,
    topic                   VARCHAR(200) NOT NULL,
    duration_minutes        INT  NOT NULL,
    completed               BOOLEAN NOT NULL DEFAULT FALSE,
    actual_duration_minutes INT,
    focus_score             SMALLINT,
    notes                   TEXT,
    completed_at            TIMESTAMP
);

-- 3. Performance indexes
CREATE INDEX idx_plan_history_plan       ON study_plan_history(plan_id);
CREATE INDEX idx_plan_history_version    ON study_plan_history(plan_id, version_number);
CREATE INDEX idx_session_history_parent  ON study_session_history(plan_history_id);

-- 4. Add change_reason to live study_plans table
ALTER TABLE study_plans ADD COLUMN change_reason VARCHAR(500);
