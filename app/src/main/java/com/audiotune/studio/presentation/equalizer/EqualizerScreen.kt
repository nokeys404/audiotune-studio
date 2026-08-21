package com.audiotune.studio.presentation.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.audiotune.studio.core.ui.theme.ElectricViolet
import com.audiotune.studio.core.ui.theme.Slate800
import com.audiotune.studio.core.ui.theme.Slate900

@OptIn(ExperimentalMaterial3Api::class)
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
        Text(
            text = "DSP Pipeline",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Tabs
        PrimaryTabRow(
            selectedTabIndex = uiState.currentTab.ordinal,
            containerColor = Color.Transparent
        ) {
            DspTab.values().forEach { tab ->
                Tab(
                    selected = uiState.currentTab == tab,
                    onClick = { viewModel.setTab(tab) },
                    text = { 
                        Text(
                            text = tab.name.replace("_", " "), 
                            style = MaterialTheme.typography.labelSmall
                        ) 
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            when (uiState.currentTab) {
                DspTab.EQ -> EqSection(uiState, viewModel)
                DspTab.COMPRESSOR -> CompressorSection(uiState, viewModel)
                DspTab.LIMITER -> LimiterSection(uiState, viewModel)
                DspTab.NOISE_GATE -> GateSection(uiState, viewModel)
                DspTab.EXPANDER -> ExpanderSection(uiState, viewModel)
            }
        }
    }
}

@Composable
fun EqSection(uiState: DspUiState, viewModel: EqualizerViewModel) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (uiState.eqEnabled) "ON" else "BYPASS",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (uiState.eqEnabled) ElectricViolet else Color.Gray,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Switch(
                    checked = uiState.eqEnabled,
                    onCheckedChange = { viewModel.toggleEqBypass(it) }
                )
            }
            Button(onClick = { viewModel.applyEqFlatPreset() }) {
                Text("Flat")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // EQ Curve Graph
        EqCurveGraph(
            bands = uiState.eqBands,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Slate900)
                .padding(16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.eqBands.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.eqBands.size) { index ->
                    val band = uiState.eqBands[index]
                    val isSelected = index == uiState.selectedBandIndex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ElectricViolet else Slate800)
                            .clickable { viewModel.selectEqBand(index) }
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

            val selectedBand = uiState.eqBands[uiState.selectedBandIndex]
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
                        onCheckedChange = { viewModel.toggleEqBandEnabled(uiState.selectedBandIndex) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            SliderControl(
                label = "Gain",
                value = selectedBand.gainDb,
                valueRange = -15f..15f,
                onValueChange = { viewModel.updateEqBandGain(it) },
                valueFormat = { "${if (it > 0) "+" else ""}${String.format("%.1f", it)} dB" }
            )
            SliderControl(
                label = "Frequency",
                value = selectedBand.frequencyHz,
                valueRange = 20f..20000f,
                onValueChange = { viewModel.updateEqBandFrequency(it) },
                valueFormat = { "${it.toInt()} Hz" }
            )
            SliderControl(
                label = "Q Factor",
                value = selectedBand.q,
                valueRange = 0.1f..10f,
                onValueChange = { viewModel.updateEqBandQ(it) },
                valueFormat = { String.format("%.2f", it) }
            )
        }
    }
}

@Composable
fun CompressorSection(uiState: DspUiState, viewModel: EqualizerViewModel) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Compressor", modifier = Modifier.padding(end = 8.dp))
            Switch(
                checked = uiState.compEnabled,
                onCheckedChange = { viewModel.toggleCompBypass(it) }
            )
        }
        SliderControl("Threshold", uiState.compThresholdDb, -60f..0f, { viewModel.updateCompThreshold(it) }, { "${it.toInt()} dB" })
        SliderControl("Ratio", uiState.compRatio, 1f..20f, { viewModel.updateCompRatio(it) }, { String.format("%.1f:1", it) })
        SliderControl("Attack", uiState.compAttackMs, 0.1f..100f, { viewModel.updateCompAttack(it) }, { "${it.toInt()} ms" })
        SliderControl("Release", uiState.compReleaseMs, 10f..1000f, { viewModel.updateCompRelease(it) }, { "${it.toInt()} ms" })
        SliderControl("Makeup Gain", uiState.compMakeupGainDb, 0f..24f, { viewModel.updateCompMakeupGain(it) }, { "${it.toInt()} dB" })
    }
}

