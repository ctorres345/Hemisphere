"""Tests for check_contrast.py. Run: python3 -m unittest discover -s tools/contrast"""

from __future__ import annotations

import contextlib
import io
import json
import shutil
import tempfile
import unittest
from pathlib import Path

import check_contrast as cc

REAL_TOKENS = Path(__file__).resolve().parents[2] / "tokens"


def run(tokens: Path) -> tuple[int, str]:
    out = io.StringIO()
    with contextlib.redirect_stdout(out), contextlib.redirect_stderr(out):
        code = cc.main(["--tokens", str(tokens)])
    return code, out.getvalue()


class FixtureTokens:
    """A copy of tokens/ in a temp dir, with one value overridden."""

    def __init__(self, theme: str, path: list[str], value: str | None):
        self.theme, self.path, self.value = theme, path, value

    def __enter__(self) -> Path:
        self.tmp = Path(tempfile.mkdtemp())
        for f in REAL_TOKENS.glob("*.json"):
            shutil.copy(f, self.tmp)
        file = self.tmp / f"color.{self.theme}.json"
        data = json.loads(file.read_text(encoding="utf-8"))
        node = data["color"]
        for key in self.path[:-1]:
            node = node[key]
        if self.value is None:
            del node[self.path[-1]]
        else:
            node[self.path[-1]]["$value"] = self.value
        file.write_text(json.dumps(data), encoding="utf-8")
        return self.tmp

    def __exit__(self, *exc: object) -> None:
        shutil.rmtree(self.tmp)


class ContrastMath(unittest.TestCase):
    def test_black_on_white_is_21(self):
        self.assertAlmostEqual(cc.contrast("#000000", "#FFFFFF"), 21.0, places=2)

    def test_same_color_is_1(self):
        self.assertAlmostEqual(cc.contrast("#2F5BD3", "#2F5BD3"), 1.0, places=6)

    def test_known_pair(self):
        # #767676 on white is the classic 4.54:1 AA threshold color.
        self.assertAlmostEqual(cc.contrast("#767676", "#FFFFFF"), 4.54, places=2)


class RealTokens(unittest.TestCase):
    def test_all_24_combinations_pass(self):
        code, out = run(REAL_TOKENS)
        self.assertEqual(code, 0, out)
        self.assertIn("24 combinations checked, 0 failed.", out)


class Failures(unittest.TestCase):
    def test_low_contrast_accent_fails(self):
        # Fixture: the Dark theme's light-blue accent text used in the Light theme.
        with FixtureTokens("light", ["accent", "blue", "text"], "#8AA8F8") as tokens:
            code, out = run(tokens)
        self.assertEqual(code, 1, out)
        self.assertIn("FAIL light  x blue", out)
        self.assertIn("1 failed.", out)

    def test_invisible_focus_ring_fails(self):
        with FixtureTokens("amoled", ["accent", "violet", "focus"], "#1A1A1C") as tokens:
            code, out = run(tokens)
        self.assertEqual(code, 1, out)
        self.assertIn("focus ring", out)

    def test_weak_border_strong_fails_every_accent_in_that_theme(self):
        with FixtureTokens("dark", ["border", "strong"], "#303236") as tokens:
            code, out = run(tokens)
        self.assertEqual(code, 1, out)
        self.assertIn("8 failed.", out)

    def test_missing_token_is_an_error(self):
        with FixtureTokens("dark", ["accent", "teal", "tint"], None) as tokens:
            code, out = run(tokens)
        self.assertEqual(code, 2, out)
        self.assertIn("accent.teal.tint", out)

    def test_non_hex_value_is_an_error(self):
        with FixtureTokens("light", ["surface", "canvas"], "white") as tokens:
            code, out = run(tokens)
        self.assertEqual(code, 2, out)


if __name__ == "__main__":
    unittest.main()
