package com.example.echo.data

data class Music(
    val id: Long,
    val title: String,
    val artist: String,
    val data: String, // file path
    val duration: Long = 0 // Duration in milliseconds
)