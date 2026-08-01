#!/usr/bin/env python3

#  This file is part of Metronome.
#  Copyright (C) 2026 Philipp Bobek <philipp.bobek@mailbox.org>
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  Metronome is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program.  If not, see <http://www.gnu.org/licenses/>.

"""
Generates fastlane/metadata/android/<locale>/images/featureGraphic.png for every
locale that has a translated app name, using the app icon and theme colors from
app/src/main/res/values/colors.xml so the graphic always matches the app's branding.

Layout is a 16-column grid (64px module) on a 1024x500 canvas:
  64px margin | 384px icon | 64px gap | 448px text box | 64px margin

Requires python3-pillow built with libraqm (for Arabic shaping/bidi). The Noto Sans
fonts needed to cover this app's locales are fetched on first use from the google/fonts
GitHub repo (pinned to GOOGLE_FONTS_COMMIT below) into fastlane/.fonts-cache/ (git-ignored),
so no manual font installation is needed. Each cached font's SHA-256 is recorded alongside
it at download time and re-checked on every run, to catch local cache corruption; the
initial download itself is authenticated by HTTPS + the pinned commit hash.
"""

import hashlib
import re
import sys
import urllib.request
from PIL import Image, ImageDraw, ImageFont
from functools import lru_cache
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
RES_DIR = REPO_ROOT / "app" / "src" / "main" / "res"
COLORS_XML = RES_DIR / "values" / "colors.xml"
APP_ICON = REPO_ROOT / "fastlane" / "metadata" / "android" / "en-US" / "images" / "icon.png"
METADATA_DIR = REPO_ROOT / "fastlane" / "metadata" / "android"

CANVAS_WIDTH = 1024
CANVAS_HEIGHT = 500
SUPERSAMPLE = 4

MODULE = 64
ICON_LEFT = MODULE
ICON_SIZE = CANVAS_HEIGHT - 2 * MODULE  # 384, vertically centered
ICON_CORNER_RATIO = 0.115

TEXT_LEFT = ICON_LEFT + ICON_SIZE + MODULE  # 512
TEXT_BOX_WIDTH = CANVAS_WIDTH - TEXT_LEFT - MODULE  # 448
TEXT_BOX_HEIGHT = 320
MIN_FONT_SIZE = 10
MAX_FONT_SIZE = 300

GOOGLE_FONTS_COMMIT = "2796410152d4f9524b68ed46e69c1b60f8e0f7c3"
GOOGLE_FONTS_RAW_BASE = f"https://raw.githubusercontent.com/google/fonts/{GOOGLE_FONTS_COMMIT}/"
FONTS_CACHE_DIR = REPO_ROOT / "fastlane" / ".fonts-cache"

# Script key -> path of a standalone (non-collection) variable font file in the
# google/fonts repo, covering the scripts this app's translated names use.
FONTS = {
    "latin": "ofl/notosans/NotoSans[wdth,wght].ttf",
    "arabic": "ofl/notosansarabic/NotoSansArabic[wdth,wght].ttf",
    "devanagari": "ofl/notosansdevanagari/NotoSansDevanagari[wdth,wght].ttf",
    "hebrew": "ofl/notosanshebrew/NotoSansHebrew[wdth,wght].ttf",
    "tamil": "ofl/notosanstamil/NotoSansTamil[wdth,wght].ttf",
    "cjk-jp": "ofl/notosansjp/NotoSansJP[wght].ttf",
    "cjk-kr": "ofl/notosanskr/NotoSansKR[wght].ttf",
    "cjk-sc": "ofl/notosanssc/NotoSansSC[wght].ttf",
    "cjk-tc": "ofl/notosanstc/NotoSansTC[wght].ttf",
}

# Fastlane locale directory -> FONTS key used to render its app name.
LOCALE_TO_FONT_KEY = {
    "ar": "arabic",
    "hi-IN": "devanagari",
    "iw-IL": "hebrew",
    "ta-IN": "tamil",
    "ja-JP": "cjk-jp",
    "ko-KR": "cjk-kr",
    "zh-CN": "cjk-sc",
    "zh-HK": "cjk-tc",
    "zh-TW": "cjk-tc",
}
DEFAULT_FONT_KEY = "latin"


def read_color(name: str) -> tuple[int, int, int]:
    xml = COLORS_XML.read_text(encoding="utf-8")
    match = re.search(rf'<color name="{re.escape(name)}">#([0-9A-Fa-f]{{6}})</color>', xml)
    if not match:
        sys.exit(f"Color '{name}' not found in {COLORS_XML}")
    hex_value = match.group(1)
    return tuple(int(hex_value[i: i + 2], 16) for i in (0, 2, 4))


def read_app_name(locale: str) -> str:
    title_txt = METADATA_DIR / locale / "title.txt"
    if not title_txt.exists():
        sys.exit(f"{title_txt} does not exist")
    return title_txt.read_text(encoding="utf-8").strip()


@lru_cache(maxsize=None)
def resolve_font_file(key: str) -> Path:
    repo_path = FONTS[key]
    filename = repo_path.rsplit("/", 1)[-1]
    cached = FONTS_CACHE_DIR / filename
    checksum_file = cached.with_suffix(cached.suffix + ".sha256")

    if (
            cached.exists()
            and checksum_file.exists()
            and checksum_file.read_text().strip() == hashlib.sha256(cached.read_bytes()).hexdigest()
    ):
        return cached

    url = GOOGLE_FONTS_RAW_BASE + repo_path
    print(f"Downloading font: {url}")
    try:
        with urllib.request.urlopen(url, timeout=30) as response:
            data = response.read()
    except OSError as error:
        sys.exit(
            f"Failed to download font from {url}: {error}\n"
            f"Check network connectivity, or manually place the file at {cached}."
        )

    FONTS_CACHE_DIR.mkdir(parents=True, exist_ok=True)
    tmp_file = cached.with_suffix(cached.suffix + ".part")
    tmp_file.write_bytes(data)
    tmp_file.replace(cached)
    checksum_file.write_text(hashlib.sha256(data).hexdigest() + "\n")
    return cached


