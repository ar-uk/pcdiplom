-- Add BuildCores motherboard reference table and parsed match table

CREATE TABLE IF NOT EXISTS reference_motherboard (
    id BIGSERIAL PRIMARY KEY,
    opendb_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    manufacturer VARCHAR(100) NOT NULL,
    series VARCHAR(100),
    variant VARCHAR(100),
    socket VARCHAR(64),
    form_factor VARCHAR(64),
    chipset VARCHAR(128),
    memory_max_gb INT,
    memory_ram_type VARCHAR(32),
    memory_slots INT,
    raw_json TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ref_motherboard_opendb_id ON reference_motherboard(opendb_id);
CREATE INDEX IF NOT EXISTS idx_ref_motherboard_name ON reference_motherboard(LOWER(name));

CREATE TABLE IF NOT EXISTS reference_motherboard_match (
    id BIGSERIAL PRIMARY KEY,
    opendb_id VARCHAR(64),
    name VARCHAR(1024),
    manufacturer VARCHAR(255),
    series VARCHAR(255),
    variant VARCHAR(255),
    socket VARCHAR(255),
    form_factor VARCHAR(255),
    chipset VARCHAR(255),
    memory_max_gb INTEGER,
    memory_ram_type VARCHAR(64),
    memory_slots INTEGER,
    raw_json TEXT,
    parsed_part_id BIGINT,
    source_link VARCHAR(1024)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_ref_motherboard_match_parsed_opendb
    ON reference_motherboard_match(parsed_part_id, opendb_id);
CREATE INDEX IF NOT EXISTS idx_ref_motherboard_match_parsed_part_id
    ON reference_motherboard_match(parsed_part_id);
