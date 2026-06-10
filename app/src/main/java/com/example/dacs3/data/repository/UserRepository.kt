package com.example.dacs3.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class UserInfo(
    val fullName: String = "Unknown User",
    val avatarUrl: String = "",
    val title: String = "Member",
    val totalFocusMinutes: Int = 0
)

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun listenUser(
        onResult: (UserInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onResult(UserInfo("Guest"))
            return
        }

        db.collection("users")
            .document(uid)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Lỗi khi tải thông tin user")
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val userInfo = UserInfo(
                        fullName = document.getString("fullName") ?: "Unknown User",
                        avatarUrl = document.getString("avatarUrl") ?: "",
                        title = document.getString("title") ?: "Member",
                        totalFocusMinutes = document.getLong("totalFocusMinutes")?.toInt() ?: 0
                    )
                    onResult(userInfo)
                } else {
                    onResult(UserInfo("No Data Found"))
                }
            }
    }

    suspend fun incrementFocusMinutes(minutes: Int) {
        val uid = auth.currentUser?.uid ?: return
        try {
            db.collection("users").document(uid)
                .update("totalFocusMinutes", FieldValue.increment(minutes.toLong()))
                .await()
        } catch (e: Exception) {
            // Log error if needed
        }
    }

    fun listenUserName(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        listenUser(
            onResult = { onResult(it.fullName) },
            onError = onError
        )
    }
}
