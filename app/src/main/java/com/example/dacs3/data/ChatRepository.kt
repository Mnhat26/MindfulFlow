package com.example.dacs3.data

import com.example.dacs3.model.ChatGroup
import com.example.dacs3.model.ChatMessage
import com.example.dacs3.model.TimerPreset
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun createGroup(name: String, avatarUrl: String, goal: String, leaderId: String, memberEmails: List<String> = emptyList()) {
        val groupRef = db.collection("groups").document()
        val members = mutableListOf(leaderId)
        
        for (email in memberEmails) {
            val userSnapshot = db.collection("users")
                .whereEqualTo("email", email.trim())
                .get()
                .await()
            if (!userSnapshot.isEmpty) {
                val userId = userSnapshot.documents[0].id
                if (!members.contains(userId)) {
                    members.add(userId)
                }
            }
        }

        val group = ChatGroup(
            groupId = groupRef.id,
            name = name,
            avatarUrl = avatarUrl,
            goal = goal,
            leaderId = leaderId,
            members = members
        )
        groupRef.set(group).await()
    }

    suspend fun updateGroupInfo(groupId: String, name: String, goal: String) {
        db.collection("groups").document(groupId).update(
            mapOf("name" to name, "goal" to goal)
        ).await()
    }

    suspend fun updateGroupAvatar(groupId: String, avatarUrl: String) {
        db.collection("groups").document(groupId).update("avatarUrl", avatarUrl).await()
    }

    suspend fun addMemberByEmail(groupId: String, email: String): Boolean {
        val userSnapshot = db.collection("users")
            .whereEqualTo("email", email.trim())
            .get()
            .await()
        
        return if (!userSnapshot.isEmpty) {
            val userId = userSnapshot.documents[0].id
            db.collection("groups").document(groupId)
                .update("members", FieldValue.arrayUnion(userId)).await()
            true
        } else {
            false
        }
    }

    suspend fun updateGroupGoal(groupId: String, newGoal: String) {
        db.collection("groups").document(groupId).update("goal", newGoal).await()
    }

    suspend fun deleteGroup(groupId: String) {
        db.collection("groups").document(groupId).delete().await()
    }

    suspend fun leaveGroup(groupId: String, userId: String) {
        db.collection("groups").document(groupId)
            .update("members", FieldValue.arrayRemove(userId)).await()
    }

    fun getGroups(userId: String): Flow<List<ChatGroup>> = callbackFlow {
        val registration = db.collection("groups")
            .whereArrayContains("members", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val groups = snapshot?.toObjects(ChatGroup::class.java) ?: emptyList()
                trySend(groups)
            }
        awaitClose { registration.remove() }
    }

    fun getMessages(groupId: String): Flow<List<ChatMessage>> = callbackFlow {
        val registration = db.collection("groups").document(groupId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val messages = snapshot?.toObjects(ChatMessage::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(groupId: String, message: ChatMessage) {
        val msgRef = db.collection("groups").document(groupId).collection("messages").document()
        val finalMessage = message.copy(
            messageId = msgRef.id,
            timestamp = Timestamp.now()
        )
        msgRef.set(finalMessage).await()
    }

    // 1. Leader bắt đầu hiệp học
    suspend fun startFocusSession(groupId: String, preset: TimerPreset) {
        val updates = mapOf(
            "timerStatus" to "START",
            "timerSeconds" to preset.focusMin * 60,
            "breakSeconds" to preset.breakMin * 60,
            "startTime" to FieldValue.serverTimestamp(),
            "currentPresetTitle" to preset.title
        )
        db.collection("groups").document(groupId).update(updates).await()
    }

    // 2. Chuyển sang giờ nghỉ
    suspend fun startBreakSession(groupId: String) {
        db.collection("groups").document(groupId).update(
            mapOf(
                "timerStatus" to "BREAK",
                "startTime" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    // 3. Kết thúc vòng lặp & Cộng điểm (Personal + Group)
    suspend fun finalizeCycle(groupId: String, userId: String, focusMinutes: Int) {
        val batch = db.batch()
        
        // Cộng điểm cá nhân
        val userRef = db.collection("users").document(userId)
        batch.update(userRef, "totalDeepWorkHours", FieldValue.increment((focusMinutes / 60.0))) // Giả sử tính theo giờ hoặc phút tùy model

        // Cộng điểm nhóm Real-time
        val groupRef = db.collection("groups").document(groupId)
        batch.update(groupRef, "totalFocusMinutes", FieldValue.increment(focusMinutes.toLong()))
        batch.update(groupRef, "timerStatus", "WAITING")
        batch.update(groupRef, "startTime", null)

        batch.commit().await()
    }

    suspend fun updateGroupTimer(groupId: String, status: String, seconds: Int) {
        val updates: Map<String, Any?> = mapOf(
            "timerStatus" to status,
            "timerSeconds" to seconds,
            "startTime" to if (status == "START") Timestamp.now() else null
        )
        db.collection("groups").document(groupId).update(updates).await()
    }
}
