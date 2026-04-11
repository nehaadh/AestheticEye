package com.example.aestheticeye.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aestheticeye.data.UiState
import com.example.aestheticeye.ui.components.ImageUploadCard
import com.example.aestheticeye.ui.components.LoadingScreen
import com.example.aestheticeye.ui.components.ResultDisplay
import com.example.aestheticeye.ui.components.UrlImportSheet
import com.example.aestheticeye.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current

    val roomBitmap    by viewModel.roomBitmap.collectAsStateWithLifecycle()
    val productBitmap by viewModel.productBitmap.collectAsStateWithLifecycle()
    val detailBitmap  by viewModel.detailBitmap.collectAsStateWithLifecycle()

    val roomLabel    by viewModel.roomLabel.collectAsStateWithLifecycle()
    val productLabel by viewModel.productLabel.collectAsStateWithLifecycle()
    val detailLabel  by viewModel.detailLabel.collectAsStateWithLifecycle()

    val prompt         by viewModel.prompt.collectAsStateWithLifecycle()
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val isIterating    by viewModel.isIterating.collectAsStateWithLifecycle()
    val productsPlaced by viewModel.productsPlaced.collectAsStateWithLifecycle()
    val showUrlSheet   by viewModel.showUrlSheet.collectAsStateWithLifecycle()
    val urlImportState by viewModel.urlImportState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text  = "Aesthetic Eye",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text  = "AI Interior Design Assistant",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── INPUT CARD ──────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Text(
                        text  = if (isIterating) "Add Another Product" else "Create Your Scene",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (isIterating)
                            "Pick your next product to add to the scene"
                        else
                            "Upload photos or paste URLs for each slot",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── IMAGE SLOTS ──────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                        // ROW 1: Room + Product
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ImageUploadCard(
                                label         = roomLabel,
                                icon          = Icons.Default.Add,
                                bitmap        = roomBitmap,
                                onImagePicked = { uri -> viewModel.setRoomImage(uri, context) },
                                onClear       = { viewModel.clearRoom() },
                                onUrlImport   = if (!isIterating) {
                                    { viewModel.openUrlSheet(MainViewModel.ImageSlot.ROOM) }
                                } else null,  // Don't allow changing room during iteration
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
                        }

                        // ROW 2: Detail (full width)
                        if (!isIterating) {
                            ImageUploadCard(
                                label         = detailLabel,
                                icon          = Icons.Default.Add,
                                bitmap        = detailBitmap,
                                onImagePicked = { uri -> viewModel.setDetailImage(uri, context) },
                                onClear       = { viewModel.clearDetail() },
                                onUrlImport   = { viewModel.openUrlSheet(MainViewModel.ImageSlot.DETAIL) },
                                optional      = true,
                                modifier      = Modifier
                                    .fillMaxWidth(0.48f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── PLACEMENT INSTRUCTIONS ──────────────────────────
                    Text(
                        if (isIterating) "Placement for this product" else "4. Placement Instructions",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value         = prompt,
                        onValueChange = { viewModel.setPrompt(it) },
                        modifier      = Modifier.fillMaxWidth(),
                        minLines      = 2,
                        maxLines      = 4,
                        placeholder   = {
                            Text(
                                if (isIterating)
                                    "e.g., Place the lamp on the side table next to the sofa."
                                else
                                    "e.g., Place the sofa against the main wall.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── GENERATE BUTTON ─────────────────────────────────
                    val isLoading   = uiState is UiState.Loading
                    val canGenerate = roomBitmap != null && productBitmap != null && !isLoading

                    Button(
                        onClick  = { viewModel.generateScene() },
                        enabled  = canGenerate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                color       = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Generating...")
                        } else {
                            Text(
                                if (isIterating) "Add Product to Scene"
                                else "Generate Scene & Review",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }

                    // Start Over button during iteration
                    if (isIterating) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick  = { viewModel.startOver() },
                            modifier = Modifier.fillMaxWidth(),
                            shape    = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Over with Original Room")
                        }
                    }

                    if (uiState is UiState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text  = (uiState as UiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // ── OUTPUT CARD ─────────────────────────────────────────────
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 300.dp),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 300.dp)
                        .padding(16.dp)
                ) {
                    when (val state = uiState) {
                        is UiState.Idle -> {
                            Column(
                                modifier            = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text  = if (isIterating)
                                        "Pick your next product above"
                                    else
                                        "Your AI Feedback Awaits",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        is UiState.Loading -> {
                            LoadingScreen(step = state.step, modifier = Modifier.fillMaxSize())
                        }
                        is UiState.Success -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ResultDisplay(result = state.result)

                                Spacer(modifier = Modifier.height(12.dp))

                                // ── Add Another Product button ──────────
                                state.result.generatedBitmap?.let { generatedBitmap ->
                                    Button(
                                        onClick  = { viewModel.addAnotherProduct(generatedBitmap) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Add Another Product to This Scene",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                // ── Done / Start Over ───────────────────
                                TextButton(
                                    onClick  = { viewModel.resetResult() },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("Done — Start New Scene")
                                }
                            }
                        }
                        is UiState.Error -> {
                            Column(
                                modifier            = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text  = state.message,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(12.dp))
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

    // ── URL Import Bottom Sheet ─────────────────────────────────────
    if (showUrlSheet) {
        UrlImportSheet(
            slotType   = viewModel.getSlotType(),
            state      = urlImportState,
            onExtract  = { url -> viewModel.extractFromUrl(url) },
            onUseImage = { bitmap, name -> viewModel.useImportedImage(bitmap, name) },
            onDismiss  = { viewModel.closeUrlSheet() }
        )
    }
}