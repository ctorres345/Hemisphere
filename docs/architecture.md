# Hemisphere architecture

Status: **accepted** (HEM-2, approved by Cesar on 2026-11-17).

This is how the app is built. Read it before the scaffold and before every feature ticket.
The visual rules live in [`design-system.md`](design-system.md); the code rules that follow from
this file live in [`code-guidelines.md`](code-guidelines.md). Decisions that are hard to undo have
an ADR in [`adr/`](adr/). Domain words are in [`../GLOSSARY.md`](../GLOSSARY.md).

## 1. MVP scope

The MVP is what lands on `main` as the one squash-merge from `mvp`. The rest of v1 follows in
normal releases. See ADR [0001](adr/0001-mvp-scope.md).

| In the MVP | Out (rest of v1) |
| --- | --- |
| Browse internal storage, SD card and USB OTG | Tabs (one location per pane in the MVP) |
| Single and Dual layouts, Auto mode, per-orientation pin | Grid and Gallery views |
| List view, sort by name, size, date | Search |
| Places drawer (phone) and rail (Dual) | Bookmarks |
| Breadcrumbs | Archives (create and extract) |
| Selection (touch and gamepad) | Image viewer, text editor |
| Copy, move, rename, new folder | Properties sheet, permissions, hashes |
| Copy or move to the other pane (Dual), clipboard tray (Single) | Button remapping |
| Job queue with progress, pause, cancel, notification | Material You accent |
| Conflict sheet (Skip, Replace, Keep both, apply to all) | Drag and drop between panes |
| Delete to Trash, Undo, restore, empty Trash | Comfortable density |
| Full gamepad support with the default mapping and Face buttons setting | |
| Light, Dark, AMOLED and the 8 accents | |

Network shares (v1.x) and cloud, root and plugins (v2) are out of v1 entirely.

## 2. Modules

Three layers, as in Google's guide to app architecture: UI, domain and data. Each arrow means
"depends on". Arrows only point down.

```
                          ┌──────────┐
                          │   app    │  Activity, DI root, nav host, job service
                          └────┬─────┘
        ┌──────────────┬───────┴───────┬────────────────┐
        ▼              ▼               ▼                ▼
 feature:browser  feature:jobs  feature:settings  feature:trash     UI layer
        └──────────────┴───────┬───────┴────────────────┘           (screens, ViewModels)
                               │  (features also use core:designsystem, core:input, core:mvi,
                               ▼   core:platform; simplified, the table below is authoritative)
                         ┌───────────┐
                         │core:domain│                              Domain layer
                         └─────┬─────┘                              (use cases)
               ┌───────────────┼───────────────┐   (implementation, hidden from features)
               ▼               ▼               ▼
           core:jobs ───► core:data ───► core:fs                    Data layer
               └───────────────────────────► core:fs
                                               │
             every module above ──api──► core:model                 Shared value types
```

| Module | Owns | Depends on |
| --- | --- | --- |
| `app` | `MainActivity`, Hilt root, top-level Navigation 3 host, the job service, manifest permissions | every module |
| `core:model` | Plain Kotlin value types shared by every layer: `VolumeId`, `FsPath`, `EntryKey`, `Entry`, `Listing`, `FsResult`, `FsError`, `Capabilities`, job states | nothing (a pure Kotlin/JVM module) |
| `core:designsystem` | Tokens generated from `tokens/*.json`, `HemisphereTheme`, shared components, the focus ring, `InputMode` and `LocalInputMode`, button-hint glyphs | nothing in the app |
| `core:input` | Last-input tracking (touch or gamepad), the gamepad map, Face buttons setting, key and axis handling. It sets the value of `LocalInputMode`. | `core:designsystem`, `core:data` |
| `core:platform` | Small Android-side interfaces a screen needs directly: `ShareUriProvider` (section 3) | `core:model` |
| `core:mvi` | The shared strict-MVI base (section 6.1): `MviViewModel` and the `Reducer` type. No Compose. | nothing in the app |
| `core:domain` | Use cases (section 6.2) and the domain models that combine data from several sources, such as a folder listing with its saved sort | `core:model` (`api`); `core:fs`, `core:jobs`, `core:data` (`implementation`) |
| `core:fs` | The file-system abstraction (section 3), volumes, the local and SAF providers, MediaStore scans; implements `ShareUriProvider` | `core:model`, `core:platform`; `core:fs-testing` (tests only) |
| `core:jobs` | The job queue (section 4), conflicts, Trash (section 5), and the `state.db` Room database that stores them | `core:fs`, `core:data` |
| `core:data` | The `user.db` Room database, DataStore settings, their repositories, the `VolumeStore` implementation | `core:fs` |
| `core:fs-testing` | `InMemoryProvider`, `FakeVolumeRepository` and the abstract `FileSystemProviderContract` suite every provider must pass; used by test source sets only | `core:model`, `core:fs` |
| `core:testing` | Test fakes for every data-layer interface (`FakeJobQueue`, fake repositories) and `core:fs-testing` re-exported, used by test source sets only | `core:fs-testing`, `core:fs`, `core:jobs`, `core:data` |
| `feature:browser` | The Browser screen: shell (Single and Dual), panes, file list, selection, clipboard tray | `core:domain`, `core:designsystem`, `core:input`, `core:platform`, `core:mvi` |
| `feature:jobs` | Progress chip, queue sheet, conflict sheet. `app` shows them above every screen (Browser, Trash, Settings), so a conflict can be answered from anywhere. | `core:domain`, `core:designsystem`, `core:input`, `core:mvi` |
| `feature:settings` | Settings screens (theme, accent, layout pin, Face buttons, Trash retention) | `core:domain`, `core:designsystem`, `core:input`, `core:mvi` |
| `feature:trash` | The Trash screen: list, restore, empty | `core:domain`, `core:designsystem`, `core:input`, `core:mvi` |

Rules:

