-- ============================================================
-- V3: ADD VERSION COLUMN TO STUDY PLANS
-- ============================================================
ALTER TABLE study_plans
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;