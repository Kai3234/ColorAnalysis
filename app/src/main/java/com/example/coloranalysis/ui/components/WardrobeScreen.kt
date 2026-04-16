package com.example.coloranalysis.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.coloranalysis.R
import com.example.coloranalysis.data.AppDatabase
import com.example.coloranalysis.data.helper.ColorHarmonyHelper
import com.example.coloranalysis.data.models.Outfit
import com.example.coloranalysis.data.models.Profile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun WardrobeScreen(
    profile: Profile,
    availableColorsFromPalette: List<Int>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context).outfitDao() }

    val outfits by db.getOutfitsForProfile(profile.id).collectAsState(initial = emptyList())

    var selectedOutfitToEdit by remember { mutableStateOf<Outfit?>(null) }
    var selectedItemType by remember { mutableStateOf<String>("") } // "main", "top", "bottom", "outer", "shoes", "acc"
    var showColorPicker by remember { mutableStateOf(false) }

    var outfitToRename by remember { mutableStateOf<Outfit?>(null) }
    var outfitToDelete by remember { mutableStateOf<Outfit?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val newOutfit = Outfit(
                        profileId = profile.id,
                        outfitName = "Trang phục ${outfits.size + 1}",
                        displayOrder = outfits.size
                    )
                    db.insertOutfit(newOutfit)
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Thêm Trang phục mới")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (outfits.isNotEmpty()) {

            val pagerState = rememberPagerState(
                initialPage = 0,
                pageCount = { outfits.size }
            )

            // ✅ FIX crash khi delete outfit cuối
            LaunchedEffect(outfits.size) {
                if (outfits.isNotEmpty()) {
                    val lastIndex = outfits.lastIndex
                    if (pagerState.currentPage > lastIndex) {
                        pagerState.scrollToPage(lastIndex)
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ===== OUTFIT PAGE =====
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->

                    val outfit = outfits[page]
                    Box(
                        modifier = Modifier.fillMaxSize(), // ✅ KEY FIX
                        contentAlignment = Alignment.TopCenter
                    ) {
                        OutfitCard(
                            outfit = outfit,
                            onItemClick = { itemType ->
                                selectedOutfitToEdit = outfit
                                selectedItemType = itemType
                                showColorPicker = true
                            },
                            onSchemeSelected = { scheme ->
                                scope.launch(Dispatchers.IO) {
                                    val updated = outfit.copy(colorScheme = scheme)
                                    db.updateOutfit(
                                        ColorHarmonyHelper.autoFillOutfit(updated)
                                    )
                                }
                            },
                            onRename = { outfitToRename = outfit },
                            onDelete = { outfitToDelete = outfit }
                        )
                    }


                }

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    // PREVIOUS
                    IconButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    (pagerState.currentPage - 1).coerceAtLeast(0)
                                )
                            }
                        },
                        enabled = pagerState.currentPage > 0,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Prev",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "${pagerState.currentPage + 1}/${outfits.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.width(8.dp))

                    // NEXT
                    IconButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(
                                    (pagerState.currentPage + 1)
                                        .coerceAtMost(outfits.lastIndex)
                                )
                            }
                        },
                        enabled = pagerState.currentPage < outfits.lastIndex,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Next",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (showColorPicker && selectedOutfitToEdit != null) {
        ColorPickerDialog(
            colors = availableColorsFromPalette,
            onColorSelected = { colorInt ->
                scope.launch(Dispatchers.IO) {
                    var updatedOutfit = when (selectedItemType) {
                        "main" -> selectedOutfitToEdit!!.copy(mainColor = colorInt) // Chọn màu chủ đạo
                        "top" -> selectedOutfitToEdit!!.copy(topColor = colorInt)
                        "bottom" -> selectedOutfitToEdit!!.copy(bottomColor = colorInt)
                        "outer" -> selectedOutfitToEdit!!.copy(outerwearColor = colorInt)
                        "shoes" -> selectedOutfitToEdit!!.copy(shoesColor = colorInt)
                        "acc" -> selectedOutfitToEdit!!.copy(accessoryColor = colorInt)
                        else -> selectedOutfitToEdit!!
                    }

                    // Nếu người dùng vừa chọn "Màu chủ đạo" mà đã có "Cách phối" từ trước -> Tự chạy lại autofill
                    if (selectedItemType == "main" && updatedOutfit.colorScheme != null) {
                        updatedOutfit = ColorHarmonyHelper.autoFillOutfit(updatedOutfit)
                    }

                    db.updateOutfit(updatedOutfit)
                }
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    var newName by remember { mutableStateOf("") }

    val renameTarget = outfitToRename

    renameTarget?.let { outfit ->

        LaunchedEffect(outfit.id) {
            newName = outfit.outfitName
        }

        AlertDialog(
            onDismissRequest = { outfitToRename = null },

            title = { Text("Đổi tên trang phục") },

            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {

                        scope.launch(Dispatchers.IO) {
                            db.updateOutfit(
                                outfit.copy(outfitName = newName)
                            )
                        }

                        outfitToRename = null
                    }
                ) {
                    Text("Lưu")
                }
            },

            dismissButton = {
                TextButton(onClick = { outfitToRename = null }) {
                    Text("Hủy")
                }
            }
        )
    }


    val deleteTarget = outfitToDelete

    deleteTarget?.let { outfit ->

        AlertDialog(
            onDismissRequest = { outfitToDelete = null },

            title = { Text("Xóa trang phục") },

            text = {
                Text("Bạn có chắc muốn xóa \"${outfit.outfitName}\"?")
            },

            confirmButton = {
                TextButton(
                    onClick = {

                        scope.launch(Dispatchers.IO) {
                            db.deleteOutfit(outfit)
                        }

                        outfitToDelete = null
                    }
                ) {
                    Text("Xóa")
                }
            },

            dismissButton = {
                TextButton(onClick = { outfitToDelete = null }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun OutfitCard(
    outfit: Outfit,
    onItemClick: (String) -> Unit,
    onSchemeSelected: (String) -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,

) {
    var previewColor by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            var expanded by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = outfit.outfitName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {

                        DropdownMenuItem(
                            text = { Text("Đổi tên") },
                            onClick = {
                                expanded = false
                                onRename()
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Xóa") },
                            onClick = {
                                expanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // -- HÀNG 1: CHỌN MÀU CHỦ ĐẠO --
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Màu chủ đạo:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            outfit.mainColor?.let { Color(it) } ?: MaterialTheme.colorScheme.surface
                        )
                        .border(1.dp, Color.Gray, CircleShape)
                        .clickable { onItemClick("main") },
                    contentAlignment = Alignment.Center
                ) {
                    if (outfit.mainColor == null) Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // -- HÀNG 2: CHỌN CÁCH PHỐI ĐỒ (Chỉ hiện khi đã có màu chủ đạo) --
            if (outfit.mainColor != null) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ColorHarmonyHelper.SCHEMES) { scheme ->
                        FilterChip(
                            selected = outfit.colorScheme == scheme,
                            onClick = { onSchemeSelected(scheme) },
                            label = { Text(scheme, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // -- HÀNG 3: CÁC MÓN QUẦN ÁO (Vẫn cho phép bấm chọn thủ công) --
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ===== HAT =====
                ClothingRow(
                    label = "Mũ / Phụ kiện",
                    icon = R.drawable.hat,
                    color = outfit.accessoryColor,
                    onPreview = { previewColor = it }
                ) {
                    onItemClick("acc")
                }

                Spacer(Modifier.height(12.dp))

                // ===== SHIRT =====
                ClothingRow(
                    label = "Áo",
                    icon = R.drawable.shirt,
                    color = outfit.topColor,
                    onPreview = { previewColor = it }
                ) {
                    onItemClick("top")
                }

                Spacer(Modifier.height(8.dp))

                // ===== JACKET (DỄ BẤM RIÊNG) =====
                ClothingRow(
                    label = "Áo khoác",
                    icon = R.drawable.jacket,
                    color = outfit.outerwearColor,
                    onPreview = { previewColor = it }
                ) {
                    onItemClick("outer")
                }

                Spacer(Modifier.height(12.dp))

                // ===== BOTTOM =====
                ClothingRow(
                    label = "Quần / Váy",
                    icon = R.drawable.pants,
                    color = outfit.bottomColor,
                    onPreview = { previewColor = it }
                ) {
                    onItemClick("bottom")
                }

                Spacer(Modifier.height(12.dp))

                // ===== SHOES =====
                ClothingRow(
                    label = "Giày",
                    icon = R.drawable.shoes,
                    color = outfit.shoesColor,
                    onPreview = { previewColor = it }
                ) {
                    onItemClick("shoes")
                }
            }

        }
        previewColor?.let { color ->

            Dialog(
                onDismissRequest = { previewColor = null }
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(color))
                        .clickable { previewColor = null }
                )
            }
        }


    }
}


@Composable
fun ClothingRow(
    label: String,
    icon: Int,
    color: Int?,
    modifier: Modifier = Modifier,
    onPreview: (Int) -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ===== ICON CENTER =====
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            ClothingSvg(
                icon = icon,
                color = color,
                modifier = Modifier.size(80.dp),
                onPreview = onPreview
            )

        }

        // ===== CLICKABLE TEXT =====
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable { onClick() },
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )


                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = "Chon bảng màu",
                    tint = MaterialTheme.colorScheme.primary
                )

            }
        }
    }
}
@Composable
fun ClothingSvg(
    @DrawableRes icon: Int,
    color: Int?,
    modifier: Modifier = Modifier,
    onPreview: ((Int) -> Unit)? = null
) {
    // Tính màu nền dựa trên màu của icon (nếu có)
    val backgroundColor = remember(color) {
        if (color == null) {
            // Nền xám nhạt CỐ ĐỊNH (Không dùng alpha)
            Color(0xFFEEEEEE)
        } else {
            val iconColor = Color(color)

            // Nếu icon sáng -> nền xám đen đậm. Nếu icon tối -> nền xám trắng
            if (iconColor.luminance() > 0.5) {
                Color(0xFF333333) // Đen xám tuyệt đối
            } else {
                Color(0xFFF5F5F5) // Trắng xám tuyệt đối
            }
        }
    }


    Box(
        modifier = modifier
            .clickable(
                enabled = color != null && onPreview != null
            ) {
                color?.let { onPreview?.invoke(it) }
            },
        contentAlignment = Alignment.Center
    ) {
        // Nền hình tròn phía sau Icon
        Box(
            modifier = Modifier
                .size(80.dp)               // kích thước nền (có thể điều chỉnh)
                .clip(CircleShape)         // bo tròn hoàn toàn
                .background(backgroundColor)
                .border(
                    1.dp,
                    Color.Black.copy(alpha = 0.05f),
                    CircleShape
                )
        )

        // Icon chính
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = color?.let { Color(it) } ?: Color(0xFF9E9E9E)
        )
    }
}

