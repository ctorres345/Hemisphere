---
status: proposed
---

# Trash uses the freedesktop.org format on each volume; Room is only a cache

Each writable volume has `.Trash-hemisphere/files/` and `.Trash-hemisphere/info/`. Trashing is a
rename into `files/` plus a `.trashinfo` file with the original path and deletion date. The info
files are the source of truth; Room only indexes them for the Trash screen.

## Considered options

- **Room as the source of truth.** Simpler queries, but the index is lost when the app is
  reinstalled or its data cleared, and an SD card moved to another device has trashed files with
  no way to restore them. Rejected.

## Consequences

- When a volume mounts, the app rescans its `info/` folder and updates the cache.
- A `.nomedia` file keeps trashed media out of gallery apps.
- Desktop Linux file managers do not read this folder (the spec names it `.Trash-<uid>`), so the
  folder name is ours; only the file format is shared.
