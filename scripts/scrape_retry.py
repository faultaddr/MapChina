#!/usr/bin/env python3
"""补漏脚本：重新爬取因 API 限流失败的城市，然后合并到主数据"""

import json
import os
import sys
import time
import urllib.parse
import urllib.request

# 因 CUQPS_HAS_EXCEEDED_THE_LIMIT 失败的城市
FAILED_CITIES = [
    ("131100", "衡水"),
    ("210900", "阜新"),
    ("211300", "朝阳"),
    ("220400", "辽源"),
    ("220500", "通化"),
    ("220700", "松原"),
    ("220800", "白城"),
    ("230400", "鹤岗"),
    ("231200", "绥化"),
    ("420700", "鄂州"),
    ("420800", "荆门"),
    ("421300", "随州"),
    ("450700", "钦州"),
    ("450800", "贵港"),
    ("460400", "儋州"),
    ("510400", "攀枝花"),
    ("530900", "临沧"),
    ("532600", "文山"),
    ("540300", "昌都"),
    ("540500", "山南"),
    ("542500", "阿里"),
    ("610200", "铜川"),
    ("610600", "延安"),
    ("621000", "庆阳"),
    ("630200", "海东"),
    ("632500", "海南州"),
    ("632600", "果洛"),
    ("632700", "玉树"),
    ("632800", "海西"),
    ("640200", "石嘴山"),
    ("640400", "固原"),
    ("640500", "中卫"),
    ("650500", "哈密"),
    ("652800", "巴音郭楞"),
    ("653200", "和田"),
    ("654200", "塔城"),
]

POI_TYPES = "110100|110101|110102|110103|110104|110105|110200|110201|110202|110203|110204|110205|110206|110207|110208|110209|110210|110211|110212|110300|110400"
API_BASE = "https://restapi.amap.com/v3/place/text"
OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "output")


def get_level_from_poi(poi):
    biz_ext = poi.get("biz_ext", {})
    if isinstance(biz_ext, dict):
        level_str = biz_ext.get("level", "")
        if isinstance(level_str, str):
            if "AAAAA" in level_str:
                return "A5"
            if "AAAA" in level_str:
                return "A4"
    keytag = poi.get("keytag", "")
    if isinstance(keytag, str):
        if "5A" in keytag:
            return "A5"
        if "4A" in keytag:
            return "A4"
    return None


def fetch_pois(api_key, city_code, page=1, page_size=25, retries=3):
    params = {
        "key": api_key,
        "keywords": "",
        "types": POI_TYPES,
        "city": city_code,
        "citylimit": "true",
        "offset": page_size,
        "page": page,
        "output": "json",
    }
    url = f"{API_BASE}?{urllib.parse.urlencode(params)}"
    req = urllib.request.Request(url)
    req.add_header("User-Agent", "Mozilla/5.0 MapChinaScraper/2.0")

    for attempt in range(retries):
        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                if data.get("status") == "1":
                    return data
                if "CUQPS" in data.get("info", ""):
                    wait = 2 ** (attempt + 1)
                    print(f"    限流，等待{wait}s后重试...", end="", flush=True)
                    time.sleep(wait)
                    continue
                return data
        except Exception as e:
            print(f"    请求异常: {e}，重试...", flush=True)
            time.sleep(2)
    return None


def poi_to_attraction(poi, level_key):
    name = poi.get("name", "").strip()
    if not name:
        return None
    location = poi.get("location", "")
    if not location or "," not in location:
        return None
    parts = location.split(",")
    longitude = float(parts[0])
    latitude = float(parts[1])
    pname = poi.get("pname", "")
    cityname = poi.get("cityname", "")
    adname = poi.get("adname", "")
    adcode = poi.get("adcode", "")
    poi_id = poi.get("id", "")
    import re
    safe_id = re.sub(r"[^a-zA-Z0-9]", "", poi_id)
    attraction_id = f"attr_{safe_id}"
    address = poi.get("address", "")
    description = f"{pname}{cityname}{adname}"
    if address:
        description += f" {address}"
    return {
        "id": attraction_id,
        "name": name,
        "region_id": adcode,
        "level": level_key,
        "latitude": round(latitude, 6),
        "longitude": round(longitude, 6),
        "province": pname,
        "city": cityname,
        "district": adname,
        "description": description.strip(),
    }


