package com.audiotune.studio.youtube

/**
 * YouTube Stream Extractor stub.
 * Prepared for future stage YouTube stream resolution and audio extraction.
 */
interface YouTubeStreamExtractor {
    suspend fun resolveAudioStream(videoId: String): String?
    suspend fun searchVideos(query: String): List<YouTubeVideoResult>
}

data class YouTubeVideoResult(
    val videoId: String,
    val title: String,
    val channelTitle: String,
    val durationSeconds: Long = 0L,
    val thumbnailUrl: String = ""
)
