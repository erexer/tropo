# Tropo

Tropo is free, with no ads, subscriptions, or in-app purchases. Weather requests
go directly to Open-Meteo's free non-commercial APIs; no developer-hosted backend
or paid API key is required. Provider usage limits and attribution requirements
still apply.

Tropo is an Android weather app with current conditions and ten-day forecasts
from Open-Meteo. The graph spans the full screen width and shows at least 3.75
days (more on wider screens) with attached daily summaries in one horizontal
scroll; its height fits the temperature range. Summaries show conditions,
high/low temperatures, maximum hourly wind with direction (one line when it
fits), and precipitation chance/total. Temperature ticks stay on the left;
precipitation probability and each enabled wind/AQI/UV overlay get their own
top-to-bottom unit scale side by side on the right, in fixed wind/UV/AQI order.
Dew-point and feels-like visibility do not change the temperature scale or line
position. Every temperature (current, feels-like, header, hourly, daily
high/low, axis ticks, widget, and the graph line itself) uses one color scale:
blue at 10°F or below through teal, green, amber, and orange to red at
95°F or above, with light/dark variants that keep 4.5:1 text contrast. The
feels-like graph toggle draws a dotted line on the same scale.
Selecting a day or graph point updates the selected-day summary and navigates
the single hourly row horizontally without moving the page vertically.
Plot taps snap to the nearest plotted hour using the same floating-point
pixel spacing as drawing, including after horizontal scrolling; day-header
taps select the start of that day. Fractional-width tests prevent cumulative
offset from integer division.
Current temperature and condition icon are large and centered at the top, then
crossfade into the pinned header as they scroll away. A small high/low row
under the current temperature always shows today at the location, independent
of graph selection. Below the condition, a
row such as "Partly cloudy → fog after 18 → rain after 21" summarizes changes
for the rest of today (benign changes under 2 hours merge into the previous
segment; brief thunderstorms/freezing precipitation remain visible). At most
3 segments are shown: current conditions plus 2 changes, prioritizing storms,
then freezing precipitation, then the earliest other changes.
Current gusts and cloud cover follow. First-load progress is centered below
the fixed header; refreshes retain existing content. When an
hour within the next week has at least a 10% precipitation chance, a type-colored line
such as "40% chance of rain at 15 · 0.12 in" appears ("tomorrow at 03" within
24 hours, "Thu at 15" beyond), or "Raining now · 0.05 in this hour" during
precipitation; otherwise it reads "No precipitation expected in the next week"
only when every hourly slot through the next 7 days is covered. Short or
incomplete caches use "No precipitation expected in available forecast".
The "Updated" time uses the device time zone and adds the
zone abbreviation when it differs from the forecast location. Location remains
pinned and shows the state for cities under 500,000 people (or with unknown
population). The location selector opens Locations: search with a clear button,
location cards with hearts to save favorites, and favorites reordered by
long-press dragging the handle, overflow-menu Move up/Move down commands, or
accessible reorder actions. Cards use a compact single-row layout with a
selection marker instead of a Selected overline and no reorder footer; touch
targets remain 48dp and long names can wrap at large font sizes.
Search focus and keyboard Search return the list to its first item. New
result sets start at the top; ordinary typing and background weather updates
do not repeatedly reset scrolling. Saved-location cards show temperature and day/night
condition icons in stable slots, including cached weather offline. Entries
older than 15 minutes refresh independently with at most 3 concurrent requests
and 15-second timeouts; one city's failure preserves its cache without blocking
others. Pull-to-refresh in Locations forces an update. A selected favorite is
shown once, not duplicated under Current location. Search results do not
trigger weather requests.
The home header includes a crosshair Use current location action next to the
location selector, also available with a label in Locations. Both share the
same permission, timeout, and selection behavior. The action requests foreground precise/approximate
permission only when tapped, reads a one-shot recent device position, resolves
its name when available, and selects it without automatically favoriting it.
Denial, unavailable position, and timeouts retain the previous city. After
denial, Locations shows App settings; the compact home icon routes its next
tap to permission settings without crowding the city label. No repeated
prompts. Precise requests prefer GPS; approximate requests use network/fused
providers. No background-location permission or recurring tracking is used.
Its crosshair icon differs from the graph Now clock icon. Settings contains
preferences, About/privacy links, and Report an issue. Reports open an editable
email draft containing reproduction prompts, app version/code, and Android
version/API only. No coordinates, identifiers, favorites, or logs are attached
automatically. Copy-address/report actions remain available without an email
app; unavailable external-link handlers show an error instead of crashing.
Pull down at the top to refresh without resetting the selected
hour or forecast scroll. Refresh requests do not overlap. New locations clear
the previous city's data while loading.
Returning to home refreshes data older than 15 minutes without overlapping an
active load. Current-condition cache status/time is shown independently from
forecast freshness. Precipitation and condition narratives observe the minute
clock. Individual unit edits are atomic; location selection is one Room
transaction preserving favorites. Successful selection/unit changes and
selected-location weather updates request cache-only widget renders, avoiding
extra weather calls for preference changes. Room schema remains v8; no new
table columns were added.
Night shading follows local sunrise/sunset, distinct from civil twilight.
Hourly cards show precipitation-type icons, precipitation amount and chance, wind
speed and direction, grouped optional readings, and one date header that follows
scrolling. Compact separators mark midnight without repeating that header. Total
precipitation is liquid equivalent; snowfall accumulation is shown separately.
The graph overlays temperature, wind, and US AQI on a shared time axis. Save favorite
locations, choose Celsius or Fahrenheit, and view cached forecasts offline.
Choose System, Light, or Dark appearance in Settings. Hourly readings use
the full Feels like label. Current weather controls precede Daily stats
and Hourly fields, with separate Reset actions for each group. Current choices
cover temperature, condition, today's high/low, feels like, humidity, wind,
gusts, cloud cover, condition-change narrative, and precipitation outlook.
They default on and persist even when all are hidden. Disabled current fields
leave no blank rows; collapsed temperature/condition obey those choices.
Favorites, graph, daily fields, and widget remain independent. The Hourly fields control
saves choices for temperature, precipitation amount/chance, wind, humidity,
dew point, UV, AQI, feels like, wind gusts, cloud cover, freezing level, snow
depth, and visibility. Lengths follow the rain unit (mm → m/km/cm, in →
ft/mi/in). Daily stats precede hourly fields in Settings. Enabled daily stats
appear above the graph for the selected day, with stable row heights: UV high,
daylight duration, precipitation hours, and optional Sun times (first light,
sunrise, sunset, last light). Older daily choices retain Sun times until it is
explicitly disabled. UV reads, for example, "UV high of 4 (moderate) at 13-15";
peak windows use contiguous whole-day hourly readings sharing the rounded
maximum. Separate windows are listed separately; incomplete coverage omits
peak times rather than guessing. UV numbers use green/amber/orange/red/violet
severity colors. Rain is deeper blue, snow icy blue, mixed precipitation violet,
freezing precipitation amber, and unknown types neutral, using explicit
components/WMO codes rather than temperature inference.
Time and date remain visible even with all fields hidden.
Missing values are shown as unavailable, not zero.
Temperature (Celsius/Fahrenheit), wind (km/h, mph, m/s), and rain amount (mm/in)
are independent preferences. Existing Celsius/Fahrenheit choices retain their
former wind/rain defaults until those units are explicitly selected. Hourly
cards show wind direction and speed together. Wind arrows show airflow; compass
directions identify where wind comes from. Graph controls sit directly below
the plot in Feels like/Dew point/Wind/UV/AQI order, with choices saved across
restarts. A prominent Now button in the outlook heading returns graph/hourly
navigation to the current hour without vertical scrolling. Overlay scales
name their units; wind/AQI also use distinct line styles.
Completed hourly cards use grayer surfaces and a Past label; the current hour
stays normal. Past status and current graph index use the location's real
clock, refreshed at minute boundaries, rather than the last fetch timestamp.
The hourly field picker scrolls and can reset to defaults. Weather icons are
multi-tone vectors (amber sun, gold moon, slate clouds, blue rain, icy snow,
yellow bolt) with light/dark variants that follow the in-app appearance, and
keep accessible condition labels. The background is static (no particles).
The theme is built from the icon's ocean blue (#1E6FE0 light, #8AB4F8 dark) with
every Material 3 color role set, and graph series use a dedicated weather
palette rather than theme roles. Temperature gradients include each exact
palette anchor; historical lines remain faded with at least 3:1 contrast on
base tonal surfaces. Text colors are tested across light/dark tonal surfaces,
including selected cards. The widget supplies its own light/dark surface so
temperature contrast does not depend on the wallpaper.
The current wind summary uses observed direction from the current-weather API,
not the hourly forecast. Unknown cached directions stay unknown.

