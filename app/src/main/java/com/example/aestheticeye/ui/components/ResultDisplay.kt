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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aestheticeye.data.*
import com.example.aestheticeye.ui.theme.Green500
import com.example.aestheticeye.ui.theme.Red500
import com.example.aestheticeye.ui.theme.Yellow500
import java.io.OutputStream
import java.net.URLEncoder

@Composable
fun ResultDisplay(
    result: AestheticResult,
    comparisonState: ComparisonState,
    onRequestComparison: () -> Unit,
    onDismissComparison: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {

        // ── Feature 2: Room Style Card ────────────────────────────────────────
        result.roomStyleCard?.let { RoomStyleCardSection(styleCard = it) }
        if (result.roomStyleCard != null) Spacer(modifier = Modifier.height(16.dp))

        // ── Feature 3: Size Warning ───────────────────────────────────────────
        result.sizeWarning?.let { warning ->
            if (warning.hasIssue && warning.severity != "NONE") {
                SizeWarningBanner(warning = warning)
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // ── Your Generated Scene ──────────────────────────────────────────────
        Text(
            text  = "Generated Scene",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(12.dp))
        SceneImageCard(bitmap = result.generatedBitmap, label = "Your Scene", context = context)

        // ── Director Critique ─────────────────────────────────────────────────
        result.directorFeedback?.let { feedback ->
            Spacer(modifier = Modifier.height(20.dp))
            DirectorCritiqueCard(feedback = feedback)
        }

        // ── Feature 1: On-Demand AI Comparison ───────────────────────────────
        Spacer(modifier = Modifier.height(16.dp))
        AiComparisonSection(
            userResult          = result,
            comparisonState     = comparisonState,
            onRequestComparison = onRequestComparison,
            onDismiss           = onDismissComparison,
            context             = context
        )

        // ── Feature 4: Mood Board ─────────────────────────────────────────────
        result.moodBoard?.let { moodBoard ->
            Spacer(modifier = Modifier.height(20.dp))
            MoodBoardSection(moodBoard = moodBoard)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FEATURE 1: ON-DEMAND AI COMPARISON
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AiComparisonSection(
    userResult: AestheticResult,
    comparisonState: ComparisonState,
    onRequestComparison: () -> Unit,
    onDismiss: () -> Unit,
    context: Context
) {
    when (comparisonState) {

        is ComparisonState.Idle -> {
            // Show the prompt button
            OutlinedButton(
                onClick  = onRequestComparison,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compare with AI's Placement")
            }
        }

        is ComparisonState.Loading -> {
            Surface(
                color  = MaterialTheme.colorScheme.surfaceVariant,
                shape  = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        comparisonState.step,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        is ComparisonState.Error -> {
            Surface(
                color    = MaterialTheme.colorScheme.errorContainer,
                shape    = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Comparison failed: ${comparisonState.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onDismiss) { Text("Dismiss") }
                }
            }
        }

        is ComparisonState.Success -> {
            val comparison = comparisonState.comparison
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "You vs. AI",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Side-by-side images
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Your placement
                        Column(modifier = Modifier.weight(1f)) {
                            ComparisonLabel(
                                label  = "Your Placement",
                                score  = userResult.directorFeedback?.score,
                                isUser = true
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            CompactSceneImage(bitmap = userResult.generatedBitmap, label = "Your scene", context = context)
                        }

                        // AI placement
                        Column(modifier = Modifier.weight(1f)) {
                            ComparisonLabel(
                                label  = "AI's Placement",
                                score  = comparison.aiFeedback?.score,
                                isUser = false
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            CompactSceneImage(bitmap = comparison.aiBitmap, label = "AI scene", context = context)
                        }
                    }

                    // AI rationale
                    if (comparison.aiRationale.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "🤖 AI's Reasoning",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    comparison.aiRationale,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    // AI director report
                    comparison.aiFeedback?.let { aiFeedback ->
                        Spacer(modifier = Modifier.height(12.dp))
                        DirectorCritiqueCard(
                            feedback    = aiFeedback,
                            title       = "AI Placement — Director's Report",
                            accentColor = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonLabel(label: String, score: Int?, isUser: Boolean) {
    val bg = if (isUser) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.tertiaryContainer
    val fg = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onTertiaryContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = fg)
        if (score != null) {
            Text("$score", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = fg)
        }
    }
}

@Composable
private fun CompactSceneImage(bitmap: Bitmap?, label: String, context: Context) {
    if (bitmap != null) {
        Column {
            Image(
                bitmap             = bitmap.asImageBitmap(),
                contentDescription = label,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            TextButton(
                onClick  = {
                    saveImageToGallery(bitmap, context)
                    Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("⬇ Save", style = MaterialTheme.typography.labelSmall)
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("Unavailable", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FEATURE 2: ROOM STYLE CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RoomStyleCardSection(styleCard: RoomStyleCard) {
    var expanded by remember { mutableStateOf(true) }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Home, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Room Style Detected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        Text(styleCard.styleName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    if (styleCard.dominantColors.isNotEmpty()) {
                        Text("Dominant Colors", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            styleCard.dominantColors.forEach { hex ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(parseHexColor(hex))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(hex, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (styleCard.keyTraits.isNotEmpty()) {
                        Text("Key Traits", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            styleCard.keyTraits.take(4).forEach { trait ->
                                Surface(color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)) {
                                    Text(trait, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (styleCard.styleDescription.isNotBlank()) {
                        Text(styleCard.styleDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FEATURE 3: SIZE WARNING
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SizeWarningBanner(warning: SizeWarning) {
    val isMajor   = warning.severity == "MAJOR"
    val bgColor   = if (isMajor) Red500.copy(alpha = 0.1f) else Yellow500.copy(alpha = 0.1f)
    val iconColor = if (isMajor) Red500 else Yellow500

    Surface(color = bgColor, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(if (isMajor) Icons.Default.Warning else Icons.Default.Info,
                contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(if (isMajor) "⚠ Size Mismatch Detected" else "ℹ Proportions Note",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = iconColor)
                Spacer(modifier = Modifier.height(2.dp))
                Text(warning.description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface)
                if (warning.recommendation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("💡 ${warning.recommendation}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FEATURE 4: MOOD BOARD with Google Shopping links
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MoodBoardSection(moodBoard: MoodBoard) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Mood Board", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Tap any item to shop on Google",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    moodBoard.suggestions.forEachIndexed { index, suggestion ->
                        MoodBoardItem(
                            suggestion = suggestion,
                            onShopClick = {
                                val query   = URLEncoder.encode(suggestion.searchQuery, "UTF-8")
                                val shopUrl = "https://www.google.com/search?tbm=shop&q=$query"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(shopUrl)))
                            }
                        )
                        if (index < moodBoard.suggestions.lastIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodBoardItem(
    suggestion: ProductSuggestion,
    onShopClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Surface(
            color  = MaterialTheme.colorScheme.primaryContainer,
            shape  = RoundedCornerShape(8.dp),
            modifier = Modifier
                .width(52.dp)
                .defaultMinSize(minHeight = 44.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(4.dp)
            ) {
                Text(
                    suggestion.category.take(8).uppercase(),
                    style  = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize   = 7.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color  = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 2
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(suggestion.name,
                    style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f))
                if (suggestion.priceRange.isNotBlank()) {
                    Text(suggestion.priceRange,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Green500,
                        modifier = Modifier.padding(start = 8.dp))
                }
            }

            Text(suggestion.category,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary)

            Spacer(modifier = Modifier.height(2.dp))

            Text(suggestion.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("✓ ${suggestion.rationale}",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Google Shopping button
            OutlinedButton(
                onClick  = onShopClick,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Shop on Google", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED: SCENE IMAGE CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SceneImageCard(bitmap: Bitmap?, label: String, context: Context) {
    if (bitmap != null) {
        Column {
            Image(
                bitmap             = bitmap.asImageBitmap(),
                contentDescription = label,
                contentScale       = ContentScale.Fit,
                modifier           = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick  = {
                    saveImageToGallery(bitmap, context)
                    Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.align(Alignment.End)
            ) { Text("⬇ Save to Gallery") }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth().height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("Image generation unavailable",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED: DIRECTOR CRITIQUE CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DirectorCritiqueCard(
    feedback: DesignDirectorFeedback,
    title: String = "Design Director's Report",
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val statusColor = when (feedback.status) {
        "APPROVED"       -> Green500
        "NEEDS_REVISION" -> Yellow500
        else             -> Red500
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = null,
                    tint = accentColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Score", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("${feedback.score}/100",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = statusColor)
                }
                Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(feedback.status.replace("_", " "),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            LabelValue("Detected Style", feedback.detectedStyle)
            Spacer(modifier = Modifier.height(8.dp))
            LabelValue("Summary", feedback.critiqueSummary)

            if (feedback.flaws.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Flaws", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                feedback.flaws.forEach { flaw ->
                    Row(modifier = Modifier.padding(top = 2.dp)) {
                        Text("• ", color = Red500)
                        Text(flaw, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (feedback.suggestionForImprovement.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(color = accentColor.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("💡 Suggestion",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(feedback.suggestionForImprovement,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LabelValue(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface)
    }
}



private fun parseHexColor(hex: String): Color = try {
    val cleaned   = hex.trim().removePrefix("#")
    val colorLong = java.lang.Long.parseLong(cleaned, 16)
    Color(
        red   = ((colorLong shr 16) and 0xFF) / 255f,
        green = ((colorLong shr 8)  and 0xFF) / 255f,
        blue  = (colorLong and 0xFF) / 255f
    )
} catch (e: Exception) { Color.Gray }

private fun saveImageToGallery(bitmap: Bitmap, context: Context) {
    val filename = "aesthetic_scene_${System.currentTimeMillis()}.png"
    val outputStream: OutputStream?
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AestheticEye")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        outputStream = uri?.let { context.contentResolver.openOutputStream(it) }
    } else {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val appDir = java.io.File(picturesDir, "AestheticEye").apply { mkdirs() }
        outputStream = java.io.File(appDir, filename).outputStream()
    }
    outputStream?.use { stream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) }
}