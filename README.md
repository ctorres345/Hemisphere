# Hemisphere

An open-source Android file manager for phones and Android gaming handhelds.
Store name: **Hemisphere Explorer**.

> Two halves, one mind: one pane on a phone, two on a handheld.

- **Single and Dual layouts.** One pane on a narrow screen, two side by side on a wide one.
- **Gamepad first.** Every screen works with a D-pad and gamepad buttons, with a visible focus.
- **Kotlin and Jetpack Compose**, Android 8.0 (API 26) and up.

## Status

Pre-MVP. There is no app to install yet. The repo holds the design spec and the project setup;
the Android code comes next.

Until the MVP ships, work lands on the `mvp` branch. `main` gets the MVP as one squash-merge.

## Design

The design system spec is [`docs/design-system.md`](docs/design-system.md): layouts, input,
colors, type, spacing and view modes.

## Contributing

Read [`CONTRIBUTING.md`](CONTRIBUTING.md) first. Every commit needs a DCO sign-off
(`git commit -s`). Please follow the [Code of Conduct](CODE_OF_CONDUCT.md).

Found a security problem? Report it privately, as described in [`SECURITY.md`](SECURITY.md).

## License

Each file is covered by one of these licenses:

| What | License | File |
| --- | --- | --- |
| The app: source code, build files, resources and everything not listed below | GNU GPL v3.0 or later | [`LICENSE`](LICENSE) |
| Documentation: `docs/` and the top-level `.md` files (except the Code of Conduct), plus design token files (the `tokens/` folder, once it exists) | MIT | [`LICENSE-MIT`](LICENSE-MIT) |
| [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md), adapted from the Contributor Covenant 2.1 | CC BY 4.0 | [creativecommons.org](https://creativecommons.org/licenses/by/4.0/) |

When a file is not listed in the last two rows, the GPLv3 applies. The MIT files can be reused in
other projects, including closed-source ones; the app cannot.

Copyright (C) 2026 Cesar Torres. The app is free software under the GNU GPL, version 3 or (at
your option) any later version, with no warranty. See [`LICENSE`](LICENSE).
