package com.espfancontrol.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),   // Cards & Dialogs
    large = RoundedCornerShape(24.dp),    // Hero containers
    extraLarge = RoundedCornerShape(32.dp) // Full pill indicators
)

