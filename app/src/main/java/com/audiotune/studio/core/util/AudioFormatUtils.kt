package com.audiotune.studio.core.util

object AudioFormatUtils {
    fun formatFrequency(frequencyHz: Float): String {
        return if (frequencyHz >= 1000f) {
            String.format("%.1fkHz", frequencyHz / 1000f).replace(".0kHz", "kHz")
        } else {
            "${frequencyHz.toInt()}Hz"
        }
    }

    fun formatGain(gainDb: Float): String {
        return if (gainDb > 0) "+%.1fdB".format(gainDb) else "%.1fdB".format(gainDb)
    }
}
