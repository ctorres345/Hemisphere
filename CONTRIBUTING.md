# Contributing to Hemisphere

Thanks for helping. Hemisphere is pre-MVP, so things move fast. Small, focused PRs land best.

## Where work is tracked

| You want to | Do this |
| --- | --- |
| Report a bug or suggest a feature | Open a GitHub issue. A maintainer copies it to Jira if it is accepted. |
| Report a security problem | Do **not** open an issue. Follow [`SECURITY.md`](SECURITY.md). |
| Pick something to work on | Ask on an issue first, so two people don't build the same thing. |

The maintainers plan and track work in Jira project `HEM`. GitHub issues are where outside
contributors start a conversation; Jira is the source of truth for what is planned.

## Branches

- Fork the repo and branch from `mvp`. Any branch name works in your fork.
- **Until the MVP ships, open PRs against `mvp`, not `main`.** `main` gets the MVP as one
  squash-merge.
- `main` and `mvp` are protected: changes go through a PR, and force pushes are blocked.

## Pull requests

Fill in the [PR template](.github/pull_request_template.md). Name the Jira key or the GitHub
issue the PR closes.

### Screenshots for UI changes

Every UI change needs screenshots in the PR, before and after, for each of these:

| Theme | Phone (Single layout) | Handheld (Dual layout) |
| --- | --- | --- |
| Light | touch + gamepad focus | touch + gamepad focus |
| Dark | touch + gamepad focus | touch + gamepad focus |
| AMOLED | touch + gamepad focus | touch + gamepad focus |

Use a screen recording instead for motion, flows, or a bug that changes over time. Non-UI PRs
write "N/A".

UI code uses design tokens only: no raw colors, sizes or font values. See
[`docs/design-system.md`](docs/design-system.md).

## Checks

CI runs [gitleaks](https://github.com/gitleaks/gitleaks) on every PR to catch leaked secrets
(keys, tokens, passwords). To run the same scan locally:

```sh
gitleaks git -v
```

CI also builds the app and runs every test and lint check on every PR. You need JDK 17 (Gradle
downloads it if it is missing) and the Android SDK with API 36. To run the same checks locally:

```sh
./gradlew assembleDebug   # build the debug APK
./gradlew check           # unit tests, architecture rules, Android Lint, ktlint and detekt
./gradlew ktlintFormat    # fix formatting
```

## Sign your commits (DCO)

Every commit must be signed off under the [Developer Certificate of Origin](https://developercertificate.org/).
The sign-off says you wrote the change, or have the right to submit it under the project's
license. It is a line at the end of the commit message:

```
Signed-off-by: Your Name <you@example.com>
```

Add it with `git commit -s`. The name and email must match your Git author. To sign off commits
you already made:

```sh
git remote add upstream https://github.com/ctorres345/Hemisphere.git  # once
git fetch upstream
git rebase --signoff upstream/mvp
git push --force-with-lease
```

Maintainers check the sign-offs before merging; a PR with unsigned commits waits until they are
signed.

## Licenses

By contributing you agree that your contribution is licensed the same way as the files it
touches: GPLv3 or later for the app, MIT for documentation and design tokens. The
[README](README.md#license) says which covers what.

Don't copy code from closed-source apps (such as MiXplorer). Code from GPL-compatible projects is
welcome; keep its copyright notice and say where it came from in the PR.

## Conduct

Everyone here follows the [Code of Conduct](CODE_OF_CONDUCT.md).
