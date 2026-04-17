package com.example.dacs3.ui.auth

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

val AppNavy = Color(0xFF060B26)
val AppLightGray = Color(0xFFF5F7FA)
val TextGray = Color(0xFF6B7280)

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {},
    // THÊM 2 THAM SỐ BẮT SỰ KIỆN CLICK
    onGoogleClick: () -> Unit = {},
    onAppleClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppLightGray
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AppNavy,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Logo",
                    tint = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Mindful Flow",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AppNavy
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Welcome back to your sanctuary of focus.",
                fontSize = 14.sp,
                color = TextGray
            )

            Spacer(modifier = Modifier.height(40.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "EMAIL ADDRESS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppNavy,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("alex@deepwork.com", color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    trailingIcon = { Text("@", color = Color.LightGray, fontSize = 20.sp, modifier = Modifier.padding(end = 12.dp)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SECURITY CODE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppNavy,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Forgot Password?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppNavy
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("••••••••", color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    trailingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.LightGray) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onLoginClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppNavy)
            ) {
                Text("Enter Flow State", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
                Text(" COMMUNITY ACCESS ", fontSize = 10.sp, color = TextGray, letterSpacing = 1.sp)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- NÚT ĐĂNG NHẬP MẠNG XÃ HỘI ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onGoogleClick,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black // Thêm màu content để có hiệu ứng gợn sóng đen
                    )
                ) {
                    Text("Google", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = onAppleClick,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black // Thêm màu content để có hiệu ứng gợn sóng đen
                    )
                ) {
                    Text("Apple", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Don't have an account? ", color = TextGray, fontSize = 14.sp)
                TextButton(onClick = onSignUpClick, contentPadding = PaddingValues(0.dp)) {
                    Text("Sign Up", color = AppNavy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SECURE SESSION ENCRYPTED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AppNavy)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    LoginScreen()
}