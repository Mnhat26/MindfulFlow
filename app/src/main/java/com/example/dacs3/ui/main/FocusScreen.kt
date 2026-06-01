package com.example.dacs3.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dacs3.viewmodel.FocusViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

val AppNavy = Color(0xFF0A0E2F)
val BgLight = Color(0xFFFAFAFC)
val PrimaryBlue = Color(0xFF1E3A8A)
val TrackGray = Color(0xFFE5E7EB)
val GreenPill = Color(0xFFE8F5E9)
val GreenText = Color(0xFF2E7D32)

@Composable
fun FocusScreen(
    userId: String?,
    todayFocusTime: String = "00:00",
    activeLiveUsers: Int = 1205,
    viewModel: FocusViewModel = viewModel(
        key = userId ?: "guest"
    ),
    onLogout: () -> Unit = {}
) {
    val auth = FirebaseAuth.getInstance()

    var showPresetsScreen by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }
    var inputMinutes by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scrollState = rememberScrollState()

    if (showPresetsScreen) {
        TimerPresetsScreen(
            presets = viewModel.presetsList,
            onBackClick = {
                showPresetsScreen = false
            },
            onDelete = { id ->
                viewModel.deletePreset(id)
            },
            onActivate = { preset ->
                viewModel.activatePreset(preset)
                showPresetsScreen = false
            },
            onSavePreset = { preset ->
                viewModel.savePreset(preset)
            }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = true,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color.White,
                    modifier = Modifier.width(300.dp)
                ) {
                    Spacer(modifier = Modifier.height(48.dp))

                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFE57373),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = viewModel.userName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppNavy
                        )

                        Text(
                            text = viewModel.userTitle,
                            fontSize = 14.sp,
                            color = AppNavy.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${viewModel.totalDeepWorkHours}H DEEP WORK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = AppNavy
                            )
                        },
                        label = {
                            Text(
                                text = "Focus Timer",
                                fontSize = 16.sp,
                                color = AppNavy
                            )
                        },
                        selected = false,
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = Color(0xFFE8EAF6)
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = {
                            scope.launch {
                                drawerState.close()
                            }
                            showPresetsScreen = true
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        color = Color.LightGray.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DrawerMenuItem(
                        icon = Icons.Default.Settings,
                        text = "Settings"
                    )

                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = null,
                                tint = Color.Red
                            )
                        },
                        label = {
                            Text(
                                text = "Log Out",
                                color = Color.Red
                            )
                        },
                        selected = false,
                        onClick = {
                            onLogout()
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        ) {
            Scaffold(
                containerColor = BgLight,
                bottomBar = {
                    BottomNavigationBar()
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        drawerState.open()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "Menu",
                                    tint = AppNavy
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            Text(
                                text = "Mindful Flow",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppNavy
                            )
                        }

                        Surface(
                            color = AppNavy,
                            shape = CircleShape,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User Avatar",
                                tint = Color.White,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = if (viewModel.isFocusMode) GreenPill else Color(0xFFE3F2FD),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (viewModel.isFocusMode) GreenText else PrimaryBlue
                                    )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Text(
                                text = if (viewModel.isFocusMode) "FOCUS SESSION" else "BREAK TIME",
                                color = if (viewModel.isFocusMode) GreenText else PrimaryBlue,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (viewModel.isFocusMode) {
                            viewModel.currentPreset?.title ?: "Focus Timer"
                        } else {
                            "Rest & Recharge"
                        },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppNavy
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    CircularTimerDisplay(
                        timeLeft = viewModel.timeLeft,
                        totalTime = viewModel.totalFocusSeconds
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Surface(
                            color = TrackGray,
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.restartTimer()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Restart",
                                    tint = AppNavy
                                )
                            }
                        }

                        Surface(
                            color = AppNavy,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.size(80.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.toggleTimer()
                                }
                            ) {
                                Icon(
                                    imageVector = if (viewModel.isRunning) {
                                        Icons.Default.Pause
                                    } else {
                                        Icons.Default.PlayArrow
                                    },
                                    contentDescription = "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Surface(
                            color = TrackGray,
                            shape = CircleShape,
                            modifier = Modifier.size(56.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.skipTimer()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Skip",
                                    tint = AppNavy
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "TODAY",
                            value = todayFocusTime,
                            subtitle = "Deep Work session",
                            icon = Icons.Default.BarChart,
                            bgColor = Color.White,
                            textColor = AppNavy
                        )

                        StatCard(
                            modifier = Modifier.weight(1f),
                            title = "LIVE",
                            value = "+%,d".format(activeLiveUsers),
                            subtitle = "Flowing right now",
                            icon = Icons.Default.People,
                            bgColor = AppNavy,
                            textColor = Color.White
                        )
                    }

                    viewModel.errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = message,
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        if (showTimeDialog) {
            AlertDialog(
                onDismissRequest = {
                    showTimeDialog = false
                },
                title = {
                    Text(
                        text = "Set Focus Timer",
                        fontWeight = FontWeight.Bold,
                        color = AppNavy
                    )
                },
                text = {
                    OutlinedTextField(
                        value = inputMinutes,
                        onValueChange = {
                            inputMinutes = it
                        },
                        label = {
                            Text("Enter minutes (e.g. 25)")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val minutes = inputMinutes.toIntOrNull() ?: 25

                            viewModel.createCustomTimer(minutes)

                            showTimeDialog = false
                            inputMinutes = ""
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppNavy
                        )
                    ) {
                        Text(
                            text = "Set Time",
                            color = Color.White
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showTimeDialog = false
                        }
                    ) {
                        Text(
                            text = "Cancel",
                            color = AppNavy
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun DrawerMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    NavigationDrawerItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Gray
            )
        },
        label = {
            Text(
                text = text,
                fontSize = 16.sp,
                color = AppNavy.copy(alpha = 0.8f)
            )
        },
        selected = false,
        modifier = Modifier.padding(horizontal = 16.dp),
        onClick = {}
    )
}

@Composable
fun CircularTimerDisplay(
    timeLeft: Int,
    totalTime: Int
) {
    val progress = if (totalTime > 0) {
        timeLeft.toFloat() / totalTime.toFloat()
    } else {
        0f
    }

    val minutes = timeLeft / 60
    val seconds = timeLeft % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(240.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()

            drawArc(
                color = TrackGray,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )

            drawArc(
                color = PrimaryBlue,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = timeString,
                fontSize = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AppNavy
            )

            Text(
                text = "REMAINING",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier,
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bgColor: Color,
    textColor: Color
) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = modifier.height(110.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = title,
                    color = textColor.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column {
                Text(
                    text = value,
                    color = textColor,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = subtitle,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar() {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Focus"
                )
            },
            label = {
                Text(
                    text = "FOCUS",
                    fontWeight = FontWeight.Bold
                )
            },
            selected = true,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AppNavy,
                selectedTextColor = AppNavy,
                indicatorColor = Color(0xFFE8EAF6)
            ),
            onClick = {}
        )

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Chat"
                )
            },
            label = {
                Text("CHAT")
            },
            selected = false,
            onClick = {}
        )

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Leaderboard,
                    contentDescription = "Rank"
                )
            },
            label = {
                Text("RANK")
            },
            selected = false,
            onClick = {}
        )

        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Account"
                )
            },
            label = {
                Text("ACCOUNT")
            },
            selected = false,
            onClick = {}
        )
    }
}
//