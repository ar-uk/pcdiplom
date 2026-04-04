CREATE TABLE IF NOT EXISTS manual_build_draft (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    title VARCHAR(200),
    build_json JSONB NOT NULL,
    estimated_power INTEGER NOT NULL DEFAULT 0,
    compatibility_issues_json JSONB NOT NULL,
    is_finalized BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_manual_build_draft_user_id ON manual_build_draft (user_id);
CREATE INDEX IF NOT EXISTS idx_manual_build_draft_finalized ON manual_build_draft (is_finalized);
