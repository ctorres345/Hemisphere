---
status: proposed
---

# Ship the MVP on GitHub Releases and F-Droid; Google Play comes later

GitHub Releases and F-Droid need no store review and no permission declarations, and they suit
an open-source app. Play comes after the MVP, once two Play Console declarations are filed: all
files access (ADR 0002) and the `dataSync` foreground service type, with its description and
video (ADR 0005).

The USPTO/EUIPO search for "Hemisphere Explorer" runs before the first public release on any
channel, not only Play: trademark law applies wherever the app ships.

## Consequences

- Builds must be reproducible so F-Droid can publish our signed APK. The scaffold pins the JDK,
  Gradle and AGP versions and avoids build-time timestamps. Native code (libarchive, ADR 0004)
  also pins the NDK version and builds with path-independent flags (`-ffile-prefix-map`).
- Android developer verification (from September 2026 in some countries, worldwide in 2027)
  requires a verified developer, with the package name and signing keys registered, for apps
  installed outside Play on certified devices. Before the first public release, register
  `io.github.ctorres345.hemisphere` and the release key in the Android Developer Console. F-Droid
  must ship our signature through a reproducible build: we cannot register F-Droid's own key, so
  if reproducibility breaks, F-Droid builds would not install on certified devices.
- No proprietary libraries (Google Play Services, Firebase), or F-Droid flags the app with
  anti-features. This also matches ADR 0008.
- The release signing key lives outside the repo, in the secrets folder; the steps go in the
  runbook when the first release is cut.
