package com.audiotune.studio

import com.audiotune.studio.core.util.AudioFormatUtils
import com.audiotune.studio.core.util.TimeUtils
import com.audiotune.studio.domain.model.BandType
import com.audiotune.studio.domain.model.EqualizerBand
import com.audiotune.studio.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AudioTuneUnitTest {

    @Test
    fun timeUtils_formatDuration_formatsCorrectly() {
        val durationMs = 234000L // 3 min 54 sec
        val formatted = TimeUtils.formatDuration(durationMs)
        assertEquals("03:54", formatted)
    }

    @Test
    fun audioFormatUtils_formatFrequency_formatsCorrectly() {
        assertEquals("64Hz", AudioFormatUtils.formatFrequency(64f))
        assertEquals("1kHz", AudioFormatUtils.formatFrequency(1000f))
        assertEquals("2.5kHz", AudioFormatUtils.formatFrequency(2500f))
        assertEquals("16kHz", AudioFormatUtils.formatFrequency(16000f))
    }

    @Test
    fun audioFormatUtils_formatGain_formatsCorrectly() {
        assertEquals("+4.5dB", AudioFormatUtils.formatGain(4.5f))
        assertEquals("-3.0dB", AudioFormatUtils.formatGain(-3.0f))
        assertEquals("0.0dB", AudioFormatUtils.formatGain(0.0f))
    }

    @Test
    fun equalizerBand_initialization_succeeds() {
        val band = EqualizerBand(
            index = 0,
            frequencyHz = 1000f,
            gainDb = 3.5f,
            qFactor = 1.414f,
            type = BandType.PEAKING
        )
        assertEquals(0, band.index)
        assertEquals(1000f, band.frequencyHz)
        assertEquals(3.5f, band.gainDb)
    }

    @Test
    fun track_model_instantiation() {
        val track = Track(
            id = "test_1",
            title = "Test Audio Track",
            artist = "Studio Producer",
            durationMs = 180000L
        )
        assertNotNull(track)
        assertEquals("Test Audio Track", track.title)
    }
}
