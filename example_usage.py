#!/usr/bin/env python3
"""
BuildCores Reference System - Integration Example
Demonstrates how to use the GPU/CPU matching system with shop.kz products
"""

import requests
import json
from typing import Optional, Dict, List

class BuildCoresClient:
    """Client for BuildCores reference matching system"""
    
    def __init__(self, base_url: str = "http://localhost:8080"):
        self.base_url = base_url
        self.api_prefix = f"{base_url}/api/reference"
    
    def load_all_references(self) -> bool:
        """Load all reference data from BuildCores"""
        try:
            response = requests.post(f"{self.api_prefix}/load/all", timeout=30)
            return response.status_code == 200
        except Exception as e:
            print(f"Error loading references: {e}")
            return False
    
    def match_product(self, product_name: str) -> Optional[Dict]:
        """
        Match a product name against references
        Returns: MatchResult with confidence score (0.0-1.0)
        """
        try:
            response = requests.post(
                f"{self.api_prefix}/match",
                params={"productName": product_name},
                timeout=5
            )
            if response.status_code == 200:
                return response.json()
            return None
        except Exception as e:
            print(f"Error matching product '{product_name}': {e}")
            return None
    
    def process_shop_products(self, products: List[str]) -> List[Dict]:
        """Process multiple shop products and return matches with confidence scores"""
        results = []
        for product in products:
            match = self.match_product(product)
            results.append({
                "input": product,
                "match": match,
                "action": self._determine_action(match)
            })
        return results
    
    @staticmethod
    def _determine_action(match: Optional[Dict]) -> str:
        """Determine what action to take based on confidence score"""
        if match is None:
            return "REJECT - No match found"
        
        confidence = match.get("confidence", 0)
        if confidence >= 0.95:
            return "AUTO_ACCEPT - High confidence match"
        elif confidence >= 0.75:
            return "REVIEW - Good match, needs verification"
        elif confidence >= 0.50:
            return "MANUAL_CHECK - Moderate match"
        else:
            return "REJECT - Low confidence"


def example_usage():
    """Example: Using the matching system with shop.kz products"""
    
    client = BuildCoresClient()
    
    print("═" * 80)
    print("BuildCores Reference Matching System - Example Usage")
    print("═" * 80)
    
    # Step 1: Load reference data
    print("\n[1/3] Loading BuildCores reference data...")
    if client.load_all_references():
        print("✅ Reference data loaded successfully")
    else:
        print("❌ Failed to load reference data")
        return
    
    # Step 2: Example shop.kz products to match
    print("\n[2/3] Matching shop.kz products against references...\n")
    
    shop_products = [
        "Intel Core i9-13900KS 24 Core 36MB Cache LGA1700",
        "AMD Ryzen 9 7950X 16-Core Gaming CPU",
        "NVIDIA GeForce RTX 4090 24GB GDDR6X",
        "Intel Arc A770 8GB Graphics Card",
        "Ryzen 5 5600X Gaming Processor",
        "RTX 4080 Super 16GB VRAM",
        "Gaming GPU 8GB Memory",  # Low info - should have low confidence
        "AMD processor with 16 cores",  # Ambiguous - should need review
    ]
    
    results = client.process_shop_products(shop_products)
    
    # Step 3: Display results
    print("[3/3] Results:\n")
    print(f"{'Product Name':<50} {'Confidence':<12} {'Type':<6} {'Action':<30}")
    print("─" * 100)
    
    for result in results:
        product = result["input"]
        match = result["match"]
        action = result["action"]
        
        if match:
            confidence = f"{match.get('confidence', 0):.2%}"
            component_type = match.get("type", "?")
            reference = match.get("referenceName", "N/A")
        else:
            confidence = "N/A"
            component_type = "?"
            reference = "No match"
        
        print(f"{product:<50} {confidence:<12} {component_type:<6} {action:<30}")
    
    # Detailed example: Show full match result for first CPU
    print("\n" + "═" * 80)
    print("Detailed Match Example - First CPU Product:")
    print("═" * 80)
    
    cpu_match = client.match_product(shop_products[0])
    if cpu_match:
        print(json.dumps(cpu_match, indent=2))
    
    # Statistics
    print("\n" + "═" * 80)
    print("Summary Statistics:")
    print("─" * 80)
    
    high_confidence = sum(1 for r in results if r["match"] and r["match"]["confidence"] >= 0.75)
    med_confidence = sum(1 for r in results if r["match"] and 0.50 <= r["match"]["confidence"] < 0.75)
    low_confidence = sum(1 for r in results if r["match"] and r["match"]["confidence"] < 0.50)
    no_match = sum(1 for r in results if r["match"] is None)
    
    print(f"Total products processed:  {len(results)}")
    print(f"  ✅ High confidence (≥0.75): {high_confidence} - Ready for auto-accept")
    print(f"  ⚠️  Medium confidence (0.50-0.75): {med_confidence} - Needs review")
    print(f"  ❌ Low confidence (<0.50): {low_confidence} - Needs manual check")
    print(f"  ⊘  No match found: {no_match} - Use fallback normalization")


def integration_with_scraper_example():
    """Example: How to integrate with existing KaspiScraperService"""
    
    code_example = """
    // In KaspiScraperService.java
    
    @Autowired
    private ProductMatcherService matcherService;
    
    public void scrapeQuery() {
        // ... existing code to fetch products ...
        
        List<KaspiSearchProduct> results = searchProducts(query);
        
        for (KaspiSearchProduct product : results) {
            // NEW: Try to match against BuildCores references
            Optional<ProductMatcherService.MatchResult> match = 
                matcherService.matchProduct(product.title());
            
            if (match.isPresent()) {
                double confidence = match.get().confidence;
                
                if (confidence >= 0.95) {
                    // HIGH CONFIDENCE: Use reference specifications directly
                    saveWithReferenceSpecs(product, match.get());
                    
                } else if (confidence >= 0.75) {
                    // GOOD CONFIDENCE: Use reference with flag for review
                    saveWithMatchReference(product, match.get());
                    
                } else if (confidence >= 0.50) {
                    // MEDIUM CONFIDENCE: Save with match but flag as uncertain
                    saveWithReviewFlag(product, match.get());
                    
                } else {
                    // LOW CONFIDENCE: Fall back to existing normalization
                    saveIntoParsedTable(product);
                }
            } else {
                // NO MATCH: Use existing normalization as fallback
                saveIntoParsedTable(product);
            }
        }
    }
    
    private void saveWithReferenceSpecs(KaspiSearchProduct product, 
                                        MatchResult match) {
        // Use reference data (specs, manufacturer, socket, etc.)
        // This gives us COMPLETE, ACCURATE product data
    }
    """
    
    print("\nIntegration Code Example:")
    print("─" * 80)
    print(code_example)


if __name__ == "__main__":
    print("\nBefore running this example, ensure:")
    print("  1. partService is running on http://localhost:8080")
    print("  2. Database is initialized (migration V7 applied)")
    print()
    
    try:
        example_usage()
        print("\n" + "═" * 80)
        print("✅ Example completed successfully!")
        print("═" * 80)
        
        # Show integration code
        print("\nNext step: Integrate with scraper")
        integration_with_scraper_example()
        
    except requests.exceptions.ConnectionError:
        print("❌ Error: Cannot connect to http://localhost:8080")
        print("   Make sure partService is running!")
    except Exception as e:
        print(f"❌ Error: {e}")
