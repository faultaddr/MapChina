#!/usr/bin/env python3
"""
将文旅部官方5A景区数据合并到项目数据中

策略：
1. 用MCT官方5A数据替换高德5A数据（简介更权威、图片更好）
2. 保留高德4A数据不变
3. 从高德5A数据中交叉匹配补充 tel/open_time/rating/cost
4. MCT特有的 appointment_url 作为新字段加入
5. regionId通过地址中的行政区划信息+已有高德数据反向映射获得

用法:
  python3 merge_mct_5a.py
"""

import json
import re
import os
from difflib import SequenceMatcher

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.path.dirname(SCRIPT_DIR)

MCT_FILE = os.path.join(SCRIPT_DIR, "output", "mct_5a_attractions.json")
AMAP_FILE = os.path.join(PROJECT_DIR, "androidApp", "src", "main", "assets", "attractions.json")
ENRICHED_FILE = os.path.join(SCRIPT_DIR, "output", "attractions_enriched.json")
DETAIL_FILE = os.path.join(PROJECT_DIR, "androidApp", "src", "main", "assets", "attraction_details.json")


def normalize(name):
    n = re.sub(r"^.+?(?:省|市|自治区|壮族自治区|回族自治区|维吾尔自治区)", "", name, count=1)
    n = re.sub(r"^.{1,4}(?:市|州|盟|地区|区|县)", "", n)
    n = re.sub(r"(景区|旅游区|风景区|文化旅游区|文化旅游景区|旅游景区|旅游度假区|生态旅游区|游览区)$", "", n)
    n = n.replace("—", "-").replace("–", "-").replace("•", "·")
    n = n.replace("（", "(").replace("）", ")")
    return n.strip()


def extract_core(name):
    n = re.sub(r"^[一-鿿]{1,6}(?:市|州|盟|地区|区|县|省)", "", name)
    n = re.sub(r"(景区|旅游区|风景区|名胜区|文化旅游区|旅游景区|旅游度假区|生态旅游区|游览区)$", "", n)
    n = re.sub(r"[（(].*?[）)]", "", n)
    n = n.replace("—", "-").replace("–", "-").replace("•", "·")
    return n.strip()


def build_region_name_map(amap_data):
    name_to_id = {}
    for a in amap_data:
        rid = a["regionId"]
        desc = a.get("description", "")
        parts = desc.split(" ")
        if parts:
            location = parts[0]
            no_province = re.sub(r"^.+?(省|市|自治区|壮族自治区|回族自治区|维吾尔自治区)", "", location)
            no_city = re.sub(r"^.+?(市|州|盟|地区)", "", no_province)
            district = no_city.strip()
            if district and district not in name_to_id:
                name_to_id[district] = rid
    return name_to_id


def build_city_to_regionid_map(amap_data):
    city_map = {}
    for a in amap_data:
        rid = a["regionId"]
        desc = a.get("description", "")
        parts = desc.split(" ")
        if parts:
            location = parts[0]
            m = re.search(r"([一-鿿]+(?:市|州|盟|地区))", location)
            if m:
                city_name = m.group(1)
                if city_name not in city_map:
                    city_map[city_name] = rid[:4] + "00"
    return city_map


def resolve_region_id(mct_item, region_name_map, city_map):
    city_name = mct_item.get("city_name", "")
    address = mct_item.get("address", "")
    province_code = mct_item.get("province_code", "")

    if city_name in region_name_map:
        return region_name_map[city_name]

    m = re.search(r"([一-鿿]+(?:区|县|市|旗))", address)
    if m:
        addr_district = m.group(1)
        if addr_district in region_name_map:
            return region_name_map[addr_district]

    if city_name in city_map:
        return city_map[city_name]

    m = re.search(r"([一-鿿]+(?:市|州|盟|地区))", address)
    if m:
        addr_city = m.group(1)
        if addr_city in city_map:
            return city_map[addr_city]

    if province_code:
        return province_code

    return "000000"


def match_mct_to_amap(mct_item, amap_a5):
    """用综合相似度匹配MCT景区到高德景区，返回最佳匹配"""
    m_norm = normalize(mct_item["name"])
    m_core = extract_core(mct_item["name"])

    same_province = [a for a in amap_a5 if a["regionId"][:2] == mct_item["province_code"][:2]]
    candidates = same_province if same_province else amap_a5

    best_match = None
    best_score = 0

    for a in candidates:
        a_norm = normalize(a["name"])
        a_core = extract_core(a["name"])

        s1 = SequenceMatcher(None, m_norm, a_norm).ratio()
        s2 = SequenceMatcher(None, m_core, a_core).ratio()
        s3 = 0.6 if (m_core and a_core and (m_core in a_core or a_core in m_core)) else 0

        score = max(s1, s2, s3)
        if score > best_score:
            best_score = score
            best_match = a

    return best_match, best_score