@Composable
fun ColorPickerDialog(
    colors: List<Int>,
    onColorSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var tempColor by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn màu") },
        text = {

            Column {

                // ===== TAB =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Bảng màu", "Tự chọn").forEachIndexed { index, title ->
                        TextButton(onClick = { selectedTab = index }) {
                            Text(title)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ===== PALETTE =====
                if (selectedTab == 0) {

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.heightIn(max = 320.dp)
                    ) {
                        items(colors) { colorInt ->
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(colorInt or 0xFF000000.toInt()))
                                    .border(
                                        2.dp,
                                        if (tempColor == colorInt) Color.Black else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        tempColor = colorInt
                                    }
                            )
                        }
                    }

                } else {

                    // ===== CUSTOM PICKER =====
                    CustomColorPicker(
                        onColorSelected = { color ->
                            tempColor = color
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // ===== PREVIEW =====
                tempColor?.let {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(it))
                    )
                }
            }
        },

        // ===== ACTIONS =====
        confirmButton = {
            TextButton(
                onClick = {
                    tempColor?.let { onColorSelected(it) }
                    onDismiss()
                },
                enabled = tempColor != null
            ) {
                Text("OK")
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun CustomColorPicker(
    onColorSelected: (Int) -> Unit
) {
    var hue by remember { mutableStateOf(0f) }
    var sat by remember { mutableStateOf(1f) }
    var value by remember { mutableStateOf(1f) }

    fun emit() {
        onColorSelected(Color.hsv(hue, sat, value).toArgb())
    }

    Column {

        // ===== PREVIEW =====
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Color.hsv(hue, sat, value))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(16.dp))

        // ================= HUE =================
        Text("Hue (0 - 360)")

        Row(verticalAlignment = Alignment.CenterVertically) {

            Slider(
                value = hue,
                onValueChange = {
                    hue = it
                    emit()
                },
                valueRange = 0f..360f,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            NumberField(
                value = hue.toInt(),
                range = 0..360,
                onValueChange = {
                    hue = it.toFloat()
                    emit()
                }
            )
        }

        // ================= SAT =================
        Text("Saturation (0 - 100%)")

        Row(verticalAlignment = Alignment.CenterVertically) {

            Slider(
                value = sat,
                onValueChange = {
                    sat = it
                    emit()
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            NumberField(
                value = (sat * 100).toInt(),
                range = 0..100,
                onValueChange = {
                    sat = it / 100f
                    emit()
                }
            )
        }

        // ================= VALUE =================
        Text("Brightness (0 - 100%)")

        Row(verticalAlignment = Alignment.CenterVertically) {

            Slider(
                value = value,
                onValueChange = {
                    value = it
                    emit()
                },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            NumberField(
                value = (value * 100).toInt(),
                range = 0..100,
                onValueChange = {
                    value = it / 100f
                    emit()
                }
            )
        }
    }
}

@Composable
fun NumberField(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input

            val parsed = input.toIntOrNull()
            if (parsed != null) {
                val clamped = parsed.coerceIn(range)
                onValueChange(clamped)
            }
        },
        singleLine = true,
        modifier = Modifier.width(70.dp)
    )
}
