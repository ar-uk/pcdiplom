-- Ensure market_listing table exists even when V1 was skipped by Flyway baselining.
CREATE TABLE IF NOT EXISTS market_listing (
    id BIGSERIAL PRIMARY KEY,
    part_type VARCHAR(50) NOT NULL,
    part_id BIGINT NOT NULL,
    part_name VARCHAR(255) NOT NULL,
    retailer VARCHAR(100) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(10) DEFAULT 'KZT',
    url VARCHAR(1000),
    last_scraped TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT market_listing_unique UNIQUE (part_type, part_id, retailer),
    CONSTRAINT market_listing_check_price CHECK (price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_market_listing_part ON market_listing(part_type, part_id);
CREATE INDEX IF NOT EXISTS idx_market_listing_retailer ON market_listing(retailer);
CREATE INDEX IF NOT EXISTS idx_market_listing_active ON market_listing(active);
CREATE INDEX IF NOT EXISTS idx_market_listing_last_scraped ON market_listing(last_scraped);
