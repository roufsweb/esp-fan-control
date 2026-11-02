package com.espfancontrol.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.espfancontrol.ui.components.PowerToggle
import com.espfancontrol.ui.components.PresetToggles
import com.espfancontrol.ui.components.RpmGauge
import com.espfancontrol.ui.components.SpeedSlider
import com.espfancontrol.ui.components.StatusPill
import com.espfancontrol.ui.components.UsbFlasherCard
import com.espfancontrol.viewmodel.FanViewModel

@Composable
fun DashboardScreen(
    viewModel: FanViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val flashingState by viewModel.flashingState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Fan Control",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "ESP32-C3 Mini",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                StatusPill(
                    connectionState = uiState.connectionState,
                    onClick = { viewModel.toggleConnection() }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Main Tachometer Gauge
            RpmGauge(
                rpm = uiState.liveRpm,
                isPowered = uiState.isPowered
            )

            // Master Power Toggle
            PowerToggle(
                isPowered = uiState.isPowered,
                onToggle = { viewModel.setPower(it) }
            )

            // Continuous PWM Speed Slider
            SpeedSlider(
                speedPercent = uiState.speedPercent,
                enabled = uiState.isPowered,
                onSpeedChange = { viewModel.setSpeed(it) }
            )

            // Mode Presets (Silent, Balanced, Boost, Auto)
            PresetToggles(
                selectedMode = uiState.mode,
                enabled = uiState.isPowered,
                onModeSelect = { viewModel.setMode(it) }
            )

            // Direct In-App USB-OTG Flasher Card
            UsbFlasherCard(
                flashingState = flashingState,
                onFlashClick = { viewModel.flashEsp32() }
            )
        }
    }
}

