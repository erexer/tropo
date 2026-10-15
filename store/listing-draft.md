# Tropo Store Draft

Status: owner review required; not submitted

- App name: Tropo
- Package: io.github.erexer.tropo
- Public developer: erexer
- Support: tropodev@gmail.com
- Category: Weather
- Ads: no
- Monetization: free, non-commercial; no subscriptions or in-app purchases
- Privacy URL: https://erexer.github.io/tropo/privacy.html (deployed and verified)

## Short Description

Weather, air quality and 10-day forecasts with an interactive hourly graph

## Full Description

Tropo brings current weather, air quality and a ten-day outlook together in a clear, interactive forecast.

See current conditions, today's high and low, feels-like temperature, humidity and wind. Follow temperature, precipitation chance, wind, UV and available air-quality readings across an hourly graph. Select a day or hour to explore the details, then return to the current hour with Now.

Save and reorder favorite cities, compare their current conditions and keep the weather you care about close. Search for a city without location permission, or choose an optional one-time current-location fix. Tropo does not track your position in the background.

Make the forecast your own with independent temperature, wind and precipitation units, light or dark appearance, and configurable current, daily and hourly readings. Explore sunrise, sunset, civil twilight and UV peak-hour windows when data are available.

Cached forecasts remain accessible offline, with saved-data labels and update times. A home-screen widget shows your selected city's temperature and wind. Report issues from Settings through an editable email draft, with copy options if no email app is available.

Tropo is free, with no ads, subscriptions, in-app purchases or user account.

Weather data by Open-Meteo. Air-quality data from Copernicus CAMS. Location data from GeoNames. Forecasts are informational and are not a substitute for official severe-weather alerts.

## Graphics

Upload-ready files are in `store/assets/`: app icon, feature graphic, and six
unaltered phone captures using Fahrenheit, mph, and inches. Review captures
before uploading. Store artwork does not change the blocked release-candidate
status documented in `store/release-runbook.md`.

## Console Checklist

- Verify developer identity/device/account requirements in Play Console
- Confirm unused versionCode before uploading each build
- Upload 512x512 icon, 1024x500 feature graphic, and at least two representative phone screenshots
- Complete content rating, target audience, ads, app access, Data Safety, and privacy URL forms
- No login required for review; explain optional location permission and city-search alternative
- Personal account created after 11/13/2023: recruit 12 continuously opted-in closed testers for at least 14 days, then apply for production access
- Use internal testing first; review generated split APKs and Play pre-launch report
- Do not submit until privacy URL is reachable and Data Safety answers are owner-approved
- Set the app price to Free and the contains-ads declaration to No in Play Console; do not create subscription or in-app products
- Retain Open-Meteo attribution and review free API usage limits before release