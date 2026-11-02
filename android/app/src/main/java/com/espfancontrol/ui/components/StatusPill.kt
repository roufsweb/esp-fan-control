package com.espfancontrol.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.espfancontrol.data.model.BleConnectionState

@Composable
fun StatusPill(
    connectionState: BleConnectionState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pillBg by animateColorAsState(
        targetValue = when (connectionState) {
            BleConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer
            BleConnectionState.CONNECTING, BleConnectionState.SCANNING -> MaterialTheme.colorScheme.surfaceVariant
            BleConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        },
        animationSpec = tween(300),
        label = "pillBg"
    )

    val contentColor = when (connectionState) {
        BleConnectionState.CONNECTED -> MaterialTheme.colorScheme.onPrimaryContainer
        BleConnectionState.CONNECTING, BleConnectionState.SCANNING -> MaterialTheme.colorScheme.onSurfaceVariant
        BleConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.outline
    }

    val icon = when (connectionState) {
        BleConnectionState.CONNECTED -> Icons.Outlined.BluetoothConnected
        BleConnectionState.CONNECTING, BleConnectionState.SCANNING -> Icons.Outlined.Sync
        BleConnectionState.DISCONNECTED -> Icons.Outlined.BluetoothDisabled
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(pillBg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = connectionState.label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor
            )
        }
    }
}

