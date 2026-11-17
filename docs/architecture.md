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
| Operations queue with progress, pause, cancel, notification | Material You accent |
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
  ┌────────────┬────────────┬──────────┬───────────┬────────────┐
  │core:design │ core:input │ core:ops │ core:data │  core:fs   │
  │  system    │            │    │     │           │            │
  └────────────┴────────────┴────┼─────┴───────────┴────────────┘
                                 └──────────► core:fs
```

| Module | Owns | Depends on |
| --- | --- | --- |
| `app` | `MainActivity`, Hilt root, top-level Navigation 3 host, the operations foreground service, manifest permissions | every module |
| `core:designsystem` | Tokens generated from `tokens/*.json`, `HemisphereTheme`, shared components, the focus ring | nothing in the app |
| `core:input` | Last-input tracking (touch or gamepad), the gamepad map, Face buttons setting, key and axis handling | `core:designsystem` (hint glyphs only) |
| `core:fs` | The file-system abstraction (section 3), volumes, the local and SAF providers, MediaStore scans | nothing in the app |
| `core:ops` | The operations queue (section 4), conflicts, Trash (section 5) | `core:fs`, `core:data` |
| `core:data` | Room database, DataStore settings, their repositories | nothing in the app |
| `feature:browser` | Shell (Single and Dual), panes, file list, selection, clipboard tray, conflict sheet | all `core:*` |
| `feature:settings` | Settings screens (theme, accent, layout pin, Face buttons, Trash retention) | `core:designsystem`, `core:data`, `core:input` |
| `feature:trash` | The Trash screen: list, restore, empty | `core:designsystem`, `core:ops`, `core:input` |

Rules:

- A `feature:*` module never depends on another `feature:*` module. Features talk through
  `app` (navigation keys) or through a `core:*` module.
- `core:*` modules never depend on `feature:*`.
- `core:fs` and `core:ops` have no Compose dependency. They are tested on the JVM.
- New features (search, viewer, editor, archives) arrive as new `feature:*` modules. Archive
  formats arrive as a `core:archive` module (see ADR [0004](adr/0004-archive-libraries.md)).

## 3. `core:fs`: the file-system abstraction

The rest of the app never sees `java.io.File`, a raw path string, or a `content://` URI. It sees
`FsPath` and a `FileSystemProvider`. This is what lets SMB, SFTP and cloud plug in later without a
rewrite. See ADR [0006](adr/0006-file-system-abstraction.md).

```kotlin
// Sketch, not final code.
data class FsPath(val volume: VolumeId, val segments: List<String>)

interface FileSystemProvider {
    val capabilities: Capabilities          // atomicRename, trash, randomAccess, watch, ...
    suspend fun list(dir: FsPath): FsResult<List<Entry>>
    suspend fun stat(path: FsPath): FsResult<Entry>
    suspend fun openRead(path: FsPath): FsResult<Source>
    suspend fun openWrite(path: FsPath, mode: WriteMode): FsResult<Sink>
    suspend fun createDirectory(path: FsPath): FsResult<Entry>
    suspend fun rename(path: FsPath, newName: String): FsResult<Entry>
    suspend fun move(from: FsPath, to: FsPath): FsResult<Entry>   // same volume only
    suspend fun delete(path: FsPath): FsResult<Unit>              // permanent
    fun observe(dir: FsPath): Flow<Unit>                          // change signal, may be a no-op
}
```

- **`Volume`**: one storage root (internal, an SD card, a USB OTG drive; later an SMB share).
  `VolumeRepository` lists volumes and emits when one is mounted or removed.
- **Providers in the MVP**:
  - `LocalProvider`: direct file access where the storage permission allows it (all volumes on
    Android 11+, internal storage on Android 8 to 10). See ADR [0002](adr/0002-storage-permission.md).
  - `SafProvider`: `DocumentsContract` for SD card and OTG writes on Android 8 to 10, and for any
    tree the user grants by hand.
- **MediaStore** is not a provider. After a write, `core:fs` asks MediaStore to rescan the
  changed paths so other apps see them. Gallery thumbnails (rest of v1) read from MediaStore.
- **Errors** are values: `FsResult` is `Ok` or `Err(FsError)`. `FsError` is a sealed type
  (`NotFound`, `AccessDenied`, `NoSpace`, `AlreadyExists`, `ReadOnly`, `Unsupported`,
  `Io(cause)`). No exception crosses the `core:fs` boundary.
- **Capabilities** decide behavior, not the provider type. Example: Trash is offered only when
  the volume reports `trash = true`; otherwise delete is permanent and the dialog says so.
- **Threading**: providers are `suspend` and run their blocking work on an injected
  `IoDispatcher`.
- **Test double**: `InMemoryProvider` in `core:fs` test fixtures. Every `core:ops` test runs on
  it.

Known limit: Android 11+ blocks `Android/data` and `Android/obb` for every app, even with
all-files access. The browser shows these folders as locked, with a short explanation.

## 4. `core:ops`: the operations queue

Copy, move and delete run in a queue that we own, executed by a foreground service. See ADR
[0005](adr/0005-operations-queue.md).

```
UI ──enqueue(Job)──► OperationQueue ──persists──► Room (jobs, items, state)
                          │
                          ├── starts ──► OpsForegroundService (type dataSync, notification)
                          │                    │
                          │                    └── runs ──► JobRunner ──► FileSystemProvider
                          │
UI ◄── StateFlow<List<JobState>> (progress chip, queue sheet, notification)
```

- **Job**: one user action (copy 3 items to `/SD/ROMs`). **Item**: one file inside a job.
- **States**: `Queued → Running → (Paused | WaitingForConflict) → Done | Failed | Cancelled`.
- **One job runs at a time per destination volume**; jobs on different volumes may run in
  parallel (at most 2).
- **Fast paths**: a move inside one volume is a `move`/`rename`, never copy plus delete.
- **Copy** streams in chunks through `openRead`/`openWrite`, writes to a temp name, then renames.
  A failed or cancelled item leaves no half-written file under the final name.
- **Pause** stops at the next chunk boundary. Resume restarts the current item from the start.
- **Conflicts** pause the job in `WaitingForConflict`. The conflict sheet (or the notification)
  answers it; "apply to all" is stored on the job.
- **Process death**: on start, jobs that were `Running` become `Paused` and show in the progress
  chip with a Resume action. Nothing resumes silently.
- **Android 14+** needs the `dataSync` service type and its permission. **Android 15+** caps
  `dataSync` at 6 hours a day; when the system times out the service, running jobs move to
  `Paused` with a notification.

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
- Volumes without the `trash` capability (read-only, SAF-limited, later network) delete
  permanently, with the `danger` dialog and no Undo.
- Undo for delete, move and rename is a reverse job queued in `core:ops`.

## 6. State and UI

One-way data flow with `ViewModel`s.

```
 user input (touch or gamepad)
        │
        ▼
 onAction(PaneAction) ──► PaneViewModel ──► repositories / OperationQueue
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
- **Composables** are stateless below the screen level: they take state and lambdas.
  `remember` is for UI-only state (scroll, focus, animation).
- **Focus**: `core:input` exposes `LocalInputMode` (touch or gamepad). Every interactive
  component in `core:designsystem` reads it to show or hide the focus ring.

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
- System back and Ⓑ pop the **active pane's** stack first, then the app stack.
- Tabs (rest of v1) become one back stack per tab inside a pane. The MVP code keeps the pane
  stack behind one `PaneLocation` type so tabs do not change its callers.
- Dialogs and sheets are Navigation 3 scenes or local state, never separate activities.

## 8. Persistence

| Data | Store | Why |
| --- | --- | --- |
| Settings: theme, accent, Face buttons, density, Trash retention | Preferences DataStore | Small key-value data, observed as `Flow` |
| Layout pin per orientation | Preferences DataStore, key includes a device-class id | One backup restores correctly on a phone and a handheld |
| Per-folder view and sort | Room | Many rows, queried by path |
| Operations queue (jobs, items) | Room | Must survive process death |
| Trash index | Room (cache only) | Rebuilt from `.trashinfo` files |
| Bookmarks (rest of v1) | Room | |

- Room migrations are written and tested from the first release; no `fallbackToDestructiveMigration`.
- Android Auto Backup includes DataStore and the bookmarks table, and excludes the queue and the
  Trash index.

## 9. Dependency injection

Hilt. Each `core:*` module has one Hilt `@Module` that binds its interfaces. `app` is the only
`@HiltAndroidApp`. ViewModels use `@HiltViewModel` and are created with `hiltViewModel()`.
Dispatchers are injected (`@IoDispatcher`, `@DefaultDispatcher`), never hard-coded, so tests can
swap them.

## 10. Testing

| Layer | Tool | Runs where |
| --- | --- | --- |
| `core:fs`, `core:ops`, ViewModels | JUnit 4, `kotlinx-coroutines-test`, Turbine, `InMemoryProvider` | JVM (`./gradlew test`) |
| `core:data` | Room in-memory database, migration tests | JVM with Robolectric |
| Components and screens | Roborazzi screenshots: component × {light, dark, AMOLED} × {phone, handheld} × {touch, gamepad} | JVM with Robolectric |
| Providers against real storage | Instrumented tests | Emulator (`env-emulator`) |
| Gamepad on real handhelds | Manual checklist | Device (`env-device`) |

## 11. Other decisions

| Topic | Decision | ADR |
| --- | --- | --- |
| Storage permission | All-files access on Android 11+, legacy permission on 8 to 10 | [0002](adr/0002-storage-permission.md) |
| Distribution | GitHub Releases and F-Droid for the MVP, Play later | [0003](adr/0003-distribution.md) |
| Archive libraries | Commons Compress and libarchive, no unRAR | [0004](adr/0004-archive-libraries.md) |
| Privacy | No telemetry, no crash service, no internet permission in v1 | [0008](adr/0008-privacy.md) |
| Package ID | `io.github.ctorres345.hemisphere` | design-system.md, open items |
