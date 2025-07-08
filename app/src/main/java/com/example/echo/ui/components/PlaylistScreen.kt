package com.example.echo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.echo.data.Music
import com.example.echo.data.Playlist
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlists: List<Playlist>,
    musicList: List<Music>,
    onCreatePlaylist: (String) -> Unit,
    onAddSongToPlaylist: (Long, Long) -> Unit,
    onPlayPlaylist: (Long) -> Unit,
    onPlaylistClick: (Long) -> Unit
) {
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("My Playlists") })
        }
    ) { paddingValues ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp)) {
            Button(onClick = { showCreatePlaylistDialog = true }) {
                Text("Create New Playlist")
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(playlists) { playlist ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onPlaylistClick(playlist.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = playlist.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = "${playlist.songIds.size} songs",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                                )
                            }
                            IconButton(onClick = { onPlayPlaylist(playlist.id) }) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Play Playlist", tint = Color(0xFFc79818))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create New Playlist") },
            text = {
                TextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreatePlaylist(newPlaylistName)
                        showCreatePlaylistDialog = false
                        newPlaylistName = ""
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}