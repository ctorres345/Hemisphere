# Hemisphere architecture

Status: **proposed** (HEM-2). Becomes accepted when Cesar approves the PR.

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

```
                       ┌──────────┐
                       │   app    │  Activity, DI root, nav host, ops service
                       └────┬─────┘
          ┌─────────────────┼──────────────────┐
          ▼                 ▼                  ▼
  ┌───────────────┐ ┌───────────────┐  ┌───────────────┐
  │feature:browser│ │feature:settings│ │ feature:trash │   (more features later)
  └──────┬────────┘ └──────┬────────┘  └──────┬────────┘
         └─────────┬───────┴──────────────────┘
                   ▼
  ┌────────────┬────────────┬───────────┬───────────┬────────────┐
  │core:design │ core:input │ core:jobs │ core:data │  core:fs   │
  │  system    │            │     │     │     │     │            │
  └────────────┴────────────┴─────┼─────┴─────┼─────┴────────────┘
                                  │           └──────► core:fs
                                  └──────────────────► core:fs, core:data
```

| Module | Owns | Depends on |
| --- | --- | --- |
| `app` | `MainActivity`, Hilt root, top-level Navigation 3 host, the job service, manifest permissions | every module |
| `core:designsystem` | Tokens generated from `tokens/*.json`, `HemisphereTheme`, shared components, the focus ring, `InputMode` and `LocalInputMode`, button-hint glyphs | nothing in the app |
| `core:input` | Last-input tracking (touch or gamepad), the gamepad map, Face buttons setting, key and axis handling. It sets the value of `LocalInputMode`. | `core:designsystem`, `core:data` |
| `core:fs` | The file-system abstraction (section 3), volumes, the local and SAF providers, MediaStore scans | nothing in the app |
| `core:jobs` | The job queue (section 4), conflicts, Trash (section 5), and the `state.db` Room database that stores them | `core:fs`, `core:data` |
| `core:data` | The `user.db` Room database, DataStore settings, their repositories | `core:fs` (for `FsPath` and its stable string form) |
| `feature:browser` | Shell (Single and Dual), panes, file list, selection, clipboard tray | all `core:*` |
| `feature:jobs` | Progress chip, queue sheet, conflict sheet. `app` shows them above every screen (Browser, Trash, Settings), so a conflict can be answered from anywhere. | `core:designsystem`, `core:jobs`, `core:input` |
| `feature:settings` | Settings screens (theme, accent, layout pin, Face buttons, Trash retention) | `core:designsystem`, `core:data`, `core:input` |
| `feature:trash` | The Trash screen: list, restore, empty | `core:designsystem`, `core:jobs`, `core:input` |

Rules:

- Packages follow modules: `io.github.ctorres345.hemisphere.core.fs`,
  `io.github.ctorres345.hemisphere.feature.browser`, and so on.
- A `feature:*` module never depends on another `feature:*` module. Features talk through
  `app` (navigation keys) or through a `core:*` module.
- `core:*` modules never depend on `feature:*`.
- `core:fs`, `core:jobs` and `core:data` have no Compose dependency. They are tested on the JVM.
- New features (search, viewer, editor, archives) arrive as new `feature:*` modules. Archive
  formats arrive as a `core:archive` module (see ADR [0004](adr/0004-archive-libraries.md)).

## 3. `core:fs`: the file-system abstraction

The rest of the app never sees `java.io.File`, a raw path string, or a `content://` URI. It sees
`FsPath` and a `FileSystemProvider`. The one exception is `shareableUri`, which hands a URI
straight to an `Intent` for another app (open with, share) and is never parsed or stored. This is what lets SMB, SFTP and cloud plug in later without a
rewrite. See ADR [0006](adr/0006-file-system-abstraction.md).

```kotlin
// Sketch, not final code.
data class FsPath(val volume: VolumeId, val segments: List<String>)

interface FileSystemProvider {
    val capabilities: Capabilities          // move, trash, randomAccess, share, watch, ...
    suspend fun list(dir: FsPath): FsResult<List<Entry>>
    suspend fun stat(path: FsPath): FsResult<Entry>
    suspend fun openRead(path: FsPath): FsResult<Source>
    suspend fun openRandomAccess(path: FsPath): FsResult<SeekableSource>  // only if capabilities.randomAccess
    suspend fun openWrite(path: FsPath, mode: WriteMode): FsResult<Sink>
    suspend fun createDirectory(path: FsPath): FsResult<Entry>
    suspend fun rename(path: FsPath, newName: String): FsResult<Entry>
    suspend fun move(from: FsPath, to: FsPath): FsResult<Entry>   // same volume, only if capabilities.move
    suspend fun delete(path: FsPath): FsResult<Unit>              // permanent
    fun observe(dir: FsPath): Flow<Unit>                          // change signal, may be a no-op
    suspend fun shareableUri(path: FsPath): FsResult<Uri>         // FileProvider or SAF URI, for Intents only
}
```

