package com.example.dacs3.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dacs3.model.ChatGroup
import com.example.dacs3.model.User
import com.example.dacs3.ui.theme.*
import com.example.dacs3.viewmodel.LeaderboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    userName: String? = null,
    userAvatarUrl: String? = null,
    userAvatarInitial: String = "U",
    onMenuClick: () -> Unit = {},
    viewModel: LeaderboardViewModel = viewModel()
) {
    val leaderboardType = viewModel.leaderboardType
    val topUsers = viewModel.topUsers
    val topGroups = viewModel.topGroups
    val isLoading = viewModel.isLoading

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (leaderboardType == "INDIVIDUAL") {
                CurrentUserRankBar(
                    user = User(
                        fullName = userName ?: "You",
                        avatarUrl = userAvatarUrl ?: "",
                        totalFocusMinutes = viewModel.currentUserPoints
                    ),
                    rank = viewModel.currentUserRank
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            // Header: Menu, Title, Avatar
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Default.Menu, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text = "Leaderboard",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                UserAvatar(
                    imageUrl = userAvatarUrl,
                    initial = userAvatarInitial,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Toggle INDIVIDUAL vs GROUP
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp)
            ) {
                LeaderboardTypeButton(
                    modifier = Modifier.weight(1f),
                    title = "Users",
                    isSelected = leaderboardType == "INDIVIDUAL",
                    icon = Icons.Default.Person,
                    onClick = { viewModel.leaderboardType = "INDIVIDUAL"; viewModel.loadLeaderboard() }
                )
                LeaderboardTypeButton(
                    modifier = Modifier.weight(1f),
                    title = "Groups",
                    isSelected = leaderboardType == "GROUP",
                    icon = Icons.Default.Groups,
                    onClick = { viewModel.leaderboardType = "GROUP"; viewModel.loadLeaderboard() }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    val displayList = if (leaderboardType == "INDIVIDUAL") topUsers else topGroups

                    if (displayList.isNotEmpty()) {
                        // Podium Section
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 8.dp)
                            ) {
                                if (leaderboardType == "INDIVIDUAL") {
                                    TopThreePodium(users = topUsers)
                                } else {
                                    TopThreePodiumGroups(groups = topGroups)
                                }
                            }
                        }

                        // List Header
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Ranking",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Global Rank", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    Icon(
                                        Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Other Ranks
                        if (leaderboardType == "INDIVIDUAL") {
                            itemsIndexed(topUsers.drop(3)) { index, user ->
                                RankItem(rank = index + 4, name = user.fullName, points = user.totalFocusMinutes, avatarUrl = user.avatarUrl, subtitle = user.title)
                            }
                        } else {
                            itemsIndexed(topGroups.drop(3)) { index, group ->
                                RankItem(rank = index + 4, name = group.name, points = group.totalFocusMinutes, avatarUrl = group.avatarUrl, subtitle = "${group.members.size} members")
                            }
                        }
                    } else {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardTypeButton(
    modifier: Modifier,
    title: String,
    isSelected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(40.dp)
            .clickable { onClick() },
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TopThreePodium(users: List<User>) {
    if (users.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (users.size > 1) PodiumMember(name = users[1].fullName, points = users[1].totalFocusMinutes, avatarUrl = users[1].avatarUrl, rank = 2, avatarSize = 82.dp, color = RankSilver, modifier = Modifier.weight(1f))
        else Spacer(modifier = Modifier.weight(1f))

        if (users.size > 0) PodiumMember(name = users[0].fullName, points = users[0].totalFocusMinutes, avatarUrl = users[0].avatarUrl, rank = 1, avatarSize = 114.dp, color = RankGold, modifier = Modifier.weight(1.2f).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).padding(vertical = 16.dp))
        
        if (users.size > 2) PodiumMember(name = users[2].fullName, points = users[2].totalFocusMinutes, avatarUrl = users[2].avatarUrl, rank = 3, avatarSize = 72.dp, color = RankBronze, modifier = Modifier.weight(1f))
        else Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun TopThreePodiumGroups(groups: List<ChatGroup>) {
    if (groups.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        if (groups.size > 1) PodiumMember(name = groups[1].name, points = groups[1].totalFocusMinutes, avatarUrl = groups[1].avatarUrl, rank = 2, avatarSize = 82.dp, color = RankSilver, modifier = Modifier.weight(1f), isGroup = true)
        else Spacer(modifier = Modifier.weight(1f))

        if (groups.size > 0) PodiumMember(name = groups[0].name, points = groups[0].totalFocusMinutes, avatarUrl = groups[0].avatarUrl, rank = 1, avatarSize = 114.dp, color = RankGold, modifier = Modifier.weight(1.2f).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)).padding(vertical = 16.dp), isGroup = true)
        
        if (groups.size > 2) PodiumMember(name = groups[2].name, points = groups[2].totalFocusMinutes, avatarUrl = groups[2].avatarUrl, rank = 3, avatarSize = 72.dp, color = RankBronze, modifier = Modifier.weight(1f), isGroup = true)
        else Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun PodiumMember(
    name: String,
    points: Int,
    avatarUrl: String,
    rank: Int,
    avatarSize: androidx.compose.ui.unit.Dp,
    color: Color,
    modifier: Modifier = Modifier,
    isGroup: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.padding(bottom = 4.dp)) {
            Box(
                modifier = Modifier
                    .padding(top = if (rank == 1) 20.dp else 0.dp)
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.25f))
                    .padding(3.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(2.dp)
            ) {
                if (isGroup) {
                    GroupAvatar(imageUrl = avatarUrl, modifier = Modifier.fillMaxSize())
                } else {
                    UserAvatar(imageUrl = avatarUrl, initial = name.take(1).ifEmpty { "U" }, modifier = Modifier.fillMaxSize())
                }
            }

            if (rank == 1) {
                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = RankGold, modifier = Modifier.size(34.dp).offset(y = (-4).dp))
            }

            Surface(
                color = color,
                shape = CircleShape,
                modifier = Modifier.align(Alignment.BottomCenter).offset(y = 10.dp).size(24.dp),
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "$rank", color = if (rank == 1) MaterialTheme.colorScheme.primary else Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(text = name, fontSize = if (rank == 1) 15.sp else 13.sp, fontWeight = if (rank == 1) FontWeight.ExtraBold else FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 4.dp))
        Text(text = "$points pts", fontSize = 12.sp, color = if (rank == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (rank == 1) FontWeight.Black else FontWeight.Bold)
    }
}

@Composable
fun RankItem(rank: Int, name: String, points: Int, avatarUrl: String, subtitle: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shadowElevation = 0.5.dp
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = String.format("%02d", rank), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(36.dp))
            UserAvatar(imageUrl = avatarUrl, initial = name.take(1).ifEmpty { "U" }, modifier = Modifier.size(42.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = "$points", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, letterSpacing = (-0.5).sp)
                Text(text = " pts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 1.dp, bottom = 1.dp))
            }
        }
    }
}

@Composable
fun CurrentUserRankBar(user: User, rank: Int) {
    if (rank == 0) return
    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "#$rank", color = RankGold, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(55.dp))
            UserAvatar(imageUrl = user.avatarUrl, initial = user.fullName.take(1).ifEmpty { "U" }, modifier = Modifier.size(44.dp).background(Color.White.copy(alpha = 0.15f), CircleShape).padding(2.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = user.fullName, color = MaterialTheme.colorScheme.onPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                Text(text = "Keep it up! 🚀", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${user.totalFocusMinutes}", color = MaterialTheme.colorScheme.onPrimary, fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                Text(text = "TOTAL PTS", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            }
        }
    }
}
