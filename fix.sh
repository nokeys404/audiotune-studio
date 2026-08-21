#!/bin/bash
sed -i 's/val freq = 20f \* kotlin.math.exp(fraction \* kotlin.math.ln(20000f\/20f))/val freq = 20f \* kotlin.math.exp((fraction \* kotlin.math.ln(1000.0)).toFloat())/g' app/src/main/java/com/audiotune/studio/presentation/equalizer/EqualizerScreen.kt
sed -i 's/val distance = kotlin.math.abs(kotlin.math.ln(freq \/ band.frequencyHz))/val distance = kotlin.math.abs(kotlin.math.ln((freq \/ band.frequencyHz).toDouble()).toFloat())/g' app/src/main/java/com/audiotune/studio/presentation/equalizer/EqualizerScreen.kt
sed -i 's/val influence = kotlin.math.max(0.0, 1.0 - (distance \* band.q \* 0.5)).toFloat()/val influence = kotlin.math.max(0f, 1f - (distance \* band.q \* 0.5f))/g' app/src/main/java/com/audiotune/studio/presentation/equalizer/EqualizerScreen.kt
