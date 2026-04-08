package com.example.aestheticeye.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.example.aestheticeye.viewmodel.MainViewModel
import com.example.aestheticeye.viewmodel.ProductSource
import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current

    val roomBitmap     by viewModel.roomBitmap.collectAsStateWithLifecycle()
    val productBitmap  by viewModel.productBitmap.collectAsStateWithLifecycle()
    val detailBitmap   by viewModel.detailBitmap.collectAsStateWithLifecycle()
    val prompt         by viewModel.prompt.collectAsStateWithLifecycle()
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val productSource  by viewModel.productSource.collectAsStateWithLifecycle()
    val showCatalog    by viewModel.showCatalog.collectAsStateWithLifecycle()

    if (showCatalog) {
        CatalogScreen(
            onProductSelected = { bitmap, productName ->
                viewModel.setProductFromCatalog(bitmap, productName)
            },
            onBack = { viewModel.closeCatalog() }
        )
        return
    }

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

            // ── INPUT CARD ──
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Text(
                        text  = "Create Your Scene",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ✅ NEW 2x2 GRID
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        // ROW 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ImageUploadCard(
                                label         = "1. Room Photo",
                                icon          = Icons.Default.Add,
                                bitmap        = roomBitmap,
                                onImagePicked = { uri -> viewModel.setRoomImage(uri, context) },
                                onClear       = { viewModel.setRoomImage(Uri.EMPTY, context) },
                                modifier      = Modifier.weight(1f)
                            )

                            ImageUploadCard(
                                label         = when (productSource) {
                                    is ProductSource.Catalog ->
                                        "2. ${(productSource as ProductSource.Catalog).productName}"
                                    else -> "2. Product"
                                },
                                icon          = Icons.Default.Add,
                                bitmap        = productBitmap,
                                onImagePicked = { uri -> viewModel.setProductImage(uri, context) },
                                onClear       = { viewModel.clearProduct() },
                                modifier      = Modifier.weight(1f)
                            )
                        }

                        // ROW 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ImageUploadCard(
                                label         = "3. Detail",
                                icon          = Icons.Default.Add,
                                bitmap        = detailBitmap,
                                onImagePicked = { uri -> viewModel.setDetailImage(uri, context) },
                                onClear       = { viewModel.clearDetailImage() },
                                optional      = true,
                                modifier      = Modifier.weight(1f)
                            )

                            // 4. Browse Catalog (matching card)
                            Card(
                                onClick = { viewModel.openCatalog() },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Or Browse",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("5. Placement Instructions", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value         = prompt,
                        onValueChange = { viewModel.setPrompt(it) },
                        modifier      = Modifier.fillMaxWidth(),
                        minLines      = 2,
                        maxLines      = 4,
                        placeholder   = {
                            Text(
                                "e.g., Place the blinds over the largest window.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        },
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

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
                            Text("Generate Scene & Review", style = MaterialTheme.typography.titleSmall)
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

            // OUTPUT CARD (unchanged)
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
                                    text  = "Your AI Feedback Awaits",
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
                                TextButton(
                                    onClick  = { viewModel.resetResult() },
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                ) {
                                    Text("Generate Another")
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
}