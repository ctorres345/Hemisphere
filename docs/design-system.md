# Hemisphere design system — v0 spec

Status: draft from the design grill on 2026-11-16. Store name: **Hemisphere Explorer**.

Hemisphere is an open-source Android file manager. It aims to cover what Solid Explorer and
MiXplorer do, with a better UI and first-class support for Android gaming handhelds.

> Two halves, one mind: one pane on a phone, two on a handheld.

---

## 1. Platform and stack

| Decision | Value |
|---|---|
| Platform | Android only |
| Language and UI | Kotlin + Jetpack Compose |
| minSdk | 26 |
| Components | Material 3 components under our own token layer (must not look like stock Material) |
| App license | GPLv3 (can reuse code from Material Files and Amaze) |
| Tokens and docs license | MIT |

Note: MiXplorer is closed source. Don't copy its code or exact UI.

---

## 2. Layout model: Single and Dual

```
Layout mode: [ Auto ] [ Single ] [ Dual ]

Auto:  window width ≥ 600dp → Dual
       window width <  600dp → Single
```

- Auto uses the Compose window size class.
- Each device can pin a mode, separately for **portrait** and **landscape**.
- The pin is stored per device, so one backup restores correctly on a phone and on a handheld.
- A toolbar toggle switches the mode; on gamepad it's the Select button.
- In Dual, each pane has its own view mode, tabs, and back stack.

---

## 3. Input model

Touch and gamepad/D-pad are equal, first-class input methods.

### Default gamepad mapping (remappable)

| Button | Action |
|---|---|
| Ⓐ | Open |
| Ⓑ | Back / up one level / leave selection |
| Ⓧ | Context menu |
| Ⓨ | Toggle selection (hold + D-pad: range) |
| L1 / R1 | Switch pane |
| L2 / R2 | Previous / next tab |
| Right stick | Page up / down |
| Start | Actions menu |
| Select | Layout mode |

Buttons bind to Android `KEYCODE_BUTTON_*` codes. The right stick and analog triggers arrive as
`MotionEvent` axes (`AXIS_Z`/`AXIS_RZ`, `AXIS_LTRIGGER`/`AXIS_RTRIGGER` or `AXIS_BRAKE`/`AXIS_GAS`),
so they bind by axis with a threshold, falling back to `KEYCODE_BUTTON_L2`/`R2` where a device
sends keys.

Handhelds differ in face-button layout, so a **Face buttons** setting (Auto / Xbox / Nintendo)
swaps A↔B and X↔Y, and the hint footer glyphs follow it. Android doesn't report the layout, so
Auto is a heuristic: a known-device table keyed by `InputDevice` vendor/product ID or
`Build.MODEL`, defaulting to Xbox.

### Input-aware UI

- The app tracks the **last input used**.
  - Touch: hide the focus ring and button hints, use touch density.
  - Gamepad: show the focus ring and the button-hint footer, use compact density.
- **Focus ring**: always visible in gamepad mode, in every theme × accent combination.
- **Active pane**: an accent bar at the top of the pane header. The inactive pane steps its text
  down one token (`text.primary` → `text.secondary`, `text.secondary` → `text.tertiary`) instead
  of using opacity, and those pairs are in the contrast matrix.
- **Button-hint footer**: `Ⓐ Open  Ⓑ Back  Ⓧ Menu  Ⓨ Mark  L1/R1 Pane`. Ⓨ is labeled "Mark" so it can't
  be confused with the physical Select button (layout mode).

---

## 4. Visual direction

**Direction A, calm utility.** Neutral surfaces, one accent, dense rows, borders instead of shadows.
Color shows meaning (selection, focus, file type, danger) and is never decoration.

Follow-up: before v1, run a polish pass (`/via audit`, `ai-design-fix`) on real screens to
remove anything that looks generated.

---

## 5. Color

### Themes

| Theme | Use |
|---|---|
| Light | Default when the system is light |
| Dark | Default when the system is dark |
| AMOLED | True black `#000000` canvas for OLED phones and handhelds |

### Surfaces (3 elevation steps + border)

