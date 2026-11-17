---
status: proposed
---

# Ship the MVP on GitHub Releases and F-Droid; Google Play comes later

GitHub Releases and F-Droid need no store review, no all-files-access declaration and no
trademark clearance, and they suit an open-source app. Play comes after the MVP, once the
USPTO/EUIPO search for "Hemisphere Explorer" is done and the all-files-access declaration is
filed.

## Consequences

- Builds must be reproducible so F-Droid can publish our signed APK. The scaffold pins the JDK,
  Gradle and AGP versions and avoids build-time timestamps.
- No proprietary libraries (Google Play Services, Firebase), or F-Droid flags the app with
  anti-features. This also matches ADR 0008.
- The release signing key lives outside the repo, in the secrets folder; the steps go in the
  runbook when the first release is cut.
