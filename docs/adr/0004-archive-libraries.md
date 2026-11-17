---
status: proposed
---

# Apache Commons Compress for zip, 7z and tar; libarchive for reading RAR; no unRAR

The app is GPL-3.0-or-later, so every library must be GPLv3-compatible. Apache Commons Compress
(Apache 2.0) creates and extracts zip, 7z and tar (7z uses XZ for Java, public domain).
libarchive (BSD-2-Clause) reads RAR4 and RAR5 and is used only for RAR extraction, through a
small JNI wrapper in `core:archive`.

## Considered options

- **RARLAB unRAR source, and wrappers under its license (such as junrar).** The unRAR license
  forbids using the code to recreate the RAR compression algorithm. The FSF lists it as non-free
  and GPL-incompatible. Rejected: we could not ship it in a GPL app, and RAR support would be
  dropped after release.
- **7-Zip / p7zip native code.** Mostly LGPL, but its RAR code carries the unRAR restriction.
  Rejected for the same reason.

## Consequences

- RAR is extract-only. Creating RAR files is not possible with any free library.
- libarchive is native code: the scaffold for `core:archive` (after the MVP) builds it with the
  NDK for every ABI we ship, and its version is tracked by Dependabot or a manual check.
