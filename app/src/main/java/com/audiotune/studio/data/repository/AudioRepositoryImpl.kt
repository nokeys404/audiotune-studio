package com.audiotune.studio.data.repository

import com.audiotune.studio.data.local.TrackDao
import com.audiotune.studio.data.local.TrackEntity
import com.audiotune.studio.domain.model.AudioPreset
import com.audiotune.studio.domain.model.BandType
import com.audiotune.studio.domain.model.EqualizerBand
import com.audiotune.studio.domain.model.Track
import com.audiotune.studio.domain.repository.AudioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class AudioRepositoryImpl(
    private val trackDao: TrackDao? = null
) : AudioRepository {

    // Seed sample tracks for Studio display
    private val initialSampleTracks = listOf(
        Track(
            id = "demo_1",
            title = "Midnight Odyssey (Acoustic Master)",
            artist = "Aura Synthetics",
            album = "Neon Horizon",
            durationMs = 234000L,
            sampleRate = 96000,
            bitRateKbps = 1411,
            format = "FLAC 24-bit",
            playedAt = System.currentTimeMillis() - 1000 * 60 * 12
        ),
        Track(
            id = "demo_2",
            title = "Quantum Echoes",
            artist = "SubLow Collective",
            album = "Frequency Domain",
            durationMs = 198000L,
            sampleRate = 48000,
            bitRateKbps = 320,
            format = "WAV 32-bit",
            playedAt = System.currentTimeMillis() - 1000 * 60 * 45
        ),
        Track(
            id = "demo_3",
            title = "Solar Flare Groove",
            artist = "Analog Dreamer",
            album = "Tape Warmth Vol. 3",
            durationMs = 312000L,
            sampleRate = 48000,
            bitRateKbps = 320,
            format = "FLAC",
            playedAt = System.currentTimeMillis() - 1000 * 60 * 180
        ),
        Track(
            id = "demo_4",
            title = "Hyperdrive Transit",
            artist = "Veloce Beats",
            album = "Modular Sessions",
            durationMs = 175000L,
            sampleRate = 44100,
            bitRateKbps = 256,
            format = "MP3",
            playedAt = System.currentTimeMillis() - 1000 * 60 * 360
        )
    )

    private val initialPresets = listOf(
        AudioPreset(
            id = "preset_studio_flat",
            name = "Studio Reference Flat",
            description = "Transparent reference curve with 0dB colorization",
            bands = listOf(
                EqualizerBand(0, 32f, 0f, 1.414f, BandType.LOW_SHELF),
                EqualizerBand(1, 64f, 0f, 1.414f, BandType.PEAKING),
                EqualizerBand(2, 125f, 0f, 1.414f, BandType.PEAKING),
                EqualizerBand(3, 250f, 0f, 1.414f, BandType.PEAKING),
                EqualizerBand(4, 500f, 0f, 1.414f, BandType.PEAKING),
                EqualizerBand(5, 1000f, 0f, 1.414f, BandType.PEAKING),
                EqualizerBand(6, 2000f, 0f, 1.414f, BandType.PEAKING),
                EqualizerBand(7, 4000f, 0f, 1.414f, BandType.PEAKING),
                EqualizerBand(8, 8000f, 0f, 1.414f, BandType.PEAKING),
                EqualizerBand(9, 16000f, 0f, 1.414f, BandType.HIGH_SHELF)
            )
        ),
        AudioPreset(
            id = "preset_bass_punch",
            name = "Punchy Sub-Bass",
            description = "Sub-harmonic reinforcement for club & electronic genres",
            bands = listOf(
                EqualizerBand(0, 32f, 5.5f, 1.414f, BandType.LOW_SHELF),
                EqualizerBand(1, 64f, 4.0f, 1.414f, BandType.PEAKING),
                EqualizerBand(2, 125f, 2.5f, 1.414f, BandType.PEAKING),
                EqualizerBand(3, 250f, 0.5f, 1.414f, BandType.PEAKING),
                EqualizerBand(4, 500f, -1.0f, 1.414f, BandType.PEAKING),
                EqualizerBand(5, 1000f, 0f, 1.414f, BandType.PEAKING),
                EqualizerBand(6, 2000f, 1.0f, 1.414f, BandType.PEAKING),
                EqualizerBand(7, 4000f, 2.0f, 1.414f, BandType.PEAKING),
                EqualizerBand(8, 8000f, 3.0f, 1.414f, BandType.PEAKING),
                EqualizerBand(9, 16000f, 3.5f, 1.414f, BandType.HIGH_SHELF)
            )
        ),
        AudioPreset(
            id = "preset_vocal_clarity",
            name = "Vocal Air & Presence",
            description = "Boosts intelligibility and high-frequency sparkle",
            bands = listOf(
                EqualizerBand(0, 32f, -2.0f, 1.414f, BandType.LOW_SHELF),
                EqualizerBand(1, 64f, -1.5f, 1.414f, BandType.PEAKING),
                EqualizerBand(2, 125f, 0f, 1.414f, BandType.PEAKING),
                EqualizerBand(3, 250f, 1.0f, 1.414f, BandType.PEAKING),
                EqualizerBand(4, 500f, 1.5f, 1.414f, BandType.PEAKING),
                EqualizerBand(5, 1000f, 2.5f, 1.414f, BandType.PEAKING),
                EqualizerBand(6, 2000f, 4.0f, 1.414f, BandType.PEAKING),
                EqualizerBand(7, 4000f, 3.5f, 1.414f, BandType.PEAKING),
                EqualizerBand(8, 8000f, 3.0f, 1.414f, BandType.PEAKING),
                EqualizerBand(9, 16000f, 2.0f, 1.414f, BandType.HIGH_SHELF)
            )
        )
    )

    override fun getRecentlyPlayedTracks(): Flow<List<Track>> {
        return trackDao?.getRecentlyPlayed()?.map { entities ->
            if (entities.isEmpty()) {
                initialSampleTracks
            } else {
                entities.map { it.toDomain() }
            }
        } ?: flowOf(initialSampleTracks)
    }

    override fun getLibraryTracks(): Flow<List<Track>> {
        return trackDao?.getAllTracks()?.map { entities ->
            if (entities.isEmpty()) initialSampleTracks else entities.map { it.toDomain() }
        } ?: flowOf(initialSampleTracks)
    }

    override fun getPresets(): Flow<List<AudioPreset>> {
        return flowOf(initialPresets)
    }

    override suspend fun saveTrackToHistory(track: Track) {
        trackDao?.insertTrack(track.toEntity())
    }

    override suspend fun savePreset(preset: AudioPreset) {
        // Prepared for future database preset persistence
    }

    private fun TrackEntity.toDomain() = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        audioUri = audioUri,
        sampleRate = sampleRate,
        bitRateKbps = bitRateKbps,
        format = format,
        playedAt = playedAt
    )

    private fun Track.toEntity() = TrackEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        audioUri = audioUri,
        sampleRate = sampleRate,
        bitRateKbps = bitRateKbps,
        format = format,
        playedAt = playedAt
    )
}