def load_font(locale: str, size: int) -> ImageFont.FreeTypeFont:
    key = LOCALE_TO_FONT_KEY.get(locale, DEFAULT_FONT_KEY)
    font_file = resolve_font_file(key)
    font = ImageFont.truetype(str(font_file), size, layout_engine=ImageFont.Layout.RAQM)
    try:
        if b"Medium" in font.get_variation_names():
            font.set_variation_by_name("Medium")
    except OSError:
        pass  # static (non-variable) font, e.g. the CJK collections
    return font


def _unmix(pixel: tuple[int, int, int], bg: tuple[int, int, int]) -> tuple[float, tuple[int, int, int]]:
    """Chroma-key one pixel against a flat background color.

    Assumes the icon is flat foreground color(s) anti-aliased against a solid
    background: pixel = alpha * fg + (1 - alpha) * bg, for some unknown opaque
    fg and alpha. Recovers both by casting a ray from bg through pixel and
    finding where it exits the RGB cube - that exit point is fg (saturated on
    at least one channel), and how far pixel already sits along that ray is
    alpha. This works for any foreground color, not just black/white, as long
    as it differs from the background.
    """
    diffs = tuple(p - bg_c for p, bg_c in zip(pixel, bg))
    if diffs == (0, 0, 0):
        return 0.0, bg

    t_max = float("inf")
    for d, bg_c in zip(diffs, bg):
        if d > 0:
            t_max = min(t_max, (255 - bg_c) / d)
        elif d < 0:
            t_max = min(t_max, -bg_c / d)
    alpha = min(1.0, 1.0 / t_max)
    fg = tuple(round(min(255.0, max(0.0, bg_c + d / alpha))) for d, bg_c in zip(diffs, bg))
    return alpha, fg


def build_logo(size: int, square_color: tuple[int, int, int]) -> Image.Image:
    hi_res = size * SUPERSAMPLE
    icon = Image.open(APP_ICON).convert("RGB")
    icon_px = icon.load()
    bg = icon_px[0, 0]

    glyph = Image.new("RGBA", icon.size, (255, 255, 255, 0))
    glyph_px = glyph.load()
    for y in range(icon.size[1]):
        for x in range(icon.size[0]):
            alpha, fg = _unmix(icon_px[x, y], bg)
            glyph_px[x, y] = (*fg, round(alpha * 255))
    glyph = glyph.resize((hi_res, hi_res), Image.LANCZOS)

    logo = Image.new("RGBA", (hi_res, hi_res), (0, 0, 0, 0))
    draw = ImageDraw.Draw(logo)
    draw.rounded_rectangle(
        (0, 0, hi_res - 1, hi_res - 1),
        radius=round(hi_res * ICON_CORNER_RATIO),
        fill=(*square_color, 255),
    )
    logo.alpha_composite(glyph)
    return logo.resize((size, size), Image.LANCZOS)


def fit_font_size(locale: str, text: str) -> ImageFont.FreeTypeFont:
    target_w = TEXT_BOX_WIDTH * SUPERSAMPLE
    target_h = TEXT_BOX_HEIGHT * SUPERSAMPLE
    lo, hi = MIN_FONT_SIZE, MAX_FONT_SIZE * SUPERSAMPLE
    best = load_font(locale, lo)
    scratch = ImageDraw.Draw(Image.new("RGB", (1, 1)))
    while lo <= hi:
        mid = (lo + hi) // 2
        font = load_font(locale, mid)
        left, top, right, bottom = scratch.textbbox((0, 0), text, font=font)
        if (right - left) <= target_w and (bottom - top) <= target_h:
            best = font
            lo = mid + 1
        else:
            hi = mid - 1
    return best


def render_feature_graphic(locale: str, background: tuple, icon_bg: tuple, text_color: tuple) -> Image.Image:
    text = read_app_name(locale)
    scale = SUPERSAMPLE
    canvas = Image.new("RGB", (CANVAS_WIDTH * scale, CANVAS_HEIGHT * scale), background)
    draw = ImageDraw.Draw(canvas)

    logo = build_logo(ICON_SIZE * scale, icon_bg)
    canvas.paste(logo, (ICON_LEFT * scale, MODULE * scale), logo)

    font = fit_font_size(locale, text)
    left, top, right, bottom = draw.textbbox((0, 0), text, font=font)
    x = TEXT_LEFT * scale - left
    y = (CANVAS_HEIGHT * scale) // 2 - (bottom - top) // 2 - top
    draw.text((x, y), text, font=font, fill=text_color)

    return canvas.resize((CANVAS_WIDTH, CANVAS_HEIGHT), Image.LANCZOS)


def main() -> None:
    background = read_color("md_theme_dark_primaryContainer")
    icon_bg = read_color("seed")
    text_color = read_color("md_theme_light_onPrimary")

    locales = sorted(p.name for p in METADATA_DIR.iterdir() if p.is_dir())
    for locale in locales:
        image = render_feature_graphic(locale, background, icon_bg, text_color)
        out_dir = METADATA_DIR / locale / "images"
        out_dir.mkdir(parents=True, exist_ok=True)
        out_path = out_dir / "featureGraphic.png"
        image.save(out_path, "PNG")
        print(f"Wrote {out_path}")


if __name__ == "__main__":
    main()
