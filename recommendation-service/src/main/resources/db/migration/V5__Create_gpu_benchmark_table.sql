CREATE TABLE IF NOT EXISTS gpu_benchmark (
    id UUID PRIMARY KEY,
    gpu_name VARCHAR(255) NOT NULL UNIQUE,
    canonical_name VARCHAR(255) NOT NULL UNIQUE,
    score_1080p DOUBLE PRECISION,
    score_1440p DOUBLE PRECISION,
    score_4k DOUBLE PRECISION,
    vram_gb INTEGER,
    tdp_watts INTEGER,
    source_name VARCHAR(255) NOT NULL,
    source_version VARCHAR(64) NOT NULL,
    imported_at TIMESTAMP WITHOUT TIME ZONE NOT NULL
);