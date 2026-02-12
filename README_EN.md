# MagicWord / EasyWord

![Version](https://img.shields.io/badge/version-0.0.4-blue.svg)
![License](https://img.shields.io/badge/license-MIT-green.svg)
![Status](https://img.shields.io/badge/status-Active-success.svg)

---

## 📖 Introduction

EasyWord is a vocabulary learning application that combines **AI Intelligence** with the **SM-2 Memory Algorithm**. It is designed to help users efficiently build their vocabulary and prevent forgetting through scientific review schedules. Version v0.0.4 introduces automatic updates and more user-friendly features.

## ✨ Core Features

### 1. Immersive Word Learning (Words Tab)
- **Full-Screen Card Design**: Minimalist style, focusing on memory itself.
- **Gesture Interaction**: Swipe left/right to switch, tap to view detailed explanations.
- **AI Mnemonics**: Provides contextual example sentences and root/affix mnemonics.

### 2. Intelligent AI Lookup (Search Tab)
- **Precise Query**: Uses the Qwen-2.5-7B-Instruct model for deep analysis.
- **One-Click Entry**: Lookup results are directly saved to the current library with automatic deduplication.

### 3. AI Batch Import (Library Tab)
- **Long Text Extraction**: Input articles/paragraphs, and AI automatically extracts new words and phrases.
- **Smart Deduplication**: Automatically filters out existing words.

### 4. Word List (WordList Tab) [New]
- **Custom Lists**: Create personalized word lists (e.g., "Core GRE Vocabulary").
- **Multi-View Switching**: Supports list mode and compact table mode (view state persisted).
- **Quick Jump**: Double-tap a word to jump directly to card learning mode.

### 5. Test & Review (Test Tab)
- **Multiple Choice Mode**: Review tests driven by the SM-2 algorithm.
- **Spelling Mode**: Reinforce spelling memory.
- **Statistics**: Records accuracy and details of each test.

### 6. System Features [New]
- **Auto Update**: Supports automatic checking for new GitHub Release versions via proxy nodes.
- **About Page**: Built-in detailed user guide and version information.
- **Settings Management**: Supports custom AI model parameters and user persona.

---

## 🏗 Project Structure

The project follows the standard Android MVVM architecture:

```
com.magicword.app
├── MainActivity.kt          # App Entry
├── data                     # Data Layer (Room Database)
├── network                  # Network Layer (Retrofit & AI API)
├── ui                       # UI Layer (Jetpack Compose)
│   ├── MainScreen.kt        # Main Screen
│   ├── WordsScreen.kt       # Word Cards
│   ├── SearchScreen.kt      # AI Search
│   ├── TestScreen.kt        # Testing
│   ├── WordListScreen.kt    # Word List
│   ├── SettingsScreen.kt    # Settings
│   ├── AboutScreen.kt       # About
│   └── LibraryViewModel.kt  # Core State Management
├── utils                    # Utilities (UpdateManager, LogUtil)
└── worker                   # Background Tasks
```

---

## 🚀 Quick Start

### Installation

1.  **Download APK**
    - **Latest Version**: [MagicWord.apk (Fast Download)](https://mag.upxuu.com/lijiaxu2011/MagicWord/releases/latest/download/MagicWord.apk)
    - **All Versions**: [Release Page (Fast Access)](https://mag.upxuu.com/lijiaxu2011/MagicWord/releases)
    > Note: If the direct download link fails, please visit the "All Versions" page to download the latest release manually.

2.  **Configure API Key**
    Enter your SiliconFlow API Key in the "Settings" page to start using AI features.

---

## ⚙️ Build & Update

This project uses GitHub Actions to automatically build Release versions.

- **Version Management**: Controlled by `app/build.gradle.kts`, automatically synced to Release Tags.
- **Auto Update**: Built-in `UpdateManager` checks for updates via a reverse proxy service, ensuring availability in restricted network environments.

---

## 👥 Authors & Contributors

- **Author**: [lijiaxu2011](https://github.com/lijiaxu2011)
- **Contributor**: upxuu

## 📄 License

This project is licensed under the [MIT License](LICENSE).
