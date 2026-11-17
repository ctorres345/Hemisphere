---
status: accepted
---

# Trash uses the freedesktop.org format on each volume; Room is only a cache

Each volume with Trash (architecture §5) has `.Trash-hemisphere/files/` and `.Trash-hemisphere/info/`. Trashing
writes `info/<id>.trashinfo`, creates `files/<id>/`, then moves the entry into that folder with
its own name, in one move. The id is generated. The info file holds the original path relative
to the volume root, the deletion date and a format version. The info files are the source of truth; Room only indexes
them for the Trash screen.

## Considered options

- **Room as the source of truth.** Simpler queries, but the index is lost when the app is
  reinstalled or its data cleared, and an SD card moved to another device has trashed files with
  no way to restore them. Rejected.

- **Files named after the entry** (the freedesktop default). Long names pass the 255-character
  limit once `.trashinfo` is added, and "rename if taken" needs a second, non-atomic step on
  SAF. Rejected.
- **Files named by id directly in `files/`** (`files/<id>`). On SAF, `moveDocument` keeps the
  name, so the entry needs a rename after the move; a crash between the two leaves an
  original-named entry that blocks the next one with that name. Rejected for the folder per id.

## Consequences

- The format carries `X-Hemisphere-Version`, so a later change can be read alongside old trash
  on cards that were never updated.
- A path relative to the volume root restores correctly when an SD card moves to another device.
- Crash repair is simple: an info file whose id folder is missing or empty is deleted.
- When a volume mounts, the app rescans its `info/` folder and updates the cache.
- A `.nomedia` file keeps trashed media out of gallery apps.
- Desktop Linux file managers do not read this folder (the spec names it `.Trash-<uid>`), so the
  folder name is ours; only the file format is shared.
