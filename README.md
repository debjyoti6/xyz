# Quiet Launcher 1.2 — Android 12+

A native, offline text launcher with favorites, app search, rename/hide, adjustable text and opening pauses.

## Set up
1. Install the APK and open Quiet Launcher.
2. Tap **Set Quiet as your home screen**, select Quiet, and confirm Android's dialog.
3. If the dialog is cancelled or unavailable, use **Preferences → Open Android Home app settings** and select Quiet. Phone menus vary by manufacturer.
4. Open **Focus & Scroll Guard → Distracting apps** and select the apps you want to limit.
5. Choose a 15/25/45/60-minute focus session. Selected apps are blocked when opened from Quiet, and receive an opening pause outside focus.
6. Optionally enable **Quiet Scroll Guard** in Android Accessibility settings after reading the disclosure. It applies focus blocks to selected apps opened from notifications or Recents too. Outside focus it returns home after 2/5/10/15 minutes of continuous use and enforces a one-minute break.

Focus can be ended early from Quiet. Settings, Phone and the current default home app are excluded. Hidden apps can be restored from Preferences. Switch back to another home app through Android settings at any time.

## Limits and privacy
Scroll Guard is optional and uses only app-switch events and the Home action; screen-content retrieval is disabled. It does not inspect messages, passwords, feeds, Reels or Shorts, and never uploads data. It limits the whole selected app. No network, device-admin, overlay or usage-history permission. Preferences and selected package names stay on-device; backup is disabled.

A session resets when you switch to another app or lock the screen. This is a voluntary focus aid, not tamper-proof parental control. Services can be stopped by Android or the user. If a phone restricts accessibility access for sideloaded apps, use the launcher controls or Android Digital Wellbeing. No universal device compatibility claim; OEM testing remains necessary.

This version supports the personal profile. Work profiles, Private Space, widgets and notification filtering are not implemented.

## Build and checks
Java 17, Gradle 8.9, Android SDK 35. Run `gradle :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug`.
Device checks: `gradle :app:connectedDebugAndroidTest`. GitHub Actions tests API 31–36.
Tests cover HOME registration, role request availability, favorites after recreation, rename, hide/restore, pauses and focus/cooldown boundaries. Real-phone permission flows and Scroll Guard timing still need hands-on verification.

APKs are debug builds. If an update reports a signing conflict, uninstall the old build first (this clears Quiet preferences; installed apps and files are unaffected). CI preserves its debug signing key in a cache for subsequent builds, but cache eviction may require reinstalling.
