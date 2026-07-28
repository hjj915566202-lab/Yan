#!/usr/bin/env python3
"""Convert Sanotsu/china-food-composition-data JSON files into a compact Android asset."""

from __future__ import annotations

import hashlib
import json
import re
import sys
from pathlib import Path

SOURCE_REPO = "Sanotsu/china-food-composition-data"
SOURCE_COMMIT = "095034a96376d893582b412900fa8fdf792b4194"
TRACE_VALUES = {"", "-", "--", "—", "…", "tr", "trace", "微量", "未检出", "nd", "n.d."}


def number(value: object) -> float:
    if value is None:
        return 0.0
    if isinstance(value, (int, float)):
        return float(value)
    text = str(value).strip().lower().replace(",", "")
    if text in TRACE_VALUES:
        return 0.0
    match = re.search(r"-?\d+(?:\.\d+)?", text)
    return float(match.group()) if match else 0.0


def clean_text(value: object) -> str:
    return "" if value is None else str(value).strip()


def parse_category(path: Path) -> tuple[str, str]:
    name = path.stem
    for prefix in ("merged_", "merged-"):
        if name.startswith(prefix):
            name = name[len(prefix):]
    if "-" in name:
        major, sub = name.split("-", 1)
    else:
        major, sub = name, ""
    return major.strip(), sub.strip()


def stable_id(code: str, name: str, category: str) -> str:
    if code:
        safe = re.sub(r"[^0-9A-Za-z_-]+", "_", code)
        return f"cn_{safe}"
    digest = hashlib.sha1(f"{category}|{name}".encode("utf-8")).hexdigest()[:12]
    return f"cn_{digest}"


def convert_row(row: dict, major: str, sub: str) -> dict | None:
    name = clean_text(row.get("foodName"))
    if not name:
        return None

    kcal = number(row.get("energyKCal"))
    kj = number(row.get("energyKJ"))
    if kcal <= 0 < kj:
        kcal = kj / 4.184

    protein = number(row.get("protein"))
    fat = number(row.get("fat"))
    carb = number(row.get("CHO"))
    fiber = number(row.get("dietaryFiber"))

    # Remove clearly impossible OCR rows while keeping legitimate oils, salt and dried foods.
    if not (0 <= kcal <= 1000):
        return None
    if any(v < 0 or v > 100 for v in (protein, fat, carb, fiber)):
        return None

    code = clean_text(row.get("foodCode"))
    return {
        "id": stable_id(code, name, major + "/" + sub),
        "name": name,
        "category": major or "其他类",
        "subCategory": sub,
        "sourceCode": code,
        "remark": clean_text(row.get("remark")),
        "kcal": round(kcal, 4),
        "protein": protein,
        "fat": fat,
        "carb": carb,
        "fiber": fiber,
        "sodium": number(row.get("Na")),
        "edible": number(row.get("edible")),
        "water": number(row.get("water")),
        "cholesterol": number(row.get("cholesterol")),
        "ash": number(row.get("ash")),
        "vitaminA": number(row.get("vitaminA")),
        "carotene": number(row.get("carotene")),
        "retinol": number(row.get("retinol")),
        "thiamin": number(row.get("thiamin")),
        "riboflavin": number(row.get("riboflavin")),
        "niacin": number(row.get("niacin")),
        "vitaminC": number(row.get("vitaminC")),
        "vitaminE": number(row.get("vitaminETotal")),
        "calcium": number(row.get("Ca")),
        "phosphorus": number(row.get("P")),
        "potassium": number(row.get("K")),
        "magnesium": number(row.get("Mg")),
        "iron": number(row.get("Fe")),
        "zinc": number(row.get("Zn")),
        "selenium": number(row.get("Se")),
        "copper": number(row.get("Cu")),
        "manganese": number(row.get("Mn")),
    }


def main() -> int:
    if len(sys.argv) != 3:
        print("usage: build_china_food_asset.py INPUT_DIR OUTPUT_JSON", file=sys.stderr)
        return 2

    input_dir = Path(sys.argv[1])
    output = Path(sys.argv[2])
    if not input_dir.is_dir():
        raise SystemExit(f"input directory not found: {input_dir}")

    foods: list[dict] = []
    rejected = 0
    seen: set[tuple[str, str, str]] = set()

    for path in sorted(input_dir.glob("*.json")):
        major, sub = parse_category(path)
        with path.open("r", encoding="utf-8-sig") as handle:
            rows = json.load(handle)
        if not isinstance(rows, list):
            continue
        for row in rows:
            if not isinstance(row, dict):
                rejected += 1
                continue
            item = convert_row(row, major, sub)
            if item is None:
                rejected += 1
                continue
            key = (item["sourceCode"], item["name"], item["category"] + "/" + item["subCategory"])
            if key in seen:
                continue
            seen.add(key)
            foods.append(item)

    foods.sort(key=lambda x: (x["category"], x["subCategory"], x["name"], x["sourceCode"]))
    payload = {
        "sourceRepository": SOURCE_REPO,
        "sourceCommit": SOURCE_COMMIT,
        "sourceNotice": "数据由公开GitHub仓库根据《中国食物成分表标准版（第6版）》截图经视觉模型/OCR整理；非官方授权数据，识别准确性不作保证。原始Tr、空值和无法解析值在本软件中按0处理。",
        "count": len(foods),
        "rejectedRows": rejected,
        "foods": foods,
    }

    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, separators=(",", ":"))

    print(f"generated {len(foods)} foods; rejected {rejected}; output={output}")
    if len(foods) < 1600:
        raise SystemExit("converted food count is unexpectedly low")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
