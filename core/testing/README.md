# `:core:testing`

Test fakes for every data-layer interface (`FakeJobQueue`, fake repositories), plus `core:fs-testing` (`InMemoryProvider`, `FakeVolumeRepository`) re-exported as `api`. Used from test source sets only.

What this module owns and may depend on: [docs/architecture.md §2](../../docs/architecture.md#2-modules).
The module-graph check (`./gradlew checkModuleGraph`) fails on any other dependency.
