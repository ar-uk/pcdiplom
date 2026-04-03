-- Dedicated parsed tables for scraped results.
-- These tables are separate from existing part catalog tables.

CREATE TABLE IF NOT EXISTS parsed_cpu (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_kzt NUMERIC(12,2),
    retailer VARCHAR(80) NOT NULL DEFAULT 'shop.kz',
    currency VARCHAR(8) NOT NULL DEFAULT 'KZT',
    url TEXT NOT NULL,
    last_scraped TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_parsed_cpu_url UNIQUE (url)
);

CREATE TABLE IF NOT EXISTS parsed_video_card (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_kzt NUMERIC(12,2),
    retailer VARCHAR(80) NOT NULL DEFAULT 'shop.kz',
    currency VARCHAR(8) NOT NULL DEFAULT 'KZT',
    url TEXT NOT NULL,
    last_scraped TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_parsed_video_card_url UNIQUE (url)
);

CREATE TABLE IF NOT EXISTS parsed_power_supply (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_kzt NUMERIC(12,2),
    retailer VARCHAR(80) NOT NULL DEFAULT 'shop.kz',
    currency VARCHAR(8) NOT NULL DEFAULT 'KZT',
    url TEXT NOT NULL,
    last_scraped TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_parsed_power_supply_url UNIQUE (url)
);

CREATE TABLE IF NOT EXISTS parsed_pc_case (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_kzt NUMERIC(12,2),
    retailer VARCHAR(80) NOT NULL DEFAULT 'shop.kz',
    currency VARCHAR(8) NOT NULL DEFAULT 'KZT',
    url TEXT NOT NULL,
    last_scraped TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_parsed_pc_case_url UNIQUE (url)
);

CREATE TABLE IF NOT EXISTS parsed_memory (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_kzt NUMERIC(12,2),
    retailer VARCHAR(80) NOT NULL DEFAULT 'shop.kz',
    currency VARCHAR(8) NOT NULL DEFAULT 'KZT',
    url TEXT NOT NULL,
    last_scraped TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_parsed_memory_url UNIQUE (url)
);

CREATE TABLE IF NOT EXISTS parsed_internal_hard_drive (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_kzt NUMERIC(12,2),
    retailer VARCHAR(80) NOT NULL DEFAULT 'shop.kz',
    currency VARCHAR(8) NOT NULL DEFAULT 'KZT',
    url TEXT NOT NULL,
    last_scraped TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_parsed_internal_hard_drive_url UNIQUE (url)
);

CREATE TABLE IF NOT EXISTS parsed_motherboard (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_kzt NUMERIC(12,2),
    retailer VARCHAR(80) NOT NULL DEFAULT 'shop.kz',
    currency VARCHAR(8) NOT NULL DEFAULT 'KZT',
    url TEXT NOT NULL,
    last_scraped TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_parsed_motherboard_url UNIQUE (url)
);
