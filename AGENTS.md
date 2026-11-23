# Agent Guidelines for Mags (`KippenBoutske/mags2`)

## Git Workflow — Work Directly on `main`
- **Always work directly on the `main` branch.** Do **not** create feature branches, `cursor/*` branches, or pull requests unless the user explicitly asks for a branch or PR.
- Before making changes, pull the latest `origin/main` (`git checkout main && git pull --rebase origin main`).
- Commit and push your changes directly to `origin/main`.
- If a push is rejected because `origin/main` moved ahead, rebase onto `origin/main` and push again.

## Modules & Builds
- `:core` — shared Magister API models, parsers, grade calculator, and Wear sync payload.
- `:app` — Android phone app (Jetpack Compose + Material 3 Expressive + Glance widgets).
- `:wear` — Wear OS companion app + ProtoLayout Tile.
- `:desktop` — Compose Multiplatform desktop app (Windows MSI/EXE, Linux DEB).
- Keep `:app` and `:wear` signed with `app/keystore/mags-dev.jks` so in-place APK updates and Wear OS Data Layer sync keep working.
- Bump `versionCode` and `versionName` in `app/build.gradle.kts` and `wear/build.gradle.kts` when shipping app changes.
