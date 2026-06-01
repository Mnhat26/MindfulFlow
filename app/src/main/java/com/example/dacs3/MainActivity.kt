package com.example.dacs3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.dacs3.ui.auth.LoginScreen
import com.example.dacs3.ui.auth.RegisterScreen
import com.example.dacs3.ui.main.FocusScreen
import com.example.dacs3.ui.theme.DACS3Theme
import com.example.dacs3.viewmodel.AuthViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val authViewModel = AuthViewModel()

        setContent {
            var currentScreen by remember {
                mutableStateOf(
                    if (FirebaseAuth.getInstance().currentUser != null) {
                        "main"
                    } else {
                        "login"
                    }
                )
            }

            var currentUserId by remember {
                mutableStateOf(FirebaseAuth.getInstance().currentUser?.uid)
            }

            LaunchedEffect(authViewModel.isAuthSuccess) {
                if (authViewModel.isAuthSuccess) {
                    currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                    currentScreen = "main"
                }
            }

            DACS3Theme {
                when (currentScreen) {
                    "login" -> LoginScreen(
                        viewModel = authViewModel,
                        onSignUpClick = {
                            currentScreen = "register"
                        }
                    )

                    "register" -> RegisterScreen(
                        viewModel = authViewModel,
                        onSignInClick = {
                            currentScreen = "login"
                        }
                    )

                    "main" -> FocusScreen(
                        userId = currentUserId,
                        onLogout = {
                            FirebaseAuth.getInstance().signOut()
                            authViewModel.resetAuthState()
                            currentUserId = null
                            currentScreen = "login"
                        }
                    )
                }
            }
        }
    }
}
///