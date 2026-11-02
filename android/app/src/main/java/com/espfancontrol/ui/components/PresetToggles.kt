package com.espfancontrol.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.espfancontrol.data.model.FanMode

@Composable
fun PresetToggles(
    selectedMode: FanMode,
    enabled: Boolean,
    onModeSelect: (FanMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Preset",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresetItem(
                    label = "Silent",
                    icon = Icons.Outlined.Bedtime,
                    isSelected = selectedMode == FanMode.SILENT,
                    enabled = enabled,
                    onClick = { onModeSelect(FanMode.SILENT) },
                    modifier = Modifier.weight(1f)
                )
                PresetItem(
                    label = "Balanced",
                    icon = Icons.Outlined.Tune,
                    isSelected = selectedMode == FanMode.BALANCED,
                    enabled = enabled,
                    onClick = { onModeSelect(FanMode.BALANCED) },
                    modifier = Modifier.weight(1f)
                )
                PresetItem(
                    label = "Boost",
                    icon = Icons.Outlined.Bolt,
                    isSelected = selectedMode == FanMode.BOOST,
                    enabled = enabled,
                    onClick = { onModeSelect(FanMode.BOOST) },
                    modifier = Modifier.weight(1f)
                )
                PresetItem(
                    label = "Auto",
                    icon = Icons.Outlined.Thermostat,
                    isSelected = selectedMode == FanMode.AUTO,
                    enabled = enabled,
                    onClick = { onModeSelect(FanMode.AUTO) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PresetItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val alpha = if (enabled) 1f else 0.4f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(bg.copy(alpha = alpha))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor.copy(alpha = alpha),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = alpha)
            )
        }
    }
}

