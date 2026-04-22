package com.example.coloranalysis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.coloranalysis.data.helper.PaletteHelper

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SeasonPaletteDisplay(
    seasonName: String?,
    initialPersonalities: List<String> = emptyList(),
    initialLifestyles: List<String> = emptyList(),
    onFilterChanged: (List<String>, List<String>) -> Unit = { _, _ -> },
    onColorClick: (Int) -> Unit
) {
    val context = LocalContext.current
    var selectedPers by remember { mutableStateOf(initialPersonalities) }
    var selectedLife by remember { mutableStateOf(initialLifestyles) }

    // Load dữ liệu gốc
    val fileName = PaletteHelper.getFileName(seasonName)
    val allColors = remember(fileName) {
        if (fileName != null) PaletteHelper.loadPalette(context, fileName) else emptyList()
    }

    // Lọc dữ liệu dựa trên state
    val filteredColors = remember(allColors, selectedPers, selectedLife) {
        allColors.filter { color ->
            val matchPers = selectedPers.isEmpty() || color.personality.any { it in selectedPers }
            val matchLife = selectedLife.isEmpty() || color.lifestyle.any { it in selectedLife }
            matchPers && matchLife
        }
    }

    // Load và lọc dữ liệu màu nên tránh
    val avoidColors = remember(seasonName) {
        val allAvoid = PaletteHelper.loadAvoidColors(context)
        if (seasonName != null) {
            // Chỉ lấy những màu mà trong danh sách subseason của nó có chứa tên mùa hiện tại
            allAvoid.filter { it.subseason.contains(seasonName) }
        } else {
            emptyList()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // --- PHẦN LỌC ---
        Text("Tùy chỉnh theo cá nhân", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        Text("Tính cách:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            PaletteHelper.personalityMap.forEach { (en, vi) ->
                FilterChip(
                    selected = selectedPers.contains(en),
                    onClick = {
                        selectedPers = if (selectedPers.contains(en)) selectedPers - en else selectedPers + en
                        onFilterChanged(selectedPers, selectedLife)
                    },
                    label = { Text(vi) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Text("Lối sống:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
        FlowRow(modifier = Modifier.fillMaxWidth()) {
            PaletteHelper.lifestyleMap.forEach { (en, vi) ->
                FilterChip(
                    selected = selectedLife.contains(en),
                    onClick = {
                        selectedLife = if (selectedLife.contains(en)) selectedLife - en else selectedLife + en
                        onFilterChanged(selectedPers, selectedLife)
                    },
                    label = { Text(vi) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- HIỂN THỊ MÀU ---
        Text(
            text = "Bảng màu phù hợp (${filteredColors.size})",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            filteredColors.forEach { colorItem ->
                // Fix an toàn mã Hex (tránh crash nếu thiếu dấu #)
                val safeHex = if (colorItem.hex.startsWith("#")) colorItem.hex else "#${colorItem.hex}"
                val parsedColorInt = try { android.graphics.Color.parseColor(safeHex) } catch (e: Exception) { android.graphics.Color.GRAY }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(Color(parsedColorInt), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable { onColorClick(parsedColorInt) }
                    )
                    Text(
                        text = colorItem.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(70.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // --- THÊM MỚI: HIỂN THỊ MÀU NÊN TRÁNH ---
        if (avoidColors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Màu nên tránh (${avoidColors.size})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error // Dùng màu đỏ để cảnh báo
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                avoidColors.forEach { avoidItem ->
                    // Fix an toàn mã Hex
                    val safeHex = if (avoidItem.hex.startsWith("#")) avoidItem.hex else "#${avoidItem.hex}"
                    val parsedColorInt = try { android.graphics.Color.parseColor(safeHex) } catch (e: Exception) { android.graphics.Color.GRAY }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .background(Color(parsedColorInt), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                // Bấm vào vẫn bật xem fullscreen được bình thường
                                .clickable { onColorClick(parsedColorInt) }
                        )
                        Text(
                            text = avoidItem.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.width(70.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}