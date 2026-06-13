#!/opt/homebrew/bin/python3.11
"""Generate 100 unique badge images for MapChina achievements using Flux 2 Klein 4B on MPS."""

from __future__ import annotations

import sys
import time
from pathlib import Path

import torch
from diffusers import Flux2KleinPipeline

BADGE_DIR = Path(__file__).parent / "shared" / "src" / "commonMain" / "composeResources" / "drawable"
PROMPTS_FILE = Path(__file__).parent / "badge_prompts_v2.txt"

# Achievement IDs in exact order matching badge_prompts_v2.txt
ACHIEVEMENT_IDS = [
    # Region district (4)
    "region_district_1", "region_district_10", "region_district_30", "region_district_100",
    # Region city (4)
    "region_city_1", "region_city_10", "region_city_30", "region_city_100",
    # Region province (5)
    "region_province_1", "region_province_5", "region_province_10", "region_province_20", "region_province_31",
    # Scenic 5A (5)
    "scenic_5a_1", "scenic_5a_10", "scenic_5a_30", "scenic_5a_50", "scenic_5a_100",
    # Scenic total (4)
    "scenic_total_10", "scenic_total_50", "scenic_total_100", "scenic_total_300",
    # Atlas heritage (4)
    "atlas_heritage_1", "atlas_heritage_5", "atlas_heritage_10", "atlas_heritage_20",
    # Atlas museum (3)
    "atlas_museum_5", "atlas_museum_20", "atlas_museum_50",
    # Atlas mountain (3)
    "atlas_mountain_5", "atlas_mountain_10", "atlas_mountain_20",
    # Province visit (31)
    *[f"province_visit_{c}" for c in [
        "11","12","13","14","15","21","22","23","31","32","33","34","35","36","37",
        "41","42","43","44","45","46","50","51","52","53","54","61","62","63","64","65"
    ]],
    # Province complete (31)
    *[f"province_complete_{c}" for c in [
        "11","12","13","14","15","21","22","23","31","32","33","34","35","36","37",
        "41","42","43","44","45","46","50","51","52","53","54","61","62","63","64","65"
    ]],
    # Geo (6)
    "geo_north", "geo_south", "geo_silk_road", "geo_coast", "geo_river", "geo_same_day_3",
]


def main():
    prompts = PROMPTS_FILE.read_text().strip().split("\n")
    prompts = [p.strip() for p in prompts if p.strip()]

    if len(prompts) != len(ACHIEVEMENT_IDS):
        print(f"ERROR: {len(prompts)} prompts != {len(ACHIEVEMENT_IDS)} achievement IDs")
        sys.exit(1)

    print(f"Loading Flux 2 Klein 4B on MPS...")
    pipe = Flux2KleinPipeline.from_pretrained(
        "black-forest-labs/FLUX.2-klein-4B",
        torch_dtype=torch.float16,
        local_files_only=True,
    )
    pipe.to("mps")
    pipe.enable_attention_slicing()
    print("Model loaded!")

    BADGE_DIR.mkdir(parents=True, exist_ok=True)

    for i, (prompt, aid) in enumerate(zip(prompts, ACHIEVEMENT_IDS)):
        output_path = BADGE_DIR / f"badge_{aid}.png"

        if output_path.exists():
            print(f"[{i+1:3d}/100] SKIP (exists): badge_{aid}.png")
            continue

        print(f"[{i+1:3d}/100] Generating badge_{aid}.png ...")
        start = time.time()

        generator = torch.Generator("mps").manual_seed(42 + i)
        image = pipe(
            prompt=prompt,
            width=256,
            height=256,
            num_inference_steps=4,
            guidance_scale=3.5,
            generator=generator,
        ).images[0]

        image.save(str(output_path))
        elapsed = time.time() - start
        print(f"  -> saved in {elapsed:.1f}s")

    print(f"\nDone! All badges saved to {BADGE_DIR}")


if __name__ == "__main__":
    main()
