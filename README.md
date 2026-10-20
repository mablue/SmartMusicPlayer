# SmartMusic — Adaptive Ranking & Smart Shuffle Music Player

[نسخه فارسی](README.fa.md)

[![Build & Release](https://github.com/mablue/SmartMusicPlayer/actions/workflows/release.yml/badge.svg)](https://github.com/mablue/SmartMusicPlayer/actions/workflows/release.yml)
[![Platform](https://img.shields.io/badge/Platform-Android%2024%2B-3DDC84?logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-7F52FF?logo=kotlin&logoColor=white)](#)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-4285F4?logo=jetpackcompose&logoColor=white)](#)
[![Media3](https://img.shields.io/badge/Playback-Media3%20ExoPlayer-orange)](#)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

An offline-first Android music player built with **Jetpack Compose (Material 3)** and **Media3 ExoPlayer**. Instead of manual star ratings, SmartMusic scores each track from real listening behavior — completion, skips, replays — and uses those scores to drive a weighted shuffle.

> **Status:** personal / experimental project. The build is reproducible from the single workflow in `.github/workflows/release.yml`. APIs, UI, and file layout may change between tags.

## Table of contents

- [Features](#features)
- [Architecture](#architecture)
- [Tech stack](#tech-stack)
- [CI/CD](#cicd)
- [Local build](#local-build)
- [Localization](#localization)
- [Roadmap & known limitations](#roadmap--known-limitations)
- [License](#license)

## Features

### 1. Behavioral affinity ranking

- **Exploration score:** every new track starts at **50**.
- **First listen calibration:** early skip (< 20 s or < 20 %) → **20**; full listen (≥ 75 %) → **80**; otherwise **55**.
- **Later listens:** early skip **−20** (skipped if the track was played within the last 8 minutes — fatigue protection), full listen **+20**, consecutive replay **+28**, partial listen **+5**. Scores are clamped to `[5, 100]`.
- **Weighted shuffle:** `P(song_i) = score_i^1.3 / Σ score_j^1.3`.
- **Tiers:** Diamond ≥ 80, Gold ≥ 65, Silver ≥ 45, Disliked < 45, Unplayed.
- **Live feedback:** an evaluation banner after each skip/listen, plus a per-track algorithm inspector dialog.

### 2. Media3 playback core

- **ExoPlayer (Media3 1.6.x)** with `handleAudioFocus = true` and `handleAudioBecomingNoisy = true`.
- **`PlaybackService`** extends `MediaSessionService`; a custom `MediaSession.Callback` routes Bluetooth / headset keys (Next, Previous, Play/Pause, HeadsetHook) to `SmartAudioPlayer` so queue-aware skipping works.
- **Five playback modes:** Smart Weighted Shuffle, Top Ranked First, Discovery (unplayed first), Sequential, Repeat One.
- **Queue (Up Next):** Play Next, Add to Queue, Remove, Clear; the queue is consulted before the ranking fallback.
- **Per-song resume:** last track, position, and playback mode persist across restarts.
- **Sleep timer** with gradual fade-out, **A-B repeat loop**, **playback speed 0.75×–2×**.

### 3. Lyrics HUD

- **Full-screen panel** with active-line emphasis, auto-scroll, and tap-to-seek.
- **Sources tried in order:** LRCLIB (direct) → LRCLIB (search) → lyrics.ovh → Lyrica aggregator.
- **LRC parser** for timestamped lyrics; plain lyrics are auto-fitted to the track duration.
- **Auto-search** and **auto-translate** toggles, persisted in preferences.
- **Translation** via the public Google Translate endpoint; Persian→Persian is skipped.
- **Caching:** both lyrics and translations are stored per song in Room and reused before any network call. Missing translation rows are stored with a null marker so rows never shift.

### 4. Library, playlists, favorites

- **Four bottom tabs:** Library, Folders, Podcasts, Playlists.
- **Library:** search, tier filter chips, sort menu (Smart Rank, Duration, Size, Title, Artist), multi-select (select all, play, add to playlist, toggle Music/Podcast, share, delete).
- **Folders:** group by folder, drill down, play all.
- **Podcasts:** category filter (All / Podcast / Audiobook), episode-number sort, speed chip, quick −15 s / +30 s, convert to Music.
- **Playlists:** a Favorites card (score ≥ 65 or ≥ 2 full plays) plus user playlists and a create action.
- **MediaStore scan** with URI + title/artist deduplication and heuristic language/genre detection.
- **Edit tags**, **share single/multiple**, **delete** via `MediaStore.createDeleteRequest` on Android 11+.

### 5. Audio effects

- **5-band equalizer** (or device-native band count when hardware FX is available) with **8 presets**: Flat, Deep Bass, Vocal, Rock, Pop, Jazz, Electronic, Classical.
- **Bass Boost** and **3D Virtualizer**.
- Effects are created **disabled** and only enabled from the Equalizer dialog, so the app never auto-enables loudness boosts on startup. They re-attach when ExoPlayer assigns a new audio session.

### 6. System integration

- **Foreground media notification** via Media3.
- **Bluetooth auto-play** receiver and **audio-becoming-noisy** handling.
- **Predictive Back** for the full player (Android 15 gesture preview).
- **Car Mode HUD** with large controls.
- **Dynamic Color** support, off by default (`dynamicColor = false` in `MyApplicationTheme`).

### 7. Diagnostics

- **`FileLogger`** appends to `smartmusic_logs/errors.log` under the app's external files directory, with rotation at 2 MB and an uncaught-exception handler.
- **Settings** includes a share-logs action.

## Architecture

```

app/src/main/java/com/example
├── MainActivity.kt
├── data
│   ├── db            # Room v4: songs, playlists, DAOs, MIGRATION_3_4
│   ├── lyrics        # LyricsService: LRCLIB → lyrics.ovh → Lyrica, LRC, translation
│   ├── model         # Song, RankTier, SongSortOrder, SessionEvaluation
│   ├── preferences   # PlaybackPreferences (resume, toggles, language)
│   └── repository    # SongRepository, PlaylistRepository
├── engine            # SmartRankingEngine, PlaybackMode
├── player
│   ├── SmartAudioPlayer.kt        # ExoPlayer wrapper, queue, lyrics state
│   ├── PlaybackService.kt         # MediaSessionService + media-button callback
│   ├── BluetoothAudioReceiver.kt
│   ├── AudioEffectManager.kt      # EQ, BassBoost, Virtualizer (off by default)
│   ├── ProceduralAudioGenerator.kt
│   ├── WaveControlManager.kt      # proximity-sensor gesture (not wired to UI yet)
│   └── ReplayGain*.kt             # infrastructure present, disabled for performance
├── ui
│   ├── components    # LyricsPanel, MiniPlayerBar, NowPlayingSection, dialogs, tabs
│   ├── screens       # HomeScreen
│   ├── theme         # Dark neon M3 theme + Motion tokens
│   └── viewmodel     # MusicPlayerViewModel, PlayerUiState, AppTab
└── util              # FileLogger, SongShareHelper

```

## Tech stack

| Layer | Choice |
|---|---|
| UI | Jetpack Compose, Material 3, dark neon theme, Motion tokens |
| Playback | Media3 ExoPlayer 1.6.x, `MediaSessionService` |
| Persistence | Room v4 (`songs`, `playlists`), `SharedPreferences` for playback state |
| Lyrics | LRCLIB, lyrics.ovh, Lyrica; Google Translate for translation |
| Build | AGP + Gradle wrapper; `compileSdk 36`, `minSdk 24`, `targetSdk 36`, Java 11 |
| CI | GitHub Actions, JDK 21, Android SDK 36, persistent keystore vault |

## CI/CD

The repository ships a single workflow: **`.github/workflows/release.yml`** (`Build & Release`).

- **Triggers:** push a tag matching `v*`, or run manually with a version input.
- **Environment:** JDK 21 (Temurin), Android platform 36, build-tools 36.0.0, Gradle wrapper.
- **Tag creation:** on manual dispatch, the workflow creates and pushes the tag if it does not exist.
- **Persistent signing keystore (vault):** on the first run the workflow generates `my-upload-key.jks` and stores it as a GitHub Release asset under the tag `_keystore-vault`. Later runs download the same file, so every release is signed with the same key.
- **Do not delete the `_keystore-vault` release** unless you intend to change the app signature and break updates.
- **Optional secret:** `GOOGLE_SERVICES_JSON_BASE64` for Firebase. Without it, a stub `google-services.json` is generated.
- **Outputs:** Debug and Release APKs are uploaded as workflow artifacts and attached to a GitHub Release.

> **Security note.** The vault keystore is a release asset and the keystore password is currently defined in the workflow file. That is convenient for a personal project but means the key is not secret. Before publishing on Google Play or distributing widely, move the keystore and passwords to repository secrets and restrict the vault release.

## Local build

```bash
git clone https://github.com/mablue/SmartMusicPlayer.git
cd SmartMusicPlayer
chmod +x gradlew

# Debug
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk

# Release (requires KEYSTORE_PATH / STORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD,
# or a my-upload-key.jks at the repo root)
./gradlew assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

Termux note

On Termux, install openjdk-21, gradle, aapt2, android-tools, and an Android SDK. Then add:

```properties
android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2
```

to ~/.gradle/gradle.properties so Gradle uses the Termux aapt2 binary instead of the x86_64 Maven one.

Localization

· Default strings: app/src/main/res/values/strings.xml (English).
· Persian strings: app/src/main/res/values-fa/strings.xml (automatic fallback when the device locale is fa).
· Persian README: README.fa.md.

Roadmap & known limitations

· Persian / Turkish lyrics coverage is weak: LRCLIB and Lyrica have limited data, and no public Persian lyrics API is integrated yet.
· ReplayGain classes exist but the audio processor is disabled for performance.
· Wave control (WaveControlManager, proximity sensor) exists but is not exposed in Settings.
· Dead UI components: AnalyticsTab, RankingsTabContent, DiscoveryTabContent, LyricsViewDialog are present but not linked to navigation.
· Versioning: versionCode = 1, versionName = "1.0" — bump before public releases.
· No Android Auto, home-screen widget, Wear OS, or Chromecast.
· fallbackToDestructiveMigration() remains as a last resort alongside MIGRATION_3_4.

Contributing

Issues and pull requests are welcome. For large changes, open an issue first so the approach can be discussed.

License

MIT — see LICENSE.