- **`Volume`**: one storage root (internal, an SD card, a USB OTG drive; later an SMB share).
  `VolumeRepository` lists volumes and emits when one is mounted or removed.
- **Providers in the MVP**:
  - `LocalProvider`: direct file access where the storage permission allows it and the volume
    has a readable path. See ADR [0002](adr/0002-storage-permission.md).
  - `SafProvider`: `DocumentsContract`, for volumes without a usable path and for any tree the
    user grants by hand.
  - **One provider owns each volume**, for reads and writes. `VolumeRepository` picks it when the
    volume mounts:

    | Volume | Android 8 to 10 | Android 11+ |
    | --- | --- | --- |
    | Internal storage | Local | Local |
    | SD card | SAF, volume-root grant (the legacy permission cannot write it) | Local if its path is readable, else SAF, volume-root grant |
    | USB OTG drive | SAF, volume-root grant | Local if its path is readable, else SAF, volume-root grant |
    | A folder the user granted by hand | SAF | SAF |

    **Volume-root grant**: when a removable volume mounts without a saved grant, the app asks
    for access to its root (`StorageVolume.createAccessIntent` on Android 8 and 9,
    `createOpenDocumentTreeIntent` on 10+), checks that the returned tree is that volume's root,
    and saves the grant. A volume-root grant is a whole volume and can have Trash. On Android 11+
    the system blocks root grants for internal storage and `Download/`, and may block the root of
    an SD card the device maker marks as reliable; on most devices such a card has a readable
    path anyway. If the root grant is refused, or the user picks a subfolder, that folder is
    treated as a folder granted by hand (no Trash).

    "Readable" means the app can list the root and create and delete a probe file there; a path
    that can be read but not written gets SAF. A folder granted by hand is its own volume, rooted
    at the granted folder, and never has the `trash` capability (we do not create trash folders
    inside a folder the user picked).
- **MediaStore** is not a provider. After a write, `core:fs` asks MediaStore to rescan the
  changed paths so other apps see them. Gallery thumbnails (rest of v1) read from MediaStore.
- **Errors** are values: `FsResult` is `Ok` or `Err(FsError)`. `FsError` is a sealed type
  (`NotFound`, `AccessDenied`, `NoSpace`, `AlreadyExists`, `ReadOnly`, `Unsupported`, `VolumeUnavailable`,
  `FileTooLarge`, `InvalidName`, `Io(cause)`). No exception crosses the `core:fs` boundary
  except `CancellationException`, which is always rethrown so Pause and Cancel work. The UI
  turns each `FsError` into a message and, where it can, an action (Retry, Grant access); raw
  exception text never reaches the screen.
- **Volume limits**: each `Volume` reports its file-system limits (maximum file size, forbidden
  name characters, case-insensitive names). FAT32 SD cards and USB drives cap files at 4 GB and
  reject `: ? " * | < > \` in names. `JobRunner` checks every item against the destination's
  limits before copying any bytes, and conflict checks ignore case on case-insensitive volumes.
- **Serialized paths**: `FsPath` has one stable string form, defined in `core:fs`. Room rows
  store that form; no other code builds or parses it.
- **Random access** (7z extraction, the image viewer) uses `openRandomAccess` when the provider
  has the `randomAccess` capability. Otherwise the caller copies the file to the app cache first.
  Features never fall back to `File`. Writers that need to seek (7z creation, rest of v1) write to
  the app cache, then copy the result through `openWrite`.
- **Capabilities** decide behavior, not the provider type. Example: Trash is offered only when
  the volume reports `trash = true`; otherwise delete is permanent and the dialog says so.
- **Threading**: providers are `suspend` and run their blocking work on an injected
  `IoDispatcher`.
- **Test double**: `InMemoryProvider` in `core:fs` test fixtures. Every `core:jobs` test runs on
  it.

Known limit: Android 11+ blocks `Android/data` and `Android/obb` for every app, even with
all-files access. The browser shows these folders as locked, with a short explanation.

## 4. `core:jobs`: the job queue

Copy, move and delete run in a queue that we own, executed by a foreground service. See ADR
[0005](adr/0005-job-queue.md).

```
UI ──enqueue(Job)──► JobQueue ──persists──► Room (jobs, items, state)
                          │
                          ├── starts ──► JobService (type dataSync, notification)
                          │                    │
                          │                    └── runs ──► JobRunner ──► FileSystemProvider
                          │
