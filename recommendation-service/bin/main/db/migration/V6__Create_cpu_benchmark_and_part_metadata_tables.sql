CREATE TABLE IF NOT EXISTS cpu_benchmark (
    id UUID PRIMARY KEY,
    cpu_name VARCHAR(255) NOT NULL UNIQUE,
    score_1080p DOUBLE PRECISION,
    score_1440p DOUBLE PRECISION,
    tdp_watts INTEGER,
    source_name VARCHAR(255) NOT NULL,
    source_version VARCHAR(64) NOT NULL,
    imported_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS part_metadata (
    id UUID PRIMARY KEY,
    part_type VARCHAR(64) NOT NULL,
    part_name VARCHAR(255) NOT NULL,
    tdp_watts INTEGER,
    socket VARCHAR(64),
    memory_type VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS idx_part_metadata_type_name ON part_metadata(part_type, part_name);