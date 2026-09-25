# Quiet Launcher 1.5 — Android 11+

A lightweight, native Java launcher: favorites, search, rename/hide, opening pauses, focus sessions, text sizing and light/dark appearance. No runtime dependencies, ads, network, accounts, Accessibility service, background service or requested permissions.

## Setup
Install the personal-test APK, open Quiet, choose default Home, select Quiet in Android's dialog and confirm. Press Home. Use Home app setup / Android Home settings if needed; Copy setup details helps diagnose device-specific failures. Keep Play Protect enabled and share an exact error if Android blocks installation.

## Reliability changes
- Monotonic focus timing survives activity/process recreation and clock changes within a boot; reboot recovery uses a bounded wall-clock fallback. The session remains voluntary and can be ended early.
- Countdown/expiry labels update in place only while the activity is foregrounded.
- Phone, Settings and the current system Home resolve as essential and bypass pauses/blocks.
- App queries run off the UI thread; obsolete queued queries are cancelled, stale results are ignored, and a disappearing app label cannot abort the entire list.
- Clicks guard against app-list changes. Home setup avoids misleading change-default labels. Adaptive and themed icons support system shapes.
- API36 target, API30 minimum. No architecture-specific native libraries.

Focus blocks apply only to launches through Quiet; notifications, links and Recents can still open apps. Hidden apps remain installed. Personal profile only; no work-profile or Private Space browser, widgets or notification filtering. No promise of compatibility on every manufacturer or future Android version.

## Build
Java 17, Gradle 8.11.1, SDK36, build tools35.0.0; AGP8.10.1.
- `gradle :app:assembleSideload :app:bundleRelease :app:lintRelease`
- `gradle :app:connectedDebugAndroidTest`

`sideload` is an optimized non-debuggable personal-test APK signed with the cached development key, compatible with the previous development signer while that cache persists. `release` produces an unsigned AAB unless private upload-key environment variables are supplied. A development-signed APK is not the Play submission artifact.

CI runs instrumentation and actual sideload-APK Home selection on API30–36, with reports and screenshots. Device testing and Play release work remain necessary. See docs/PLAY_STORE.md for signing, privacy, listing, test and submission steps; docs/PRIVACY_POLICY.md is a policy draft to complete and host.
