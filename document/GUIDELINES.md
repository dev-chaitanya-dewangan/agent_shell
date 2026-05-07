# agentShell — Developer Guidelines

> Last updated: 2026-05-07 | Maintained by: AI Agent + Human Owner

---

## 1. Project Identity

```
App Name    : agentShell
Package     : dev.agentshell.app
Language    : 100% Kotlin
UI          : Jetpack Compose (Material 3)
Architecture: Clean Architecture + MVI
Min SDK     : 31 (Android 12)
Target SDK  : 35 (Android 15)
Design      : Brutalist — 0dp corner radius everywhere, JetBrains Mono font
```

---

## 2. Repository Structure

```
agent_shell/
├── app/
│   └── src/main/kotlin/dev/agentshell/app/
│       ├── agent/          # AgentLoopManager, ResponseParser, ToolDispatcher
│       ├── chat/           # ChatScreen, ChatViewModel, ChatMessage (MVI)
│       ├── data/
│       │   └── db/         # Room database, DAOs, Entities
│       ├── di/             # Hilt modules (AppModule)
│       ├── llm/            # LLMEngine interface + provider impls
│       ├── terminal/       # TerminalSession, TerminalScreen, TerminalViewModel
│       └── ui/
│           ├── components/ # Reusable ShellPanel, ShellButton, ShellInput, etc.
│           ├── nav/        # AppNavGraph, BottomNavBar, NavRoute
│           ├── splash/     # SplashScreen with ASCII art animation
│           └── theme/      # Color.kt, Typography.kt, Shape.kt, Theme.kt
├── document/               # THIS DIRECTORY — dev docs (you are here)
└── llm_state/
    └── state.yaml          # Machine-readable progress tracker for AI agents
```

---

## 3. Design System Rules (DO NOT BREAK THESE)

These rules define the app's identity. Breaking them breaks the brand.

| Rule | Detail |
|------|--------|
| **0dp corner radius** | ALL UI elements — no rounding. `RectangleShape` everywhere. |
| **JetBrains Mono only** | All monospaced text. Exception: chat markdown may use Roboto at 14sp. |
| **Min font size** | 9sp. Never go below this. |
| **Color palette** | Use only `AgentShellColors` tokens from `Color.kt`. No hardcoded hex values. |
| **Border widths** | Use `Borders` constants: `thin=0.5dp`, `standard=1dp`, `accent=2dp`, `thick=3dp` |
| **Spacing** | Use `Spacing` constants: `xs=4dp`, `sm=8dp`, `md=12dp`, `lg=16dp`, `xl=24dp` |
| **Dark only** | No light theme support. App is always dark. |

### Color Quick Reference

```
Shell0  (#1C0F09) = App background (espresso black)
Shell1  (#2D1810) = Cards, panels, bottom bar
Shell2  (#4A2C1E) = Elevated surfaces, input fields
Shell3  (#6B4030) = Borders, dividers
Amber   (#B89450) = Active accent, fills, nav active
Text0   (#F2E6CC) = Primary text (headings)
Text1   (#E8D4B0) = Body text
TermBg  (#0F0704) = Terminal background (darkest)
TermSys (#6A9A6A) = System / agent messages (green)
TermErr (#C45040) = Errors (muted red)
```

---

## 4. Architecture Rules

### MVI Pattern (applied to every screen)

```
User Action → Intent → ViewModel.onIntent() → State update → UI recompose
```

- **State**: `data class XxxState` — immutable, single source of truth
- **Intent**: `sealed class XxxIntent` — all user actions
- **ViewModel**: `@HiltViewModel class XxxViewModel @Inject constructor(...)` — never hold Android context!
- **Screen**: `@Composable fun XxxScreen(viewModel = hiltViewModel())` — only reads state, sends intents

### Clean Architecture Layers

```
UI (Compose) → ViewModel (Hilt) → UseCase → Repository interface
                                                      ↓
                                           Repository impl → Room / OkHttp / etc.
```

- UI layer: `ui/`, `chat/`, `terminal/` (Compose screens)
- Domain layer: `agent/`, `llm/` (pure Kotlin, no Android imports)
- Data layer: `data/` (Room, DataStore, network)
- DI: `di/AppModule.kt` (all singletons wired here)

### Threading Rules

| Thread | Use for |
|--------|---------|
| `Dispatchers.Main` | Compose recomposition, user input only |
| `Dispatchers.Default` | LLM inference, agent loop, heavy computation |
| `Dispatchers.IO` | Database, file I/O, network calls |

---

## 5. Adding a New LLM Provider

1. Create `llm/XxxEngine.kt` implementing `LLMEngine` interface.
2. Add `XXXXX` to `ProviderType` enum in `LLMEngine.kt`.
3. Add a branch to `AppModule.provideLLMEngine()`.
4. Add a card to the onboarding provider selector (future).

---

## 6. Adding a New Agent Tool

1. Add a new `when` branch in `ToolDispatcher.dispatch()`.
2. Register the tool name in the system prompt in `AgentLoopManager`.
3. Update `ResponseParser` if the new tool has a different XML schema.
4. Write a unit test in `AgentLoopManagerTest`.

---

## 7. Git Workflow

```
main        = stable, buildable at all times
feat/xxx    = feature branches (short-lived, merge via PR)
fix/xxx     = bug fix branches
docs/xxx    = documentation-only branches
```

### Commit Message Convention

```
feat:   new feature
fix:    bug fix
chore:  build/config changes (no user-visible change)
refactor: code restructure (no behavior change)
test:   adding or updating tests
docs:   documentation only
```

Example: `feat: add ChatScreen with agent loop MVI wiring`

---

## 8. API Keys and Secrets

**Never commit API keys to git.**

Set secrets in `local.properties` (already in `.gitignore`):
```properties
OPENROUTER_API_KEY=sk-or-v1-your-key-here
```

Keys are exposed to code via `BuildConfig` (generated at build time):
```kotlin
val key = BuildConfig.OPENROUTER_API_KEY
```

For CI/CD: inject secrets as GitHub Actions secrets → pass to Gradle via `gradleFlags`.

---

## 9. State Tracking

The file `llm_state/state.yaml` is a machine-readable progress file used by AI coding agents to resume work efficiently. Always update it after completing a milestone.

The file `document/CURRENT_STAGE.md` is a human-readable progress file. Update it with each meaningful commit.

---

## 10. Testing Standards

- Unit tests in `app/src/test/` — run without a device
- Instrumented tests in `app/src/androidTest/` — run on device/emulator
- Every `ViewModel` and `UseCase` must have unit tests
- Use `kotlinx-coroutines-test` and `TestDispatcher` for coroutine testing
- Mock `LLMEngine` with a stub that returns deterministic `Flow<String>`

---

## 11. Useful Commands

```powershell
# Build debug APK
.\gradlew assembleDebug

# Run all unit tests
.\gradlew test

# Install on connected device
.\gradlew installDebug

# Check for dependency updates
.\gradlew dependencyUpdates

# Lint check
.\gradlew lint
```

---

## 12. Known Limitations & Tech Debt

| Item | Severity | Notes |
|------|----------|-------|
| `TerminalViewModel` uses `AndroidViewModel` (not Hilt) | Low | Needs migration to `@HiltViewModel` |
| No proper PTY / ANSI color parsing | Medium | Uses ProcessBuilder — no VT100 yet |
| OpenRouterEngine uses `HttpURLConnection` | Low | Should be replaced with OkHttp |
| No error retry logic in agent loop | Medium | LLM calls fail silently |
| No onboarding flow | High | Phase 2 priority |

---

*This document is maintained by the development team. Update it as you add features.*