UI ◄── StateFlow<List<JobState>> (progress chip, queue sheet, notification)
```

- **Job**: one user action (copy 3 items to `/SD/ROMs`). **Item**: one file inside a job.
- **States**: `Queued → Running → (Paused | WaitingForConflict) → Done | Failed | Cancelled`.
- **Volume removed** (SD card or USB drive pulled): jobs that touch it move to `Paused` with a
  `VolumeUnavailable` error and resume only if the same volume comes back. Its temp files are
  cleaned when it mounts again. A pane showing it falls back to internal storage.
- **One job runs at a time per destination volume**; jobs on different volumes may run in
  parallel (at most 2).
- **Fast paths**: a move inside one volume uses `move` when the provider has the `move`
  capability. Copy plus delete is used only across volumes or when `move` is missing.
- **Copy** streams in chunks through `openRead`/`openWrite`, writes to a temp name, then renames.
  A failed or cancelled item leaves no half-written file under the final name.
- **Temp files** are named `.hemisphere-part-<job>-<item>` and created as
  `application/octet-stream` so SAF keeps the name. The item row stores the path the provider
  returns, not the one requested. They are deleted on cancel, on resume (before the item restarts) and at app start for any
  job that is not running.
- **Replace** (conflict choice) moves the existing target to Trash first, then renames the temp
  file into place, so Replace can be undone like a delete. On a volume without the `trash`
  capability the target is deleted permanently, and the conflict sheet says so with `danger`.
  The item records each step, so a crash between the two is reported, not hidden.
- **Folder conflicts**: copying or moving a folder onto an existing folder merges them. Each file
  inside that already exists is its own conflict. Replace never applies to a whole folder.
- **Pause** stops at the next chunk boundary. Resume restarts the current item from the start.
- **Conflicts** pause the job in `WaitingForConflict`. The conflict sheet answers it, from the
  progress chip, the queue sheet or the notification; "apply to all" is stored on the job.
- **Process death**: on start, jobs that were `Running` become `Paused` and show in the progress
  chip with a Resume action. Nothing resumes silently.
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
    files/   holiday.jpg      ← the trashed item, renamed if the name is taken
    info/    holiday.jpg.trashinfo
```

- The `.trashinfo` file holds the original path and the deletion date (the freedesktop.org
  Trash format). It is the source of truth.
- Room keeps an index of every volume's trash for the Trash screen. It is a cache and can be
  rebuilt by scanning `info/` when a volume mounts.
- A `.nomedia` file in the trash folder keeps trashed media out of galleries.
- Retention (30 days by default, adjustable) is enforced when a volume mounts and once a day
  while the app runs. No background job wakes the device for it.
- A volume has the `trash` capability when its provider has `move` and the app can create the
  trash folder at the volume root. That includes SD cards and OTG drives through SAF on Android 8 to 10
  (`DocumentsContract.moveDocument`, API 24+), so they keep Trash as the design spec promises.
- Volumes without the `trash` capability (read-only, providers that cannot move, later network) delete
  permanently, with the `danger` dialog and no Undo.
- Undo for delete, move and rename is a reverse job queued in `core:jobs`.
- **Restore** is a job too. It recreates missing parent folders, and if the original path is
  taken, it shows the conflict sheet (Skip, Replace, Keep both).

## 6. State and UI

One-way data flow with `ViewModel`s.

```
 user input (touch or gamepad)
        │
        ▼
 onAction(PaneAction) ──► PaneViewModel ──► repositories / JobQueue
                                │
                                ▼
                     StateFlow<PaneUiState> ──► composables (stateless)
```

- **`ShellViewModel`**: layout mode, active pane, clipboard tray, last input.
- **`PaneViewModel`**: one per pane, keyed by pane id (`left`, `right`; Single uses `left`). It
  owns the pane's location, listing, sort and selection.
- **UI state** is one immutable data class per screen, exposed as `StateFlow` and collected with
  `collectAsStateWithLifecycle`.
- **Actions** go in through one `onAction(action)` function per ViewModel, with a sealed action
  type.
