package com.example.echo.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val songId: Long,
    val title: String,
    val artist: String,
    val data: String
)
