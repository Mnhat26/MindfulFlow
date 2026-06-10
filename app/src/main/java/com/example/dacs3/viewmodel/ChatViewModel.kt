package com.example.dacs3.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dacs3.data.AuthRepository
import com.example.dacs3.data.ChatRepository
import com.example.dacs3.data.SupabaseStorageRepository
import com.example.dacs3.data.repository.PresetRepository
import com.example.dacs3.data.repository.UserRepository
import com.example.dacs3.model.ChatGroup
import com.example.dacs3.model.ChatMessage
import com.example.dacs3.model.TimerPreset
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class ChatViewModel : ViewModel() {
    private val repository = ChatRepository()
    private val presetRepository = PresetRepository()
    private val authRepository = AuthRepository()
    private val userRepository = UserRepository()
    private val supabaseStorageRepository = SupabaseStorageRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _groups = MutableStateFlow<List<ChatGroup>>(emptyList())
    val groups: StateFlow<List<ChatGroup>> = _groups

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _userPresets = MutableStateFlow<List<TimerPreset>>(emptyList())
    val userPresets: StateFlow<List<TimerPreset>> = _userPresets

    var currentGroup by mutableStateOf<ChatGroup?>(null)
    var messageText by mutableStateOf("")
    var addMemberStatus by mutableStateOf<String?>(null)
    var currentUserName by mutableStateOf("User")
    var currentUserAvatar by mutableStateOf("")
    var currentUserAvatarInitial by mutableStateOf("U")
    var userAvatarUpdateStatus by mutableStateOf<String?>(null)
    var isUploadingUserAvatar by mutableStateOf(false)
    var groupAvatarUpdateStatus by mutableStateOf<String?>(null)
    var isUploadingGroupAvatar by mutableStateOf(false)

    // File upload states
    var isUploadingFile by mutableStateOf(false)
    var fileUploadStatus by mutableStateOf<String?>(null)

    // Timer states
    var timeLeft by mutableIntStateOf(0)
    var timerString by mutableStateOf("00:00")
    var isChatLocked by mutableStateOf(false)

    private var currentUserId: String? = null
    private var groupsJob: Job? = null
    private var messagesJob: Job? = null
    private var timerJob: Job? = null
    private var secondsTracked = 0 // Theo dõi số giây tập trung trong nhóm

    fun initData(userId: String?) {
        if (userId == null || currentUserId == userId) return
        clearOldData()
        currentUserId = userId
        loadGroups(userId)
        listenUserPresets()
        listenUser(userId)
    }

    private fun clearOldData() {
        currentGroup = null
        messageText = ""
        _messages.value = emptyList()
        _groups.value = emptyList()
        groupsJob?.cancel()
        messagesJob?.cancel()
        timerJob?.cancel()
        secondsTracked = 0
    }

    private fun listenUser(userId: String) {
        userRepository.listenUser(
            onResult = { userInfo ->
                currentUserName = userInfo.fullName
                currentUserAvatar = userInfo.avatarUrl
                currentUserAvatarInitial = userInfo.fullName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "U"
            },
            onError = { }
        )
    }

    private fun loadGroups(userId: String) {
        groupsJob?.cancel()
        groupsJob = viewModelScope.launch {
            repository.getGroups(userId).collectLatest { updatedGroups ->
                _groups.value = updatedGroups
                // Cập nhật group hiện tại nếu đang trong phòng
                currentGroup?.let { current ->
                    updatedGroups.find { it.groupId == current.groupId }?.let { updated ->
                        currentGroup = updated
                        handleTimerState(updated)
                    }
                }
            }
        }
    }

    private fun handleTimerState(group: ChatGroup) {
        // Cập nhật trạng thái khóa chat: Khóa nếu đang START (Focus)
        isChatLocked = group.timerStatus == "START"

        // Logic đếm ngược
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            if (group.timerStatus != "WAITING" && group.timerStatus != "STOP" && group.startTime != null) {
                while (true) {
                    val now = System.currentTimeMillis() / 1000
                    val start = group.startTime.seconds
                    val elapsed = (now - start).toInt()
                    val total = if (group.timerStatus == "START") group.timerSeconds else group.breakSeconds
                    
                    val remaining = (total - elapsed).coerceAtLeast(0)
                    timeLeft = remaining
                    
                    val mins = remaining / 60
                    val secs = remaining % 60
                    timerString = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)

                    // Tính điểm cho cá nhân khi đang ở trong phiên tập trung của nhóm
                    if (group.timerStatus == "START" && remaining > 0) {
                        secondsTracked++
                        if (secondsTracked >= 60) {
                            userRepository.incrementFocusMinutes(1)
                            secondsTracked = 0
                        }
                    }

                    if (remaining <= 0) {
                        onTimerFinished(group)
                        break
                    }
                    delay(1000)
                }
            } else {
                secondsTracked = 0
                timeLeft = group.timerSeconds
                val mins = timeLeft / 60
                val secs = timeLeft % 60
                timerString = String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
            }
        }
    }

    private fun onTimerFinished(group: ChatGroup) {
        if (currentUserId == null) return
        secondsTracked = 0
        viewModelScope.launch {
            if (group.timerStatus == "START") {
                if (group.leaderId == currentUserId) {
                    repository.startBreakSession(group.groupId)
                    sendMessage("Giai đoạn tập trung kết thúc! Hãy nghỉ ngơi nào.", isSystem = true)
                }
            } else if (group.timerStatus == "BREAK") {
                // Kết thúc vòng lặp
                repository.finalizeCycle(group.groupId, currentUserId!!, group.timerSeconds / 60)
                if (group.leaderId == currentUserId) {
                    sendMessage("Vòng lặp hoàn thành! Nhóm đã được cộng điểm.", isSystem = true)
                }
            }
        }
    }

    fun startGroupSession(preset: TimerPreset) {
        val group = currentGroup ?: return
        if (group.leaderId != currentUserId) return
        viewModelScope.launch {
            repository.startFocusSession(group.groupId, preset)
            sendMessage("Leader đã bắt đầu phiên tập trung: ${preset.title}", isSystem = true)
        }
    }

    private fun listenUserPresets() {
        presetRepository.listenPresets(
            onResult = { presets -> _userPresets.value = presets },
            onError = { }
        )
    }

    fun selectGroup(group: ChatGroup) {
        _messages.value = emptyList()
        currentGroup = group
        handleTimerState(group)
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getMessages(group.groupId).collectLatest {
                _messages.value = it
            }
        }
    }

    fun sendMessage(content: String = messageText, isSystem: Boolean = false) {
        val group = currentGroup ?: return
        val uid = currentUserId ?: auth.currentUser?.uid ?: return
        if (content.isBlank()) return
        if (isChatLocked && !isSystem) return

        val message = ChatMessage(
            senderId = if (isSystem) "SYSTEM" else uid,
            senderName = if (isSystem) "System" else currentUserName,
            senderAvatar = if (isSystem) "" else currentUserAvatar,
            content = content,
            type = if (isSystem) "SYSTEM" else "TEXT"
        )

        viewModelScope.launch {
            repository.sendMessage(group.groupId, message)
            if (!isSystem) messageText = ""
        }
    }

    fun uploadAndSendFile(file: File, contentType: String) {
        val group = currentGroup ?: return
        val uid = currentUserId ?: return
        if (isChatLocked) return

        viewModelScope.launch {
            runCatching {
                isUploadingFile = true
                fileUploadStatus = "Đang tải file lên..."
                val fileUrl = supabaseStorageRepository.uploadChatFile(group.groupId, file, contentType)
                
                val type = when {
                    contentType.contains("image") -> "IMAGE"
                    contentType.contains("video") -> "VIDEO"
                    else -> "FILE"
                }

                val message = ChatMessage(
                    senderId = uid,
                    senderName = currentUserName,
                    senderAvatar = currentUserAvatar,
                    content = fileUrl,
                    type = type,
                    fileName = file.name,
                    fileSize = "${file.length() / 1024} KB"
                )
                repository.sendMessage(group.groupId, message)
                fileUploadStatus = null
            }.onFailure { exception ->
                fileUploadStatus = "Lỗi khi gửi file: ${exception.message}"
            }
            isUploadingFile = false
        }
    }

    fun applyPresetToGroup(preset: TimerPreset) {
        startGroupSession(preset)
    }

    fun createNewGroup(name: String, goal: String, memberEmails: List<String> = emptyList()) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            repository.createGroup(name, "", goal, userId, memberEmails)
        }
    }

    fun addMember(email: String) {
        val group = currentGroup ?: return
        viewModelScope.launch {
            val success = repository.addMemberByEmail(group.groupId, email)
            addMemberStatus = if (success) "Member added successfully" else "User not found"
            if (success) {
                sendMessage(content = "Thành viên mới đã gia nhập nhóm", isSystem = true)
            }
        }
    }

    fun updateGroupInfo(name: String, goal: String) {
        val group = currentGroup ?: return
        viewModelScope.launch {
            repository.updateGroupInfo(group.groupId, name, goal)
            sendMessage(content = "Thông tin nhóm đã được cập nhật", isSystem = true)
        }
    }

    fun updateGroupAvatar(imageFile: File) {
        val group = currentGroup ?: return
        val userId = currentUserId ?: return
        if (group.leaderId != userId) {
            groupAvatarUpdateStatus = "Chỉ leader mới có quyền đổi avatar nhóm"
            return
        }

        viewModelScope.launch {
            runCatching {
                isUploadingGroupAvatar = true
                groupAvatarUpdateStatus = null
                val avatarUrl = supabaseStorageRepository.uploadGroupAvatarFile(group.groupId, imageFile)
                repository.updateGroupAvatar(group.groupId, avatarUrl)
                currentGroup = group.copy(avatarUrl = avatarUrl)
                sendMessage(content = "Avatar nhóm đã được cập nhật", isSystem = true)
            }.onSuccess {
//                groupAvatarUpdateStatus = "Đã cập nhật avatar nhóm"
            }.onFailure { exception ->
                groupAvatarUpdateStatus = exception.message ?: "Không thể cập nhật avatar nhóm"
            }
            isUploadingGroupAvatar = false
        }
    }

    fun updateCurrentUserAvatar(imageFile: File) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            runCatching {
                isUploadingUserAvatar = true
                userAvatarUpdateStatus = null
                val avatarUrl = authRepository.uploadUserAvatar(userId, imageFile)
                authRepository.updateUserAvatar(userId, avatarUrl)
//                userAvatarUpdateStatus = "Đã cập nhật avatar cá nhân"
            }.onFailure { exception ->
                userAvatarUpdateStatus = exception.message ?: "Không thể cập nhật avatar cá nhân"
            }
            isUploadingUserAvatar = false
        }
    }

    fun onClearStatus() {
        addMemberStatus = null
    }

    fun leaveOrDeleteGroup() {
        val group = currentGroup ?: return
        val userId = currentUserId ?: return
        viewModelScope.launch {
            if (group.leaderId == userId) {
                repository.deleteGroup(group.groupId)
            } else {
                repository.leaveGroup(group.groupId, userId)
                sendMessage(content = "Một thành viên đã rời nhóm", isSystem = true)
            }
            currentGroup = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        groupsJob?.cancel()
        messagesJob?.cancel()
        timerJob?.cancel()
    }
}
