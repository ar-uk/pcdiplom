-- Add price_kzt column to reference match tables to enable pricing in recommendation service

ALTER TABLE IF EXISTS reference_cpu_match
    ADD COLUMN IF NOT EXISTS price_kzt NUMERIC(12, 2);

ALTER TABLE IF EXISTS reference_gpu_match
    ADD COLUMN IF NOT EXISTS price_kzt NUMERIC(12, 2);

ALTER TABLE IF EXISTS reference_motherboard_match
    ADD COLUMN IF NOT EXISTS price_kzt NUMERIC(12, 2);

CREATE INDEX IF NOT EXISTS idx_ref_cpu_match_price ON reference_cpu_match(price_kzt);
CREATE INDEX IF NOT EXISTS idx_ref_gpu_match_price ON reference_gpu_match(price_kzt);
CREATE INDEX IF NOT EXISTS idx_ref_motherboard_match_price ON reference_motherboard_match(price_kzt);
