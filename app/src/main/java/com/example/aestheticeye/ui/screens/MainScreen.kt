package com.example.aestheticeye.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aestheticeye.data.UiState
import com.example.aestheticeye.ui.components.*
import com.example.aestheticeye.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current

    val roomBitmap        by viewModel.roomBitmap.collectAsStateWithLifecycle()
    val productBitmap     by viewModel.productBitmap.collectAsStateWithLifecycle()
    val detailBitmap      by viewModel.detailBitmap.collectAsStateWithLifecycle()
    val roomLabel         by viewModel.roomLabel.collectAsStateWithLifecycle()
    val productLabel      by viewModel.productLabel.collectAsStateWithLifecycle()
    val detailLabel       by viewModel.detailLabel.collectAsStateWithLifecycle()
    val prompt            by viewModel.prompt.collectAsStateWithLifecycle()
    val uiState           by viewModel.uiState.collectAsStateWithLifecycle()
    val improvementState   by viewModel.improvementState.collectAsStateWithLifecycle()
    val isIterating       by viewModel.isIterating.collectAsStateWithLifecycle()
    val showUrlSheet      by viewModel.showUrlSheet.collectAsStateWithLifecycle()
    val urlImportState    by viewModel.urlImportState.collectAsStateWithLifecycle()
    val showFavorites     by viewModel.showFavorites.collectAsStateWithLifecycle()
    val favorites         by viewModel.favorites.collectAsStateWithLifecycle()
    val selectedFavorite  by viewModel.selectedFavorite.collectAsStateWithLifecycle()
    val saveDialogVisible by viewModel.saveDialogVisible.collectAsStateWithLifecycle()

    if (showFavorites) {
        FavoritesScreen(
            favorites        = favorites,
            selectedFavorite = selectedFavorite,
            loadBitmap       = { }, // unused, viewModel called directly
            onOpenDetail     = { viewModel.openFavoriteDetail(it) },
            onCloseDetail    = { viewModel.closeFavoriteDetail() },
            onDelete         = { viewModel.deleteFavorite(it) },
            onClose          = { viewModel.closeFavorites() },
            viewModel        = viewModel
        )
        return
    }

    var saveProjectName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Aesthetic Eye",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("AI Interior Design",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openFavorites() }) {
                        BadgedBox(badge = {
                            if (favorites.isNotEmpty()) Badge { Text("${favorites.size}") }
                        }) {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = "Collections")
                        }
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── INPUT CARD ────────────────────────────────────────────────────
            Surface(
                color  = MaterialTheme.colorScheme.surface,
                shape  = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Text(
                        if (isIterating) "Add Another Product" else "Create Your Scene",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (isIterating) "Select the next product to add"
                        else "Upload photos or paste product URLs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )

                    Spacer(Modifier.height(16.dp))

                    // Image slots — always 3 equal cards in a row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ImageUploadCard(
                            label         = roomLabel,
                            icon          = Icons.Default.Add,
                            bitmap        = roomBitmap,
                            onImagePicked = { uri -> viewModel.setRoomImage(uri, context) },
                            onClear       = { viewModel.clearRoom() },
                            onUrlImport   = if (!isIterating) {
                                { viewModel.openUrlSheet(MainViewModel.ImageSlot.ROOM) }
                            } else null,
                            modifier      = Modifier.weight(1f)
                        )
                        ImageUploadCard(
                            label         = productLabel,
                            icon          = Icons.Default.Add,
                            bitmap        = productBitmap,
                            onImagePicked = { uri -> viewModel.setProductImage(uri, context) },
                            onClear       = { viewModel.clearProduct() },
                            onUrlImport   = { viewModel.openUrlSheet(MainViewModel.ImageSlot.PRODUCT) },
                            modifier      = Modifier.weight(1f)
                        )
                        if (!isIterating) {
                            ImageUploadCard(
                                label         = detailLabel,
                                icon          = Icons.Default.Add,
                                bitmap        = detailBitmap,
                                onImagePicked = { uri -> viewModel.setDetailImage(uri, context) },
                                onClear       = { viewModel.clearDetail() },
                                onUrlImport   = { viewModel.openUrlSheet(MainViewModel.ImageSlot.DETAIL) },
                                optional      = true,
                                modifier      = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        if (isIterating) "PLACEMENT INSTRUCTION" else "PLACEMENT INSTRUCTION",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value         = prompt,
                        onValueChange = { viewModel.setPrompt(it) },
                        modifier      = Modifier.fillMaxWidth(),
                        minLines      = 2,
                        maxLines      = 4,
                        placeholder   = {
                            Text(
                                if (isIterating) "e.g., Place the lamp beside the sofa."
                                else "e.g., Place the sofa against the main wall.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(Modifier.height(14.dp))

                    val isLoading   = uiState is UiState.Loading || uiState is UiState.SceneReady
                    val canGenerate = roomBitmap != null && productBitmap != null && !isLoading

                    Button(
                        onClick  = { viewModel.generateScene() },
                        enabled  = canGenerate,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape    = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            AeDots(color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(10.dp))
                            Text("Generating…")
                        } else {
                            Text(
                                if (isIterating) "Add to Scene" else "Generate & Review",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    if (isIterating) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick  = { viewModel.startOver() },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Start Over with Original Room", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    if (uiState is UiState.Error) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            (uiState as UiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // ── OUTPUT ────────────────────────────────────────────────────────
            Surface(
                color  = MaterialTheme.colorScheme.surface,
                shape  = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 280.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 280.dp)
                        .padding(16.dp)
                ) {
                    when (val state = uiState) {

                        is UiState.Idle -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome, null,
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    if (isIterating) "Pick your next product" else "Your results will appear here",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                )
                            }
                        }

                        is UiState.Loading -> {
                            LoadingScreen(step = state.step, modifier = Modifier.fillMaxSize())
                        }

                        is UiState.SceneReady -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Inline analysis progress banner
                                if (state.analysisStep.isNotBlank()) {
                                    Surface(
                                        color  = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                        shape  = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            AeDots(color = MaterialTheme.colorScheme.primary)
                                            Text(
                                                state.analysisStep,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                }
                                ResultDisplay(
                                    result              = state.result,
                                    improvementState     = improvementState,
                                    onRequestImprovement = { viewModel.requestAiImprovement() },
                                    onDismissImprovement = { viewModel.dismissImprovement() },
                                    onContinueWithScene = { chosenBitmap -> viewModel.addAnotherProduct(chosenBitmap) }
                                )
                            }
                        }

                        is UiState.Success -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ResultDisplay(
                                    result              = state.result,
                                    improvementState     = improvementState,
                                    onRequestImprovement = { viewModel.requestAiImprovement() },
                                    onDismissImprovement = { viewModel.dismissImprovement() },
                                    onContinueWithScene = { chosenBitmap ->
                                        viewModel.addAnotherProduct(chosenBitmap)
                                    }
                                )

                                Spacer(Modifier.height(4.dp))

                                // Action buttons
                                state.result.generatedBitmap?.let { bitmap ->

                                    OutlinedButton(
                                        onClick  = { saveProjectName = ""; viewModel.openSaveDialog() },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape    = RoundedCornerShape(10.dp),
                                        border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                    ) {
                                        Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Save to Collection", style = MaterialTheme.typography.labelLarge)
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Button(
                                        onClick  = { viewModel.addAnotherProduct(bitmap) },
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        shape    = RoundedCornerShape(10.dp),
                                        colors   = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor   = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Add Another Product", style = MaterialTheme.typography.labelLarge)
                                    }

                                    Spacer(Modifier.height(8.dp))
                                }

                                Spacer(Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick  = { viewModel.resetResult() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape    = RoundedCornerShape(12.dp),
                                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                                    colors   = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Icon(Icons.Default.Refresh, null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Start New Scene",
                                        style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        is UiState.Error -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Warning, null,
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                                    modifier = Modifier.size(32.dp))
                                Spacer(Modifier.height(8.dp))
                                Text(state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(onClick = { viewModel.resetResult() }) {
                                    Text("Try Again")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUrlSheet) {
        UrlImportSheet(
            slotType   = viewModel.getSlotType(),
            state      = urlImportState,
            onExtract  = { url -> viewModel.extractFromUrl(url) },
            onUseImage = { bitmap, name -> viewModel.useImportedImage(bitmap, name) },
            onDismiss  = { viewModel.closeUrlSheet() }
        )
    }

    if (saveDialogVisible) {
        val successState = when (val s = uiState) {
            is UiState.Success    -> s
            is UiState.SceneReady -> UiState.Success(s.result) // treat as success for saving
            else -> null
        }
        AlertDialog(
            onDismissRequest = { viewModel.closeSaveDialog() },
            title = { Text("Save to Collection") },
            text  = {
                Column {
                    Text("Name this scene to save it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value         = saveProjectName,
                        onValueChange = { saveProjectName = it },
                        label         = { Text("Scene name") },
                        placeholder   = { Text("e.g., Living Room Refresh") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick  = {
                        successState?.result?.generatedBitmap?.let { bitmap ->
                            viewModel.saveFavorite(context, saveProjectName, bitmap, successState.result)
                        }
                    },
                    enabled = successState?.result?.generatedBitmap != null
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeSaveDialog() }) { Text("Cancel") }
            }
        )
    }
}