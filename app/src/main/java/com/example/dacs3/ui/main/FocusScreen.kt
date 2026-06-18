package com.example.dacs3.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dacs3.viewmodel.FocusViewModel
import com.example.dacs3.ui.theme.*
import com.example.dacs3.ui.profile.EditProfileScreen
import com.example.dacs3.service.TimerService
import com.example.dacs3.viewmodel.TimerManager
import kotlinx.coroutines.launch

@Composable
fun FocusScreen(
    userId: String?,
    activeLiveUsers: Int = 1205,
    isDarkMode: Boolean = false,
    onDarkModeToggle: (Boolean) -> Unit = {},
    viewModel: FocusViewModel = viewModel(
        key = userId ?: "guest"
    ),
    onLogout: () -> Unit = {}
) {
    var currentTab by remember { mutableStateOf("FOCUS") }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var showPresetsScreen by remember { mutableStateOf(false) }
    var showEditProfile by remember { mutableStateOf(false) }
    var showExitWarning by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // --- LIÊN KẾT TRẠNG THÁI CHẠY NGẦM CHO NÚT BACK ---
    val isServiceRunning by TimerManager.isRunning.collectAsState()

    // Xử lý nút quay lại khi đồng hồ đang chạy ngầm
    BackHandler(enabled = isServiceRunning) {
        showExitWarning = true
    }

    if (showExitWarning) {
        AlertDialog(
            onDismissRequest = { showExitWarning = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cảnh báo thoát")
                }
            },
            text = { Text("Đồng hồ đếm ngược vẫn đang chạy. Bạn có chắc chắn muốn thoát ứng dụng không? Tiến trình hiện tại sẽ bị dừng.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitWarning = false
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Thoát", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitWarning = false }) {
                    Text("Tiếp tục tập trung", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    UserAvatar(
                        imageUrl = viewModel.userAvatarUrl,
                        initial = viewModel.userAvatarInitial,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = viewModel.userName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = viewModel.userTitle, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "${viewModel.totalDeepWorkHours}H DEEP WORK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(32.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    label = { Text("Focus Timer") },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; showPresetsScreen = true },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                DrawerMenuItem(Icons.Default.Settings, "Settings")
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.Red) },
                    label = { Text("Log Out", color = Color.Red) },
                    selected = false,
                    onClick = onLogout,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    ) {
        if (showPresetsScreen) {
            TimerPresetsScreen(
                presets = viewModel.presetsList,
                onBackClick = { showPresetsScreen = false },
                onDelete = { viewModel.deletePreset(it) },
                onActivate = { preset ->
                    // --- ĐỒNG BỘ DỮ LIỆU SANG TIMER MANAGER ---
                    TimerManager.currentPresetTitle.value = preset.title
                    TimerManager.currentFocusMin.value = preset.focusMin
                    TimerManager.currentBreakMin.value = preset.breakMin
                    TimerManager.isFocusMode.value = true
                    TimerManager.totalFocusSeconds.value = preset.focusMin * 60
                    TimerManager.timeLeft.value = preset.focusMin * 60

                    if (isServiceRunning) {
                        val intent = Intent(context, TimerService::class.java).apply { action = "TOGGLE" }
                        context.startService(intent)
                    }

                    viewModel.activatePreset(preset)
                    showPresetsScreen = false
                },
                onSavePreset = { viewModel.savePreset(it) }
            )
        } else if (showEditProfile) {
            EditProfileScreen(
                onBack = { showEditProfile = false }
            )
        } else {
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                    when (currentTab) {
                        "FOCUS" -> FocusContent(
                            userId = userId,
                            todayFocusTime = viewModel.totalDeepWorkFormatted,
                            activeLiveUsers = activeLiveUsers,
                            viewModel = viewModel,
                            onMenuClick = { scope.launch { drawerState.open() } }
                        )
                        "CHAT" -> ChatScreen(
                            userId = userId,
                            onMenuClick = { scope.launch { drawerState.open() } }
                        )
                        "RANK" -> LeaderboardScreen(
                            userName = viewModel.userName,
                            userAvatarUrl = viewModel.userAvatarUrl,
                            userAvatarInitial = viewModel.userAvatarInitial,
                            onMenuClick = { scope.launch { drawerState.open() } }
                        )
                        "ACCOUNT" -> AccountScreen(
                            userName = viewModel.userName,
                            userAvatarUrl = viewModel.userAvatarUrl,
                            userAvatarInitial = viewModel.userAvatarInitial,
                            userEmail = viewModel.userEmail,
                            totalFocusHours = viewModel.totalDeepWorkHours.toString(),
                            globalRank = "#${viewModel.globalRank}",
                            currentStreak = viewModel.currentStreak.toString(),
                            isUploadingAvatar = viewModel.isUploadingAvatar,
                            isDarkMode = isDarkMode,
                            isNotificationSoundEnabled = viewModel.isNotificationSoundEnabled,
                            selectedSoundUri = viewModel.selectedSoundUri,
                            onDarkModeToggle = onDarkModeToggle,
                            onNotificationSoundToggle = { viewModel.toggleNotificationSound(it) },
                            onSoundSelected = { viewModel.setSelectedSound(it) },
                            onPomodoroSettingsClick = { showPresetsScreen = true },
                            onEditProfile = { showEditProfile = true },
                            onLogout = onLogout,
                            onAvatarSelected = { file -> viewModel.updateAvatar(file) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CenterText(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FocusContent(
    userId: String?,
    todayFocusTime: String,
    activeLiveUsers: Int,
    viewModel: FocusViewModel,
    onMenuClick: () -> Unit
) {
    var showTimeDialog by remember { mutableStateOf(false) }
    var inputMinutes by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    // --- LIÊN KẾT GIAO DIỆN VỚI SERVICE CHẠY NGẦM THÔNG QUA TIMER MANAGER ---
    val timeLeft by TimerManager.timeLeft.collectAsState()
    val totalFocusSeconds by TimerManager.totalFocusSeconds.collectAsState()
    val isRunning by TimerManager.isRunning.collectAsState()
    val isFocusMode by TimerManager.isFocusMode.collectAsState()
    val currentPresetTitle by TimerManager.currentPresetTitle.collectAsState()

    fun sendServiceCommand(actionStr: String) {
        val intent = Intent(context, TimerService::class.java).apply { action = actionStr }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && actionStr == "TOGGLE" && !isRunning) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(text = "Mindful Flow", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            UserAvatar(
                imageUrl = viewModel.userAvatarUrl,
                initial = viewModel.userAvatarInitial,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Surface(color = if (isFocusMode) GreenPill else Color(0xFFE3F2FD), shape = RoundedCornerShape(16.dp)) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (isFocusMode) GreenText else PrimaryBlue))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (isFocusMode) "FOCUS SESSION" else "BREAK TIME", color = if (isFocusMode) GreenText else PrimaryBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(text = if (isFocusMode) currentPresetTitle else "Rest & Recharge", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(40.dp))
        Box(modifier = Modifier.clickable { showTimeDialog = true }) {
            CircularTimerDisplay(timeLeft = timeLeft, totalTime = totalFocusSeconds)
        }
        Spacer(modifier = Modifier.height(40.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape, modifier = Modifier.size(56.dp)) {
                IconButton(onClick = {
                    if (isRunning) sendServiceCommand("TOGGLE")
                    TimerManager.timeLeft.value = totalFocusSeconds
                    viewModel.restartTimer()
                }) { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(24.dp), modifier = Modifier.size(80.dp)) {
                IconButton(onClick = { sendServiceCommand("TOGGLE") }) {
                    Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape, modifier = Modifier.size(56.dp)) {
                IconButton(onClick = { sendServiceCommand("SKIP") }) { Icon(Icons.Default.SkipNext, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(Modifier.weight(1f), "TODAY", todayFocusTime, "Deep Work", Icons.Default.BarChart, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
            StatCard(Modifier.weight(1f), "LIVE", "+%,d".format(activeLiveUsers), "Flowing", Icons.Default.People, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary)
        }
        viewModel.errorMessage?.let { Text(it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 16.dp)) }
        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showTimeDialog) {
        AlertDialog(
            onDismissRequest = { showTimeDialog = false },
            title = { Text("Set Focus Timer") },
            text = { OutlinedTextField(value = inputMinutes, onValueChange = { inputMinutes = it }, label = { Text("Minutes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) },
            confirmButton = {
                Button(onClick = {
                    val mins = inputMinutes.toIntOrNull() ?: 25
                    TimerManager.currentPresetTitle.value = "Custom Timer"
                    TimerManager.currentFocusMin.value = mins
                    TimerManager.currentBreakMin.value = 5
                    TimerManager.isFocusMode.value = true
                    TimerManager.totalFocusSeconds.value = mins * 60
                    TimerManager.timeLeft.value = mins * 60

                    if (isRunning) sendServiceCommand("TOGGLE")

                    viewModel.createCustomTimer(mins)
                    showTimeDialog = false
                }) { Text("Set") }
            }
        )
    }
}

@Composable
fun DrawerMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    NavigationDrawerItem(icon = { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }, label = { Text(text) }, selected = false, onClick = {}, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
fun CircularTimerDisplay(timeLeft: Int, totalTime: Int) {
    val progress = if (totalTime > 0) timeLeft.toFloat() / totalTime.toFloat() else 0f
    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawArc(trackColor, 0f, 360f, false, style = Stroke(14.dp.toPx(), cap = StrokeCap.Round))
            drawArc(progressColor, -90f, 360f * progress, false, style = Stroke(14.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(String.format("%02d:%02d", minutes, seconds), fontSize = 56.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            Text("REMAINING", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 2.sp)
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, title: String, value: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, bgColor: Color, textColor: Color) {
    Surface(color = bgColor, shape = RoundedCornerShape(16.dp), shadowElevation = 2.dp, modifier = modifier.height(110.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(icon, null, tint = textColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                Text(title, color = textColor.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Column {
                Text(value, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = textColor.copy(alpha = 0.7f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun BottomNavigationBar(currentTab: String, onTabSelected: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp) {
        val items = listOf(
            Triple("FOCUS", Icons.Default.Timer, "FOCUS"),
            Triple("CHAT", Icons.Default.Chat, "CHAT"),
            Triple("RANK", Icons.Default.Leaderboard, "RANK"),
            Triple("ACCOUNT", Icons.Default.Person, "ACCOUNT")
        )
        items.forEach { (tab, icon, label) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal) },
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = MaterialTheme.colorScheme.primary, selectedTextColor = MaterialTheme.colorScheme.primary, indicatorColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    }
}