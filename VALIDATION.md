# Quiet Launcher 1.5 validation — 25 September 2026

Tested app commit: 62265434880dcaa3e41ebe9d5d1bb11897f9626b.
Workflow: https://github.com/debjyoti6/xyz/actions/runs/36131113388

## Passed
- Optimized personal-test APK build, unsigned release AAB build and release lint.
- APK signing verification, no requested permissions or services, non-debuggable build, backup disabled, min API30 / target API36.
- Nine instrumentation checks on each API30–36 emulator (63 executions): favorites after recreation, hide/restore, rename/search, opening-pause cancellation, HOME registration, focus selection/expiry, clock-change stability, bounded reboot/legacy recovery, and visible expiry status.
- Installed the actual optimized APK on each emulator; selected HOME through Android's UI, cancelled/retried the chooser, confirmed system resolution, and pressed Home both directly and from Settings.
- Optimized APK app-list/search checks, 150% font scale, activity recreation and landscape rendering on each API30–36 emulator.
- Fixed the actual onboarding regression found by the font-scale test: completed default-Home setup now persists from the singleTask launcher too.
- Examined Android11 large-font and landscape captures. Store screenshots are genuine captures.
- APK: 50,104 bytes, SHA256 7c95aa86c7905314fdb62de30667c54f9bc01da9a5a3289254d1c49b05309a1f. Same development signing certificate as v1.4.
- Release AAB: 35,741 bytes, ZIP integrity valid, unsigned as intended, no native libraries.

## Still required before public release
Physical OEM phones, Play-generated install/update testing, completing and hosting the privacy policy, eligible publisher/account setup, private upload-key signing, accurate store declarations, account-specific closed testing and Play review. No claim of universal device compatibility, Play Protect acceptance or Play Store approval. The sideload APK is a personal-test artifact; do not submit its development key as your Play release key.