- Packages follow modules: `io.github.ctorres345.hemisphere.core.fs`,
  `io.github.ctorres345.hemisphere.feature.browser`, and so on.
- A `feature:*` module never depends on another `feature:*` module. Features talk through
  `app` (navigation keys) or through a `core:*` module.
- `core:*` modules never depend on `feature:*`.
- A `feature:*` module depends on `core:domain`, `core:designsystem`, `core:input`, `core:mvi` and (for
  sharing) `core:platform`, never on `core:fs`, `core:jobs` or `core:data`. `core:domain` takes
  the data modules as `implementation` and exposes only `core:model` as `api`, so providers, the
  `JobQueue` and repositories are not reached through `core:domain`. A build file could still
  declare one directly, so the scaffold adds a module-graph check task: it lists the allowed
  dependencies per module type and fails the build on any other edge, including Compose in
  `core:domain`, `core:fs`, `core:jobs` or `core:data`. It also fails when an edge these rules
  require is missing: `core:model` as `api` from the data layer, `core:platform` and
  `core:domain`; the data layer as `implementation` from `core:domain`; and the data layer as
  `api` from `core:testing`, which also exposes `core:fs-testing` as `api`. The table lists what a module may use, so an edge it lists that a
  module does not use yet is not required.
- The convention plugins give every module's tests JUnit and `kotlinx-coroutines-test`, and every
  Android module's tests `core:testing` (`testImplementation`), so build files do not list them.
  `core:testing` and the modules it is built from (`core:model`, `core:fs`, `core:fs-testing`,
  `core:jobs`, `core:data`, `core:platform`) do not get `core:testing`; the module-graph check fails
  if one does. `core:testing` declares `core:fs`, `core:fs-testing`, `core:jobs` and `core:data` as `api`, so test
  code (and only test code) gets the fakes and the data interfaces needed to build real use cases over them.
- `core:fs-testing` lets the data layer test against `InMemoryProvider` without `core:testing`:
  `core:fs`, `core:jobs` and `core:data` may declare it as `testImplementation` (and only that). It
  depends on `core:model` and `core:fs` and nothing else, so it never loops with them: `core:fs`'s
  tests use it on top of `core:fs`'s main code, where `LocalProvider` (internal) runs the contract
  suite.
- `core:domain`, `core:fs`, `core:jobs`, `core:data` and `core:mvi` have no Compose dependency. They are
  tested on the JVM. `core:domain` is an Android library module (its dependencies are), but its
  own code imports nothing from `android.*`.
- `core:input` is UI infrastructure, not a screen: it may read the Face buttons setting from
  `core:data` directly.
- New features (search, viewer, editor, archives) arrive as new `feature:*` modules. Archive
  formats arrive as a `core:archive` module (see ADR [0004](adr/0004-archive-libraries.md)),
  a data-layer module that depends on `core:model`, `core:fs` and `core:jobs`; the module-graph
  check lists it when it is added.

## 3. `core:fs`: the file-system abstraction

The rest of the app never sees `java.io.File`, a raw path string, or a `content://` URI. It sees
`FsPath` and a `FileSystemProvider`. The one exception is `ShareUriProvider.shareableUri`, which hands a URI
straight to an `Intent` for another app (open with, share) and is never parsed or stored. This is what lets SMB, SFTP and cloud plug in later without a
rewrite. See ADR [0006](adr/0006-file-system-abstraction.md).

```kotlin
// Sketch, not final code. The value types live in core:model, the interfaces in core:fs.
@JvmInline value class VolumeId(val value: String)    // stable identity, see "Identity" below
@JvmInline value class ProviderRef(val value: String) // opaque: SAF document ID, later a cloud file ID

class FsPath(                     // not a data class: equality is by key, not by every field
    val volume: VolumeId,
    val segments: List<String>,   // names, for display, breadcrumbs and conflict checks
    val ref: ProviderRef? = null, // a lookup hint; see "Identity"
) {
    val key: EntryKey             // what equals() and hashCode() use
}

// One child of a folder. It does not repeat the full path: the caller knows the parent.
data class Entry(val name: String, val ref: ProviderRef?, val kind: Kind, val size: Long?, val modified: Instant?)

// One emission of a listing: everything read so far, sorted, plus whether reading is done.
data class Listing(val entries: List<Entry>, val complete: Boolean, val error: FsError?)

interface FileSystemProvider {
    val capabilities: Capabilities  // idAddressed, move, randomAccess, resume, serverCopy, setTimes, watch, ...
    fun list(dir: FsPath): Flow<FsResult<List<Entry>>>             // raw batches; the flow ends when the folder is done
    suspend fun stat(path: FsPath): FsResult<Entry>
    suspend fun openRead(path: FsPath, offset: Long = 0): FsResult<Source>  // offset > 0 only if capabilities.resume
    suspend fun openRandomAccess(path: FsPath): FsResult<SeekableSource>    // only if capabilities.randomAccess; else use RandomAccessOpener
    suspend fun openWrite(path: FsPath, mode: WriteMode, sizeHint: Long? = null): FsResult<Sink> // WriteMode.Append only if capabilities.resume
    suspend fun createDirectory(path: FsPath): FsResult<Entry>
    suspend fun rename(path: FsPath, newName: String): FsResult<Entry>
    suspend fun move(from: FsPath, to: FsPath): FsResult<Entry>     // same volume, only if capabilities.move
    suspend fun copy(from: FsPath, to: FsPath): FsResult<Entry>     // same volume, server side, only if capabilities.serverCopy
    suspend fun setTimes(path: FsPath, modified: Instant): FsResult<Unit> // only if capabilities.setTimes
    suspend fun freeSpace(volume: VolumeId): FsResult<Long?>        // null when the backend cannot tell
    suspend fun delete(path: FsPath): FsResult<Unit>                // permanent
    fun observe(dir: FsPath): Flow<Unit>                            // change signal, may be a no-op
}

// In core:platform, kept out of FileSystemProvider so that interface stays plain Kotlin.
interface ShareUriProvider {
    suspend fun shareableUri(path: FsPath): FsResult<Uri>  // FileProvider or SAF URI, for Intents only
}

// In core:fs: private scratch space on internal storage, for work that must seek.
interface ScratchSpace {
    suspend fun create(sizeHint: Long?): FsResult<ScratchFile>  // NoSpace if internal storage cannot fit it
}
interface ScratchFile : AutoCloseable {   // close() deletes the file
    fun channel(): SeekableByteChannel    // java.nio.channels: read, write and seek
    fun source(): Source                  // read it back from the start, to copy it out
}
```

