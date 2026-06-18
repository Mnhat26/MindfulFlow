package com.example.dacs3.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class UserInfo(
    val fullName: String = "Unknown User",
    val avatarUrl: String = "",
    val title: String = "Member",
    val totalFocusMinutes: Int = 0,
    val streak: Int = 0  // ĐÃ ĐỔI VỀ STREAK
)

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun listenUser(
        onResult: (UserInfo) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Lỗi kết nối Firestore")
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    val lastDate = document.getString("lastFocusDate") ?: ""
                    var dbStreak = document.getLong("streak")?.toInt() ?: 0 // ĐÃ ĐỔI VỀ STREAK

                    if (dbStreak > 0 && lastDate.isNotEmpty()) {
                        val cal = Calendar.getInstance()
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val todayStr = sdf.format(cal.time)

                        cal.add(Calendar.DAY_OF_YEAR, -1)
                        val yesterdayStr = sdf.format(cal.time)

                        if (lastDate != todayStr && lastDate != yesterdayStr) {
                            dbStreak = 0
                            document.reference.update("streak", 0) // ĐÃ ĐỔI VỀ STREAK
                        }
                    }

                    val userInfo = UserInfo(
                        fullName = document.getString("fullName") ?: "Unknown User",
                        avatarUrl = document.getString("avatarUrl") ?: "",
                        title = document.getString("title") ?: "Member",
                        totalFocusMinutes = document.getLong("totalFocusMinutes")?.toInt() ?: 0,
                        streak = dbStreak // ĐÃ ĐỔI VỀ STREAK
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
            // Log error
        }
    }

    suspend fun updateProfile(fullName: String, title: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .set(mapOf(
                "fullName" to fullName,
                "title" to title
            ), SetOptions.merge())
            .await()
    }

    suspend fun updateUserAvatar(avatarUrl: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .set(mapOf("avatarUrl" to avatarUrl), SetOptions.merge())
            .await()
    }

    suspend fun getGlobalRank(): Int {
        val uid = auth.currentUser?.uid ?: return 0
        return try {
            val userDoc = db.collection("users").document(uid).get().await()
            val userMinutes = userDoc.getLong("totalFocusMinutes") ?: 0L

            val query = db.collection("users")
                .whereGreaterThan("totalFocusMinutes", userMinutes)
                .get()
                .await()

            query.size() + 1
        } catch (e: Exception) {
            1
        }
    }
}