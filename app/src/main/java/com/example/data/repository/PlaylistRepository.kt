package com.example.data.repository

import com.example.data.db.PlaylistDao
import com.example.data.db.PlaylistEntity
import com.example.data.db.SongDao
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao
) {
    val allPlaylists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun createPlaylist(name: String, songIds: List<Long> = emptyList(), description: String = ""): Long =
        withContext(Dispatchers.IO) {
            val entity = PlaylistEntity.fromSongIds(name.trim(), songIds, description)
            playlistDao.insert(entity)
        }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return@withContext
        val currentIds = playlist.getSongIds().toMutableList()
        if (!currentIds.contains(songId)) {
            currentIds.add(songId)
            val updated = PlaylistEntity.fromSongIds(playlist.name, currentIds, playlist.description).copy(id = playlist.id)
            playlistDao.update(updated)
        }
    }

    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<Long>) = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return@withContext
        val currentIds = playlist.getSongIds().toMutableList()
        var changed = false
        for (id in songIds) {
            if (!currentIds.contains(id)) {
                currentIds.add(id)
                changed = true
            }
        }
        if (changed) {
            val updated = PlaylistEntity.fromSongIds(playlist.name, currentIds, playlist.description).copy(id = playlist.id)
            playlistDao.update(updated)
        }
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getPlaylistById(playlistId) ?: return@withContext
        val currentIds = playlist.getSongIds().filter { it != songId }
        val updated = PlaylistEntity.fromSongIds(playlist.name, currentIds, playlist.description).copy(id = playlist.id)
        playlistDao.update(updated)
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        playlistDao.deleteById(playlistId)
    }

    suspend fun getSongsForPlaylist(playlist: PlaylistEntity): List<Song> = withContext(Dispatchers.IO) {
        val songIds = playlist.getSongIds()
        val allSongs = songDao.getAllSongsOnce().map { it.toSong() }
        val songMap = allSongs.associateBy { it.id }
        songIds.mapNotNull { songMap[it] }
    }
}
