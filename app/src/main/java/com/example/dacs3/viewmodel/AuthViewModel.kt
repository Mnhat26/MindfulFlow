package com.example.dacs3.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dacs3.data.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()


    var fullName by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isAuthSuccess by mutableStateOf(false)

    private fun validateInputs(isRegister: Boolean): Boolean {
        errorMessage = null
        if (isRegister && fullName.trim().isEmpty()) {
            errorMessage = "Vui lòng nhập họ tên!"
            return false
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorMessage = "Email không hợp lệ!"
            return false
        }
        if (password.length < 6) {
            errorMessage = "Mật khẩu phải từ 6 ký tự!"
            return false
        }
        if (isRegister && password != confirmPassword) {
            errorMessage = "Mật khẩu xác nhận không khớp!"
            return false
        }
        return true
    }

    fun onLogin() {
        if (!validateInputs(false)) return
        viewModelScope.launch {
            try {
                isLoading = true
                repository.login(email, password)
                isAuthSuccess = true
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
            } finally {
                isLoading = false
            }
        }
    }

    fun onRegister() {
        if (!validateInputs(true)) return
        viewModelScope.launch {
            try {
                isLoading = true
                repository.register(email, password, fullName)
                isAuthSuccess = true
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
            } finally {
                isLoading = false
            }
        }
    }

    fun onGoogleLogin(idToken: String) {
        viewModelScope.launch {
            try {
                isLoading = true
                repository.loginWithGoogle(idToken)
                isAuthSuccess = true
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
            } finally {
                isLoading = false
            }
        }
    }
    fun resetAuthState() {
        fullName = ""
        email = ""
        password = ""
        confirmPassword = ""
        isLoading = false
        errorMessage = null
        isAuthSuccess = false
    }
}
