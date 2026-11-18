# `:core:platform`

Small Android-side interfaces a screen needs directly, such as `ShareUriProvider`.

What this module owns and may depend on: [docs/architecture.md §2](../../docs/architecture.md#2-modules).
The module-graph check (`./gradlew checkModuleGraph`) fails on any other dependency.

`DeviceClassResolver` (HEM-32): Handheld if a built-in gamepad exists, else Tablet at 600 dp
smallest width, else Phone. `PlatformModule` computes it once and provides it to Hilt.
