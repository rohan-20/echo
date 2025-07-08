package com.example.echo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.echo.data.Music
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar

import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicListScreen(
    musicList: List<Music>,
    onSongClick: (Int) -> Unit,
    onAddSongToPlaylistClick: (Music) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("All Songs") })
        }
    ) { paddingValues ->
        LazyColumn(contentPadding = paddingValues) {
            items(musicList) { music ->
                MusicItem(
                    music = music,
                    onClick = { clickedMusic ->
                        val index = musicList.indexOf(clickedMusic)
                        onSongClick(index)
                    },
                    onAddClick = { onAddSongToPlaylistClick(it) }
                )
            }
        }
    }
}

@Composable
fun MusicItem(music: Music, onClick: (Music) -> Unit, onAddClick: (Music) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick(music) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = "Music Note",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = music.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = music.artist,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                    )
                }
            }
            IconButton(onClick = { onAddClick(music) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add to playlist")
            }
        }
    }
}