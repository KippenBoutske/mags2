# Cita

Onofficiële Android-app voor [Magister](https://www.magister.nl) (Iddink). Gebouwd voor een 5 VWO-leerling van het **Citadel College** in Lent/Nijmegen. Doel: sneller en rustiger dan de officiële app, met een cijfercalculator en export naar een planningsassistent.

Cita is **geen** product van Iddink/Magister. Alleen de ingelogde leerling ziet de eigen data.

## Wat zit erin

- **Cijfers** met gewogen gemiddelde per vak en *Wat moet ik halen?*
- **Rooster** (afspraken, inclusief uitval)
- **Huiswerk** (uit lesinhoud + opdrachten, lokaal afvinken, live-PUT als het kan)
- **Berichten** lezen
- **Cijfers exporteren** naar Plan Goon (Grok Bot-webhook) of als JSON delen
- Offline JSON-cache, pull-to-refresh, **Material 3 Expressive** (`material3:1.5.0-beta02`: `MaterialExpressiveTheme`, `MotionScheme.expressive()`, `ButtonGroup`, `LoadingIndicator`, `HorizontalFloatingToolbar`, `ShortNavigationBar`), dynamische kleur, donkere modus
- Nederlandse UI

## School / tenant

Gecontroleerd op 21 oktober 2026:

| Host | Status |
| --- | --- |
| `citadel.magister.net` | Live Magister-tenant (HTTP 200) |
| `citadelcollege.magister.net` | **Bestaat niet** (DNS lost niet op) |

De app start met Citadel College / `citadel.magister.net`. Onder inloggen kun je een andere school of hostname kiezen (`school` wordt `school.magister.net`).

## Installeren (debug-APK)

1. Zet *Onbekende bronnen* / *Installeren uit deze bron* aan.
2. Installeer `artifacts/cita-debug.apk` (of `app/build/outputs/apk/debug/app-debug.apk` na een lokale build).
3. ApplicationId van de debug-build: `nl.cita.magister.debug`.

Zelf bouwen:

```bash
export ANDROID_HOME=/pad/naar/android-sdk
./gradlew :core:test :app:assembleDebug
```

Vereist JDK 17+, Android SDK platform 36 en build-tools 35.

## Inloggen

Magister heeft geen officiële publieke leerling-API. Cita volgt dezelfde OAuth/OIDC-stroom als bekende open-source clients (o.a. [MagisterAPIKt](https://github.com/Tiebe/MagisterAPIKt) / Otarium):

- Authorization: `https://accounts.magister.net/connect/authorize` (discovery geverifieerd)
- Token: `https://accounts.magister.net/connect/token`
- Client: `M6LOAPP`
- Redirect: `m6loapp://oauth2redirect/`
- Scope: `openid profile email offline_access`
- PKCE S256 + `acr_values=tenant:<host>`

Stappen:

1. Kies je school.
2. **Inloggen** opent een Custom Tab (beste voor Microsoft/Entra-SSO).
3. **In de app** gebruikt een WebView als fallback.
4. Magister stuurt je terug naar `m6loapp://…`. Cita wisselt de code om voor tokens.
5. Tokens staan in **EncryptedSharedPreferences**. Geen wachtwoord wordt opgeslagen.
6. **Voorbeeld** laadt duidelijk gemarkeerde 5 VWO-fixtures (geen netwerk, geen credentials).

Als de officiële Magister-app ook geïnstalleerd is, kan Android vragen welke app de `m6loapp://`-link opent. Kies Cita.

## Cijfers exporteren (Plan Goon)

Onder **Cijfers** of **Meer** staat **Cijfers exporteren**. Dat is een `HTTPS POST` naar een Grok Bot-routine-webhook, zodat Plan Goon weet welke vakken aandacht nodig hebben.

In **Meer**:

1. Plak de webhook-URL (`https://…`).
2. Plak de bearer-key. Die wordt versleuteld bewaard en **nooit gelogd**.
3. Tik *Webhook opslaan*, daarna *Cijfers exporteren*.
4. HTTP **200** = geaccepteerd; je ziet een snackbar bij succes of fout.
5. **Deel JSON** / **Kopieer / deel JSON** is de fallback zonder webhook.

### Payload-schema

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
  "subjects": [
    {
      "code": "wi",
      "name": "Wiskunde B",
      "average": 5.9,
      "weighted": true,
      "grades": [
        { "value": 6.4, "weight": 3.0, "description": "Differentiaalrekening", "date": "2025-09-18T10:00:00.0000000Z" }
      ]
    }
  ],
  "attention": [
    {
      "code": "wi",
      "name": "Wiskunde B",
      "reason": "gemiddelde 5,9 ligt onder de 6,0",
      "average": 5.9
    }
  ]
}
```

`attention` bevat vakken met gemiddelde **onder 6,0** of een **dalende trend** (laatste cijfer ≥ 0,3 lager dan het eerdere gemiddelde, of drie opeenvolgende dalende cijfers).

## Architectuur

- `:core` — JVM, geïsoleerde endpoints (`MagisterEndpoints`), PKCE, parsers, calculator, fixtures, export-schema
- `:app` — Compose UI, Room-cache, OkHttp, EncryptedSharedPreferences, Plan Goon-client (apart van Magister-auth)

REST-paden (onofficieel, kunnen wijzigen) staan alleen in `MagisterEndpoints.kt`.

## Wat is geverifieerd vs. ongetest

**Geverifieerd**

- OIDC-discovery van `accounts.magister.net` (authorize/token/PKCE S256)
- Tenant `citadel.magister.net` leeft; `citadelcollege.magister.net` niet
- JSON-parsers, gewogen gemiddelde, calculator en export-payload tegen fixtures (unit tests)
- Debug-APK-build in deze omgeving (als `assembleDebug` slaagt)

**Ongetest tegen de live Magister-API** (geen leerling-credentials in deze omgeving)

- Complete login inclusief school-SSO
- Token-refresh
- Cijfers / rooster / huiswerk-PUT / berichten
- Plan Goon-webhook (geen echte bot-URL hier)

Gebruik **Voorbeeld** om de UI te zien zonder login.

## Licentie / verantwoordelijkheid

Alleen voor persoonlijk gebruik van je eigen Magister-account. Endpoints zijn ongedocumenteerd en kunnen breken. Geen garantie.
