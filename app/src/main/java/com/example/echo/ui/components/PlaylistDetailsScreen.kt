package com.example.echo.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.echo.data.Music

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailsScreen(playlistName: String, songs: List<Music>, onSongClick: (Music) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(playlistName) })
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            LazyColumn {
                items(songs) { song ->
                    MusicItem(music = song, onClick = { clickedSong ->
                        onSongClick(clickedSong)
                    }, onAddClick = {})
                }
            }
        }
    }
}
