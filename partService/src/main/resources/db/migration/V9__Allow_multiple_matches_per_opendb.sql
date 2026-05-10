-- Allow the same opendb_id to be used for multiple parsed parts
-- Drop unique index on opendb_id and create a composite unique index on (parsed_part_id, opendb_id)

-- CPU matches
DROP INDEX IF EXISTS idx_ref_cpu_match_opendb_id;
CREATE UNIQUE INDEX IF NOT EXISTS idx_ref_cpu_match_parsed_opendb ON reference_cpu_match(parsed_part_id, opendb_id);

-- GPU matches
DROP INDEX IF EXISTS idx_ref_gpu_match_opendb_id;
CREATE UNIQUE INDEX IF NOT EXISTS idx_ref_gpu_match_parsed_opendb ON reference_gpu_match(parsed_part_id, opendb_id);
