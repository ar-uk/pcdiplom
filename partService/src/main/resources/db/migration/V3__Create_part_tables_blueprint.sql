-- Blueprint migration for dedicated part tables used by CSV imports.
-- Uses IF NOT EXISTS so it is safe on environments where tables already exist.

CREATE TABLE IF NOT EXISTS cpu (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_usd NUMERIC(12,2),
    core_count INTEGER,
    core_clock NUMERIC(6,2),
    boost_clock NUMERIC(6,2),
    microarchitecture VARCHAR(120),
    tdp INTEGER,
    graphics VARCHAR(120)
);

CREATE INDEX IF NOT EXISTS idx_cpu_name ON cpu (LOWER(name));

CREATE TABLE IF NOT EXISTS video_card (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_usd NUMERIC(12,2),
    chipset VARCHAR(120),
    memory_gb INTEGER,
    core_clock INTEGER,
    boost_clock INTEGER,
    color VARCHAR(80),
    length_mm INTEGER
);

CREATE INDEX IF NOT EXISTS idx_video_card_name ON video_card (LOWER(name));

CREATE TABLE IF NOT EXISTS power_supply (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_usd NUMERIC(12,2),
    psu_type VARCHAR(120),
    efficiency VARCHAR(60),
    wattage INTEGER,
    modular VARCHAR(40),
    color VARCHAR(80)
);

CREATE INDEX IF NOT EXISTS idx_power_supply_name ON power_supply (LOWER(name));

CREATE TABLE IF NOT EXISTS pc_case (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_usd NUMERIC(12,2),
    case_type VARCHAR(120),
    color VARCHAR(80),
    psu VARCHAR(120),
    side_panel VARCHAR(120),
    external_volume NUMERIC(10,2),
    internal_35_bays INTEGER
);

CREATE INDEX IF NOT EXISTS idx_pc_case_name ON pc_case (LOWER(name));

-- Logical part: RAM
CREATE TABLE IF NOT EXISTS memory (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_usd NUMERIC(12,2),
    ddr INTEGER,
    speed_mhz INTEGER,
    sticks INTEGER,
    gb_per_stick INTEGER,
    price_per_gb NUMERIC(10,4),
    color VARCHAR(80),
    first_word_latency NUMERIC(8,3),
    cas_latency INTEGER
);

CREATE INDEX IF NOT EXISTS idx_memory_name ON memory (LOWER(name));

-- Logical part: INTERNAL_MEMORY
CREATE TABLE IF NOT EXISTS internal_hard_drive (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_usd NUMERIC(12,2),
    capacity_gb INTEGER,
    price_per_gb NUMERIC(10,4),
    drive_type VARCHAR(60),
    cache_mb INTEGER,
    form_factor VARCHAR(60),
    interface VARCHAR(60)
);

CREATE INDEX IF NOT EXISTS idx_internal_hard_drive_name ON internal_hard_drive (LOWER(name));

CREATE TABLE IF NOT EXISTS motherboard (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_usd NUMERIC(12,2),
    socket VARCHAR(80),
    form_factor VARCHAR(60),
    max_memory INTEGER,
    memory_slots INTEGER,
    color VARCHAR(80)
);

CREATE INDEX IF NOT EXISTS idx_motherboard_name ON motherboard (LOWER(name));