## Architecture

- Jetpack Compose provides the home and settings screens.
- Home rendering is split into current conditions, forecast/daily details, and hourly cards; graph labels and location cards have focused rendering files. Screen interaction state stays with its owner.
- Preference-only SettingsViewModel and search/favorite/device LocationsViewModel use navigation-owned ViewModelStores. Location/search flows run only while subscribed; owner disposal cancels active location work, covered by a regression test.
- Ktor calls Open-Meteo forecast, geocoding, and air-quality APIs.
- Room stores favorite locations, current conditions, hourly/daily forecasts, and AQI.
- Forecast days and graph boundaries use the selected location's timezone.
- DataStore persists independent weather units, appearance mode, current/hourly fields, versioned daily stats, and graph layers; Room schema remains v8, with no new table columns.
- Legacy use_celsius decoding remains for older installations; current UI uses independent unit preferences. Production repository operations are explicit, without silent no-op defaults; test defaults live in a shared fake.
- MaterialTheme owns app colors; semantic temperature/UV/precipitation palettes remain separate. No duplicate composition-local theme wrapper.
- Widget temperature and wind use the same unit conversions as the app.
- All displayed forecast times use 24-hour local time.
- Room v5 adds nullable rain, snowfall, and weather-code columns to hourly forecasts.
- Room v6 adds nullable hourly humidity; older cached forecasts retain unknown humidity.
- Room v7 adds nullable current `windDirectionDeg` to the weather table.
- Room v8 adds nullable hourly feels-like, gusts, cloud cover, freezing level, snow depth, and visibility (stored in meters); daily UV max, daylight seconds, and precipitation hours; current gusts and cloud cover; and location `admin1`/population. A seeded v7 migration test verifies preserved rows and unknown new values.
- Saved locations fill in state once successfully via the geocoding `get?id=` lookup. Enrichment runs in the background with a 5-second timeout and a 15-minute retry cooldown per location during the process lifetime. Failed lookups retain the name; successful updates refresh visible header/favorite labels. Widgets pick up saved labels on their next update.
- Missing precipitation-type or solar data is not inferred from temperature or clock time.
- Adaptive launcher icons show Earth's ocean-only limb from space, with a narrow bright lower atmosphere, distinct cyan/blue outer layers, and soft sunrise/sunset glow. A curved diagonal storm front has a scalloped leading edge, broken trailing clouds, subtle shadows, and clear ocean ahead. Five stars form an Aquila-inspired T for Tropo, not an exact sky chart. No land, constellation lines, or horizon cloud/tower. Monochrome art preserves the stars and matching storm-front cutouts. Native pixel tests verify cloud interiors, themed cutouts, clear ocean, and stars.

