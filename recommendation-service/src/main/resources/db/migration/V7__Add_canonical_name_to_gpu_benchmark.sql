ALTER TABLE IF EXISTS gpu_benchmark
    ADD COLUMN IF NOT EXISTS canonical_name VARCHAR(255);

UPDATE gpu_benchmark
SET canonical_name = gpu_name
WHERE canonical_name IS NULL OR canonical_name = '';

ALTER TABLE IF EXISTS gpu_benchmark
    ALTER COLUMN canonical_name SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_gpu_benchmark_canonical_name ON gpu_benchmark(canonical_name);