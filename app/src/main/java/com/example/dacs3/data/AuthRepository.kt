package com.example.dacs3.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun login(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass).await()
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

        if (result.additionalUserInfo?.isNewUser == true) {
            result.user?.let { user ->
                saveUserInfo(user.uid, user.displayName ?: "Google User", user.email ?: "")
            }
        }
    }

    private suspend fun saveUserInfo(uid: String, name: String, email: String) {
        val userMap = hashMapOf(
            "fullName" to name,
            "email" to email,
        )
        try {
            db.collection("users").document(uid).set(userMap).await()
            Log.d("FIREBASE_OK", "Đã lưu user $name vào Firestore thành công!")
        } catch (e: Exception) {
            Log.e("FIREBASE_ERROR", "Lỗi lưu DB: ${e.message}")
        }
    }
}
///