def fetch_all_pois_for_city(api_key, city_adcode, city_name):
    all_pois = []
    page = 1
    while True:
        data = fetch_pois(api_key, city_adcode, page=page)
        if data is None:
            print(f"  [{city_name}] 第{page}页最终失败", file=sys.stderr)
            break
        pois = data.get("pois", [])
        if not pois:
            break
        all_pois.extend(pois)
        count = int(data.get("count", "0"))
        if page * 25 >= count or page >= 40:
            break
        page += 1
        time.sleep(0.15)
    return all_pois


def generate_kotlin_seed(attractions):
    lines = ["val attractions = listOf("]
    for a in attractions:
        desc = a["description"].replace('"', '\\"') if a["description"] else ""
        lines.append(
            f'    Attraction("{a["id"]}", "{a["name"]}", "{a["region_id"]}", '
            f'AttractionLevel.{a["level"]}, {a["latitude"]}, {a["longitude"]}, "{desc}"),'
        )
    lines.append(")")
    lines.append("")
    lines.append("attractions.forEach { attractionRepo.insertAttraction(it) }")
    return "\n".join(lines)


def main():
    api_key = sys.argv[1] if len(sys.argv) > 1 else None
    if not api_key:
        print("用法: python3 scrape_retry.py YOUR_AMAP_API_KEY")
        sys.exit(1)

    # 加载已有数据
    json_path = os.path.join(OUTPUT_DIR, "attractions.json")
    with open(json_path, encoding="utf-8") as f:
        existing = json.load(f)
    existing_keys = {(a["name"], round(a["latitude"], 2), round(a["longitude"], 2)) for a in existing}
    print(f"已有 {len(existing)} 条记录，开始补漏...")

    new_attractions = []
    total_a5 = 0
    total_a4 = 0

    for city_adcode, city_name in FAILED_CITIES:
        # 城市间加长间隔，避免限流
        time.sleep(1)

        print(f"[{city_name}] 搜索中...", end="", flush=True)
        pois = fetch_all_pois_for_city(api_key, city_adcode, city_name)

        a5 = a4 = 0
        for poi in pois:
            level = get_level_from_poi(poi)
            if level is None:
                continue
            attraction = poi_to_attraction(poi, level)
            if attraction is None:
                continue
            dedup_key = (attraction["name"], round(attraction["latitude"], 2), round(attraction["longitude"], 2))
            if dedup_key in existing_keys:
                continue
            existing_keys.add(dedup_key)
            new_attractions.append(attraction)
            if level == "A5":
                a5 += 1
            else:
                a4 += 1

        total_a5 += a5
        total_a4 += a4
        print(f" 新增 5A={a5} 4A={a4} (补漏累计 5A={total_a5} 4A={total_a4})")

    if not new_attractions:
        print("\n无新增数据")
        return

    # 合并
    merged = existing + new_attractions
    merged.sort(key=lambda a: (a["province"], a["level"], a["name"]))

    a5_total = sum(1 for a in merged if a["level"] == "A5")
    a4_total = sum(1 for a in merged if a["level"] == "A4")
    print(f"\n===== 补漏完成 =====")
    print(f"新增: 5A={total_a5} 4A={total_a4}")
    print(f"合并后: 5A={a5_total} 4A={a4_total} 总计={len(merged)}")

    # 重新写入所有文件
    import csv

    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(merged, f, ensure_ascii=False, indent=2)
    print(f"已更新 JSON: {json_path}")

    csv_path = os.path.join(OUTPUT_DIR, "attractions.csv")
    with open(csv_path, "w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=[
            "id", "name", "level", "latitude", "longitude",
            "province", "city", "district", "region_id", "description",
        ])
        writer.writeheader()
        writer.writerows(merged)
    print(f"已更新 CSV: {csv_path}")

    kt_path = os.path.join(OUTPUT_DIR, "seed_data.kt")
    with open(kt_path, "w", encoding="utf-8") as f:
        f.write(generate_kotlin_seed(merged))
    print(f"已更新 Kotlin: {kt_path}")


if __name__ == "__main__":
    main()
