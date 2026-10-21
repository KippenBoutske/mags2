# Mags

Onofficiële Android-app voor [Magister](https://www.magister.nl). Sneller en rustiger dan de officiële app: cijfers, rooster, huiswerk, berichten, een cijfercalculator en export naar een planningsassistent.

Mags is **geen** product van Iddink/Magister. Werkt met elke Magister-school. Alleen de ingelogde leerling ziet de eigen data.

## Wat zit erin

- **Cijfers** met gewogen gemiddelde per vak en *Wat moet ik halen?*
- **Rooster**, **huiswerk** en **berichten**
- **Cijfers exporteren** naar Plan Goon of als JSON
- Live schoolzoeken via Magister tenant-search
- Offline cache, pull-to-refresh, Material 3 Expressive
- Crash-scherm met kopieerbare stacktrace
- Nederlandse UI

## Installeren (debug-APK)

1. Zet *Onbekende bronnen* / *Installeren uit deze bron* aan.
2. Plak de split-parts weer aan elkaar:

```bash
cat mags-debug.apk.part-aa mags-debug.apk.part-ab > mags-debug.apk
sha256sum -c mags-debug.apk.sha256
```

3. Installeer `mags-debug.apk`. ApplicationId: `app.mags.debug`. Versie **0.1.5** (`targetSdk` 37).
4. Bij een crash: scherm **Mags is gestopt** → *Kopieer stacktrace*.

Zelf bouwen:

```bash
export ANDROID_HOME=/pad/naar/android-sdk
./gradlew :core:test :app:assembleDebug
```

Vereist JDK 17+, Android SDK platform 37.

## Inloggen

1. Zoek je school (live via `accounts.magister.net/challenges/tenant/search`). Of typ `school.magister.net`.
2. **Inloggen** opent een Custom Tab (beste voor school-SSO).
3. **In de app** (menu ⋯) is de WebView-fallback. **Voorbeeld** laadt testdata.
4. Magister stuurt je terug naar `m6loapp://oauth2redirect/`. Tokens gaan versleuteld in Android Keystore. Geen wachtwoord wordt opgeslagen.

School wisselen kan onder **Meer**. Dat logt je uit.

Als de officiële Magister-app ook geïnstalleerd is, kan Android vragen welke app de `m6loapp://`-link opent. Kies Mags.

OAuth (zelfde stroom als bekende open-source clients):

- Client `M6LOAPP`, PKCE S256, `acr_values=tenant:<host>`
- Scopes: `openid profile offline_access magister.mobile magister.ecs` (exacte M6LOAPP-string uit Discipulus / Friday / WalburgApp-V2)

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
