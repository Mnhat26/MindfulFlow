package com.example.dacs3.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import java.util.Calendar

data class UserInfo(
    val fullName: String = "Unknown User",
    val avatarUrl: String = "",
    val title: String = "Member",
    val totalFocusMinutes: Int = 0,
    val currentStreak: Int = 0
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
                        totalFocusMinutes = document.getLong("totalFocusMinutes")?.toInt() ?: 0,
                        currentStreak = document.getLong("currentStreak")?.toInt() ?: 0
                    )
                    
                    // Kiểm tra và cập nhật streak khi mở app
                    updateDailyStreak(document.getLong("lastOpenTimestamp") ?: 0L, document.getLong("currentStreak")?.toInt() ?: 0)
                    
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
            
            // Đếm số lượng user có totalFocusMinutes lớn hơn
            val query = db.collection("users")
                .whereGreaterThan("totalFocusMinutes", userMinutes)
                .get()
                .await()
            
            query.size() + 1
        } catch (e: Exception) {
            1 // Default rank
        }
    }

    private fun updateDailyStreak(lastOpenTime: Long, currentStreak: Int) {
        val uid = auth.currentUser?.uid ?: return
        val currentTime = System.currentTimeMillis()
        
        if (isSameDay(lastOpenTime, currentTime)) {
            // Đã mở app hôm nay rồi, không làm gì cả
            return
        }

        val newStreak = if (isYesterday(lastOpenTime, currentTime)) {
            currentStreak + 1
        } else {
            1 // Bỏ lỡ hoặc mới bắt đầu
        }

        db.collection("users").document(uid).update(
            mapOf(
                "currentStreak" to newStreak,
                "lastOpenTimestamp" to currentTime
            )
        )
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        if (time1 == 0L) return false
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(lastTime: Long, currentTime: Long): Boolean {
        if (lastTime == 0L) return false
        val calYesterday = Calendar.getInstance().apply { 
            timeInMillis = currentTime
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val calLast = Calendar.getInstance().apply { timeInMillis = lastTime }
        
        return calLast.get(Calendar.YEAR) == calYesterday.get(Calendar.YEAR) &&
                calLast.get(Calendar.DAY_OF_YEAR) == calYesterday.get(Calendar.DAY_OF_YEAR)
    }
}
