@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.dacs3.ui.main

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.dacs3.model.ChatGroup
import com.example.dacs3.model.ChatMessage
import com.example.dacs3.viewmodel.ChatViewModel
import com.example.dacs3.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ChatScreen(
    userId: String?,
    viewModel: ChatViewModel = viewModel(key = userId ?: "guest"),
    onMenuClick: () -> Unit = {}
) {
    LaunchedEffect(userId) { viewModel.initData(userId) }

    val currentGroup = viewModel.currentGroup
    val groups by viewModel.groups.collectAsState()

    if (currentGroup == null) {
        GroupListScreen(
            groups = groups,
            onGroupClick = { viewModel.selectGroup(it) },
            onCreateGroup = { name, goal, emails -> viewModel.createNewGroup(name, goal, emails) },
            onMenuClick = onMenuClick,
            userAvatarUrl = viewModel.currentUserAvatar,
            userAvatarInitial = viewModel.currentUserAvatarInitial,
            onPickUserAvatar = { file -> viewModel.updateCurrentUserAvatar(file) },
            isUploadingUserAvatar = viewModel.isUploadingUserAvatar,
            userAvatarUpdateStatus = viewModel.userAvatarUpdateStatus
        )
    } else {
        ChatRoomScreen(
            userId = userId,
            viewModel = viewModel,
            group = currentGroup,
            messages = viewModel.messages.collectAsState().value,
            messageText = viewModel.messageText,
            addMemberStatus = viewModel.addMemberStatus,
            onMessageChange = { viewModel.messageText = it },
            onSend = { viewModel.sendMessage() },
            onBack = { viewModel.currentGroup = null },
            onAddMember = { viewModel.addMember(it) },
            onClearStatus = { viewModel.onClearStatus() },
            onLeaveOrDelete = { viewModel.leaveOrDeleteGroup() },
            userAvatarUrl = viewModel.currentUserAvatar,
            userAvatarInitial = viewModel.currentUserAvatarInitial,
            onPickUserAvatar = { file -> viewModel.updateCurrentUserAvatar(file) }
        )
    }
}

@Composable
fun GroupListScreen(
    groups: List<ChatGroup>,
    onGroupClick: (ChatGroup) -> Unit,
    onCreateGroup: (String, String, List<String>) -> Unit,
    onMenuClick: () -> Unit,
    userAvatarUrl: String,
    userAvatarInitial: String,
    onPickUserAvatar: (File) -> Unit,
    isUploadingUserAvatar: Boolean,
    userAvatarUpdateStatus: String?
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val userAvatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPickUserAvatar(uri.toTempImageFile(context))
    }

    Scaffold(
        containerColor = BgLight,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }, containerColor = MyMessageBlue, shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 24.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, contentDescription = "Menu", tint = AppNavy) }
                Text(text = "Study Groups", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppNavy)
                Box(modifier = Modifier) {
                    UserAvatar(imageUrl = userAvatarUrl, initial = userAvatarInitial, modifier = Modifier.size(36.dp))
                }
            }

            if (isUploadingUserAvatar) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            userAvatarUpdateStatus?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, color = if (it.contains("Không") || it.contains("lỗi", ignoreCase = true)) Color.Red else Color(0xFF2E7D32), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(groups) { group ->
                    GroupItem(group = group, onClick = { onGroupClick(group) })
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        var goal by remember { mutableStateOf("") }
        var emails by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Group", fontWeight = FontWeight.Bold, color = AppNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Group Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = goal, onValueChange = { goal = it }, label = { Text("Goal") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = emails, onValueChange = { emails = it }, label = { Text("Members (emails separated by ,)") }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("user@gmail.com") })
                }
            },
            confirmButton = {
                Button(onClick = { onCreateGroup(name, goal, emails.split(",").map { it.trim() }.filter { it.isNotEmpty() }); showCreateDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = AppNavy)) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel", color = AppNavy) }
            }
        )
    }
}

