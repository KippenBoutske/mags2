# Mags

Onofficiële Android-app voor [Magister](https://www.magister.nl). Sneller en rustiger dan de officiële app: cijfers, rooster, huiswerk, berichten, een cijfercalculator en export naar een planningsassistent.

Mags is **geen** product van Iddink/Magister. Werkt met elke Magister-school. Alleen de ingelogde leerling ziet de eigen data.

## Wat zit erin

- **Cijfers** met Magister’s eigen GEM/SE als kopcijfer, weging uit kolom-info, zonder gemiddelde-kolommen of V/G/O; balk t.o.v. 10 met 5,5-streep, grafiek, *Wat moet ik halen?* en cijferblad
- **Rooster** met tussenuren, nu-lijn in de huidige les, en lesblad
- **Huiswerk** gegroepeerd per dag, *Verlopen* ingeklapt, Toets in de meta-regel, swipe-naar-klaar met herstel
- **Berichten** (alleen lezen), **afwezigheid**, **studiewijzers** en **opdrachten**
- Startscherm-snelkoppelingen naar Vandaag, Rooster, Cijfers en Huiswerk
- **Deel**-icoon rechtsboven op Cijfers: JSON naar Meer → Geavanceerd, anders het Android-deelblad
- Adaptive icoon + Android 13-monochroom + SplashScreen-animatie
- Live schoolzoeken
- Offline cache, pull-to-refresh, Material 3 Expressive (shared elements, spring-lijst, morphing `LoadingIndicator`)
- Donker-warm thema (concept 1), Photos-stijl zwevende navbar
- Material You onder Meer
- 48 dp-targets, font-schaal 1.3×
- Baseline profile + ProfileInstaller
- Meldingen bij nieuwe cijfers en roosterwijzigingen (WorkManager)
- Startscherm-widget **Volgende les** (Glance)
- Crash-scherm met kopieerbare stacktrace
- Nederlandse UI

## Installeren (debug-APK)

1. Zet *Onbekende bronnen* / *Installeren uit deze bron* aan.
2. Plak de split-parts weer aan elkaar:

```bash
cat mags-debug.apk.part-aa mags-debug.apk.part-ab > mags-debug.apk
# of, als de parts 00/01 heten:
# cat mags-debug.apk.part-00 mags-debug.apk.part-01 > mags-debug.apk
sha256sum -c mags-debug.apk.sha256
```

3. Installeer `mags-debug.apk`. ApplicationId: `app.mags.debug`. Versie **0.1.15** (`targetSdk` 37).
4. Er is ook een **release-APK** (`mags-release.apk`, applicationId `app.mags`) met R8. Die is getekend met de lokale dev-keystore in `app/keystore/` — **niet** voor Play.
5. Bij een crash: scherm **Mags is gestopt** → *Kopieer stacktrace*.

Zelf bouwen:

```bash
export ANDROID_HOME=/pad/naar/android-sdk
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease
# of de hele zelftest (unit + screenshots + optionele emulator-smoke):
./scripts/self-test.sh
```

Vereist JDK 17+, Android SDK platform 37. Screenshots landden in `app/build/outputs/self-test/screenshots`. Fake Magister (MockWebServer) zit in `:core:test` (`FakeMagisterFlowTest`).

## Inloggen

1. Zoek je school. **Inloggen** opent een Custom Tab.
2. **In de app** (menu ⋯) is de WebView-fallback. **Voorbeeld** (onderaan of ⋯) laadt testdata.
3. Magister stuurt je terug naar `m6loapp://oauth2redirect/`. Tokens gaan versleuteld in Android Keystore.

School wisselen kan onder **Meer** (avatar rechtsboven). Dat logt je uit.

OAuth (zelfde stroom als bekende open-source clients):

- Client `M6LOAPP`, PKCE S256, `acr_values=tenant:<host>`
- Scopes: `openid profile offline_access magister.mobile magister.ecs`. Geen extra scopes.

## Berichten

Mags probeert eerst `postvakin`, daarna `mappen/1/berichten`, de collectie, en pas daarna de maplijst. Als geen pad werkt, blijft Berichten leeg — paden en HTTP-codes zitten achter **Details**. Alleen lezen: M6LOAPP heeft geen schrijfrechten voor nieuwe berichten.

## Cijfers exporteren (Plan Goon)

Op **Cijfers**: deel-icoon naast de avatar. Stuurt JSON naar het adres onder **Meer → Geavanceerd** als dat gezet is; anders het Android-deelblad. Geen adres is hardgecodeerd.

`Content-Type: application/json`  
`Authorization: Bearer <sleutel>` (alleen als er een sleutel is)

```json
{
  "source": "magister-app",
  "exported_at": "2026-10-21T22:00:00Z",
  "student": {
    "name": "Jens de Vries",
    "class": "5VWO-A",
    "study": "VWO",
    "school": "Citadel College",
    "tenant": "citadel.magister.net"
  },
  "subjects": [],
  "attention": []
}
```

Elk vak: `name`, `average`, `grades[]` met `value`, `weight`, `description`, `date`. `attention` bevat vakken onder 6,0 of een dalende trend.

## Architectuur

- `:core` — endpoints, PKCE, parsers, calculator, fixtures, export
- `:app` — Compose UI, JSON-cache, OkHttp, Android Keystore, Plan Goon

REST-paden staan in `MagisterEndpoints.kt`.

## Licentie / verantwoordelijkheid

Alleen voor persoonlijk gebruik van je eigen Magister-account. Endpoints zijn ongedocumenteerd en kunnen breken. Geen garantie.
