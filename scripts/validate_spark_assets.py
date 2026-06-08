from pathlib import Path
from PIL import Image

REQUIRED = [
    "spark_fairy_idle.png",
    "spark_fairy_happy.png",
    "spark_fairy_thinking.png",
    "spark_fairy_listening.png",
    "spark_fairy_alert.png",
    "spark_fairy_sleepy.png",
    "spark_fairy_speaking.png",
]

BASE = Path("app/src/main/res/drawable-nodpi")
ok = True

print("=== Spark Baby Asset Validation ===")

for name in REQUIRED:
    path = BASE / name

    if not path.exists():
        print(f"❌ Missing: {path}")
        ok = False
        continue

    img = Image.open(path)
    print(f"\n{name}")
    print(f"  Size: {img.size}")
    print(f"  Mode: {img.mode}")
    print(f"  Bytes: {path.stat().st_size}")

    if img.size != (1024, 1024):
        print("  ⚠️ Must be 1024x1024")
        ok = False

    if img.mode != "RGBA":
        print("  ⚠️ Must be RGBA with alpha")
        ok = False

    if path.stat().st_size < 50_000:
        print("  ⚠️ Suspiciously small file")

if not ok:
    raise SystemExit("❌ Spark Baby assets failed validation.")

print("\n✅ All Spark Baby assets validated.")