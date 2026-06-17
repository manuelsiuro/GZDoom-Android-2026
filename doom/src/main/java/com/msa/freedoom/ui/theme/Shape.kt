package com.msa.freedoom.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Slightly tighter corners than the Material default for a more utilitarian, "panel" feel
 * that suits the dark Doom look. Components that hardcoded RoundedCornerShape values should
 * prefer MaterialTheme.shapes so the corner language stays consistent.
 */
val DoomShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(24.dp),
)
