# Tropo Release Runbook

## Local Signing

Upload key: `%LOCALAPPDATA%\TropoSigning\tropo-upload.p12`, alias `tropo-upload`
Encrypted password: `%LOCALAPPDATA%\TropoSigning\upload-password.dpapi`
Public upload certificate: `%LOCALAPPDATA%\TropoSigning\upload-certificate.pem`

The password is protected by Windows DPAPI for the current user. Neither key
nor password belongs in Git, chat, screenshots, or build logs. Back up the
keystore and recover/export the password yourself using a trusted local tool
before changing/reinstalling Windows. A copied DPAPI blob alone is not a
portable backup. Play App Signing must be enabled in Console; this is an upload
key, not a claim that Play-managed app signing is already provisioned.

```powershell
.\scripts\Build-Release.ps1
.\scripts\Test-NativeAlignment.ps1
```

The wrapper restores process environment values after Gradle exits. Without
signing environment variables, ordinary Gradle release builds are unsigned
and must not be uploaded. Do not use the debug key for production.

## Required Checks

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:ktlintCheck :app:detekt :app:lintRelease
.\scripts\Build-Release.ps1
```

- Confirm final package `io.github.erexer.tropo` and unused versionCode before upload
- Current candidate: versionCode `3`, versionName `1.0.0`; codes `1` and `2` are already used in Play Console
- Verify signed AAB signature and validate with bundletool
- Generate/install AAB-derived APKs and run release-only tests, including widget/permissions/offline/upgrade
- Check 64-bit native ELF LOAD/RELRO alignment and AAB page-alignment configuration, not only APK ZIP alignment
- Archive release mapping and upload public certificate only where Console requests it
- Back up upload key/password securely; never share private signing material

Release builds use `ndk.debugSymbolLevel = "SYMBOL_TABLE"` to include available
native symbols in the AAB. Check for `BUNDLE-METADATA/com.android.tools.build.debugsymbols/`
entries. An APK build may also produce
`%LOCALAPPDATA%\gradle-builds\tropo\app\outputs\native-debug-symbols\release\native-debug-symbols.zip`
for manual upload to the matching version in Play Console. Already-stripped
dependency libraries cannot supply missing symbols; their native-symbol warning
may remain and does not block testing or publishing. Do not upload an empty ZIP
or substitute the R8 mapping file for native symbols.

Verified 10/13/2026: NDK `28.2.13676358` is installed. After forcing native
extraction to rerun, the signed AAB contains four DataStore symbol-table entries
(arm64-v8a, armeabi-v7a, x86, x86_64). Graphics symbols are already stripped and
remain unavailable; the Play warning may still apply to that dependency.

All 105 unit tests, debug assembly, KtLint, Detekt, and release lint pass on JDK 21.
The test-only `jdk.internal.access` export fixes Robolectric's SharedSecrets
access failure; no test failures were suppressed and the baseline was not
expanded. Signed AAB/APK builds, versionCode `2` / versionName `1.0.0`, signatures,
and 16 KB APK ZIP alignment were verified. AAB verification reports local
certificate/JarInputStream warnings; it does not replace bundletool validation.
Bundletool validation, AAB-derived installation, and fresh release-device,
widget, permission, offline, and upgrade checks remain outstanding.

Audit fixes verified in regression tests: guarded report/link launches, minimal
email metadata, stale-resume refresh coalescing, independent current-weather
cache status, atomic unit updates, transactional cancelled/competing location
selection, favorite preservation, and cache-only widget update notifications.
Hour and midnight rollover cases pass; compiled precipitation/narrative
composables receive the changing Instant clock explicitly.
Before uploading, test Report an issue with and without an email handler and
verify installed widgets after unit/location changes. Privacy publication was
owner-approved; hosting files were pushed separately from app source. If Play
has accepted versionCode `2`, bump it before the next upload.

The updated signed AAB/APK were rebuilt with versionCode `2`; APK signature,
16 KB ZIP alignment, and all four AAB native-symbol entries pass. Device UI
checks were attempted without wiping AVD data, but emulator graphics startup
and ADB availability blocked them. Missing-handler behavior is covered by
Robolectric tests, not a completed emulator smoke test. Active Glance sessions
observe selected cached weather/location/units, covered by a real-Room test.

## Privacy Hosting And Console

GitHub Pages workflow is manual-only. Hosting files were pushed in commit
`92a94b9` on 10/13/2026; app source remains unpushed. The owner reported a
successful Pages deployment on 10/14/2026 after retrying GitHub's HTTP 500.
The Node 20 and upcoming Ubuntu 26 annotations were non-blocking warnings.

The workflow now pins `ubuntu-24.04` and immutable official release commits:
checkout `v7.0.1`, configure-pages `v6.0.0`, upload-pages-artifact `v5.0.0`, and
deploy-pages `v5.0.1`. The JavaScript actions use Node 24; the composite upload
action pins upload-artifact `v7.0.0`, also verified as Node 24. The workflow-only
update was committed and pushed as `7c95f68`; unrelated app changes remain local.
Checksum-verified actionlint `1.7.12` validates the workflow. Both public privacy
and credits URLs were verified reachable on 10/14/2026.

Rerun the updated workflow and inspect its annotations before marking warning
removal verified. The agent has no workflow-dispatch tool, and its integrated
browser is not signed in, so the new run must be started in an authenticated
browser. The earlier successful run does not verify the new pins. No app rebuild
or version-code change is needed.

For manual deployment:

1. Settings > Pages > Build and deployment > Source: GitHub Actions
2. Configure the `github-pages` environment if required by repository settings
3. Actions > Publish Privacy Pages > Run workflow on main
4. Review the deployment result and verify both privacy and credits URLs

Verify `https://erexer.github.io/tropo/privacy.html` is public and reachable
before completing the Console privacy URL or shipping publicly. An editor
environment-name diagnostic can remain until that environment exists remotely.

