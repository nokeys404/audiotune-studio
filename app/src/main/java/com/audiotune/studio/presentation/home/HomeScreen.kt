package com.audiotune.studio.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.audiotune.studio.core.ui.components.QuickActionButton
import com.audiotune.studio.core.ui.components.StudioHeader
import com.audiotune.studio.core.ui.components.StudioVisualizer
import com.audiotune.studio.core.ui.theme.AudioTuneTheme
import com.audiotune.studio.core.ui.theme.CyanGlow
import com.audiotune.studio.core.ui.theme.ElectricViolet
import com.audiotune.studio.core.ui.theme.NeonCyan
import com.audiotune.studio.core.ui.theme.NeonPink
import com.audiotune.studio.core.ui.theme.Slate100
import com.audiotune.studio.core.ui.theme.Slate300
import com.audiotune.studio.core.ui.theme.Slate400
import com.audiotune.studio.core.ui.theme.Slate700
import com.audiotune.studio.core.ui.theme.Slate800
import com.audiotune.studio.core.ui.theme.Slate850
import com.audiotune.studio.core.ui.theme.Slate900
import com.audiotune.studio.core.ui.theme.Slate950
import com.audiotune.studio.core.ui.theme.StudioAmber
import com.audiotune.studio.core.util.TimeUtils
import com.audiotune.studio.domain.model.Track

@Composable
fun HomeScreen(
    onNavigateToMusic: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeContent(
        uiState = uiState,
        onNavigateToMusic = onNavigateToMusic,
        onNavigateToEqualizer = onNavigateToEqualizer,
        onTrackSelected = { viewModel.onTrackSelected(it) },
        modifier = modifier
    )
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onNavigateToMusic: () -> Unit,
    onNavigateToEqualizer: () -> Unit,
    onTrackSelected: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 700.dp)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // App Header
            item {
                StudioHeader()
            }

            // Real-Time Studio Spectrum / Visualizer Section
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Slate900)
                        .border(1.dp, Slate800, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Equalizer,
                                    contentDescription = "Spectrum",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DSP MASTER OUTPUT",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate300
                                )
                            }
                            Text(
                                text = "48kHz / 24-bit",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonCyan,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        StudioVisualizer(
                            modifier = Modifier.fillMaxWidth(),
                            barCount = 32
                        )
                    }
                }
            }

            // Recently Played Section
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recently Played",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Slate100,
                            modifier = Modifier.testTag("recently_played_title")
                        )
                        Text(
                            text = "${uiState.recentlyPlayed.size} tracks",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.recentlyPlayed.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Slate900)
                                .border(1.dp, Slate800, RoundedCornerShape(14.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No recently played tracks yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate400
                            )
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(uiState.recentlyPlayed, key = { it.id }) { track ->
                                val isSelected = uiState.selectedTrack?.id == track.id
                                RecentlyPlayedCard(
                                    track = track,
                                    isSelected = isSelected,
                                    onClick = { onTrackSelected(track) }
                                )
                            }
                        }
                    }
                }
            }

            // Quick Action Buttons Section
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Studio Modules",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Slate100
                    )

                    // Music Library button
                    QuickActionButton(
                        title = "Music Library",
                        subtitle = "Browse local high-res audio & track albums",
                        icon = Icons.Default.LibraryMusic,
                        iconGradient = listOf(NeonCyan, CyanGlow),
                        testTag = "btn_music_library",
                        badgeText = "LOCAL",
                        onClick = onNavigateToMusic
                    )

                    // Equalizer button
                    QuickActionButton(
                        title = "Parametric Equalizer",
                        subtitle = "10-Band precision EQ, DSP filters & presets",
                        icon = Icons.Default.GraphicEq,
                        iconGradient = listOf(ElectricViolet, NeonPink),
                        testTag = "btn_equalizer",
                        badgeText = "DSP",
                        onClick = onNavigateToEqualizer
                    )

                    // YouTube button
                    QuickActionButton(
                        title = "YouTube Audio Stream",
                        subtitle = "Stream high-definition audio directly into EQ chain",
                        icon = Icons.Default.Subscriptions,
                        iconGradient = listOf(NeonPink, StudioAmber),
                        testTag = "btn_youtube",
                        badgeText = "PREVIEW",
                        onClick = {
                            // YouTube integration prepared for future stage
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RecentlyPlayedCard(
    track: Track,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Slate800 else Slate900)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) NeonCyan else Slate800,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .testTag("recent_track_${track.id}")
            .padding(14.dp)
    ) {
        Column {
            // Track artwork placeholder with play overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        brush = Brush.linearGradient(
                            listOf(Slate850, Slate800)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) NeonCyan else Slate700),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.PlayArrow else Icons.Default.MusicNote,
                        contentDescription = "Play ${track.title}",
                        tint = if (isSelected) Slate950 else Slate300,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Audio Format Tag
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Slate950.copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = track.format,
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Slate100,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = Slate400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${track.sampleRate / 1000}kHz",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400,
                    fontSize = 11.sp
                )
                Text(
                    text = TimeUtils.formatDuration(track.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate300,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    AudioTuneTheme {
        HomeContent(
            uiState = HomeUiState(
                recentlyPlayed = listOf(
                    Track(
                        id = "1",
                        title = "Midnight Odyssey",
                        artist = "Aura Synthetics",
                        durationMs = 210000L,
                        format = "FLAC 24-bit"
                    ),
                    Track(
                        id = "2",
                        title = "Quantum Echoes",
                        artist = "SubLow",
                        durationMs = 185000L,
                        format = "WAV 32-bit"
                    )
                )
            ),
            onNavigateToMusic = {},
            onNavigateToEqualizer = {},
            onTrackSelected = {}
        )
    }
}
