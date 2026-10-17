# 🎵 SmartMusic — Adaptive Ranking & Smart Shuffle Music Player

[![Android CI / CD Build](https://github.com/user/smartmusic/actions/workflows/build.yml/badge.svg)](https://github.com/user/smartmusic/actions/workflows/build.yml)
[![Platform](https://img.shields.io/badge/Platform-Android_24+-3DDC84.svg?logo=android)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

An intelligent, futuristic offline Android audio player built with **Jetpack Compose (Material 3)**, **Room Database**, and a dynamic **Behavioral Affinity Ranking Engine**. 

Instead of treating all songs equally or requiring cumbersome 5-star manual tagging, SmartMusic learns your genuine music affinity in real time by observing how long you actually listen to each song.

---

## 🌟 Key Features

### 🧠 1. Smart Behavioral Affinity Engine
- **Fair Exploration Score (50 pts):** Newly discovered tracks enter the library with a neutral score of 50, guaranteeing they receive immediate playtime in Smart Shuffle.
- **First Impression Calibration:** On the very first listen, the placeholder score of 50 resets directly based on engagement:
  - Skip before 20s $\to$ Resets to **20 pts** (*Disliked Tier*).
  - Listened through $\ge 75\%$ $\to$ Resets to **80 pts** (*Diamond/Gold Tier*).
- **Recent Fatigue Protection (8-Minute Window):** If you recently completed a full listen of a song, skipping it during a subsequent play does **not** penalize the song. The engine recognizes natural listening fatigue rather than genuine dislike.
- **Weighted Mathematical Shuffle:** Playback probability in Smart Shuffle mode is proportional to:
  $$P(\text{song}_i) = \frac{(\text{Score}_i)^{1.3}}{\sum (\text{Score}_j)^{1.3}}$$
  Your favorite tracks surface frequently without completely starving newer or neutral songs.

### 🎧 2. Full Media Notification & Background Service
- Real-time **Foreground Media Playback Service** with `android.media.session.MediaSession`.
- Full lockscreen controls: **Previous**, **Play / Pause**, **Next**, and custom **+20 Love** boost action.
- Automatic track title, artist, score points, and tier display directly on the system lockscreen and notification shade.

### 🌐 3. Real-Time Lyrics HUD & Simultaneous Translation
- High-speed query engine integrating **LRCLIB** (open-source synced lyrics registry).
- **Synchronized HUD Mode:** Highlights the active line with smooth auto-scrolling to the playback timestamp. Tap any line to seek immediately.
- **Simultaneous Translation:** One-tap real-time translation into Persian (`fa`) or English (`en`) without requiring paid third-party API keys.

### 📡 4. Hardware Telemetry & Low-Cost Metadata Extraction
- **Zero-lag Heuristics:** Automatically detects song genre, language (Persian, English, Instrumental), folder hierarchy, and file size in megabytes without disk strain.
- **Bluetooth Auto-Play & Safety Disconnect:** Automatically resumes playback when connecting to Bluetooth headsets or car audio units. Automatically pauses when disconnected to prevent public loudspeaker blasting (`AUDIO_BECOMING_NOISY`).

### 🗑️ 5. Android 11+ Scoped Storage Physical Deletion
- True storage deletion via `MediaStore.createDeleteRequest` and ContentResolver fallback.
- **Smart Next-Track Transition:** Deleting a song gracefully advances to the next track in the queue without interrupting playback state or applying negative algorithmic penalties.
- Notch-safe and status-bar-safe padding to guarantee complete visibility on modern edge-to-edge displays.

### 💾 6. State Persistence
- Remembers active track ID, position in milliseconds, queue configuration, and user preferences across app restarts.

---

## 🏗️ Architecture & Technology Stack

```
SmartMusic
├── data
│   ├── db              # Room DB: SongEntity, SongDao, Migrations
│   ├── lyrics          # LRCLIB & Google Translate client
│   ├── model           # Immutable Song models, RankTier, SessionEvaluation
│   ├── preferences     # PlaybackPreferences (State preservation)
│   └── repository      # SongRepository (MediaStore scanner, physical deletion)
├── engine              # SmartRankingEngine & PlaybackMode enums
├── player              # SmartAudioPlayer, MediaPlaybackService, BluetoothAudioReceiver
├── ui
│   ├── components      # FullPlayerContent, MiniPlayerBar, LyricsViewDialog, VisualizerWaves
│   ├── screens         # HomeScreen, LibraryTab, RankingsTab, DiscoveryTab, AnalyticsTab
│   ├── theme           # Cyberpunk/Neon Dark M3 Theme
│   └── viewmodel       # MusicPlayerViewModel
└── MainActivity.kt     # Edge-to-edge Compose container
```

---

## 🚀 CI / CD Build Workflow

This repository includes a ready-to-run **GitHub Actions Workflow** located at `.github/workflows/build.yml`.

### Automated Artifacts
Every push to `main`, `master`, or tags will automatically:
1. Initialize JDK 17 and configure Android SDK build tools.
2. Generate signing keys (either from your repository secrets or self-signed CI fallback).
3. Build both **Debug** and **Release** APKs.
4. Upload both APKs to the GitHub Actions **Artifacts** tab for instant download.

### Custom Release Signing (Optional)
To sign release APKs with your production keystore, add the following secrets in **GitHub Repository Settings $\to$ Secrets and variables $\to$ Actions**:
- `SIGNING_KEYSTORE_BASE64`: Base64 encoded `.jks` or `.keystore` file (`base64 -w 0 my-upload-key.jks`).
- `KEYSTORE_PASSWORD`: Keystore password.
- `KEY_ALIAS`: Key alias.
- `KEY_PASSWORD`: Key password.

---

## 🛠️ Local Build Instructions

```bash
# 1. Clone repository
git clone https://github.com/user/smartmusic.git
cd smartmusic

# 2. Grant gradlew permissions
chmod +x gradlew

# 3. Build Debug APK
./gradlew assembleDebug

# 4. Build Release APK
./gradlew assembleRelease

# Output APK paths:
# app/build/outputs/apk/debug/app-debug.apk
# app/build/outputs/apk/release/app-release.apk
```

---

## 🌍 Localization & Language Support

- **Default Language:** English (`values/strings.xml`).
- **Persian Language:** Automatic fallback (`values-fa/strings.xml`) only when the device system locale is set to Persian (`fa`).

---

## 📄 License
This project is open-source and available under the [MIT License](LICENSE).
