# `:core:model`

Plain Kotlin value types shared by every layer: `FsPath`, `EntryKey`, `Entry`, `FsResult`, `Volume`, `VolumeLimits`, `Capabilities`, the v1 path string form, job states. A pure Kotlin/JVM module.

What this module owns and may depend on: [docs/architecture.md §2](../../docs/architecture.md#2-modules).
The module-graph check (`./gradlew checkModuleGraph`) fails on any other dependency.
