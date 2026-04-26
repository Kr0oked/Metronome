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

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing env vars)
fastlane android apk

# Run lint
./gradlew lint

# Screenshots via Fastlane (requires device)
fastlane android grab_screen_phone_1
fastlane android grab_screen_phone_2
fastlane android grab_screen_seven_inch_1
fastlane android grab_screen_seven_inch_2
fastlane android grab_screen_ten_inch_1
fastlane android grab_screen_ten_inch_2
```

Fastlane release builds require env vars: `KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
Google Play deployment additionally requires `ANDROID_JSON_KEY_FILE`.

## Architecture

The app follows MVVM with a foreground service for audio playback:

- **`MetronomeApplication`** — Hilt entry point
- **`MainActivity`** — Single Compose activity; binds to `MetronomeService`, handles `POST_NOTIFICATIONS` permission
  (Android 13+)
- **`MetronomeViewModel`** — All UI state via `StateFlow`; tap tempo (5-second window); debounced settings writes
  (1 second); communicates with bound service
- **`MetronomeService`** — Foreground service managing the `Metronome` engine; local binder for IPC with Activity

### Key Packages

| Package     | Responsibility                                                                                       |
|-------------|------------------------------------------------------------------------------------------------------|
| `domain/`   | `Metronome` — core engine using `AudioTrack` API at 48kHz PCM FLOAT; tick listener pattern           |
| `data/`     | Immutable data models: `Beats`, `Subdivisions`, `Gaps`, `Tempo`, `Sound`, `TickType`, `TempoMarking` |
| `audio/`    | `SoundLoader` / `SoundProvider` — loads audio assets for tick types                                  |
| `settings/` | `DataStoreSettingsRepository` — persists preferences via Jetpack DataStore; injected via Hilt        |
| `ui/`       | Jetpack Compose screens: `metronome/`, `settings/`, `licenses/`, `theme/`                            |

### Data Flow

User interaction → `MetronomeViewModel` → `MetronomeService` (bound) → `Metronome` (domain) → `AudioTrack`

Settings changes are debounced 1 second before being written to DataStore.

## Tech Stack

- **UI:** Jetpack Compose + Material3, Navigation Compose
- **DI:** Hilt + KSP
- **Persistence:** DataStore Preferences
- **Audio:** Android `AudioTrack` (PCM FLOAT, 48kHz)
- **Build:** AGP 9.1.0, Kotlin 2.3.x, Java 11 toolchain
- **Testing:** JUnit4, Compose UI Test, kotlinx-coroutines-test, Fastlane Screengrab

## Branch Notes

`master` is the main branch used for releases and PRs.

## Translations

Translations are managed via Weblate. Do not manually edit `strings.xml` files in locale-specific resource directories —
changes come in through automated PRs from Weblate.