Review `store/data-safety-draft.md` against actual provider contracts and
platform backup behavior. Complete rating/audience/ads/app-access forms and
store artwork. The new personal account needs 12 continuously opted-in closed
testers for at least 14 days before production-access application. Internal
testing can begin earlier after Console setup.

No Console uploads, account changes, purchases, or production rollout are
authorized by merely running the local build wrapper.

## Candidate 3 Status - 10/14/2026

This is a blocked candidate, not a production sign-off. Stable graphics-path
`1.1.0` was tested after `1.0.1` failed the native RELRO check; both versions
retain non-16-KB RELRO ends. Do not suppress or bypass the native regression
check. AndroidX selects a platform path iterator on API 34+, but this and a
passing modern emulator run do not establish ARM64/native-backport safety.

- Passed: all five local gates; AAB structure/version/16-KB packaging; APK/AAB signatures; APK ZIP alignment
- Passed device checks: fresh AAB-derived test install on x86_64 API 37, PAGE_SIZE 16384, fallback fatal/disabled; home/graph, report/copy fallback, unit persistence, offline cache, permission denial/recovery, synthetic precise selection, hosted widget unit/city updates
- Not verified: ARM64 16-KB runtime, Play-installed upgrade preserving data, real email delivery, completed Console forms/artwork, closed testing/production access
- Failed: graphics native RELRO static check; not presented as a confirmed app crash
- Advisory review: OSV checked 192 resolved Maven coordinates with zero matches; coverage is not exhaustive
- Owner decisions: leave app changes uncommitted; do not vendor/rebuild native code yet; verify ARM64/Play next
- Owner confirmation: independent upload-key/password backup is NOT complete; store forms/assets/testing setup are NOT complete

Candidate archive:
`%LOCALAPPDATA%\TropoReleases\3\20261014-102506\`
contains signed AAB/APK, matching R8 mapping, signature reports, SHA-256 hashes,
the OSV report, and a manifest recording the dirty source state and failed native
check. AAB-derived device test APKs use the local debug test key, not Play App
Signing or the upload key; do not upload those test APKs.

Before continuing: back up the upload keystore to independently recoverable,
encrypted storage and place its password in your own trusted password manager.
The DPAPI file alone is not a portable password backup. Do not send either secret
through chat. Complete Data Safety/listing/rating/audience/app-access forms and
store artwork, then run the required 12-tester/14-day closed test. Commit/push
and tag require separate approval; do not tag this candidate as production-ready.

## Free Release

Tropo is free with no ads, subscriptions, or in-app purchases. It calls
Open-Meteo's free non-commercial APIs directly; no developer-hosted backend or
paid API key is required. Provider usage limits and attribution still apply.

- Set the app price to Free and the contains-ads declaration to No in Play Console
- Do not create subscription or in-app products
- Confirm the release has no advertising or billing SDKs
- Retain weather/data attribution and review provider usage limits before release