- **Volume identity**: `VolumeId` is stable across mounts and reboots, because resuming a job
  and restoring from Trash depend on it. Internal storage is `primary`; an SD card or USB drive
  is its file-system UUID (`StorageVolume.getUuid()`); a folder granted by hand is its tree URI;
  a network share (v1.x) is its saved connection id. A removable volume with no UUID gets a
  random id saved in `VolumeStore`, matched by label and capacity, and the user confirms the
  match before a job resumes on it. FAT serials are 32 bits and can repeat across cards, so a
  resumed job also checks that its next source and target still look as recorded (size and
  modified time) and pauses with `VolumeUnavailable` if not.
- **Overlapping volumes**: a folder granted by hand that lies inside a volume the app already
  has (a folder on an SD card read by path on Android 8 to 10) does not become a second volume.
  It extends that volume's write access to the granted subtree, which still has no Trash. Only a
  grant outside every known volume becomes its own volume. One set of files never has two
  `VolumeId`s, so the per-volume job lock holds.
- **Entry identity**: an `EntryKey` is `(volume, ref)` on a provider with the `idAddressed`
  capability (cloud drives, v2), where two entries in one folder can share a name. On every
  other provider it is `(volume, segments)`, with each segment case-folded on a
  case-insensitive volume. `FsPath` equality, selection, scroll anchors, conflict checks and
  database lookups all use the key, never the string form.
- **`ref` is a hint** on name-addressed providers (local, SAF): the provider must be able to
  find the entry from `segments` alone, and falls back to them when the `ref` gives `NotFound`.
  A volume's provider can change between mounts (permission revoked, Android 10 to 11 upgrade),
  and SAF document ids on external storage change when a parent is renamed, so a saved `ref` may
  be stale. Only `idAddressed` providers rely on it.
- **Listing**: `ObserveFolderUseCase` turns the provider's batches into `Listing` emissions: all
  entries read so far, sorted on `@DefaultDispatcher`, with `complete = false` until the last
  batch. A 10,000-entry folder paints its first rows before the rest are read. Rows keep their
  key, so gamepad focus stays on the same entry while later batches land. An error part-way
  through keeps the entries read so far, sets `error`, and the screen shows them with a Retry
  banner. Change signals from `observe` are debounced (300 ms) before a relist, so a copy into
  the open folder does not relist on every file. Budget: in a 10,000-entry folder the first
  rows show in under 300 ms on a low-end handheld (section 10).

- **`Volume`**: one storage root (internal, an SD card, a USB OTG drive; later an SMB share).
  `VolumeRepository` lists volumes and emits when one is mounted or removed.
- **`VolumeStore`**: an interface in `core:fs` for what must be saved about volumes (SAF grants,
  ids of volumes without a UUID, later network connections and their credentials).
  `core:data` implements it, so `core:fs` does not depend on `core:data`, and network providers
  (v1.x, one module each, such as `core:fs-smb`) add no cycle.
- **Providers in the MVP**:
  - `LocalProvider`: direct file access where the storage permission allows it and the volume
    has a readable path. See ADR [0002](adr/0002-storage-permission.md).
  - `SafProvider`: `DocumentsContract`, for volumes without a usable path and for any tree the
    user grants by hand.
  - `LegacyRemovableProvider` (Android 8 to 10 only): reads an SD card or USB drive by path,
    which the legacy permission allows and which lists several times faster than SAF, and
    writes through its SAF volume-root grant, which the legacy permission cannot. On AOSP the SAF
    document id of a file on such a volume is `<uuid>:<relative path>`, so writes try that id
    first. It is not a documented contract: if it gives `NotFound`, the provider looks the
    document up by name from the root.
  - **One provider object owns each volume**. `VolumeRepository` picks it when the volume mounts:

    | Volume | Android 8 to 10 | Android 11+ |
    | --- | --- | --- |
    | Internal storage | Local | Local |
    | SD card | `LegacyRemovableProvider` with a volume-root grant; read-only (no grant) until the user grants the root or a subfolder; SAF if its path is not readable | Local if its path is readable, else SAF, volume-root grant |
    | USB OTG drive | Same as SD card | Local if its path is readable, else SAF, volume-root grant |
    | A folder the user granted by hand | SAF | SAF |

    **Volume-root grant**: when a removable volume mounts without a saved grant, the app asks
    for access to its root (`StorageVolume.createAccessIntent` on Android 8 and 9,
    `createOpenDocumentTreeIntent` on 10+), checks that the returned tree is that volume's root,
    and saves the grant. A volume-root grant is a whole volume and can have Trash. On Android 11+
    the system blocks root grants for internal storage and `Download/`, and may block the root of
    an SD card the device maker marks as reliable; on most devices such a card has a readable
    path anyway. If the root grant is refused, or the user picks a subfolder, that folder is
    treated as a folder granted by hand (no Trash; see "Overlapping volumes" when the volume is
    already readable by path).

    **Readable** means the app can list the volume root by path. **Writable** means it can also
    create and delete a probe file there. On Android 11+, Local needs both; a readable but not
    writable path gets SAF. On Android 8 to 10, `LegacyRemovableProvider` needs only a readable
    path, because its writes go through SAF. A folder granted by hand is its own volume, rooted
    at the granted folder, and never has Trash (we do not create trash folders
    inside a folder the user picked).
