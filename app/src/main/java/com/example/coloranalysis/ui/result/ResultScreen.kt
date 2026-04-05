package com.example.coloranalysis.ui.result

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.coloranalysis.data.AppDatabase
import com.example.coloranalysis.data.models.Profile
import com.example.coloranalysis.ui.components.SeasonPaletteDisplay
import com.example.coloranalysis.ui.photo.loadBitmapFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import androidx.compose.ui.graphics.Color as ComposeColor

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ResultScreen(
    profileId: Int,
    navigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context).profileDao() }

    var profile by remember { mutableStateOf<Profile?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(profileId) {
        withContext(Dispatchers.IO) {
            val p = db.getProfileById(profileId)
            if (p != null) {
                val scores = calculateColorScores(p)
                val scaledProfile = p.copy(
                    hueScore = scores.hue,
                    chromaScore = scores.chroma,
                    valueScore = scores.value
                )
                val season = getSeason(scaledProfile)
                val updatedProfile = scaledProfile.copy(seasonType = season)
                db.insertProfile(updatedProfile)

                withContext(Dispatchers.Main) {
                    profile = updatedProfile
                    isLoading = false
                }
            }

            val uriString = profile?.imgProcessedUri

            // 2. Load the Bitmap (Fixed URI Loading)
            val processed = loadBitmapFromUri(context, Uri.parse(uriString))
            processedBitmap = processed
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Kết quả Phân tích") }) }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            profile?.let { p ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Tên mùa
                    Text(
                        text = p.seasonType ?: "Unknown",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 2. PHẦN BẢNG MÀU JSON + LỌC (Sử dụng Component đã tách)
                    SeasonPaletteDisplay(
                        seasonName = p.seasonType,
                        initialPersonalities = p.personalityType ?: emptyList(),
                        initialLifestyles = p.lifestyleType ?: emptyList(),
                        onFilterChanged = { pers, life ->
                            // Lưu vào DB khi người dùng thay đổi filter
                            scope.launch(Dispatchers.IO) {
                                db.insertProfile(p.copy(personalityType = pers, lifestyleType = life))
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    HorizontalDivider(thickness = 4.dp)
                    Spacer(modifier = Modifier.height(24.dp))

                    // 3. PERSONAL SCORES
                    ScoreCard(p)

                    Spacer(modifier = Modifier.height(32.dp))

                    // 4. DETAILED BREAKDOWN
                    Text( text = "Thông số kỹ thuật (HSV)", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            ColorDetailRow("Da (Skin)", p.skinColor)
                            ColorDetailRow("Tóc (Hair)", p.hairColor)
                            ColorDetailRow("Mắt (Eye)", p.eyeColor)
                            ColorDetailRow("Môi (Lip)", p.lipColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Ảnh dùng để phân tích", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
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

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = navigateToHome,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Về Trang chủ", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * Data class to hold calculation results
 */
data class ColorScores(val hue: Float, val chroma: Float, val value: Float)

/**
 * Hue: 0 (Red/Cool) -> 50 (Yellow/Warm)
 * Scaled: 0 (Cool) -> 100 (Warm)
 */
fun scaleHue(hue: Float): Float {
    val min = 0f
    val max = 50f
    return ((hue - min) / (max - min) * 100f).coerceIn(0f, 100f)
}

/**
 * Chroma: 0.13 (Muted/Soft) -> 0.85 (Clear/Bright)
 * Scaled: 0 (Soft) -> 100 (Bright)
 */
fun scaleChroma(chroma: Float): Float {
    val min = 0.13f
    val max = 0.85f
    return ((chroma - min) / (max - min) * 100f).coerceIn(0f, 100f)
}

/**
 * Value: 0.18 (Dark) -> 0.75 (Light)
 * Scaled: 0 (Dark) -> 100 (Light)
 */
fun scaleValue(value: Float): Float {
    val min = 0.18f
    val max = 0.75f
    return ((value - min) / (max - min) * 100f).coerceIn(0f, 100f)
}

/**
 * Calculations based on your formula:
 * 1. Hue = Hue of skin
 * 2. Chroma = (Chroma skin + Chroma eye + Chroma lip) / 3
 * 3. Value = (0.7 * Val skin) + (0.15 * Val eye) + (0.15 * Val hair)
 */
private fun calculateColorScores(p: Profile): ColorScores {
    val hsvSkin = FloatArray(3)
    val hsvHair = FloatArray(3)
    val hsvEye = FloatArray(3)
    val hsvLip = FloatArray(3)

    // Convert Int colors to HSV arrays
    Color.colorToHSV(p.skinColor ?: Color.GRAY, hsvSkin)
    Color.colorToHSV(p.hairColor ?: Color.BLACK, hsvHair)
    Color.colorToHSV(p.eyeColor ?: Color.BLACK, hsvEye)
    Color.colorToHSV(p.lipColor ?: Color.RED, hsvLip)

    val rawHue = hsvSkin[0]
    val rawChroma = (hsvSkin[1] + hsvEye[1] + hsvLip[1]) / 3f
    val rawValue = (0.7f * hsvSkin[2]) + (0.15f * hsvEye[2]) + (0.15f * hsvHair[2])

    return ColorScores(
        hue = scaleHue(rawHue),
        chroma = scaleChroma(rawChroma),
        value = scaleValue(rawValue)
    )
}


fun getSeason(p: Profile): String {
    val hue = p.hueScore ?: 50f
    val chroma = p.chromaScore ?: 50f
    val value = p.valueScore ?: 50f

    val hueDist = abs(hue - 50f)
    val chromaDist = abs(chroma - 50f)
    val valueDist = abs(value - 50f)

    val primary = when (maxOf(hueDist, chromaDist, valueDist)) {
        hueDist -> "HUE"
        chromaDist -> "CHROMA"
        else -> "VALUE"
    }

    val secondary = if (primary == "HUE") "CHROMA" else "HUE"

    val hueType = if (hue >= 50f) "Warm" else "Cool"
    val chromaType = if (chroma >= 50f) "Bright" else "Soft"
    val valueType = if (value >= 50f) "Light" else "Dark"

    return when (primary) {

        // ===== PRIMARY: HUE =====
        "HUE" -> {
            when (hueType) {
                "Warm" -> {
                    if (chromaType == "Bright") "True Spring"
                    else "True Autumn"
                }
                else -> {
                    if (chromaType == "Bright") "True Winter"
                    else "True Summer"
                }
            }
        }

        // ===== PRIMARY: VALUE =====
        "VALUE" -> {
            when (valueType) {
                "Light" -> {
                    if (hueType == "Warm") "Light Spring"
                    else "Light Summer"
                }
                else -> {
                    if (hueType == "Warm") "Dark Autumn"
                    else "Dark Winter"
                }
            }
        }

        // ===== PRIMARY: CHROMA =====
        "CHROMA" -> {
            when (chromaType) {
                "Bright" -> {
                    if (hueType == "Warm") "Bright Spring"
                    else "Bright Winter"
                }
                else -> {
                    if (hueType == "Warm") "Soft Autumn"
                    else "Soft Summer"
                }
            }
        }

        else -> "Unknown"
    }
}
@Composable
fun ScoreCard(p: Profile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Chỉ số Cá nhân (0-100)", style = MaterialTheme.typography.titleLarge)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Removed the ° symbol since it's a 0-100 score now
            ScoreRow("Tông màu (Hue):", String.format("%.0f/100", p.hueScore ?: 0f))
            ScoreRow("Độ bão hòa (Chroma):", String.format("%.0f/100", p.chromaScore ?: 0f))
            ScoreRow("Độ sáng (Value):", String.format("%.0f/100", p.valueScore ?: 0f))
        }
    }
}

@Composable
fun ScoreRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun ColorDetailRow(label: String, colorInt: Int?) {
    val hsv = FloatArray(3)
    colorInt?.let { Color.colorToHSV(it, hsv) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(ComposeColor(colorInt ?: Color.TRANSPARENT), MaterialTheme.shapes.small)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall)
            // Displaying H, S, V for debugging/transparency
            Text(
                "H: ${hsv[0].toInt()}° S: ${(hsv[1] * 100).toInt()}% V: ${(hsv[2] * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = ComposeColor.Gray
            )
        }
    }
}
