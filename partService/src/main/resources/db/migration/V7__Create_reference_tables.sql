-- Create reference CPU table - BuildCores reference data
CREATE TABLE IF NOT EXISTS reference_cpu (
    id BIGSERIAL PRIMARY KEY,
    opendb_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    manufacturer VARCHAR(100),
    series VARCHAR(100),
    variant VARCHAR(100),
    cores INT,
    threads INT,
    base_clock_ghz NUMERIC(5, 2),
    boost_clock_ghz NUMERIC(5, 2),
    socket VARCHAR(50),
    microarchitecture VARCHAR(100),
    tdp INT,
    raw_json TEXT
);

-- Create reference GPU table - BuildCores reference data
CREATE TABLE IF NOT EXISTS reference_gpu (
    id BIGSERIAL PRIMARY KEY,
    opendb_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    manufacturer VARCHAR(100),
    series VARCHAR(100),
    variant VARCHAR(100),
    chipset VARCHAR(100),
    memory_gb INT,
    memory_type VARCHAR(50),
    base_clock_mhz NUMERIC(10, 2),
    boost_clock_mhz NUMERIC(10, 2),
    core_count INT,
    interface_ VARCHAR(100),
    tdp INT,
    release_year INT,
    raw_json TEXT
);
