CREATE TABLE IF NOT EXISTS part_performance_mapping (
    id BIGSERIAL PRIMARY KEY,
    part_id BIGINT NOT NULL,
    part_type VARCHAR(64) NOT NULL,
    performance_score INTEGER NOT NULL,
    tier_label VARCHAR(64) NOT NULL,
    score_version VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_part_performance_mapping UNIQUE (part_id, part_type, score_version)
);

CREATE INDEX IF NOT EXISTS idx_part_performance_mapping_lookup
    ON part_performance_mapping (part_type, part_id, score_version);
