"""Tests for check_contrast.py. Run: python3 -m unittest discover -s tools/contrast"""

from __future__ import annotations

import contextlib
import io
import json
import re
import shutil
import tempfile
import unittest
from collections.abc import Iterator
from pathlib import Path

import check_contrast as cc

REPO = Path(__file__).resolve().parents[2]
REAL_TOKENS = REPO / "tokens"
PROTOTYPE = REPO / "docs" / "prototype" / "index.html"
SPEC = REPO / "docs" / "design-system.md"
COMBINATIONS = len(cc.THEMES) * len(cc.ACCENTS)
# accent.tint is accent.fill mixed into surface.raised at this share (docs/design-system.md section 5).
TINT_SHARE = {"light": 0.12, "dark": 0.18, "amoled": 0.20}


def run(tokens: Path) -> tuple[int, str]:
    out = io.StringIO()
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(out):
        code = cc.main(["--tokens", str(tokens)])
    return code, out.getvalue()


@contextlib.contextmanager
def fixture_tokens(theme: str, path: list[str], value: str | None) -> Iterator[Path]:
    """A copy of the color tokens in a temp dir, with one value overridden (None deletes the token)."""
    with tempfile.TemporaryDirectory() as tmp:
        for f in REAL_TOKENS.glob("color.*.json"):
            shutil.copy(f, tmp)
        file = Path(tmp) / f"color.{theme}.json"
        data = json.loads(file.read_text(encoding="utf-8"))
        node = data["color"]
        for key in path[:-1]:
            node = node[key]
        if value is None:
            del node[path[-1]]
        else:
            node[path[-1]]["$value"] = value
        file.write_text(json.dumps(data), encoding="utf-8")
        yield Path(tmp)


class ContrastMath(unittest.TestCase):
    def test_black_on_white_is_21(self):
        self.assertAlmostEqual(cc.contrast("#000000", "#FFFFFF"), 21.0, places=2)

    def test_same_color_is_1(self):
        self.assertAlmostEqual(cc.contrast("#2F5BD3", "#2F5BD3"), 1.0, places=6)

    def test_known_pair(self):
        # #767676 on white is the classic 4.54:1 AA threshold color.
        self.assertAlmostEqual(cc.contrast("#767676", "#FFFFFF"), 4.54, places=2)


class RealTokens(unittest.TestCase):
    def test_every_combination_passes(self):
        code, out = run(REAL_TOKENS)
        self.assertEqual(code, 0, out)
        self.assertEqual(COMBINATIONS, 24)
        self.assertIn(f"{COMBINATIONS} combinations checked, 0 failed.", out)

    def test_aliases_resolve_to_the_fill(self):
        colors = cc.load_theme(REAL_TOKENS, "dark")
        self.assertEqual(colors["accent.blue.focus"], colors["accent.blue.fill"])

    def test_tint_follows_its_rule(self):
        for theme in cc.THEMES:
            colors = cc.load_theme(REAL_TOKENS, theme)
            raised = colors["surface.raised"]
            for accent in cc.ACCENTS:
                fill, tint = colors[f"accent.{accent}.fill"], colors[f"accent.{accent}.tint"]
                for i in (1, 3, 5):
                    expected = int(fill[i : i + 2], 16) * TINT_SHARE[theme] + int(raised[i : i + 2], 16) * (1 - TINT_SHARE[theme])
                    self.assertLessEqual(abs(int(tint[i : i + 2], 16) - expected), 1, f"{theme} {accent} tint")


