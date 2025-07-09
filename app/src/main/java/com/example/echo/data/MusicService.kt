package com.example.echo.data

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import android.os.Binder
import android.os.IBinder
import com.example.echo.data.database.AppDatabase
import com.example.echo.data.database.PlaylistDao
import com.example.echo.data.database.PlaylistEntity
import com.example.echo.data.database.PlaylistSongCrossRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

class MusicService : Service(), MediaPlayer.OnCompletionListener {

    private var mediaPlayer: MediaPlayer? = null
    private var musicList: List<Music> = emptyList()
    private var currentSongIndex: Int = -1
    private val binder = MusicBinder()

    private lateinit var playlistDao: PlaylistDao
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists = _playlists.asStateFlow()

    private val _currentPosition = MutableStateFlow(0f)
    val currentPosition = _currentPosition.asStateFlow()

    private val _totalDuration = MutableStateFlow(0f)
    val totalDuration = _totalDuration.asStateFlow()

    private var positionUpdateTimer: Timer? = null

    private var playlistSongs: List<Music> = emptyList()
    private var currentPlaylistId: Long? = null

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        playlistDao = AppDatabase.getDatabase(applicationContext).playlistDao()
        serviceScope.launch {
            _playlists.value = getPlaylistsFromDb()
        }
    }

    override fun onCompletion(mp: MediaPlayer?) {
        playNextSong()
    }

    fun setMusicList(list: List<Music>) {
        musicList = list
    }

    fun playSong(index: Int) {
        if (index < 0 || index >= musicList.size) {
            Log.e("MusicService", "Invalid song index: $index")
            return
        }

        currentSongIndex = index
        val song = musicList[currentSongIndex]

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()

            mediaPlayer?.apply {
                setDataSource(song.data)
                prepareAsync() // Use prepareAsync for non-blocking preparation
                setOnPreparedListener { mp ->
                    mp.start()
                    setOnCompletionListener(this@MusicService)
                    _totalDuration.value = mp.duration.toFloat()
                    startPositionUpdates()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e("MusicService", "MediaPlayer error: what=$what, extra=$extra for song: ${song.title}")
                    Toast.makeText(applicationContext, "Error playing song: ${song.title}", Toast.LENGTH_SHORT).show()
                    mp.release()
                    mediaPlayer = null
                    stopPositionUpdates()
                    true // Indicate that the error was handled
                }
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Exception during playSong for ${song.title}: ", e)
            Toast.makeText(applicationContext, "Error playing song: ${song.title}", Toast.LENGTH_SHORT).show()
            mediaPlayer?.release()
            mediaPlayer = null
            stopPositionUpdates()
        }
    }

    fun playPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                stopPositionUpdates()
            } else {
                it.start()
                startPositionUpdates()
            }
        }
    }

    fun seekTo(position: Float) {
        mediaPlayer?.seekTo(position.toInt())
        _currentPosition.value = position
    }

    

    private fun startPositionUpdates() {
        positionUpdateTimer?.cancel()
        positionUpdateTimer = Timer()
        positionUpdateTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                _currentPosition.value = mediaPlayer?.currentPosition?.toFloat() ?: 0f
            }
        }, 0, 1000) // Update every second
    }

    private fun stopPositionUpdates() {
        positionUpdateTimer?.cancel()
        positionUpdateTimer = null
    }

    fun playNextSong() {
        _currentPosition.value = 0f
        if (currentPlaylistId != null) {
            if (playlistSongs.isEmpty()) return
            val currentSong = getCurrentSong() ?: return
            val currentPlaylistIndex = playlistSongs.indexOf(currentSong)

            if (currentPlaylistIndex != -1) {
                val nextPlaylistIndex = (currentPlaylistIndex + 1) % playlistSongs.size
                val nextSong = playlistSongs[nextPlaylistIndex]
                val globalIndex = musicList.indexOf(nextSong)
                if (globalIndex != -1) {
                    playSong(globalIndex)
                }
            } else {
                if (playlistSongs.isNotEmpty()) {
                    playSong(musicList.indexOf(playlistSongs.first()))
                }
            }
        } else {
            if (musicList.isEmpty()) return
            val nextIndex = (currentSongIndex + 1) % musicList.size
            playSong(nextIndex)
        }
    }

    fun playPreviousSong() {
        if (currentPlaylistId != null) {
            if (playlistSongs.isEmpty()) return
            val currentSong = getCurrentSong() ?: return
            val currentPlaylistIndex = playlistSongs.indexOf(currentSong)

            if (currentPlaylistIndex != -1) {
                val prevPlaylistIndex = if (currentPlaylistIndex - 1 < 0) playlistSongs.size - 1 else currentPlaylistIndex - 1
                val prevSong = playlistSongs[prevPlaylistIndex]
                val globalIndex = musicList.indexOf(prevSong)
                if (globalIndex != -1) {
                    playSong(globalIndex)
                }
            } else {
                if (playlistSongs.isNotEmpty()) {
                    playSong(musicList.indexOf(playlistSongs.last()))
                }
            }
        } else {
            if (musicList.isEmpty()) return
            val prevIndex = if (currentSongIndex - 1 < 0) musicList.size - 1 else currentSongIndex - 1
            playSong(prevIndex)
        }
    }

    

    fun getCurrentSong(): Music? {
        return if (currentSongIndex != -1 && currentSongIndex < musicList.size) {
            musicList[currentSongIndex]
        } else {
            null
        }
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying ?: false
    }

    suspend fun createPlaylist(name: String) {
        val playlistEntity = PlaylistEntity(playlistName = name)
        playlistDao.insertPlaylist(playlistEntity)
        _playlists.value = getPlaylistsFromDb()
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        playlistDao.insertPlaylistSongCrossRef(PlaylistSongCrossRef(playlistId, songId))
        _playlists.value = getPlaylistsFromDb()
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.deleteSongFromPlaylist(playlistId, songId)
        _playlists.value = getPlaylistsFromDb()
    }

    private suspend fun getPlaylistsFromDb(): List<Playlist> {
        val playlistEntities = playlistDao.getAllPlaylists()
        return playlistEntities.map { entity ->
            val songIds = playlistDao.getSongsForPlaylist(entity.playlistId)
            Playlist(entity.playlistId, entity.playlistName, songIds.toMutableList())
        }
    }

    suspend fun playPlaylist(playlistId: Long) {
        currentPlaylistId = playlistId
        val songIds = playlistDao.getSongsForPlaylist(playlistId)
        playlistSongs = musicList.filter { music -> music.id in songIds }
        if (playlistSongs.isNotEmpty()) {
            playSong(musicList.indexOf(playlistSongs.first()))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        stopPositionUpdates()
    }
}