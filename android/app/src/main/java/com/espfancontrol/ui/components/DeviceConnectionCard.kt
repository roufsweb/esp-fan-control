package com.espfancontrol.ui.components

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.espfancontrol.data.model.BleConnectionState
import com.espfancontrol.data.model.DiscoveredBleDevice

@Composable
fun DeviceConnectionCard(
    connectionState: BleConnectionState,
    discoveredDevices: List<DiscoveredBleDevice>,
    onScanClick: () -> Unit,
    onDeviceConnectClick: (android.bluetooth.BluetoothDevice) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // When connected, this full card hides (the top StatusPill represents connection)
    if (connectionState == BleConnectionState.CONNECTED) {
        return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header: State icon + Title + Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val icon = when (connectionState) {
                        BleConnectionState.SCANNING -> Icons.Outlined.BluetoothSearching
                        BleConnectionState.CONNECTING -> Icons.Outlined.BluetoothConnected
                        BleConnectionState.BLUETOOTH_OFF -> Icons.Outlined.BluetoothDisabled
                        else -> Icons.Outlined.Bluetooth
                    }
                    val iconTint = when (connectionState) {
                        BleConnectionState.BLUETOOTH_OFF -> MaterialTheme.colorScheme.error
                        BleConnectionState.SCANNING, BleConnectionState.CONNECTING -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )

                    Column {
                        val title = when (connectionState) {
                            BleConnectionState.SCANNING -> "Searching for Fan..."
                            BleConnectionState.CONNECTING -> "Connecting..."
                            BleConnectionState.BLUETOOTH_OFF -> "Bluetooth Disabled"
                            BleConnectionState.LOCATION_OFF -> "Location Disabled"
                            BleConnectionState.NO_DEVICE_FOUND -> "No Fan Found"
                            else -> "Fan Disconnected"
                        }

                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        val subtitle = when (connectionState) {
                            BleConnectionState.SCANNING -> "Looking for 'ESP32-C3 Fan'"
                            BleConnectionState.CONNECTING -> "Establishing BLE GATT session"
                            BleConnectionState.BLUETOOTH_OFF -> "Turn on Bluetooth to connect"
                            BleConnectionState.LOCATION_OFF -> "Required for BLE on Android 10/11"
                            BleConnectionState.NO_DEVICE_FOUND -> "Ensure ESP32 is powered on & reset"
                            else -> "Auto-connects when device is in range"
                        }

                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action Pill Button
                when (connectionState) {
                    BleConnectionState.SCANNING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    BleConnectionState.BLUETOOTH_OFF -> {
                        Button(
                            onClick = {
                                try {
                                    context.startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                                } catch (_: Exception) {
                                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                }
                            },
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                            shape = CircleShape
                        ) {
                            Text("Turn On", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    BleConnectionState.LOCATION_OFF -> {
                        Button(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            },
                            contentPadding = ButtonDefaults.TextButtonContentPadding,
                            shape = CircleShape
                        ) {
                            Text("Settings", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    else -> {
                        OutlinedButton(
                            onClick = onScanClick,
                            shape = CircleShape,
                            contentPadding = ButtonDefaults.TextButtonContentPadding
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            // Troubleshooting Tip Box (especially useful after flashing)
            if (connectionState == BleConnectionState.NO_DEVICE_FOUND || connectionState == BleConnectionState.DISCONNECTED) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Setup tip:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "1. After flashing, press the RST button on the ESP32.\n2. Verify the on-board LED pulses (indicates active BLE).\n3. Keep phone within 5 meters of the fan.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Discovered Nearby Devices List
            if (discoveredDevices.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Discovered Devices (${discoveredDevices.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    discoveredDevices.take(5).forEach { dev ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (dev.isTarget) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { onDeviceConnectClick(dev.device) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (dev.isTarget) Icons.Outlined.Check else Icons.Outlined.Sensors,
                                    contentDescription = null,
                                    tint = if (dev.isTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = dev.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${dev.address} • ${dev.rssi} dBm",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Button(
                                onClick = { onDeviceConnectClick(dev.device) },
                                shape = CircleShape,
                                contentPadding = ButtonDefaults.TextButtonContentPadding,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (dev.isTarget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (dev.isTarget) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text("Connect", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

