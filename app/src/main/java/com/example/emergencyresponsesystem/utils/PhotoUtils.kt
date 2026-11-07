package com.example.emergencyresponsesystem.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
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
import androidx.compose.ui.window.Dialog
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object PhotoUtils {

    fun saveBitmapToCache(context: Context, bitmap: Bitmap): String? {
        return try {
            val file = File(context.cacheDir, "incident_photo_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@Composable
fun IncidentPhotoThumbnail(
    photoUri: String?,
    modifier: Modifier = Modifier,
    height: Int = 160
) {
    val context = LocalContext.current
    var loadedBitmap by remember(photoUri) { mutableStateOf<Bitmap?>(null) }
    var showFullscreenDialog by remember { mutableStateOf(false) }

    LaunchedEffect(photoUri) {
        if (!photoUri.isNullOrBlank()) {
            try {
                val uri = Uri.parse(photoUri)
                val inputStream: InputStream? = if (photoUri.startsWith("content://") || photoUri.startsWith("file://")) {
                    context.contentResolver.openInputStream(uri)
                } else if (photoUri.startsWith("/")) {
                    File(photoUri).inputStream()
                } else null

                inputStream?.use {
                    loadedBitmap = BitmapFactory.decodeStream(it)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadedBitmap = null
            }
        } else {
            loadedBitmap = null
        }
    }

    if (showFullscreenDialog && loadedBitmap != null) {
        Dialog(onDismissRequest = { showFullscreenDialog = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📷 Incident Photo View", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { showFullscreenDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Image(
                        bitmap = loadedBitmap!!.asImageBitmap(),
                        contentDescription = "Full Incident Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clickable(enabled = loadedBitmap != null) {
                showFullscreenDialog = true
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF263238))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (loadedBitmap != null) {
                Image(
                    bitmap = loadedBitmap!!.asImageBitmap(),
                    contentDescription = "Incident Photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Text(
                        "🔍 Tap to expand",
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (!photoUri.isNullOrBlank()) "📷 Incident Photo Attached" else "No Photo Attached",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
