package com.example.coloranalysis.ui.result

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.coloranalysis.data.AppDatabase
import com.example.coloranalysis.data.helper.PaletteHelper
import com.example.coloranalysis.data.models.Profile
import com.example.coloranalysis.ui.components.SeasonPaletteDisplay
import com.example.coloranalysis.ui.components.WardrobeScreen
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

    val profile by db.observeProfile(profileId)
        .collectAsState(initial = null)
    var isLoading by remember { mutableStateOf(true) }

    var processedBitmap by remember { mutableStateOf<Bitmap?>(null) }


    // Thêm state này ở đầu hàm ResultScreen (cùng chỗ với các state khác)
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Bảng màu", "Tủ đồ", "Thông số")

    // Khai báo riêng ScrollState cho từng tab để khi chuyển tab không bị nhảy vị trí cuộn
    val tab1ScrollState = rememberScrollState()
    val tab3ScrollState = rememberScrollState()

    // Update lại logic hiện icon cuộn xuống tùy theo tab đang mở
    val showScrollDownIcon by remember {
        derivedStateOf {
            when (selectedTabIndex) {
                0 -> tab1ScrollState.canScrollForward
                2 -> tab3ScrollState.canScrollForward
                else -> false
            }
        }
    }

    var previewColor by remember { mutableStateOf<Int?>(null) }

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

                db.updateProfile(updatedProfile)

                val uriString = updatedProfile.imgProcessedUri
                val processed = loadBitmapFromUri(context, Uri.parse(uriString))

                withContext(Dispatchers.Main) {
                    processedBitmap = processed
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("Kết quả Phân tích") }) },
        bottomBar = {
            Surface(
                modifier = Modifier.navigationBarsPadding(),
                color = MaterialTheme.colorScheme.background, // Giữ màu nền tệp với app
                tonalElevation = 4.dp // Tạo đổ bóng nhẹ để tách biệt với nội dung cuộn
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = navigateToHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Về Trang chủ", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
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
                        .padding(padding) // Padding của Scaffold
                ) {
                    // --- 1. THANH TAB HEADER ---
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }

                    // --- 2. NỘI DUNG TỪNG TAB ---
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        when (selectedTabIndex) {

                            // TAB 1: BẢNG MÀU MÙA
                            0 -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp)
                                        .verticalScroll(tab1ScrollState),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Tên mùa
                                    Text(
                                        text = p.seasonType ?: "Unknown",
                                        style = MaterialTheme.typography.displaySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Bảng màu JSON + Lọc
                                    SeasonPaletteDisplay(
                                        seasonName = p.seasonType,
                                        initialPersonalities = p.personalityType ?: emptyList(),
                                        initialLifestyles = p.lifestyleType ?: emptyList(),
                                        onFilterChanged = { pers, life ->
                                            scope.launch(Dispatchers.IO) {
                                                db.updateProfile(
                                                    p.copy(
                                                        personalityType = pers,
                                                        lifestyleType = life
                                                    )
                                                )
                                            }
                                        },
                                        onColorClick = { color ->
                                            previewColor = color
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(80.dp)) // Tránh bị nút cuộn che
                                }
                            }

                            // TAB 2: TỦ ĐỒ
                            1 -> {
                                val context = LocalContext.current

                                // Thêm p.personalityType và p.lifestyleType vào remember key
                                // để khi DB thay đổi, danh sách màu tự động cập nhật
                                val paletteColors: List<Int> = remember(p.seasonType, p.personalityType, p.lifestyleType) {
                                    val fileName = PaletteHelper.getFileName(p.seasonType)

                                    if (fileName != null) {
                                        val allItems = PaletteHelper.loadPalette(context, fileName)

                                        // LỌC DỮ LIỆU y hệt như SeasonPaletteDisplay
                                        val filteredItems = allItems.filter { item ->
                                            val matchPers = p.personalityType.isNullOrEmpty() || item.personality.any { it in p.personalityType!! }
                                            val matchLife = p.lifestyleType.isNullOrEmpty() || item.lifestyle.any { it in p.lifestyleType!! }
                                            matchPers && matchLife
                                        }

                                        // Chuyển đổi mã Hex sang Int
                                        filteredItems.mapNotNull { item ->
                                            try {
                                                val hexString = item.hex
                                                val safeHex = if (hexString.startsWith("#")) hexString else "#$hexString"
                                                android.graphics.Color.parseColor(safeHex)
                                            } catch (e: Exception) {
                                                null
                                            }
                                        }
                                    } else {
                                        emptyList()
                                    }

                                }


                                // Truyền List<Int> đã ĐƯỢC LỌC vào màn hình Wardrobe
                                WardrobeScreen(
                                    profile = p,
                                    availableColorsFromPalette = paletteColors
                                )
                            }

                            // TAB 3: THÔNG SỐ CHI TIẾT
                            2 -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 20.dp)
                                        .verticalScroll(tab3ScrollState),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Score Card
                                    ScoreCard(p)

                                    Spacer(modifier = Modifier.height(32.dp))

                                    // Bảng thông số HSV
                                    Text(
                                        "Thông số kỹ thuật (HSV)",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.5f
                                            )
                                        )
                                    ) {
                                        Column(Modifier.padding(8.dp)) {
                                            ColorDetailRow("Da (Skin)", p.skinColor)
                                            ColorDetailRow("Tóc (Hair)", p.hairColor)
                                            ColorDetailRow("Mắt (Eye)", p.eyeColor)
                                            ColorDetailRow("Môi (Lip)", p.lipColor)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Ảnh xử lý
                                    Text(
                                        "Ảnh dùng để phân tích",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
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



                                }
                            }
                        }

                        previewColor?.let { colorInt ->

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(androidx.compose.ui.graphics.Color(colorInt or 0xFF000000.toInt()))
                                    .zIndex(100f)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) {
                                        previewColor = null
                                    }
                            )
                        }

                        // --- 3. NÚT CUỘN XUỐNG (Dùng chung cho cả Tab 1 và Tab 3) ---
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Khai báo rõ package để tránh lỗi implicit receiver
                            androidx.compose.animation.AnimatedVisibility(
                                visible = showScrollDownIcon,
                                enter = fadeIn() + slideInVertically { it / 2 },
                                exit = fadeOut() + slideOutVertically { it / 2 },
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f))
                                        .padding(12.dp)
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
        }
    }
}

