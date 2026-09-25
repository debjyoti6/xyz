# Play Store submission checklist — Quiet Launcher 1.5

This repository is prepared for testing and submission work. It is NOT published, Play-approved, or guaranteed on every manufacturer. The included sideload APK uses a development certificate. The CI app bundle is UNSIGNED and must be signed before upload.

## 1. Confirm ownership and listing
- Use your own eligible, verified Play Console account. Confirm that you can register `com.quiet.launcher` before its first Play upload; package names cannot be changed after publication. If unavailable, choose your own unique applicationId and rebuild/retest before the first upload.
- Confirm that the Quiet Launcher name and original artwork are appropriate for your listing. Do not market it as an official Minimalist Phone app or promise system-wide blocking.
- Supply the developer name, monitored support email, countries, content-rating answers and appropriate target audience in Console. No account/login is required in this app; declare no ads.

## 2. Create a real upload key on your own computer
Open the project in Android Studio, choose Build > Generate Signed App Bundle or APK > Android App Bundle. Create/select your own upload keystore, choose the release variant, and generate the bundle. Store the keystore and passwords securely with a backup; never commit them, paste them in chat or include them in release artifacts. Enroll in Play App Signing.

The `release` variant never falls back to debug signing. For command-line builds, set all four private environment variables: QUIET_UPLOAD_STORE (absolute keystore path), QUIET_UPLOAD_STORE_PASSWORD, QUIET_UPLOAD_ALIAS, QUIET_UPLOAD_KEY_PASSWORD. Run `gradle :app:bundleRelease` with Java 17 / Gradle 8.11.1. Without those variables CI deliberately produces an unsigned bundle. Android Studio's signing wizard can also sign the release build.

Do NOT upload the sideload APK/development key to Play. Moving from a sideload development-signed installation to a Play-signed installation can require uninstalling the old app and lose its local preferences. Test this before public distribution.

## 3. Privacy and declarations
Publish the completed PRIVACY_POLICY.md at a public HTTPS URL. Fill developer/contact/support-retention details first. The app also describes local data handling under Preferences > Help & privacy.

Based on this exact version: no data is transmitted off-device automatically, no collection/sharing SDKs, no ads, no accounts, no sensitive runtime permissions, no Accessibility service, no notification listener and no background monitoring. Review every Data safety question against the final binary and Google's definitions; local-only processing is not an automatic license to skip the form. Voluntary support email is described separately in the policy. Reassess if you add any SDK or network feature.

## 4. Listing draft
App name: Quiet Launcher
Short description: A calm home screen with favorites, opening pauses and focus sessions.

Full description:
Quiet Launcher gives your Android home screen a simple, readable app list. Keep essential apps close, search your apps, rename labels, hide distractions from Quiet and choose light or dark appearance.

Add a short pause before opening selected apps. Start a 15, 25, 45 or 60-minute focus session to pause selected launches through Quiet. Phone, Settings and the current default Home app remain accessible.

Quiet works offline without ads or an account. Focus controls apply only inside Quiet: notifications, links, Recents and other launchers can still open apps. Quiet does not monitor scrolling, block Reels or Shorts, or measure screen time. Hidden apps remain installed and accessible outside Quiet.

Requires Android 11 or later. Personal profile only. Widgets, work-profile/private-space app browsing and notification filtering are not included.

Provide a 512×512 app icon, 1024×500 feature graphic and at least two accurate phone screenshots. Use the supplied original graphics and genuine screenshots, not mock claims. Review the listing for readability on mobile.

## 5. Test and release
The code targets API36 and has no native libraries. CI checks API30–36 and tests the actual optimized sideload APK. Those checks do not simulate all OEM firmware, Google Play Protect classifications, Play-generated split delivery or real-device battery management.

Upload a properly signed AAB to internal testing first. Test the Play-delivered install/update and Home-role flow on physical Samsung, Xiaomi/Redmi, Pixel and Oppo/Realme devices available to you, including Android 11 and 16. Check gesture/three-button navigation, rotation, large fonts, tablet/foldable layouts, reboot, app install/remove, keyboard search, focus expiry, pause cancellation and recovery to system Home. Review Play's pre-launch report and accessibility/crash findings. Do not mark these checks complete until actually performed.

New personal accounts subject to Google's testing rule need at least 12 testers opted in continuously for 14 days before applying for production access. Follow the requirements shown for your account; emulator tests do not replace closed testing or review.

## Official references (checked 25 September 2026)
- Target API: https://support.google.com/googleplay/android-developer/answer/11926878
- Signing: https://developer.android.com/studio/publish/app-signing
- Testing: https://support.google.com/googleplay/android-developer/answer/14151465
- User data: https://support.google.com/googleplay/android-developer/answer/10144311
