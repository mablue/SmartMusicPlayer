package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY id ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY score DESC, playCount DESC")
    fun getRankedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isUnplayed = 1 ORDER BY id ASC")
    fun getUnplayedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :songId LIMIT 1")
    suspend fun getSongById(songId: Long): SongEntity?

    @Query("SELECT uri FROM songs")
    suspend fun getAllUris(): List<String>

    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSong(song: SongEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(songs: List<SongEntity>)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("DELETE FROM songs WHERE id = :songId")
    suspend fun deleteSong(songId: Long)

    @Query("DELETE FROM songs WHERE uri LIKE '%smart_song_%'")
    suspend fun deleteDefaultSeedSongs()

    @Query("UPDATE songs SET score = 50.0, isUnplayed = 1, playCount = 0, skipCount = 0, lastListenRatio = 0.0")
    suspend fun resetAllRanks()

    @Query("DELETE FROM songs")
    suspend fun deleteAll()
}
