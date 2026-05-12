package com.example.dacs3.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

        val historyId = System.currentTimeMillis().toString()

        val historyData = hashMapOf(
            "id" to historyId,
            "presetName" to presetName,
            "durationMinutes" to durationMinutes,
            "timestamp" to System.currentTimeMillis()
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
}