#!/usr/bin/env python3
"""Smoke test local MapChina auth and generic sync endpoints."""

import json
import os
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

BASE_URL = os.environ.get("MAPCHINA_API_BASE_URL", "http://127.0.0.1:8080").rstrip("/")


def request(method, path, body=None, token=None):
    data = None
    headers = {}
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(f"{BASE_URL}{path}", data=data, headers=headers, method=method)
    with urllib.request.urlopen(req, timeout=10) as resp:
        raw = resp.read().decode("utf-8")
        if not raw:
            return None
        if resp.headers.get("Content-Type", "").startswith("application/json"):
            return json.loads(raw)
        try:
            return json.loads(raw)
        except json.JSONDecodeError:
            return raw


def main():
    print(f"Smoke testing {BASE_URL}")
    health = request("GET", "/health")
    if health != "OK":
        raise AssertionError(f"Unexpected /health response: {health!r}")

    login = request("POST", "/auth/login", {"phone": "18800009999", "code": "123456"})
    token = login["accessToken"]
    user_id = login["userId"]
    now = int(time.time() * 1000)
    entity_id = f"smoke-carving-{now}"
    payload = {
        "id": entity_id,
        "userId": user_id,
        "regionId": "110000",
        "regionName": "北京市",
        "imagePath": None,
        "strokeData": "[]",
        "createdAt": now,
        "attractionId": None,
        "attractionName": None,
        "previewAspectRatio": None,
    }
    push = request(
        "POST",
        "/sync/push",
        {
            "items": [
                {
                    "entityType": "CARVING",
                    "entityId": entity_id,
                    "operation": "UPSERT",
                    "payload": json.dumps(payload, ensure_ascii=False, separators=(",", ":")),
                    "updatedAt": now,
                    "deleted": False,
                }
            ]
        },
        token=token,
    )
    if push.get("accepted") != 1:
        raise AssertionError(f"Unexpected push response: {push}")

    query = urllib.parse.urlencode({"since": 0})
    delta = request("GET", f"/sync/pull?{query}", token=token)
    items = delta.get("items", [])
    if not any(item.get("entityId") == entity_id for item in items):
        raise AssertionError(f"Smoke item {entity_id} not found in delta: {delta}")

    print("Smoke sync passed.")


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, KeyError, urllib.error.URLError) as exc:
        print(f"Smoke sync failed: {exc}", file=sys.stderr)
        sys.exit(1)
