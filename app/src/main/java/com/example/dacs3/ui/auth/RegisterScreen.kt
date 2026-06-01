package com.example.dacs3.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dacs3.R
import com.example.dacs3.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

private val InputBgGray = Color(0xFFF3F4F6)

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = viewModel(),
    onSignInClick: () -> Unit = {},
    onGoogleClick: () -> Unit = {}
) {
    var isAgreed by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { viewModel.onGoogleLogin(it) }
            } catch (e: Exception) {
                viewModel.errorMessage = "Google Sign-In failed: ${e.localizedMessage}"
            }
        }
    }

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

            Button(
                onClick = {
                    onGoogleClick()
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    launcher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = InputBgGray,
                    contentColor = Color.Black
                )
            ) {
                Text("Continue with Google", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
                Text(" OR WITH EMAIL ", fontSize = 10.sp, color = TextGray, letterSpacing = 1.sp)
                HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray.copy(alpha = 0.5f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            AuthTextField(
                label = "FULL NAME",
                value = viewModel.fullName,
                onValueChange = { viewModel.fullName = it },
                placeholder = "Alex Rivers",
                icon = Icons.Outlined.Person
            )

            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField("EMAIL ADDRESS", viewModel.email, { viewModel.email = it }, "alex@flow.com", Icons.Outlined.Email, keyboardType = KeyboardType.Email)
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField("PASSWORD", viewModel.password, { viewModel.password = it }, "••••••••", Icons.Outlined.Lock, isPassword = true)
            Spacer(modifier = Modifier.height(16.dp))
            AuthTextField("CONFIRM", viewModel.confirmPassword, { viewModel.confirmPassword = it }, "••••••••", Icons.Default.CheckCircle, isPassword = true)

            // HIỂN THỊ LỖI (GIỮ NGUYÊN)
            viewModel.errorMessage?.let {
                Text(
                    text = it,
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- CHECKBOX (GIỮ NGUYÊN) ---
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
                onClick = {
                    if (!isAgreed) {
                        viewModel.errorMessage = "Vui lòng đồng ý với Điều khoản dịch vụ!"
                    } else {
                        viewModel.onRegister()
                    }
                },
                enabled = !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppNavy)
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
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
//