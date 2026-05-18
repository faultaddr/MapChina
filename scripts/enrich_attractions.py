#!/usr/bin/env python3
"""
为景点数据补充图文信息（图片、详细描述、评分等）

使用高德 POI 详情接口，通过已有的 POI ID 获取：
  - photos: 景点图片 URL 列表
  - biz_ext: 评分、价格、营业时间等
  - detail_info: 详细描述

输出:
  - attractions_enriched.json: 增强后的完整数据
  - attraction_images.json: 仅图片映射（id -> [urls]），便于 App 直接使用

用法:
  python3 enrich_attractions.py --key YOUR_AMAP_API_KEY [--input ./output/attractions.json] [--batch-size 10] [--delay 0.15]
"""

import argparse
import json
import os
import sys
import time
import urllib.parse
import urllib.request
from typing import Optional


API_BASE = "https://restapi.amap.com/v3/place/detail"


def fetch_poi_details(
    api_key: str,
    poi_ids: list[str],
    extensions: str = "all",
    retries: int = 3,
) -> Optional[list[dict]]:
    """调用高德 POI 详情接口，批量获取景点详情（最多10个）"""
    params = {
        "key": api_key,
        "id": ",".join(poi_ids),
        "extensions": extensions,
        "output": "json",
    }
    url = f"{API_BASE}?{urllib.parse.urlencode(params)}"

    for attempt in range(retries):
        try:
            req = urllib.request.Request(url)
            req.add_header("User-Agent", "Mozilla/5.0 MapChinaEnricher/1.0")
            with urllib.request.urlopen(req, timeout=20) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                if data.get("status") == "1":
                    return data.get("pois", [])
                if "CUQPS" in data.get("info", ""):
                    wait = 2 ** (attempt + 1)
                    print(f"  限流，等待{wait}s...", end="", flush=True)
                    time.sleep(wait)
                    continue
                print(f"  API错误: {data.get('info')} ({data.get('infocode')})", file=sys.stderr)
                return None
        except Exception as e:
            print(f"  请求异常: {e}，重试 {attempt+1}/{retries}", file=sys.stderr)
            time.sleep(2)

    return None


def extract_poi_id(attraction_id: str) -> str:
    """从 'attr_B00154DUI6' 提取高德原始 POI ID 'B00154DUI6'"""
    return attraction_id.replace("attr_", "", 1)


def extract_enrichment(poi: dict) -> dict:
    """从 POI 详情中提取图文信息"""
    result = {
        "image_urls": [],
        "rating": None,
        "cost": None,
        "open_time": "",
        "tel": "",
        "website": "",
        "tag": "",
    }

    # 提取图片
    photos = poi.get("photos", [])
    if isinstance(photos, list):
        for photo in photos:
            if isinstance(photo, dict):
                url = photo.get("url", "")
                if url:
                    result["image_urls"].append(url)
            elif isinstance(photo, str) and photo:
                result["image_urls"].append(photo)

    # 提取 biz_ext
    biz_ext = poi.get("biz_ext", {})
    if isinstance(biz_ext, dict):
        rating = biz_ext.get("rating", "")
        if rating and str(rating) not in ("", "-1", "[]"):
            result["rating"] = str(rating)

        cost = biz_ext.get("cost", "")
        if cost and str(cost) not in ("", "-1", "[]"):
            result["cost"] = str(cost)

        open_time = biz_ext.get("open_time", "")
        if open_time and str(open_time) not in ("", "[]"):
            result["open_time"] = str(open_time)

    # 提取电话
    tel = poi.get("tel", "")
    if tel and str(tel) not in ("", "[]"):
        result["tel"] = str(tel)

    # 提取官网
    website = poi.get("website", "")
    if website and str(website) not in ("", "[]"):
        result["website"] = str(website)

    # 提取分类标签
    tag = poi.get("type", "")
    if tag and str(tag) not in ("", "[]"):
        result["tag"] = str(tag)

    return result


