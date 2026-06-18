package com.example.dacs3.ui.main

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dacs3.ui.theme.*
import java.io.File

@Immutable
data class SoundOption(val name: String, val uri: String?)

@Composable
fun AccountScreen(
    userName: String = "User Name",
    userEmail: String = "user@example.com",
    userAvatarUrl: String? = null,
    userAvatarInitial: String = "U",
    totalFocusHours: String = "0",
    globalRank: String = "#0",
    currentStreak: String = "0",
    isUploadingAvatar: Boolean = false,
    isDarkMode: Boolean = false,
    isNotificationSoundEnabled: Boolean = true,
    selectedSoundUri: String? = null,
    onDarkModeToggle: (Boolean) -> Unit = {},
    onNotificationSoundToggle: (Boolean) -> Unit = {},
    onSoundSelected: (String?) -> Unit = {},
    onPomodoroSettingsClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onAvatarSelected: (File) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var showSoundPicker by remember { mutableStateOf(false) }

    // Lấy danh sách nhạc chuông hệ thống
    val systemSounds = remember {
        val manager = RingtoneManager(context)
        manager.setType(RingtoneManager.TYPE_NOTIFICATION)
        val cursor = manager.cursor
        val list = mutableListOf<SoundOption>()
        list.add(SoundOption("Default", null))
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = manager.getRingtoneUri(cursor.position).toString()
            list.add(SoundOption(title, uri))
        }
        list
    }

    val selectedSoundName = systemSounds.find { it.uri == selectedSoundUri }?.name ?: "Default"

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val file = uri.toTempImageFile(context)
                onAvatarSelected(file)
            } catch (e: Exception) {
                Log.e("AccountScreen", "Error creating temp file", e)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // 1. Profile Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Main Avatar Area
                Box(modifier = Modifier.size(100.dp)) {
                    UserAvatar(
                        imageUrl = userAvatarUrl,
                        initial = userAvatarInitial,
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Loading Overlay
                    if (isUploadingAvatar) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // Camera Edit Button
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .clickable(enabled = !isUploadingAvatar) {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    shadowElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Change Avatar",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = userName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = userEmail,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onEditProfile,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
                elevation = null
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // 2. Stats Dashboard
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccountStatCard(
                modifier = Modifier.weight(1f),
                title = "Focus Hours",
                value = totalFocusHours,
                icon = Icons.Outlined.Timer,
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                tint = MaterialTheme.colorScheme.primary,
                textColor = MaterialTheme.colorScheme.onBackground
            )
            AccountStatCard(
                modifier = Modifier.weight(1f),
                title = "Global Rank",
                value = globalRank,
                icon = Icons.Filled.EmojiEvents,
                containerColor = RankGoldBg,
                tint = RankGold,
                textColor = MaterialTheme.colorScheme.onBackground
            )
            AccountStatCard(
                modifier = Modifier.weight(1f),
                title = "Streak",
                value = "$currentStreak Days",
                icon = Icons.Filled.LocalFireDepartment,
                containerColor = StreakOrangeBg,
                tint = StreakOrange,
                textColor = StreakOrange
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 3. Preferences
        SectionTitle("PREFERENCES")
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Notification Sound Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Notification Sounds", modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = isNotificationSoundEnabled,
                        onCheckedChange = onNotificationSoundToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                
                // Sound Selection Item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isNotificationSoundEnabled) { showSoundPicker = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Sound Type", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (isNotificationSoundEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                        Text(selectedSoundName, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                
                // Dark Mode Toggle Item
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Dark Mode", modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onDarkModeToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                AccountActionItem(
                    icon = Icons.Outlined.Settings,
                    title = "Pomodoro Settings",
                    value = "25m / 5m",
                    onClick = onPomodoroSettingsClick
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Support & Account
        SectionTitle("SUPPORT & ACCOUNT")
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                AccountActionItem(Icons.Outlined.HelpOutline, "Help Center", "")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                AccountActionItem(Icons.Outlined.Info, "About Us", "")
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                LogoutItem(onLogout)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }

    if (showSoundPicker) {
        AlertDialog(
            onDismissRequest = { showSoundPicker = false },
            title = { Text("Select Notification Sound") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(systemSounds) { sound ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSoundSelected(sound.uri)
                                    // Không đóng picker ngay để user nghe thử, 
                                    // hoặc có thể đóng tùy UX bạn muốn. 
                                    // Ở đây mình đóng luôn cho gọn.
                                    showSoundPicker = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = sound.uri == selectedSoundUri,
                                onClick = null // Clickable Row handles this
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(sound.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSoundPicker = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun Uri.toTempImageFile(context: Context): File {
    val inputStream = context.contentResolver.openInputStream(this) 
        ?: throw IllegalStateException("Không thể đọc ảnh từ nguồn này. Hãy thử tải ảnh về máy trước.")
    val tempFile = File.createTempFile("avatar_${System.currentTimeMillis()}_", ".jpg", context.cacheDir)
    inputStream.use { input ->
        tempFile.outputStream().use { output -> input.copyTo(output) }
    }
    return tempFile
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp),
        letterSpacing = 0.5.sp
    )
}

@Composable
fun AccountStatCard(
    modifier: Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    tint: Color,
    textColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.height(105.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
            Column {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AccountActionItem(icon: ImageVector, title: String, value: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        if (value.isNotEmpty()) {
            Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 8.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun LogoutItem(onLogout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLogout() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color.Red.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Logout, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            "Log Out",
            modifier = Modifier.weight(1f),
            fontSize = 15.sp, fontWeight = FontWeight.Bold,
            color = Color.Red
        )
    }
}