@Composable
fun LimiterSection(uiState: DspUiState, viewModel: EqualizerViewModel) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Limiter", modifier = Modifier.padding(end = 8.dp))
            Switch(
                checked = uiState.limEnabled,
                onCheckedChange = { viewModel.toggleLimBypass(it) }
            )
        }
        SliderControl("Ceiling", uiState.limCeilingDb, -12f..0f, { viewModel.updateLimCeiling(it) }, { String.format("%.1f dB", it) })
        SliderControl("Release", uiState.limReleaseMs, 10f..1000f, { viewModel.updateLimRelease(it) }, { "${it.toInt()} ms" })
    }
}

@Composable
fun GateSection(uiState: DspUiState, viewModel: EqualizerViewModel) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Noise Gate", modifier = Modifier.padding(end = 8.dp))
            Switch(
                checked = uiState.gateEnabled,
                onCheckedChange = { viewModel.toggleGateBypass(it) }
            )
        }
        SliderControl("Threshold", uiState.gateThresholdDb, -100f..0f, { viewModel.updateGateThreshold(it) }, { "${it.toInt()} dB" })
        SliderControl("Attack", uiState.gateAttackMs, 0.1f..50f, { viewModel.updateGateAttack(it) }, { "${it.toInt()} ms" })
        SliderControl("Release", uiState.gateReleaseMs, 10f..1000f, { viewModel.updateGateRelease(it) }, { "${it.toInt()} ms" })
    }
}

@Composable
fun ExpanderSection(uiState: DspUiState, viewModel: EqualizerViewModel) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Expander", modifier = Modifier.padding(end = 8.dp))
            Switch(
                checked = uiState.expEnabled,
                onCheckedChange = { viewModel.toggleExpBypass(it) }
            )
        }
        SliderControl("Threshold", uiState.expThresholdDb, -100f..0f, { viewModel.updateExpThreshold(it) }, { "${it.toInt()} dB" })
        SliderControl("Ratio", uiState.expRatio, 1f..10f, { viewModel.updateExpRatio(it) }, { String.format("%.1f:1", it) })
        SliderControl("Attack", uiState.expAttackMs, 0.1f..50f, { viewModel.updateExpAttack(it) }, { "${it.toInt()} ms" })
        SliderControl("Release", uiState.expReleaseMs, 10f..1000f, { viewModel.updateExpRelease(it) }, { "${it.toInt()} ms" })
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
fun EqCurveGraph(bands: List<com.audiotune.studio.audio.dsp.eq.EqBand>, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        val maxGain = 15f
        
        // Draw grid lines
        drawLine(
            color = Color.DarkGray,
            start = androidx.compose.ui.geometry.Offset(0f, centerY),
            end = androidx.compose.ui.geometry.Offset(width, centerY),
            strokeWidth = 1f
        )
        
        // Draw frequency curve
        if (bands.isNotEmpty()) {
            val path = androidx.compose.ui.graphics.Path()
            path.moveTo(0f, centerY)
            
            for (x in 0..width.toInt() step 5) {
                // Simplified visualization
                var totalGain = 0f
                val fraction = x / width
                val freq = 20f * kotlin.math.exp((fraction * kotlin.math.ln(1000.0)).toFloat())
                
                for (band in bands) {
                    if (band.isEnabled) {
                        val distance = kotlin.math.abs(kotlin.math.ln((freq / band.frequencyHz).toDouble()).toFloat())
                        val influence = kotlin.math.max(0f, 1f - (distance * band.q * 0.5f))
                        totalGain += band.gainDb * influence
                    }
                }
                
                val y = centerY - (totalGain / maxGain) * (height / 2f)
                val clampedY = y.coerceIn(0f, height)
                
                if (x == 0) {
                    path.moveTo(x.toFloat(), clampedY)
                } else {
                    path.lineTo(x.toFloat(), clampedY)
                }
            }
            
            drawPath(
                path = path,
                color = ElectricViolet,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 4f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }
    }
}
