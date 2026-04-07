package com.example.aestheticeye.ui.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.aestheticeye.data.AestheticResult
import com.example.aestheticeye.data.DesignDirectorFeedback
import com.example.aestheticeye.ui.theme.Green500
import com.example.aestheticeye.ui.theme.Red500
import com.example.aestheticeye.ui.theme.Yellow500
import java.io.OutputStream

@Composable
fun ResultDisplay(
    result: AestheticResult,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // NO verticalScroll here — parent already scrolls
    Column(modifier = modifier.fillMaxWidth()) {

        Text(
            text  = "Generated Scene",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (result.generatedBitmap != null) {
            Image(
                bitmap             = result.generatedBitmap.asImageBitmap(),
                contentDescription = "Generated interior scene",
                contentScale       = ContentScale.Fit,
                modifier           = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick  = {
                    saveImageToGallery(result.generatedBitmap, context)
                    Toast.makeText(context, "Saved to Gallery!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("⬇ Save to Gallery")
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "Image generation unavailable.\nSee director critique below.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        result.directorFeedback?.let { feedback ->
            Spacer(modifier = Modifier.height(20.dp))
            DirectorCritiqueCard(feedback = feedback)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DirectorCritiqueCard(feedback: DesignDirectorFeedback) {
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
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Design Director's Report",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
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
                        Text(flaw, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            if (feedback.suggestionForImprovement.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("💡 Suggestion",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(feedback.suggestionForImprovement,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

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