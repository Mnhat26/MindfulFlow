package com.example.dacs3

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dacs3.data.PreferenceManager
import com.example.dacs3.ui.auth.LoginScreen
import com.example.dacs3.ui.auth.RegisterScreen
import com.example.dacs3.ui.main.FocusScreen
import com.example.dacs3.ui.theme.DACS3Theme
import com.example.dacs3.viewmodel.AuthViewModel
import com.example.dacs3.viewmodel.FocusViewModel
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createNotificationChannel()

//        val authViewModel = AuthViewModel()


        setContent {
            val context = LocalContext.current
            val preferenceManager = remember { PreferenceManager(context) }
            val authViewModel: AuthViewModel = viewModel()

            var isDarkMode by remember { mutableStateOf(preferenceManager.isDarkMode) }
            
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

            // ViewModel để theo dõi trạng thái timer toàn cục
            val focusViewModel: FocusViewModel = viewModel(
                key = currentUserId ?: "guest"
            )

            // Yêu cầu quyền thông báo cho Android 13+
            var hasNotificationPermission by remember {
                mutableStateOf(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    } else true
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                hasNotificationPermission = isGranted
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            // Theo dõi vòng đời để gửi thông báo khi app xuống background
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        if (focusViewModel.isRunning) {
                            showWarningNotification()
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(authViewModel.isAuthSuccess) {
                if (authViewModel.isAuthSuccess) {
                    val newUser = FirebaseAuth.getInstance().currentUser
                    currentUserId = newUser?.uid
                    currentScreen = "main"
                }
            }

            DACS3Theme(darkTheme = isDarkMode) {
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
                        isDarkMode = isDarkMode,
                        onDarkModeToggle = { 
                            isDarkMode = it
                            preferenceManager.isDarkMode = it
                        },
                        viewModel = focusViewModel,
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Focus Timer Warning"
            val descriptionText = "Cảnh báo khi thoát ứng dụng lúc timer đang chạy"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("FOCUS_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showWarningNotification() {
        val builder = NotificationCompat.Builder(this, "FOCUS_CHANNEL")
            .setSmallIcon(R.mipmap.ic_launcher) // Dùng icon mặc định
            .setContentTitle("Mindful Flow vẫn đang chạy")
            .setContentText("Đừng quên quay lại để hoàn thành phiên tập trung của bạn nhé!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, builder.build())
    }
}
