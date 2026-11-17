# Design decisions and Via ledger

<!-- via:ledger:start -->
```json
{
  "findings": [
    {
      "id": "VIA-conflict-sheet-ecb4136b",
      "status": "fixed",
      "first_seen": "2026-11-17T11:26:25",
      "screen": "conflict-sheet",
      "rule": "focus-visible",
      "severity": "high",
      "file": "docs/prototype/index.html",
      "line": 154,
      "issue": "In gamepad mode the focus ring on Keep both is the same color as its fill, so focus is invisible.",
      "last_seen": "2026-11-17T11:26:25",
      "run": "20261117T162142Z",
      "fixed_in": "20261117T170556Z"
    },
    {
      "id": "VIA-dual-000ea22d",
      "status": "fixed",
      "first_seen": "2026-11-17T11:26:25",
      "screen": "dual",
      "rule": "middle-cut",
      "severity": "low",
      "file": "docs/prototype/index.html",
      "line": 409,
      "issue": "The middle cut can leave a fragment such as \"e)\" at the end of a name.",
      "last_seen": "2026-11-17T11:26:25",
      "run": "20261117T162142Z",
      "fixed_in": "20261117T170556Z"
    },
    {
      "id": "VIA-dual-6eb3e438",
      "status": "fixed",
      "first_seen": "2026-11-17T11:26:25",
      "screen": "dual",
      "rule": "active-pane",
      "severity": "med",
      "file": "docs/prototype/index.html",
      "line": 191,
      "issue": "The inactive pane's selected tab chip looks the same as the active pane's, so it is unclear which pane is active.",
      "last_seen": "2026-11-17T11:26:25",
      "run": "20261117T162142Z",
      "fixed_in": "20261117T170556Z"
    },
    {
      "id": "VIA-dual-b8efbe00",
      "status": "fixed",
      "first_seen": "2026-11-17T11:26:25",
      "screen": "dual",
      "rule": "text-size",
      "severity": "low",
      "file": "docs/prototype/index.html",
      "line": 199,
      "issue": "Hint footer text (11sp) is small on a handheld.",
      "last_seen": "2026-11-17T11:26:25",
      "run": "20261117T162142Z",
      "fixed_in": "20261117T170556Z"
    },
    {
      "id": "VIA-prototype-b034222a",
      "status": "open",
      "first_seen": "2026-11-17T15:42:37",
      "screen": "prototype",
      "rule": "narrow-viewport",
      "severity": "med",
      "file": "docs/prototype/index.html",
      "line": 99,
      "issue": "At 390px wide the device frame runs past the viewport, cutting the meta column and status bar. Pre-existing prototype harness layout.",
      "last_seen": "2026-11-17T15:51:17",
      "run": "20261117T205052Z",
      "ticket": "docs/design/tickets/prototype-harness.md"
    },
    {
      "id": "VIA-prototype-bd847faf",
      "status": "open",
      "first_seen": "2026-11-17T15:42:37",
      "screen": "prototype",
      "rule": "legend-glyphs",
      "severity": "low",
      "file": "docs/prototype/index.html",
      "line": 859,
      "issue": "Button glyphs in the notes panel legend are small.",
      "last_seen": "2026-11-17T15:51:17",
      "run": "20261117T205052Z",
      "ticket": "docs/design/tickets/prototype-harness.md"
    },
    {
      "id": "VIA-prototype-df10c7fd",
      "status": "fixed",
      "first_seen": "2026-11-17T15:42:37",
      "screen": "prototype",
      "rule": "coverage",
      "severity": "high",
      "file": "docs/prototype/index.html",
      "line": 864,
      "issue": "The first shots showed only Phone · Single, so the changed selected-row text and accents could not be judged.",
      "last_seen": "2026-11-17T15:42:37",
      "run": "20261117T203820Z",
      "fixed_in": "20261117T205052Z"
    },
    {
      "id": "VIA-prototype-ebd9144a",
      "status": "open",
      "first_seen": "2026-11-17T15:42:37",
      "screen": "prototype",
      "rule": "middle-cut-gap",
      "severity": "low",
      "file": "docs/prototype/index.html",
      "line": 441,
      "issue": "The middle cut leaves a space after the ellipsis (\"th…  (USA\").",
      "last_seen": "2026-11-17T15:51:17",
      "run": "20261117T205052Z",
      "ticket": "docs/design/tickets/prototype.md"
    },
    {
      "id": "VIA-selection-2d688562",
      "status": "fixed",
      "first_seen": "2026-11-17T11:26:25",
      "screen": "selection",
      "rule": "selected-rows",
      "severity": "low",
      "file": "docs/prototype/index.html",
      "line": 128,
      "issue": "Neighbouring selected rows merge into one block with no divider.",
      "last_seen": "2026-11-17T11:26:25",
      "run": "20261117T162142Z",
      "fixed_in": "20261117T170556Z"
    },
    {
      "id": "VIA-settings-b4ac2aea",
      "status": "fixed",
      "first_seen": "2026-11-17T11:26:25",
      "screen": "settings",
      "rule": "focus-vs-selected",
      "severity": "med",
      "file": "docs/prototype/index.html",
      "line": 159,
      "issue": "Focus on a theme chip is hard to tell from the selected chip.",
      "last_seen": "2026-11-17T11:26:25",
      "run": "20261117T162142Z",
      "fixed_in": "20261117T170556Z"
    }
  ]
}
```

| ID | Screen | Severity | Status | Location | Issue |
|---|---|---|---|---|---|
| VIA-conflict-sheet-ecb4136b | conflict-sheet | high | fixed | docs/prototype/index.html\:154 | In gamepad mode the focus ring on Keep both is the same color as its fill, so focus is invisible. |
| VIA-dual-000ea22d | dual | low | fixed | docs/prototype/index.html\:409 | The middle cut can leave a fragment such as "e)" at the end of a name. |
| VIA-dual-6eb3e438 | dual | med | fixed | docs/prototype/index.html\:191 | The inactive pane's selected tab chip looks the same as the active pane's, so it is unclear which pane is active. |
| VIA-dual-b8efbe00 | dual | low | fixed | docs/prototype/index.html\:199 | Hint footer text (11sp) is small on a handheld. |
| VIA-prototype-b034222a | prototype | med | open | docs/prototype/index.html\:99 | At 390px wide the device frame runs past the viewport, cutting the meta column and status bar. Pre-existing prototype harness layout. |
| VIA-prototype-bd847faf | prototype | low | open | docs/prototype/index.html\:859 | Button glyphs in the notes panel legend are small. |
| VIA-prototype-df10c7fd | prototype | high | fixed | docs/prototype/index.html\:864 | The first shots showed only Phone · Single, so the changed selected-row text and accents could not be judged. |
| VIA-prototype-ebd9144a | prototype | low | open | docs/prototype/index.html\:441 | The middle cut leaves a space after the ellipsis ("th…  (USA"). |
| VIA-selection-2d688562 | selection | low | fixed | docs/prototype/index.html\:128 | Neighbouring selected rows merge into one block with no divider. |
| VIA-settings-b4ac2aea | settings | med | fixed | docs/prototype/index.html\:159 | Focus on a theme chip is hard to tell from the selected chip. |
<!-- via:ledger:end -->
