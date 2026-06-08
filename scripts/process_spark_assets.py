from pathlib import Path
from PIL import Image, ImageFilter
from collections import deque

RAW_DIR = Path("art/spark_raw")
OUT_DIR = Path("app/src/main/res/drawable-nodpi")
OUT_DIR.mkdir(parents=True, exist_ok=True)

MOODS = {
    "idle.png": "spark_fairy_idle.png",
    "happy.png": "spark_fairy_happy.png",
    "thinking.png": "spark_fairy_thinking.png",
    "listening.png": "spark_fairy_listening.png",
    "alert.png": "spark_fairy_alert.png",
    "sleepy.png": "spark_fairy_sleepy.png",
    "speaking.png": "spark_fairy_speaking.png",
}

TARGET = 1024
PADDING = 72


def is_bg(r, g, b, a):
    if a < 12:
        return True
    if r > 235 and g > 235 and b > 235:
        return True
    if g > 140 and g > r * 1.22 and g > b * 1.22:
        return True
    if abs(r - g) < 10 and abs(g - b) < 10 and 45 <= r <= 225:
        return True
    return False


def remove_edge_bg(img):
    img = img.convert("RGBA")
    px = img.load()
    w, h = img.size
    seen = [[False] * h for _ in range(w)]
    q = deque()

    def add(x, y):
        if x < 0 or y < 0 or x >= w or y >= h:
            return
        if seen[x][y]:
            return
        r, g, b, a = px[x, y]
        if is_bg(r, g, b, a):
            seen[x][y] = True
            q.append((x, y))

    for x in range(w):
        add(x, 0)
        add(x, h - 1)
    for y in range(h):
        add(0, y)
        add(w - 1, y)

    while q:
        x, y = q.popleft()
        add(x + 1, y)
        add(x - 1, y)
        add(x, y + 1)
        add(x, y - 1)

    for x in range(w):
        for y in range(h):
            if seen[x][y]:
                r, g, b, a = px[x, y]
                px[x, y] = (r, g, b, 0)

    alpha = img.getchannel("A").filter(ImageFilter.GaussianBlur(0.35))
    img.putalpha(alpha)
    return img


def trim(img):
    bbox = img.getchannel("A").point(lambda p: 255 if p > 18 else 0).getbbox()
    return img.crop(bbox) if bbox else img


def square(img):
    img = trim(img)
    max_dim = TARGET - PADDING * 2
    scale = min(max_dim / img.width, max_dim / img.height)
    img = img.resize((int(img.width * scale), int(img.height * scale)), Image.Resampling.LANCZOS)

    canvas = Image.new("RGBA", (TARGET, TARGET), (0, 0, 0, 0))
    x = (TARGET - img.width) // 2
    y = (TARGET - img.height) // 2 - 18
    canvas.alpha_composite(img, (x, max(0, y)))
    return canvas


for raw_name, out_name in MOODS.items():
    src = RAW_DIR / raw_name
    if not src.exists():
        raise SystemExit(f"Missing raw render: {src}")

    img = Image.open(src)
    img = remove_edge_bg(img)
    img = square(img)
    img.save(OUT_DIR / out_name)
    print(f"Created {OUT_DIR / out_name}")

print("Done.")