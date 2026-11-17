# Security policy

## Supported versions

Hemisphere is pre-MVP and has no release yet. Security fixes go to the `mvp` branch until the
first release; after that, to the latest release only.

## Report a vulnerability

Please report it privately. Do **not** open a public issue, PR or discussion.

1. Open [Report a vulnerability](https://github.com/ctorres345/Hemisphere/security/advisories/new)
   (repo **Security** tab → **Report a vulnerability**).
2. Say what is affected, how to reproduce it, and what an attacker could do.

Only the maintainers can see the report.

## What to expect

- We reply within 7 days.
- We keep you posted while we fix it, and agree with you on when to publish.
- We credit you in the advisory, unless you'd rather stay anonymous.

## Scope

In scope: the Hemisphere app and this repository's code, CI and build files. For example, a way
to read or change files outside what the user granted, a leaked secret, or a CI workflow that runs
untrusted code with write access.

Out of scope: bugs in Android or third-party libraries (report those upstream), and problems
that need a rooted device or physical access to an unlocked phone.
