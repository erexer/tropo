# Phone Screenshots

Six real, unaltered app captures from candidate 3 on a 1080 x 1920 Android
emulator. English display; Fahrenheit, mph, and inches. Weather readings are
live provider data at capture time, not mock forecasts. Manual cities avoid
disclosing a real device position. Original AVD data is preserved with read-only
emulation. Screenshots are store artwork, not a production-readiness sign-off.

Upload PNGs in this order:

1. `01-current-weather.png`: current conditions and ten-day outlook
2. `02-forecast.png`: interactive forecast graph and overlays
3. `03-hourly.png`: hourly forecast details
4. `04-locations.png`: saved cities and current weather
5. `05-settings.png`: reporting, units, and appearance
6. `06-dark-weather.png`: dark appearance

Do not upload this README. Review screenshots before submitting the listing.

Capture note: candidate 3 crashed during search/favorite interaction with
`IllegalStateException: visitAncestors called on an unattached node`. Crash
evidence is retained outside the upload directory in the temporary capture
session. These stable-screen captures do not establish that the crash is fixed;
resolve it before production. No app runtime code was changed for the artwork.