- **MediaStore** is not a provider. After a write, `core:fs` asks MediaStore to rescan the
  changed paths so other apps see them. Gallery thumbnails (rest of v1) read from MediaStore.
- **Errors** are values: `FsResult` is `Ok` or `Err(FsError)`. `FsError` is a sealed type
  (`NotFound`, `AccessDenied`, `NoSpace`, `AlreadyExists`, `ReadOnly`, `Unsupported`, `VolumeUnavailable`,
  `FileTooLarge`, `InvalidName`, `AuthRequired`, `Offline`, `Timeout`, `Io(cause)`). The MVP
  providers never return the three network errors, but the UI handles them from the start. No exception crosses the `core:fs` boundary
  except `CancellationException`, which is always rethrown so Pause and Cancel work. The UI
  turns each `FsError` into a message and, where it can, an action (Retry, Grant access); raw
  exception text never reaches the screen.
- **Volume limits**: each `Volume` reports its file-system limits (maximum file size, forbidden
  name characters, case-insensitive names). FAT32 SD cards and USB drives cap files at 4 GB and
  reject `: ? " * | < > \` in names. `JobRunner` checks each item against the destination's
  limits before copying that item's bytes, and conflict checks use `EntryKey`, so they ignore
  case on case-insensitive volumes.
- **Serialized paths**: `FsPath` has one stable string form, defined in `core:model`. It starts
  with a version tag and carries the `ProviderRef` when there is one. The parser reads every
  older version. The string form is for storage only: Room tables store it next to a separate
  key column (the serialized `EntryKey`) and look rows up by the key, so a new string version or
  a refreshed `ref` never hides an old row. No other code builds or parses either form.
- **Random access** (7z extraction, the image viewer) uses `openRandomAccess`. `SeekableSource`
  wraps a read-only `SeekableByteChannel`, which Commons Compress `SevenZFile` accepts. Callers
  go through `RandomAccessOpener` in `core:fs`: it calls `openRandomAccess` when the provider has
  the `randomAccess` capability, and otherwise copies the file into a `ScratchFile` first, with
  progress, cancellation and a `NoSpace` error like any copy. Closing that `SeekableSource`
  deletes the scratch file. Callers never see `File`.
- **Seekable writes** (7z creation, rest of v1): `SevenZOutputFile` needs a
  `SeekableByteChannel`, which a `Sink` cannot give. The writer creates a `ScratchFile`, writes
  the archive through its `channel()`, then copies `source()` to the destination through
  `openWrite`, with the usual temp name, conflicts and progress. Both stages are one job, and
  the job owns the `ScratchFile`: it closes it when it ends, fails or is cancelled. A scratch
  file cannot be resumed, so after process death that job becomes `Failed` (not `Paused`), says
  so, and its scratch file is deleted at app start with every other one.
  Scratch files live in a `scratch/` folder under `noBackupFilesDir`, not the cache folder,
  because Android may purge the cache while the job runs. They never appear as a volume.
  `ScratchSpace` reserves the size hint with `StorageManager.allocateBytes` (it frees the app's
  cache if it must) and returns `NoSpace` when that fails. Limit: the archive is built on
  internal storage, so creating a 7z for an SD card needs that much free internal space too.
  Only data-layer modules (`core:fs`, `core:archive`) use `ScratchSpace`.
- **Capabilities** decide behavior, not the provider type. Example: a move inside one volume
  uses `move` only when the provider has it. Trash is not a provider capability: `core:jobs`
  decides per volume whether it has Trash (section 5).
- **Hidden app entries**: `.Trash-hemisphere`, `.hemisphere-part-*` and `.hemisphere-rename-*` are the app's own files.
  `core:jobs` owns the rule that names them (`InternalEntries`). `ObserveFolderUseCase` drops
  them from listings, and the job walker skips them, so a whole-volume copy does not copy the
  Trash.
- **Threading**: providers are `suspend` and run their blocking work on an injected
  `IoDispatcher`.
- **Test double**: `InMemoryProvider` in `core:fs-testing` (re-exported by `core:testing`). Every
  `core:jobs` and use-case test runs on it. It can act as name-addressed or `idAddressed`, can be
  case-insensitive, can fail or stall any call on demand, and reports free space, so both identity
  rules and the error paths are tested. `FileSystemProviderContract`, in the same module, is the
  suite every provider subclasses (`InMemoryProvider` today; the local and SAF providers once their
  tests have a module that may use `core:fs-testing`, see §2); it pins that `rename`, `move`, `copy`
  and `openWrite(Create)` onto a taken name return `AlreadyExists` and change nothing. The fake keeps
  names unique in a folder even when `idAddressed` (only a seeded listing may repeat a name), so
  one contract covers both identity rules; a cloud provider's own tests cover two files with one
  name. Faults also cover streams (`failNextStream`: a read, write or close that throws).
- **Streams**: `Source` and `Sink` are `java.io.InputStream` and `OutputStream`. `Sink.close()`
  returns only after the bytes are synced to storage, so a job may rename its temp file to the final
  name right after it.

Known limit: Android 11+ blocks `Android/data` and `Android/obb` for every app, even with
all-files access. The browser shows these folders as locked, with a short explanation.

## 4. `core:jobs`: the job queue

Copy, move, delete, restore, rename and new folder run in a queue that we own. Long jobs run in
a foreground service. See ADR [0005](adr/0005-job-queue.md).

```
use case ──enqueue(Job)──► JobQueue ──persists──► Room (jobs, cursor, records)
                          │
                          ├── starts ──► JobService (type dataSync, notification)
                          │                    │
                          │                    └── runs ──► JobRunner ──► FileSystemProvider
                          │
