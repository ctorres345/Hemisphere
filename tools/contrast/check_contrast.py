#!/usr/bin/env python3
"""Contrast check for the color tokens (HEM-6, docs/design-system.md section 5).

Reads tokens/color.<theme>.json for the Light, Dark and AMOLED themes and checks every theme x accent
combination against WCAG 2.2 contrast:

- text pairs need 4.5:1 (WCAG 1.4.3, body text; no token pair here is large-text only),
- the focus ring, border.strong and accent shapes need 3:1 (WCAG 1.4.11, non-text contrast).

Usage: python3 tools/contrast/check_contrast.py [--tokens DIR]
Exit codes: 0 all pass, 1 a pair fails, 2 a token file is missing or malformed.

Standard library only, so CI needs nothing but Python 3.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path

THEMES = ("light", "dark", "amoled")
ACCENTS = ("blue", "teal", "green", "amber", "orange", "red", "pink", "violet")
ACCENT_ROLES = ("fill", "onFill", "tint", "focus", "text")
SURFACES = ("surface.canvas", "surface.raised", "surface.overlay")
TEXT = 4.5
NON_TEXT = 3.0

HEX = re.compile(r"^#[0-9A-Fa-f]{6}$")
DEFAULT_TOKENS = Path(__file__).resolve().parents[2] / "tokens"


class TokenError(Exception):
    """A token file is missing, unreadable, or lacks a required token."""


@dataclass(frozen=True)
class Pair:
    fg: str
    bg: str
    minimum: float
    why: str


def relative_luminance(hex_color: str) -> float:
    def channel(i: int) -> float:
        c = int(hex_color[i : i + 2], 16) / 255
        return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4

    r, g, b = (channel(i) for i in (1, 3, 5))
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def contrast(a: str, b: str) -> float:
    hi, lo = sorted((relative_luminance(a), relative_luminance(b)), reverse=True)
    return (hi + 0.05) / (lo + 0.05)


def load_theme(tokens_dir: Path, theme: str) -> dict[str, str]:
    """Flatten color.<theme>.json into {"surface.canvas": "#F7F7F8", ...} without the leading "color."."""
    path = tokens_dir / f"color.{theme}.json"
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as e:
        raise TokenError(f"{path}: missing") from e
    except json.JSONDecodeError as e:
        raise TokenError(f"{path}: not valid JSON ({e})") from e

    flat: dict[str, str] = {}

    def walk(node: object, prefix: str) -> None:
        if not isinstance(node, dict):
            return
        if "$value" in node:
            value = node["$value"]
            if not isinstance(value, str) or not HEX.match(value):
                raise TokenError(f"{path}: {prefix} must be a #RRGGBB color, got {value!r}")
            flat[prefix] = value.upper()
            return
        for key, child in node.items():
            if not key.startswith("$"):
                walk(child, f"{prefix}.{key}" if prefix else key)

    color = data.get("color") if isinstance(data, dict) else None
    if not isinstance(color, dict):
        raise TokenError(f"{path}: no top-level \"color\" group")
    walk(color, "")

    required = [
        *SURFACES,
        "border.default",
        "border.strong",
        "text.primary",
        "text.secondary",
        "text.tertiary",
        "semantic.danger",
        "semantic.warning",
        "semantic.success",
        *(f"accent.{a}.{r}" for a in ACCENTS for r in ACCENT_ROLES),
    ]
    missing = [t for t in required if t not in flat]
    if missing:
        raise TokenError(f"{path}: missing tokens: {', '.join(missing)}")
    return flat


def theme_pairs() -> list[Pair]:
    """Pairs that depend only on the theme, not on the accent."""
    pairs = []
    for s in SURFACES:
        # text.secondary and text.tertiary are also the inactive pane's step-down (section 3).
        for t in ("text.primary", "text.secondary", "text.tertiary"):
            pairs.append(Pair(t, s, TEXT, "text"))
        for t in ("semantic.danger", "semantic.warning", "semantic.success"):
            pairs.append(Pair(t, s, TEXT, "semantic label"))
        pairs.append(Pair("border.strong", s, NON_TEXT, "control boundary"))
    return pairs


def accent_pairs(accent: str) -> list[Pair]:
    a = f"accent.{accent}"
    pairs = [Pair(f"{a}.onFill", f"{a}.fill", TEXT, "label on accent fill")]
    for s in (*SURFACES, f"{a}.tint"):
        pairs.append(Pair(f"{a}.text", s, TEXT, "accent text"))
        pairs.append(Pair(f"{a}.focus", s, NON_TEXT, "focus ring"))
    for s in SURFACES:
        pairs.append(Pair(f"{a}.fill", s, NON_TEXT, "accent shape"))
    # Selected rows: names (primary, or secondary in the inactive pane) and meta stay readable.
    # text.tertiary is never used on accent.tint (docs/design-system.md section 3).
    for t in ("text.primary", "text.secondary"):
        pairs.append(Pair(t, f"{a}.tint", TEXT, "selected row"))
    return pairs


@dataclass(frozen=True)
class Result:
    theme: str
    accent: str
    failures: list[str]
    lowest_text: float
    lowest_focus: float


def check(tokens_dir: Path) -> list[Result]:
    results = []
    for theme in THEMES:
        colors = load_theme(tokens_dir, theme)
        shared = theme_pairs()
        for accent in ACCENTS:
            failures, text_ratios, focus_ratios = [], [], []
            for p in (*shared, *accent_pairs(accent)):
                ratio = contrast(colors[p.fg], colors[p.bg])
                if p.why in ("accent text", "label on accent fill", "selected row"):
                    text_ratios.append(ratio)
                if p.why == "focus ring":
                    focus_ratios.append(ratio)
                # Round down to 2 decimals so 4.499 never passes as 4.50.
                if int(ratio * 100) / 100 < p.minimum:
                    failures.append(
                        f"{p.fg} {colors[p.fg]} on {p.bg} {colors[p.bg]}: "
                        f"{ratio:.2f}:1, needs {p.minimum}:1 ({p.why})"
                    )
            results.append(Result(theme, accent, failures, min(text_ratios), min(focus_ratios)))
    return results


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--tokens", type=Path, default=DEFAULT_TOKENS, help="token directory (default: tokens/)")
    args = parser.parse_args(argv)

    try:
        results = check(args.tokens)
    except TokenError as e:
        print(f"error: {e}", file=sys.stderr)
        return 2

    failed = [r for r in results if r.failures]
    for r in results:
        status = "FAIL" if r.failures else "ok  "
        print(
            f"{status} {r.theme:<6} x {r.accent:<6}  "
            f"lowest text {r.lowest_text:5.2f}:1  lowest focus {r.lowest_focus:5.2f}:1"
        )
        for f in r.failures:
            print(f"       {f}")
    print(f"\n{len(results)} combinations checked, {len(failed)} failed.")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
