---
status: proposed
---

# No telemetry, no crash-reporting service, and no internet permission in v1

A file manager sees everything on the device, so users must be able to trust it without reading
the code. v1 has no analytics, no crash service, and does not declare the `INTERNET` permission
at all, which anyone can check in the manifest. When the app crashes, the next start offers a
"Share crash report" action: the user sees the report and sends it themselves (email or a GitHub
issue).

## Consequences

- We learn about crashes only from users who choose to report them.
- v1.x network shares need `INTERNET`. Adding it is a visible change: the release notes say
  why, and the privacy policy still says no data leaves the device except to servers the user
  adds.
- No Firebase or Play Services (also needed for F-Droid, ADR 0003).
