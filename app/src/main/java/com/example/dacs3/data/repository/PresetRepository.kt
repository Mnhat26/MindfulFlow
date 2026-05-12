package com.example.dacs3.data.repository

import com.example.dacs3.model.TimerPreset
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PresetRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun listenPresets(
        onResult: (List<TimerPreset>) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onError("Người dùng chưa đăng nhập")
            return
        }

        db.collection("users")
            .document(uid)
            .collection("presets")
            .addSnapshotListener { value, error ->
                if (error != null) {
                    onError(error.localizedMessage ?: "Lỗi khi tải presets")
                    return@addSnapshotListener
                }

                val presets = value?.documents
                    ?.mapNotNull { document ->
                        document.toObject(TimerPreset::class.java)
                    }
                    ?: emptyList()

                onResult(presets)
            }
    }

    fun savePreset(
        preset: TimerPreset,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onError("Người dùng chưa đăng nhập")
            return
        }

        val finalId = if (preset.id == 0L) {
            System.currentTimeMillis()
        } else {
            preset.id
        }

        val finalPreset = preset.copy(id = finalId)

        db.collection("users")
            .document(uid)
            .collection("presets")
            .document(finalId.toString())
            .set(finalPreset)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.localizedMessage ?: "Lỗi khi lưu preset")
            }
    }

    fun deletePreset(
        presetId: Long,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onError("Người dùng chưa đăng nhập")
            return
        }

        db.collection("users")
            .document(uid)
            .collection("presets")
            .document(presetId.toString())
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(exception.localizedMessage ?: "Lỗi khi xóa preset")
            }
    }
}