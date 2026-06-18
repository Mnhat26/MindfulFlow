package com.example.dacs3.data

import android.util.Log
import com.example.dacs3.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val supabaseUrl = BuildConfig.SUPABASE_URL.trim().removeSuffix("/")
    private val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY.trim()
    private val bucketName = BuildConfig.SUPABASE_BUCKET_NAME.trim()

    suspend fun login(email: String, pass: String) {
        val result = auth.signInWithEmailAndPassword(email, pass).await()
        result.user?.let { user ->
            ensureUserIdField(user.uid)
        }
    }

    suspend fun register(email: String, pass: String, name: String) {
        val result = auth.createUserWithEmailAndPassword(email, pass).await()
        result.user?.uid?.let { uid ->
            saveUserInfo(uid, name, email)
        }
    }

    suspend fun loginWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()

        val user = result.user
        if (user != null) {
            if (result.additionalUserInfo?.isNewUser == true) {
                saveUserInfo(user.uid, user.displayName ?: "Google User", user.email ?: "")
            } else {
                ensureUserIdField(user.uid)
            }
        }
    }

    suspend fun updateUserAvatar(userId: String, avatarUrl: String) {
        db.collection("users").document(userId)
            .set(mapOf("avatarUrl" to avatarUrl), SetOptions.merge())
            .await()
    }

    suspend fun uploadUserAvatar(userId: String, imageFile: File): String = withContext(Dispatchers.IO) {
        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank() || bucketName.isBlank()) {
            throw IllegalStateException("Supabase storage is not configured. Please check local.properties.")
        }

        val objectPath = "user-avatars/$userId/${UUID.randomUUID()}.jpg"
        val uploadUrl = "$supabaseUrl/storage/v1/object/$bucketName/$objectPath"
        val publicUrl = "$supabaseUrl/storage/v1/object/public/$bucketName/$objectPath"

        val connection = java.net.URL(uploadUrl).openConnection() as java.net.HttpURLConnection
        try {
            connection.connectTimeout = 30000 // 30 seconds
            connection.readTimeout = 30000    // 30 seconds
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $supabaseAnonKey")
            connection.setRequestProperty("apikey", supabaseAnonKey)
            connection.setRequestProperty("Content-Type", "image/jpeg")
            connection.setRequestProperty("x-upsert", "true")
            
            // Sử dụng stream để tránh OutOfMemoryError
            imageFile.inputStream().use { input ->
                connection.outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                Log.e("SupabaseUpload", "Error $responseCode: $errorBody")
                throw IllegalStateException("Supabase upload failed ($responseCode): $errorBody")
            }
            publicUrl
        } catch (e: Exception) {
            Log.e("SupabaseUpload", "Upload error: ${e.message}")
            throw e
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun saveUserInfo(uid: String, name: String, email: String) {
        val userMap = hashMapOf(
            "id" to uid,
            "fullName" to name,
            "email" to email,
            "avatarUrl" to ""
        )
        try {
            db.collection("users").document(uid).set(userMap).await()
            Log.d("FIREBASE_OK", "Đã lưu user $name với ID: $uid thành công!")
        } catch (e: Exception) {
            Log.e("FIREBASE_ERROR", "Lỗi lưu DB: ${e.message}")
        }
    }

    private suspend fun ensureUserIdField(uid: String) {
        try {
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            
            if (snapshot.exists()) {
                if (!snapshot.contains("id")) {
                    docRef.update("id", uid).await()
                    Log.d("FIREBASE_UPDATE", "Đã bổ sung trường id cho user cũ: $uid")
                }
            } else {
                val basicInfo = hashMapOf("id" to uid, "email" to (auth.currentUser?.email ?: ""), "avatarUrl" to "")
                docRef.set(basicInfo, SetOptions.merge()).await()
            }
        } catch (e: Exception) {
            Log.e("FIREBASE_ERROR", "Lỗi kiểm tra/bổ sung id: ${e.message}")
        }
    }
}
