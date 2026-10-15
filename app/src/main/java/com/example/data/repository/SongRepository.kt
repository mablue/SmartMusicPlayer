package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.example.data.db.SongDao
import com.example.data.db.SongEntity
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class ScanResult(
    val addedCount: Int,
    val skippedDuplicates: Int,
    val totalFound: Int
)

class SongRepository(
    private val songDao: SongDao,
    private val context: Context
) {

    val allSongs: Flow<List<Song>> = songDao.getAllSongs().map { list ->
        list.map { it.toSong() }
    }

    val rankedSongs: Flow<List<Song>> = songDao.getRankedSongs().map { list ->
        list.map { it.toSong() }
    }

    val unplayedSongs: Flow<List<Song>> = songDao.getUnplayedSongs().map { list ->
        list.map { it.toSong() }
    }

    suspend fun updateSong(song: Song) = withContext(Dispatchers.IO) {
        songDao.updateSong(SongEntity.fromSong(song))
    }

    suspend fun insertSong(song: Song): Long = withContext(Dispatchers.IO) {
        songDao.insertSong(SongEntity.fromSong(song))
    }

    suspend fun deleteSong(songId: Long) = withContext(Dispatchers.IO) {
        songDao.deleteSong(songId)
    }

    suspend fun resetAllRanks() = withContext(Dispatchers.IO) {
        songDao.resetAllRanks()
    }

    suspend fun deleteAllSongs() = withContext(Dispatchers.IO) {
        songDao.deleteAll()
    }

    suspend fun removeDefaultSeedSongs() = withContext(Dispatchers.IO) {
        songDao.deleteDefaultSeedSongs()
    }

    suspend fun getSongById(songId: Long): Song? = withContext(Dispatchers.IO) {
        songDao.getSongById(songId)?.toSong()
    }

    /**
     * Scans Android device MediaStore for local audio files.
     * Guaranteed deduplication: checks both content URI and title+artist signatures
     * to prevent any duplicate insertion when scanning multiple times.
     */
    suspend fun scanDeviceAudioFiles(): ScanResult = withContext(Dispatchers.IO) {
        var addedCount = 0
        var skippedDuplicates = 0
        var totalFound = 0

        try {
            // Retrieve existing URIs and signatures from database to prevent duplicates
            val existingUris = songDao.getAllUris().toHashSet()
            val existingSongs = allSongs.firstOrNull() ?: emptyList()
            val existingSignatures = existingSongs.map {
                "${it.title.trim().lowercase()}|${it.artist.trim().lowercase()}"
            }.toHashSet()

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (it.moveToNext()) {
                    val mediaId = it.getLong(idCol)
                    val title = it.getString(titleCol)?.trim() ?: "قطعه نامشخص"
                    val artist = it.getString(artistCol)?.trim() ?: "خواننده نامشخص"
                    val album = it.getString(albumCol)?.trim() ?: "آلبوم"
                    val duration = it.getLong(durationCol)

                    // Ignore very short audio files (notification pings under 10 seconds)
                    if (duration > 10_000L) {
                        totalFound++
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            mediaId
                        ).toString()

                        val songSignature = "${title.lowercase()}|${artist.lowercase()}"

                        // Check if already in database by URI or Title+Artist
                        if (existingUris.contains(contentUri) || existingSignatures.contains(songSignature)) {
                            skippedDuplicates++
                            continue
                        }

                        val newSong = Song(
                            id = 0,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            uri = contentUri,
                            isUnplayed = true,
                            score = 50.0f
                        )

                        val insertedId = songDao.insertSong(SongEntity.fromSong(newSong))
                        if (insertedId > 0) {
                            existingUris.add(contentUri)
                            existingSignatures.add(songSignature)
                            addedCount++
                        } else {
                            skippedDuplicates++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SongRepository", "Device media scan error", e)
        }

        ScanResult(
            addedCount = addedCount,
            skippedDuplicates = skippedDuplicates,
            totalFound = totalFound
        )
    }
}
