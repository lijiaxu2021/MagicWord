# EasyWord (MagicWord)

> 🚀 **一款基于 Jetpack Compose 和 AI 驱动的现代化 Android 背词应用。**  
> 沉浸式学习体验，智能语义分析，让记单词变得简单而高效。

![Version](https://img.shields.io/badge/version-0.0.4-blue.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)
![Status](https://img.shields.io/badge/status-Active-success.svg)

[English Version](README_EN.md) | [中文版](README.md)

---

## 📖 简介

EasyWord 是一款结合了 **AI 智能**与 **SM-2 记忆算法**的背单词应用。它旨在帮助用户高效地构建词汇量，并通过科学的复习计划防止遗忘。版本 v0.0.3 引入了自动更新和更多人性化功能。

## ✨ 核心功能

### 1. 沉浸式单词学习 (Words Tab)
- **全屏卡片设计**：极简风格，专注于记忆本身。
- **手势交互**：左右滑动切换，点击查看详细解析。
- **AI 记忆法**：提供语境化的例句和词根词缀助记。

### 2. 智能 AI 查词 (Search Tab)
- **精准查询**：调用 Qwen-2.5-7B-Instruct 模型深度解析。
- **一键录入**：查词结果直接存入当前词库，自动去重。

### 3. AI 批量导入 (Library Tab)
- **长文本提取**：输入文章/段落，AI 自动提取生词和短语。
- **智能去重**：自动过滤已存在的单词。

### 4. 单词表 (WordList Tab) [New]
- **自定义列表**：创建个性化单词表（如“考研核心词”）。
- **多视图切换**：支持列表模式和紧凑表格模式（记忆状态持久化）。
- **快速跳转**：双击单词直接跳转至卡片学习模式。

### 5. 测试与复习 (Test Tab)
- **选择题模式**：SM-2 算法驱动的复习测试。
- **拼写模式**：强化拼写记忆。
- **数据统计**：记录每次测试的正确率和详情。

### 6. 系统功能 [New]
- **自动更新**：支持通过反代节点自动检查 GitHub Release 新版本。
- **关于页面**：内置详细使用指南和版本信息。
- **设置管理**：支持自定义 AI 模型参数和用户偏好 (Persona)。

---

## 🏗 项目结构

项目采用标准的 Android MVVM 架构：

```
com.magicword.app
├── MainActivity.kt          # 应用入口
├── data                     # 数据层 (Room Database)
├── network                  # 网络层 (Retrofit & AI API)
├── ui                       # UI 层 (Jetpack Compose)
│   ├── MainScreen.kt        # 主界面
│   ├── WordsScreen.kt       # 单词卡片
│   ├── SearchScreen.kt      # AI 搜词
│   ├── TestScreen.kt        # 测试
│   ├── WordListScreen.kt    # 单词表
│   ├── SettingsScreen.kt    # 设置
│   ├── AboutScreen.kt       # 关于
│   └── LibraryViewModel.kt  # 核心状态管理
├── utils                    # 工具类 (UpdateManager, LogUtil)
└── worker                   # 后台任务
```

---

## 🚀 快速开始

### 安装步骤

1.  **下载 APK**
    - **最新版本下载**: [MagicWord.apk (国内加速)](https://mag.upxuu.com/lijiaxu2011/MagicWord/releases/latest/download/MagicWord.apk)
    - **所有版本列表**: [查看发布页 (国内加速)](https://mag.upxuu.com/lijiaxu2011/MagicWord/releases)
    > 注意：如果直接下载链接无法访问，请点击“所有版本列表”选择最新版本下载。

2.  **配置 API Key**
    在“设置”页面填入您的 SiliconFlow API Key 即可开始使用 AI 功能。

---

## ⚙️ 构建与更新

本项目使用 GitHub Actions 自动构建 Release 版本。

- **版本号管理**：版本号由 `app/build.gradle.kts` 控制，自动同步到 Release Tag。
- **自动更新**：应用内置 `UpdateManager`，通过反代服务检查更新，确保国内网络可用。

---

## 👥 作者与贡献者

- **Author**: [lijiaxu2011](https://github.com/lijiaxu2011)
- **Contributor**: upxuu

## 📄 License

本项目采用 [MIT License](LICENSE) 许可证。
