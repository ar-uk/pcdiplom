# Kaspi Scraper Fixes - Summary

## Problem
The Kaspi scraper was failing to save any products because:
1. The offers endpoint (`/yml/offer-view/offers/{sku}`) is protected and returns 403 Forbidden
2. The `scrapeQuery()` method was skipping products when `fetchOfferDetails()` returned null
3. No products were being persisted to the database

## Solution
Modified the scraper to use search-only data from the JSON API endpoint instead of waiting for offers data:

### Changes Made

#### 1. **Updated `scrapeQuery()` method** (Lines 150-198)
**Before:** Called `fetchOfferDetails()` and skipped products if it returned null
**After:** Uses search product data directly without calling `fetchOfferDetails()`

**Key changes:**
```java
// Old: KaspiOfferDetails offerDetails = fetchOfferDetails(product.sku());
// Old: if (offerDetails == null) { continue; } // Skip if no offers

// New: Use product data directly
String title = cleanText(product.title());
BigDecimal price = product.priceKzt() != null ? product.priceKzt() : BigDecimal.ZERO;

ShopProduct normalizedSource = new ShopProduct(
    title,
    price,  // Now using price from search results
    product.url(),
    "in_stock",
    product.sku()
);
```

#### 2. **Updated `KaspiSearchProduct` record** (Line 564)
**Before:** `record KaspiSearchProduct(String sku, String title, String url)`
**After:** `record KaspiSearchProduct(String sku, String title, String url, BigDecimal priceKzt, String description, JsonNode rawItem)`

**Purpose:** Include all available data from search results

#### 3. **Updated `searchProducts()` method** (Lines 203-290)
**Changes:**
- Extract price from JSON attributes: `item.path("attributes").path("price")`
- Extract description from JSON attributes: `item.path("attributes").path("description")`
- Pass all fields when creating `KaspiSearchProduct`:
  ```java
  products.add(new KaspiSearchProduct(sku, title, productUrl, price, description, item));
  ```

#### 4. **Updated `buildSourcePayloadJsonFromSearch()` method** (Lines 467-480)
**Before:** Only included basic fields (retailer, sku, sourceTitle, url, cityId, source)
**After:** Now includes price and description:
```java
payload.put("description", product.description());
payload.put("priceKzt", product.priceKzt());
```

### Data Quality Improvements
- **Price data:** Now extracted from search JSON (may still be sparse)
- **Description:** Now extracted and included in source metadata
- **Product data:** Richer source payload for better normalization context
- **No more missing products:** All search results are processed, not just those with offer data

### Data Available from Search-Only API
From `/yml/product-view/pl/results`:
- ✅ SKU/ID
- ✅ Title
- ✅ Price (when available)
- ✅ Description (when available)
- ✅ Full JsonNode (for additional attributes if available)

**Not available from search API** (requires offers endpoint):
- ❌ Merchant/seller data
- ❌ Detailed pricing options
- ❌ Availability status per merchant
- ❌ Ratings/reviews

### Next Steps for Data Quality
1. **Test the scraper** with `POST /api/scraper/kaspi/scrape-strategies` to verify products are now being saved
2. **Validate product links** by checking saved URLs work: `https://kaspi.kz/shop/p/{sku}/?c=353220100`
3. **Verify price extraction** by comparing saved prices with Kaspi website
4. **Improve normalization** by adjusting confidence scores for search-only data
5. **Extract more fields** from the raw JSON if available (brand, category, specs, etc.)

### Build Status
✅ **BUILD SUCCESSFUL** - Code compiles without errors

### Files Modified
- `partService/src/main/java/org/example/partservice/scraper/KaspiScraperService.java`
  - `scrapeQuery()` method
  - `searchProducts()` method  
  - `buildSourcePayloadJsonFromSearch()` method
  - `KaspiSearchProduct` record definition
  - `applyPriceFilter()` method (already compatible)
