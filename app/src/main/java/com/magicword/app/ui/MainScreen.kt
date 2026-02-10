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

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    // Navigation items
    val items = listOf(
        Screen.Words,
        Screen.Search,
        Screen.Test,
        Screen.Library
    )

    // Current Screen State management for overlays (Settings, Logs)
    var currentOverlay by remember { mutableStateOf<String?>(null) } // null, "settings", "logs"

    if (currentOverlay == "settings") {
        SettingsScreen(
            onBack = { currentOverlay = null },
            onNavigateToLogs = { currentOverlay = "logs" }
        )
    } else if (currentOverlay == "logs") {
        LogListScreen(onBack = { currentOverlay = "settings" })
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    
                    items.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                                LogUtil.logFeature("TabSwitch", "Manual", "{ \"to\": \"${screen.label}\" }")
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController, 
                startDestination = Screen.Words.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Words.route) { 
                    WordsScreen(onOpenSettings = { currentOverlay = "settings" }) 
                }
                composable(Screen.Search.route) { 
                    SearchScreen() 
                }
                composable(Screen.Test.route) { 
                    TestScreen() 
                }
                composable(Screen.Library.route) { 
                    LibraryScreen() 
                }
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Words : Screen("words", "单词", Icons.Default.Book)
    object Search : Screen("search", "搜词", Icons.Default.Search)
    object Test : Screen("test", "测试", Icons.Default.CheckCircle)
    object Library : Screen("library", "词库", Icons.Default.List)
}