/**
 * Data class to hold calculation results
 */
data class ColorScores(val hue: Float, val chroma: Float, val value: Float)

fun scaleHue(rawHue: Float): Float {

    // Fix 1: Xử lý số âm đúng cách (Rất chuẩn xác)
    val hue = ((rawHue % 360f) + 360f) % 360f

    return when {
        // ===== VÙNG 1: SKIN RANGE (0–40°) =====
        // 0° (Hồng/Đỏ) -> Cool (0 điểm)
        // 40° (Cam/Vàng) -> Warm (100 điểm)
        hue <= 40f -> {
            (hue / 40f) * 100f
        }

        // ===== VÙNG 2: QUÁ VÀNG / ÁM XANH LÁ (40°–120°) =====
        // Điểm ấm giảm dần từ 100 về 0
        // Tại 40.01° -> ~100 điểm (Khớp hoàn hảo với Vùng 1)
        // Tại 120° -> 0 điểm
        hue <= 120f -> {
            ((120f - hue) / 80f) * 100f
        }

        // ===== VÙNG 3: OUTLIERS LẠNH (120°–360°) =====
        // Xanh lam (210°), Tím (270°), Hồng tía/Đỏ lạnh (340°)
        // Tất cả đều được tính là Undertone LẠNH (0 điểm)
        // Tại 359.99° -> 0 điểm (Khớp hoàn hảo với mốc 0° của Vùng 1)
        else -> {
            0f
        }
    }.coerceIn(0f, 100f)
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
    val min = 0.10f
    val max = 0.90f
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
