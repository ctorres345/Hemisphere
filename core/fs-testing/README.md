# `:core:fs-testing`

`InMemoryProvider`, `FakeVolumeRepository` and the abstract `FileSystemProviderContract` suite that every
`FileSystemProvider` must pass. A data-layer module (`core:fs`, `core:jobs`, `core:data`) may take it as
`testImplementation` without pulling in `core:testing`; `core:testing` re-exports it as `api`.
Used from test source sets only.

What this module owns and may depend on: [docs/architecture.md §2](../../docs/architecture.md#2-modules).
The module-graph check (`./gradlew checkModuleGraph`) fails on any other dependency.
