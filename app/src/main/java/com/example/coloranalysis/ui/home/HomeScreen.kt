package com.example.coloranalysis.ui.home

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    // States cho các Dialog
    var showAddDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }

    var profileToRename by remember { mutableStateOf<Profile?>(null) }
    var renameValue by remember { mutableStateOf("") }

    var profileToDelete by remember { mutableStateOf<Profile?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Color Analysis", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = navigateToPreview) {
                        Icon(Icons.Default.Palette, "Khám phá bảng màu", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, "Thêm")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            if (profiles.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().align(Alignment.Center),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Không có phân tích nào.", fontSize = 24.sp, modifier = Modifier.padding(16.dp))
                    Text("Bấm + để bắt đầu.", fontSize = 24.sp, modifier = Modifier.padding(16.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
                ) {
                    // Sắp xếp đảm bảo thời gian mới nhất lên đầu
                    items(profiles.sortedByDescending { it.dateCreated }) { profile ->
                        val isInvalidSeason = profile.seasonType.isNullOrBlank()

                        ProfileCard(
                            profile = profile,
                            onCardClick = {
                                if (!isInvalidSeason) navigateToResult(profile.id)
                                else Toast.makeText(context, "Profile lỗi. Vui lòng xóa.", Toast.LENGTH_SHORT).show()
                            },
                            onRenameClick = {
                                profileToRename = profile
                                renameValue = profile.profileName
                            },
                            onMoveToTopClick = {
                                // Mẹo thay đổi thứ tự: Cập nhật thời gian thành hiện tại để nó nổi lên đầu
                                coroutineScope.launch(Dispatchers.IO) {
                                    db.updateProfile(profile.copy(dateCreated = System.currentTimeMillis()))
                                }
                            },
                            onDeleteClick = {
                                profileToDelete = profile
                            }
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG THÊM MỚI ---
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Tên hồ sơ phân tích") },
            text = {
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    textStyle = TextStyle(fontSize = 20.sp),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAddDialog = false
                        coroutineScope.launch(Dispatchers.IO) {
                            val newProfile = Profile(profileName = newProfileName, dateCreated = System.currentTimeMillis())
                            val newId = db.insertProfile(newProfile).toInt()
                            withContext(Dispatchers.Main) {
                                newProfileName = ""
                                navigateToPhoto(newId)
                            }
                        }
                    },
                    enabled = newProfileName.isNotBlank()
                ) { Text("Tiếp theo") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Đóng") } }
        )
    }

    // --- DIALOG ĐỔI TÊN ---
    if (profileToRename != null) {
        AlertDialog(
            onDismissRequest = { profileToRename = null },
            title = { Text("Đổi tên hồ sơ") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            db.updateProfile(profileToRename!!.copy(profileName = renameValue))
                            withContext(Dispatchers.Main) { profileToRename = null }
                        }
                    },
                    enabled = renameValue.isNotBlank()
                ) { Text("Lưu") }
            },
            dismissButton = { TextButton(onClick = { profileToRename = null }) { Text("Hủy") } }
        )
    }

    // --- DIALOG XÁC NHẬN XÓA ---
    if (profileToDelete != null) {
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text("Xóa hồ sơ") },
            text = { Text("Bạn có chắc chắn muốn xóa hồ sơ '${profileToDelete!!.profileName}' không? Dữ liệu tủ đồ liên quan cũng sẽ bị xóa.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            db.deleteProfile(profileToDelete!!)
                            withContext(Dispatchers.Main) { profileToDelete = null }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { profileToDelete = null }) { Text("Hủy") } }
        )
    }
}

// ==========================================
// THẺ PROFILE
// ==========================================
@Composable
fun ProfileCard(
    profile: Profile,
    onCardClick: () -> Unit,
    onRenameClick: () -> Unit,
    onMoveToTopClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(profile.dateCreated))

    // 1. Tính toán màu sắc
    val backgroundColor = SeasonData.palettes[profile.seasonType ?: ""]?.uiColor
        ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White
    val secondaryContentColor = contentColor.copy(alpha = 0.7f)

    var expanded by remember { mutableStateOf(false) }

    Card(
        onClick = onCardClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.profileName,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                        color = contentColor
                    )
                    Text(
                        text = profile.seasonType ?: "Chưa phân tích",
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryContentColor
                )
            }

            // MENU 3 CHẤM
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, "Tùy chọn", tint = contentColor)
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Đổi tên") },
                        onClick = { expanded = false; onRenameClick() }
                    )
                    DropdownMenuItem(
                        text = { Text("Đặt lên đầu") },
                        onClick = { expanded = false; onMoveToTopClick() }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Xóa", color = MaterialTheme.colorScheme.error) },
                        onClick = { expanded = false; onDeleteClick() }
                    )
                }
            }
        }
    }
}