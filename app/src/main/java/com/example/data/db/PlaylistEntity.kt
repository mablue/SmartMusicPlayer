package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val songIdsJson: String = "[]",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getSongIds(): List<Long> {
        return try {
            val jsonArray = JSONArray(songIdsJson)
            val list = mutableListOf<Long>()
            for (i in 0 until jsonArray.length()) {
                list.add(jsonArray.getLong(i))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        fun fromSongIds(name: String, songIds: List<Long>, description: String = ""): PlaylistEntity {
            val jsonArray = JSONArray()
            songIds.forEach { jsonArray.put(it) }
            return PlaylistEntity(
                name = name,
                description = description,
                songIdsJson = jsonArray.toString()
            )
        }
    }
}
