---
status: accepted
---

# Everything outside `core:fs` uses `FsPath` and `FileSystemProvider`, never `File` or URIs

Hemisphere will browse local storage, SAF trees, and later SMB, SFTP, WebDAV and cloud drives.
If features use `java.io.File`, path strings or `content://` URIs, each new backend means a
rewrite. So `core:fs` exposes one path type (a stable `VolumeId`, name segments, and an optional
opaque `ProviderRef` for backends that find entries by id, with equality by an `EntryKey`), one provider interface with
`suspend` calls, streamed listings and error values, and a capabilities set that features check
instead of the provider type.

## Considered options

- **Names only** (volume plus segments). Simplest, but SAF must resolve a document id for every
  segment on every call, and cloud drives allow two entries with the same name in one folder.
  The path's string form is stored in `state.db`, `user.db` and saved state, so adding the id
  later would be a migration of three stores. Rejected.

## Consequences

- Features cannot use "fast path" tricks such as `File.listFiles()`; any speed-up goes inside a
  provider.
- Every operation has an `InMemoryProvider` test double, so `core:jobs`, use cases and
  ViewModels are tested on the JVM.
- The path's string form carries a version tag, and the parser reads every older version. It is
  for storage only; lookups use the `EntryKey`, stored in its own column.
- On name-addressed providers `ProviderRef` is only a hint, so a stale one never loses a file.
- `FileSystemProvider` has no Android types; `shareableUri` lives on a separate
  `ShareUriProvider` in `core:platform`.
- Network capabilities and errors (`serverCopy`, `setTimes`, `AuthRequired`, `Offline`,
  `Timeout`) are in the interface from the start, so v1.x providers do not change it.
