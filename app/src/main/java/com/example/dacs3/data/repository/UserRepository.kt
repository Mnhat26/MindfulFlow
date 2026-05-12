package com.example.dacs3.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun listenUserName(
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onResult("Guest")
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
                    val fullName = document.getString("fullName") ?: "Unknown User"
                    onResult(fullName)
                } else {
                    onResult("No Data Found")
                }
            }
    }
}