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
| VIA-selection-2d688562 | selection | low | fixed | docs/prototype/index.html\:128 | Neighbouring selected rows merge into one block with no divider. |
| VIA-settings-b4ac2aea | settings | med | fixed | docs/prototype/index.html\:159 | Focus on a theme chip is hard to tell from the selected chip. |
<!-- via:ledger:end -->
