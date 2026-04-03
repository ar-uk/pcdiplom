CREATE TABLE IF NOT EXISTS saved_build (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    user_id VARCHAR(128),
    prompt TEXT NOT NULL,
    currency VARCHAR(16),
    region VARCHAR(32),
    strict_budget BOOLEAN NOT NULL DEFAULT FALSE,
    intent_json JSONB NOT NULL,
    build_json JSONB NOT NULL,
    totals_json JSONB NOT NULL,
    checks_json JSONB NOT NULL,
    reasoning_json JSONB NOT NULL,
    alternatives_json JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_saved_build_user_id ON saved_build (user_id);
CREATE INDEX IF NOT EXISTS idx_saved_build_session_id ON saved_build (session_id);
