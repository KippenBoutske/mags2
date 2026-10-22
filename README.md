# Mags

Onofficiële Android-app voor [Magister](https://www.magister.nl). Sneller en rustiger dan de officiële app: cijfers, rooster, huiswerk, berichten, een cijfercalculator en export naar een planningsassistent.

Mags is **geen** product van Iddink/Magister. Werkt met elke Magister-school. Alleen de ingelogde leerling ziet de eigen data.

## Wat zit erin

- **Cijfers** met gewogen gemiddelde per vak en *Wat moet ik halen?*
- **Rooster**, **huiswerk**, **berichten**, **afwezigheid**, **studiewijzers** en **opdrachten**
- **Cijfers exporteren** naar Plan Goon of als JSON
- Live schoolzoeken via Magister tenant-search
- Offline cache, pull-to-refresh, Material 3 Expressive
- Donker-warm thema (concept 1), Photos-stijl zwevende navbar (één indicator, wrap-content, ondoorzichtige `surfaceContainer` + scrim)
- Material You-schakelaar op login en onder Meer
- Lege/ladende skeletons in concept-1-stijl, 48 dp-targets, font-schaal 1.3×
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

3. Installeer `mags-debug.apk`. ApplicationId: `app.mags.debug`. Versie **0.1.10** (`targetSdk` 37).
4. Bij een crash: scherm **Mags is gestopt** → *Kopieer stacktrace*.

Zelf bouwen:

```bash
export ANDROID_HOME=/pad/naar/android-sdk
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug
# of de hele zelftest (unit + screenshots + optionele emulator-smoke):
./scripts/self-test.sh
```

Vereist JDK 17+, Android SDK platform 37. Screenshots landden in `app/build/outputs/self-test/screenshots`. Fake Magister (MockWebServer) zit in `:core:test` (`FakeMagisterFlowTest`).

## Inloggen

1. Zoek je school (live via `accounts.magister.net/challenges/tenant/search`). Of typ `school.magister.net`.
2. **Inloggen** opent een Custom Tab (beste voor school-SSO).
3. **In de app** (menu ⋯) is de WebView-fallback. **Voorbeeld** laadt testdata.
4. Magister stuurt je terug naar `m6loapp://oauth2redirect/`. Tokens gaan versleuteld in Android Keystore. Geen wachtwoord wordt opgeslagen.

School wisselen kan onder **Meer** (avatar rechtsboven). Dat logt je uit.

Als de officiële Magister-app ook geïnstalleerd is, kan Android vragen welke app de `m6loapp://`-link opent. Kies Mags.

OAuth (zelfde stroom als bekende open-source clients):

- Client `M6LOAPP`, PKCE S256, `acr_values=tenant:<host>`
- Scopes: `openid profile offline_access magister.mobile magister.ecs` (exacte M6LOAPP-string uit Discipulus / Friday / WalburgApp-V2). Geen extra scopes: `grades.read` / `email` gaf `invalid_scope`.

## Berichten

De officiële Magister-web-inbox (Angular-app `/magister-berichten/`) en WalburgApp-V2 (2026, zelfde M6LOAPP-scopes) lezen via `GET /api/berichten/postvakin/berichten`. Discipulus/Otarium beginnen bij `berichten/mappen/alle` en volgen daarna de folder-href; op Citadel gaf die maplijst HTTP 403.

Mags probeert daarom eerst `postvakin`, daarna `mappen/1/berichten`, de collectie, en pas daarna de maplijst. Als geen pad werkt, blijft Berichten leeg met een nette fout — paden en HTTP-codes zitten achter **Details**, nooit in de gewone UI. Hetzelfde per onderdeel voor cijfers en rooster: een 403 legt niet het hele scherm plat.

## Cijfers exporteren (Plan Goon)

Onder **Cijfers** of **Meer**: webhook-URL + bearer-key opslaan, daarna exporteren. **Deel JSON** is de fallback.

`Content-Type: application/json`  
`Authorization: Bearer <key>`

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

`attention` bevat vakken met gemiddelde **onder 6,0** of een dalende trend.

## Architectuur

- `:core` — endpoints, PKCE, parsers, calculator, fixtures, export
- `:app` — Compose UI, JSON-cache, OkHttp, Android Keystore, Plan Goon

REST-paden staan in `MagisterEndpoints.kt`.

## Licentie / verantwoordelijkheid

Alleen voor persoonlijk gebruik van je eigen Magister-account. Endpoints zijn ongedocumenteerd en kunnen breken. Geen garantie.
