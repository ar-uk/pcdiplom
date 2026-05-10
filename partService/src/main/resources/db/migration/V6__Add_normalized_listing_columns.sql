ALTER TABLE IF EXISTS parsed_cpu
    ADD COLUMN IF NOT EXISTS normalized_name TEXT,
    ADD COLUMN IF NOT EXISTS normalized_specs_json TEXT,
    ADD COLUMN IF NOT EXISTS source_payload_json TEXT,
    ADD COLUMN IF NOT EXISTS confidence_score NUMERIC(4,3);

ALTER TABLE IF EXISTS parsed_video_card
    ADD COLUMN IF NOT EXISTS normalized_name TEXT,
    ADD COLUMN IF NOT EXISTS normalized_specs_json TEXT,
    ADD COLUMN IF NOT EXISTS source_payload_json TEXT,
    ADD COLUMN IF NOT EXISTS confidence_score NUMERIC(4,3);

ALTER TABLE IF EXISTS parsed_power_supply
    ADD COLUMN IF NOT EXISTS normalized_name TEXT,
    ADD COLUMN IF NOT EXISTS normalized_specs_json TEXT,
    ADD COLUMN IF NOT EXISTS source_payload_json TEXT,
    ADD COLUMN IF NOT EXISTS confidence_score NUMERIC(4,3);

ALTER TABLE IF EXISTS parsed_pc_case
    ADD COLUMN IF NOT EXISTS normalized_name TEXT,
    ADD COLUMN IF NOT EXISTS normalized_specs_json TEXT,
    ADD COLUMN IF NOT EXISTS source_payload_json TEXT,
    ADD COLUMN IF NOT EXISTS confidence_score NUMERIC(4,3);

ALTER TABLE IF EXISTS parsed_memory
    ADD COLUMN IF NOT EXISTS normalized_name TEXT,
    ADD COLUMN IF NOT EXISTS normalized_specs_json TEXT,
    ADD COLUMN IF NOT EXISTS source_payload_json TEXT,
    ADD COLUMN IF NOT EXISTS confidence_score NUMERIC(4,3);

ALTER TABLE IF EXISTS parsed_internal_hard_drive
    ADD COLUMN IF NOT EXISTS normalized_name TEXT,
    ADD COLUMN IF NOT EXISTS normalized_specs_json TEXT,
    ADD COLUMN IF NOT EXISTS source_payload_json TEXT,
    ADD COLUMN IF NOT EXISTS confidence_score NUMERIC(4,3);

ALTER TABLE IF EXISTS parsed_motherboard
    ADD COLUMN IF NOT EXISTS normalized_name TEXT,
    ADD COLUMN IF NOT EXISTS normalized_specs_json TEXT,
    ADD COLUMN IF NOT EXISTS source_payload_json TEXT,
    ADD COLUMN IF NOT EXISTS confidence_score NUMERIC(4,3);

ALTER TABLE IF EXISTS parsed_cpu_cooler
    ADD COLUMN IF NOT EXISTS normalized_name TEXT,
    ADD COLUMN IF NOT EXISTS normalized_specs_json TEXT,
    ADD COLUMN IF NOT EXISTS source_payload_json TEXT,
    ADD COLUMN IF NOT EXISTS confidence_score NUMERIC(4,3);