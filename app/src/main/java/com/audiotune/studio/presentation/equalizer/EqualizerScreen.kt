package com.audiotune.studio.presentation.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.audiotune.studio.audio.dsp.eq.EqBand
import com.audiotune.studio.core.ui.theme.ElectricViolet
import com.audiotune.studio.core.ui.theme.Slate800
import com.audiotune.studio.core.ui.theme.Slate900
import kotlin.math.exp
import kotlin.math.log10

@Composable
fun EqualizerScreen(
    modifier: Modifier = Modifier,
    viewModel: EqualizerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Parametric EQ",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (uiState.isEnabled) "ON" else "BYPASS",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (uiState.isEnabled) ElectricViolet else Color.Gray,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(
                    checked = uiState.isEnabled,
                    onCheckedChange = { viewModel.toggleBypass(it) }
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Button(onClick = { viewModel.applyFlatPreset() }) {
                    Text("Flat")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // EQ Curve Graph
        EqCurveGraph(
            bands = uiState.bands,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Slate900)
                .padding(16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.bands.isNotEmpty()) {
            // Band Selection
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.bands.size) { index ->
                    val band = uiState.bands[index]
                    val isSelected = index == uiState.selectedBandIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ElectricViolet else Slate800)
                            .clickable { viewModel.selectBand(index) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        val freqText = if (band.frequencyHz >= 1000f) {
                            "${(band.frequencyHz / 1000f).toInt()}k"
                        } else {
                            "${band.frequencyHz.toInt()}"
                        }
                        Text(
                            text = freqText,
                            color = if (isSelected) Color.White else Color.LightGray,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Controls for selected band
            val selectedBand = uiState.bands[uiState.selectedBandIndex]
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Band ${uiState.selectedBandIndex + 1}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Enabled", modifier = Modifier.padding(end = 8.dp))
                    Switch(
                        checked = selectedBand.isEnabled,
                        onCheckedChange = { viewModel.toggleBandEnabled(uiState.selectedBandIndex) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gain Slider
            SliderControl(
                label = "Gain",
                value = selectedBand.gainDb,
                valueRange = -15f..15f,
                onValueChange = { viewModel.updateBandGain(it) },
                valueFormat = { "${if (it > 0) "+" else ""}${String.format("%.1f", it)} dB" }
            )

            // Frequency Slider
            SliderControl(
                label = "Frequency",
                value = selectedBand.frequencyHz,
                valueRange = 20f..20000f,
                onValueChange = { viewModel.updateBandFrequency(it) },
                valueFormat = { "${it.toInt()} Hz" }
            )

            // Q Slider
            SliderControl(
                label = "Q Factor",
                value = selectedBand.q,
                valueRange = 0.1f..10f,
                onValueChange = { viewModel.updateBandQ(it) },
                valueFormat = { String.format("%.2f", it) }
            )
        }
    }
}

@Composable
fun SliderControl(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueFormat: (Float) -> String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.LightGray)
            Text(text = valueFormat(value), fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun EqCurveGraph(bands: List<EqBand>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        val minFreq = 20f
        val maxFreq = 20000f
        val minDb = -15f
        val maxDb = 15f
        
        val logMin = log10(minFreq)
        val logMax = log10(maxFreq)
        
        // Draw grid lines
        val gridLines = listOf(-10f, -5f, 0f, 5f, 10f)
        gridLines.forEach { db ->
            val y = h - ((db - minDb) / (maxDb - minDb)) * h
            drawLine(
                color = Color.DarkGray,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = if (db == 0f) 2f else 1f
            )
        }
        
        // Draw curve
        if (bands.isEmpty()) return@Canvas
        
        val path = Path()
        val numPoints = 100
        
        for (i in 0..numPoints) {
            val xFraction = i.toFloat() / numPoints
            val x = xFraction * w
            
            // Calculate frequency for this x position
            val currentLogF = logMin + xFraction * (logMax - logMin)
            val f = Math.pow(10.0, currentLogF.toDouble()).toFloat()
            
            // Calculate total gain at this frequency
            var totalGain = 0f
            for (band in bands) {
                if (!band.isEnabled || band.gainDb == 0f) continue
                
                val fRatio = f / band.frequencyHz
                val logRatio = log10(fRatio.coerceAtLeast(0.001f))
                val width = 1f / (band.q * 2f)
                val influence = exp(-(logRatio * logRatio) / width)
                totalGain += band.gainDb * influence
            }
            
            val clampedGain = totalGain.coerceIn(minDb, maxDb)
            val yFraction = (clampedGain - minDb) / (maxDb - minDb)
            val y = h - (yFraction * h)
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = ElectricViolet,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )
        
        // Draw band handles
        for (band in bands) {
            if (!band.isEnabled) continue
            val bandLogF = log10(band.frequencyHz)
            val bandX = ((bandLogF - logMin) / (logMax - logMin)) * w
            val bandY = h - (((band.gainDb - minDb) / (maxDb - minDb)) * h)
            
            if (bandX in 0f..w) {
                drawCircle(
                    color = Color.White,
                    radius = 6f,
                    center = Offset(bandX, bandY)
                )
            }
        }
    }
}