class NoDrift(unittest.TestCase):
    """The prototype and the spec repeat token values; they must match the token files."""

    def test_prototype_accents_match_tokens(self):
        html = PROTOTYPE.read_text(encoding="utf-8")
        block = re.search(r"const ACCENTS = \{(.*?)\};", html, re.S).group(1)
        proto = {k: v for k, v in re.findall(r"(\w+): \[([^\]]*)\]", block)}
        light, dark, amoled = (cc.load_theme(REAL_TOKENS, t) for t in cc.THEMES)
        self.assertEqual(set(proto), set(cc.ACCENTS))
        for accent, values in proto.items():
            hexes = [h.upper() for h in re.findall(r"#[0-9A-Fa-f]{6}", values)]
            a = f"accent.{accent}"
            self.assertEqual(hexes, [light[f"{a}.fill"], light[f"{a}.onFill"], dark[f"{a}.fill"], dark[f"{a}.onFill"]], accent)
            # The prototype has one pair for Dark and AMOLED, so the tokens must agree.
            self.assertEqual((dark[f"{a}.fill"], dark[f"{a}.onFill"]), (amoled[f"{a}.fill"], amoled[f"{a}.onFill"]), accent)

    def test_prototype_neutrals_match_tokens(self):
        html = PROTOTYPE.read_text(encoding="utf-8")
        names = {
            "canvas": "surface.canvas", "raised": "surface.raised", "overlay": "surface.overlay",
            "bd": "border.default", "bd-strong": "border.strong",
            "t1": "text.primary", "t2": "text.secondary", "t3": "text.tertiary",
            "danger": "semantic.danger", "warning": "semantic.warning", "success": "semantic.success",
        }
        selectors = {"light": r"\.dev \{", "dark": r'\.dev\[data-t="dark"\] \{', "amoled": r'\.dev\[data-t="amoled"\] \{'}
        for theme, selector in selectors.items():
            body = re.search(selector + r"(.*?)\}", html, re.S).group(1)
            css = {k: v.upper() for k, v in re.findall(r"--([\w-]+): (#[0-9A-Fa-f]{6})", body)}
            colors = cc.load_theme(REAL_TOKENS, theme)
            for var, token in names.items():
                self.assertEqual(css.get(var), colors[token], f"{theme} --{var}")

    def test_prototype_tint_share_matches_tokens(self):
        html = PROTOTYPE.read_text(encoding="utf-8")
        shares = re.findall(r"--a-tint: color-mix\(in srgb, var\(--a-fill\) (\d+)%, var\(--raised\)\)", html)
        self.assertEqual([int(s) / 100 for s in shares], [TINT_SHARE[t] for t in cc.THEMES])

    def test_spec_neutral_and_semantic_tables_match_tokens(self):
        spec = SPEC.read_text(encoding="utf-8")
        light, dark, amoled = (cc.load_theme(REAL_TOKENS, t) for t in cc.THEMES)
        for token in ("surface.canvas", "surface.raised", "surface.overlay", "border.default", "border.strong",
                      "text.primary", "text.secondary", "text.tertiary"):
            self.assertIn(f"| `{token}` | `{light[token]}` | `{dark[token]}` | `{amoled[token]}` |", spec, token)
        for name in ("danger", "warning", "success"):
            token = f"semantic.{name}"
            self.assertEqual(dark[token], amoled[token], token)
            self.assertRegex(spec, rf"\| `{name}` \|[^\n]*\| `{light[token]}` \| `{dark[token]}` \|", token)

    def test_spec_accent_table_matches_tokens(self):
        spec = SPEC.read_text(encoding="utf-8")
        light, dark = cc.load_theme(REAL_TOKENS, "light"), cc.load_theme(REAL_TOKENS, "dark")
        for accent in cc.ACCENTS:
            row = f"| {accent} | `{light[f'accent.{accent}.fill']}` | `{dark[f'accent.{accent}.fill']}` |"
            self.assertIn(row, spec, accent)


class Failures(unittest.TestCase):
    def test_low_contrast_accent_fails(self):
        # Fixture: the Dark theme's light-blue accent fill used in the Light theme.
        with fixture_tokens("light", ["accent", "blue", "fill"], "#8AA8F8") as tokens:
            code, out = run(tokens)
        self.assertEqual(code, 1, out)
        self.assertIn("FAIL light  x blue", out)
        self.assertIn("1 failed.", out)

    def test_invisible_focus_ring_fails(self):
        with fixture_tokens("amoled", ["accent", "violet", "focus"], "#1A1A1C") as tokens:
            code, out = run(tokens)
        self.assertEqual(code, 1, out)
        self.assertIn("focus ring", out)

    def test_weak_border_strong_fails_every_accent_in_that_theme(self):
        with fixture_tokens("dark", ["border", "strong"], "#303236") as tokens:
            code, out = run(tokens)
        self.assertEqual(code, 1, out)
        self.assertIn(f"{len(cc.ACCENTS)} failed.", out)

    def test_missing_token_is_an_error(self):
        with fixture_tokens("dark", ["accent", "teal", "tint"], None) as tokens:
            code, out = run(tokens)
        self.assertEqual(code, 2, out)
        self.assertIn("accent.teal.tint", out)

    def test_non_hex_value_is_an_error(self):
        with fixture_tokens("light", ["surface", "canvas"], "white") as tokens:
            code, out = run(tokens)
        self.assertEqual(code, 2, out)

    def test_broken_alias_is_an_error(self):
        with fixture_tokens("light", ["accent", "red", "text"], "{color.accent.red.nope}") as tokens:
            code, out = run(tokens)
        self.assertEqual(code, 2, out)
        self.assertIn("accent.red.text", out)


if __name__ == "__main__":
    unittest.main()
