---
status: proposed
---

# All-files access on Android 11+, legacy storage permission on Android 8 to 10

A file manager must read and write any folder the user picks, quickly and in bulk. On Android 11+
we request `MANAGE_EXTERNAL_STORAGE` (all-files access). On Android 8 and 9 we request
`READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE`; on Android 10 the same plus
`requestLegacyExternalStorage`. On Android 8 to 10, writes to an SD card or OTG drive go through
SAF, because the legacy permission does not cover removable volumes.

## Considered options

- **SAF only.** Works with no special permission and passes Play review easily, but every volume
  needs a folder grant, listing is several times slower, and some paths are unreachable. Rejected:
  too slow and clumsy for a file manager's core job.
- **MediaStore only.** Sees only media files. Rejected.

## Consequences

- Google Play requires a permission declaration for all-files access. File managers are an
  allowed use case. The declaration is filed when the Play listing is made (ADR 0003).
- If the user denies the permission, the app offers SAF folder grants as a fallback, through
  `SafProvider`.
- `Android/data` and `Android/obb` stay unreachable on Android 11+. The app shows them as locked.
