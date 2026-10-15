package com.example

import com.example.data.model.Song
import com.example.engine.PlaybackMode
import com.example.engine.SmartRankingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartRankingEngineTest {

    @Test
    fun unplayedSong_earlySkip_resetsScoreToDisliked() {
        val initialSong = Song(
            id = 1,
            title = "Test Song",
            artist = "Artist",
            durationMs = 180_000L,
            isUnplayed = true,
            score = 50.0f
        )

        // Skipped at 10 seconds (< 20s threshold and < 20% duration)
        val (updatedSong, evaluation) = SmartRankingEngine.evaluateSession(
            song = initialSong,
            listenedMs = 10_000L,
            totalMs = 180_000L,
            isManualSkip = true
        )

        assertFalse("Song should no longer be unplayed", updatedSong.isUnplayed)
        assertEquals("Score should be reset to 20.0f for early skip", 20.0f, updatedSong.score, 0.01f)
        assertEquals(1, updatedSong.skipCount)
        assertEquals(0, updatedSong.playCount)
        assertFalse(evaluation.isPositive)
        assertTrue(evaluation.wasUnplayed)
    }

    @Test
    fun unplayedSong_fullListen_resetsScoreToLoved() {
        val initialSong = Song(
            id = 2,
            title = "Test Song 2",
            artist = "Artist",
            durationMs = 180_000L,
            isUnplayed = true,
            score = 50.0f
        )

        // Completed 95% of song
        val (updatedSong, evaluation) = SmartRankingEngine.evaluateSession(
            song = initialSong,
            listenedMs = 175_000L,
            totalMs = 180_000L,
            isManualSkip = false
        )

        assertFalse("Song should no longer be unplayed", updatedSong.isUnplayed)
        assertEquals("Score should be reset to 80.0f for full listen", 80.0f, updatedSong.score, 0.01f)
        assertEquals(1, updatedSong.playCount)
        assertEquals(0, updatedSong.skipCount)
        assertTrue(evaluation.isPositive)
        assertTrue(evaluation.wasUnplayed)
    }

    @Test
    fun playedSong_subsequentEarlySkip_penalizesScore() {
        val playedSong = Song(
            id = 3,
            title = "Played Song",
            artist = "Artist",
            durationMs = 180_000L,
            isUnplayed = false,
            score = 70.0f,
            playCount = 1,
            skipCount = 0
        )

        val (updatedSong, _) = SmartRankingEngine.evaluateSession(
            song = playedSong,
            listenedMs = 5_000L,
            totalMs = 180_000L,
            isManualSkip = true
        )

        assertEquals("Score should decrease by 20", 50.0f, updatedSong.score, 0.01f)
        assertEquals(1, updatedSong.skipCount)
    }

    @Test
    fun playedSong_recentPlay_earlySkip_doesNotPenalize() {
        val playedSong = Song(
            id = 5,
            title = "Recently Heard Song",
            artist = "Artist",
            durationMs = 180_000L,
            isUnplayed = false,
            score = 75.0f,
            playCount = 2,
            skipCount = 0,
            lastPlayedTimestamp = System.currentTimeMillis() - (60 * 1000L) // 1 minute ago
        )

        // User skips after 5 seconds because they just heard it
        val (updatedSong, evaluation) = SmartRankingEngine.evaluateSession(
            song = playedSong,
            listenedMs = 5_000L,
            totalMs = 180_000L,
            isManualSkip = true
        )

        assertEquals("Score should NOT decrease when skipped shortly after playing", 75.0f, updatedSong.score, 0.01f)
        assertEquals("Skip count should not increase for fatigue skip", 0, updatedSong.skipCount)
        assertTrue(evaluation.reasonFa.contains("خستگی"))
    }

    @Test
    fun playedSong_consecutiveReplay_givesBonus() {
        val playedSong = Song(
            id = 4,
            title = "Favorite Song",
            artist = "Artist",
            durationMs = 180_000L,
            isUnplayed = false,
            score = 70.0f,
            playCount = 1,
            skipCount = 0
        )

        val (updatedSong, evaluation) = SmartRankingEngine.evaluateSession(
            song = playedSong,
            listenedMs = 180_000L,
            totalMs = 180_000L,
            isManualSkip = false,
            isConsecutiveReplay = true
        )

        assertEquals("Score should get replay bonus (+28)", 98.0f, updatedSong.score, 0.01f)
        assertTrue(evaluation.isPositive)
    }

    @Test
    fun smartShuffle_selectsValidCandidate() {
        val playlist = listOf(
            Song(id = 1, title = "High Ranked", artist = "A", score = 95.0f, isUnplayed = false),
            Song(id = 2, title = "Unplayed", artist = "B", score = 50.0f, isUnplayed = true),
            Song(id = 3, title = "Skipped", artist = "C", score = 10.0f, isUnplayed = false)
        )

        val selected = SmartRankingEngine.selectNextSong(
            playlist = playlist,
            currentSongId = 1,
            mode = PlaybackMode.SMART_SHUFFLE
        )

        assertNotNull(selected)
        assertTrue(selected!!.id == 2L || selected.id == 3L)
    }
}
