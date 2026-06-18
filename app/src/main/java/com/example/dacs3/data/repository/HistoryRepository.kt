package com.example.dacs3.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class HistoryRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun saveFocusHistory(
        presetName: String,
        durationMinutes: Int,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onError("Người dùng chưa đăng nhập")
            return
        }

        val timestamp = System.currentTimeMillis()
        val historyId = timestamp.toString()

        val historyData = hashMapOf(
            "id" to historyId,
            "presetName" to presetName,
            "durationMinutes" to durationMinutes,
            "timestamp" to timestamp
        )

        db.collection("users")
            .document(uid)
            .collection("history")
            .document(historyId)
            .set(historyData)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.localizedMessage ?: "Lỗi khi lưu lịch sử focus")
            }
    }

    suspend fun updateStreak() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val userRef = db.collection("users").document(uid)
            val snapshot = userRef.get().await()
            
            val lastFocusTime = snapshot.getLong("lastFocusTimestamp") ?: 0L
            val currentStreak = snapshot.getLong("currentStreak")?.toInt() ?: 0
            val currentTime = System.currentTimeMillis()

            if (isSameDay(lastFocusTime, currentTime)) {
                // Đã focus hôm nay rồi, không cần tăng streak
                return
            }

            val newStreak = if (isYesterday(lastFocusTime, currentTime)) {
                currentStreak + 1
            } else {
                1 // Đã đứt chuỗi hoặc bắt đầu mới
            }

            userRef.update(
                mapOf(
                    "currentStreak" to newStreak,
                    "lastFocusTimestamp" to currentTime
                )
            ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance()
        cal1.timeInMillis = time1
        val cal2 = Calendar.getInstance()
        cal2.timeInMillis = time2
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(lastTime: Long, currentTime: Long): Boolean {
        if (lastTime == 0L) return false
        val calLast = Calendar.getInstance()
        calLast.timeInMillis = lastTime
        
        val calYesterday = Calendar.getInstance()
        calYesterday.timeInMillis = currentTime
        calYesterday.add(Calendar.DAY_OF_YEAR, -1)
        
        return calLast.get(Calendar.YEAR) == calYesterday.get(Calendar.YEAR) &&
                calLast.get(Calendar.DAY_OF_YEAR) == calYesterday.get(Calendar.DAY_OF_YEAR)
    }
}