package com.example.dacs3.ui.auth

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Mình đã thêm lại 2 màu này để code không bị báo đỏ
private val InputBgGray = Color(0xFFF3F4F6)

@Composable
fun RegisterScreen(
    onRegisterClick: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    onGoogleClick: () -> Unit = {},
    onAppleClick: () -> Unit = {}
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isAgreed by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Logo",
                    tint = AppNavy,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mindful Flow", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppNavy)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Create your\nsanctuary",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AppNavy,
                lineHeight = 36.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Start your journey to deeper focus today.", fontSize = 14.sp, color = TextGray)

            Spacer(modifier = Modifier.height(32.dp))

            // --- NÚT ĐĂNG NHẬP MẠNG XÃ HỘI ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onGoogleClick,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    // Thêm contentColor = Color.Black để có hiệu ứng nhấn gợn sóng đen đậm
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InputBgGray,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Google", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onAppleClick,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    // Thêm contentColor = Color.Black để có hiệu ứng nhấn gợn sóng đen đậm
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InputBgGray,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Apple", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
                Text(" OR WITH EMAIL ", fontSize = 10.sp, color = TextGray, letterSpacing = 1.sp)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            AuthTextField("FULL NAME", fullName, { fullName = it }, "Alex Rivers", Icons.Outlined.Person)
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField("EMAIL ADDRESS", email, { email = it }, "alex@flow.com", Icons.Outlined.Email, keyboardType = KeyboardType.Email)
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField("PASSWORD", password, { password = it }, "••••••••", Icons.Outlined.Lock, isPassword = true)
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField("CONFIRM", confirmPassword, { confirmPassword = it }, "••••••••", Icons.Default.CheckCircle, isPassword = true)

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isAgreed,
                    onCheckedChange = { isAgreed = it },
                    colors = CheckboxDefaults.colors(checkedColor = AppNavy)
                )
                Text(
                    text = buildAnnotatedString {
                        append("I agree to the ")
                        withStyle(style = SpanStyle(color = AppNavy, fontWeight = FontWeight.Bold)) { append("Terms of Service") }
                        append(" and\n")
                        withStyle(style = SpanStyle(color = AppNavy, fontWeight = FontWeight.Bold)) { append("Privacy Policy") }
                        append(".")
                    },
                    fontSize = 13.sp,
                    color = TextGray,
                    lineHeight = 18.sp,
                    modifier = Modifier.clickable { isAgreed = !isAgreed }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRegisterClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppNavy)
            ) {
                Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Already have an account? ", color = TextGray, fontSize = 14.sp)
                TextButton(onClick = onSignInClick, contentPadding = PaddingValues(0.dp)) {
                    Text("Sign In", color = AppNavy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AppNavy, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.Gray) },
            leadingIcon = { Icon(imageVector = icon, contentDescription = null, tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = InputBgGray,
                unfocusedContainerColor = InputBgGray,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true,
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRegisterScreen() {
    RegisterScreen()
}