| Token | Light | Dark | AMOLED |
|---|---|---|---|
| `surface.canvas` | `#F7F7F8` | `#111214` | `#000000` |
| `surface.raised` | `#FFFFFF` | `#18191C` | `#0B0B0C` |
| `surface.overlay` | `#FFFFFF` | `#202226` | `#141416` |
| `border.default` | `#E3E4E7` | `#26282C` | `#1E1F22` |
| `text.primary` | `#16171A` | `#E9E9EB` | `#E9E9EB` |
| `text.secondary` | `#5E6168` | `#A3A5AA` | `#A3A5AA` |
| `text.tertiary` | `#6E7075` | `#898B90` | `#7E8085` |
| `border.strong` | `#8A8C91` | `#6C6E73` | `#626469` |

Values are provisional and must pass the contrast check in CI (section 11).

- `border.default` is decorative only (row dividers, pane split). Anything whose boundary is the
  only way to see it (text fields, checkboxes, toggles) uses `border.strong` at ≥ 3:1 against its
  surface (WCAG 1.4.11).

### Accent

- Users pick **1 of 8 curated accents**: blue, teal, green, amber, orange, red, pink, violet.
- Each accent defines `accent.fill`, `accent.onFill`, `accent.tint`, `accent.focus`, and `accent.text`
  for all three themes.
- 3 themes × 8 accents = **24 combinations**, all checked for contrast in CI.
- **Material You** (wallpaper color) is an optional toggle, off by default, available on Android 12+
  (API 31) and hidden below. Its color is checked at runtime, and if it fails contrast the nearest
  curated accent is used instead.
- The accent is used only for selection, focus, the active pane, and the one primary action.

### Semantic colors (independent of the accent)

| Role | Use |
|---|---|
| `danger` | Permanent delete, errors |
| `warning` | Low storage, root warnings |
| `success` | Operation finished |

Semantic states never rely on color alone: they always carry an icon and a text label. Selection
always shows a check mark. So a red, green, or amber accent can't be mistaken for danger, success,
or warning.

### File-type colors (icons only, muted, fixed in v1)

folder · image · video · audio · archive · APK · ROM/ISO · document · code · other

---

## 6. Typography

| Token | Size | Weight | Use |
|---|---|---|---|
| `title` | 20sp | 500 | Screen and dialog titles |
| `heading` | 16sp | 500 | Pane headers, section labels |
| `body` | 14sp | 400 | File names |
| `meta` | 12sp | 400 | Size, date, counts (tabular numbers) |
| `caption` | 11sp | 400 | Button hints, badges |

- **Inter** (variable, OFL) for all UI, with `tnum` on numbers.
- **JetBrains Mono** (OFL) only for permissions, hashes, and paths in the editor.
- Two weights only: 400 and 500.
- All sizes in `sp`, so they respect the system font size.
- Long filenames are cut in the **middle**: `super_mario_wor…(USA).zip`.

---

## 7. Spacing, shape, density

```
space:  4 · 8 · 12 · 16 · 24 · 32   (4dp grid)
radius: 4 chips, badges · 8 focused rows, inputs · 12 sheets, dialogs
```

| Density | Row | Default for | Content |
|---|---|---|---|
| Comfortable | 56dp | Opt-in | Two lines: name / size · date |
| Default | 48dp | Touch | One line + size or count on the right |
| Compact | 40dp | Gamepad | One line, smaller icon |

Touch targets never go below 48dp in touch mode.

---

## 8. View modes

| View | Content |
|---|---|
| List | Dense rows; sortable columns (name · size · date) on wide screens |
| Grid | Icon tiles, 3–6 columns by width |
| Gallery | Large thumbnails; default for DCIM, Pictures, and Movies |

- The view is **remembered per folder**, falling back to the global default.
- In Dual, each pane has its own view.

---

## 9. Navigation structure

```
PHONE (Single)                         HANDHELD (Dual)
┌──────────────────────┐               ┌──┬───────────────┬───────────────┐
│ ☰  Internal  ⌕  ⋮    │               │▣ │ [Int][ROMs]+  │ [SD]+         │
│ / › Download › ROMs  │               │★ │ / › Download  │ / › ROMs › GBA│
│ ─ file rows ───────  │               │⟲ │ ─ rows ─────  │ ─ rows ─────  │
│                  (+) │               │⚙ │               │               │
└──────────────────────┘               └──┴───────────────┴───────────────┘
```