@Composable
fun GroupItem(group: ChatGroup, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GroupAvatar(imageUrl = group.avatarUrl, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = group.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppNavy)
            Text(text = "Score: ${group.totalFocusMinutes} pts • ${group.members.size} members", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ChatRoomScreen(
    userId: String?,
    viewModel: ChatViewModel,
    group: ChatGroup,
    messages: List<ChatMessage>,
    messageText: String,
    addMemberStatus: String?,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    onAddMember: (String) -> Unit,
    onClearStatus: () -> Unit,
    onLeaveOrDelete: () -> Unit,
    userAvatarUrl: String,
    userAvatarInitial: String,
    onPickUserAvatar: (File) -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val userAvatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPickUserAvatar(uri.toTempImageFile(context))
    }

    // Launchers for media and files
    val mediaPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            val contentType = context.contentResolver.getType(it) ?: "image/jpeg"
            viewModel.uploadAndSendFile(it.toTempFile(context), contentType)
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val contentType = context.contentResolver.getType(it) ?: "application/octet-stream"
            viewModel.uploadAndSendFile(it.toTempFile(context), contentType)
        }
    }

    val listState = rememberLazyListState()

    // Tự động scroll xuống cuối khi danh sách tin nhắn thay đổi
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = ChatBg,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = ChatBg),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GroupAvatar(imageUrl = group.avatarUrl, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(group.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AppNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${group.members.size} ACTIVE MINDS • ${group.totalFocusMinutes} PTS", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = AppNavy) } },
                    actions = {
                        Box(modifier = Modifier.clickable { userAvatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                            UserAvatar(imageUrl = userAvatarUrl, initial = userAvatarInitial, modifier = Modifier.size(36.dp))
                        }
                        IconButton(onClick = { showDetails = true }) { Icon(Icons.Default.MoreVert, contentDescription = "Group Details", tint = AppNavy) }
                    }
                )
            },
            bottomBar = { 
                Column {
                    if (viewModel.isUploadingFile) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = MyMessageBlue)
                    }
                    ChatInputBar(
                        text = messageText, 
                        isLocked = viewModel.isChatLocked, 
                        onValueChange = onMessageChange, 
                        onSend = onSend,
                        onAttachMedia = { mediaPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                        onAttachFile = { filePickerLauncher.launch("*/*") }
                    )
                }
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                SharedTimerWidget(group = group, viewModel = viewModel, userId = userId)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp), 
                    reverseLayout = false
                ) {
                    item {
                        Text("TODAY", modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    items(messages) { message ->
                        if (message.type == "SYSTEM") SystemMessage(message) else MessageBubble(
                            message = message, 
                            isMine = message.senderId == userId, 
                            currentUserAvatarUrl = if (message.senderId == userId) userAvatarUrl else message.senderAvatar,
                            currentUserInitial = userAvatarInitial
                        )
                    }
                }
                viewModel.fileUploadStatus?.let {
                    Text(it, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), textAlign = TextAlign.Center, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        if (showDetails) {
            GroupDetailsPanel(
                userId = userId,
                group = group,
                viewModel = viewModel,
                onClose = { showDetails = false },
                onAddMember = onAddMember,
                addMemberStatus = addMemberStatus,
                onClearStatus = onClearStatus,
                onLeaveOrDelete = {
                    showDetails = false
                    onLeaveOrDelete()
                }
            )
        }
    }
}

@Composable
fun SharedTimerWidget(group: ChatGroup, viewModel: ChatViewModel, userId: String?) {
    val isLeader = group.leaderId == userId
    var showPresetDialog by remember { mutableStateOf(false) }
    val userPresets by viewModel.userPresets.collectAsState()

    val bgColor = when (group.timerStatus) {
        "START" -> Color(0xFFE8F5E9)
        "BREAK" -> Color(0xFFFFF3E0)
        else -> MyMessageBlue.copy(alpha = 0.05f)
    }

    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), color = bgColor, shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = when (group.timerStatus) {
                    "START" -> Icons.Default.Timer
                    "BREAK" -> Icons.Default.Coffee
                    else -> Icons.Default.HourglassEmpty
                }, contentDescription = null, tint = AppNavy, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = when (group.timerStatus) {
                        "START" -> "FOCUS: ${group.currentPresetTitle}"
                        "BREAK" -> "BREAK TIME"
                        else -> "WAITING FOR LEADER"
                    }, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = AppNavy, letterSpacing = 0.5.sp)
                    Text(text = viewModel.timerString, fontSize = 22.sp, fontWeight = FontWeight.Black, color = AppNavy)
                }
            }

            if (isLeader && (group.timerStatus == "WAITING" || group.timerStatus == "STOP")) {
                Button(onClick = { showPresetDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = AppNavy), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("START", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showPresetDialog) {
        AlertDialog(
            onDismissRequest = { showPresetDialog = false },
            title = { Text("Select Focus Preset", fontWeight = FontWeight.Bold) },
            text = {
                if (userPresets.isEmpty()) Text("No presets found. Please create one in Focus tab.") else {
                    LazyColumn {
                        items(userPresets) { preset ->
                            ListItem(
                                headlineContent = { Text(preset.title, fontWeight = FontWeight.Bold) }, 
                                supportingContent = { Text("${preset.focusMin}m Focus / ${preset.breakMin}m Break") }, 
                                modifier = Modifier.clickable { 
                                    viewModel.startGroupSession(preset)
                                    showPresetDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun ChatInputBar(
    text: String, 
    isLocked: Boolean, 
    onValueChange: (String) -> Unit, 
    onSend: () -> Unit,
    onAttachMedia: () -> Unit = {},
    onAttachFile: () -> Unit = {}
) {
    Surface(color = Color.White, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        if (isLocked) {
            Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5)).padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Phòng học đang trong giờ tập trung, chat sẽ mở lại khi hết giờ", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        } else {
            Row(modifier = Modifier.imePadding().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onAttachMedia) {
                    Icon(Icons.Outlined.Image, contentDescription = "Send image/video", tint = AppNavy)
                }
                IconButton(onClick = onAttachFile) {
                    Icon(Icons.Outlined.AttachFile, contentDescription = "Send file", tint = AppNavy)
                }
                Surface(modifier = Modifier.weight(1f).height(44.dp), shape = CircleShape, color = Color(0xFFF1F3F4)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (text.isEmpty()) Text("Type a message...", color = Color.Gray, fontSize = 14.sp)
                            BasicTextField(
                                value = text, 
                                onValueChange = onValueChange, 
                                modifier = Modifier.fillMaxWidth(), 
                                textStyle = TextStyle(fontSize = 14.sp, color = MyMessageBlue),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onSend() }),
                                singleLine = true
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onSend, enabled = text.isNotBlank(), modifier = Modifier.size(44.dp).background(if (text.isNotBlank()) MyMessageBlue else Color.LightGray, CircleShape)) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun SystemMessage(message: ChatMessage) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        Surface(color = Color.Gray.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
            Text(text = message.content, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontSize = 11.sp, color = Color.Gray, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isMine: Boolean,
    currentUserAvatarUrl: String? = null,
    currentUserInitial: String = "U"
) {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val timeString = message.timestamp?.toDate()?.let { timeFormat.format(it) } ?: ""

    val displayAvatarUrl = if (isMine) currentUserAvatarUrl else message.senderAvatar
    val displayInitial = if (isMine) currentUserInitial else (message.senderName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            UserAvatar(imageUrl = displayAvatarUrl, initial = displayInitial, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (!isMine) {
                Text(
                    text = message.senderName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }
            Surface(
                color = if (isMine) MyMessageBlue else OtherMessageGreen,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMine) 16.dp else 0.dp,
                    bottomEnd = if (isMine) 0.dp else 16.dp
                )
            ) {
                MessageContent(message = message, isMine = isMine)
            }
            Text(text = timeString, fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
        }
        if (isMine) {
            Spacer(modifier = Modifier.width(8.dp))
            UserAvatar(imageUrl = displayAvatarUrl, initial = displayInitial, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun MessageContent(message: ChatMessage, isMine: Boolean) {
    val textColor = if (isMine) Color.White else MyMessageBlue
    
    when (message.type) {
        "IMAGE" -> {
            AsyncImage(
                model = message.content,
                contentDescription = null,
                modifier = Modifier
                    .sizeIn(maxWidth = 240.dp, maxHeight = 320.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { /* Handle full screen image if needed */ },
                contentScale = ContentScale.Fit
            )
        }
        "VIDEO" -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(180.dp)
                    .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            ) {
                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = textColor, modifier = Modifier.size(48.dp))
                Text("Video", modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp), color = textColor, fontSize = 12.sp)
            }
        }
        "FILE" -> {
            Row(
                modifier = Modifier.padding(12.dp).clickable { /* Download/Open file logic */ },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = textColor)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = message.fileName ?: "File",
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = message.fileSize ?: "", color = textColor.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
        }
        else -> {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = textColor,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun GroupDetailsPanel(
    userId: String?,
    group: ChatGroup,
    viewModel: ChatViewModel,
    onClose: () -> Unit,
    onAddMember: (String) -> Unit,
    addMemberStatus: String?,
    onClearStatus: () -> Unit,
    onLeaveOrDelete: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(group.name) }
    var editGoal by remember { mutableStateOf(group.goal) }
    var showAddMemberDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isLeader = group.leaderId == userId

    val pickAvatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.updateGroupAvatar(uri.toTempImageFile(context))
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppNavy) }
                Text("Group Info", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppNavy, modifier = Modifier.weight(1f))
                if (isLeader) {
                    TextButton(onClick = { if (isEditing) { viewModel.updateGroupInfo(editName, editGoal); isEditing = false } else isEditing = true }) {
                        Text(if (isEditing) "Save" else "Edit", color = AppNavy, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(100.dp)) {
                    GroupAvatar(imageUrl = group.avatarUrl, modifier = Modifier.matchParentSize())
                    if (isLeader) {
                        Surface(modifier = Modifier.align(Alignment.BottomEnd).size(32.dp).clickable { pickAvatarLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, shape = CircleShape, color = AppNavy) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PhotoCamera, contentDescription = "Change avatar", tint = Color.White, modifier = Modifier.size(16.dp)) }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                viewModel.groupAvatarUpdateStatus?.let { status -> Text(status, color = if (status.contains("Không") || status.contains("Chỉ leader")) Color.Red else Color(0xFF2E7D32), fontSize = 12.sp) }
                if (viewModel.isUploadingGroupAvatar) { Spacer(modifier = Modifier.height(8.dp)); CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
                Spacer(modifier = Modifier.height(24.dp))

                if (isEditing) {
                    OutlinedTextField(value = editName, onValueChange = { editName = it }, label = { Text("Group Name") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = editGoal, onValueChange = { editGoal = it }, label = { Text("Goal") }, modifier = Modifier.fillMaxWidth())
                } else {
                    Text(group.name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = AppNavy, textAlign = TextAlign.Center)
                    Text(group.goal, fontSize = 16.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(color = AppNavy, shape = RoundedCornerShape(12.dp)) {
                        Text("SCORE: ${group.totalFocusMinutes} PTS", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("MEMBERS (${group.members.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    IconButton(onClick = { showAddMemberDialog = true }) { Icon(Icons.Default.PersonAdd, contentDescription = "Add", tint = AppNavy) }
                }

                Spacer(modifier = Modifier.weight(1f))
                Button(onClick = onLeaveOrDelete, modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 16.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = if (isLeader) Color.Red else Color.Gray)) {
                    Icon(if (isLeader) Icons.Filled.DeleteForever else Icons.AutoMirrored.Outlined.ExitToApp, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isLeader) "Disband Group" else "Leave Group", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAddMemberDialog) {
        var email by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddMemberDialog = false },
            title = { Text("Add Member", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("User Email") }, modifier = Modifier.fillMaxWidth())
                    addMemberStatus?.let { Text(it, color = if (it.contains("successfully")) Color(0xFF2E7D32) else Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
                }
            },
            confirmButton = { Button(onClick = { onAddMember(email) }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showAddMemberDialog = false; onClearStatus() }) { Text("Cancel") } }
        )
    }
}

@Composable
fun GroupAvatar(imageUrl: String?, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = Color(0xFFE0E0E0)) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(Icons.Default.Groups, contentDescription = null, tint = Color.Gray, modifier = Modifier.padding(16.dp)) }
        }
    }
}

@Composable
fun UserAvatar(imageUrl: String?, initial: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = Color(0xFFBDBDBD)) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(text = initial.ifBlank { "U" }, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun Uri.toTempFile(context: Context): File {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(this)
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "tmp"
    
    var fileName = "file_${System.currentTimeMillis()}.$extension"
    contentResolver.query(this, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (nameIndex != -1 && cursor.moveToFirst()) {
            fileName = cursor.getString(nameIndex)
        }
    }

    val inputStream = contentResolver.openInputStream(this) ?: throw IllegalStateException("Cannot read file")
    val tempFile = File(context.cacheDir, fileName)
    inputStream.use { input ->
        tempFile.outputStream().use { output -> input.copyTo(output) }
    }
    return tempFile
}

private fun Uri.toTempImageFile(context: Context): File {
    val inputStream = context.contentResolver.openInputStream(this) ?: throw IllegalStateException("Không thể đọc ảnh đã chọn")
    val tempFile = File.createTempFile("avatar_", ".jpg", context.cacheDir)
    inputStream.use { input ->
        tempFile.outputStream().use { output -> input.copyTo(output) }
    }
    return tempFile
}
