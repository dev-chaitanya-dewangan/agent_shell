# agentShell — Current Stage & Build Status

> Auto-maintained by developer + AI agent. Update after each meaningful commit.
> **Last updated: 2026-05-10**
> **Last build: ✅ BUILD SUCCESSFUL** (`assembleDebug`, 43 tasks, 0 errors)

---

## Overall Progress

```
Phase 1 (Core Foundation)  ████████████████████  ~95% complete
Phase 2 (Refinement)       ████░░░░░░░░░░░░░░░░  ~20% started
Phase 3 (Polish)           ░░░░░░░░░░░░░░░░░░░░   0%
```

---

## ✅ Completed Features

### Phase 1 — Core Foundation

| Feature | File(s) | Status |
|---------|---------|--------|
| Project scaffold & Gradle | `build.gradle.kts`, `settings.gradle.kts` | ✅ Done |
| Theme system (Brutalist design) | `ui/theme/Color.kt`, `Theme.kt`, `Shape.kt`, `Typography.kt` | ✅ Done |
| Bottom navigation (4 tabs) | `ui/nav/AppNavGraph.kt`, `BottomNavBar.kt` | ✅ Done |
| Reusable UI components | `ui/components/ShellPanel.kt`, `ShellButton.kt`, `ShellInput.kt` | ✅ Done |
| Splash screen (ASCII art) | `ui/splash/SplashScreen.kt` | ✅ Done |
| Terminal screen | `terminal/TerminalScreen.kt`, `TerminalViewModel.kt` | ✅ Done |
| Termux bridge (file+intent) | `agent/TermuxBridgeRepository.kt` | ✅ Done |
| LLM interface + OpenRouter | `llm/LLMEngine.kt`, `OpenRouterEngine.kt` | ✅ Done |
| Gemini REST engine | `llm/GeminiEngine.kt` | ✅ Done (parser fixed) |
| Dynamic engine switcher | `llm/DynamicLLMEngine.kt` | ✅ Done |
| Settings persistence (DataStore) | `data/settings/SettingsRepository.kt` | ✅ Done |
| Settings screen UI | `ui/settings/SettingsScreen.kt` | ✅ Done |
| Agent loop (PLAN→ACT→OBSERVE) | `agent/AgentLoopManager.kt` | ✅ Done |
| Tool dispatcher | `agent/ToolDispatcher.kt` | ✅ Done (DB wired) |
| Response parser | `agent/ResponseParser.kt` | ✅ Done (regex fixed) |
| Chat screen (MVI) | `chat/ChatScreen.kt`, `ChatViewModel.kt`, `ChatMessage.kt` | ✅ Done |
| Chat suggestion chips | `chat/ChatScreen.kt` (LazyRow at bottom) | ✅ Done |
| Hilt DI wiring | `di/AppModule.kt` | ✅ Done |
| Room DB | `data/db/AppDatabase.kt` (version 3) | ✅ Done |
| Mini-apps list screen | `miniapp/MiniAppsScreen.kt` | ✅ Done |
| Mini-app detail (WebView) | `miniapp/MiniAppDetailScreen.kt` | ✅ Done (reflection removed) |
| Mini-app ViewModel | `miniapp/MiniAppsViewModel.kt` | ✅ Done |
| Brain logger | `brain/BrainLogger.kt`, `BrainLogDao.kt`, `BrainLogEntity.kt` | ✅ Done |
| Hermes context builder | `brain/HermesContextBuilder.kt` | ✅ Done |
| Accessibility service | `accessibility/AgentAccessibilityService.kt` | ✅ Done |

---

## 🐛 Bugs Fixed This Session (2026-05-10)

> **CRITICAL — Read before touching these files.**
> These bugs were all silent failures (no crash, wrong behaviour). Fixed and verified in build.

### Bug 1 — `create_mini_app` tool never saved to DB (mini-apps invisible in APPS tab)

**Symptom:** User asks agent to "Create a Calculator Mini-App". Agent runs tools, shows success
in chat, but the APPS tab always shows "No mini-apps generated yet."

