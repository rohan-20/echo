package com.example.echo

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.echo.data.Music
import com.example.echo.data.MusicRepository
import com.example.echo.data.MusicService
import com.example.echo.ui.components.MusicListScreen
import com.example.echo.ui.components.PlaylistDetailsScreen
import com.example.echo.ui.components.PlaylistScreen
import com.example.echo.ui.components.PlaylistSelectionDialog
import com.example.echo.ui.theme.EchoTheme
import kotlinx.coroutines.launch
import java.util.*
import com.example.echo.R

class MainActivity : ComponentActivity() {

    private var musicService by mutableStateOf<MusicService?>(null)
    private var isBound = false
    private lateinit var musicRepository: MusicRepository

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true
            musicService?.setMusicList(musicRepository.getAllMusic())
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            musicService = null
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            musicRepository = MusicRepository(applicationContext)
            val intent = Intent(this, MusicService::class.java)
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } else {
            // Show permission denied message or disable music functionality
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permissionToRequest) == PackageManager.PERMISSION_GRANTED -> {
                Log.d("MainActivity", "Permission already granted: $permissionToRequest")
                musicRepository = MusicRepository(applicationContext)
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
            else -> {
                Log.d("MainActivity", "Requesting permission: $permissionToRequest")
                requestPermissionLauncher.launch(permissionToRequest)
            }
        }

        setContent {
            EchoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val musicList = remember { mutableStateListOf<Music>() }
                    val playlists by musicService?.playlists?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(emptyList()) }
                    var currentSong by remember { mutableStateOf<Music?>(null) }
                    var isPlaying by remember { mutableStateOf(false) }
                    var selectedTab by remember { mutableStateOf(0) }
                    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
                    var showPlaylistDialog by remember { mutableStateOf(false) }
                    var selectedSongForPlaylist by remember { mutableStateOf<Music?>(null) }

                    val currentPosition by musicService?.currentPosition?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(0f) }
                    val totalDuration by musicService?.totalDuration?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(0f) }

                    val coroutineScope = rememberCoroutineScope()

                    LaunchedEffect(musicService) {
                        if (musicService != null && ::musicRepository.isInitialized) {
                            musicList.clear()
                            musicList.addAll(musicRepository.getAllMusic())
                        }
                    }

                    // TODO: Replace with Flow-based updates from MusicService
                    DisposableEffect(musicService) {
                        val updatePlaybackState = {
                            currentSong = musicService?.getCurrentSong()
                            isPlaying = musicService?.isPlaying() ?: false
                        }
                        val timer = Timer()
                        timer.scheduleAtFixedRate(object : TimerTask() {
                            override fun run() {
                                updatePlaybackState()
                            }
                        }, 0, 1000)
                        onDispose { timer.cancel() }
                    }

                    

                    Scaffold(
                        bottomBar = {
                            Column {
                                PlaybackControls(
                                    currentSong = currentSong,
                                    isPlaying = isPlaying,
                                    currentPosition = currentPosition,
                                    totalDuration = totalDuration,
                                    onPlayPauseClick = { musicService?.playPause() },
                                    onNextClick = { musicService?.playNextSong() },
                                    onPreviousClick = { musicService?.playPreviousSong() },
                                    onSeek = { position -> musicService?.seekTo(position) }
                                )
                                if (selectedPlaylistId == null) {
                                    TabRow(selectedTabIndex = selectedTab) {
                                        Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                                            Text("Songs", modifier = Modifier.padding(16.dp))
                                        }
                                        Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                                            Text("Playlists", modifier = Modifier.padding(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    ) { paddingValues ->
                        Column(modifier = Modifier.padding(paddingValues)) {
                            Image(
                                painter = painterResource(id = R.drawable.echo_banner),
                                contentDescription = "Echo Banner",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            )
                            if (selectedPlaylistId != null) {
                                val playlist = playlists.find { it.id == selectedPlaylistId }
                                if (playlist != null) {
                                    PlaylistDetailsScreen(
                                        playlistName = playlist.name,
                                        songs = musicList.filter { it.id in playlist.songIds },
                                        onSongClick = { clickedSong ->
                                            val index = musicList.indexOf(clickedSong)
                                            if (index != -1) {
                                                musicService?.playSong(index)
                                            }
                                        }
                                    )
                                }
                            } else {
                                when (selectedTab) {
                                    0 -> MusicListScreen(
                                        musicList = musicList,
                                        onSongClick = { index -> musicService?.playSong(index) },
                                        onAddSongToPlaylistClick = {
                                            selectedSongForPlaylist = it
                                            showPlaylistDialog = true
                                        }
                                    )
                                    1 -> PlaylistScreen(
                                        playlists = playlists,
                                        musicList = musicList,
                                        onCreatePlaylist = { name ->
                                            coroutineScope.launch {
                                                musicService?.createPlaylist(name)
                                            }
                                        },
                                        onAddSongToPlaylist = { playlistId, songId ->
                                            coroutineScope.launch {
                                                musicService?.addSongToPlaylist(playlistId, songId)
                                            }
                                        },
                                        onPlayPlaylist = { playlistId ->
                                            coroutineScope.launch {
                                                musicService?.playPlaylist(playlistId)
                                            }
                                        },
                                        onPlaylistClick = { playlistId ->
                                            selectedPlaylistId = playlistId
                                        }
                                    )
                                }
                            }
                        }
                    }

                    BackHandler(enabled = selectedPlaylistId != null) {
                        selectedPlaylistId = null
                    }

                    if (showPlaylistDialog) {
                        PlaylistSelectionDialog(
                            playlists = playlists,
                            onDismiss = { showPlaylistDialog = false },
                            onPlaylistSelected = { playlist ->
                                selectedSongForPlaylist?.let { song ->
                                    coroutineScope.launch {
                                        musicService?.addSongToPlaylist(playlist.id, song.id)
                                    }
                                }
                                showPlaylistDialog = false
                                selectedSongForPlaylist = null
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
        }
    }
}

@Composable
fun PlaybackControls(
    currentSong: Music?,
    isPlaying: Boolean,
    currentPosition: Float,
    totalDuration: Float,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeek: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong?.title ?: "No song playing",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1
                    )
                    Text(
                        text = currentSong?.artist ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.7f)),
                        maxLines = 1
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onPreviousClick) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White)
                    }
                    IconButton(onClick = onPlayPauseClick) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onNextClick) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White)
                    }
                }
            }

            Slider(
                value = currentPosition,
                onValueChange = onSeek,
                valueRange = 0f..totalDuration,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.secondary,
                    activeTrackColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
