# Quiet Launcher — Android 12+

An original native Android launcher with a text-first, monochrome interface. Inspired by minimal launchers; not affiliated with minimalist phone and not an exact copy of its proprietary app.

## Read this first

**This download is source code, not an APK. Version 1.1 is uncompiled and untested on Android.**

The authoring environment has no Android SDK, Gradle, Java compiler, or emulator. Downloading Android build tools timed out. GitHub is connected, but there is no dedicated launcher repository; the browser is signed out. No existing repository was modified. A private repository must be created/selected before the supplied workflow can build the APK and run compatibility checks.

The minimum supported Android version in the source is **Android 12 (API 31)**. Newer-version compatibility is a design target, not a verified universal guarantee. The app has no native libraries, so the APK does not require separate ARM/x86 versions. Device/OEM restrictions still apply.

## Implemented source features

- Black or paper-colored text-only home screen.
- Live clock and date that follow the phone's 12/24-hour preference.
- Up to eight favorite apps; long-press to remove or move to the top.
- Alphabetical app drawer with case-insensitive search; searches original and renamed labels.
- Rename app labels within Quiet (does not modify the installed app).
- Hide apps from Quiet and restore individual apps or all hidden apps in Preferences.
- Optional opening pauses of 3, 5, 10, or 15 seconds. Requires an explicit tap to open; cancels when the launcher goes into the background.
- Three app-text sizes plus Android's system font scaling.
- Scrollable home/preferences screens and keyboard, navigation-bar, status-bar, and display-cutout insets.
- Background app discovery, package-change refresh, and navigation/search restoration on activity recreation.
- First-run introduction, default-home chooser, and Android settings shortcut.
- No network, accessibility, notification-access, usage-access, or device-admin permissions. No ads, accounts, or analytics. App preferences remain local; Android backup is disabled.

## What it does not include

No device-wide app blocking, notification filtering, in-app reminders, work-profile support, Private Space support, widgets, or icon packs. Opening apps through notifications, links, Android settings, or another launcher bypasses Quiet's pause. Hidden apps stay installed and accessible elsewhere. This is visual decluttering, not security or parental control.

## Build using GitHub Actions

1. Create a **private** GitHub repository for this project and initialize it with a README. If asking the assistant to finish the build, provide that repository's URL and make it accessible to the connected GitHub integration.
2. Put the **contents of this folder** at the repository root. `settings.gradle`, `app/`, and `.github/` must be at the root. Include the hidden `.github` folder.
3. In **Actions**, run **Build and test Android 12+ launcher**. A push to `main` or `launcher/**` also triggers it.
4. The `build` job compiles the app and instrumentation tests, then runs Android Lint. If successful, it uploads **Quiet-Launcher-APK-built** containing `app-debug.apk`.
5. The compatibility jobs run UI acceptance tests across APIs **31, 32, 33, 34, 35, 36** (Android 12, 12L, 13, 14, 15, 16). Download the per-device reports to inspect results.
6. Wait for the build and **all compatibility jobs** to succeed before treating the artifact as an emulator-tested candidate. This still does not replace testing on your physical phone.

The workflow is prepared but has not been run. It requires available GitHub Actions minutes, Android system images, and network access. It uses disposable GitHub-hosted runners. It does not add billing, publish the app, or create a Play Store release.

Debug builds are for personal testing. Different GitHub runners may generate different debug signing keys; installing a later APK over an earlier one can fail with a signature conflict. For long-term updates, configure a persistent private signing key in repository secrets; do not commit it. Without stable signing, uninstalling the previous test build before reinstalling resets preferences.

## Build on your computer

Use Java JDK 17, Gradle 8.9, Android SDK platform 35, and build-tools 34.0.0. Set `ANDROID_HOME` to the SDK directory and accept SDK licenses normally.

```sh
gradle :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

The APK appears at `app/build/outputs/apk/debug/app-debug.apk`.

On a connected Android 12+ test device/emulator, run:

```sh
gradle :app:connectedDebugAndroidTest
```

The tests clear Quiet Launcher's app preferences before/after each test. Use an emulator or a dedicated test installation, not your configured daily launcher.

A Gradle wrapper binary is not included because Gradle could not be downloaded here. With Gradle 8.9 installed, run `gradle wrapper --gradle-version 8.9` to generate it; then open the folder in Android Studio with JDK 17 selected for Gradle. Build versions are pinned to AGP 8.7.3 and Gradle 8.9. This is not a claim of current Play Store submission compliance.

## Installation after a successful build

1. Extract the build artifact and transfer `app-debug.apk` to the phone.
2. Open it and follow Android's normal installation prompts. Do not bypass device-management or security restrictions.
3. Open **Quiet Launcher**, tap **Start using Quiet**, then **Set Quiet as your home screen**.
4. Open **All apps**, hold an app name, and add favorites or customize it.
5. To return to the original launcher: Android **Settings → Apps → Default apps → Home app**. Menu wording varies by manufacturer. Your original launcher remains installed.

## Acceptance tests prepared

- Favorite persistence and visible restoration after activity recreation.
- Hiding an app and restoring it through Preferences.
- Disabled early-open button and pause cancellation on backgrounding.
- Renamed label search and empty-search results.

Manual checks still required: default-home selection and cancellation, launching actual apps, Home/Back gestures, large fonts/landscape/foldables, call and camera access through All apps, app uninstall/reinstall, TalkBack, full-duration countdown behavior, and switching back to the original launcher. Test on at least your actual phone before daily use.

## Official references

- Android Home role: https://developer.android.com/reference/android/app/role/RoleManager#ROLE_HOME
- Android package visibility: https://developer.android.com/training/package-visibility/declaring
- Android instrumentation: https://developer.android.com/training/testing/instrumented-tests
- Android 16 behavior: https://developer.android.com/about/versions/16/behavior-changes-16
- Build compatibility: https://developer.android.com/build/releases/agp-8-7-0-release-notes
- Emulator runner: https://github.com/ReactiveCircus/android-emulator-runner
