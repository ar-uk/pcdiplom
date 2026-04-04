CREATE TABLE IF NOT EXISTS parsed_cpu_cooler (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_kzt NUMERIC(12,2),
    retailer VARCHAR(80) NOT NULL DEFAULT 'shop.kz',
    currency VARCHAR(8) NOT NULL DEFAULT 'KZT',
    url TEXT NOT NULL,
    last_scraped TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_parsed_cpu_cooler_url UNIQUE (url)
);
