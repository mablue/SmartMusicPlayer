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

    private val prefs by lazy { com.example.data.preferences.PlaybackPreferences.getInstance(context) }

    val allSongs: Flow<List<Song>> = songDao.getAllSongs().map { list ->
        list.map { it.toSong().copy(resumePositionMs = prefs.getSongResumePosition(it.id)) }
    }

    val rankedSongs: Flow<List<Song>> = songDao.getRankedSongs().map { list ->
        list.map { it.toSong().copy(resumePositionMs = prefs.getSongResumePosition(it.id)) }
    }

    val unplayedSongs: Flow<List<Song>> = songDao.getUnplayedSongs().map { list ->
        list.map { it.toSong().copy(resumePositionMs = prefs.getSongResumePosition(it.id)) }
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

    /**
     * Physically deletes the audio file from device storage and removes it from the database.
     */
    suspend fun deleteSongFileAndDb(song: Song): Boolean = withContext(Dispatchers.IO) {
        var fileDeleted = false
        try {
            if (song.uri.isNotEmpty()) {
                val uri = android.net.Uri.parse(song.uri)
                if (uri.scheme == "content") {
                    try {
                        val rows = context.contentResolver.delete(uri, null, null)
                        fileDeleted = (rows > 0)
                    } catch (sec: SecurityException) {
                        android.util.Log.w("SongRepository", "SecurityException on direct delete: ${sec.message}")
                    }
                } else {
                    val filePath = if (uri.scheme == "file") uri.path ?: song.uri else song.uri
                    val file = java.io.File(filePath)
                    if (file.exists()) {
                        fileDeleted = file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SongRepository", "Error deleting physical audio file", e)
        }

        // Delete from Room database
        songDao.deleteSong(song.id)
        fileDeleted
    }

    /**
     * On Android 11+ (API 30+), MediaStore requires system consent dialog for deleting
     * external media files owned by other apps.
     */
    fun getDeletePendingIntent(song: Song): android.app.PendingIntent? {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            val uri = android.net.Uri.parse(song.uri)
            if (uri.scheme == "content") {
                return try {
                    android.provider.MediaStore.createDeleteRequest(
                        context.contentResolver,
                        listOf(uri)
                    )
                } catch (e: Exception) {
                    null
                }
            }
        }
        return null
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
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE
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
                val dataCol = it.getColumnIndex(MediaStore.Audio.Media.DATA)
                val sizeCol = it.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val mimeCol = it.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)

                while (it.moveToNext()) {
                    val mediaId = it.getLong(idCol)
                    val title = it.getString(titleCol)?.trim() ?: "Unknown Track"
                    val artist = it.getString(artistCol)?.trim() ?: "Unknown Artist"
                    val album = it.getString(albumCol)?.trim() ?: "Album"
                    val duration = it.getLong(durationCol)
                    val filePath = if (dataCol >= 0) it.getString(dataCol) ?: "" else ""
                    val fileSizeBytes = if (sizeCol >= 0) it.getLong(sizeCol) else 0L
                    val mimeType = if (mimeCol >= 0) it.getString(mimeCol) ?: "audio/mpeg" else "audio/mpeg"

                    // Extract parent folder name
                    val folderName = if (filePath.isNotEmpty()) {
                        try {
                            java.io.File(filePath).parentFile?.name ?: "Music"
                        } catch (e: Exception) {
                            "Music"
                        }
                    } else "Music"

                    // Low-cost heuristic language & genre detection
                    val detectedLang = when {
                        Regex("[\u0600-\u06FF]").containsMatchIn("$title $artist $album") -> "Persian"
                        Regex("[a-zA-Z]").containsMatchIn("$title $artist") -> "English"
                        else -> "Instrumental"
                    }

                    val detectedGenre = when {
                        folderName.contains("rock", ignoreCase = true) || title.contains("rock", ignoreCase = true) -> "Rock"
                        folderName.contains("pop", ignoreCase = true) || title.contains("pop", ignoreCase = true) -> "Pop"
                        folderName.contains("electro", ignoreCase = true) || folderName.contains("edm", ignoreCase = true) || title.contains("remix", ignoreCase = true) -> "Electronic"
                        folderName.contains("rap", ignoreCase = true) || folderName.contains("hip", ignoreCase = true) -> "Hip-Hop"
                        folderName.contains("traditional", ignoreCase = true) || folderName.contains("sonati", ignoreCase = true) -> "Traditional"
                        folderName.contains("jazz", ignoreCase = true) -> "Jazz"
                        folderName.contains("classic", ignoreCase = true) -> "Classical"
                        else -> "Music"
                    }

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
                            score = 50.0f,
                            folderName = folderName,
                            fileSizeBytes = fileSizeBytes,
                            bitrateKbps = 320,
                            mimeType = mimeType,
                            detectedLanguage = detectedLang,
                            genre = detectedGenre
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
