package com.example.coloranalysis.ui.face

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.coloranalysis.data.AppDatabase
import com.example.coloranalysis.ui.photo.loadBitmapFromUri
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker.FaceLandmarkerOptions
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter.ImageSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceLandmarkScreen(
    profileId: Int,
    navigateToResult: () -> Unit,
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context).profileDao() }

    // Display States
    var maskedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var hairBitmap by remember { mutableStateOf<Bitmap?>(null) }



    // Colors
    var hairColor by remember { mutableStateOf<Int?>(null) }
    var skinColor by remember { mutableStateOf<Int?>(null) }
    var eyeColor by remember { mutableStateOf<Int?>(null) }
    var lipColor by remember { mutableStateOf<Int?>(null) }

    var isProcessing by remember { mutableStateOf(true) }
    var statusMessage by remember { mutableStateOf("Đang chuẩn bị...") }

    LaunchedEffect(profileId) {
        isProcessing = true
        withContext(Dispatchers.IO) {
            var landmarker: FaceLandmarker? = null
            var segmenter: ImageSegmenter? = null

            try {
                val profile = db.getProfileById(profileId)
                val uriString = profile?.imgProcessedUri ?: return@withContext
                val loadedBitmap = loadBitmapFromUri(context, Uri.parse(uriString))
                val original = downscaleBitmap(loadedBitmap, 1024)

                // 1. HAIR SEGMENTATION
                val optionsSegment = ImageSegmenterOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("models/selfie_multiclass_256x256.tflite").build())
                    .setRunningMode(RunningMode.IMAGE)
                    .setOutputCategoryMask(true)
                    .build()
                segmenter = ImageSegmenter.createFromOptions(context, optionsSegment)

                val mpImage = BitmapImageBuilder(original).build()
                val resultSegment = segmenter.segment(mpImage)

                var baseMaskedBitmap: Bitmap? = null

                if (resultSegment != null && resultSegment.categoryMask().isPresent) {
                    val mask = resultSegment.categoryMask().get()
                    val buffer = ByteBufferExtractor.extract(mask)

                    // Tạo ảnh preview có phủ màu tóc
                    baseMaskedBitmap = applyHairMaskOnly(original, buffer)

                    // Bước A: Cắt vùng tóc (Category 1)
                    val localCroppedHair = extractAndCropRegion(original, buffer, 1)

                    // Bước B: Tính màu tóc trung bình từ vùng đã cắt (Bỏ qua pixel trong suốt)
                    var hColor: Int? = null
                    if (localCroppedHair != null) {
                        hColor = getAverageColor(localCroppedHair)
                    }

                    // Bước C: Cập nhật UI trên Main Thread một lần duy nhất
                    withContext(Dispatchers.Main) {
                        hairBitmap = localCroppedHair
                        hairColor = hColor
                    }
                }

                // 2. FACE LANDMARKER
                val optionsLandmark = FaceLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("models/face_landmarker.task").build())
                    .setRunningMode(RunningMode.IMAGE)
                    .build()
                landmarker = FaceLandmarker.createFromOptions(context, optionsLandmark)

                val landmarkResult = landmarker.detect(mpImage)

                if (landmarkResult.faceLandmarks().isNotEmpty()) {
                    val landmarks = landmarkResult.faceLandmarks()[0]

                    // Define Indices
                    val skinIndices = intArrayOf(6, 117, 123, 346, 352)
                    val eyeIndices = intArrayOf(468, 473) // Iris indices
                    val lipIndices = intArrayOf(11, 15, 72, 86, 302, 316)

                    // A: Calculate Average Colors
                    val sColor = getAverageColorFromLandmarks(original, landmarks, skinIndices)
                    val eColor = getAverageColorFromLandmarks(original, landmarks, eyeIndices)
                    val lColor = getAverageColorFromLandmarks(original, landmarks, lipIndices)

                    // 1. Save the final masked preview to a file (Internal Storage)

                    // 2. SAVE TO DATABASE
                    db.updateColorResults(
                        id = profileId,
                        skin = sColor,
                        hair = hairColor,
                        eye = eColor,
                        lip = lColor
                    )

                    // C: Draw Pinpoints on the Preview
                    val finalOverlay = drawPinpointsOnBitmap(
                        baseMaskedBitmap ?: original,
                        landmarks,
                        skinIndices, eyeIndices, lipIndices
                    )

                    withContext(Dispatchers.Main) {
                        maskedBitmap = finalOverlay
                        skinColor = sColor
                        eyeColor = eColor
                        lipColor = lColor
                    }
                    statusMessage = "Phân tích hoàn tất!"
                }



            } catch (e: Exception) {
                Log.e("FaceAnalysis", "Error: ${e.localizedMessage}")
                statusMessage = "Lỗi: ${e.localizedMessage}"
            } finally {
                segmenter?.close()
                landmarker?.close()
                withContext(Dispatchers.Main) { isProcessing = false }
            }
        }
    }

    Scaffold(
        // Scaffold sẽ tự chọn màu nền (trắng/đen) dựa trên Theme của máy
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Phân tích đặc điểm") })
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
            Text(statusMessage, color = MaterialTheme.colorScheme.secondary)

            Spacer(modifier = Modifier.height(16.dp))

            if (isProcessing) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 20.dp))
            } else {
                // 1. MAIN IMAGE PREVIEW (Giữ nguyên)
                maskedBitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Masked Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(350.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Kết quả chi tiết", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))

                // 2. HIỂN THỊ VÙNG TÓC ĐÃ TÁCH
                hairBitmap?.let {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Hair Region",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text("Vùng tóc", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("Bảng màu nhận diện", style = MaterialTheme.typography.titleSmall)

                // 3. DÃY MÀU KẾT QUẢ (Tóc, Da, Mắt, Môi)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ColorChip("Màu Tóc", hairColor ?: android.graphics.Color.DKGRAY)
                    ColorChip("Màu Da", skinColor ?: android.graphics.Color.GRAY)
                    ColorChip("Màu Mắt", eyeColor ?: android.graphics.Color.BLACK)
                    ColorChip("Màu Môi", lipColor ?: android.graphics.Color.RED)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = navigateToResult,
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                enabled = !isProcessing
            ) {
                Text("Tiếp tục")
            }
        }
    }


}

