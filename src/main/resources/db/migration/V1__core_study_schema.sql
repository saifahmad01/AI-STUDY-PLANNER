-- ─────────────────────────────────────────────────────────────
-- V1: Initial Core Schema (Users + Study Planner Domain)
-- ─────────────────────────────────────────────────────────────

-- ============================================================
-- 1. EXTENSIONS
-- ============================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- 2. COMMON FUNCTIONS
-- ============================================================

-- Auto update updated_at column
CREATE OR REPLACE FUNCTION trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Update completed session counter
CREATE OR REPLACE FUNCTION update_plan_session_counts()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.completed = TRUE AND OLD.completed = FALSE THEN
        UPDATE study_plans
        SET completed_sessions = completed_sessions + 1
        WHERE id = NEW.plan_id;

    ELSIF NEW.completed = FALSE AND OLD.completed = TRUE THEN
        UPDATE study_plans
        SET completed_sessions = GREATEST(completed_sessions - 1, 0)
        WHERE id = NEW.plan_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- 3. USERS TABLE (REQUIRED BASE)
-- ============================================================
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 4. SUBJECTS
-- ============================================================
CREATE TABLE subjects (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name         VARCHAR(150) NOT NULL,
    category     VARCHAR(80),
    color_hex    VARCHAR(7) DEFAULT '#6B7280',
    is_archived  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- 5. STUDY PLANS
-- ============================================================
CREATE TABLE study_plans (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    subject_id          UUID REFERENCES subjects(id) ON DELETE SET NULL,
    title               VARCHAR(200) NOT NULL,
    goal                TEXT,
    difficulty          VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'
                            CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    start_date          DATE NOT NULL,
    end_date            DATE NOT NULL,
    daily_hours         INT NOT NULL DEFAULT 2
                            CHECK (daily_hours BETWEEN 1 AND 16),
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE', 'PAUSED', 'COMPLETED', 'ARCHIVED')),

    total_sessions      INT NOT NULL DEFAULT 0,
    completed_sessions  INT NOT NULL DEFAULT 0,

    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT end_after_start CHECK (end_date > start_date)
);

-- ============================================================
-- 6. STUDY SESSIONS
-- ============================================================
CREATE TABLE study_sessions (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id                 UUID NOT NULL REFERENCES study_plans(id) ON DELETE CASCADE,
    scheduled_date          DATE NOT NULL,
    topic                   VARCHAR(200) NOT NULL,
    duration_minutes        INT NOT NULL CHECK (duration_minutes > 0),

    completed               BOOLEAN NOT NULL DEFAULT FALSE,
    actual_duration_minutes INT,
    focus_score             SMALLINT CHECK (focus_score BETWEEN 1 AND 10),
    notes                   TEXT,
    completed_at            TIMESTAMP
);

-- ============================================================
-- 7. INDEXES (PERFORMANCE)
-- ============================================================
CREATE INDEX idx_subjects_user         ON subjects(user_id);
CREATE INDEX idx_study_plans_user      ON study_plans(user_id);
CREATE INDEX idx_study_plans_subject   ON study_plans(subject_id);
CREATE INDEX idx_study_plans_status    ON study_plans(user_id, status);
CREATE INDEX idx_sessions_plan         ON study_sessions(plan_id);
CREATE INDEX idx_sessions_date         ON study_sessions(scheduled_date);
CREATE INDEX idx_sessions_plan_date    ON study_sessions(plan_id, scheduled_date);
CREATE INDEX idx_sessions_plan_done    ON study_sessions(plan_id, completed);

-- ============================================================
-- 8. TRIGGERS
-- ============================================================

-- updated_at auto-update
CREATE TRIGGER trg_update_plan_timestamp
BEFORE UPDATE ON study_plans
FOR EACH ROW
EXECUTE FUNCTION trigger_set_updated_at();

-- session completion tracking
CREATE TRIGGER trg_session_completion
AFTER UPDATE OF completed ON study_sessions
FOR EACH ROW
EXECUTE FUNCTION update_plan_session_counts();