**Root cause:** `ToolDispatcher.kt` `create_mini_app` case was a `// TODO` stub:
```kotlin
"create_mini_app" -> {
    // TODO: Database insertion    ← was just this
    emit("[Mini app created]")    ← fake success, nothing saved
}
```

**Fix applied:**
- `ToolDispatcher` now has `MiniAppDao` and `@ApplicationContext Context` injected
- Creates directory: `context.filesDir/mini_apps/<uuid>/`
- Writes `index.html` (from `params["html"]`) to that directory
- Calls `miniAppDao.insert(MiniAppEntity(...))` → Flow in `MiniAppsScreen` updates live
- `AppModule.provideToolDispatcher()` updated to pass the two new deps

**Files changed:** `agent/ToolDispatcher.kt`, `di/AppModule.kt`

---

### Bug 2 — `MiniAppDetailScreen` used reflection to access private DAO field

**Symptom:** Detail screen sometimes showed blank/loading forever, especially after Hilt obfuscation.

**Root cause:**
```kotlin
// DANGEROUS — breaks with Hilt, proguard, Kotlin name mangling
val dao = viewModel.javaClass.getDeclaredField("dao")
    .apply { isAccessible = true }.get(viewModel) as? MiniAppDao
```

**Fix applied:**
- Added `suspend fun getById(id: String): MiniAppEntity? = dao.getById(id)` to `MiniAppsViewModel`
- `MiniAppDetailScreen` now calls `viewModel.getById(appId)` directly
- Added proper loading state (shows "Loading...") and file-not-found error state

**Files changed:** `miniapp/MiniAppsViewModel.kt`, `miniapp/MiniAppDetailScreen.kt`

---

### Bug 3 — `MiniAppsViewModel` used `SharingStarted.Lazily` (list didn't refresh on tab re-enter)

**Symptom:** After a mini-app was created and the user navigated away then back to APPS tab,
the list didn't update because `Lazily` never restarts the upstream Flow.

**Fix:** Changed to `SharingStarted.WhileSubscribed(5_000)` so the Flow re-collects
from Room whenever the screen is active.

**File changed:** `miniapp/MiniAppsViewModel.kt`

---

### Bug 4 — Gemini Flash Preview model returns 404

**Symptom:** Any chat with Gemini selected shows `[LLM Error: ...]` immediately.

**Root cause:** `SettingsRepository` default was `gemini-1.5-flash-preview-0514` —
a retired preview model that Google has removed from the API.

**Fix:** Default model changed to `gemini-2.0-flash` (free tier, GA, works reliably).

**File changed:** `data/settings/SettingsRepository.kt`

**Free-tier Gemini models that work (v1beta endpoint):**
| Model ID | Notes |
|----------|-------|
| `gemini-2.0-flash` | ⭐ **Default** — fast, free quota, GA |
| `gemini-2.0-flash-lite` | Lightest/cheapest |
| `gemini-1.5-flash` | Stable, generous free quota |
| `gemini-1.5-flash-8b` | Small 8B variant |
| `gemini-1.5-pro` | Best quality, limited free quota |
| `gemini-2.5-flash-preview-04-17` | Preview only — avoid in prod |
| `gemini-2.5-pro-preview-05-06` | Preview only — avoid in prod |

---

### Bug 5 — `GeminiEngine` streaming parser missed most of the response text

**Symptom:** Gemini responses were either empty or only showed the first line of text.

**Root cause:** Parser used fragile line-scan:
```kotlin
if (l.startsWith("\"text\": \"")) {  // breaks on multi-line JSON, whitespace variants
```

The Gemini `streamGenerateContent` endpoint returns a JSON array:
```json
[{"candidates":[{"content":{"parts":[{"text":"..."}]}}]}, ...]
```

**Fix:** Replaced with proper `JSONArray` traversal:
```kotlin
val chunks = JSONArray(body)
// chunks[i].candidates[ci].content.parts[pi].text
```
HTTP error body is now also surfaced to UI as `[LLM Error 4xx]: <body>`.

**File changed:** `llm/GeminiEngine.kt`

---

### Bug 6 — `ResponseParser` greedy regex collapsed tool params / threw on HTML content

**Symptom:** Agent ran `create_mini_app` but params were empty — LLM's HTML content was lost.
`getString()` threw `JSONException` on any non-primitive param value.

