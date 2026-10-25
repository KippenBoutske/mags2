# Mags

Onofficiële client voor [Magister](https://www.magister.nl). Kotlin Multiplatform + Compose Multiplatform: **één codebase, Android en Windows-desktop**.

Mags is **geen** product van Iddink/Magister. Werkt met elke Magister-school. Alleen de ingelogde leerling ziet de eigen data.

## Schrijven in common, shippen in beide

Toekomstige features en fixes horen in `:core`:

| Source set | Wat erin zit |
|---|---|
| `core/src/commonMain` | expect-API’s, haptics-model, gedeelde Compose-hooks |
| `core/src/androidAndDesktopMain` | Magister-API, DTOs, PKCE, repositories, CacheBlob, ViewModel, alle schermen |
| `core/src/androidMain` | Keystore-tokens, Custom Tabs, WebView, Material You, Pixel-haptics |
| `core/src/jvmMain` | Versleutelde bestanden in het user-profiel, desktop-login |

`:app` is de Android-schil (splash, WorkManager, Glance-widget, Live Update, crash-scherm).
`:desktop` is het Compose Desktop-venster. Beide roepen dezelfde `CitaRoot` / `CitaViewModel` aan.

```text
                 ┌──────────────────── :core ────────────────────┐
Android APK  ←── │ commonMain + androidAndDesktopMain + android  │
Windows exe  ←── │ commonMain + androidAndDesktopMain + jvm      │
                 └───────────────────────────────────────────────┘
```

## Wat zit erin

- **Cijfers** met Magister’s eigen GEM/SE als kopcijfer, weging uit kolom-info, zonder gemiddelde-kolommen of V/G/O; balk t.o.v. 10 met 5,5-streep, grafiek, *Wat moet ik halen?* en cijferblad
- **Rooster** ma–vr, **Huiswerk**, **Berichten** (alleen lezen), **afwezigheid**, **studiewijzers**, **opdrachten**
- **Vandaag**-startscherm, cijfercalculator, Plan Goon JSON-export
- Offline JSON-cache (`CacheBlob`), pull-to-refresh, Material 3 Expressive
- Android: meldingen (WorkManager), Live Update (Android 16+), Glance-widget **Volgende les**, crash-scherm
- Nederlandse UI

## Inloggen

1. Zoek je school.
2. **Android:** *Inloggen* opent een Custom Tab. Magister stuurt je terug naar `m6loapp://oauth2redirect/`. Tokens gaan AES-GCM via Android Keystore. Menu ⋯ → *In de app* is de WebView-fallback. *Voorbeeld* laadt testdata.
3. **Desktop:** Magister accepteert alleen de officiële redirect `m6loapp://oauth2redirect/` (client `M6LOAPP`). Een `http://127.0.0.1:<port>`-callback werkt daarom niet. De desktop-app opent een ingebedde JavaFX-WebView die die custom scheme onderschept. Als JavaFX ontbreekt: Magister in de systeembrowser + de `m6loapp://…`-URL plakken. Tokens staan AES-GCM in `%APPDATA%/Mags` (Windows) of `~/.mags` (Linux/macOS).

OAuth: client `M6LOAPP`, PKCE S256, `acr_values=tenant:<host>`, scopes `openid profile offline_access magister.mobile magister.ecs`.

## Bouwen

Vereist JDK 17+ (21 is ok), Android SDK platform 37.

```bash
export ANDROID_HOME=/pad/naar/android-sdk
# Android
./gradlew :core:jvmTest :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease

# Desktop (runnable distributie voor dit OS)
./gradlew :desktop:createDistributable :desktop:packageDistributionForCurrentOS

# Alleen runnen
./gradlew :desktop:run
```

**Windows-installer (MSI)** — `packageMsi` gebruikt WiX en draait alleen op Windows:

```bat
gradlew.bat :desktop:packageMsi
```

Cross-build van een MSI op Linux lukt niet (WiX ontbreekt). Op Linux levert `createDistributable` een runnable map; `packageDeb` heeft `fakeroot` nodig (`sudo apt install fakeroot`). Op macOS: DMG via `packageDistributionForCurrentOS`.

GitHub Actions (`.github/workflows/desktop-msi.yml`) bouwt het MSI op `windows-latest` bij elke push naar `main`, bij tags `v*` (upload naar een GitHub Release) en via *Run workflow*. Dezelfde workflow bouwt de Android-APKs op Ubuntu.

ApplicationId blijft `app.mags` (debug: `app.mags.debug`) zodat de Android-app in-place update. Versie **0.2.4**.

Release-APK is getekend met de lokale dev-keystore in `app/keystore/` — **niet** voor Play.

## Architectuur

- `:core` — gedeelde Magister-client (Ktor), PKCE, parsers, CacheBlob, repositories, ViewModel, Compose-schermen
- `:app` — Android-applicationId, splash, widgets, WorkManager, notificaties
- `:desktop` — Compose Desktop-venster, `packageMsi` / `packageDistributionForCurrentOS`

REST-paden staan in `MagisterEndpoints.kt`.

## Licentie / verantwoordelijkheid

Alleen voor persoonlijk gebruik van je eigen Magister-account. Endpoints zijn ongedocumenteerd en kunnen breken. Geen garantie.
