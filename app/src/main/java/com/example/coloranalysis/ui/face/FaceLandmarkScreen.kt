package com.example.coloranalysis.ui.face

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.font.FontWeight
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
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceLandmarkScreen(
    profileId: Int,
    navigateToResult: () -> Unit,
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context).profileDao() }

    val scrollState = rememberScrollState()

    // Kiểm tra xem màn hình có thể cuộn xuống nữa không
    val showScrollDownIcon by remember {
        derivedStateOf { scrollState.canScrollForward }
    }

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
                    val skinIndices = intArrayOf(10, 67, 103, 109, 67)
                    val eyeIndices = intArrayOf(468, 473) // Tâm Iris (Tròng đen) trái và phải
                    val lipIndices = intArrayOf(85, 86, 315, 316)


                    // A: Calculate Average Colors
                    // Da và Môi để mặc định isEyeRegion = false (Dùng ConvexHull)
                    val sColor = getAverageColorFromLandmarks(original, landmarks, skinIndices, FaceRegion.SKIN)
                    val eColor = getAverageColorFromLandmarks(original, landmarks, eyeIndices, FaceRegion.EYE)
                    val lColor = getAverageColorFromLandmarks(original, landmarks, lipIndices, FaceRegion.LIP)
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
        bottomBar = {
            Surface(
                modifier = Modifier.navigationBarsPadding(),
                color = MaterialTheme.colorScheme.background, // Giữ màu nền tệp với app
                tonalElevation = 4.dp // Tạo viền mờ tách biệt với nội dung cuộn
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = navigateToResult,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = if (isProcessing) "Đang phân tích..." else "Xem Kết Quả",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // 1. NỘI DUNG CUỘN (COLUMN)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = statusMessage,
                    color = if (isProcessing) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isProcessing) {
                    Box(modifier = Modifier.height(350.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(60.dp))
                    }
                } else {
                    // MAIN IMAGE PREVIEW
                    maskedBitmap?.let {
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Masked Preview",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(350.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // CARD KẾT QUẢ CHI TIẾT
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Kết quả tách",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            hairBitmap?.let {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "Hair Region",
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(CircleShape)
                                            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Vùng tóc nội suy", style = MaterialTheme.typography.labelMedium)
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Màu của các đặc điểm", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                ColorChip("Tóc", hairColor ?: android.graphics.Color.DKGRAY)
                                ColorChip("Da", skinColor ?: android.graphics.Color.GRAY)
                                ColorChip("Mắt", eyeColor ?: android.graphics.Color.BLACK)
                                ColorChip("Môi", lipColor ?: android.graphics.Color.RED)
                            }
                        }
                    }

                    // Khoảng trống dưới cùng để cuộn lên được thoải mái
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }

            // 2. ICON CUỘN XUỐNG
            // Do nằm chung trong "BOX TỔNG" nên nó tự động bị đẩy lên trên BottomBar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp), // Đẩy icon lên cách mép BottomBar 16dp
                contentAlignment = Alignment.BottomCenter
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollDownIcon && !isProcessing,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f))
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Cuộn xuống",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
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
    val src = Mat()
    Utils.bitmapToMat(bitmap, src)

    // Convert sang LAB (ổn định ánh sáng hơn RGB)
    val lab = Mat()
    Imgproc.cvtColor(src, lab, Imgproc.COLOR_RGB2Lab)

    val mean = Core.mean(lab)

    val l = mean.`val`[0]
    val a = mean.`val`[1]
    val b = mean.`val`[2]

    val result = MatOfDouble(l, a, b)

    val labPixel = Mat(1, 1, CvType.CV_8UC3)
    labPixel.put(0, 0, l, a, b)

    val rgb = Mat()
    Imgproc.cvtColor(labPixel, rgb, Imgproc.COLOR_Lab2RGB)

    val data = ByteArray(3)
    rgb.get(0, 0, data)

    src.release()
    lab.release()
    labPixel.release()
    rgb.release()

    return Color.rgb(
        data[0].toInt() and 0xFF,
        data[1].toInt() and 0xFF,
        data[2].toInt() and 0xFF
    )
}

// Enum định nghĩa vùng cần lấy mẫu
enum class FaceRegion { EYE, SKIN, LIP }

private fun getAverageColorFromLandmarks(
    bitmap: Bitmap,
    landmarks: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
    indices: IntArray,
    regionType: FaceRegion
): Int {
    val src = Mat()
    Utils.bitmapToMat(bitmap, src)

    // Mask đen (0)
    val mask = Mat.zeros(src.size(), CvType.CV_8UC1)
    val points = mutableListOf<Point>()

    indices.forEach { index ->
        if (index < landmarks.size) {
            val lm = landmarks[index]
            val x = (lm.x() * bitmap.width).toInt()
            val y = (lm.y() * bitmap.height).toInt()
            points.add(Point(x.toDouble(), y.toDouble()))
        }
    }

    if (points.isEmpty()) {
        src.release(); mask.release()
        return Color.BLACK
    }

    when (regionType) {
        FaceRegion.EYE -> {
            // MẮT: Vẽ vòng tròn rất nhỏ (bán kính ~ 1.5%) tại tâm 2 tròng đen
            val radius = (bitmap.width * 0.015).toInt().coerceAtLeast(2)
            for (pt in points) {
                Imgproc.circle(mask, pt, radius, Scalar(255.0), -1)
            }
        }

        FaceRegion.SKIN -> {
            // DA: Vẽ các mảng hình tròn (bán kính ~ 4-5%)
            // Bán kính lớn hơn để lấy trung bình được nhiều lỗ chân lông/vùng da hơn, giảm nhiễu cục bộ.
            val radius = (bitmap.width * 0.04).toInt().coerceAtLeast(5)
            for (pt in points) {
                Imgproc.circle(mask, pt, radius, Scalar(255.0), -1)
            }
        }

        FaceRegion.LIP -> {
            // MÔI: Do môi có hình dáng dài mảnh, ta vẫn dùng ConvexHull để bao quanh khối
            val matOfPoint = MatOfPoint(*points.toTypedArray())
            val hull = MatOfInt()
            Imgproc.convexHull(matOfPoint, hull)

            val hullPoints = hull.toArray().map { points[it] }
            val poly = MatOfPoint(*hullPoints.toTypedArray())

            Imgproc.fillConvexPoly(mask, poly, Scalar(255.0))
        }
    }

    // Tính giá trị trung bình chỉ tại những điểm có màu trắng (255) trên Mask
    val mean = Core.mean(src, mask)

    src.release()
    mask.release()

    return Color.rgb(
        mean.`val`[0].toInt().coerceIn(0, 255),
        mean.`val`[1].toInt().coerceIn(0, 255),
        mean.`val`[2].toInt().coerceIn(0, 255)
    )
}

