#!/usr/bin/env python3
"""
爬取文化和旅游部官网 (lyfw.mct.gov.cn) 所有 5A 景区数据

数据来源：文化和旅游部大众旅游服务栏目
  - API: https://lyfw.mct.gov.cn/api/marker/list
  - 图片: https://lyfw.mct.gov.cn/_static/{picture_path}

输出:
  - mct_5a_attractions.json: 完整景区数据（含名称、经纬度、地址、简介、图片等）
  - mct_5a_summary.csv:    CSV 摘要，便于查看

用法:
  python3 scrape_mct_5a.py [--output-dir ./output] [--delay 0.5]
"""

import argparse
import csv
import json
import os
import sys
import time
import urllib.parse
import urllib.request
from typing import Optional


API_BASE = "https://lyfw.mct.gov.cn/api/marker/list"
IMG_BASE = "https://lyfw.mct.gov.cn/_static"
TYPE_ID_5A = 1
PAGE_SIZE = 20


def fetch_page(
    type_id: int = TYPE_ID_5A,
    page: int = 1,
    province_id: Optional[int] = None,
    retries: int = 3,
) -> Optional[dict]:
    """调用文旅部 API 获取景区列表"""
    params = {
        "type_id": type_id,
        "page": page,
        "pageSize": PAGE_SIZE,
    }
    if province_id is not None:
        params["province_id"] = province_id

    url = f"{API_BASE}?{urllib.parse.urlencode(params)}"

    for attempt in range(retries):
        try:
            req = urllib.request.Request(url)
            req.add_header("User-Agent", "Mozilla/5.0 MapChinaMCTScraper/1.0")
            req.add_header("Referer", "https://lyfw.mct.gov.cn/site/special/scenic")
            with urllib.request.urlopen(req, timeout=20) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                if data.get("code") == 20000:
                    return data.get("data", {})
                print(f"  API error: code={data.get('code')}, msg={data.get('message')}", file=sys.stderr)
                return None
        except Exception as e:
            print(f"  请求异常 (attempt {attempt+1}/{retries}): {e}", file=sys.stderr)
            time.sleep(2)

    return None


def fetch_all_5a() -> list[dict]:
    """分页获取所有 5A 景区数据"""
    # 先获取总数
    first = fetch_page(page=1)
    if first is None:
        print("无法获取数据，请检查网络连接", file=sys.stderr)
        sys.exit(1)

    total = first.get("total", 0)
    total_pages = (total + PAGE_SIZE - 1) // PAGE_SIZE
    print(f"全国 5A 景区总数: {total}，共 {total_pages} 页")

    all_items = list(first.get("list", []))
    print(f"  第 1/{total_pages} 页完成 ({len(all_items)}/{total})")

    for page in range(2, total_pages + 1):
        time.sleep(args.delay)
        data = fetch_page(page=page)
        if data is None:
            print(f"  第 {page} 页获取失败，跳过", file=sys.stderr)
            continue

        items = data.get("list", [])
        all_items.extend(items)
        print(f"  第 {page}/{total_pages} 页完成 ({len(all_items)}/{total})")

    return all_items


def normalize_attraction(item: dict) -> dict:
    """将 API 返回的景区数据标准化"""
    # 构建图片完整URL
    pictures = item.get("picture", [])
    image_urls = []
    if isinstance(pictures, list):
        for pic in pictures:
            if isinstance(pic, str) and pic:
                image_urls.append(f"{IMG_BASE}/{pic}")
            elif isinstance(pic, dict) and pic.get("src"):
                image_urls.append(f"{IMG_BASE}/{pic['src']}")

    # 省份代码转字符串
    province_code = item.get("province", 0)
    city_code = item.get("city", 0)

    return {
        "id": f"mct_{item.get('id', '')}",
        "mct_id": item.get("id"),
        "name": item.get("name", "").strip(),
        "level": "A5",
        "latitude": float(item.get("latitude", 0)) if item.get("latitude") else None,
        "longitude": float(item.get("longitude", 0)) if item.get("longitude") else None,
        "address": item.get("address", "").strip(),
        "introduce": item.get("introduce", "").strip(),
        "province_code": str(province_code) if province_code else "",
        "province_name": item.get("province_name", ""),
        "city_code": str(city_code) if city_code else "",
        "city_name": item.get("city_name", ""),
        "image_urls": image_urls,
        "website": item.get("web_link_url", ""),
        "appointment_url": item.get("appointment_link_url", ""),
    }


def main():
    parser = argparse.ArgumentParser(description="爬取文旅部官网所有 5A 景区数据")
    parser.add_argument("--output-dir", default="./output", help="输出目录")
    parser.add_argument("--delay", type=float, default=0.5, help="每页请求间隔(秒)")
    global args
    args = parser.parse_args()

    os.makedirs(args.output_dir, exist_ok=True)

    # 获取数据
    raw_items = fetch_all_5a()
    print(f"\n获取到 {len(raw_items)} 条原始数据")

    # 标准化
    attractions = []
    for item in raw_items:
        norm = normalize_attraction(item)
        if norm["name"]:
            attractions.append(norm)

    # 按省份排序
    attractions.sort(key=lambda a: (a["province_name"], a["city_name"], a["name"]))

    # 统计
    provinces = {}
    for a in attractions:
        p = a["province_name"]
        provinces[p] = provinces.get(p, 0) + 1

    print(f"\n===== 全国 5A 景区统计 =====")
    print(f"总计: {len(attractions)} 个")
    print(f"覆盖省份/直辖市: {len(provinces)} 个")
    for p in sorted(provinces.keys()):
        print(f"  {p}: {provinces[p]} 个")

    with_intro = sum(1 for a in attractions if a["introduce"])
    with_image = sum(1 for a in attractions if a["image_urls"])
    with_address = sum(1 for a in attractions if a["address"])
    with_website = sum(1 for a in attractions if a["website"])
    print(f"\n数据完整度:")
    print(f"  有简介: {with_intro}/{len(attractions)}")
    print(f"  有图片: {with_image}/{len(attractions)}")
    print(f"  有地址: {with_address}/{len(attractions)}")
    print(f"  有官网: {with_website}/{len(attractions)}")

    # 保存 JSON
    json_path = os.path.join(args.output_dir, "mct_5a_attractions.json")
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(attractions, f, ensure_ascii=False, indent=2)
    print(f"\n已保存 JSON: {json_path}")

    # 保存 CSV
    csv_path = os.path.join(args.output_dir, "mct_5a_summary.csv")
    csv_fields = [
        "id", "mct_id", "name", "level", "latitude", "longitude",
        "province_name", "province_code", "city_name", "city_code",
        "address", "introduce", "website", "appointment_url",
        "image_urls",
    ]
    with open(csv_path, "w", encoding="utf-8-sig", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=csv_fields, extrasaction="ignore")
        writer.writeheader()
        for a in attractions:
            row = {**a}
            row["image_urls"] = " | ".join(row["image_urls"])
            row["introduce"] = row["introduce"][:200] + "..." if len(row["introduce"]) > 200 else row["introduce"]
            writer.writerow(row)
    print(f"已保存 CSV: {csv_path}")


if __name__ == "__main__":
    main()
