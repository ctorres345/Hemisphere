<!-- Created by nousfx_sync. This file is yours: write the repo's rules here. Every tool reads it. -->

# Agent rules

Hemisphere (store name: Hemisphere Explorer) is an open-source (GPLv3) Android file manager in
Kotlin and Jetpack Compose, built for phones and Android gaming handhelds.

## Build and test

JDK 17 (Gradle downloads it when missing; `gradle/gradle-daemon-jvm.properties`). Android SDK 36.

- Build: `./gradlew assembleDebug`
- Everything CI and the review gate run: `./gradlew check` (unit tests, Konsist layer rules,
  Android Lint with slack/compose-lints, ktlint, detekt with type resolution, the module-graph
  check, the no-`INTERNET` manifest check and build-logic's own tests)
- Format: `./gradlew ktlintFormat`

The review gate runs `[check]` from `.nousfx/project.toml`. Build conventions live in
`build-logic/` (convention plugins); the allowed module graph is `ModuleGraphRules.kt` there, and a
new module is added to it and to `docs/architecture.md` §2 in the same PR. Detekt rules are in
`config/detekt/`.

## Where things are

- Design system spec: `docs/design-system.md`. It is the visual source for every UI ticket until
  a prototype is approved. Use tokens only: no raw colors, sizes or font values in UI code.
- Architecture: `docs/architecture.md` (modules, `core:fs`, the job queue, Trash, state,
  layers, navigation, persistence, DI). Decisions and their reasons: `docs/adr/`. Domain words:
  `GLOSSARY.md`.
- Code rules: `docs/code-guidelines.md`. Read it before writing Kotlin.
- Tickets: Jira project `HEM`. Template and labels: `docs/agents/`.
- Branches: `cesar/<HEM-KEY>` off `mvp` (the default branch until the MVP ships); screenshots on `cesar/<HEM-KEY>-shots`. Until the MVP
  ships, ticket PRs target `mvp`, not `main` (see `docs/agents/issue-tracker.md`).

## Rules

- Gamepad and D-pad input is first-class: every interactive component needs a visible focus state.
- Every UI change is checked in light, dark and AMOLED, on phone (Single) and handheld (Dual).
- No Claude co-author trailer on commits and no generated-by line in PR bodies.