@Composable
fun ColorChip(label: String, colorInt: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(40.dp)) {
            drawCircle(color = androidx.compose.ui.graphics.Color(colorInt))
        }
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}



/**
 * Only tints the hair region (category 1) for visual feedback
 */
private fun applyHairMaskOnly(original: Bitmap, buffer: java.nio.ByteBuffer): Bitmap {
    val width = original.width
    val height = original.height
    val pixels = IntArray(width * height)
    original.getPixels(pixels, 0, width, 0, 0, width, height)

    buffer.rewind()
    for (i in pixels.indices) {
        val category = buffer.get().toInt()
        if (category == 1) { // HAIR
            val p = pixels[i]
            // Blend 40% Magenta overlay
            pixels[i] = Color.rgb(
                (Color.red(p) * 0.6f + 255 * 0.4f).toInt(),
                (Color.green(p) * 0.6f + 0 * 0.4f).toInt(),
                (Color.blue(p) * 0.6f + 255 * 0.4f).toInt()
            )
        }
    }
    val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    result.setPixels(pixels, 0, width, 0, 0, width, height)
    return result
}

/**
 * Draws small colored circles on the bitmap where landmarks were sampled
 */
private fun drawPinpointsOnBitmap(
    base: Bitmap,
    landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
    skinIdx: IntArray,
    eyeIdx: IntArray,
    lipIdx: IntArray
): Bitmap {
    val result = base.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(result)
    val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Draw Skin points (Yellow)
    paint.color = Color.YELLOW
    skinIdx.forEach { idx ->
        val x = landmarks[idx].x() * result.width
        val y = landmarks[idx].y() * result.height
        canvas.drawCircle(x, y, 6f, paint)
    }

    // Draw Eye points (Cyan/Blue)
    paint.color = Color.CYAN
    eyeIdx.forEach { idx ->
        val x = landmarks[idx].x() * result.width
        val y = landmarks[idx].y() * result.height
        canvas.drawCircle(x, y, 6f, paint)
    }

    // Draw Lip points (Red/Pink)
    paint.color = Color.RED
    lipIdx.forEach { idx ->
        val x = landmarks[idx].x() * result.width
        val y = landmarks[idx].y() * result.height
        canvas.drawCircle(x, y, 6f, paint)
    }

    return result
}

