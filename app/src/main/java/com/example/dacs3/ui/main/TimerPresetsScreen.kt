package com.example.dacs3.ui.main

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import com.example.dacs3.model.TimerPreset

fun String.toComposeColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color(0xFF3F51B5)
    }
}

val PresetTextGray = Color(0xFF6B7280)
val PresetLightGrayBg = Color(0xFFF3F4F6)

@Composable
fun TimerPresetsScreen(
    presets: List<TimerPreset>,
    onBackClick: () -> Unit,
    onDelete: (Long) -> Unit,
    onActivate: (TimerPreset) -> Unit,
    onSavePreset: (TimerPreset) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var editingPreset by remember { mutableStateOf<TimerPreset?>(null) }
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = BgLight,
        bottomBar = { BottomNavigationBar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppNavy)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mindful Flow", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppNavy)
                }
                Surface(color = AppNavy, shape = CircleShape, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Person, contentDescription = "User Avatar", tint = Color.White, modifier = Modifier.padding(6.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Timer Presets", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AppNavy)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Design your sanctuary. Create custom focus and break durations.", fontSize = 14.sp, color = PresetTextGray)

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    editingPreset = null
                    showDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppNavy)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Preset", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (presets.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    Text("Chưa có Preset nào, bấm nút trên để tạo!", color = Color.Gray)
                }
            } else {
                presets.forEach { preset ->
                    PresetCard(
                        preset = preset,
                        onActivateClick = { onActivate(preset) },
                        onEditClick = {
                            editingPreset = preset
                            showDialog = true
                        },
                        onDeleteClick = { onDelete(preset.id) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showDialog) {
        var name by remember(editingPreset) { mutableStateOf(editingPreset?.title ?: "") }
        var fMin by remember(editingPreset) { mutableStateOf(editingPreset?.focusMin?.toString() ?: "25") }
        var bMin by remember(editingPreset) { mutableStateOf(editingPreset?.breakMin?.toString() ?: "5") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = if (editingPreset == null) "Create New Preset" else "Edit Preset",
                    fontWeight = FontWeight.Bold,
                    color = AppNavy
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Preset Name (e.g. Reading)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fMin,
                        onValueChange = { fMin = it },
                        label = { Text("Focus Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bMin,
                        onValueChange = { bMin = it },
                        label = { Text("Break Minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newPreset = TimerPreset(
                            id = editingPreset?.id ?: System.currentTimeMillis(),
                            title = name.ifEmpty { "Custom Focus" },
                            focusMin = fMin.toIntOrNull() ?: 25,
                            breakMin = bMin.toIntOrNull() ?: 5,
                            colorHex = editingPreset?.colorHex ?: "#3F51B5"
                        )
                        onSavePreset(newPreset)
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppNavy)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = AppNavy)
                }
            }
        )
    }
}

@Composable
fun PresetCard(
    preset: TimerPreset,
    onActivateClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val themeColor = preset.colorHex.toComposeColor()

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = themeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = themeColor, modifier = Modifier.padding(12.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Outlined.Edit, "Edit", tint = AppNavy, modifier = Modifier.size(20.dp).clickable { onEditClick() })
                        Icon(Icons.Outlined.Delete, "Delete", tint = AppNavy, modifier = Modifier.size(20.dp).clickable { onDeleteClick() })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(preset.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppNavy)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("FOCUS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PresetTextGray)
                    Surface(color = PresetLightGrayBg, shape = RoundedCornerShape(8.dp)) {
                        Text(String.format("%02d:00", preset.focusMin), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppNavy, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("BREAK", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PresetTextGray)
                    Surface(color = PresetLightGrayBg, shape = RoundedCornerShape(8.dp)) {
                        Text(String.format("%02d:00", preset.breakMin), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = themeColor, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onActivateClick,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PresetLightGrayBg)
                ) {
                    Text("Activate Flow", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AppNavy)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(themeColor))
        }
    }
}