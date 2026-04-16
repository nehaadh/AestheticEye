package com.example.aestheticeye.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.aestheticeye.ui.theme.AeGold
import com.example.aestheticeye.ui.theme.AeGreen

@Composable
fun LoadingScreen(step: String, modifier: Modifier = Modifier) {
    Box(
        modifier         = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            RoomGridVisualiser()
            StepText(step = step)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ROOM GRID — 3×3 pulsing tiles staggered across the cycle
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RoomGridVisualiser() {
    val tileColors = listOf(
        AeGold.copy(alpha = 0.7f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
        AeGreen.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
        AeGold,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
        AeGreen.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f),
        AeGold.copy(alpha = 0.6f),
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(3) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(3) { col ->
                    val index = row * 3 + col
                    AnimatedTile(
                        color         = tileColors[index],
                        delayFraction = index / 9f
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedTile(color: Color, delayFraction: Float) {
    val transition = rememberInfiniteTransition(label = "tile_$delayFraction")

    // Animate from delayFraction to delayFraction+1 over one cycle.
    // Each tile starts at a different point in the wave = stagger with no keyframes variables.
    val raw by transition.animateFloat(
        initialValue  = delayFraction,
        targetValue   = delayFraction + 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1600, easing = EaseInOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "tile_raw_$delayFraction"
    )

    // Triangle wave: 0->1->0 mapped from cycle position 0..1
    val cycle = raw % 1f
    val pulse = if (cycle < 0.5f) cycle * 2f else (1f - cycle) * 2f

    Box(
        modifier = Modifier
            .size(22.dp)
            .scale(0.7f + pulse * 0.3f)
            .alpha(0.2f + pulse * 0.8f)
            .clip(RoundedCornerShape(5.dp))
            .background(color)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP TEXT with animated gold dots
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepText(step: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text      = step,
            style     = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            textAlign = TextAlign.Center,
            modifier  = Modifier.widthIn(max = 220.dp)
        )
        Spacer(Modifier.height(12.dp))
        AeDots()
    }
}

@Composable
private fun AnimatedDot(
    delayFraction: Float,
    color: androidx.compose.ui.graphics.Color = AeGold
) {
    val transition = rememberInfiniteTransition(label = "dot_$delayFraction")
    val raw by transition.animateFloat(
        initialValue  = delayFraction,
        targetValue   = delayFraction + 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 900, easing = EaseInOut),
            repeatMode = RepeatMode.Restart
        ),
        label = "dot_raw_$delayFraction"
    )
    val cycle = raw % 1f
    val alpha = 0.2f + (if (cycle < 0.5f) cycle * 2f else (1f - cycle) * 2f) * 0.8f

    Box(
        modifier = Modifier
            .size(5.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(color)
    )
}
// ── Public reusable dots indicator — used across the app instead of spinners ──

@Composable
fun AeDots(
    color: androidx.compose.ui.graphics.Color = AeGold,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            AnimatedDot(delayFraction = i / 3f, color = color)
        }
    }
}