package com.example.dacs3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.dacs3.ui.auth.LoginScreen
import com.example.dacs3.ui.auth.RegisterScreen
import com.example.dacs3.ui.theme.DACS3Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DACS3Theme {

                RegisterScreen(
                    onRegisterClick = {},
                    onSignInClick = {}
                )
            }
        }
    }
}