ObserveJobsUseCase ◄── StateFlow<List<JobState>> (progress chip, queue sheet, notification)
```

- **Job**: one user action (copy 3 items to `/SD/ROMs`). **Item**: one file inside a job.
- **What is stored**: the job's roots (sources, destination, options) and a cursor: the relative
  path of the last finished item in a depth-first walk sorted by a fixed ordinal comparator (code
  points, not the locale), plus `bytesDone` of the current file. Resume walks again and skips
  everything at or before the cursor, so files that were moved away or added elsewhere do not
  shift it. There is no row per item written up front: a 100,000-file folder starts copying at
  once. Progress lives in memory and is written to Room at most once a second and when a file
  finishes.
- **Records** that must outlive the job (failures, skips, what Undo needs) grow by one row per
  file they cover. They are written in the same Room transaction that advances the cursor, so a
  crash never leaves finished files without their records and Undo is never silently partial.
  Undo covers delete, move, rename and new folder (section 5), not copy, so a copy writes records only for
  failures, skips and each Replace (the target it sent to Trash).
- **Counting**: a counting pass runs beside the copy and fills in the totals for the progress
  bar. It does not hold back the first byte.
- **States**: `Queued → Running → (Paused | WaitingForConflict) → Done | Failed | Cancelled`.
- **Volume removed** (SD card or USB drive pulled): jobs that touch it move to `Paused` with a
  `VolumeUnavailable` error and resume only if the same volume comes back. Its temp files are
  cleaned when it mounts again. A pane showing it falls back to internal storage.
- **One long job runs at a time per destination volume**; jobs on different volumes may run in
  parallel (at most 2).
- **Short jobs**: rename and new folder are jobs too, so they share the queue's records, Undo
  and errors. They run at once, in the app process, without starting the service and without
  waiting behind a long job. Two jobs may not overlap: `JobQueue.enqueue` checks this
  atomically, in both directions (a short job against queued and running long jobs, and a long
  job against unfinished short jobs). Paths overlap when one is the other, or an ancestor or
  descendant of it, so renaming `/SD` while copying into `/SD/ROMs` is refused too. A job claims
  its roots and each entry it creates in the destination (`destination/<root name>`); those claims
  are exclusive. The destination folder itself is a shared claim: two copies may write into one
  folder, but no job may change that folder or an ancestor while another writes into it. The
  refused job is not queued; the user sees "In use by a job". A rename waiting on the conflict sheet
  keeps its claim until it is answered or cancelled.
- **Rename**: the use case checks the new name against the volume's limits before enqueueing.
  The conflict check skips the entry being renamed: on a case-insensitive volume `a.txt` to
  `A.txt` has the same `EntryKey`, so it is a case-only rename, not a conflict. A case-only
  rename goes through a temp name (`.hemisphere-rename-<job>`), because some file systems ignore
  a rename to a name that differs only in case. The job records each step, and stores the path
  the provider returned after the first step (SAF may return a new document id), as for temp
  files; the second step renames that path and then checks the final name. If the app dies
  between the steps, the rename finishes when the job's volume is next mounted. This is the one
  exception to "Nothing resumes silently" below, because until then the user's file is hidden.
  Rename temps are never deleted by temp-file cleanup. When the mount scan finds a
  `.hemisphere-rename-*` entry with no job record (the app data was cleared, or the card comes
  from another device), it renames the entry to `Recovered <job>`, with a Keep-both suffix if
  that name is taken, so the file never stays hidden.
  A name taken by another entry shows the conflict sheet (Skip, Replace, Keep both); Replace
  sends that other entry to Trash first, as for copy.
- **New folder**: a taken name is shown as an error in the name dialog, before the job is
  queued; `AlreadyExists` from the provider is the backstop. Undo deletes the folder only if it
  is still empty.
- **Fast paths**: a move inside one volume uses `move` when the provider has the `move`
  capability. Copy plus delete is used only across volumes or when `move` is missing. A copy
  inside one volume uses `copy` when the provider has `serverCopy` (network shares, v1.x).
- **Space check**: when the counting pass ends, the job compares the bytes still to copy with
  `freeSpace` and pauses with `NoSpace` if they cannot fit. Before each file it checks that file
  alone. A `NoSpace` error from a write is the backstop. `null` (unknown) skips the checks.
- **Move across volumes** deletes each source file right after its copy is complete and renamed
  into place, not at the end, so a stopped move never leaves two full copies to clean up.
- **Copy** streams in chunks through `openRead`/`openWrite`, writes to a temp name, then renames.
  A failed or cancelled item leaves no half-written file under the final name.
- **Temp files** are named `.hemisphere-part-<job>-<n>`, where `n` is a counter stored on the job
  that only goes up, and created as `application/octet-stream` so SAF keeps the name. The job stores the
  current temp file's path as the provider returns it (not the one requested), in its own field
  next to the cursor. They are deleted on cancel, when a resume
  restarts the item, and at app start for any job that is not running and cannot resume.
- **Replace** (conflict choice) moves the existing target to Trash first, then renames the temp
  file into place, so Replace can be undone like a delete. On a volume without Trash the target is deleted permanently, and the conflict sheet says so with `danger`.
  The item records each step, so a crash between the two is reported, not hidden.
- **Folder conflicts**: copying or moving a folder onto an existing folder merges them. Each file
  inside that already exists is its own conflict. Replace never applies to a whole folder.
- **Pause** stops at the next chunk boundary, flushes the sink and saves `bytesDone`.
  **Resume** continues the current file when both providers have the `resume` capability and
  the source's size and modified time have not changed. It continues from the temp file's
  length when that length is at least `bytesDone` and at most the source size; the bytes
  written after the last save are a straight copy of an unchanged source, so they are valid.
  The sink is flushed before every save of `bytesDone`, so after process death the temp file is
  never shorter than the saved value. Otherwise it restarts that file. A paused or killed 30 GB
  copy does not start over.
- **Modified times**: after a copy, the target gets the source's modified time when the
  destination has `setTimes`.
- **Conflicts** pause the job in `WaitingForConflict`. The conflict sheet answers it, from the
  progress chip, the queue sheet or the notification; "apply to all" is stored on the job.
- **Process death**: on start, jobs that were `Running` become `Paused` and show in the progress
  chip with a Resume action. Nothing resumes silently, except an unfinished case-only rename
  (see Rename), and a 7z creation job fails instead (section 3).
- **Notifications**: on Android 13+ the app asks for `POST_NOTIFICATIONS` when the first job is
  queued. If it is denied, the job still runs. The progress chip and the queue sheet offer every
  action the notification has (pause, cancel, answer a conflict, resume), so nothing depends on
  the notification.
- **Android 14+** needs the `dataSync` service type and its permission (and a Play Console
  declaration, ADR [0003](adr/0003-distribution.md)).
- **Android 15+** caps `dataSync` at 6 hours a day. The service overrides `onTimeout`: it marks
  running jobs `Paused`, posts a notification and calls `stopSelf()` at once (a late stop crashes
  the app). Resuming catches `ForegroundServiceStartNotAllowedException` and keeps the job
  `Paused` until the app is in the foreground.

## 5. Trash

Delete moves items into a hidden trash folder on the **same volume**, so trashing is a rename,
never a cross-volume copy. See ADR [0007](adr/0007-trash-format.md).

```
<volume root>/.Trash-hemisphere/
    files/   k7m2q9xd4tbn/holiday.jpg   ← one folder per trashed entry, named by a generated id
    info/    k7m2q9xd4tbn.trashinfo
