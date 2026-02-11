package com.magicword.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.magicword.app.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于 EasyWord") },
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
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Header
            Text(
                text = "MagicWord / EasyWord",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "版本: 1.0.0 (Beta)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Introduction
            SectionTitle("简介")
            Text(
                text = "EasyWord 是一款结合了 AI 智能与 SM-2 记忆算法的背单词应用。它旨在帮助用户高效地构建词汇量，并通过科学的复习计划防止遗忘。",
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Tutorial / Guide
            SectionTitle("使用指南")
            
            GuideItem(
                title = "1. 智能录入",
                content = "在主界面的搜索框中输入任何英文单词或短语，AI 将自动分析并生成包含释义、例句和记忆方法的详细卡片。如果是新词，它会被自动保存到您的当前词库中。"
            )
            
            GuideItem(
                title = "2. 批量导入",
                content = "在“词库”页面，您可以粘贴一段长文本（如文章、字幕），AI 会提取其中的生词和短语，并批量生成卡片。"
            )
            
            GuideItem(
                title = "3. 记忆与复习",
                content = "系统采用 SM-2 算法安排复习。在“学习”页面，您会看到今日需要复习的单词。根据您的反馈（忘记、模糊、认识），系统会自动调整下次复习的时间。"
            )
            
            GuideItem(
                title = "4. 单词表",
                content = "您可以创建自定义的单词表（如“考研核心词汇”），将多个词库整合在一起。单词表支持多种视图（列表、表格），并支持双击快速跳转到卡片模式。"
            )
            
            GuideItem(
                title = "5. 测试模式",
                content = "支持“选择题”和“拼写”两种测试模式。您可以测试选中的单词，或让系统随机抽取。测试结果会被记录，帮助您分析薄弱环节。"
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Contact / Feedback
            SectionTitle("联系与反馈")
            Text(
                text = "如果您在使用过程中遇到问题或有任何建议，欢迎通过 GitHub Issues 反馈。",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Divider(modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun GuideItem(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
