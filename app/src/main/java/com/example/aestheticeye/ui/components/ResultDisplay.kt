package com.example.aestheticeye.ui.components

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aestheticeye.data.*
import com.example.aestheticeye.network.buildRetailerUrls
import com.example.aestheticeye.ui.theme.AeGold
import com.example.aestheticeye.ui.theme.AeGreen
import com.example.aestheticeye.ui.theme.AeRed
import com.example.aestheticeye.ui.theme.AeAmber
import java.io.OutputStream
import java.net.URLEncoder

@Composable
fun ResultDisplay(
    result: AestheticResult,
    improvementState: ImprovementState,
    onRequestImprovement: () -> Unit,
    onDismissImprovement: () -> Unit,
    // Called when user picks a scene to continue with (user or AI bitmap)
    onContinueWithScene: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {

        // ── Style Card ────────────────────────────────────────────────────────
        result.roomStyleCard?.let { RoomStyleCardSection(it) }
        if (result.roomStyleCard != null) Spacer(Modifier.height(12.dp))

        // ── Size Badge ────────────────────────────────────────────────────────
        result.sizeWarning?.let { SizeBadge(it) }
        if (result.sizeWarning != null) Spacer(Modifier.height(12.dp))

        // ── Scene Image ───────────────────────────────────────────────────────
        SectionLabel("Generated Scene")
        Spacer(Modifier.height(8.dp))
        SceneImageCard(bitmap = result.generatedBitmap, label = "Your scene", context = context)

        // ── Director Critique ─────────────────────────────────────────────────
        result.directorFeedback?.let { feedback ->
            Spacer(Modifier.height(16.dp))
            DirectorCritiqueCard(feedback = feedback)
        }

        // ── AI Improvement — only shown once design report is fully loaded ─────
        if (result.directorFeedback != null) {
            Spacer(Modifier.height(16.dp))
            AiImprovementSection(
                userResult           = result,
                improvementState     = improvementState,
                onRequestImprovement = onRequestImprovement,
                onDismiss            = onDismissImprovement,
                onContinueWithScene  = onContinueWithScene,
                context              = context
            )
        }

        // ── Mood Board ────────────────────────────────────────────────────────
        result.moodBoard?.let { moodBoard ->
            Spacer(Modifier.height(16.dp))
            MoodBoardSection(moodBoard = moodBoard)
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION LABEL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text  = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 2.sp,
            fontWeight    = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// FEATURE 2: ROOM STYLE CARD — clean horizontal layout
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RoomStyleCardSection(styleCard: RoomStyleCard) {
    var expanded by remember { mutableStateOf(true) }

    Surface(
        color  = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape  = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Style name + description
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "ROOM STYLE",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        styleCard.styleName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                // Color swatches (always visible)
                if (styleCard.dominantColors.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        styleCard.dominantColors.take(3).forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex))
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(12.dp))

                    // Style description
                    if (styleCard.styleDescription.isNotBlank()) {
                        Text(
                            styleCard.styleDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    // Traits as a flowing wrap row
                    if (styleCard.keyTraits.isNotEmpty()) {
                        Text(
                            "KEY TRAITS",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(6.dp))
                        // Use a column of rows to wrap traits naturally
                        val rows = styleCard.keyTraits.chunked(2)
                        rows.forEach { rowTraits ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowTraits.forEach { trait ->
                                    Surface(
                                        color  = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        shape  = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            trait,
                                            style    = MaterialTheme.typography.labelSmall,
                                            color    = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                // Fill empty slot if odd number
                                if (rowTraits.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    // Colour hex labels
                    if (styleCard.dominantColors.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            styleCard.dominantColors.take(3).forEach { hex ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(parseHexColor(hex))
                                    )
                                    Text(
                                        hex.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FEATURE 3: SIZE BADGE — simple inline label
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SizeBadge(warning: SizeWarning) {
    val (bgColor, textColor, icon) = when (warning.label) {
        "TOO LARGE" -> Triple(AeRed.copy(alpha = 0.1f), AeRed, Icons.Default.Warning)
        "TOO SMALL" -> Triple(AeAmber.copy(alpha = 0.1f), AeAmber, Icons.Default.Info)
        else        -> Triple(AeGreen.copy(alpha = 0.1f), AeGreen, Icons.Default.Check)
    }

    Surface(
        color  = bgColor,
        shape  = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
            Text(
                warning.label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
            Text(
                warning.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCENE IMAGE CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SceneImageCard(bitmap: Bitmap?, label: String, context: Context, compact: Boolean = false) {
    if (bitmap != null) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                bitmap             = bitmap.asImageBitmap(),
                contentDescription = label,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
            )
            // Save button overlay
            Surface(
                color    = Color.Black.copy(alpha = 0.55f),
                shape    = RoundedCornerShape(bottomEnd = 16.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .clickable {
                        saveImageToGallery(bitmap, context)
                        Toast.makeText(context, "Saved to Gallery", Toast.LENGTH_SHORT).show()
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Download, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text("Save", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Image, null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(6.dp))
                Text("Image generation failed — try again",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIRECTOR CRITIQUE CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DirectorCritiqueCard(
    feedback: DesignDirectorFeedback,
    title: String = "Design Director",
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val statusColor = when (feedback.status) {
        "APPROVED"       -> AeGreen
        "NEEDS_REVISION" -> AeAmber
        else             -> AeRed
    }

    Surface(
        color  = MaterialTheme.colorScheme.surface,
        shape  = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = accentColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Star, null,
                        tint = accentColor,
                        modifier = Modifier.padding(6.dp).size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(title.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                    Text("Design Report",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(Modifier.weight(1f))
                // Score pill
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("${feedback.score}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = statusColor)
                        Text("/100",
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor.copy(alpha = 0.7f))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
            Spacer(Modifier.height(12.dp))

            // Status badge + style
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        feedback.status.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (feedback.detectedStyle.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            feedback.detectedStyle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(feedback.critiqueSummary,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))

            if (feedback.flaws.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("ISSUES",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                Spacer(Modifier.height(4.dp))
                feedback.flaws.forEach { flaw ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 5.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(AeRed.copy(alpha = 0.7f))
                        )
                        Text(flaw,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }

            if (feedback.suggestionForImprovement.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    color  = accentColor.copy(alpha = 0.07f),
                    shape  = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.Lightbulb, null,
                            tint = accentColor, modifier = Modifier.size(15.dp).padding(top = 1.dp))
                        Text(feedback.suggestionForImprovement,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FEATURE 1: AI COMPARISON SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AiImprovementSection(
    userResult: AestheticResult,
    improvementState: ImprovementState,
    onRequestImprovement: () -> Unit,
    onDismiss: () -> Unit,
    onContinueWithScene: (Bitmap) -> Unit,
    context: Context
) {
    when (improvementState) {

        is ImprovementState.Idle -> {
            OutlinedButton(
                onClick  = onRequestImprovement,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(10.dp),
                border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("See AI's Improvement", style = MaterialTheme.typography.labelLarge)
            }
        }

        is ImprovementState.Loading -> {
            Surface(
                color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape    = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AeDots(color = MaterialTheme.colorScheme.primary)
                    Text(
                        improvementState.step,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        is ImprovementState.Error -> {
            Surface(
                color    = AeRed.copy(alpha = 0.08f),
                shape    = RoundedCornerShape(10.dp),
                border   = BorderStroke(1.dp, AeRed.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Could not generate improvement: ${improvementState.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AeRed,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        is ImprovementState.Success -> {
            val improvement = improvementState.improvement
            Surface(
                color  = MaterialTheme.colorScheme.surface,
                shape  = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.padding(6.dp).size(16.dp))
                            }
                            Column {
                                Text("AI REDESIGN",
                                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                Text("What the AI improved",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }

                    // ── Your scene first ─────────────────────────────────
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Your Scene",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Score: ${userResult.directorFeedback?.score ?: "—"}/100",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    val yourBitmapTop = userResult.generatedBitmap
                        ?: userResult._generatedSceneB64?.let { b64 ->
                            try {
                                val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Exception) { null }
                        }
                    SceneImageCard(bitmap = yourBitmapTop, label = "Your scene", context = context)
                    Spacer(Modifier.height(8.dp))
                    yourBitmapTop?.let { bmp ->
                        OutlinedButton(
                            onClick  = { onContinueWithScene(bmp) },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape    = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Keep Mine", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    Spacer(Modifier.height(12.dp))

                    // ── AI redesign scene ─────────────────────────────────────
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("AI Redesign",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text("Improved version",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    SceneImageCard(bitmap = improvement.improvedBitmap, label = "AI improved scene", context = context)
                    Spacer(Modifier.height(8.dp))
                    improvement.improvedBitmap?.let { bmp ->
                        Button(
                            onClick  = { onContinueWithScene(bmp) },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary,
                                contentColor   = MaterialTheme.colorScheme.onTertiary
                            )
                        ) {
                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Use AI's Version", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(Modifier.height(14.dp))

                    // ── What the AI changed ───────────────────────────────────
                    // Overall note
                    if (improvement.overallNote.isNotBlank()) {
                        Text(
                            improvement.overallNote,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    // Bullet improvements
                    if (improvement.improvements.isNotEmpty()) {
                        Text("CHANGES MADE",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        Spacer(Modifier.height(6.dp))
                        improvement.improvements.forEach { point ->
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 5.dp)
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f))
                                )
                                Text(point,
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                            }
                        }
                    }

                    // Changed products section — added, replaced, or removed
                    if (improvement.changedProducts.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("PRODUCT CHANGES",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        Spacer(Modifier.height(8.dp))
                        improvement.changedProducts.forEach { product ->
                            val (bgColor, tagLabel, tagColor) = when (product.type) {
                                ChangeType.ADDED    -> Triple(
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                                    "ADDED",
                                    MaterialTheme.colorScheme.tertiary
                                )
                                ChangeType.REPLACED -> Triple(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    "REPLACED",
                                    MaterialTheme.colorScheme.primary
                                )
                                ChangeType.REMOVED  -> Triple(
                                    AeRed.copy(alpha = 0.06f),
                                    "REMOVED",
                                    AeRed
                                )
                            }
                            Surface(
                                color    = bgColor,
                                shape    = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        // Type tag
                                        Surface(
                                            color = tagColor.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(tagLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.5.sp
                                                ),
                                                color = tagColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        // For REPLACED: show "Old → New"
                                        if (product.type == ChangeType.REPLACED && product.replacedName.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(product.replacedName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                                    maxLines = 1)
                                                Icon(Icons.Default.ArrowForward, null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                                Text(product.name,
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1)
                                            }
                                        } else {
                                            Text(product.name,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        if (product.reason.isNotBlank()) {
                                            Spacer(Modifier.height(2.dp))
                                            Text(product.reason,
                                                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 15.sp),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                                        }
                                    }
                                    // Shop button for added/replaced, nothing for removed
                                    if (product.type != ChangeType.REMOVED && product.searchQuery.isNotBlank()) {
                                        Spacer(Modifier.width(8.dp))
                                        val shopUrl = "https://www.google.com/search?tbm=shop&q=" +
                                                java.net.URLEncoder.encode(product.searchQuery, "UTF-8")
                                        IconButton(
                                            onClick  = { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(shopUrl))) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Search, "Shop",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }


                }
            }
        }
    }
}


@Composable
fun MoodBoardSection(moodBoard: MoodBoard) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Surface(
        color  = MaterialTheme.colorScheme.surface,
        shape  = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = AeGold.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, null,
                            tint = AeGold,
                            modifier = Modifier.padding(6.dp).size(16.dp))
                    }
                    Column {
                        Text("MOOD BOARD",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        Text("Curated Product Picks",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("${moodBoard.suggestions.size} items",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    moodBoard.suggestions.forEachIndexed { index, suggestion ->
                        MoodBoardItem(
                            suggestion = suggestion,
                            context    = context
                        )
                        if (index < moodBoard.suggestions.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.06f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodBoardItem(suggestion: ProductSuggestion, context: Context) {
    val (ikeaUrl, amazonUrl, wayfairUrl) = buildRetailerUrls(suggestion.searchQuery)

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        // Name + price + category
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(suggestion.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(suggestion.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                }
            }
            if (suggestion.priceRange.isNotBlank()) {
                Text(suggestion.priceRange,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = AeGreen)
            }
        }

        Spacer(Modifier.height(5.dp))
        Text(suggestion.description,
            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

        Spacer(Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(AeGreen.copy(alpha = 0.6f)))
            Text(suggestion.rationale,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }

        Spacer(Modifier.height(10.dp))

        val googleShoppingUrl = "https://www.google.com/search?tbm=shop&q=" + java.net.URLEncoder.encode(suggestion.searchQuery, "UTF-8")
        OutlinedButton(
            onClick  = { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(googleShoppingUrl))) },
            modifier = Modifier.fillMaxWidth().height(34.dp),
            shape    = RoundedCornerShape(8.dp),
            border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            Icon(Icons.Default.Search, null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("Shop on Google", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun RetailerButton(label: String, url: String, context: Context, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick  = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) },
        modifier = modifier.height(32.dp),
        shape    = RoundedCornerShape(6.dp),
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        contentPadding = PaddingValues(horizontal = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────

fun parseHexColor(hex: String): Color = try {
    val cleaned = hex.trim().removePrefix("#")
    val v = java.lang.Long.parseLong(cleaned.padEnd(6, '0').take(6), 16)
    Color(red = ((v shr 16) and 0xFF) / 255f, green = ((v shr 8) and 0xFF) / 255f, blue = (v and 0xFF) / 255f)
} catch (e: Exception) { Color.Gray }

private fun saveImageToGallery(bitmap: Bitmap, context: Context) {
    val filename = "aesthetic_scene_${System.currentTimeMillis()}.png"
    val outputStream: OutputStream?
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val cv = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AestheticEye")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
        outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
    } else {
        val dir  = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)
        val app  = java.io.File(dir, "AestheticEye").apply { mkdirs() }
        outputStream = java.io.File(app, filename).outputStream()
    }
    outputStream?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
}