## Play Store Assets

Listing copy is in `store/listing-draft.md`. `store/assets/app-icon.png` is a
512 x 512 native Android render of the packaged launcher layers, without a
baked-in rounded mask or external shadow. `store/assets/feature-graphic.png`
is 1024 x 500 and uses a real forecast capture. Six 1080 x 1920 phone PNGs are
in `store/assets/phone/`, using Fahrenheit, mph, and inches. These assets do not
change candidate 3's unresolved production blockers.

To export the icon, set `TROPO_STORE_ICON` to the desired absolute PNG path and
run the existing `LauncherIconTest` with `--no-daemon --no-configuration-cache`.
Restore/unset the environment variable afterward. The normal test run does not
write an icon. `scripts/Build-StoreGraphic.ps1` composes the feature graphic from
the captured forecast screenshot; its crop must preserve the source aspect ratio.

## Design Reference

NASA Image and Video Library, ISS012-E-21787, 03/24/2006, Expedition 12,
provided an Earth-horizon design reference. The unused raster derivative was
removed; the app packages vector launcher art only. Source: [NASA catalog](https://images.nasa.gov/details/iss012e21787)
and [image asset](https://images-assets.nasa.gov/image/iss012e21787/iss012e21787~medium.jpg).
Usage follows [NASA Images and Media Guidelines](https://www.nasa.gov/nasa-brand-center/images-and-media/).
NASA is credited as the source; no endorsement of Tropo is implied. The user's
watermarked reference image is not included in the app.

## Build and test

Use JDK 21 and Android SDK 37; Java/Kotlin compilation targets remain 17.
Robolectric's Android shared-memory setup requires access to
`jdk.internal.access.SharedSecrets`; unit-test JVMs receive the scoped
`--add-exports=java.base/jdk.internal.access=ALL-UNNAMED` option. This does not
change Android runtime behavior. Build with `gradlew.bat assembleDebug` and run
unit tests with `gradlew.bat testDebugUnitTest`. Run static analysis with
`gradlew.bat :app:detekt`; `app/detekt-baseline.xml` records existing findings
so new Detekt issues remain visible.

Required gates:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:ktlintCheck :app:detekt
```

Release builds use R8 plus resource shrinking; Compose tooling is debug-only.
The candidate uses version code `3` and version name `1.0.0`; codes `1` and `2`
were already used in Play Console. Each subsequent upload needs an unused higher
version code. Stable graphics-path `1.1.0` is selected explicitly, but it does
not resolve the native RELRO check failure; this candidate is not production-approved.
Release bundles request `SYMBOL_TABLE` native debug metadata for Play crash
reports. Extraction requires Android NDK tools installed in the SDK.
Already-stripped dependency libraries cannot supply missing symbols;
Play's native-symbol warning may remain for those libraries.
Unused weather XMLs, launcher raster, shimmer helper, legacy catalog/DTO/icon
helpers, and instrumentation dependencies with no test sources were removed.
Canonical condition assertions and migration/scale coverage remain.

Verified 10/13/2026: all 105 unit tests, debug assembly, KtLint, Detekt, and release
lint pass on JDK 21. Formatting-only source fixes preserve behavior; the Detekt
baseline was not expanded. Signed version-code-2 AAB/APK builds pass, with
signature verification and 16 KB APK ZIP alignment checked. NDK 28.2.13676358 is
installed; the AAB contains DataStore symbol tables for all four ABIs. The
graphics dependency is already stripped and cannot provide missing symbols.
Bundletool validation and fresh release-device/widget/upgrade tests remain
outstanding; these checks do not establish full device-level 16 KB compatibility.

Verified 10/14/2026 for candidate 3: all five local gates pass. The AAB validates
and requests `PAGE_ALIGNMENT_16K`; signatures and APK ZIP alignment pass. The
native LOAD/RELRO regression script still fails graphics-path `1.1.0` on arm64
and x86_64. A fresh AAB-derived, test-key-signed install on a read-only x86_64
Android 17 emulator with 16 KB pages and compatibility fallback disabled runs
the home/graph, reporting fallback/copy controls, independent units, offline
cached startup, permission denial/recovery, synthetic precise selection, and
hosted widget unit/city updates. This is not a Play-installed upgrade or ARM64
validation. OSV returned no advisory matches for 192 resolved Maven coordinates;
that does not prove absence of vulnerabilities. Candidate artifacts, mapping,
hashes, and advisory evidence are archived under `%LOCALAPPDATA%\TropoReleases\3\`.
Production remains blocked on native-risk resolution, ARM64/Play upgrade checks,
key backup, Console completion, closed testing, and an approved source freeze.

Cleanup measurement 10/09/2026: unsigned release APK changed from 3,305,466
to 2,653,657 bytes, a 651,809-byte (19.7%) reduction. Both builds used the same
toolchain/configuration except cleanup and resource shrinking. Required launcher,
widget, and action resources were verified in the shrunk release resource table.
The main home file fell from 1,114 to 224 lines; settings state now covers only
preferences. Lifecycle/theme/component cleanup passed all four gates and the
release build; emulator smoke checks covered live favorites/search/reordering,
preference/theme persistence, exact graph selection without vertical movement,
offline cache, and foreground-permission routing. Original appearance, selected
city, favorite order, and connectivity were preserved. Release APK installation
was not tested because the comparison artifact is unsigned.

Verified 10/09/2026: all four gates pass. Emulator checks cover light/dark
appearance, font scale 1.3, centered first-load progress, live UV high/peak
hours, daily-first Settings, Sun times hiding, past cards, all graph layers,
and graph day/point/Now actions preserving vertical page bounds. Current
checks also cover today's high/low, all-current-fields hidden/reset, full
hourly Feels like text, live favorite weather, refresh/reordering, selected
favorite deduplication, permission denial/no-repeat behavior, timeout retention,
and successful precise-device selection with a simulated fresh fix. Emulator
checks also cover compact-card menu reordering, search returning to the top
after scrolling/keyboard Search, home permission denial/recovery at font
scale 1.3, and exact graph hour selections (18:00, then scrolled 07:00,
then 12:00 with all overlays and large text) without vertical movement. Snow
hue/contrast and fractional pixel hit-tests pass. Host emulator window zoom
and its mouse-coordinate scaling were not independently controlled. Native pixel
tests cover launcher cloud geometry; normal/themed variants are checked on
the launcher. Original app/launcher preferences were restored. A live API
check returned 240 hourly points and 10 daily rows; imperial length fields
arrive in feet and normalize to meters. Hosted widget rendering and older
physical-device location/geocoder behavior still need device verification.

When the repo lives under OneDrive, build outputs are redirected to
`%LOCALAPPDATA%\gradle-builds\tropo\` (for example the debug APK lands in
`%LOCALAPPDATA%\gradle-builds\tropo\app\outputs\apk\debug\`) because OneDrive
sync locks files under `build/` and breaks incremental task cleanup.
