---
status: proposed
---

# Everything outside `core:fs` uses `FsPath` and `FileSystemProvider`, never `File` or URIs

Hemisphere will browse local storage, SAF trees, and later SMB, SFTP, WebDAV and cloud drives.
If features use `java.io.File`, path strings or `content://` URIs, each new backend means a
rewrite. So `core:fs` exposes one path type (volume plus name segments), one provider interface
with `suspend` calls and error values, and a capabilities set that features check instead of the
provider type.

## Consequences

- Features cannot use "fast path" tricks such as `File.listFiles()`; any speed-up goes inside a
  provider.
- Every operation has an `InMemoryProvider` test double, so `core:jobs` and ViewModels are tested
  on the JVM.
