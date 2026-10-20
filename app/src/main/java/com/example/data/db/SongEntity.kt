package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Song

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String = "Album",
    val durationMs: Long = 180000L,
    val uri: String = "",
    val isUnplayed: Boolean = true,
    val playCount: Int = 0,
    val skipCount: Int = 0,
    val score: Float = 50.0f,
    val lastListenRatio: Float = 0.0f,
    val lastPlayedTimestamp: Long = 0L,
    val colorHex: String = "#8B5CF6",
    val genre: String = "Music",
    val folderName: String = "Music",
    val fileSizeBytes: Long = 0L,
    val bitrateKbps: Int = 320,
    val mimeType: String = "audio/mpeg",
    val detectedLanguage: String = "Auto",
    val lyrics: String = "",
    val syncedLyrics: String = "",
    val translatedLyrics: String = ""
) {
    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        uri = uri,
        isUnplayed = isUnplayed,
        playCount = playCount,
        skipCount = skipCount,
        score = score,
        lastListenRatio = lastListenRatio,
        lastPlayedTimestamp = lastPlayedTimestamp,
        colorHex = colorHex,
        genre = genre,
        folderName = folderName,
        fileSizeBytes = fileSizeBytes,
        bitrateKbps = bitrateKbps,
        mimeType = mimeType,
        detectedLanguage = detectedLanguage,
        lyrics = lyrics,
        syncedLyrics = syncedLyrics,
        translatedLyrics = translatedLyrics
    )

    companion object {
        fun fromSong(song: Song): SongEntity = SongEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            durationMs = song.durationMs,
            uri = song.uri,
            isUnplayed = song.isUnplayed,
            playCount = song.playCount,
            skipCount = song.skipCount,
            score = song.score,
            lastListenRatio = song.lastListenRatio,
            lastPlayedTimestamp = song.lastPlayedTimestamp,
            colorHex = song.colorHex,
            genre = song.genre,
            folderName = song.folderName,
            fileSizeBytes = song.fileSizeBytes,
            bitrateKbps = song.bitrateKbps,
            mimeType = song.mimeType,
            detectedLanguage = song.detectedLanguage,
            lyrics = song.lyrics,
            syncedLyrics = song.syncedLyrics,
            translatedLyrics = song.translatedLyrics
        )
    }
}
