# AgentShell: Project Status & Features Guide

This document maintains a living record of what has been implemented, what remains on the roadmap, and what capabilities are currently available on each screen.

## 🚀 Features Added

### Core Architecture
- **Hilt Dependency Injection**: Singletons for `LLMEngine`, `TerminalSession`, `ToolDispatcher`, and Room DAOs.
- **Room Database**: Infrastructure set up with `AppDatabase`, `ChatSessionEntity`, and `ChatMessageEntity`.
- **DataStore Preferences**: Local key-value storage for User Settings (API keys, models, provider selection).
- **PowerShell TUI**: `run_build_test.ps1` script for streamlined physical device deployment via ADB.

### AI Integration
- **Google Gemini Engine**: Direct REST API integration targeting `gemini-1.5-flash-preview` for daily free quota.
- **OpenRouter Engine**: Support for routing through OpenRouter APIs.
- **Dynamic Provider Switching**: Instantly switch between engines based on DataStore preferences via `DynamicLLMEngine`.

### UI & UX (Brutalist Design)
- **Settings Screen**: Full UI to configure providers, input API keys, and dynamically add custom models via CSV.
- **Splash Screen**: Typewriter ASCII art animation on cold boot.
- **Terminal Screen**: Transitioned to a persistent, stateful background `sh` process (commands like `cd /sdcard` carry over state).
- **Chat Screen**: MVI-based interface featuring live token streaming and agent loop status visualization.

---

## 📱 Screen Capabilities

| Screen | Location | What you can do |
| :--- | :--- | :--- |
| **Splash** | Launch | Watch the boot sequence and ASCII animation. |
| **Terminal** | `NavRoute.SHELL` | Execute local Android shell commands. Session is stateful, so `cd` and `export` commands persist. Includes mock aliases for `pkg` to gracefully handle unsupported commands. |
| **Chat** | `NavRoute.CHAT` | Interact with the agent, issue natural language commands, and watch the agent autonomously execute tools (like `run_shell` or `write_file`) in real-time. |
| **Apps** | `NavRoute.APPS` | *(Coming Soon)* This will house dynamic "mini-apps" (like hydration trackers or sensors) rendered via JSON DSL. |
| **Settings** | `NavRoute.SETTINGS` | Configure the app. Choose between Google Gemini or OpenRouter, securely input API keys, and add custom models to your dropdown list via comma-separated text. |

---

## 🚧 What's Left (Backlog)

### High Priority
1. **Chat History Persistence**: Wire the existing `ChatMessageDao` into the `ChatViewModel` so sessions persist across app restarts.
2. **Onboarding Flow**: Implement the 7-screen onboarding sequence outlined in the PRD (warnings, downloads, pings).
3. **Hamburger Navigation**: Add the navigation drawer for quick access to specific chat sessions and system metrics.

### Mid-Term / Core Features
1. **Dynamic Screen Renderer**: Implement the parser and UI components to dynamically render "mini-apps" based on the JSON DSL.
2. **Foreground Service**: Implement `AgentShellService` so the agent and terminal can continue running even when the app is backgrounded.
3. **Tool Upgrades**: Improve the `ToolDispatcher` with more complex tool definitions and proper JSON-schema based argument parsing.

### Technical Debt
1. **Networking Migration**: Replace `HttpURLConnection` in the `GeminiEngine` and `OpenRouterEngine` with `OkHttp` for better connection pooling, interceptors, and robust streaming.
2. **File Sandboxing**: Ensure all agent tool file-writes strictly adhere to the `PathSandboxHook` to prevent dangerous out-of-bounds file modifications.