def main():
    parser = argparse.ArgumentParser(description="为景点补充图文信息")
    parser.add_argument("--key", required=True, help="高德地图 Web 服务 API Key")
    parser.add_argument("--input", default="./output/attractions.json", help="输入 JSON 文件路径")
    parser.add_argument("--batch-size", type=int, default=10, help="每次批量查询的 POI 数量（1-10）")
    parser.add_argument("--delay", type=float, default=0.15, help="每批次请求间隔（秒）")
    parser.add_argument("--max-count", type=int, default=0, help="最多处理多少条（0=全部）")
    parser.add_argument("--resume-from", type=int, default=0, help="从第 N 条继续（0-based）")
    args = parser.parse_args()

    script_dir = os.path.dirname(os.path.abspath(__file__))
    input_path = args.input if os.path.isabs(args.input) else os.path.join(script_dir, args.input)

    with open(input_path, encoding="utf-8") as f:
        attractions = json.load(f)

    print(f"加载 {len(attractions)} 条景点数据")

    if args.max_count > 0:
        attractions = attractions[:args.max_count]
        print(f"限制处理前 {args.max_count} 条")

    # 尝试加载已有的增强数据（用于断点续传）
    enriched_path = os.path.join(os.path.dirname(input_path), "attractions_enriched.json")
    existing_enrichment = {}
    if os.path.exists(enriched_path):
        with open(enriched_path, encoding="utf-8") as f:
            prev_data = json.load(f)
            for item in prev_data:
                existing_enrichment[item["id"]] = item
        print(f"加载已有增强数据 {len(existing_enrichment)} 条")

    enriched = []
    success_count = 0
    fail_count = 0
    skip_count = 0
    photo_count = 0

    start_idx = args.resume_from
    batch_poi_ids = []
    batch_attr_indices = []

    for i in range(start_idx, len(attractions)):
        attr = attractions[i]
        attr_id = attr["id"]

        # 跳过已处理的
        if attr_id in existing_enrichment:
            enriched.append(existing_enrichment[attr_id])
            skip_count += 1
            continue

        poi_id = extract_poi_id(attr_id)
        batch_poi_ids.append(poi_id)
        batch_attr_indices.append(i)

        if len(batch_poi_ids) >= args.batch_size or i == len(attractions) - 1:
            # 发起批量请求
            pois = fetch_poi_details(args.key, batch_poi_ids)

            if pois is None:
                # 批量失败，逐个处理
                print(f"\n  批量失败，逐个重试...", flush=True)
                for j, idx in enumerate(batch_attr_indices):
                    single_poi = fetch_poi_details(args.key, [batch_poi_ids[j]])
                    enrichment = extract_enrichment(single_poi[0]) if single_poi else {}
                    merged = {**attractions[idx], **enrichment}
                    enriched.append(merged)
                    if single_poi:
                        success_count += 1
                        photo_count += len(enrichment.get("image_urls", []))
                    else:
                        fail_count += 1
                    time.sleep(args.delay)
            else:
                # 构建 poi_id -> poi 的映射
                poi_map = {}
                for poi in pois:
                    raw_id = poi.get("id", "")
                    poi_map[raw_id] = poi

                for j, idx in enumerate(batch_attr_indices):
                    poi_id = batch_poi_ids[j]
                    poi = poi_map.get(poi_id)
                    enrichment = extract_enrichment(poi) if poi else {}
                    merged = {**attractions[idx], **enrichment}
                    enriched.append(merged)
                    if poi:
                        success_count += 1
                        photo_count += len(enrichment.get("image_urls", []))
                    else:
                        fail_count += 1

            processed = i + 1 - start_idx
            total = len(attractions) - start_idx
            print(f"\r  进度: {processed}/{total} | 成功={success_count} 失败={fail_count} 跳过={skip_count} 图片={photo_count}", end="", flush=True)

            batch_poi_ids = []
            batch_attr_indices = []
            time.sleep(args.delay)

    print()

    # 统计
    with_images = sum(1 for a in enriched if a.get("image_urls"))
    with_rating = sum(1 for a in enriched if a.get("rating"))
    print(f"\n===== 完成 =====")
    print(f"总计: {len(enriched)} 条")
    print(f"有图片: {with_images} 条")
    print(f"有评分: {with_rating} 条")
    print(f"图片总数: {photo_count}")

    # 保存完整增强数据
    with open(enriched_path, "w", encoding="utf-8") as f:
        json.dump(enriched, f, ensure_ascii=False, indent=2)
    print(f"\n已保存: {enriched_path}")

    # 保存精简的图片映射
    image_map = {}
    for a in enriched:
        urls = a.get("image_urls", [])
        if urls:
            image_map[a["id"]] = urls

    image_map_path = os.path.join(os.path.dirname(input_path), "attraction_images.json")
    with open(image_map_path, "w", encoding="utf-8") as f:
        json.dump(image_map, f, ensure_ascii=False, indent=2)
    print(f"已保存图片映射: {image_map_path} ({len(image_map)} 条有图)")


if __name__ == "__main__":
    main()
