# TG Equalizer Free — Android project

A clean-room, key-free Equalizer prototype inspired by the general workflow of TaGunner EQ. It does not include activation, device codes, license keys, network calls, or copied proprietary app code/assets.

## Features
- Dark audio-console style interface
- Five-band EQ (uses the number of bands supported by Android, up to five)
- Bass Boost and output gain controls where supported by the device
- Presets: Balanced, Bass Boost, Deep Bass, Vocal/Clear, Treble, Gaming
- Local persistence of user settings
- Master effect toggle and reset
- No internet permission and no key/activation system

## Important Android limitation
Android's `Equalizer`, `BassBoost`, and `LoudnessEnhancer` APIs are audio-session effects. This project requests audio session `0` to attempt global-output processing, but many Android/OEM builds do not allow third-party applications to process all system audio this way. OriginOS may reject the global session or limit the effect to compatible audio paths. The UI can launch even if global audio effects are unavailable. This cannot be guaranteed to work globally on every iQOO Z9x firmware without on-device testing.

## Build APK
1. Open this folder in Android Studio (Giraffe or newer; current Android Studio recommended).
2. Allow Gradle sync and install Android SDK Platform 35 plus Build Tools offered by Android Studio.
3. Choose **Build > Build APK(s)**.
4. The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

Command line from a machine with Android SDK and Gradle installed:

```bash
gradle assembleDebug
```

For a release APK, configure a signing key in Android Studio. Do not publish a release build unsigned.

## Project identity
- Application ID: `com.deeods.tgeqfree`
- Version: `1.0.0`
- Minimum Android: 6.0 (API 23)
- Target SDK: 28 (kept conservative for compatibility; review target SDK before publishing)

## Test checklist on iQOO Z9x / OriginOS 6
- Install and launch
- Test music from at least two apps
- Toggle effects and compare bypass/on
- Change EQ bands, Bass Boost and gain
- Reopen app and verify settings persist
- Check output clipping/distortion at positive gain
- Verify effect availability with wired USB-C DAC and Bluetooth output

## Optional automated APK build
A GitHub Actions workflow is included at `.github/workflows/android-build.yml`. Upload/push this project to a GitHub repository, open **Actions → Android APK Build → Run workflow**, then download the `TG-Equalizer-Free-debug-apk` artifact from the completed run. This workflow is provided but has not been executed from this environment.