```

- Each trashed entry gets its own folder in `files/`, named by a random 12-character id, and
  keeps its own name inside it. The info file is named by the same id. Names never collide, the
  info file name never passes the 255-character limit, and no rename is needed: the trash
  operation is one move, which works the same on SAF (`moveDocument` keeps the name).
- The `.trashinfo` file uses the freedesktop.org format. `Path=` is relative to the volume root
  (the spec allows this for trash on removable media), so an SD card restores correctly in
  another device. It also holds `DeletionDate=` and `X-Hemisphere-Version=1`. It is the source
  of truth.
- **Order**: write the info file, create the id folder, then move the entry into it. When a
  volume mounts, the scan repairs a crash between steps: an info file whose id folder is missing
  or empty is deleted with that folder. There is nothing to match by name.
- Room keeps an index of every volume's trash for the Trash screen. It is a cache and can be
  rebuilt by scanning `info/` when a volume mounts.
- A `.nomedia` file in the trash folder keeps trashed media out of galleries.
- Retention (30 days by default, adjustable) is enforced when a volume mounts and once a day
  while the app runs. No background job wakes the device for it.
- **Who decides**: Trash is a property of a volume, owned by `core:jobs` (`TrashSupport`), not a
  provider capability. A volume has Trash when its provider has `move`, the volume is a whole
  volume (not a folder granted by hand), and the app can create the trash folder at its root.
  That includes SD cards and OTG drives through SAF on Android 8 to 10
  (`DocumentsContract.moveDocument`, API 24+), so they keep Trash as the design spec promises.
  `core:jobs` also scans each volume's `info/` folder when `VolumeRepository` reports it mounted.
- Volumes without Trash (read-only, providers that cannot move, folders granted by hand, later
  network) delete permanently, with the `danger` dialog and no Undo.
- Undo for delete, move, rename and new folder is a reverse job queued in `core:jobs`.
- **Restore** is a job too. It recreates missing parent folders, and if the original path is
  taken, it shows the conflict sheet (Skip, Replace, Keep both).

## 6. State, UI and domain layer

We follow Google's guide to app architecture: UI, domain and data layers, with unidirectional
data flow. Two choices go further than the guide (ADR
[0009](adr/0009-strict-mvi-and-domain-layer.md)): the domain layer, optional in the guide, is
required; and the UI layer uses strict MVI, our stricter form of Google's UDF (intents, results
and a pure reducer; the guide itself does not prescribe reducers). Data flows one way, in a loop:

```
 user input (touch or gamepad)
        │
        ▼
 BrowserIntent ──► BrowserViewModel.onIntent ──► use cases (core:domain) ──► data layer
                                                  │
                                                  ▼
                                   BrowserResult (what happened)
                                                  │
                                                  ▼
               reduce(BrowserUiState, BrowserResult) ──► new BrowserUiState
                                                  │
                                                  ▼
             StateFlow<BrowserUiState> ──► composables (stateless) ──► user input
