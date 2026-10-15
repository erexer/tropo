# Data Safety Draft

Owner review required. This is not a submitted declaration or legal determination.
Tropo is free with no ads, subscriptions, or in-app purchases.

## Actual Flows

| Data | Handling | Purpose / choice |
| --- | --- | --- |
| Precise device position | Optional one-shot foreground fix; coordinates sent to Open-Meteo; cached locally and eligible for Android backup | Weather/air-quality functionality when current location is chosen |
| Approximate position / chosen city coordinates | Sent to weather providers; saved/cached locally and eligible for backup | Weather functionality; manual city search works without location permission |
| Search terms | Sent to Open-Meteo geocoding | City search |
| IP/request metadata | Received/logged by Open-Meteo; provider states logs can include coordinates and remain up to 90 days | Service operation/abuse prevention; do not claim ephemeral-only processing |
| Favorites, forecasts, units/display preferences | Local Room/DataStore; Android cloud backup/device transfer enabled | User features and restoration; platform controls backup |
| Support email/message | Optional reviewed email draft from Settings includes app version/code and Android version/API; no automatic coordinates, identifiers, favorites, or logs; user chooses whether to send or copy | Support; handled by the chosen email/clipboard service |

## Review Before Completing Console

- Assess location collection and third-party sharing classifications from actual provider arrangements; a service-provider exemption must not be assumed
- Precise device location is optional; manual search remains available
- Requests to Open-Meteo use HTTPS; system geocoding/backup are provider-controlled
- No app account, ads, analytics SDK, advertising identifier, billing SDK, or developer-operated weather backend
- Removing a favorite only unpins it; clearing Android app storage removes local records, while provider logs/backups may remain
- Do not answer "no data collected" simply because Tropo has no developer backend
- Account-deletion workflow is not applicable to a nonexistent app account; support/provider retention still needs truthful disclosure