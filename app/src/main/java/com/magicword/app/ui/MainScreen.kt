package com.magicword.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import com.magicword.app.utils.LogUtil

import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hierarchy

import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import com.magicword.app.ui.components.SlideInEntry
import androidx.compose.ui.platform.LocalContext
import com.magicword.app.utils.AuthManager

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.magicword.app.worker.SyncWorker
import kotlinx.coroutines.delay

import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.magicword.app.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Intent

import androidx.compose.material.icons.filled.School

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE) }
    var isLoggedIn by remember { mutableStateOf(AuthManager.isLoggedIn(context)) }

    // Observe current library name for title
    val database = AppDatabase.getDatabase(context)
    val viewModel: LibraryViewModel = viewModel(
        factory = LibraryViewModelFactory(database.wordDao(), prefs)
    )
    val libraries by viewModel.allLibraries.collectAsState(initial = emptyList())
    val currentLibraryId by viewModel.currentLibraryId.collectAsState()
    val currentLibraryName = libraries.find { it.id == currentLibraryId }?.name ?: "默认词库"

    // Bypass Auth for now
    // if (!isLoggedIn) {
    //     AuthScreen(onLoginSuccess = { isLoggedIn = true })
    //     return
    // }

    val pagerState = rememberPagerState(pageCount = { 4 }) // Updated count
    val scope = rememberCoroutineScope()
    
    // Navigation items
    val items = listOf(
        Screen.Study,
        Screen.Words,
        Screen.Test,
        Screen.WordList // Moved to end
    )

    // Observe Jump Event to switch tabs
    // MainScreen doesn't need to observe this if WordsScreen handles it.
    // WordsScreen now listens to pendingJumpWordId.
    // However, if we are on a different tab (e.g. Test), we might want to switch to Words tab.
    val pendingJumpWordId by viewModel.pendingJumpWordId.collectAsState()
    
    LaunchedEffect(pendingJumpWordId) {
        if (pendingJumpWordId != null) {
            pagerState.scrollToPage(1) // Switch to Words tab (Index 1)
        }
    }

    // Current Screen State management for overlays (Settings, Logs, Profile)
    var currentOverlay by remember { mutableStateOf<String?>(null) } // null, "settings", "logs", "profile"

    if (currentOverlay == "settings") {
        SettingsScreen(
            onBack = { currentOverlay = null },
            onNavigateToLogs = { currentOverlay = "logs" },
            onNavigateToAbout = { currentOverlay = "about" }
        )
    } else if (currentOverlay == "logs") {
        LogListScreen(onBack = { currentOverlay = "settings" })
    } else if (currentOverlay == "about") {
        AboutScreen(onBack = { currentOverlay = "settings" })
    } else if (currentOverlay == "profile") {
        ProfileScreen(
            onBack = { currentOverlay = null },
            onLogout = {
                isLoggedIn = false
                currentOverlay = null
            }
        )
    } else {
        Scaffold(
            // Removed global TopBar to prevent conflict with screen-specific TopBars
            bottomBar = {
                NavigationBar {
                    items.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                                LogUtil.logFeature("TabSwitch", "Manual", "{ \"to\": \"${screen.label}\" }")
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                userScrollEnabled = true
            ) { page ->
                SlideInEntry {
                    when (page) {
                        0 -> StudyScreen() // New Screen
                        1 -> WordsScreen(
                            onOpenSettings = { currentOverlay = "settings" },
                            onOpenProfile = { currentOverlay = "profile" },
                            onJumpToTest = {
                                scope.launch { pagerState.animateScrollToPage(2) } // Test is now index 2
                            }
                        )
                        2 -> TestScreen() // Test is now index 2
                        3 -> WordListScreen(viewModel) // WordList is now index 3
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Study : Screen("study", "学习", Icons.Default.School) // New Screen
    object Words : Screen("words", "词库", Icons.Default.Book)
    object Test : Screen("test", "测试", Icons.Default.CheckCircle)
    object WordList : Screen("list", "单词表", Icons.Default.List) // New Tab
}
