package com.magicword.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magicword.app.data.AppDatabase
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(database.wordDao())
    )
    val words by viewModel.allWords.collectAsState(initial = emptyList())

    // Mock User Data (Replace with real auth later)
    val username = "User"
    val email = "user@example.com"
    
    // Stats
    val totalWords = words.size
    val totalReviews = words.sumOf { it.reviewCount }
    val learnedWords = words.count { it.reviewCount > 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人中心") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar Placeholder
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.size(80.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = username.take(1).uppercase(),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(username, style = MaterialTheme.typography.headlineSmall)
            Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Stats Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("总单词", totalWords.toString())
                StatItem("已学习", learnedWords.toString())
                StatItem("复习次数", totalReviews.toString())
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Divider()
            Spacer(modifier = Modifier.height(16.dp))
            
            Text("同步日志", style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(8.dp))
            
            // Sync Logs Placeholder
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无同步记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { /* TODO: Logout */ },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("退出登录")
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
