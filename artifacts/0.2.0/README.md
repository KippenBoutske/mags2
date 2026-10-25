# Mags 0.2.0 build artifacts

- `mags-debug.apk` — `app.mags.debug`
- `mags-release.apk` — `app.mags` (dev keystore)
- Desktop runnable map (Linux, this CI): produced by `./gradlew :desktop:createDistributable` at `desktop/build/compose/binaries/main/app/Mags`. Not checked in (bundled JRE).
- Windows MSI: on a Windows machine run `gradlew.bat :desktop:packageMsi`.
