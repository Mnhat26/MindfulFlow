package com.example.dacs3.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dacs3.data.AuthRepository
import com.example.dacs3.data.repository.UserInfo
import com.example.dacs3.data.repository.UserRepository
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ProfileViewModel : ViewModel() {
    private val userRepository = UserRepository()
    private val authRepository = AuthRepository()

    var userInfo by mutableStateOf<UserInfo?>(null)
    var fullName by mutableStateOf("")
    var title by mutableStateOf("")
    var avatarUrl by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var isUpdating by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var updateSuccess by mutableStateOf(false)

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        isLoading = true
        userRepository.listenUser(
            onResult = { info ->
                userInfo = info
                fullName = info.fullName
                title = info.title
                avatarUrl = info.avatarUrl
                isLoading = false
            },
            onError = { error ->
                errorMessage = error
                isLoading = false
            }
        )
    }

    fun updateProfile() {
        if (fullName.isBlank()) {
            errorMessage = "Họ tên không được để trống"
            return
        }

        viewModelScope.launch {
            try {
                isUpdating = true
                userRepository.updateProfile(fullName, title)
                updateSuccess = true
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
            } finally {
                isUpdating = false
            }
        }
    }

    fun uploadAvatar(context: Context, uri: Uri) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                isUpdating = true
                val file = uriToFile(context, uri)
                val uploadedUrl = authRepository.uploadUserAvatar(uid, file)
                userRepository.updateUserAvatar(uploadedUrl)
                avatarUrl = uploadedUrl
            } catch (e: Throwable) {
                errorMessage = "Lỗi upload ảnh: ${e.localizedMessage}"
            } finally {
                isUpdating = false
            }
        }
    }

    private fun uriToFile(context: Context, uri: Uri): File {
        val file = File(context.cacheDir, "temp_avatar_${System.currentTimeMillis()}.jpg")
        val inputStream = context.contentResolver.openInputStream(uri) 
            ?: throw IllegalStateException("Không thể mở tệp từ nguồn này (Google Drive/Cloud). Vui lòng tải xuống máy hoặc chọn ảnh khác.")
        
        inputStream.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    fun clearMessage() {
        errorMessage = null
        updateSuccess = false
    }
}
