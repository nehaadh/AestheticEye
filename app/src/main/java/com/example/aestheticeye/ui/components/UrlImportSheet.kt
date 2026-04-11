package com.example.aestheticeye.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.aestheticeye.network.ProductUrlExtractor

// ── State for URL import flow ───────────────────────────────────────────────

sealed class UrlImportState {
    object Idle : UrlImportState()
    data class Loading(val step: String) : UrlImportState()
    data class ProductResult(
        val product: ProductUrlExtractor.ExtractedProduct,
        val bitmap: Bitmap?
    ) : UrlImportState()
    data class ImageResult(
        val title: String,
        val bitmap: Bitmap?
    ) : UrlImportState()
    data class Error(val message: String) : UrlImportState()
}

/**
 * The type of image slot being imported into.
 * Product slots show full product details; room/detail slots show just the image.
 */
enum class ImportSlotType {
    ROOM, PRODUCT, DETAIL
}

/**
 * Bottom sheet for importing an image from a URL.
 * For product slots: extracts full product details (name, price, material, etc.)
 * For room/detail slots: just extracts and downloads the main image.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrlImportSheet(
    slotType: ImportSlotType,
    state: UrlImportState,
    onExtract: (String) -> Unit,
    onUseImage: (Bitmap, String) -> Unit,
    onDismiss: () -> Unit
) {
    var urlText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current

    val slotLabel = when (slotType) {
        ImportSlotType.ROOM -> "Room Photo"
        ImportSlotType.PRODUCT -> "Product"
        ImportSlotType.DETAIL -> "Detail Image"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Import $slotLabel",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = if (slotType == ImportSlotType.PRODUCT)
                            "Paste any furniture product link"
                        else
                            "Paste an image URL or page link",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── URL Input ───────────────────────────────────────────────
            OutlinedTextField(
                value = urlText,
                onValueChange = { urlText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "https://...",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (urlText.isBlank()) {
                        TextButton(
                            onClick = {
                                val clip = clipboardManager.getText()?.text ?: ""
                                if (clip.startsWith("http")) urlText = clip
                            }
                        ) {
                            Text("Paste", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                enabled = state is UrlImportState.Idle || state is UrlImportState.Error
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── Idle / Error: Show extract button ───────────────────────
            if (state is UrlImportState.Idle || state is UrlImportState.Error) {
                Button(
                    onClick = { onExtract(urlText.trim()) },
                    enabled = urlText.isNotBlank() && urlText.startsWith("http"),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (slotType == ImportSlotType.PRODUCT) "Extract Product"
                        else "Import Image",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                if (state is UrlImportState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (slotType) {
                        ImportSlotType.PRODUCT -> "Works with IKEA, Amazon, Wayfair, Target, and most furniture sites"
                        ImportSlotType.ROOM -> "Paste a link to a room photo, interior design page, or direct image URL"
                        ImportSlotType.DETAIL -> "Paste a direct image URL or a page with a detail/texture image"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            // ── Loading ─────────────────────────────────────────────────
            if (state is UrlImportState.Loading) {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Product Result ──────────────────────────────────────────
            if (state is UrlImportState.ProductResult) {
                Spacer(modifier = Modifier.height(16.dp))
                val product = state.product
                val bitmap = state.bitmap

                // Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = product.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No image", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )

                if (product.brand.isNotBlank()) {
                    Text(product.brand, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                if (product.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        product.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 3, overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (product.material.isNotBlank()) InfoChip("Material", product.material)
                    if (product.color.isNotBlank()) InfoChip("Color", product.color)
                }

                if (product.dimensions.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Dimensions: ${product.dimensions}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }

                if (product.price.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("$${product.price}", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { bitmap?.let { onUseImage(it, product.name) } },
                    enabled = bitmap != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("✨ Use This Product", style = MaterialTheme.typography.titleSmall)
                }
            }

            // ── Image Result (room / detail) ────────────────────────────
            if (state is UrlImportState.ImageResult) {
                Spacer(modifier = Modifier.height(16.dp))
                val bitmap = state.bitmap

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = state.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No image", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        }
                    }
                }

                if (state.title.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        state.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2, overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { bitmap?.let { onUseImage(it, state.title) } },
                    enabled = bitmap != null,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("✨ Use This Image", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Text(value.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}