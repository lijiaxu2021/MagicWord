package com.magicword.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import androidx.compose.ui.platform.LocalContext
import com.magicword.app.data.AppDatabase

import com.magicword.app.data.Word

import androidx.compose.runtime.LaunchedEffect
import com.magicword.app.utils.LogUtil

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen() {
    val context = LocalContext.current
    val database = AppDatabase.getDatabase(context)
    val viewModel: SearchViewModel = viewModel(
        factory = SearchViewModelFactory(database.wordDao())
    )
    var query by remember { mutableStateOf("") }
    val result by viewModel.searchResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val prefs = remember { context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE) }
    val currentLibraryId = prefs.getInt("current_library_id", 1)

    // Auto-save when result is ready
    LaunchedEffect(result) {
        result?.let { word ->
            viewModel.saveWord(word, libraryId = currentLibraryId)
            LogUtil.logFeature("AutoSave", "Success", "{ \"word\": \"${word.word}\", \"libraryId\": $currentLibraryId }")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("输入单词") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = { viewModel.searchWord(query) }) {
                        Icon(Icons.Default.Search, contentDescription = "搜索")
                    }
                }
            },
            singleLine = true,
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onDone = { viewModel.searchWord(query) }
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        result?.let { word ->
            Text(
                text = "查询结果 (已自动保存)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            // Show Card without flip animation for search result, just the content
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                WordDetailContent(word = word)
            }
        }
    }
}
