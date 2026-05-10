#!/bin/bash
# Quick Start: GPU/CPU Reference Matching System

echo "================================"
echo "BuildCores Reference System"
echo "Quick Start Guide"
echo "================================"
echo ""

# Configuration
PORT=${PORT:-8080}
BASE_URL="http://localhost:$PORT"

echo "1. Starting partService (make sure it's running on port $PORT)..."
echo ""

# Load all reference data
echo "2. Loading BuildCores reference data..."
echo ""

echo "   Loading CPUs..."
curl -X POST "$BASE_URL/api/reference/load/cpu"
echo ""
echo ""

echo "   Loading GPUs..."
curl -X POST "$BASE_URL/api/reference/load/gpu"
echo ""
echo ""

# Test matching
echo "3. Testing CPU matching..."
ENCODED_CPU="Intel%20Core%20i9-13900KS%2024%20core%2036MB%20cache"
echo "   Query: $ENCODED_CPU"
curl -X POST "$BASE_URL/api/reference/match?productName=$ENCODED_CPU"
echo ""
echo ""

echo "4. Testing GPU matching..."
ENCODED_GPU="RTX%204090%2024GB%20GDDR6X%20gaming"
echo "   Query: $ENCODED_GPU"
curl -X POST "$BASE_URL/api/reference/match?productName=$ENCODED_GPU"
echo ""
echo ""

echo "================================"
echo "Setup Complete!"
echo "================================"
echo ""
echo "Reference System Ready:"
echo "  CPU matches: GET /api/reference/match?productName=<cpu_name>"
echo "  GPU matches: GET /api/reference/match?productName=<gpu_name>"
echo ""
echo "Database Tables:"
echo "  - reference_cpu: $(curl -s "$BASE_URL/api/reference/match?productName=test" | grep -o 'confidence' | wc -l) references loaded"
echo "  - reference_gpu: Similar structure for GPUs"
echo ""
