<!-- LATEST_RELEASE_START -->
<!-- LATEST_RELEASE_END -->

# SmartPlayer 🎵

> A smart music player that learns what you love — one skip at a time.

SmartPlayer is an Android music player built with **Kotlin** and **Jetpack Compose** that ranks every track by how you actually listen to it. No manual ratings. No playlists to curate. The algorithm watches your behavior and quietly decides what deserves a spot in the shuffle.

---

## ✨ Features

- 🎧 **Smart weighted shuffle** — tracks are picked by score, not randomly
- 🧠 **Learns from behavior** — skips, listens, and re-listens all feed the ranking
- 🛡️ **Fatigue protection** — recent skips don't get punished twice
- 🎨 **Modern Compose UI** — Material 3, dark theme, smooth animations
- 📊 **Analytics tab** — see your top tracks and listening trends
- 🔒 **100% offline** — no accounts, no cloud, no tracking

---

## 🧮 Ranking & Selection Formula

SmartPlayer doesn't ask "did you like this?" — it watches **how** you listen. Every track carries a score that evolves with each play, and the shuffle is weighted so higher-scored tracks show up more often.

### 1. First encounter — *Unplayed tracks*

Every new track enters the library with a **neutral score of `50`**.
This guarantees a fair chance in the smart shuffle — no track is buried before it's ever heard.

### 2. First impression — *The reset rule*

The first play is special. It decides the track's starting personality:

| Behavior on first play | New score | Label |
|---|---|---|
| Skipped **before 20 seconds** | `20` | 🔻 *Disliked* |
| Listened **past 20 seconds** | `80` | ⭐ *Gold affinity* |

The `50` placeholder is discarded — the track is now judged by how you actually reacted.

### 3. Subsequent plays — *The ongoing opinion*

| Behavior | Change |
|---|---|
| Early skip (< 20s) | `−20` |
| Full play | `+20` |

Each listen nudges the score up or down. A track you love climbs to `100+`. A track you hate sinks to `0` and stops appearing in the shuffle.

### 4. Fatigue protection — *The 8-minute rule*

If you skip a track that was **already played within the last 8 minutes**, the penalty is **bypassed**.

*Why?* You might skip because you're not in the mood, not because you dislike the song. Without this rule, one bad session could permanently damage the score of a track you actually love.

### 5. Weighted shuffle — *Probability is not linear*

When picking the next track, SmartPlayer doesn't just favor higher scores — it **supercharges** them.
