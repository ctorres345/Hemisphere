---
status: proposed
---

# All-files access on Android 11+, legacy storage permission on Android 8 to 10

A file manager must read and write any folder the user picks, quickly and in bulk. On Android 11+
we request `MANAGE_EXTERNAL_STORAGE` (all-files access). On Android 8 and 9 we request
`READ_EXTERNAL_STORAGE` and `WRITE_EXTERNAL_STORAGE`; on Android 10 the same plus
`requestLegacyExternalStorage`. On Android 8 to 10, SD cards and USB OTG drives go through SAF,
because the legacy permission cannot write removable volumes. On Android 11+, a USB OTG drive
also goes through SAF when the system does not expose it as a readable path.
The provider for each volume is chosen when it mounts (architecture §3).

## Considered options

- **SAF only.** Works with no special permission and passes Play review easily, but every volume
  needs a folder grant, listing is several times slower, and some paths are unreachable. Rejected:
  too slow and clumsy for a file manager's core job.
- **MediaStore only.** Sees only media files. Rejected.

## Consequences

- Google Play requires a permission declaration for all-files access. File managers are an
  allowed use case. The declaration is filed when the Play listing is made (ADR 0003).
- If the user denies the permission, the app offers SAF folder grants as a fallback, through
  `SafProvider`. On Android 11+ SAF cannot grant the root of internal storage or `Download/`
  (and some devices block an SD card root), so in fallback mode internal storage has no Trash:
  delete there is permanent, with the `danger` dialog. Removable volumes keep Trash when their
  root grant succeeds (architecture §3). A folder granted by hand
  never gets Trash (architecture §3).
- `Android/data` and `Android/obb` stay unreachable on Android 11+. The app shows them as locked.
