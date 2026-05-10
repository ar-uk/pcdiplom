-- Flyway migration: create tables to store matched reference data
-- Stores the reference fields plus a link and parsed part id from parsed tables

CREATE TABLE IF NOT EXISTS reference_cpu_match (
    id BIGSERIAL PRIMARY KEY,
    opendb_id VARCHAR(64),
    name VARCHAR(1024),
    manufacturer VARCHAR(255),
    series VARCHAR(255),
    variant VARCHAR(255),
    cores INTEGER,
    threads INTEGER,
    base_clock_ghz NUMERIC(5,2),
    boost_clock_ghz NUMERIC(5,2),
    socket VARCHAR(255),
    microarchitecture VARCHAR(255),
    tdp INTEGER,
    raw_json TEXT,
    parsed_part_id BIGINT,
    source_link VARCHAR(1024)
);

CREATE INDEX IF NOT EXISTS idx_ref_cpu_match_opendb_id ON reference_cpu_match(opendb_id);
CREATE INDEX IF NOT EXISTS idx_ref_cpu_match_parsed_part_id ON reference_cpu_match(parsed_part_id);

CREATE TABLE IF NOT EXISTS reference_gpu_match (
    id BIGSERIAL PRIMARY KEY,
    opendb_id VARCHAR(64),
    name VARCHAR(1024),
    manufacturer VARCHAR(255),
    chipset VARCHAR(255),
    memory_gb INTEGER,
    memory_type VARCHAR(255),
    base_clock_mhz INTEGER,
    boost_clock_mhz INTEGER,
    core_count INTEGER,
    interface_ VARCHAR(255),
    tdp INTEGER,
    release_year INTEGER,
    raw_json TEXT,
    parsed_part_id BIGINT,
    source_link VARCHAR(1024)
);

CREATE INDEX IF NOT EXISTS idx_ref_gpu_match_opendb_id ON reference_gpu_match(opendb_id);
CREATE INDEX IF NOT EXISTS idx_ref_gpu_match_parsed_part_id ON reference_gpu_match(parsed_part_id);