| Piece | Behavior |
|---|---|
| Places | A drawer on phone; a collapsible 56dp icon rail in Dual that you reach with the D-pad |
| Breadcrumbs | Per pane; scroll sideways; tap to jump; long-press to copy the path |
| Tabs | Per pane, compact chips, up to 8; L2/R2 to switch |
| Back stack | One per tab; system back follows the active tab |
| Search | Scoped to the current folder; a chip widens it to all storage |

---

## 10. Selection, clipboard, and operations

| Input | Start | Range | End |
|---|---|---|---|
| Touch | Long-press | Long-press a second item | Back / ✕ |
| Gamepad | Ⓨ | Hold Ⓨ + D-pad | Ⓑ |

- The selection header shows **"3 selected · 1.2 GB"**.
- Action bar: at the bottom on phone, in the active pane's header in Dual.
- **Dual**: one-step **Copy → other pane** and **Move → other pane**, without the clipboard.
- **Single**: Copy or Cut puts items in the **clipboard tray**, which stays at the bottom across
  tabs and places: `⧉ 3 items · 1.2 GB  [Paste here]  ✕`.
- Drag and drop between panes on touch.
- **Conflicts**: one sheet with Skip / Replace / Keep both + "apply to all", comparing size
  and date side by side.
- **Queue**: copy and move run in the background, with a progress chip in the header
  (tap it to see the list, pause, or cancel) and a system notification.
- **Trash**: delete goes to Trash (30-day retention, adjustable). Each writable volume (internal,
  SD card, USB OTG) has its own hidden trash folder, so trashing is a rename, never a cross-volume
  copy. Where that isn't possible (read-only or SAF-limited volumes, network shares), delete is
  permanent: the dialog says so, uses `danger`, and offers no Undo. Elsewhere a snackbar offers
  Undo for delete, move, and rename.

---

## 11. Source of truth and tooling

```
tokens/*.json  (W3C Design Tokens format)
      ├──► generated Kotlin: HemisphereTheme
      ├──► debug component catalog + @Previews
      │      └──► Roborazzi screenshot tests:
      │           component × {light, dark, amoled} × {phone, handheld} × {touch, gamepad}
      ├──► CI contrast check: 24 theme × accent combinations (WCAG AA; focus ring ≥ 3:1)
      └──► docs site on GitHub Pages
```

No Figma for now. Throwaway HTML mockups are fine for exploring.

---

## 12. Release scope (the design system covers all of it now)

| Release | Features |
|---|---|
| **v1: local** | Internal storage, SD card, USB OTG · Single/Dual + tabs · List/Grid/Gallery · selection, queue, Trash · search · bookmarks · archives (zip/7z/tar create; rar extract) · image viewer · text editor · properties (permissions, hashes) · themes and accents · full gamepad support |
| **v1.x: network** | SMB, SFTP, FTP, WebDAV · storage analyzer · app manager (APK backup and install) |
| **v2: power** | Cloud (Drive, Dropbox, OneDrive) · root · encrypted folders · media player · plugins |

---

## 13. Component inventory (to design)

**Shell**: app bar · places drawer · places rail · pane header with breadcrumbs · tab chips ·
layout-mode toggle · button-hint footer

**Content**: file row (3 densities) · grid tile · gallery tile · column header (sortable) ·
empty state · loading skeleton · error state

**Selection and operations**: selection header · action bar · clipboard tray · progress chip ·
queue sheet · conflict sheet · snackbar with Undo · confirm dialog (danger)

**Forms and sheets**: context menu · properties sheet · rename dialog · new file/folder dialog ·
sort and view sheet · search field + scope chip · settings rows · accent picker · theme picker

**Later tiers**: network connection form · cloud auth sheet · root warning · password/encryption
prompt · storage analyzer chart · app list row

**Focus states**: every interactive component has a defined gamepad focus state.

---

## Open items

- Name: no file manager called Hemisphere on Play, F-Droid or GitHub (checked 2026-11-16).
  Bare "Hemisphere" is crowded (Hemisphere GNSS trademarks, Hemisphere Games), so the store
  name is **Hemisphere Explorer** (in-app and icon: "Hemisphere") and the package ID is `io.github.<owner>.hemisphere`. Do a
  USPTO/EUIPO search before a Play Store launch.
- Mascot and icon: two half-circles with Via the cat curled between them.
- Final hex values for the 8 accents, after the contrast script exists.
