# agentShell System Design

## 1. Architecture Overview

**Core Stack:**
- Pure Kotlin
- Jetpack Compose
- Coroutines & Flow
- Room DB
- Hilt (Dependency Injection)

**Architectural Pattern:** Clean Architecture + MVI (Model-View-Intent)

### 1.1 Layer Breakdown

1.  **UI Layer (Compose):**
    *   `Screen.kt` + `ScreenViewModel.kt` + `ScreenState.kt`
    *   State is managed as `StateFlow<ScreenState>`.
    *   Events are handled via one-time `Channel<ScreenEvent>`.
    *   User actions are modeled as `ScreenIntent`.
2.  **Domain Layer (Use Cases):**
    *   Pure Kotlin, no Android dependencies.
    *   Single responsibility use cases (e.g., `RunAgentTaskUseCase`, `ExecuteTerminalCommandUseCase`).
3.  **Data Layer (Repositories):**
    *   Interface boundaries defined in the domain layer, implementation injected from the data layer.
    *   Examples: `ChatRepository`, `AgentRepository`, `TerminalRepository`.
4.  **Infrastructure Layer:**
    *   Database (Room, SQLite-Vec for vectors).
    *   Hardware / Low-level: MediaPipe (LLM), ProcessBuilder, SensorManager.
    *   System Services: AlarmManager, WorkManager, OkHttp, FileObserver.

### 1.2 Foreground Service Architecture

The app runs essentially as a permanent host process encapsulated in `AgentShellService`.
*   Starts via `BOOT_COMPLETED` and `startForegroundService`.
*   Hosts singleton managers: `AgentLoopManager`, `LLMEngineManager`, `TerminalSessionManager`, `ScreenWatcher`.
*   Uses a `ServiceScope` (SupervisorJob + Dispatchers.Default) to ensure background processes (LLM warm-up, screen observing) survive UI recreation.

### 1.3 Concurrency & Chunked Processing

*   **Main Thread (`Dispatchers.Main`):** Strictly for Compose recomposition and UI events.
*   **Default (`Dispatchers.Default`):** LLM inference, RAG embeddings, Agent loops.
*   **IO (`Dispatchers.IO`):** File/DB reads and network requests. Terminal process builder is limited here.
*   **Conflict Prevention:** Mutex on the Agent Loop to prevent parallel loops, Job cancellation for LLM streaming.

## 2. Terminal System Architecture

The Terminal acts as the "hands" of the agent and a real Linux environment for the user.

*   **UI (`TerminalScreen.kt`):** A custom Canvas renderer drawing text cells (not a WebView) for maximum performance. Retains up to 10,000 lines scrollback buffer.
*   **Emulator (`TerminalEmulator.kt`):** VT100/VT220 compatible. Handles ANSI escape codes.
*   **Session (`TerminalSession.kt`):** Uses `pty4j` to bridge UI and native file descriptors (`/dev/pts/*`).
*   **Environment (`proot` + `bash`):** Runs a statically linked ARM64 binary structure with isolated `HOME`, `PREFIX`, and `PATH` mimicking a typical Linux setup (Termux compatible).

## 3. Dependency Injection (Hilt)

Modules are clearly separated:
*   `LLMModule`
*   `DatabaseModule`
*   `RepositoryModule`
*   `UseCaseModule`

No manual DI is allowed. All singletons are managed by Hilt.

## 4. Module & File Structure

```
app/src/main/kotlin/dev/agentshell/
  ├── ui/               # Compose screens and reusable components
  ├── domain/           # Models, UseCases, Repo interfaces
  ├── agent/            # AgentLoopManager, Hooks, ToolRegistry
  ├── llm/              # Engine implementations (Local, OpenRouter, etc)
  ├── terminal/         # Session management, PkgManager, Emulator
  ├── memory/           # Markdown memory, Vector index, RAG
  ├── screens/          # Dynamic DSL rendering system
  ├── chat/             # Chat session persistence
  ├── service/          # Foreground service, BootReceiver
  ├── data/             # Room DB, DataStore
  └── di/               # Hilt modules
```