**Root cause:**
1. `Regex("\\{.*\"tool\".*\\}", DOT_MATCHES_ALL)` — greedy, collapses everything between
   first `{` and last `}` in entire response, breaks on multi-paragraph LLM output.
2. `paramsObj.getString(key)` — throws if the value is a JSONObject/JSONArray,
   which happens when HTML content has escaped braces inside a JSON string.

**Fix:** Replaced with `findJsonToolCall()` — a brace-balanced character scanner that:
- Walks char-by-char, tracks nesting depth, skips string literals
- Tries `JSONObject(candidate)` on each balanced block
- Uses `paramsObj.opt(key)?.toString()` instead of `getString()` — never throws

**File changed:** `agent/ResponseParser.kt`

---

### Bug 7 — `AgentLoopManager` system prompt had no param schemas for tools

**Symptom:** LLM would call `create_mini_app` with no params (or wrong keys like `app_name`).

**Root cause:** System prompt only listed tool names, not their JSON schemas.

**Fix:** System prompt now documents every tool with its exact JSON format, especially:
```json
{"tool": "create_mini_app", "params": {
    "name": "<display name>",
    "description": "<one line description>",
    "html": "<full self-contained HTML string>"
}}
```

**File changed:** `agent/AgentLoopManager.kt`

---

## Architecture At-a-Glance (current, accurate)

```
UI Layer:
  SplashScreen ──► AppNavGraph ──┬──► TerminalScreen   ◄── TerminalViewModel
                                  ├──► ChatScreen        ◄── ChatViewModel
                                  │     └── SuggestionChips (LazyRow, bottom)
                                  ├──► MiniAppsFlow      ◄── MiniAppsViewModel
                                  │     ├── MiniAppsScreen   (list from Room Flow)
                                  │     └── MiniAppDetailScreen (WebView, file://)
                                  └──► SettingsScreen    ◄── SettingsViewModel

Agent Layer:
  ChatViewModel ──► AgentLoopManager ──► LLMEngine (DynamicLLMEngine)
                                     │     ├── GeminiEngine   (v1beta REST, JSONArray parser)
                                     │     └── OpenRouterEngine (REST)
                                     └──► ToolDispatcher ──► TermuxBridgeRepository
                                               └── create_mini_app ──► MiniAppDao.insert()
                                                                   └── filesDir/mini_apps/<uuid>/index.html

Data Layer:
  Room AppDatabase (v3) ──► ChatSessionDao
                        ├──► ChatMessageDao
                        ├──► BrainLogDao
                        └──► MiniAppDao  ←─ written by ToolDispatcher, read by MiniAppsViewModel

Settings:
  DataStore<Preferences> ──► SettingsRepository ──► DynamicLLMEngine
    Keys: provider_type, gemini_api_key, gemini_model,
          openrouter_api_key, openrouter_model, *_custom_models

DI:
  AppModule (Hilt SingletonComponent) — see di/AppModule.kt for full wiring
  ToolDispatcher: needs TermuxBridgeRepository + MiniAppDao + @ApplicationContext Context
```

---

## Mini-App Lifecycle (end-to-end)

```
1. User types "Create a Calculator Mini-App" in ChatScreen
2. ChatViewModel → AgentLoopManager.run(task)
3. LLMEngine called with system prompt that includes create_mini_app JSON schema
4. LLM responds: {"tool":"create_mini_app","params":{"name":"...","html":"<!DOCTYPE html>..."}}
5. ResponseParser.findJsonToolCall() extracts ToolCall("create_mini_app", params)
6. ToolDispatcher.dispatch("create_mini_app", params):
     a. Creates: filesDir/mini_apps/<uuid>/index.html
     b. Writes HTML content to file
     c. Calls miniAppDao.insert(MiniAppEntity(...))
7. MiniAppsScreen observes Flow<List<MiniAppEntity>> via MiniAppsViewModel.miniApps
     → Card appears in APPS tab immediately (WhileSubscribed)
8. User taps card → MiniAppDetailScreen.getById(id) → WebView.loadUrl("file://...index.html")
```

---

## Known Patterns & Conventions (follow these)

