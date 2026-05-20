-- ============================================================
-- V2: CREATE PROGRESS TABLE
-- ============================================================

CREATE TABLE progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL,
    session_id UUID NOT NULL UNIQUE,

    study_date DATE NOT NULL,

    planned_minutes INTEGER NOT NULL,
    actual_minutes INTEGER,

    completion_percentage DOUBLE PRECISION,

    focus_score SMALLINT,

    completed BOOLEAN NOT NULL,

    remarks TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- ========================================================
    -- FOREIGN KEYS
    -- ========================================================

    CONSTRAINT fk_progress_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_progress_session
        FOREIGN KEY (session_id)
        REFERENCES study_sessions(id)
        ON DELETE CASCADE,

    -- ========================================================
    -- VALIDATIONS
    -- ========================================================

    CONSTRAINT chk_focus_score
        CHECK (focus_score BETWEEN 1 AND 10),

    CONSTRAINT chk_completion_percentage
        CHECK (completion_percentage BETWEEN 0 AND 100)
);

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_progress_user
    ON progress(user_id);

CREATE INDEX idx_progress_date
    ON progress(study_date);

CREATE INDEX idx_progress_completed
    ON progress(completed);