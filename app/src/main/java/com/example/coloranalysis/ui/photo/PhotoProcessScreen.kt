package com.example.coloranalysis.ui.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.coloranalysis.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoProcessScreen(
    profileId: Int,
    navigateToFaceLandmark: () -> Unit,
    navigateToResult: () -> Unit // Add this if needed
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context).profileDao() }

    // State management
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }



    LaunchedEffect(profileId) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Fetch Profile from Database
                val profile = db.getProfileById(profileId)
                val uriString = profile?.imgOriginalUri

                if (uriString == null) {
                    errorMessage = "Không tìm thấy đường dẫn ảnh."
                    return@withContext
                }

                // 2. Load the Bitmap (Fixed URI Loading)
                val original = loadBitmapFromUri(context, Uri.parse(uriString))
                originalBitmap = original

                // 3. Apply OpenCV Whitening
                val processed = applyOpenCVWhitening(original)
                processedBitmap = processed

                // 4. Save the Whitened Image
                val savedUri = saveBitmap(context, processed, "whitened")

                // 5. Update Database
                db.updateImgProcessed(profileId, savedUri)

            } catch (e: Exception) {
                Log.e("PhotoProcess", "Lỗi xử lý: ${e.message}")
                errorMessage = "Lỗi: ${e.localizedMessage}"
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Xử lý hình ảnh") })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isProcessing) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                Text("Đang làm sáng ảnh bằng OpenCV...")
            } else if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            } else {
                // Display Original
                Text("Ảnh gốc", fontWeight = FontWeight.SemiBold)
                originalBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(250.dp)
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Display Processed
                Text("Ảnh sau khi xử lý", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                processedBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(250.dp)
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = navigateToFaceLandmark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tiếp tục phân tích khuôn mặt")
                }
            }
        }
    }
}

/**
 * Pure OpenCV Whitening Logic
 */
fun applyOpenCVWhitening(bitmap: Bitmap): Bitmap {
    val src = Mat()
    Utils.bitmapToMat(bitmap, src)

    // 1. Convert to LAB color space
    val labImage = Mat()
    Imgproc.cvtColor(src, labImage, Imgproc.COLOR_RGB2Lab)

    // 2. Split channels (L, A, B)
    val channels = mutableListOf<Mat>()
    Core.split(labImage, channels)

    // 3. Brighten only the L channel (Lightness)
    // Adding 15-20 is usually enough for a clean look
    Core.add(channels[0], org.opencv.core.Scalar(15.0), channels[0])

    // 4. Merge channels back
    Core.merge(channels, labImage)

    // 5. Convert back to RGB
    val resultMat = Mat()
    Imgproc.cvtColor(labImage, resultMat, Imgproc.COLOR_Lab2RGB)

    val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(resultMat, resultBitmap)

    // Cleanup
    src.release()
    labImage.release()
    channels.forEach { it.release() }
    resultMat.release()

    return resultBitmap
}

/**
 * Robust loading from Content URIs (Fixes "Image didn't load" issue)
 */
fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap {
    return context.contentResolver.openInputStream(uri)?.use { stream ->
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        BitmapFactory.decodeStream(stream, null, options)
    } ?: throw Exception("Không thể mở tệp ảnh")
}

/**
 * Saves processed bitmap to Internal storage
 */
fun saveBitmap(context: Context, bitmap: Bitmap, prefix: String): String {
    val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
    val file = File(context.filesDir, fileName)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
    }
    return Uri.fromFile(file).toString()
}