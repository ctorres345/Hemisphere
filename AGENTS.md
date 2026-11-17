<!-- Created by nousfx_sync. This file is yours: write the repo's rules here. Every tool reads it. -->

# Agent rules

Hemisphere (store name: Hemisphere Explorer) is an open-source (GPLv3) Android file manager in
Kotlin and Jetpack Compose, built for phones and Android gaming handhelds.

## Build and test

No Android scaffold yet. When it lands, the commands are:

- Build: `./gradlew assembleDebug`
- Test: `./gradlew test`
- Lint: `./gradlew lint`

Add the test command to `.nousfx/project.toml` under `[check]` in the same PR as the scaffold.

## Where things are

- Design system spec: `docs/design-system.md`. It is the visual source for every UI ticket until
  a prototype is approved. Use tokens only: no raw colors, sizes or font values in UI code.
- Tickets: Jira project `HEM`. Template and labels: `docs/agents/`.
- Branches: `cesar/<HEM-KEY>` off `main`; screenshots on `cesar/<HEM-KEY>-shots`. Until the MVP
  ships, ticket PRs target `mvp`, not `main` (see `docs/agents/issue-tracker.md`).

## Rules

- Gamepad and D-pad input is first-class: every interactive component needs a visible focus state.
- Every UI change is checked in light, dark and AMOLED, on phone (Single) and handheld (Dual).
- No Claude co-author trailer on commits and no generated-by line in PR bodies.