- **All screens** use `hiltViewModel()`, never construct ViewModels manually
- **Theme colors** always use `AgentShellColors.*` (never hardcode hex in composables)
- **Typography** always use `AgentShellTypography.*`
- **Spacing** use `Spacing.sm/md/lg` from `ui/components/Spacing`
- **Room DAOs** injected via Hilt — never call `AppDatabase.getInstance()` directly in ViewModels
- **File I/O in tools** always use `context.filesDir` as base, never hardcode `/sdcard` paths
  (exception: Termux bridge uses `/sdcard/Download/agentshell/bridge` intentionally)
- **Coroutine context** — ToolDispatcher `flow { }` runs in caller's context; IO-bound work
  (file writes) is fast enough on Dispatchers.Default; network calls use `.flowOn(Dispatchers.IO)`
- **No reflection** anywhere in the codebase — if you need to access a ViewModel field, add a
  public method/property instead

---

## 🔄 In Progress / Partial

| Feature | Status | Notes |
|---------|--------|-------|
| Chat session persistence | Partial | `ChatMessageDao` exists, not wired into `ChatViewModel` |
| Hilt DI on `TerminalViewModel` | Partial | Still `AndroidViewModel`, works fine |
| OkHttp for `OpenRouterEngine` | Not started | Using `HttpURLConnection` as stopgap |

---

## ❌ Not Yet Built

### Phase 1 — Remaining

| Feature | Description | Priority |
|---------|-------------|----------|
| Chat session persistence | Wire `ChatMessageDao` into `ChatViewModel` — save each message, load history on init | **HIGH** |
| Hamburger nav drawer | 280dp sidebar showing past chat sessions from Room | **HIGH** |

### Phase 2 — Refinement

| Feature | Description | PRD Ref |
|---------|-------------|---------|
| Onboarding flow (7 screens) | Provider selection, permission requests, model download | §14 |
| Gemma 2B local model | MediaPipe `LlmInference` integration | §5.2, §6 |
| RAG memory | MiniLM embeddings + semantic search over chat history | §7 |
| Dynamic screen renderer | JSON → Compose DSL rendering engine | §10 |
| MarkdownMemory | `context.md` + `mistakes.md` append-only memory | §7 |
| Foreground service | `AgentShellService` — persist agent across config changes | §4.3 |

### Phase 3 — Polish

| Feature | Description |
|---------|-------------|
| ASCII radar display | 8-spoke radial sensor visualization |
| ANSI/VT100 terminal parsing | Colored terminal output |
| Vertical bar charts | Block-char data visualization in panels |
| FIGlet ASCII art engine | Dynamic text art from bundled fonts |

---

## Build Info

```
versionCode   : 2
versionName   : 0.2.0
minSdk        : 31
targetSdk     : 35
compileSdk    : 35
Kotlin        : 1.9.x  (KAPT — falls back from 2.0, warning is safe to ignore)
Compose BOM   : 2024.06.00
Room          : 2.6.1  (DB version = 3, fallbackToDestructiveMigration in dev mode)
Hilt          : 2.51.1
AGP           : 8.5.0  (compileSdk=35 warning is safe to ignore)
```

---

## Next Immediate Tasks (for next session)

1. **Wire chat session persistence** — `ChatViewModel` should call `ChatMessageDao.insert()`
   after each message and load history on init for the current session ID.
   Entity is already in Room, DAO is already provided by Hilt — just needs ViewModel wiring.

2. **Hamburger nav drawer** — Scaffold-level drawer (280dp) showing `ChatSessionDao.getAll()`
   as a Flow. Tapping a session should load its messages into the Chat screen.

3. **Onboarding flow** — 7-screen first-run experience (check PRD §14):
   LLM provider selection → API key entry → permission grants → completion.
   Only shown when `DataStore` has no `provider_type` set.

4. **Foreground service** — `AgentShellService` so the agent loop continues when the app
   is backgrounded. Bind `AgentLoopManager` to the service lifecycle.

5. **`open_app` tool** — Currently a TODO stub. Implement with `Intent(Intent.ACTION_MAIN)`
   + `addCategory(Intent.CATEGORY_LAUNCHER)` using the `package` param from tool call.
