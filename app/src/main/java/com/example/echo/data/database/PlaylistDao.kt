package com.example.echo.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PlaylistWithSongs(
    @Embedded val playlist: PlaylistEntity,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "songId",
        associateBy = Junction(PlaylistSongCrossRef::class)
    )
    val songs: List<SongEntity>
)

@Dao
interface PlaylistDao {
    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert
    suspend fun insertPlaylistSongCrossRef(crossRef: PlaylistSongCrossRef)

    @Query("SELECT * FROM playlists")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Transaction
    @Query("SELECT * FROM playlists")
    suspend fun getAllPlaylistsWithSongs(): List<PlaylistWithSongs>

    @Query("SELECT songId FROM PlaylistSongCrossRef WHERE playlistId = :playlistId")
    suspend fun getSongsForPlaylist(playlistId: Long): List<Long>

    @Transaction
    suspend fun createPlaylistWithSongs(playlistName: String, songIds: List<Long>): PlaylistEntity {
        val playlistId = insertPlaylist(PlaylistEntity(playlistName = playlistName))
        songIds.forEach { songId ->
            insertPlaylistSongCrossRef(PlaylistSongCrossRef(playlistId, songId))
        }
        return PlaylistEntity(playlistId, playlistName)
    }

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM PlaylistSongCrossRef WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun deleteSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("DELETE FROM PlaylistSongCrossRef WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)
}