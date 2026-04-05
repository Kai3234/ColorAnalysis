package com.example.coloranalysis.ui.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coloranalysis.data.AppDatabase
import com.example.coloranalysis.data.SeasonData
import com.example.coloranalysis.data.models.Profile
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigateToPhoto: (Int) -> Unit,
    navigateToResult: (Int) -> Unit,
    navigateToPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    // database
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context).profileDao() }
    val coroutineScope = rememberCoroutineScope()

    val profiles by db.getAllProfiles().collectAsState(initial = emptyList())

    var showDialog by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf("") }
    Scaffold (
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Color Analysis", fontWeight = FontWeight.Bold) },
                actions = {
                    // Nút Khám phá bảng màu nằm ở góc trên bên phải
                    IconButton(onClick = navigateToPreview) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Khám phá bảng màu",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                shape = MaterialTheme.shapes.medium,

                modifier = Modifier.padding(16.dp)

            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Thêm"
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) {
        innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (profiles.isEmpty()) {
                // Nếu không có gì
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Không có phân tích nào.",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(16.dp),
                    )
                    Text(
                        text = "Bấm + để bắt đầu.",
                        fontSize = 24.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                // Danh sách hồ sơ
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(profiles) { profile ->
                        val isInvalidSeason =
                            profile.seasonType.isNullOrBlank()

                        ProfileCard(
                            profile = profile,
                            onDeleteClick = {
                                coroutineScope.launch {
                                    db.deleteProfile(profile)
                                }
                            },
                            onCardClick = {
                                if (!isInvalidSeason) {
                                    navigateToResult(profile.id)
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Profile không có kết quả phân tích hoặc lỗi. Vui lòng xóa.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    // Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false

                        coroutineScope.launch {
                            val newProfile = Profile(
                                profileName = profileName,
                                dateCreated = System.currentTimeMillis() // Saves current time
                            )
                            val newProfileId = db.insertProfile(newProfile).toInt()

                            profileName = "" // Reset name for next time
                            navigateToPhoto(newProfileId)
                        }
                    },
                    enabled = profileName.isNotBlank()
                ) {
                    Text("Tiếp theo")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDialog = false }
                ) {
                    Text("Đóng")
                }
            },
            title = { Text("Tên hồ sơ phân tích ") },
            text = {
                TextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    textStyle = TextStyle(fontSize = 24.sp),
                )
            }
        )
    }
}

@Composable
fun ProfileCard(
    profile: Profile,
    onDeleteClick: () -> Unit,
    onCardClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(profile.dateCreated))

    // 1. Lấy màu nền, nếu null dùng surfaceContainer thay vì surface
    val backgroundColor = SeasonData.palettes[profile.seasonType ?: ""]?.uiColor
        ?: MaterialTheme.colorScheme.surfaceContainerHigh

    // 2. Tự động tính toán màu chữ dựa trên độ sáng của nền (Đảm bảo luôn đọc được)
    val contentColor = if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White

    // Màu phụ cho ngày tháng (giảm độ đậm một chút)
    val secondaryContentColor = contentColor.copy(alpha = 0.7f)

    Card(
        onClick = onCardClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor // Thiết lập mặc định cho các component con
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.profileName,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                        color = contentColor // Áp dụng màu tương phản
                    )

                    Text(
                        text = profile.seasonType ?: "Unknown",
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor // Áp dụng màu tương phản
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryContentColor // Màu phụ rõ trên nền tương ứng
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Xóa",
                    tint = contentColor // Icon cũng tự đổi màu theo nền
                )
            }
        }
    }
}