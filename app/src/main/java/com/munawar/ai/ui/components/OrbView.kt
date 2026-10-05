package com.munawar.ai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


/**
 * The signature glowing orb. Pulses gently at idle, faster while listening,
 * flickers while speaking, and spins subtly while thinking.
 */
@Composable
fun MunawarOrb(stateLabel: String, colorA: Color, colorB: Color, modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "orb")
    val speedMs = when (stateLabel) {
        "LISTENING" -> 900
        "SPEAKING" -> 500
        "THINKING" -> 1400
        else -> 2200
    }
    val scale by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(speedMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier
            .size(180.dp)
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .blur(40.dp)
                .background(
                    Brush.radialGradient(listOf(colorA.copy(alpha = 0.9f), colorB.copy(alpha = 0.0f))),
                )
        )
        Box(
            modifier = Modifier
                .size((140 * scale).dp)
                .background(
                    Brush.radialGradient(listOf(colorA, colorB)),
                    shape = androidx.compose.foundation.shape.CircleShape
                )
        )
    }
}
