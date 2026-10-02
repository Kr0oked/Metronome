# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Simple metronome app for Android (Kotlin + Jetpack Compose), published on F-Droid and Google Play.
Package: `com.bobek.metronome`.

## Build & Test Commands

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run all tests, auto-creating/booting/shutting down a Test_Phone AVD for the instrumented ones
bundle exec fastlane android test

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing env vars)
bundle exec fastlane android apk

# Run lint
./gradlew lint

# Screenshots via Fastlane; each lane grabs a light (1.png) and dark (2.png) shot
bundle exec fastlane android grab_screens               # creates Screenshots_* AVDs if missing, boots each in turn
bundle exec fastlane android setup_screenshot_emulators # just (re-)create the Screenshots_* AVDs, without grabbing screenshots
bundle exec fastlane android grab_screen_phone          # requires a connected/already-running device
bundle exec fastlane android grab_screen_seven_inch
bundle exec fastlane android grab_screen_ten_inch

# Regenerate fastlane/metadata/android/*/images/featureGraphic.png from the app icon,
# app/src/main/res/values/colors.xml theme colors, and each locale's translated app name.
# Requires python3-pillow (built with libraqm). Needed fonts are downloaded automatically
# on first run into fastlane/.fonts-cache/ (git-ignored), so a network connection is
# needed the first time only.
bundle exec fastlane android generate_feature_graphics
```

Fastlane release builds require env vars: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
Google Play deployment additionally requires `ANDROID_JSON_KEY_FILE`.

## Architecture

The app follows MVVM in a single-Activity Compose setup with a foreground service for audio playback:

- **`MetronomeApplication`** — Hilt entry point
- **`MainActivity`** — Single Compose activity; hosts `AppViewModel` and `MetronomeViewModel`; binds to
  `MetronomeService`, handles `POST_NOTIFICATIONS` permission (Android 13+)
- **`AppViewModel`** — Night mode preference via `StateFlow`; reads from `SettingsRepository`
- **`MetronomeViewModel`** — All metronome UI state via `StateFlow`; tap tempo (5-second window); communicates with
  bound service; loaded from `SettingsRepository` on init
- **`MetronomeService`** — Foreground service managing the `Metronome` engine; local binder for IPC with Activity

### Key Packages

What belongs where, rather than an inventory; a class is named only when it carries a rule worth knowing.

- **`domain/`**: the playback engine. `Metronome` — `AudioTrack` at 48kHz PCM FLOAT, notifies a
  `MetronomeTickListener` per tick; the beat, tick-type, gap and period math lives as pure functions in
  `MetronomeCalculations`, unit-testable without Android.
- **`data/`**: immutable value models, e.g. `Beats`, `Subdivisions`, `Gaps`, `Tempo`, `Sound`, `TickType`,
  `TempoMarking`, plus preference models `AppNightMode`, `PreferenceChoice`.
- **`audio/`**: `SoundProvider` loads the tick sounds from `res/raw` (`SoundLoader` parses the WAV PCM FLOAT data);
  a new `Sound` needs strong, weak and sub WAV files there.
- **`settings/`**: the `SettingsRepository` interface and its Jetpack DataStore implementation
  (`DataStoreSettingsRepository`, bound in `SettingsModule`); keys live in `PreferenceConstants`. A new setting also
  needs loading and debounced persisting in `AppViewModel` or `MetronomeViewModel`, a control in `SettingsScreen` or
  `MetronomeContent`, the `ComposeAppViewModel` / `ComposeMetronomeViewModel` preview stand-ins, and the two test
  fakes (`AppViewModelTest`, `MetronomeViewModelTest`). One that affects playback must also go through
  `MetronomeService` and be synced both ways in `MetronomeViewModel` (`initServiceValues`, `updateViewModel`).
- **`licenses/`**: `LicenseRepository` reads the app and third-party license texts from `res/raw`.
- **`ui/`**: Compose code, one subpackage per screen (`metronome/`, `settings/`, `licenses/`) plus `theme/`.
  `MainContent` hosts navigation.

### Data Flow

User interaction → `MetronomeViewModel` → `MetronomeService` (bound) → `Metronome` (domain) → `AudioTrack`

Settings changes are debounced 1 second before being written to DataStore.

## Tech Stack

- **UI:** Jetpack Compose + Material3, Navigation Compose
- **DI:** Hilt + KSP
- **Persistence:** DataStore Preferences
- **Audio:** Android `AudioTrack` (PCM FLOAT, 48kHz)
- **Build:** AGP 9.x, Kotlin 2.3.x, Java 11 toolchain
- **Testing:** JUnit4, Compose UI Test, kotlinx-coroutines-test, Fastlane Screengrab

## Branch Notes

`master` is the main branch used for releases and PRs.
