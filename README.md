# Quiet Launcher 1.4 — Android 12+

Native, offline text launcher with favorites, search, rename/hide, opening pauses and focus sessions.

## Install and choose Home
Install the APK, open Quiet, tap Choose default home app, then Choose Quiet as default Home. Select Quiet Launcher in Android and confirm. Press Home. The setup screen also offers Android Home settings and Copy setup details for troubleshooting.

Version 1.4 removes Scroll Guard and its Accessibility service entirely. Google Play Protect can block internet-sideloaded apps with sensitive access in some markets. This is a potential cause of earlier installation problems, not a confirmed diagnosis on the user's phone. Keep Play Protect enabled; allow a requested app scan. If blocked, share the exact warning and phone model rather than disabling device protection.

## Focus and limits
Choose distracting apps under Focus controls. Start a 15/25/45/60-minute focus session. Selected apps are blocked when opened from Quiet during focus and get an opening pause outside focus. Focus can be ended early. Notifications, links, Recents and other launchers can still open these apps. Use Android Digital Wellbeing for system app timers. Quiet cannot monitor scrolling, close other apps, or detect Reels/Shorts.

## Privacy
No requested permissions, Accessibility service, notification listener, network, analytics, ads or account. Preferences stay on the device; backup is disabled. Phone, Settings and the current default Home app are excluded from focus blocks. Personal profile only; work profiles, Private Space, widgets and notification filtering are not implemented.

## Build and validation
Java 17, Gradle 8.9, SDK 35. CI builds a non-debuggable release, verifies its APK signature and packaged manifest, and runs lint. Android 12 and 15 emulator checks run six instrumentation tests, then install the actual release artifact and exercise the real HOME chooser, cancellation/retry and Home-button behavior. Emulator tests do not verify Play Protect acceptance on a physical phone.

CI uses the same cached development signing key as v1.3 to permit updates. This is a personal test distribution, not a Play Store release. Cache loss can change the signer; an update conflict then requires uninstalling the previous build, which clears Quiet's preferences. For local release signing supply QUIET_SIGNING_STORE pointing to an Android debug-format keystore, or configure your own signing credentials.
