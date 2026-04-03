-- Split build storage into AI session builds and normal user builds.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'saved_build'
    )
    AND NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'ai_saved_build'
    )
    THEN
        ALTER TABLE saved_build RENAME TO ai_saved_build;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS ai_saved_build (
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

CREATE INDEX IF NOT EXISTS idx_ai_saved_build_user_id ON ai_saved_build (user_id);
CREATE INDEX IF NOT EXISTS idx_ai_saved_build_session_id ON ai_saved_build (session_id);

CREATE TABLE IF NOT EXISTS user_build (
    id BIGSERIAL PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    title VARCHAR(200),
    description TEXT,
    build_json JSONB NOT NULL,
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    source_session_id VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_build_user_id ON user_build (user_id);
CREATE INDEX IF NOT EXISTS idx_user_build_public ON user_build (is_public);
CREATE INDEX IF NOT EXISTS idx_user_build_source_session_id ON user_build (source_session_id);
