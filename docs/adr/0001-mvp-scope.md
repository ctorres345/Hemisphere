---
status: proposed
---

# The MVP is a complete local file manager with one location per pane

The MVP lands on `main` as one squash-merge, so it must be small enough to finish and useful
enough to install. We keep everything that makes Hemisphere different (Single and Dual, full
gamepad support, the three themes) and everything needed to manage files safely (the queue,
conflicts, Trash with Undo). We leave out features that add screens but not core value: tabs,
Grid and Gallery, search, bookmarks, archives, viewers, properties, remapping and Material You.
The full in/out list is in `docs/architecture.md` section 1.

## Consequences

- Tabs are out, but the pane code keeps its location behind one `PaneLocation` type so tabs can
  be added without changing callers.
- Archives are out, so ADR 0004 is decided now but built after the MVP.
