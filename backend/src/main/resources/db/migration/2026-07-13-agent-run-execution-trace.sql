-- Apply once when upgrading an existing MySQL database created before 2026-07-13.
ALTER TABLE agent_run
    ADD COLUMN workflow_mode VARCHAR(32) NOT NULL DEFAULT 'RAG_MULTI_AGENT' AFTER final_answer,
    ADD COLUMN failed_role VARCHAR(64) NULL AFTER workflow_mode,
    ADD COLUMN error_summary VARCHAR(500) NULL AFTER failed_role,
    ADD COLUMN started_at DATETIME NULL AFTER error_summary,
    ADD COLUMN finished_at DATETIME NULL AFTER started_at,
    ADD COLUMN metrics_json TEXT NULL AFTER finished_at;

UPDATE agent_run
SET started_at = created_at
WHERE started_at IS NULL;

ALTER TABLE agent_run
    MODIFY COLUMN started_at DATETIME NOT NULL;
