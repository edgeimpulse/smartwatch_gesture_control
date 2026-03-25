package com.example.wizbulb.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text

// ── Typed UI state ─────────────────────────────────────────────────────────────
sealed class GestureUiState {
    object Stopped             : GestureUiState()
    object Filling             : GestureUiState()
    object Listening           : GestureUiState()
    object Motion              : GestureUiState()
    data class Success(val label: String, val accentColor: Color) : GestureUiState()
    data class LowConfidence(val label: String, val pct: Int)     : GestureUiState()
}

val GestureUiState.ringColor: Color
    get() = when (this) {
        GestureUiState.Stopped              -> Color(0xFF1A237E)
        GestureUiState.Filling              -> Color(0xFF1565C0)
        GestureUiState.Listening            -> Color(0xFF1976D2)
        GestureUiState.Motion               -> Color(0xFFFF8F00)
        is GestureUiState.Success           -> accentColor
        is GestureUiState.LowConfidence     -> Color(0xFF546E7A)
    }

val GestureUiState.buttonColor: Color
    get() = when (this) {
        GestureUiState.Stopped              -> Color(0xFF0D1626)
        GestureUiState.Filling,
        GestureUiState.Listening            -> Color(0xFF0D47A1)
        GestureUiState.Motion               -> Color(0xFFE65100)
        is GestureUiState.Success           -> accentColor.copy(alpha = 0.3f)
        is GestureUiState.LowConfidence     -> Color(0xFF263238)
    }

val GestureUiState.statusText: String
    get() = when (this) {
        GestureUiState.Stopped              -> ""
        GestureUiState.Filling              -> "warming up"
        GestureUiState.Listening            -> "listening"
        GestureUiState.Motion               -> "gesture!"
        is GestureUiState.Success           -> when (label) {
            "double_clap" -> "on / off"
            "brighten"    -> "brighter"
            "dim"         -> "dimmer"
            "color_cycle" -> "colour"
            else          -> label
        }
        is GestureUiState.LowConfidence     -> "$label  $pct%"
    }

// ── Composable ─────────────────────────────────────────────────────────────────
@Composable
fun GestureScreen(
    uiState: GestureUiState,
    isRunning: Boolean,
    onToggle: () -> Unit
) {
    val isMotion = uiState is GestureUiState.Motion

    val ringColor   by animateColorAsState(uiState.ringColor,   tween(400), label = "ring")
    val buttonColor by animateColorAsState(uiState.buttonColor, tween(400), label = "btn")

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.25f,
        targetValue   = 1.0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(if (isMotion) 350 else 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val effectiveAlpha = when (uiState) {
        is GestureUiState.Stopped -> 0.35f
        is GestureUiState.Success -> 1.0f
        else                      -> pulseAlpha
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080C10))
    ) {
        // Dual-ring: soft glow + sharp edge
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pad  = 9.dp.toPx()
            val glow = pad - 7.dp.toPx()

            // Outer glow
            drawArc(
                color      = ringColor.copy(alpha = effectiveAlpha * 0.2f),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft    = Offset(glow, glow),
                size       = Size(size.width - glow * 2, size.height - glow * 2),
                style      = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
            )
            // Crisp ring
            drawArc(
                color      = ringColor.copy(alpha = effectiveAlpha),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft    = Offset(pad, pad),
                size       = Size(size.width - pad * 2, size.height - pad * 2),
                style      = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick  = onToggle,
                colors   = ButtonDefaults.buttonColors(backgroundColor = buttonColor),
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    imageVector        = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint               = Color.White,
                    modifier           = Modifier.size(34.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text          = uiState.statusText,
                fontSize      = 11.sp,
                fontWeight    = FontWeight.Light,
                color         = if (uiState.statusText.isEmpty()) Color.Transparent else ringColor,
                textAlign     = TextAlign.Center,
                letterSpacing = 2.sp
            )
        }

    }
}