```

### 6.1 UI layer: strict MVI

Each ViewModel has four types, in one package, named after it (`Browser*` for
`BrowserViewModel`, `Trash*` for `TrashViewModel`, and so on):

| Type | What it is |
| --- | --- |
| `BrowserUiState` | One immutable data class: everything the screen shows. |
| `BrowserIntent` | A sealed interface: everything the user can do (`OpenEntry`, `Mark`, `Paste`…). |
| `BrowserResult` | A sealed interface: everything that can change the state (`ListingLoaded`, `MarkToggled`, `JobFailed`…). |
| `reduce(state, result)` | A pure function that returns the new state. |

- **One `BrowserViewModel`** for the whole Browser screen, in both Single and Dual. Paste, "copy
  to the other pane", the clipboard tray and back order all need both panes and the shell at
  once, so they live in one state and change in one reduce:

  ```kotlin
  data class BrowserUiState(
      val shell: ShellState,             // layout mode, active pane, clipboard tray
      val panes: Map<PaneId, PaneState>, // left, right; Single shows only left
  )
  data class PaneState(val tabs: List<TabState>, val activeTab: Int)
  data class TabState(val backStack: List<FsPath>, val anchors: Map<EntryKey, Anchor>,
                      val listing: Listing, val selection: Set<EntryKey>)
  ```

  Results that touch one pane are wrapped as `BrowserResult.Pane(paneId, result)`, and the
  browser reducer hands the inner result to a shared `reducePane(PaneState, PaneResult)`, so
  each pane's logic is written and tested once. `PaneResult` (and `PaneIntent`, wrapped the same
  way in `BrowserIntent.Pane`) are nested types, not a second top-level `XxxResult`.
  The last input is not ViewModel state: `core:input` provides it through `LocalInputMode`.
- **Tabs**: each `TabState` holds its back stack, a scroll anchor for each folder on that back
  stack (the focused entry and first visible row; an anchor is dropped when its folder is
  popped, so anchors never outnumber the capped stack) and its selection. The MVP has one tab per pane; tabs (rest of v1) add
  more without changing the state's shape.
- **Large folders**: each tab's listing is an immutable list that only changes when the folder does.
  Selection is a separate set of entry keys in the state, so a Mark changes only that set and
  only the marked row redraws.
- **Intents** go in through one public function, `onIntent(intent)`. A ViewModel has no other
  public function. Type names always carry the ViewModel prefix (`BrowserIntent`), never a bare
  `Intent`, so they are not confused with `android.content.Intent`.
- **The ViewModel** turns each intent into use-case calls and maps what they return to results.
  It does not change state itself.
- **The reducer** is the only place state changes. It is pure: no I/O, no coroutines, no use
  cases, no clock or random values. Anything like that is read before and passed in the result.
- **The base class**: every screen ViewModel extends `MviViewModel` from `core:mvi`, which holds
  the state flow and the only write to it (`emit(result)`). How to add a screen:
  [`templates/screen.md`](templates/screen.md).
- **State** lives in a private `MutableStateFlow(initial)`. Its only write is
  `state.update { reduce(it, result) }`, and it is exposed with `asStateFlow()`. It lives as
  long as the ViewModel, so marks and selection survive the app going to the background. The UI
  collects it with `collectAsStateWithLifecycle`. Results come from intent handling and from
  use-case flows the ViewModel collects in `viewModelScope`; they are internal, not UI events.
- **One-off events** (show a snackbar, open the `danger` delete dialog) are fields in the UI state, as
  Google recommends. The UI sends an intent (`MessageShown`) after it handles one, and the
  reducer clears it. No `Channel`s or `SharedFlow` events.
- **Saved state** that must survive process death (the tab back stacks and scroll anchors, not
  listings or selection) goes in `SavedStateHandle`. The ViewModel builds `initial` from it and
  registers `handle.setSavedStateProvider(KEY) { ... }`, which serializes the current state only
  when Android saves, not on every change. The reducer never writes it. Back stacks are capped
  (section 7) so the saved bundle stays well under the binder limit. Tab history survives
  process death, not a cold start.
- **Anchors** are captured when the user leaves a folder (open, back, breadcrumb), not on every
  scroll or focus move, so scrolling does not go through the reducer.
- **Composables** are stateless below the screen level: they take state and lambdas, with the
  `Modifier` parameter first among the optional parameters (Compose API guidelines).
  `remember` is for UI-only state (scroll, focus, animation).
- **Focus**: `core:designsystem` defines `LocalInputMode` (touch or gamepad); `core:input` sets
  its value at the root from the last input. Every interactive component in `core:designsystem`
  reads it to show or hide the focus ring and the hints.

### 6.2 Domain layer: use cases

`core:domain` holds the app's rules in plain Kotlin, between the UI and the data layer. Every
ViewModel calls use cases only, never a repository, a provider or the `JobQueue`. This keeps
each rule in one place that is tested on the JVM, and lets features grow without copying logic
between ViewModels.

| Use case | What it does |
| --- | --- |
| `ObserveFolderUseCase` | Streams a folder's entries, sorted and filtered by its saved view settings |
| `CopyEntriesUseCase`, `MoveEntriesUseCase` | Checks the target and enqueues a job |
| `RenameEntryUseCase`, `CreateFolderUseCase` | Checks the name and enqueues a short job |
| `DeleteEntriesUseCase` | Sends entries to Trash; on a volume without Trash it returns "needs confirmation" so the screen shows the `danger` dialog first |
| `RestoreFromTrashUseCase` | Enqueues a restore job |
| `ObserveJobsUseCase`, `ResolveConflictUseCase` | Job progress and conflict answers |
| `ObserveSettingsUseCase`, `SetThemeUseCase`… | Settings |

Rules:

- **Name**: `VerbNounUseCase`. One public `operator fun invoke`, so the call reads as
  `observeFolder(path)`.
- **Shape**: a `suspend fun` that returns a result (`FsResult` or a domain result) for one-shot
  work, or a `Flow` for streams. No callbacks.
- **No state**: a use case holds no mutable fields. It gets its dependencies through an
  `@Inject constructor`, so it needs no Hilt module.
- **ViewModel constructors** take only use cases, `SavedStateHandle` and assisted plain values
  (`@Assisted`, such as an entry id passed by navigation). Never a repository, a provider or the
  `JobQueue`.
- **Main-safe**: blocking or heavy work runs on an injected dispatcher, in the data layer or in
  the use case (sorting a large listing uses `@DefaultDispatcher`).
- **Every action has one**, even when it only passes a call through. A small use case costs a
  file; a ViewModel that reaches past the domain layer costs a rule that is not tested in one
  place.
- A use case may call other use cases. It never depends on a ViewModel or on Android UI types.
- **Packages by area**: `core.domain.files`, `core.domain.jobs`, `core.domain.trash`,
  `core.domain.settings`. Use cases stay in one module because features share them (ADR 0009).

## 7. Navigation

Navigation 3: the back stack is a list we own, which fits panes that each keep their own history.

```
App back stack (app):        [ Access ] ⇢ [ Browser ] → [ Settings ] → [ AccentPicker ]
                                                │
Pane back stacks (browser):  left:  [ /Internal ] → [ /Internal/Download ]
                             right: [ /SD ] → [ /SD/ROMs ] → [ /SD/ROMs/GBA ]
