#!/usr/bin/env python3
"""从 attractions_enriched.json 生成精简的 attraction_details.json，打包进 assets"""

import json
import os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
INPUT = os.path.join(SCRIPT_DIR, "output", "attractions_enriched.json")
OUTPUT = os.path.join(SCRIPT_DIR, "..", "androidApp", "src", "main", "assets", "attraction_details.json")


def main():
    with open(INPUT, encoding="utf-8") as f:
        data = json.load(f)

    details = []
    for item in data:
        detail = {
            "id": item["id"],
            "iu": item.get("image_urls", []),
            "r": item.get("rating"),
            "c": item.get("cost"),
            "ot": item.get("open_time"),
            "t": item.get("tel"),
            "w": item.get("website"),
        }
        details.append(detail)

    with open(OUTPUT, "w", encoding="utf-8") as f:
        json.dump(details, f, ensure_ascii=False, separators=(",", ":"))

    print(f"Generated {OUTPUT}")
    print(f"  {len(details)} entries")

    with_images = sum(1 for d in details if d["iu"])
    with_rating = sum(1 for d in details if d["r"])
    print(f"  with images: {with_images}")
    print(f"  with rating: {with_rating}")


if __name__ == "__main__":
    main()
