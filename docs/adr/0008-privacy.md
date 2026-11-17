---
status: proposed
---

# No telemetry, no crash-reporting service, and no internet permission in v1

A file manager sees everything on the device, so users must be able to trust it without reading
the code. v1 has no analytics, no crash service, and does not declare the `INTERNET` permission
at all, which anyone can check in the manifest. When the app crashes, the next start offers a
"Share crash report" action: the user sees the report and sends it themselves (email or a GitHub
issue).

Android backup is the one other path off the device. Cloud backup (Google Drive) holds only the
settings, which contain no file names or paths. Bookmarks and per-folder views, which contain
paths, move only by device-to-device transfer (architecture §8).

## Consequences

- Release builds never log file names or paths at info level or higher, so a shared log or crash
  report does not expose them.

- We learn about crashes only from users who choose to report them.
- v1.x network shares need `INTERNET`. Adding it is a visible change: the release notes say
  why, and the privacy policy still says no data leaves the device except to servers the user
  adds.
- No Firebase or Play Services (also needed for F-Droid, ADR 0003).