```

- Top-level screens (Browser, Settings, Trash) are `NavKey`s on the app back stack in `app`.
- Access is a gate, not history (HEM-39, ADR 0002): until storage access is granted the stack is
  only `[ Access ]`. A change of access replaces the whole stack, so a grant opens Browser and a
  revoke (read again each time the app comes to the foreground) puts Access back. `StorageAccess`
  lives in `core:fs` and its `StorageAccessState` in `core:model`, because `core:domain` cannot see
  `core:platform`.
- Each tab keeps its own back stack of `FsPath` in `TabState`, saved across process death
  (section 6.1). History is capped at 50 folders per tab, so saved state stays small.
- Back restores the scroll anchor, so a gamepad user lands on the folder they left, not the
  first row.
- System back and Ⓑ go in this order: leave selection mode if it is on; then, only while
  Browser is the top app key, pop the active pane's stack; then pop the app stack. On Settings or
  Trash, back closes that screen.
- Back is history, not "up": after a jump from Places or a breadcrumb, back returns to where the
  user was. Going up one folder is the breadcrumb's parent segment (D-pad up into the
  breadcrumbs, then Ⓐ).
- Dialogs and sheets are Navigation 3 scenes or local state, never separate activities.

## 8. Persistence

| Data | Store | Why |
| --- | --- | --- |
| Settings: theme, accent, Face buttons, density, Trash retention | Preferences DataStore | Small key-value data, observed as `Flow` |
| Layout pin per orientation | Preferences DataStore, key includes a device-class id | One backup restores correctly on a phone and a handheld |
| Per-folder view and sort | Room, `user.db` | Many rows, queried by path |
| Job queue (jobs, cursors, records) | Room, `state.db` | Must survive process death |
| Trash index | Room, `state.db` (cache only) | Rebuilt from `.trashinfo` files |
| Bookmarks (rest of v1) | Room, `user.db` | |
| `VolumeStore`: SAF grants, ids of volumes without a UUID, later network connections | Room, `volumes.db` in `core:data` | Grants do not move between devices and hold paths; never backed up |
| Network credentials (v1.x) | Encrypted with an Android Keystore key, beside `state.db` | Never backed up |

- Features read settings and saved data only through use cases in `core:domain`, which call the
  repositories in `core:data` and the `JobQueue`. Nothing outside the data layer (`core:data`,
  `core:jobs`) touches DataStore keys or DAOs.
- Room migrations are written and tested from the first release; no `fallbackToDestructiveMigration`.
- Two Room database files: `user.db` (in `core:data`) holds what the user chose (per-folder
  views, bookmarks); `state.db` (in `core:jobs`) holds what the app can rebuild or must not
  restore (the queue, the Trash index).
  Android backup works on whole files, so the split is what makes the rules below possible.
- Backup rules (`dataExtractionRules` on Android 12+, `fullBackupContent` below) are an
  allowlist: only the files named here are backed up, so files added later (logs, crash reports)
  stay on the device.
  - The DataStore settings file: cloud backup and device-to-device transfer. It holds no paths.
  - `user.db` with its `-wal` and `-shm` files: device-to-device transfer only, never cloud
    backup, because it holds folder paths (ADR [0008](adr/0008-privacy.md)). Android 8.x cannot
    limit a rule to device-to-device (that flag needs Android 9), so there `user.db` is not
    backed up at all.
  - `state.db`, `volumes.db` and the encrypted credentials: never backed up.

## 9. Dependency injection

Hilt. Each data-layer module (`core:fs`, `core:jobs`, `core:data`) exposes interfaces, keeps its
implementations `internal`, and has one Hilt `@Module` that binds them. Use cases in `core:domain`
are public classes with an `@Inject constructor` and need no module. `app` is the only
`@HiltAndroidApp`. ViewModels use `@HiltViewModel` and are created with `hiltViewModel()`.
Dispatchers are injected (`@IoDispatcher`, `@DefaultDispatcher`), never hard-coded, so tests can
swap them.

## 10. Testing

| Layer | Tool | Runs where |
| --- | --- | --- |
| Reducers | JUnit 4: given a state and a result, check the new state. No coroutines, no fakes. | JVM (`./gradlew test`) |
| Use cases | JUnit 4, `kotlinx-coroutines-test`, Turbine, with fakes: `InMemoryProvider`, a fake `JobQueue`, fake repositories | JVM (`./gradlew test`) |
| ViewModels | Same tools: send intents, check the `StateFlow`. Real use cases over fake data sources, no mocks. | JVM (`./gradlew test`) |
| `core:fs`, `core:jobs` | JUnit 4, `kotlinx-coroutines-test`, Turbine, `InMemoryProvider` | JVM (`./gradlew test`) |
| Layer rules | Konsist architecture tests: ViewModels take only use cases, use-case naming, no `android.*` in `core:domain` | JVM (`./gradlew test`) |
| `core:data` | Room in-memory database, migration tests | JVM with Robolectric |
| Components and screens | Roborazzi screenshots: component × {light, dark, AMOLED} × {phone, handheld} × {touch, gamepad} | JVM with Robolectric |
| Providers against real storage | Instrumented tests | Emulator (`env-emulator`) |
| Gamepad on real handhelds | Manual checklist | Device (`env-device`) |
| Large folders | Macrobenchmark: 10,000 entries, first rows under 300 ms on a low-end handheld | Device (`env-device`), before each release |

A bug fix starts with a failing test that shows the bug, at the lowest layer in this table that
can show it.

## 11. Other decisions

| Topic | Decision | ADR |
| --- | --- | --- |
| Storage permission | All-files access on Android 11+, legacy permission on 8 to 10 | [0002](adr/0002-storage-permission.md) |
| Distribution | GitHub Releases and F-Droid for the MVP, Play later | [0003](adr/0003-distribution.md) |
| Archive libraries | Commons Compress and libarchive, no unRAR | [0004](adr/0004-archive-libraries.md) |
| Privacy | No telemetry, no crash service, no internet permission in v1 | [0008](adr/0008-privacy.md) |
| UI and domain layers | Strict MVI per screen; ViewModels call use cases only | [0009](adr/0009-strict-mvi-and-domain-layer.md) |
| Package ID | `io.github.ctorres345.hemisphere` | design-system.md, open items (updated in HEM-2) |
