package com.example.dacs3.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.dacs3.model.ChatGroup
import com.example.dacs3.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class LeaderboardViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    var leaderboardType by mutableStateOf("INDIVIDUAL") // "INDIVIDUAL" or "GROUP"
    
    var topUsers by mutableStateOf<List<User>>(emptyList())
    var topGroups by mutableStateOf<List<ChatGroup>>(emptyList())
    
    var currentUserRank by mutableIntStateOf(0)
    var currentUserPoints by mutableIntStateOf(0)
    
    var isLoading by mutableStateOf(false)

    init {
        loadLeaderboard()
    }

    fun loadLeaderboard() {
        isLoading = true
        if (leaderboardType == "INDIVIDUAL") {
            fetchTopUsers()
        } else {
            fetchTopGroups()
        }
    }

    private fun fetchTopUsers() {
        db.collection("users")
            .orderBy("totalFocusMinutes", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }
                val users = snapshot?.toObjects(User::class.java) ?: emptyList()
                topUsers = users
                
                // Find current user rank
                val currentUserId = auth.currentUser?.uid
                val index = users.indexOfFirst { it.id == currentUserId }
                currentUserRank = if (index != -1) index + 1 else 0
                currentUserPoints = users.find { it.id == currentUserId }?.totalFocusMinutes ?: 0
                
                isLoading = false
            }
    }

    private fun fetchTopGroups() {
        db.collection("groups")
            .orderBy("totalFocusMinutes", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoading = false
                    return@addSnapshotListener
                }
                val groups = snapshot?.toObjects(ChatGroup::class.java) ?: emptyList()
                topGroups = groups
                isLoading = false
            }
    }
}