def main():
    with open(MCT_FILE, encoding="utf-8") as f:
        mct_data = json.load(f)
    with open(AMAP_FILE, encoding="utf-8") as f:
        amap_data = json.load(f)
    with open(DETAIL_FILE, encoding="utf-8") as f:
        detail_data = json.load(f)

    # 加载高德enriched数据（有tel/open_time等），这是原始高德数据
    enriched_map = {}
    amap_a5_original = []
    if os.path.exists(ENRICHED_FILE):
        with open(ENRICHED_FILE, encoding="utf-8") as f:
            enriched_data = json.load(f)
        enriched_map = {a["id"]: a for a in enriched_data}
        # 从enriched取原始高德5A做匹配，统一regionId字段名
        for a in enriched_data:
            if a.get("level") == "A5":
                a["regionId"] = a.get("region_id", a.get("regionId", ""))
                amap_a5_original.append(a)
    else:
        print(f"警告: 未找到高德enriched数据 ({ENRICHED_FILE})，将无法补充tel/open_time")
        amap_a5_original = [a for a in amap_data if a["level"] == "A5"]

    # 建立高德id -> detail映射
    detail_map = {d["id"]: d for d in detail_data}

    # 构建regionId映射
    region_name_map = build_region_name_map(amap_data)
    city_map = build_city_to_regionid_map(amap_data)

    print(f"MCT 5A景区: {len(mct_data)}")
    amap_a5_count = len([a for a in amap_data if a["level"] == "A5"])
    amap_a4_count = len(amap_data) - amap_a5_count
    print(f"高德景区: {len(amap_data)} (5A={amap_a5_count}, 4A={amap_a4_count})")
    print(f"原始高德5A(匹配源): {len(amap_a5_original)}")

    # 转换MCT数据
    new_attractions = []
    new_details = []

    tel_from_amap = 0
    ot_from_amap = 0
    rating_from_amap = 0
    appointment_from_mct = 0

    for m in mct_data:
        region_id = resolve_region_id(m, region_name_map, city_map)
        attraction_id = f"mct_{m['mct_id']}"

        # 匹配高德5A，补充tel/open_time/rating
        amap_match, match_score = match_mct_to_amap(m, amap_a5_original)

        tel = None
        open_time = None
        rating = None
        cost = None

        if amap_match and match_score >= 0.35:
            # 先从enriched数据取
            amap_enriched = enriched_map.get(amap_match["id"])
            if amap_enriched:
                if amap_enriched.get("tel"):
                    tel = amap_enriched["tel"]
                    tel_from_amap += 1
                if amap_enriched.get("open_time"):
                    open_time = amap_enriched["open_time"]
                    ot_from_amap += 1
                if amap_enriched.get("rating"):
                    rating = amap_enriched["rating"]
                    rating_from_amap += 1
                if amap_enriched.get("cost"):
                    cost = amap_enriched["cost"]
            else:
                # 从detail文件取
                amap_detail = detail_map.get(amap_match["id"])
                if amap_detail:
                    if amap_detail.get("t"):
                        tel = amap_detail["t"]
                        tel_from_amap += 1
                    if amap_detail.get("ot"):
                        open_time = amap_detail["ot"]
                        ot_from_amap += 1
                    if amap_detail.get("r"):
                        rating = amap_detail["r"]
                        rating_from_amap += 1
                    if amap_detail.get("c"):
                        cost = amap_detail["c"]

        # MCT特有数据
        appointment_url = m.get("appointment_url")
        if appointment_url:
            appointment_from_mct += 1

        new_attractions.append({
            "id": attraction_id,
            "name": m["name"],
            "regionId": region_id[:6] if len(region_id) >= 6 else region_id,
            "level": "A5",
            "latitude": m["latitude"],
            "longitude": m["longitude"],
            "description": m["introduce"] or m["address"] or "",
        })

        images = [url.replace("http://", "https://") for url in m.get("image_urls", [])]
        new_details.append({
            "id": attraction_id,
            "iu": images,
            "r": rating,
            "c": cost,
            "ot": open_time,
            "t": tel,
            "w": m.get("website") or None,
            "au": appointment_url or None,
        })

    # 合并: 移除高德5A, 保留4A, 加入MCT 5A
    amap_a4 = [a for a in amap_data if a["level"] == "A4"]
    detail_a4_ids = {a["id"] for a in amap_a4}
    detail_a4 = [d for d in detail_data if d["id"] in detail_a4_ids]

    # 4A详情也需要加au字段(都为null)
    for d in detail_a4:
        if "au" not in d:
            d["au"] = None

    merged_attractions = amap_a4 + new_attractions
    merged_details = detail_a4 + new_details

    merged_attractions.sort(key=lambda a: (a["regionId"], a["level"], a["name"]))
    merged_details.sort(key=lambda d: d["id"])

    print(f"\n交叉匹配结果 (高德 -> MCT 5A):")
    print(f"  电话: {tel_from_amap}/{len(mct_data)}")
    print(f"  营业时间: {ot_from_amap}/{len(mct_data)}")
    print(f"  评分: {rating_from_amap}/{len(mct_data)}")
    print(f"  预约链接(MCT): {appointment_from_mct}/{len(mct_data)}")

    a5_count = sum(1 for a in merged_attractions if a["level"] == "A5")
    a4_count = sum(1 for a in merged_attractions if a["level"] == "A4")
    print(f"\n合并结果:")
    print(f"  总计: {len(merged_attractions)}")
    print(f"  5A: {a5_count} (MCT官方)")
    print(f"  4A: {a4_count} (高德)")

    with open(AMAP_FILE, "w", encoding="utf-8") as f:
        json.dump(merged_attractions, f, ensure_ascii=False, indent=2)
    print(f"\n已写入: {AMAP_FILE}")

    with open(DETAIL_FILE, "w", encoding="utf-8") as f:
        json.dump(merged_details, f, ensure_ascii=False, separators=(",", ":"))
    print(f"已写入: {DETAIL_FILE}")


if __name__ == "__main__":
    main()
