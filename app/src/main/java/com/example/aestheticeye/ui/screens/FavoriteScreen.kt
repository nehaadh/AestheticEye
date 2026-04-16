package com.example.aestheticeye.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import com.example.aestheticeye.data.FavoriteScene
import com.example.aestheticeye.data.SceneStage
import kotlinx.serialization.json.Json
import com.example.aestheticeye.ui.components.parseHexColor
import com.example.aestheticeye.ui.theme.AeGold
import com.example.aestheticeye.ui.theme.AeGreen
import com.example.aestheticeye.ui.theme.AeAmber
import com.example.aestheticeye.ui.theme.AeRed
import com.example.aestheticeye.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favorites: List<FavoriteScene>,
    selectedFavorite: FavoriteScene?,
    loadBitmap: (String) -> Unit,
    onOpenDetail: (FavoriteScene) -> Unit,
    onCloseDetail: () -> Unit,
    onDelete: (FavoriteScene) -> Unit,
    onClose: () -> Unit,
    viewModel: MainViewModel
) {
    if (selectedFavorite != null) {
        FavoriteDetailScreen(
            scene    = selectedFavorite,
            viewModel = viewModel,
            onDelete  = { onDelete(selectedFavorite) },
            onBack    = onCloseDetail
        )
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("My Scenes",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("${favorites.size} saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        if (favorites.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.FavoriteBorder, null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text("No saved scenes yet",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Spacer(Modifier.height(4.dp))
                    Text("Generate a scene and save it to your collection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favorites, key = { it.id }) { scene ->
                    FavoriteCard(
                        scene     = scene,
                        viewModel = viewModel,
                        onClick   = { onOpenDetail(scene) },
                        onDelete  = { onDelete(scene) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COLLECTION CARD — thumbnail + key info only
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FavoriteCard(
    scene: FavoriteScene,
    viewModel: MainViewModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val bitmap = remember(scene.imagePath) { viewModel.loadBitmapFromPath(scene.imagePath) }
    val dateStr = remember(scene.createdAt) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(scene.createdAt))
    }
    val statusColor = statusColor(scene.status)
    val dominantColors = remember(scene.dominantColors) {
        scene.dominantColors.split(",").filter { it.isNotBlank() }.take(3)
    }

    Surface(
        color    = MaterialTheme.colorScheme.surface,
        shape    = RoundedCornerShape(16.dp),
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap             = bitmap.asImageBitmap(),
                        contentDescription = scene.projectName,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize()
                    )
                    // Gradient scrim
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.6f to Color.Transparent,
                                    1f to Color.Black.copy(alpha = 0.45f)
                                )
                            )
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Image, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.size(32.dp))
                    }
                }

                // Score pill — top right
                Surface(
                    color  = Color.Black.copy(alpha = 0.55f),
                    shape  = RoundedCornerShape(bottomStart = 10.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text("${scene.score}/100",
                        style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color    = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }

                // Name + style — bottom left overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) {
                    Text(scene.projectName,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White)
                    if (scene.detectedStyle.isNotBlank()) {
                        Text(scene.detectedStyle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f))
                    }
                }
            }

            // Bottom row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Color swatches
                    dominantColors.forEach { hex ->
                        Box(
                            modifier = Modifier.size(14.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                        )
                    }
                    if (dominantColors.isNotEmpty()) Spacer(Modifier.width(2.dp))
                    // Product count pill
                    if (scene.productsPlaced > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(5.dp)
                        ) {
                            Text("${scene.productsPlaced} item${if (scene.productsPlaced != 1) "s" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
                    IconButton(
                        onClick  = { showDeleteDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, null,
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                            modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text("Delete Scene") },
            text    = { Text("Remove \"${scene.projectName}\" from your collection?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DETAIL SCREEN — clean, focused, not overwhelming
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteDetailScreen(
    scene: FavoriteScene,
    viewModel: MainViewModel,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val bitmap = remember(scene.imagePath) { viewModel.loadBitmapFromPath(scene.imagePath) }
    val dateStr = remember(scene.createdAt) {
        SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date(scene.createdAt))
    }
    val statusColor = statusColor(scene.status)

    // Parse stored data safely — flaws use "|" separator to avoid splitting mid-sentence
    val flaws = remember(scene.flaws) {
        scene.flaws.split("|").map { it.trim() }.filter { it.isNotBlank() }
    }
    val productLabels = remember(scene.productLabels) {
        scene.productLabels.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    val dominantColors = remember(scene.dominantColors) {
        scene.dominantColors.split(",").map { it.trim() }.filter { it.isNotBlank() }.take(3)
    }
    val keyTraits = remember(scene.keyTraits) {
        scene.keyTraits.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    val stages = remember(scene.stagesJson) {
        if (scene.stagesJson.isBlank()) emptyList()
        else try {
            Json { ignoreUnknownKeys = true }.decodeFromString<List<SceneStage>>(scene.stagesJson)
        } catch (_: Exception) { emptyList() }
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(scene.projectName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(dateStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Full scene image — no padding, edge to edge ──────────────────
            if (bitmap != null) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        bitmap             = bitmap.asImageBitmap(),
                        contentDescription = scene.projectName,
                        contentScale       = ContentScale.FillWidth,
                        modifier           = Modifier.fillMaxWidth()
                    )
                    // Score + status overlay bottom-left
                    Box(
                        modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                )
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("${scene.score}/100",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = statusColor)
                            Surface(
                                color = statusColor.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(scene.status.replace("_", " "),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ── Style + palette ──────────────────────────────────────────
                DetailCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(scene.detectedStyle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            if (scene.styleDescription.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(scene.styleDescription,
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                        if (dominantColors.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.padding(start = 12.dp)
                            ) {
                                dominantColors.forEach { hex ->
                                    Box(
                                        modifier = Modifier.size(22.dp)
                                            .clip(CircleShape)
                                            .background(parseHexColor(hex))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                                    )
                                }
                            }
                        }
                    }
                    if (keyTraits.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            keyTraits.forEach { trait ->
                                Surface(
                                    color  = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    shape  = RoundedCornerShape(5.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                ) {
                                    Text(trait,
                                        style    = MaterialTheme.typography.labelSmall,
                                        color    = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }

                // ── Products added ───────────────────────────────────────────
                if (productLabels.isNotEmpty()) {
                    DetailCard {
                        FsLabel("PRODUCTS ADDED")
                        Spacer(Modifier.height(8.dp))
                        productLabels.forEachIndexed { i, label ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(vertical = 3.dp)
                            ) {
                                Surface(
                                    color = AeGold.copy(alpha = 0.15f),
                                    shape = CircleShape,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${i + 1}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = AeGold)
                                    }
                                }
                                Text(label,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                // ── Design Director's critique ───────────────────────────────
                DetailCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FsLabel("DESIGN DIRECTOR")
                        Surface(
                            color  = statusColor.copy(alpha = 0.12f),
                            shape  = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
                        ) {
                            Text(scene.status.replace("_", " "),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // Large score
                    Text("${scene.score}",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 48.sp
                        ),
                        color = statusColor)
                    Text("out of 100",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))

                    if (scene.critiqueSummary.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(scene.critiqueSummary,
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                    }

                    // Issues — only if present, one per line, clean dots
                    if (flaws.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        FsLabel("NOTES")
                        Spacer(Modifier.height(5.dp))
                        flaws.forEach { flaw ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(Modifier.padding(top = 5.dp).size(4.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)))
                                Text(flaw,
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }

                    // Suggestion
                    if (scene.suggestion.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            color  = AeGold.copy(alpha = 0.08f),
                            shape  = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AeGold.copy(alpha = 0.18f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Default.Lightbulb, null,
                                    tint = AeGold,
                                    modifier = Modifier.size(14.dp).padding(top = 1.dp))
                                Text(scene.suggestion,
                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                            }
                        }
                    }
                }

                // ── Scene history ───────────────────────────────────────────
                if (stages.isNotEmpty()) {
                    stages.forEach { stage ->
                        DetailCard {
                            // Stage header
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
                                        color = AeGold.copy(alpha = 0.15f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("${stage.stageNumber}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = AeGold)
                                        }
                                    }
                                    Text(stage.productLabel,
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                }
                                if (stage.aiScenePath.isNotBlank()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(5.dp)
                                    ) {
                                        Text("AI compared",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            // Always show user scene
                            if (stage.userScenePath.isNotBlank()) {
                                val userBitmap = remember(stage.userScenePath) {
                                    try { BitmapFactory.decodeFile(stage.userScenePath) } catch (_: Exception) { null }
                                }
                                if (userBitmap != null) {
                                    StageSceneBlock(
                                        label     = "YOUR SCENE",
                                        bitmap    = userBitmap,
                                        chosen    = stage.userChose,
                                        chosenLabel = "Used for next stage"
                                    )
                                }
                            }

                            // Show AI scene if improvement was run
                            if (stage.aiScenePath.isNotBlank()) {
                                val aiBitmap = remember(stage.aiScenePath) {
                                    try { BitmapFactory.decodeFile(stage.aiScenePath) } catch (_: Exception) { null }
                                }
                                if (aiBitmap != null) {
                                    Spacer(Modifier.height(10.dp))
                                    StageSceneBlock(
                                        label     = "AI REDESIGN",
                                        bitmap    = aiBitmap,
                                        chosen    = !stage.userChose,
                                        chosenLabel = "Used for next stage"
                                    )
                                    // AI improvement notes
                                    if (stage.aiImprovements.isNotEmpty()) {
                                        Spacer(Modifier.height(6.dp))
                                        stage.aiImprovements.forEach { point ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                                            ) {
                                                Box(Modifier.padding(top = 5.dp).size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)))
                                                Text(point,
                                                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 15.sp),
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Size note — compact single line ──────────────────────────
                if (scene.sizeNote.isNotBlank()) {
                    Surface(
                        color  = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape  = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Straighten, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp))
                            Text(scene.sizeNote,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title   = { Text("Delete Scene") },
            text    = { Text("Remove \"${scene.projectName}\" permanently?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED HELPERS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StageSceneBlock(
    label: String,
    bitmap: android.graphics.Bitmap,
    chosen: Boolean,
    chosenLabel: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FsLabel(label)
            if (chosen) {
                Surface(
                    color = AeGold.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(5.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.Check, null,
                            tint = AeGold,
                            modifier = Modifier.size(10.dp))
                        Text(chosenLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = AeGold)
                    }
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Image(
            bitmap             = bitmap.asImageBitmap(),
            contentDescription = label,
            contentScale       = ContentScale.FillWidth,
            modifier           = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
        )
    }
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color    = MaterialTheme.colorScheme.surface,
        shape    = RoundedCornerShape(14.dp),
        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), content = content)
    }
}

@Composable
private fun FsLabel(text: String) {
    Text(text,
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.2.sp,
            fontWeight    = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
}

private fun statusColor(status: String): androidx.compose.ui.graphics.Color {
    return when (status) {
        "APPROVED"       -> AeGreen
        "NEEDS_REVISION" -> AeAmber
        else             -> AeRed
    }
}