private fun downscaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
    val width = if (ratio > 1) maxSize else (maxSize * ratio).toInt()
    val height = if (ratio > 1) (maxSize / ratio).toInt() else maxSize
    return Bitmap.createScaledBitmap(bitmap, width, height, true)
}



private fun extractAndCropRegion(original: Bitmap, buffer: java.nio.ByteBuffer, targetCategory: Int): Bitmap? {
    val width = original.width
    val height = original.height
    val pixels = IntArray(width * height)
    val originalPixels = IntArray(width * height)
    original.getPixels(originalPixels, 0, width, 0, 0, width, height)

    var minX = width
    var maxX = 0
    var minY = height
    var maxY = 0
    var found = false

    buffer.rewind()
    for (y in 0 until height) {
        for (x in 0 until width) {
            val category = buffer.get().toInt()
            val index = y * width + x

            if (category == targetCategory) {
                // Keep the original pixel color
                pixels[index] = originalPixels[index]

                // Update Bounding Box boundaries
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
                found = true
            } else {
                // Make non-target pixels transparent
                pixels[index] = android.graphics.Color.TRANSPARENT
            }
        }
    }

    if (!found) return null

    // Create a temporary full-size bitmap with the transparent background
    val fullBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    fullBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

    // Add a small padding (10 pixels) to the crop so it's not too tight
    val padding = 10
    val cropX = (minX - padding).coerceAtLeast(0)
    val cropY = (minY - padding).coerceAtLeast(0)
    val cropW = (maxX + padding).coerceAtMost(width) - cropX
    val cropH = (maxY + padding).coerceAtMost(height) - cropY

    // Return the cropped region
    return Bitmap.createBitmap(fullBitmap, cropX, cropY, cropW, cropH)
}


private fun getAverageColor(bitmap: Bitmap): Int {
    var r = 0L; var g = 0L; var b = 0L
    var count = 0
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

    for (p in pixels) {
        // Chỉ tính toán nếu pixel không trong suốt
        if (android.graphics.Color.alpha(p) > 0) {
            r += android.graphics.Color.red(p)
            g += android.graphics.Color.green(p)
            b += android.graphics.Color.blue(p)
            count++
        }
    }
    return if (count > 0) {
        android.graphics.Color.rgb((r/count).toInt(), (g/count).toInt(), (b/count).toInt())
    } else {
        android.graphics.Color.TRANSPARENT
    }
}
private fun getAverageColorFromLandmarks(
    bitmap: Bitmap,
    landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
    indices: IntArray
): Int {
    var totalR = 0L
    var totalG = 0L
    var totalB = 0L
    var count = 0

    indices.forEach { index ->
        if (index < landmarks.size) {
            val lm = landmarks[index]
            val x = (lm.x() * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
            val y = (lm.y() * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)

            // Sample a tiny 3x3 patch around each point to avoid single-pixel noise
            for (i in -1..1) {
                for (j in -1..1) {
                    val px = (x + i).coerceIn(0, bitmap.width - 1)
                    val py = (y + j).coerceIn(0, bitmap.height - 1)
                    val pixel = bitmap.getPixel(px, py)

                    totalR += android.graphics.Color.red(pixel)
                    totalG += android.graphics.Color.green(pixel)
                    totalB += android.graphics.Color.blue(pixel)
                    count++
                }
            }
        }
    }

    return if (count > 0) {
        Color.rgb((totalR / count).toInt(), (totalG / count).toInt(), (totalB / count).toInt())
    } else {
        Color.TRANSPARENT
    }
}

