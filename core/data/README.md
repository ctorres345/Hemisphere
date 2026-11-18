# `:core:data`

The `user.db` Room database, DataStore settings and their repositories.

What this module owns and may depend on: [docs/architecture.md §2](../../docs/architecture.md#2-modules).
The module-graph check (`./gradlew checkModuleGraph`) fails on any other dependency.

Settings (HEM-32): `SettingsRepository` over a Preferences DataStore (`settings.preferences_pb`). The
layout pin key includes the device class, `layout_pin_<phone|handheld|tablet>_<portrait|landscape>`.
`DeviceClass` is injected, not computed here. A corrupt file falls back to defaults.