- **One-off events** (show a snackbar, open the conflict sheet) are fields in the UI state that
  the UI clears with an action after it handles them. No `Channel`s or `SharedFlow` events.
- **Composables** are stateless below the screen level: they take state and lambdas, with the
  `Modifier` parameter first among the optional parameters (Compose API guidelines).
  `remember` is for UI-only state (scroll, focus, animation).
- **Focus**: `core:designsystem` defines `LocalInputMode` (touch or gamepad); `core:input` sets
  its value at the root from the last input. Every interactive component in `core:designsystem`
  reads it to show or hide the focus ring and the hints.

## 7. Navigation

Navigation 3: the back stack is a list we own, which fits panes that each keep their own history.

```
App back stack (app):        [ Browser ] → [ Settings ] → [ AccentPicker ]
                                  │
Pane back stacks (browser):  left:  [ /Internal ] → [ /Internal/Download ]
                             right: [ /SD ] → [ /SD/ROMs ] → [ /SD/ROMs/GBA ]
```

- Top-level screens (Browser, Settings, Trash) are `NavKey`s on the app back stack in `app`.
- Each pane keeps its own back stack of `FolderKey(FsPath)` in `PaneViewModel`, saved across
  process death.
- System back and Ⓑ go in this order: leave selection mode if it is on; then, only while
  Browser is the top app key, pop the active pane's stack; then pop the app stack. On Settings or
  Trash, back closes that screen.
- Back is history, not "up": after a jump from Places or a breadcrumb, back returns to where the
  user was. Going up one folder is the breadcrumb's parent segment (D-pad up into the
  breadcrumbs, then Ⓐ).
- Tabs (rest of v1) become one back stack per tab inside a pane. The MVP code keeps the pane
  stack behind one `PaneLocation` type so tabs do not change its callers.
- Dialogs and sheets are Navigation 3 scenes or local state, never separate activities.

## 8. Persistence

| Data | Store | Why |
| --- | --- | --- |
| Settings: theme, accent, Face buttons, density, Trash retention | Preferences DataStore | Small key-value data, observed as `Flow` |
| Layout pin per orientation | Preferences DataStore, key includes a device-class id | One backup restores correctly on a phone and a handheld |
| Per-folder view and sort | Room, `user.db` | Many rows, queried by path |
| Job queue (jobs, items) | Room, `state.db` | Must survive process death |
| Trash index | Room, `state.db` (cache only) | Rebuilt from `.trashinfo` files |
| Bookmarks (rest of v1) | Room, `user.db` | |

- Features read settings and saved data only through the repositories in `core:data`, never
  DataStore keys or DAOs directly.
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
  - `state.db`: never backed up.

## 9. Dependency injection

Hilt. Each `core:*` module exposes interfaces, keeps its implementations `internal`, and has one
Hilt `@Module` that binds them. `app` is the only
`@HiltAndroidApp`. ViewModels use `@HiltViewModel` and are created with `hiltViewModel()`.
Dispatchers are injected (`@IoDispatcher`, `@DefaultDispatcher`), never hard-coded, so tests can
swap them.

## 10. Testing

| Layer | Tool | Runs where |
| --- | --- | --- |
| `core:fs`, `core:jobs`, ViewModels | JUnit 4, `kotlinx-coroutines-test`, Turbine, `InMemoryProvider` | JVM (`./gradlew test`) |
| `core:data` | Room in-memory database, migration tests | JVM with Robolectric |
| Components and screens | Roborazzi screenshots: component × {light, dark, AMOLED} × {phone, handheld} × {touch, gamepad} | JVM with Robolectric |
| Providers against real storage | Instrumented tests | Emulator (`env-emulator`) |
| Gamepad on real handhelds | Manual checklist | Device (`env-device`) |

A bug fix starts with a failing test that shows the bug, at the lowest layer in this table that
can show it.

## 11. Other decisions

| Topic | Decision | ADR |
| --- | --- | --- |
| Storage permission | All-files access on Android 11+, legacy permission on 8 to 10 | [0002](adr/0002-storage-permission.md) |
| Distribution | GitHub Releases and F-Droid for the MVP, Play later | [0003](adr/0003-distribution.md) |
| Archive libraries | Commons Compress and libarchive, no unRAR | [0004](adr/0004-archive-libraries.md) |
| Privacy | No telemetry, no crash service, no internet permission in v1 | [0008](adr/0008-privacy.md) |
| Package ID | `io.github.ctorres345.hemisphere` | design-system.md, open items (updated in HEM-2) |
