#!/usr/bin/env python3
"""Process seed_data.kt to generate attractions.json with proper regionId mapping."""

import re
import json

# Extract city name -> region code mapping from DataSeeder.kt
# Format: Region("XXXXXX", "城市名", RegionLevel.CITY, "YY0000")
def extract_city_mapping():
    mapping = {}  # city_name -> region_code

    # Province code -> province name mapping (for prefix matching)
    provinces = {
        "110000": "北京市", "120000": "天津市", "130000": "河北省", "140000": "山西省",
        "150000": "内蒙古自治区", "210000": "辽宁省", "220000": "吉林省", "230000": "黑龙江省",
        "310000": "上海市", "320000": "江苏省", "330000": "浙江省", "340000": "安徽省",
        "350000": "福建省", "360000": "江西省", "370000": "山东省", "410000": "河南省",
        "420000": "湖北省", "430000": "湖南省", "440000": "广东省", "450000": "广西壮族自治区",
        "460000": "海南省", "500000": "重庆市", "510000": "四川省", "520000": "贵州省",
        "530000": "云南省", "540000": "西藏自治区", "610000": "陕西省", "620000": "甘肃省",
        "630000": "青海省", "640000": "宁夏回族自治区", "650000": "新疆维吾尔自治区",
        "710000": "台湾省", "810000": "香港特别行政区", "820000": "澳门特别行政区"
    }

    with open("shared/src/commonMain/kotlin/com/mapchina/data/remote/DataSeeder.kt", "r") as f:
        content = f.read()

    # Match city-level Region entries
    pattern = r'Region\("(\d{6})",\s*"([^"]+)",\s*RegionLevel\.CITY'
    for match in re.finditer(pattern, content):
        code = match.group(1)
        name = match.group(2)
        mapping[name] = code

    return mapping, provinces

# Parse attractions from seed_data.kt
def parse_seed_attractions():
    attractions = []

    with open("scripts/output/seed_data.kt", "r") as f:
        content = f.read()

    # Match Attraction("id", "name", "", AttractionLevel.AX, lat, lng, "address")
    pattern = r'Attraction\("([^"]+)",\s*"([^"]+)",\s*"",\s*AttractionLevel\.(A[45]),\s*([\d.]+),\s*([\d.]+),\s*"([^"]*)"\)'
    for match in re.finditer(pattern, content):
        attractions.append({
            "id": match.group(1),
            "name": match.group(2),
            "level": match.group(3),
            "latitude": float(match.group(4)),
            "longitude": float(match.group(5)),
            "address": match.group(6)
        })

    return attractions

# Province name alternatives for matching
province_names = {
    "北京": "110000", "天津": "120000", "河北": "130000", "山西": "140000",
    "内蒙古": "150000", "辽宁": "210000", "吉林": "220000", "黑龙江": "230000",
    "上海": "310000", "江苏": "320000", "浙江": "330000", "安徽": "340000",
    "福建": "350000", "江西": "360000", "山东": "370000", "河南": "410000",
    "湖北": "420000", "湖南": "430000", "广东": "440000", "广西": "450000",
    "海南": "460000", "重庆": "500000", "四川": "510000", "贵州": "520000",
    "云南": "530000", "西藏": "540000", "陕西": "610000", "甘肃": "620000",
    "青海": "630000", "宁夏": "640000", "新疆": "650000", "台湾": "710000",
    "香港": "810000", "澳门": "820000"
}

def find_region_id(address, city_mapping):
    """Find the city-level region ID from an address string."""
    # Try to match city names in the address, longest match first
    matched_cities = []
    for city_name, city_code in city_mapping.items():
        if city_name in address:
            matched_cities.append((len(city_name), city_name, city_code))

    if matched_cities:
        # Sort by length descending - prefer longer (more specific) matches
        matched_cities.sort(key=lambda x: -x[0])
        return matched_cities[0][2]

    # Fallback: try province prefix
    for prov_name, prov_code in province_names.items():
        if prov_name in address:
            # Return province code as fallback
            return prov_code

    return ""

def main():
    city_mapping, provinces = extract_city_mapping()
    print(f"Loaded {len(city_mapping)} city mappings")

    attractions = parse_seed_attractions()
    print(f"Loaded {len(attractions)} attractions")

    result = []
    unmatched = 0
    for attr in attractions:
        region_id = find_region_id(attr["address"], city_mapping)
        if not region_id:
            unmatched += 1
            print(f"  Unmatched: {attr['name']} - {attr['address']}")

        result.append({
            "id": attr["id"],
            "name": attr["name"],
            "regionId": region_id,
            "level": attr["level"],
            "latitude": attr["latitude"],
            "longitude": attr["longitude"],
            "description": attr["address"]
        })

    print(f"\nMatched: {len(result) - unmatched}/{len(result)}")
    print(f"Unmatched: {unmatched}")

    with open("androidApp/src/main/assets/attractions.json", "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    print(f"Written to androidApp/src/main/assets/attractions.json")

if __name__ == "__main__":
    main()
