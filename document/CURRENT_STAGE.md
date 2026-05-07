# agentShell — Current Stage & Build Status

> Auto-maintained by developer + AI agent. Update after each meaningful commit.
> Last updated: 2026-05-07

---

## Overall Progress

```
Phase 1 (Core Foundation)  ██████████████████░░  ~90% complete
Phase 2 (Refinement)       ████░░░░░░░░░░░░░░░░  ~20% started
Phase 3 (Polish)           ░░░░░░░░░░░░░░░░░░░░   0%
```

---

## ✅ Completed Features

### Phase 1 — Core Foundation

| Feature | File(s) | Commit |
|---------|---------|--------|
| Project scaffold & Gradle | `build.gradle.kts`, `settings.gradle.kts` | `bc83f28` |
| Theme system (Brutalist design) | `Color.kt`, `Theme.kt`, `Shape.kt`, `Typography.kt` | `bc83f28` |
| Bottom navigation | `AppNavGraph.kt`, `BottomNavBar.kt` | `5088da0` |
| Reusable UI components | `ShellPanel.kt`, `ShellButton.kt`, `ShellInput.kt`, `TierSelectorBar.kt` | `5088da0` |
| Terminal bridge | `TerminalSession.kt`, `TerminalScreen.kt`, `TerminalViewModel.kt` | `56f9098` |
| LLM interface + OpenRouter impl | `LLMEngine.kt`, `OpenRouterEngine.kt` | `b426d4e` |
| Agent loop (PLAN→ACT→OBSERVE) | `AgentLoopManager.kt`, `ResponseParser.kt`, `ToolDispatcher.kt` | `b426d4e` |
| Unit tests (agent + parser) | `AgentLoopManagerTest.kt`, `ResponseParserTest.kt` | `b426d4e` |
| **Hilt DI module** | `di/AppModule.kt` | _current_ |
| **Chat Screen (MVI)** | `chat/ChatScreen.kt`, `ChatViewModel.kt`, `ChatMessage.kt` | _current_ |
| **Splash Screen (ASCII art)** | `ui/splash/SplashScreen.kt` | _current_ |
| **Room DB (chat history)** | `data/db/AppDatabase.kt`, `*Entity.kt`, `*Dao.kt` | _current_ |
| **Updated build.gradle** (Room, Hilt Nav, BuildConfig) | `app/build.gradle.kts` | _current_ |

---

## 🔄 In Progress / Partial

| Feature | Status | Blocker |
|---------|--------|---------|
| Hilt DI on `TerminalViewModel` | Partial — still `AndroidViewModel` | Low priority, works |
| OkHttp for OpenRouterEngine | Not started | Using `HttpURLConnection` as stopgap |

---

## ❌ Not Yet Built

### Phase 1 — Remaining

| Feature | Description | Priority |
|---------|-------------|----------|
| Chat session persistence | Wire `ChatMessageDao` into `ChatViewModel` to save/load history | High |
| Settings screen | API key entry, provider selection | High |

### Phase 2 — Refinement

| Feature | Description | PRD Section |
|---------|-------------|-------------|
| Onboarding flow (7 screens) | Provider selection, permission requests, model download | §14 |
| Gemma 2B local model | MediaPipe `LlmInference` integration | §5.2, §6 |
| RAG memory | MiniLM embeddings + semantic search over chat history | §7 |
| Dynamic screen renderer | JSON → Compose DSL rendering engine | §10 |
| MarkdownMemory | `context.md` + `mistakes.md` append-only memory | §7 |
| Foreground service | `AgentShellService` — persist agent across config changes | §4.3 |
| Hamburger sidebar | 280dp drawer with session list | §3.4 |

### Phase 3 — Polish

| Feature | Description |
|---------|-------------|
| ASCII radar display | 8-spoke radial sensor visualization |
| ANSI/VT100 terminal parsing | Colored terminal output |
| Vertical bar charts | Block-char data visualization in panels |
| Level meters | `═══○···` style animated meters |
| FIGlet ASCII art engine | Dynamic text art from bundled fonts |

---

## Architecture At-a-Glance

```
UI Layer:
  SplashScreen ──► AppNavGraph ──┬──► TerminalScreen ◄── TerminalViewModel
                                  ├──► ChatScreen      ◄── ChatViewModel
                                  ├──► [AppsScreen]    (placeholder)
                                  └──► [SettingsScreen] (placeholder)

Agent Layer:
  ChatViewModel ──► AgentLoopManager ──► LLMEngine (OpenRouter)
                                     └──► ToolDispatcher ──► TerminalSession

Data Layer:
  AppDatabase ──► ChatSessionDao
             └──► ChatMessageDao

DI:
  AppModule (Hilt SingletonComponent) provides everything above
```

---

## Build Info

```
versionCode   : 2
versionName   : 0.2.0
minSdk        : 31
targetSdk     : 35
compileSdk    : 35
Kotlin        : 1.9.x
Compose BOM   : 2024.06.00
Room          : 2.6.1
Hilt          : 2.51.1
```

---

## Next Immediate Tasks (for next session)

1. **Wire chat session persistence** — `ChatViewModel` should call `ChatMessageDao.insert()` after each message, and load history on init for a given session ID.
2. **Settings screen** — allow user to enter OpenRouter API key, saved to `DataStore`.
3. **Hamburger nav drawer** — sidebar showing past chat sessions from Room DB.
4. **Onboarding flow** — 7-screen first-run experience with LLM provider selection.
5. **Foreground service** — `AgentShellService` so agent continues when app is in background.
