#!/usr/bin/env python3
"""Seed the MapChina PostgreSQL database with regions and attractions data."""

import json
import os
import psycopg2
import sys

ASSETS_DIR = os.path.join(os.path.dirname(__file__), '..', 'androidApp', 'src', 'main', 'assets')
BOUNDARIES_DIR = os.path.join(ASSETS_DIR, 'boundaries')
DISTRICTS_DIR = os.path.join(ASSETS_DIR, 'districts')

DB_CONFIG = {
    'host': 'localhost',
    'port': 5432,
    'database': 'mapchina',
    'user': 'mapchina',
    'password': 'mapchina',
}


def get_level(adcode):
    if adcode[2:] == '0000':
        return 'province'
    elif adcode[4:] == '00':
        return 'city'
    return 'district'


def get_parent_id(adcode):
    if adcode[2:] == '0000':
        return None
    elif adcode[4:] == '00':
        return adcode[:2] + '0000'
    return adcode[:4] + '00'


def build_region_map():
    """Build a map of adcode -> name from all available sources."""
    name_map = {}

    # From boundary files (primary source)
    for filename in os.listdir(BOUNDARIES_DIR):
        if not filename.endswith('.json'):
            continue
        filepath = os.path.join(BOUNDARIES_DIR, filename)
        with open(filepath, 'r') as f:
            data = json.load(f)
        props = data.get('properties', {})
        if 'name' in props:
            name_map[filename.replace('.json', '')] = props['name']

    # From district files (supplementary)
    for filename in os.listdir(DISTRICTS_DIR):
        if not filename.endswith('.json'):
            continue
        with open(os.path.join(DISTRICTS_DIR, filename)) as f:
            districts = json.load(f)
        for d in districts:
            name_map[str(d['adcode'])] = d['name']

    return name_map


def seed_regions(cur):
    """Insert all regions: from boundary files + placeholders for missing."""
    name_map = build_region_map()

    # Collect all needed region IDs
    needed_ids = set(name_map.keys())

    # Also add region IDs referenced by attractions
    attractions_path = os.path.join(ASSETS_DIR, 'attractions.json')
    with open(attractions_path, 'r') as f:
        attractions = json.load(f)
    attraction_region_ids = set(a['regionId'] for a in attractions)
    needed_ids.update(attraction_region_ids)

    # Also add parent IDs for all districts
    all_ids = set(needed_ids)
    for rid in list(all_ids):
        parent = get_parent_id(rid)
        if parent:
            needed_ids.add(parent)
        if rid[2:] != '0000':
            needed_ids.add(rid[:2] + '0000')  # province

    # Insert regions with boundary data first
    regions_with_boundary = []
    for filename in sorted(os.listdir(BOUNDARIES_DIR)):
        if not filename.endswith('.json'):
            continue
        adcode = filename.replace('.json', '')
        filepath = os.path.join(BOUNDARIES_DIR, filename)

        with open(filepath, 'r') as f:
            data = json.load(f)

        name = name_map.get(adcode, '')
        level = get_level(adcode)
        parent_id = get_parent_id(adcode)
        boundary_json = json.dumps(data, ensure_ascii=False)

        regions_with_boundary.append((adcode, name, level, parent_id, boundary_json))

    if regions_with_boundary:
        print(f'Inserting {len(regions_with_boundary)} regions with boundaries...')
        cur.executemany(
            'INSERT INTO regions (id, name, level, parent_id, boundary_json) VALUES (%s, %s, %s, %s, %s) '
            'ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, level = EXCLUDED.level, parent_id = EXCLUDED.parent_id, boundary_json = EXCLUDED.boundary_json',
            regions_with_boundary,
        )

    # Insert placeholder regions for missing IDs
    existing_codes = set(r[0] for r in regions_with_boundary)
    placeholders = []
    for adcode in sorted(needed_ids):
        if adcode in existing_codes:
            continue
        name = name_map.get(adcode, adcode)
        level = get_level(adcode)
        parent_id = get_parent_id(adcode)
        placeholders.append((adcode, name, level, parent_id, None))

    if placeholders:
        print(f'Inserting {len(placeholders)} placeholder regions...')
        cur.executemany(
            'INSERT INTO regions (id, name, level, parent_id, boundary_json) VALUES (%s, %s, %s, %s, %s) '
            'ON CONFLICT (id) DO UPDATE SET name = COALESCE(NULLIF(EXCLUDED.name, EXCLUDED.id), regions.name)',
            placeholders,
        )

    total = len(regions_with_boundary) + len(placeholders)
    print(f'  Total regions: {total}')


def seed_attractions(cur):
    """Insert attractions from the attractions JSON file."""
    attractions_path = os.path.join(ASSETS_DIR, 'attractions.json')
    with open(attractions_path, 'r') as f:
        attractions = json.load(f)

    if not attractions:
        print('No attractions found!')
        return

    rows = []
    for a in attractions:
        rows.append((
            a['id'],
            a['name'],
            a['regionId'],
            a.get('level', ''),
            a.get('latitude', 0.0),
            a.get('longitude', 0.0),
            a.get('description'),
        ))

    print(f'Inserting {len(rows)} attractions...')
    cur.executemany(
        'INSERT INTO attractions (id, name, region_id, level, latitude, longitude, description) VALUES (%s, %s, %s, %s, %s, %s, %s) '
        'ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, region_id = EXCLUDED.region_id, level = EXCLUDED.level, latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude, description = EXCLUDED.description',
        rows,
    )
    print(f'  Inserted/updated {len(rows)} attractions')


def main():
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        conn.autocommit = False
        cur = conn.cursor()

        print('Seeding MapChina database...')
        print()

        # Clear existing data (respecting FK order)
        print('Clearing existing data...')
        cur.execute('DELETE FROM attraction_visits')
        cur.execute('DELETE FROM footprints')
        cur.execute('DELETE FROM attractions')
        cur.execute('DELETE FROM regions')
        print('  Cleared.')
        print()

        seed_regions(cur)
        seed_attractions(cur)

        conn.commit()
        print()
        print('Seed complete!')

        # Verify
        cur.execute('SELECT count(*) FROM regions')
        print(f'  Regions: {cur.fetchone()[0]}')
        cur.execute('SELECT count(*) FROM attractions')
        print(f'  Attractions: {cur.fetchone()[0]}')
        cur.execute("SELECT level, count(*) FROM regions GROUP BY level ORDER BY level")
        for row in cur.fetchall():
            print(f'    {row[0]}: {row[1]}')

        cur.close()
        conn.close()

    except Exception as e:
        print(f'Error: {e}', file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
