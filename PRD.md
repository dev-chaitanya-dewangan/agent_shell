# agentShell — Product Requirements Document v2.0
**Stack:** Pure Kotlin + Jetpack Compose  
**Min SDK:** 31 (Android 12)  
**Target SDK:** 35 (Android 15)  
**Architecture:** Clean Architecture + MVI  
**Primary Device:** Pixel 6a (6 GB RAM, Tensor G2) — generalizes to all Android 12+ phones  
**No Root Required**  
**Last Updated:** 2026-05-02

---

## TABLE OF CONTENTS

1. [App Identity](#1-app-identity)
2. [UI Reference — Image Analysis](#2-ui-reference--image-analysis)
3. [Design System](#3-design-system)
4. [Architecture Overview](#4-architecture-overview)
5. [LLM Provider System](#5-llm-provider-system)
6. [Gemma Model Management](#6-gemma-model-management)
7. [Agent Architecture — Claude Code Loop](#7-agent-architecture--claude-code-loop)
8. [Agent Tier System](#8-agent-tier-system)
9. [Terminal System — Termux Compatible](#9-terminal-system--termux-compatible)
10. [Dynamic Screen Renderer](#10-dynamic-screen-renderer)
11. [Chat Session Management](#11-chat-session-management)
12. [Data Schema](#12-data-schema)
13. [User Stories & Flows](#13-user-stories--flows)
14. [Onboarding Flow](#14-onboarding-flow)
15. [Module & File Structure](#15-module--file-structure)
16. [Reusable Component Library](#16-reusable-component-library)
17. [Future: SSH Cloud Processing](#17-future-ssh-cloud-processing)
18. [CI/CD Architecture](#18-cicd-architecture)
19. [Performance Budget](#19-performance-budget)
20. [TDD Specification](#20-tdd-specification)
21. [Security Model](#21-security-model)

---

## 1. APP IDENTITY

```
Name        : agentShell
Package     : dev.agentshell.app
Logo        : Lowercase serif 'a' — white #F2E6CC on espresso brown #1C0F09
              Square icon, 22% corner radius, no border, no shadow
Tagline     : "Your phone. Your agent. No cloud required."
Min Android : 12 (API 31) — works on all phones at this level and above
Stack       : 100% Kotlin, Jetpack Compose, Coroutines, Flow, Room, Hilt
```

---

## 2. UI REFERENCE — IMAGE ANALYSIS

> Three screens visible in the reference image. All terminal UI must match these exactly.

### Screen A — Splash / ASCII Loading (Left)

```
WHAT IT SHOWS:
  Full-screen terminal loading sequence on espresso brown background.
  A large ASCII art composition (60% screen height) built from block
  characters: █ ▓ ▒ ░ · | — renders character-by-character (typewriter).
  Above: monospaced status line "[SYS] / INIT ···"
  Below: text-based progress bar filling left-to-right using █ and ░

DIMENSIONS:
  Status line     : y=28dp, 12sp mono, cream #E8D4B0
  ASCII art block : starts y=80dp, 24 chars wide × 12 lines tall
  Progress bar    : 16 segments [████████░░░░░░░░] below art
  Status text     : centered bottom quarter, 10sp

ANIMATION SEQUENCE (implement exactly):
  0ms    → Background fade in #1C0F09 (300ms)
  300ms  → Status line appears: "[SYS] / INIT ···" cursor blinks
  600ms  → ASCII art typewriter: 2ms per character, top-left → bottom-right
  art done → Progress bar fills: █ appended every 60ms over 1.5s
  Status cycles: "LOADING CORE" → "MOUNTING FS" → "STARTING LLM" → "READY"
  READY  → Horizontal wipe-right transition to Home (300ms ease-in-out)

ASCII ART ENGINE (feature, reusable):
  - Bundled file: assets/splash/agentshell_logo.ascii (pre-baked 24×12 grid)
  - FIGlet renderer: supports fonts block, slant, doom, digital, banner
  - Font files bundled: assets/figlet_fonts/*.flf
  - Terminal command: ascii-art <text> [--font slant]
  - Radar display: ascii-art --radar (8-spoke compass from char set + - | / \ *)
  - All ASCII art is pure text — rendered in JetBrains Mono in terminal view
```

### Screen B — Terminal Dashboard (Middle)

```
WHAT IT SHOWS:
  Primary terminal UI with multiple brutalist data panels. Dense layout.
  Header bar shows path-style label. 3-4 labeled sections with:
    - Vertical bar graph (8 bars, variable height, block chars ▁▂▃▄▅▆▇█)
    - Horizontal level meters (═══○·· format)
    - Scrolling text log
    - Pinned input bar at bottom

EXACT LAYOUT (dp measurements):
  Header bar       : 36dp tall, 11sp mono, cream text, Shell-1 background
  Section panels   : 1dp border #3D2418, 0dp corner radius (sharp/brutalist)
  Panel header     : 28dp, 10sp, amber label "SECTION // detail"
  Vertical bars    : 8dp wide, 2dp gap, max 48dp tall, colors: Amber→MidBrown
  Level meters     : format "LABEL ═══════○·····" — Amber filled, cream head
  Log view         : 9sp mono, scrollable, cream text on near-black bg
  Input bar        : 40dp pinned bottom, "> " amber prefix, cream text

COLORS (terminal-specific):
  Terminal BG      : #0F0704 (slightly darker than app BG)
  Command input    : #B89450 (amber)
  Normal output    : #C4A882 (sand)
  Error output     : #C45040 (muted red)
  System/agent msg : #6A9A6A (muted green)
  Selection BG     : #4A2C1E
  Cursor           : #F2E6CC, blinking block, 500ms interval

SECTION BORDER STYLE:
  border: 1dp solid #3D2418
  corners: 0dp (NO rounding anywhere — strict brutalist)
  inner padding: 8dp all sides
```

### Screen C — Sensor / Data Detail (Right)

```
WHAT IT SHOWS:
  Dense data screen with radial ASCII radar (center-symmetric star built
  from * - | + / \ chars), multi-column data text, bottom tab row,
  and sharp-corner action buttons.

RADIAL RADAR SPECIFICATION:
  Characters: center +, horizontal arms ─, vertical │, diagonal ╱ ╲
  Tip markers: * or ◆ based on value intensity
  Size: ~80×80dp character grid (9-char radius)
  Axes: 8 spokes (N NE E SE S SW W NW)
  Update interval: 250ms — smooth char-by-char redraw
  Use cases: CPU radar, memory usage, LLM confidence radar, sensor data

TAB ROW (bottom of screen):
  Height: 40dp
  Active tab: 1dp underline, Amber #B89450
  Font: 10sp mono, uppercase labels
  Separator: 0.5dp Shell-3 full width above tabs

ACTION BUTTONS:
  Background: transparent
  Border: 1dp Amber #B89450
  Text: 10sp bold mono, cream, uppercase
  Padding: 8dp × 16dp
  Press state: background briefly #2D1810 for 200ms
  NO corner radius anywhere
```

---

## 3. DESIGN SYSTEM

### 3.1 Color Palette

```kotlin
// Theme.kt — single source of truth for all colors

object AgentShellColors {

    // BACKGROUND SCALE (darkest → lightest surface)
    val Shell0 = Color(0xFF1C0F09)   // Espresso Black — app background
    val Shell1 = Color(0xFF2D1810)   // Dark Brown — card/panel backgrounds
    val Shell2 = Color(0xFF4A2C1E)   // Mid Brown — elevated surfaces, input BG
    val Shell3 = Color(0xFF6B4030)   // Warm Brown — borders, dividers
    val Shell4 = Color(0xFF8C5A3C)   // Tan Brown — secondary borders, inactive

    // TEXT SCALE (brightest → most muted)
    val Text0 = Color(0xFFF2E6CC)   // Cream White — primary, headings
    val Text1 = Color(0xFFE8D4B0)   // Warm Cream — body text
    val Text2 = Color(0xFFC4A882)   // Sand — secondary, metadata
    val Text3 = Color(0xFF9A7A5A)   // Muted Tan — placeholder, hints
    val Text4 = Color(0xFF6B5540)   // Deep Tan — very muted background text

    // ACCENT
    val Amber    = Color(0xFFB89450)  // Primary accent — active, fills, highlights
    val AmberLow = Color(0xFF7A5C28)  // Dark amber — pressed state

    // SEMANTIC
    val Success = Color(0xFF6A9A6A)   // Muted green
    val Error   = Color(0xFFC45040)   // Muted red
    val Info    = Color(0xFF5A7A9A)   // Muted blue
    val Warning = Color(0xFFB87830)   // Muted orange

    // TERMINAL-SPECIFIC
    val TermBg  = Color(0xFF0F0704)   // Terminal background (darkest)
    val TermFg  = Color(0xFFE8D4B0)   // Default terminal text
    val TermCmd = Color(0xFFB89450)   // Command/input text (amber)
    val TermOut = Color(0xFFC4A882)   // stdout output
    val TermErr = Color(0xFFC45040)   // stderr output
    val TermSys = Color(0xFF6A9A6A)   // Agent/system messages (green)
    val TermCur = Color(0xFFF2E6CC)   // Cursor color
    val TermSel = Color(0xFF4A2C1E)   // Selection background
}
```

### 3.2 Typography

```kotlin
// Typography.kt

val AgentShellTypography = Typography(
    // All terminal/UI text: JetBrains Mono
    // Download via downloadable fonts API or bundle .ttf in assets/fonts/

    displayLarge  = TextStyle(fontFamily = JetBrainsMono, fontSize = 24.sp),
    headlineMedium = TextStyle(fontFamily = JetBrainsMono, fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge     = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp),
    bodySmall     = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp),
    labelSmall    = TextStyle(fontFamily = JetBrainsMono, fontSize = 9.sp),
    // 9sp is the MINIMUM — never go below this
)

// Rule: JetBrains Mono everywhere except chat markdown content
// Chat message bodies may use Roboto (system default) at 14sp
```

### 3.3 Spacing & Shape System

```kotlin
// Shape.kt
// STRICT RULE: 0dp corner radius everywhere (brutalist aesthetic)
// The ONLY exception: the app logo icon (22% radius)

val AgentShellShapes = Shapes(
    small  = RectangleShape,   // 0dp — all buttons, chips
    medium = RectangleShape,   // 0dp — all cards, panels
    large  = RectangleShape    // 0dp — all bottom sheets, dialogs
)

// Spacing constants
object Spacing {
    val xs  = 4.dp
    val sm  = 8.dp
    val md  = 12.dp
    val lg  = 16.dp
    val xl  = 24.dp
    val xxl = 32.dp
}

// Border widths
object Borders {
    val thin     = 0.5.dp   // Dividers
    val standard = 1.dp     // Panels, sections
    val accent   = 2.dp     // Navigation, hamburger, active states
    val thick    = 3.dp     // Left accent bar on active nav item
}
```

### 3.4 Navigation Components

```kotlin
// HAMBURGER SIDEBAR SPEC:

// Trigger icon: 3 lines, each 18dp wide × 2dp height, 4dp gap between
// Touch target: 48×48dp
// Animation: lines 1+3 rotate to X in 250ms, line 2 fades 150ms

// Drawer:
//   Width: 280dp
//   Background: Shell1
//   Border-right: 2dp Shell3  ← exact 2dp as required
//   Slide animation: 300ms ease-in-out from left
//   Scrim behind: Shell0 at 60% alpha

// Drawer header (100dp tall):
//   Logo 'a' (40×40dp) + "agentShell" (14sp) + version (10sp Text3)
//   Padding: 20dp left, 16dp top

// Nav items (52dp each):
//   Icon: 20×20dp, Phosphor outlined style, 2dp stroke  ← 2dp as required
//   Label: 13sp JetBrains Mono
//   Active: 3dp left accent bar Amber + Text0 label
//   Inactive: Text2 label, no bar
//   Dividers: 0.5dp Shell3

// BOTTOM NAV BAR:
//   Height: 56dp
//   Background: Shell1
//   Border-top: 2dp Shell3  ← 2dp as required
//   Icons: 22×22dp outlined, 2dp stroke
//   Labels: 9sp mono
//   Active: Amber icon + label
//   Inactive: Text3
//   Items: Shell | Chat | Apps | Settings
//   New app badge: amber dot on Apps icon
```

---

## 4. ARCHITECTURE OVERVIEW

### 4.1 Clean Architecture + MVI

```
┌─────────────────────────────────────────────────────────────────────┐
│                         UI LAYER (Compose)                          │
│  Each screen: Screen.kt + ScreenViewModel.kt + ScreenState.kt       │
│  State: StateFlow<ScreenState> consumed via collectAsStateWithLifecycle│
│  Events: Channel<ScreenEvent> → one-time actions (navigation, toast) │
│  Intents: sealed class ScreenIntent (user actions sent to VM)        │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ MVI: Intent → ViewModel → State
┌───────────────────────────────▼─────────────────────────────────────┐
│                      DOMAIN LAYER (UseCases)                        │
│  Pure Kotlin — no Android imports                                   │
│  RunAgentTaskUseCase, SendChatMessageUseCase, InstallPackageUseCase  │
│  ExecuteTerminalCommandUseCase, RenderMiniAppUseCase                 │
│  Each UseCase: single responsibility, injected via Hilt             │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ Interface boundaries (Repository pattern)
┌───────────────────────────────▼─────────────────────────────────────┐
│                       DATA LAYER (Repositories)                     │
│  ChatRepository, AgentRepository, TerminalRepository                │
│  LLMRepository, MemoryRepository, ScreenRepository                  │
│  Each Repository: interface in domain, impl in data (injected)       │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────────┐
│                    INFRASTRUCTURE LAYER                             │
│  Room DB · SQLite-Vec · MediaPipe · ProcessBuilder · AlarmManager   │
│  OkHttp · DataStore · FileObserver · WorkManager · SensorManager     │
└─────────────────────────────────────────────────────────────────────┘
```

### 4.2 Dependency Injection (Hilt)

```kotlin
// Hilt modules — one per layer boundary

@Module @InstallIn(SingletonComponent::class)
object LLMModule {
    @Provides @Singleton
    fun provideLLMEngine(
        settings: SettingsDataStore,
        context: Application
    ): LLMEngine = LLMEngineFactory.create(settings.currentProvider, context)
}

@Module @InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "agentshell.db")
            .addMigrations(*ALL_MIGRATIONS)
            .build()
}

// All ViewModels, UseCases, Repositories injected via @HiltViewModel
// No manual DI, no service locators
```

### 4.3 Foreground Service Architecture

```kotlin
// AgentShellService.kt — the permanent host process

@AndroidEntryPoint
class AgentShellService : Service() {

    // Injected managers (all singletons, share this service's lifecycle)
    @Inject lateinit var agentLoopManager: AgentLoopManager
    @Inject lateinit var llmEngineManager: LLMEngineManager
    @Inject lateinit var terminalSessionManager: TerminalSessionManager
    @Inject lateinit var screenWatcher: ScreenWatcher
    @Inject lateinit var notificationHelper: NotificationHelper

    // ServiceScope: survives config changes, cancelled only when service stops
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildPersistentNotification())
        serviceScope.launch { llmEngineManager.warmUp() }
        serviceScope.launch { screenWatcher.start() }
        return START_STICKY   // restart if killed by OS
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    // Service started on BOOT_COMPLETED (BroadcastReceiver → startForegroundService)
}
```

### 4.4 Chunked Processing — No Conflicts, No Blocking

```
RULE: Every expensive operation uses its own dispatcher.
      Nothing blocks the main thread. Nothing blocks another operation.

Dispatcher assignments:
  Dispatchers.Main       — Compose recomposition, user input handling only
  Dispatchers.Default    — LLM inference, RAG search, agent loop steps
  Dispatchers.IO         — File reads/writes, network, database queries
  Dispatchers.IO (limit) — Terminal ProcessBuilder (limited to 4 threads)

Chunked processing pattern (used everywhere large data is processed):
  fun processInChunks(items: List<T>, chunkSize: Int = 50): Flow<Result<T>> = flow {
      items.chunked(chunkSize).forEach { chunk ->
          chunk.forEach { item -> emit(processItem(item)) }
          yield()  // ← cooperative cancellation + scheduler breathing room
      }
  }.flowOn(Dispatchers.Default)

LLM streaming:
  Gemma inference → emits tokens one-by-one via Flow<String>
  UI collects with collectLatest (cancels previous if new prompt arrives)
  Each token emission: yield() to prevent starvation of other coroutines

Terminal I/O:
  PTY stdout → separate IO coroutine, buffers in 1024-byte chunks
  Never accumulates unbounded — ring buffer capped at 10,000 lines

Conflict prevention:
  AgentLoopManager has a Mutex — only one agent loop runs at a time
  LLM inference: one active Job, new request cancels previous via job.cancelAndJoin()
  File writes: AtomicFile for all .md memory files (crash-safe writes)
  Database: Room handles its own concurrency via SQLite WAL mode
```

---

## 5. LLM PROVIDER SYSTEM

### 5.1 Provider Abstraction

```kotlin
// LLMEngine.kt — the single interface all providers implement

interface LLMEngine {
    val providerType: ProviderType
    val isReady: Boolean
    val statusFlow: StateFlow<EngineStatus>

    // Streaming inference — emits tokens one at a time
    fun generate(
        prompt: String,
        systemPrompt: String,
        maxTokens: Int = 1024,
        temperature: Float = 0.8f
    ): Flow<String>

    // Single-shot for embeddings/classification (non-streaming)
    suspend fun complete(prompt: String, maxTokens: Int = 256): String

    // Token counting (no inference — fast)
    fun countTokens(text: String): Int

    // Health check — verify provider is reachable
    suspend fun ping(): PingResult
}

enum class ProviderType {
    LOCAL_GEMMA,       // MediaPipe on-device
    OPENROUTER,        // OpenRouter API (minimax-m1:free + others)
    GOOGLE_GEMINI,     // Google Gemini preview models
    SELF_HOSTED_OLLAMA, // Ollama running on user's PC
    SELF_HOSTED_LMSTUDIO // LM Studio running on user's PC
}

// Factory — reads settings, returns correct implementation
object LLMEngineFactory {
    fun create(settings: LLMSettings, context: Context): LLMEngine = when (settings.provider) {
        ProviderType.LOCAL_GEMMA         -> LocalGemmaEngine(context, settings.gemmaConfig)
        ProviderType.OPENROUTER          -> OpenRouterEngine(settings.openRouterConfig)
        ProviderType.GOOGLE_GEMINI       -> GeminiEngine(settings.geminiConfig)
        ProviderType.SELF_HOSTED_OLLAMA  -> OllamaEngine(settings.ollamaConfig)
        ProviderType.SELF_HOSTED_LMSTUDIO -> LMStudioEngine(settings.lmStudioConfig)
    }
}
```

### 5.2 Local Gemma (MediaPipe)

```kotlin
// LocalGemmaEngine.kt

class LocalGemmaEngine(
    private val context: Context,
    private val config: GemmaConfig
) : LLMEngine {

    private var inference: LlmInference? = null

    override val providerType = ProviderType.LOCAL_GEMMA

    // Load model — call once at service start
    suspend fun load() = withContext(Dispatchers.Default) {
        val options = LlmInferenceOptions.builder()
            .setModelPath(config.modelPath)      // Path to .task file (user-selected or downloaded)
            .setMaxTokens(config.maxTokens)      // Default: 1024
            .setTopK(config.topK)                // Default: 40
            .setTemperature(config.temperature)  // Default: 0.8
            .setNumDraftTokens(3)                // Speculative decoding (speed improvement)
            .build()
        inference = LlmInference.createFromOptions(context, options)
        // GPU delegate auto-used on Tensor G2 (no explicit flag needed with MediaPipe 0.10+)
    }

    // Unload to free ~1.62 GB RAM when phone locked + idle 15 min
    fun unload() {
        inference?.close()
        inference = null
    }

    override fun generate(
        prompt: String,
        systemPrompt: String,
        maxTokens: Int,
        temperature: Float
    ): Flow<String> = callbackFlow {
        val fullPrompt = buildGemmaPrompt(systemPrompt, prompt)
        inference?.generateResponseAsync(fullPrompt) { partialResult, done ->
            trySend(partialResult)
            if (done) close()
        } ?: close(IllegalStateException("Model not loaded"))
        awaitClose()
    }.flowOn(Dispatchers.Default)

    override fun countTokens(text: String): Int =
        inference?.sizeInTokens(text) ?: (text.length / 4) // fallback estimate

    // Gemma prompt format: <start_of_turn>user\n{system}\n{user}<end_of_turn>\n<start_of_turn>model\n
    private fun buildGemmaPrompt(system: String, user: String): String =
        "<start_of_turn>user\n$system\n\n$user<end_of_turn>\n<start_of_turn>model\n"
}
```

### 5.3 Self-Hosted Ollama / LM Studio

```kotlin
// OllamaEngine.kt — connects to user's PC running Ollama

class OllamaEngine(private val config: OllamaConfig) : LLMEngine {

    // config.baseUrl = "http://192.168.1.100:11434" (user-entered IP)
    // config.modelName = "llama3.2", "mistral", etc. (selected from /api/tags)

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    override fun generate(
        prompt: String,
        systemPrompt: String,
        maxTokens: Int,
        temperature: Float
    ): Flow<String> = flow {
        val request = Request.Builder()
            .url("${config.baseUrl}/api/generate")
            .post(buildOllamaBody(prompt, systemPrompt, maxTokens, temperature).toRequestBody())
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Ollama error: ${response.code}")
            val source = response.body?.source() ?: return@use
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                val chunk = Json.decodeFromString<OllamaChunk>(line)
                if (chunk.response.isNotEmpty()) emit(chunk.response)
                if (chunk.done) break
            }
        }
    }.flowOn(Dispatchers.IO)

    // Validate connection + list available models
    suspend fun fetchAvailableModels(): List<String> = withContext(Dispatchers.IO) {
        val response = client.newCall(
            Request.Builder().url("${config.baseUrl}/api/tags").build()
        ).execute()
        Json.decodeFromString<OllamaTagsResponse>(response.body!!.string()).models.map { it.name }
    }

    // Ping — check if Ollama is reachable on the given IP
    override suspend fun ping(): PingResult = withContext(Dispatchers.IO) {
        runCatching {
            val response = client.newCall(
                Request.Builder().url("${config.baseUrl}/api/tags")
                    .build()
            ).execute()
            if (response.isSuccessful)
                PingResult.Success(latencyMs = response.receivedResponseAtMillis - response.sentRequestAtMillis)
            else PingResult.Failure("HTTP ${response.code}")
        }.getOrElse { PingResult.Failure(it.message ?: "Unreachable") }
    }
}

// LMStudioEngine.kt — identical to OllamaEngine but uses OpenAI-compatible API
// LM Studio default port: 1234, endpoint: /v1/chat/completions
class LMStudioEngine(private val config: LMStudioConfig) : LLMEngine {
    // config.baseUrl = "http://192.168.1.100:1234"
    // Uses OpenAI chat completions format (SSE streaming)
    // Implementation: standard OpenAI-compatible client, same as OpenRouter
}
```

### 5.4 Onboarding — LLM Provider Card System

```
ONBOARDING SCREEN 3: CHOOSE YOUR AI ENGINE

Shows 5 cards, single-select. Each card:
  Border: 1dp Shell3, 0dp radius
  Selected: border 1dp Amber, left accent 3dp Amber
  Icon: 20dp outlined
  Title: 13sp Text0
  Subtitle: 11sp Text2
  Status chip: small label (e.g., "Free", "Requires PC", "1.62 GB")

CARDS:
  ┌─────────────────────────────────────────────┐
  │ ◉ [📱] LOCAL — Gemma 2B INT4               │
  │   Runs fully on your phone                  │
  │   ✓ 100% private  ⚠ 1.62 GB download       │
  │   [FREE] [OFFLINE]                          │
  └─────────────────────────────────────────────┘
  
  ┌─────────────────────────────────────────────┐
  │ ○ [🌐] OpenRouter                          │
  │   Cloud API — minimax-m1:free               │
  │   ✓ No storage  ⚠ Internet + API key       │
  │   [FREE TIER]                               │
  └─────────────────────────────────────────────┘
  
  ┌─────────────────────────────────────────────┐
  │ ○ [🔷] Google Gemini                       │
  │   Preview models via Gemini API             │
  │   ✓ Fast  ⚠ API key required               │
  │   [PREVIEW MODELS]                          │
  └─────────────────────────────────────────────┘
  
  ┌─────────────────────────────────────────────┐
  │ ○ [🖥] Self-Hosted — Ollama                │
  │   Connect to Ollama on your PC              │
  │   ✓ Any model  ⚠ Both on same Wi-Fi       │
  │   [REQUIRES PC]                             │
  └─────────────────────────────────────────────┘
  
  ┌─────────────────────────────────────────────┐
  │ ○ [🖥] Self-Hosted — LM Studio             │
  │   Connect to LM Studio on your PC           │
  │   ✓ OpenAI compatible  ⚠ Same Wi-Fi        │
  │   [REQUIRES PC]                             │
  └─────────────────────────────────────────────┘

All 5 providers can also be changed later in Settings → AI Engine.
```

---

## 6. GEMMA MODEL MANAGEMENT

### 6.1 Three Ways to Get the Model

```
METHOD 1 — INTERNAL DOWNLOAD (easiest for non-technical users):
  User taps "Download" → consent screen → download starts in Foreground Service
  Source: MediaPipe model registry (Google CDN)
  Progress: real-time bar with bytes, speed, ETA, pause/resume
  Resumes on app restart (uses ranged HTTP requests with Range header)
  Wi-Fi check before starting (warn but don't block on mobile data)

METHOD 2 — EXTERNAL DOWNLOAD + FILE PICKER (for data-conscious users):
  User taps "I'll download it myself" button
  App shows: the exact URL, file name, expected size (with copy button)
  User downloads externally (browser, IDM, any downloader)
  When done: taps "Select Downloaded File" → file picker opens
  App validates: file size check + MediaPipe model format check
  If valid: copies to /files/models/ and proceeds
  
  Copy URL button shows:
  ┌─────────────────────────────────────────────────────┐
  │ Download this file with any downloader:             │
  │                                                     │
  │ [https://huggingface.co/google/.../ ■■■■■] [COPY] │
  │                                                     │
  │ File: gemma-2b-it-gpu-int4.bin                     │
  │ Size: 1.62 GB — download on Wi-Fi recommended      │
  │                                                     │
  │ [SELECT FILE AFTER DOWNLOADING]                    │
  └─────────────────────────────────────────────────────┘

METHOD 3 — API (skip model entirely):
  User selects any cloud or self-hosted provider
  No model download, no storage used
  Full functionality except 100% offline operation
```

### 6.2 File Validation

```kotlin
// ModelValidator.kt

class ModelValidator {

    suspend fun validate(file: File): ValidationResult = withContext(Dispatchers.IO) {
        // Step 1: Size check (Gemma 2B INT4 is ~1.5-1.7 GB)
        val sizeOk = file.length() in 1_400_000_000L..1_800_000_000L
        if (!sizeOk) return@withContext ValidationResult.WrongSize(file.length())

        // Step 2: MediaPipe task file header magic bytes
        // MediaPipe .task files start with specific ZIP-based header
        val header = file.inputStream().use { it.readNBytes(8) }
        val isMpTask = header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() // PK = ZIP
        if (!isMpTask) return@withContext ValidationResult.InvalidFormat

        // Step 3: Try loading with MediaPipe (lightweight options, no inference)
        // If this succeeds without exception, file is valid
        runCatching {
            LlmInference.createFromOptions(
                context,
                LlmInferenceOptions.builder()
                    .setModelPath(file.absolutePath)
                    .setMaxTokens(10)
                    .build()
            ).also { it.close() }
        }.fold(
            onSuccess = { ValidationResult.Valid },
            onFailure = { ValidationResult.CorruptFile(it.message) }
        )
    }
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class WrongSize(val actualBytes: Long) : ValidationResult()
    object InvalidFormat : ValidationResult()
    data class CorruptFile(val reason: String?) : ValidationResult()
}
```

### 6.3 Model Storage

```
Storage locations (user can choose in settings):

DEFAULT: /data/data/dev.agentshell.app/files/models/
  - Always available, app-private, no permissions needed
  - NOT visible in Files app

EXTERNAL (optional): /storage/emulated/0/Android/data/dev.agentshell.app/files/models/
  - Visible in Files app (easier for external download → select flow)
  - Requires READ_MEDIA permission (already requested in onboarding)
  
Model file naming:
  gemma-2b-it-gpu-int4.task   ← MediaPipe format (preferred)
  gemma-2b-it-gpu-int4.bin    ← Raw format (validated + converted by app)

Multiple models:
  App supports multiple .task files in models/ dir
  User can switch between them in Settings → AI Engine → Local Model
  Model selector shows: filename, size, last-used date
```

---

## 7. AGENT ARCHITECTURE — CLAUDE CODE LOOP

### 7.1 Context Loading (CLAUDE.md equivalent)

```kotlin
// AgentContextBuilder.kt
// Builds the full prompt context before EVERY LLM call — never exceed 1024 tokens

class AgentContextBuilder(
    private val ragRetriever: RAGRetriever,
    private val chatRepository: ChatRepository,
    private val markdownMemory: MarkdownMemory,
    private val toolRegistry: ToolRegistry
) {
    suspend fun build(task: String, sessionId: String): AgentContext {
        // 1. System prompt from assets/system_prompt.md (256 token hard cap)
        val systemPrompt = loadSystemPrompt()            // ~200 tokens

        // 2. RAG: embed task → retrieve top 3 relevant .md chunks
        val ragChunks = ragRetriever.retrieve(task, topK = 3)  // ~300 tokens max (100 each)

        // 3. Recent turns: last 3 from active session (compacted if needed)
        val recentTurns = chatRepository.getRecentTurns(sessionId, limit = 3) // ~300 tokens max

        // 4. Available tools for this agent tier (see Section 8)
        val tools = toolRegistry.getToolsForContext()        // ~100 tokens (XML schema)

        // 5. Task itself
        val taskPrompt = task.take(600)                      // ~150 tokens max

        // TOTAL: never exceeds 1024 tokens (hard enforced — truncate if over)
        return AgentContext(systemPrompt, ragChunks, recentTurns, tools, taskPrompt)
            .also { assert(it.totalTokens <= 1024) }
    }
}
```

### 7.2 The Reasoning Loop

```kotlin
// AgentLoopManager.kt

class AgentLoopManager @Inject constructor(
    private val llmEngine: LLMEngine,
    private val toolDispatcher: ToolDispatcher,
    private val hookMiddleware: HookMiddleware,
    private val memoryCompactor: MemoryCompactor,
    private val contextBuilder: AgentContextBuilder
) {
    // Mutex: only ONE loop runs at a time (prevents conflicts)
    private val loopMutex = Mutex()

    // State visible to UI
    val agentState = MutableStateFlow<AgentState>(AgentState.Idle)

    suspend fun run(task: String, sessionId: String, tier: AgentTier): AgentResult {
        return loopMutex.withLock {  // ← prevents parallel loops
            runLoop(task, sessionId, tier)
        }
    }

    private suspend fun runLoop(
        task: String,
        sessionId: String,
        tier: AgentTier
    ): AgentResult = withContext(Dispatchers.Default) {

        val maxSteps = tier.maxSteps          // LOCAL=4, API=12, SELF_HOSTED=8
        var stepCount = 0
        val stepHistory = mutableListOf<AgentStep>()

        agentState.emit(AgentState.Planning(task))

        // ──── STEP LOOP ────────────────────────────────────────────────
        while (stepCount < maxSteps) {
            stepCount++

            // ── 1. PLAN: Build context + call LLM ──────────────────────
            val context = contextBuilder.build(
                task = buildTaskWithHistory(task, stepHistory),
                sessionId = sessionId
            )

            agentState.emit(AgentState.Thinking(step = stepCount, maxSteps = maxSteps))

            val llmResponse = StringBuilder()
            llmEngine.generate(
                prompt = context.toPromptString(),
                systemPrompt = context.systemPrompt
            ).collect { token ->
                llmResponse.append(token)
                agentState.emit(AgentState.Streaming(partial = llmResponse.toString()))
            }

            val parsed = ResponseParser.parse(llmResponse.toString())

            // ── Check for completion ────────────────────────────────────
            if (parsed.isDone) {
                reflect(task, stepHistory, success = true)
                return@withContext AgentResult.Success(
                    message = parsed.finalMessage,
                    stepsExecuted = stepCount,
                    artifacts = stepHistory.flatMap { it.filesCreated }
                )
            }

            // ── 2. ACT: Execute tool via dispatcher ─────────────────────
            val toolCall = parsed.toolCall ?: break

            agentState.emit(AgentState.Acting(tool = toolCall.name, step = stepCount))

            val toolResult = runCatching {
                hookMiddleware.executeWithHooks(toolCall, tier)  // pre + post hooks
            }.getOrElse { e ->
                ToolResult.Failure(reason = e.message ?: "Hook blocked")
            }

            // ── 3. OBSERVE: Record result ───────────────────────────────
            stepHistory.add(AgentStep(
                step = stepCount,
                toolCall = toolCall,
                result = toolResult,
                timestamp = System.currentTimeMillis()
            ))

            // Yield to other coroutines between steps (cooperative scheduling)
            yield()
        }

        // Max steps reached
        reflect(task, stepHistory, success = false)
        return@withContext AgentResult.MaxDepthReached(stepsExecuted = maxSteps)
    }

    // ── 4. REFLECT: Write to memory ─────────────────────────────────────
    private suspend fun reflect(task: String, steps: List<AgentStep>, success: Boolean) {
        agentState.emit(AgentState.Reflecting)

        if (success) {
            // Write summary to context.md
            markdownMemory.append("context.md", buildSummary(task, steps))
        } else {
            // Write failure entry to mistakes.md
            val failedStep = steps.lastOrNull { it.result is ToolResult.Failure }
            markdownMemory.append("mistakes.md", buildMistakeEntry(task, failedStep))
        }

        agentState.emit(AgentState.Idle)
    }
}
```

### 7.3 Tool System (22 tools, XML protocol)

```kotlin
// Tool definitions — LLM emits these as XML, dispatcher executes them

// XML format the LLM must use:
// <tool_call>
//   <name>write_file</name>
//   <params>
//     <path>screens/hydration.json</path>
//     <content>{"type":"dashboard"...}</content>
//   </params>
// </tool_call>
// <done>Optional final message to user</done>

sealed class AgentTool(
    val name: String,
    val description: String,
    val requiredTier: AgentTier  // Which tier can use this tool
) {
    // ── File System (all tiers) ──────────────────────────────────────
    object WriteFile   : AgentTool("write_file",    "Write content to a file", LOCAL)
    object ReadFile    : AgentTool("read_file",     "Read a file's content",   LOCAL)
    object ListDir     : AgentTool("list_dir",      "List directory contents", LOCAL)
    object DeleteFile  : AgentTool("delete_file",   "Delete a file",           LOCAL)
    object MoveFile    : AgentTool("move_file",     "Move or rename a file",   LOCAL)

    // ── Shell Execution (all tiers, sandboxed) ───────────────────────
    object RunShell    : AgentTool("run_shell",     "Execute bash command",    LOCAL)
    object RunPython   : AgentTool("run_python",    "Execute Python script",   LOCAL)
    object RunNode     : AgentTool("run_node",      "Execute Node.js script",  LOCAL)
    object PkgInstall  : AgentTool("pkg_install",   "Install a package",       LOCAL)

    // ── UI / Screen (all tiers) ──────────────────────────────────────
    object RenderScreen : AgentTool("render_screen",    "Register a mini-app screen", LOCAL)
    object ShowNotif    : AgentTool("show_notification","Push a notification",         LOCAL)
    object ShowToast    : AgentTool("show_toast",       "Show a brief toast",          LOCAL)

    // ── Scheduler (all tiers) ────────────────────────────────────────
    object ScheduleAlarm : AgentTool("schedule_alarm",  "Set an exact alarm",         LOCAL)
    object ScheduleWork  : AgentTool("schedule_work",   "Schedule background work",   LOCAL)
    object CancelAlarm   : AgentTool("cancel_alarm",    "Cancel a scheduled alarm",   LOCAL)

    // ── Memory (all tiers) ──────────────────────────────────────────
    object ReadMD      : AgentTool("read_md",       "Read memory .md file",    LOCAL)
    object WriteMD     : AgentTool("write_md",      "Append to memory file",   LOCAL)
    object SearchMem   : AgentTool("search_memory", "Semantic memory search",  LOCAL)

    // ── Network (API tier only — requires user consent hook) ─────────
    object FetchURL    : AgentTool("fetch_url",     "HTTP GET a URL",          API_ONLY)
    object PostURL     : AgentTool("post_url",      "HTTP POST to a URL",      API_ONLY)

    // ── System ───────────────────────────────────────────────────────
    object ReadSensor  : AgentTool("read_sensor",   "Read device sensor",      LOCAL)
    object GetMemInfo  : AgentTool("get_mem_info",  "Get RAM/storage stats",   LOCAL)

    // ── Orchestration (API tier only) ────────────────────────────────
    object SpawnAgent  : AgentTool("spawn_agent",   "Spawn a sub-agent",       API_ONLY)
}
```

### 7.4 Hook Middleware (Safety Layer)

```kotlin
// HookMiddleware.kt — every tool call passes through this, no exceptions

class HookMiddleware @Inject constructor(
    private val userConsentManager: UserConsentManager,
    private val storageMonitor: StorageMonitor
) {
    private val preHooks: List<PreHook> = listOf(
        PathSandboxHook(),           // Block writes outside app dir
        DangerousCommandHook(),      // Block: rm -rf /, dd, mkfs, chmod 777
        NetworkConsentHook(),        // Show dialog before any fetch_url (user must approve)
        StorageQuotaHook(),          // Block if < 100 MB free
        PermissionCheckHook(),       // Verify runtime permission granted
        RateLimitHook(),             // Max 3 LLM calls/min per tier
        TierCapabilityHook(),        // Block API_ONLY tools on local tier
    )

    private val postHooks: List<PostHook> = listOf(
        ApiKeyRedactHook(),          // Remove any API keys from tool output
        OutputTruncationHook(),      // Truncate stdout > 3000 chars
        ErrorClassifierHook(),       // Tag errors recoverable/fatal for agent
    )

    suspend fun executeWithHooks(toolCall: ToolCall, tier: AgentTier): ToolResult {
        // Pre-hooks: any can throw HookException to block execution
        preHooks.forEach { it.check(toolCall, tier) }

        // Execute the actual tool
        val result = ToolDispatcher.execute(toolCall)

        // Post-hooks: inspect and transform output
        val processedResult = postHooks.fold(result) { acc, hook -> hook.process(acc) }

        return processedResult
    }
}

// NetworkConsentHook — shows UI dialog, waits for user response
class NetworkConsentHook : PreHook {
    override suspend fun check(call: ToolCall, tier: AgentTier) {
        if (call.tool is AgentTool.FetchURL || call.tool is AgentTool.PostURL) {
            val url = call.params["url"] ?: throw HookException("Missing URL")
            // Suspend until user responds in UI (via SharedFlow event)
            val approved = userConsentManager.requestNetworkConsent(url)
            if (!approved) throw HookException("User denied network access to $url")
        }
    }
}
```

---

## 8. AGENT TIER SYSTEM

### 8.1 Three Tiers

```
The agent tier controls:
  - How complex tasks can be (max steps)
  - Which tools are available
  - Whether sub-agents can be spawned
  - Token budget per call

TIER 1 — LOCAL AGENT (Gemma on-device):
  ┌─────────────────────────────────────────────────────┐
  │ MAX STEPS  : 4                                      │
  │ MAX TOKENS : 512 per call (conserve RAM)            │
  │ TOOLS      : All file/shell/memory/scheduler tools  │
  │              NO fetch_url, NO spawn_agent           │
  │ NETWORK    : None (keeps it fully offline)          │
  │ SUB-AGENTS : Not supported                          │
  │ BEST FOR   : Simple task execution, reminders,      │
  │              reading files, running scripts,         │
  │              quick answers, local-only operations    │
  └─────────────────────────────────────────────────────┘

TIER 2 — SELF-HOSTED AGENT (Ollama / LM Studio):
  ┌─────────────────────────────────────────────────────┐
  │ MAX STEPS  : 8                                      │
  │ MAX TOKENS : 1024 per call                          │
  │ TOOLS      : All tools including fetch_url          │
  │              NO spawn_agent                         │
  │ NETWORK    : With consent dialog per domain         │
  │ SUB-AGENTS : Not supported (model-dependent)        │
  │ BEST FOR   : Moderate tasks, web fetching, data     │
  │              processing, building simple screens     │
  └─────────────────────────────────────────────────────┘

TIER 3 — API AGENT (OpenRouter / Gemini):
  ┌─────────────────────────────────────────────────────┐
  │ MAX STEPS  : 12                                     │
  │ MAX TOKENS : 1024 per call                          │
  │ TOOLS      : ALL 22 tools including spawn_agent     │
  │ NETWORK    : With consent (batch-approved per task) │
  │ SUB-AGENTS : Up to 3 concurrent sub-agents         │
  │ BEST FOR   : Complex app creation, multi-step       │
  │              workflows, building full mini-apps,     │
  │              orchestrating multiple operations       │
  └─────────────────────────────────────────────────────┘
```

### 8.2 Tier Selection in UI

```
CHAT SCREEN — tier selector (shown in input area, above keyboard):

  [LOCAL ●] [SELF-HOST ○] [API ○]    ← compact 3-segment selector
  
  Small tooltip on first use:
  "LOCAL: Fast, offline, simple tasks
   API: Powerful, builds full apps (uses your API credits)"

AUTOMATIC TIER UPGRADE PROMPT:
  If user submits a complex task to LOCAL tier, agent can detect
  (via keyword heuristics or step overflow) and ask:
  
  "This task needs more steps than the local model supports.
   Switch to API agent for this task? [YES] [NO — KEEP LOCAL]"

TERMINAL: agent run <task> --tier=local|selfhost|api
```

### 8.3 Sub-Agent Orchestration (API tier only)

```kotlin
// SubAgentManager.kt

class SubAgentManager @Inject constructor(
    private val agentLoopManager: AgentLoopManager
) {
    // Each sub-agent: independent coroutine, reports back to parent
    suspend fun spawn(task: SubAgentTask): Deferred<SubAgentResult> =
        coroutineScope {
            async(Dispatchers.Default) {
                val result = agentLoopManager.run(
                    task = task.description,
                    sessionId = task.parentSessionId,
                    tier = AgentTier.API.copy(maxSteps = 6) // sub-agents: lower limit
                )
                SubAgentResult(taskId = task.id, result = result)
            }
        }

    // Orchestrate multiple sub-agents concurrently
    // Example: "Build news app" → 3 sub-agents run in parallel
    suspend fun orchestrate(tasks: List<SubAgentTask>): List<SubAgentResult> =
        coroutineScope {
            tasks.map { spawn(it) }.awaitAll()
        }
    
    // Max 3 concurrent to avoid memory pressure on 6 GB phones
    companion object { const val MAX_CONCURRENT = 3 }
}
```

---

## 9. TERMINAL SYSTEM — TERMUX COMPATIBLE

### 9.1 Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│          TerminalScreen.kt (Compose UI)                         │
│  SurfaceView → custom Canvas renderer (NOT WebView — faster)    │
│  Font: JetBrains Mono, char cell: 8×16dp at default 13sp       │
│  Ring buffer: 10,000 lines (older lines dropped)                 │
│  Scroll: smooth momentum, hardware-accelerated                   │
│  Selection: long-press starts selection, drag to extend          │
└──────────────────────────────┬───────────────────────────────────┘
                               │ char I/O
┌──────────────────────────────▼───────────────────────────────────┐
│          TerminalEmulator.kt (VT100/VT220 compatible)           │
│  Handles ANSI escape codes: color, cursor, clear, scroll        │
│  State: cursor position, color attributes, char buffer          │
│  Library: use termux-term-lib (LGPL, extract from Termux source)│
└──────────────────────────────┬───────────────────────────────────┘
                               │ PTY (pseudo-terminal)
┌──────────────────────────────▼───────────────────────────────────┐
│          TerminalSession.kt (pty4j bridge)                      │
│  Creates /dev/pts/* file descriptor pair (master/slave)         │
│  stdin → write to master fd                                      │
│  stdout/stderr → read from master fd (IO coroutine, 1KB chunks) │
│  Window size: sent via TIOCSWINSZ ioctl on resize               │
└──────────────────────────────┬───────────────────────────────────┘
                               │ Process
┌──────────────────────────────▼───────────────────────────────────┐
│          proot environment + bash                               │
│  Binary: files/usr/bin/bash (ARM64, statically linked)          │
│  Environment variables:                                          │
│    HOME=/data/data/dev.agentshell.app/files/home               │
│    PREFIX=/data/data/dev.agentshell.app/files/usr               │
│    PATH=$PREFIX/bin:$PREFIX/local/bin                           │
│    LD_LIBRARY_PATH=$PREFIX/lib                                   │
│    TERM=xterm-256color                                           │
│    AGENTSHELL=1 (so scripts can detect the environment)         │
└──────────────────────────────────────────────────────────────────┘
```

### 9.2 Bundled Runtimes (ARM64, statically linked or PIE ELF)

```
Bundled in assets/bootstrap/ (shipped with APK, extracted on first run):

RUNTIMES:
  bash 5.2        — Default shell
  Python 3.11     — With pip, venv, cffi support
  Node.js 20 LTS  — With npm (v10)
  Ruby 3.3        — Optional (user-installable via pkg)

TOOLS:
  git 2.44        — Full git client (JGit NOT used — native binary is faster)
  curl 8.x        — HTTP client
  wget            — Alternative HTTP downloader
  OpenSSH 9.x     — ssh, scp, sftp client (for future cloud SSH feature)
  vim + nano      — Text editors
  jq              — JSON processor
  htop            — Process viewer
  tree            — Directory tree
  ripgrep (rg)    — Fast grep alternative
  fd              — Fast find alternative

COMPILERS (user-installable, not bundled):
  gcc, clang → via pkg install gcc
  rustc      → via pkg install rust

All binaries: ARM64 ABI, tested on Android 12+
Bootstrap extraction: async on first launch, ~180 MB extracted
```

### 9.3 Termux Repository System

```
REPOSITORY CONFIG FILE:
  /files/usr/etc/apt/sources.list

FORMAT (identical to Termux):
  deb <url> stable main

BUILT-IN MIRRORS (user selects in Settings → Terminal → Package Mirrors):

  ┌────────────────────────────────────────────────────────┐
  │ Mirror                    URL                  Region  │
  │ ─────────────────────────────────────────────────────  │
  │ Termux Official (CDN)     packages.termux.dev  Global  │
  │ TUNA (Tsinghua Univ.)     mirrors.tuna.tsinghua China  │
  │ BFSU                      mirrors.bfsu.edu.cn  China   │
  │ XTOM                      mirrors.xtom.de      EU      │
  │ A1batross                 termux.mentality.rip  RU     │
  │ 8bytes.io                 termux.8bytes.io     Global  │
  └────────────────────────────────────────────────────────┘

AUTO-MIRROR SELECTION:
  On first terminal open: ping all mirrors concurrently (3s timeout)
  Select lowest-latency responsive mirror
  Show result: "Selected: TUNA (38ms) — change in Settings"
  Store in DataStore, re-ping weekly

CUSTOM REPO SUPPORT:
  User can add custom repo URLs in Settings → Terminal → Custom Repos
  Format: deb <url> <distro> <component>

PACKAGE MANAGER COMMANDS (all work same as Termux):
  pkg update           — Update package lists (apt-get update)
  pkg upgrade          — Upgrade all packages
  pkg install <name>   — Install package
  pkg remove <name>    — Remove package
  pkg search <query>   — Search packages
  pkg list-installed   — List installed packages
  pkg show <name>      — Package info
  apt-get <cmd>        — Full apt-get passthrough

ADDITIONAL REPOS (user-addable):
  science:  packages.termux.dev/apt/termux-science
  games:    packages.termux.dev/apt/termux-games
  x11:      NOT supported (no display server without root)
  root:     NOT supported
```

### 9.4 Terminal Features

```kotlin
// TerminalScreen feature list (implement all):

// 1. MULTI-SESSION (up to 4 concurrent PTYs):
//    Tab strip at top: [~] [project/myapp] [+]
//    Swipe left/right between sessions OR tap tab
//    Each session: own PTY, own scrollback buffer, own CWD

// 2. TOOLBAR (above keyboard, collapses when not needed):
//    [Ctrl] [Alt] [Tab] [ESC] [↑] [↓] [←] [→] [PgUp] [PgDn]
//    Special: [Ctrl+C] [Ctrl+D] [Ctrl+L] as shortcuts

// 3. FONT CONTROLS:
//    Pinch to zoom: 9sp to 20sp, step 1sp
//    Long press font size chip in status bar to reset to default (13sp)

// 4. URL DETECTION:
//    URLs in output auto-highlighted (amber underline)
//    Long press → context menu: [OPEN IN BROWSER] [COPY URL]

// 5. SEARCH:
//    Swipe down in terminal → search bar appears
//    Regex support, highlight matches, prev/next navigation

// 6. SHARE OUTPUT:
//    Long press on selection → [COPY] [SHARE] [SEND TO AGENT]
//    "SEND TO AGENT" pastes selection as context in chat

// 7. AGENT INTEGRATION COMMANDS:
//    agent run "<task>" --tier=local|api
//    agent status         — Show active loop state
//    agent stop           — Cancel current loop
//    agent memory show    — Cat all .md files in pager
//    agent memory search <query>  — RAG search
//    ascii-art <text> [--font doom|block|slant|digital|banner]
//    ascii-art --radar    — Interactive sensor radar display

// 8. PERSISTENCE:
//    Terminal state saved every 30s to logs/terminal_scroll.buf
//    On resume: PTY re-attaches, scrollback restored
//    On kill+restart: "RESTORED SESSION [timestamp]" shown in gray
//    Sessions survive phone restarts (Foreground Service auto-restarts)
```

### 9.5 Agent Terminal Access

```kotlin
// All three LLM tiers can execute terminal commands via the run_shell tool
// This is by design — the terminal is the agent's "hands"

// How agent-executed commands appear in terminal:
// Agent writes to a named pipe that connects to the PTY session
// Commands appear in a special AGENT session (separate tab: "[AGENT]")
// User can watch in real-time or switch to their own session

// Security:
//   Agent shell runs as same Android UID (same sandbox, no escalation)
//   PathSandboxHook prevents writes outside app directory
//   DangerousCommandHook blocks destructive commands
//   User sees every command before/as it executes (via AgentState stream)
```

---

## 10. DYNAMIC SCREEN RENDERER

### 10.1 JSON DSL v1.0

```json5
// screens/hydration.json — example complete mini-app screen

{
  "schema": "agentshell-screen/1.0",
  "id": "hydration",
  "title": "Hydration Tracker",
  "icon_emoji": "💧",
  "background": "shell_0",
  "refresh_ms": 60000,

  "data_sources": [
    { "id": "ml_today", "type": "local_kv", "key": "hydration_ml", "default": 0 },
    { "id": "goal_ml",  "type": "local_kv", "key": "hydration_goal", "default": 2500 }
  ],

  "layout": {
    "type": "column",
    "padding": 16,
    "children": [
      {
        "type": "panel",
        "header": "TODAY // HYDRATION",
        "children": [
          { "type": "big_number", "source": "ml_today",
            "suffix": " ml", "divisor": 1000, "suffix_large": " L", "color": "text_0" },
          { "type": "progress_bar", "source": "ml_today",
            "max_source": "goal_ml", "style": "block_chars", "width": 24 },
          { "type": "text", "content": "Goal: {goal_ml} ml", "color": "text_2", "size": 11 }
        ]
      },
      {
        "type": "row", "gap": 8,
        "children": [
          { "type": "button", "label": "+250 ML", "style": "primary",
            "action": { "type": "increment_kv", "key": "hydration_ml", "amount": 250 } },
          { "type": "button", "label": "+500 ML", "style": "secondary",
            "action": { "type": "increment_kv", "key": "hydration_ml", "amount": 500 } },
          { "type": "button", "label": "RESET", "style": "danger",
            "action": { "type": "set_kv", "key": "hydration_ml", "value": 0 } }
        ]
      },
      {
        "type": "log_view", "source": "agent_log", "max_lines": 20,
        "header": "AGENT LOG // HYDRATION"
      }
    ]
  },

  "notifications": [
    {
      "id": "hydration_alert",
      "interval_min": 45,
      "title": "Hydration Check 💧",
      "message": "You've had {ml_today}ml. Drink some water!",
      "alarm_flags": "UPDATE_CURRENT|IMMUTABLE"
    }
  ]
}
```

### 10.2 Supported Widget Types

```
Layout:      column, row, panel, scroll, stack
Display:     text, big_number, label, divider, badge
Progress:    progress_bar (block_chars | equals_style), level_meter, bar_graph
Visualization: radar_chart (8-axis ASCII), sparkline, log_view
Interactive: button (primary|secondary|danger), input, toggle, tab_bar, slider
Sensor:      step_counter, heart_rate, battery, ram_usage (binds to sensor data)
```

### 10.3 Auto Registration Flow

```kotlin
// ScreenWatcher.kt — FileObserver on /files/screens/

class ScreenWatcher @Inject constructor(
    private val screensDir: File,
    private val markdownMemory: MarkdownMemory,
    private val appsRepository: AppsRepository,
    private val notificationHelper: NotificationHelper
) {
    fun start() {
        val observer = object : FileObserver(screensDir, CREATE or MODIFY) {
            override fun onEvent(event: Int, path: String?) {
                if (path?.endsWith(".json") != true) return

                val file = File(screensDir, path)
                val dsl = runCatching {
                    Json.decodeFromString<ScreenDSL>(file.readText())
                }.getOrNull() ?: return

                // Validate DSL schema version
                if (dsl.schema != "agentshell-screen/1.0") return

                // Atomic write to apps.md (no partial writes on crash)
                markdownMemory.atomicAppend("apps.md", dsl.toMarkdownEntry())

                // Notify repository (updates UI via StateFlow)
                appsRepository.onScreenRegistered(dsl)

                // Push notification to user
                notificationHelper.show(
                    "New app ready: ${dsl.title}",
                    "Built by agent. Open Apps to use it."
                )
            }
        }
        observer.startWatching()
    }
}
```

---

## 11. CHAT SESSION MANAGEMENT

### 11.1 Storage Format

```
File: /files/chat_sessions/{uuid}.agchat
Encoding: GZIP level 6 compressed JSON
Target compression: 4:1 ratio
Max uncompressed size: 500 KB per session

JSON structure inside .agchat:
{
  "id": "uuid",
  "title": "Building hydration tracker",   // auto: first 40 chars of first user msg
  "created_at": 1746182400000,
  "updated_at": 1746186000000,
  "llm_provider": "local-gemma",
  "tier": "LOCAL",
  "is_compacted": false,
  "turn_count": 12,
  "turns": [...]
}

Turn structure:
{
  "id": "turn-001",
  "role": "user|assistant|system",
  "content": "...",
  "timestamp": 1746182400000,
  "agent_steps": 4,             // how many steps agent took
  "tool_calls": [               // collapsible in UI
    { "tool": "write_file", "duration_ms": 45, "success": true }
  ]
}
```

### 11.2 Compaction & Memory Management

```kotlin
// MemoryCompactor.kt — triggered when tokenCount > 2000

class MemoryCompactor @Inject constructor(private val llmEngine: LLMEngine) {

    suspend fun compact(session: ChatSession): ChatSession {
        if (llmEngine.countTokens(session.toPromptString()) < THRESHOLD) return session

        val recentTurns = session.turns.takeLast(3)
        val olderTurns  = session.turns.dropLast(3)

        // Use 100-token summary prompt — very cheap
        val summary = llmEngine.complete(
            "Summarize this conversation in 3 sentences:\n${olderTurns.toText()}"
        )

        // Persist summary to context.md (survives session deletion)
        markdownMemory.atomicAppend(
            "context.md",
            "## Session Summary [${Instant.now()}]\n$summary\n"
        )

        return session.copy(
            turns = listOf(Turn.system("Prior context: $summary")) + recentTurns,
            isCompacted = true
        )
    }

    companion object { const val THRESHOLD = 2000 }
}
```

### 11.3 Agent Access to Previous Sessions

```kotlin
// ChatRepository.kt

// Agent can access previous sessions via search_memory tool
// RAG retrieves relevant session summaries, NOT full turn history

suspend fun findRelevantSessions(query: String): List<SessionSummary> {
    val allSummaries = db.chatDao().getAllSummaries()  // fast — no decompression
    val queryEmbed = embeddingEngine.embed(query)
    return allSummaries
        .map { it to cosineSimilarity(queryEmbed, it.embedding) }
        .filter { it.second > 0.5f }   // relevance threshold
        .sortedByDescending { it.second }
        .take(3)
        .map { it.first }
}
```

---

## 12. DATA SCHEMA

### 12.1 Room Database

```kotlin
// AppDatabase.kt

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatTurnEntity::class,
        VectorChunkEntity::class,
        MiniAppEntity::class,
        ScheduledTaskEntity::class,
        InstalledPackageEntity::class,
    ],
    version = 1,
    exportSchema = true   // ← enables proper migration tracking for CI/CD
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun vectorDao(): VectorDao
    abstract fun appsDao(): AppsDao
    abstract fun schedulerDao(): SchedulerDao
    abstract fun packagesDao(): PackagesDao
}

// Migration strategy:
// Every DB version bump: add a Migration object in Migrations.kt
// CI pipeline runs Room schema verification on every PR
// val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, ...)

@Entity("chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val llmProvider: String,
    val agentTier: String,
    val isCompacted: Boolean,
    val compressedData: ByteArray,   // GZIP JSON of turns
    val turnCount: Int,
    val summaryEmbedding: FloatArray?, // For RAG search over sessions
    @ColumnInfo(name = "summary_text") val summaryText: String?
)

@Entity("vector_chunks")
data class VectorChunkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sourceFile: String,          // "memory/context.md"
    val chunkText: String,
    val embedding: FloatArray,       // 384-dim MiniLM-L6 embedding
    val chunkIndex: Int,             // position in source file
    val updatedAt: Long
)

@Entity("mini_apps")
data class MiniAppEntity(
    @PrimaryKey val id: String,
    val title: String,
    val iconEmoji: String,
    val screenFilePath: String,
    val createdAt: Long,
    val lastUsed: Long,
    val isActive: Boolean,
    val createdByTier: String,
    val agentSessionId: String
)

@Entity("scheduled_tasks")
data class ScheduledTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val alarmManagerId: Int,         // For cancellation
    val serializedToolCall: String,  // JSON
    val isActive: Boolean,
    val lastRunAt: Long?,
    val nextRunAt: Long,
    val intervalMs: Long?            // null = one-shot
)
```

### 12.2 DataStore (Settings)

```kotlin
// SettingsDataStore.kt — typed preferences, no SharedPreferences

data class AppSettings(
    // LLM Configuration
    val activeProvider: ProviderType,

    val gemmaModelPath: String?,         // Null = not downloaded
    val gemmaTemperature: Float = 0.8f,
    val gemmaTopK: Int = 40,
    val gemmaMaxTokens: Int = 1024,

    val openRouterApiKey: String = "",   // Encrypted in storage
    val openRouterModel: String = "minimax/minimax-m1:free",
    val openRouterCustomModel: String = "",

    val geminiApiKey: String = "",       // Encrypted in storage
    val geminiModel: String = "",        // Auto-selected from preview list

    val ollamaBaseUrl: String = "http://192.168.1.100:11434",
    val ollamaModel: String = "llama3.2",

    val lmStudioBaseUrl: String = "http://192.168.1.100:1234",
    val lmStudioModel: String = "",

    // Agent Configuration
    val defaultTier: AgentTier = AgentTier.LOCAL,
    val agentMaxSteps: Int = 12,
    val agentContextTokens: Int = 1024,
    val networkConsentAlways: Boolean = true,

    // Terminal Configuration
    val terminalFontSize: Int = 13,
    val terminalMirror: String = "auto",
    val terminalCustomRepos: List<String> = emptyList(),
    val maxTerminalSessions: Int = 4,

    // UI Configuration
    val themeMode: ThemeMode = ThemeMode.DARK,    // Only dark in v1
    val asciiSplashEnabled: Boolean = true,
)

// Keys encrypted with EncryptedSharedPreferences (AES-256-GCM)
// API keys NEVER go to Room DB — only EncryptedSharedPreferences
```

### 12.3 Memory File Formats

```markdown
<!-- /files/memory/context.md — user profile and accumulated knowledge -->
# agentShell Context

## User Profile
- Device: Pixel 6a, 6 GB RAM, Android 14
- Preferred provider: local-gemma
- Expertise: intermediate developer
- Languages: Python, JavaScript

## Preferences
- Terminal font size: 13sp
- Reminder style: notification only
- Mirror: TUNA (Tsinghua)

## Session Summaries
### 2026-05-02T10:30:00
Built hydration tracker. User prefers 45-min intervals.
Set goal to 2500ml. Reminder uses FLAG_UPDATE_CURRENT.

---

<!-- /files/memory/mistakes.md — agent error log with cures -->
# Agent Mistakes & Cures

## 2026-05-02T11:45:00
**Task:** Schedule hydration alarm
**Error:** AlarmManager fires twice on reboot
**Root Cause:** PendingIntent missing FLAG_UPDATE_CURRENT
**Fix:** Always use FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE in PendingIntent
**Verified:** true

## 2026-05-02T14:20:00
**Task:** Fetch RSS feed
**Error:** javax.net.ssl.SSLHandshakeException on some feeds
**Root Cause:** Old feed servers with weak TLS cipher suites
**Fix:** Add OkHttpClient.Builder().hostnameVerifier for known old servers
         OR advise user to use a different feed source
**Verified:** false

---

<!-- /files/memory/apps.md — mini-app manifest (read by ScreenWatcher) -->
# Mini-App Registry

## hydration
- title: Hydration Tracker
- icon: 💧
- screen_file: screens/hydration.json
- created: 2026-05-02T10:35:00
- active: true
- tier_used: LOCAL

## news
- title: News Feeder
- icon: 📰
- screen_file: screens/news.json
- created: 2026-05-02T14:20:00
- active: true
- tier_used: API
```

---

## 13. USER STORIES & FLOWS

### User Stories (Priority Ordered)

```
P0 — MUST HAVE FOR v1.0:

US-001  First launch shows ASCII splash → onboarding (no data downloaded yet)
US-002  User warned with exact size before ANY download starts
US-003  User can download Gemma internally OR copy link and download externally
US-004  User can select an already-downloaded Gemma .task or .bin file from storage
US-005  User can choose Ollama/LM Studio and enter IP address with instant ping test
US-006  API keys validated instantly in onboarding (green/red feedback)
US-007  Terminal works with Python, bash, git, curl on first launch (no extra setup)
US-008  pkg install works and uses fastest available mirror auto-selected
US-009  User can type "build me a hydration reminder" and get a working mini-app
US-010  Agent shows live steps (step 1/4, tool name, result) in chat
US-011  New mini-app appears in Apps without restart
US-012  Chat sessions auto-save, compress, and resume correctly
US-013  Local agent (Gemma) limited to 4 steps — complex tasks suggest API upgrade

P1 — SHOULD HAVE FOR v1.0:

US-020  Hamburger nav shows all sections, 2dp borders, amber active state
US-021  Bottom nav has 2dp top border, amber active, outlined 2dp icons
US-022  ASCII art renders in terminal with ascii-art command (FIGlet fonts)
US-023  Terminal has multi-session tabs (up to 4 sessions)
US-024  Memory .md files viewable in terminal with mem show command
US-025  mistakes.md auto-updated on agent errors, used in next task context
US-026  Settings → AI Engine allows switching provider + configuring all options
US-027  Gemma model can be re-downloaded or replaced (path re-selectable in settings)

P2 — NICE TO HAVE FOR v1.0:

US-030  Sub-agents for complex tasks (API tier only)
US-031  Sensor integration (steps, battery) for mini-apps
US-032  Chat session export as .md file
US-033  ASCII radar display for sensor data (Screen C style)
US-034  Custom repo URLs added in settings
```

---

## 14. ONBOARDING FLOW

```
SCREEN 1 — WELCOME (no data, no permissions yet)
  Content: Logo + app name + tagline
  ASCII art: small agentShell logo art plays once (1s)
  Action: [CONTINUE →]

SCREEN 2 — WHAT IS THIS?
  Content: 3 terminal-style bullets explaining the app
  > on-device AI agent that lives on your phone
  > real Linux terminal (Python, bash, git, Node.js)
  > builds mini-apps from plain English
  Action: [CONTINUE →]  [SKIP TO SETUP →]

SCREEN 3 — CHOOSE AI ENGINE (critical screen)
  Shows 5 provider cards (see Section 5.4)
  Default selected: LOCAL GEMMA
  Action: [CONTINUE WITH SELECTION →]

SCREEN 4A — GEMMA DOWNLOAD CONSENT (only if LOCAL selected)
  Shows download manifest:
    📦 Gemma 2B INT4  ·  1.62 GB  ·  Source: HuggingFace
    📦 MiniLM Embedding  ·  22 MB  ·  Source: HuggingFace
  Two options:
    [DOWNLOAD NOW]  — starts internal download
    [I'LL DOWNLOAD IT MYSELF]  — shows copy-link flow

  COPY-LINK SUB-FLOW (if "I'll download it myself"):
    Shows URL with [COPY] button (copies to clipboard)
    Shows filename + expected size
    [SELECT DOWNLOADED FILE]  → file picker opens
    On file select → validation runs → success → continues

  TWO CHECKBOXES required before [DOWNLOAD NOW] activates:
    ☐ I'm on Wi-Fi (auto-checked if Wi-Fi detected)
    ☐ I have 2 GB free storage (auto-checked if space available)

SCREEN 4B — DOWNLOAD PROGRESS (if internal download chosen)
  Live progress bar per file
  Format: [████████░░░░░░░░] 48% · 778 MB / 1.62 GB · 2.3 MB/s · ETA 6:12
  [PAUSE]  [CANCEL]
  Download continues if app backgrounded (Foreground Service)

SCREEN 5 — API CONFIG (only if cloud/selfhost provider selected)
  For OpenRouter: key input + [TEST KEY] → instant validation
  For Gemini: key input + [TEST KEY] → fetches preview models → shows dropdown
  For Ollama: IP field (default 192.168.1.100:11434) + [PING] → latency shown
              Model dropdown populated after ping (fetches /api/tags)
  For LM Studio: IP field (default :1234) + [PING] + model field

SCREEN 6 — PERMISSIONS (all at once, with rationale)
  Shows each permission as a row: [ICON] [NAME] [WHY? button] [checkbox]
  [WHY?] opens a one-paragraph explanation popup
  [REQUEST ALL] button → standard Android permission dialogs
  Shows which were granted (✓ green) vs denied (⚠ amber)
  Required: Notifications, Foreground Service
  Recommended: Storage, Exact Alarms, Activity Recognition, Microphone

SCREEN 7 — INITIALIZATION (the ASCII loading screen — Screen A)
  Plays full terminal loading animation
  Shows real initialization steps as they complete:
    > INIT FOREGROUND SERVICE .............. ✓
    > MOUNTING FILE SYSTEM ................. ✓
    > EXTRACTING BOOTSTRAP PACKAGES ........ ✓ (first time only)
    > LOADING LLM ENGINE ................... ✓
    > STARTING AGENT LOOP .................. ✓
    > READY
  Auto-transitions to terminal home screen
```

---

## 15. MODULE & FILE STRUCTURE

```
agentShell/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/dev/agentshell/
│   │   │   │   ├── App.kt                          ← Hilt application class
│   │   │   │   ├── MainActivity.kt                  ← Single activity, nav host
│   │   │   │   │
│   │   │   │   ├── ui/                              ← UI layer (Compose screens)
│   │   │   │   │   ├── theme/
│   │   │   │   │   │   ├── Color.kt                 ← AgentShellColors
│   │   │   │   │   │   ├── Typography.kt            ← AgentShellTypography
│   │   │   │   │   │   ├── Shape.kt                 ← All RectangleShape
│   │   │   │   │   │   └── Theme.kt                 ← MaterialTheme wrapper
│   │   │   │   │   ├── components/                  ← REUSABLE components
│   │   │   │   │   │   ├── ShellPanel.kt            ← Panel with brutalist border
│   │   │   │   │   │   ├── ShellButton.kt           ← Primary/secondary/danger
│   │   │   │   │   │   ├── ShellInput.kt            ← "> " prefix input field
│   │   │   │   │   │   ├── ShellProgressBar.kt      ← Block-char progress bar
│   │   │   │   │   │   ├── ShellLevelMeter.kt       ← ═══○·· meter
│   │   │   │   │   │   ├── ShellBarGraph.kt         ← ▁▂▃▄▅▆▇█ bar graph
│   │   │   │   │   │   ├── ShellRadar.kt            ← ASCII radar chart
│   │   │   │   │   │   ├── ProviderStatusChip.kt    ← LLM status indicator
│   │   │   │   │   │   └── AgentStepCard.kt         ← Collapsible step view
│   │   │   │   │   ├── nav/
│   │   │   │   │   │   ├── AppNavGraph.kt           ← All routes defined here
│   │   │   │   │   │   ├── HamburgerDrawer.kt       ← Sidebar nav
│   │   │   │   │   │   └── BottomNavBar.kt          ← Persistent bottom nav
│   │   │   │   │   ├── onboarding/
│   │   │   │   │   │   ├── OnboardingFlow.kt        ← All 7 screens as NavGraph
│   │   │   │   │   │   ├── ProviderSelectionScreen.kt
│   │   │   │   │   │   ├── DownloadConsentScreen.kt
│   │   │   │   │   │   ├── DownloadProgressScreen.kt
│   │   │   │   │   │   ├── ApiConfigScreen.kt
│   │   │   │   │   │   ├── PermissionsScreen.kt
│   │   │   │   │   │   └── InitializationScreen.kt ← ASCII loading screen
│   │   │   │   │   ├── terminal/
│   │   │   │   │   │   ├── TerminalScreen.kt
│   │   │   │   │   │   ├── TerminalViewModel.kt
│   │   │   │   │   │   ├── TerminalState.kt
│   │   │   │   │   │   └── TerminalView.kt         ← Custom Canvas SurfaceView
│   │   │   │   │   ├── chat/
│   │   │   │   │   │   ├── ChatScreen.kt
│   │   │   │   │   │   ├── ChatViewModel.kt
│   │   │   │   │   │   ├── ChatState.kt
│   │   │   │   │   │   ├── SessionListScreen.kt
│   │   │   │   │   │   └── TierSelectorBar.kt      ← LOCAL|SELFHOST|API selector
│   │   │   │   │   ├── apps/
│   │   │   │   │   │   ├── AppsScreen.kt           ← Mini-apps grid
│   │   │   │   │   │   ├── AppsViewModel.kt
│   │   │   │   │   │   ├── MiniAppScreen.kt        ← DSL-rendered screen
│   │   │   │   │   │   └── DSLComposables.kt       ← Widget renderers
│   │   │   │   │   └── settings/
│   │   │   │   │       ├── SettingsScreen.kt
│   │   │   │   │       ├── LLMSettingsScreen.kt
│   │   │   │   │       ├── TerminalSettingsScreen.kt
│   │   │   │   │       └── MirrorSettingsScreen.kt
│   │   │   │   │
│   │   │   │   ├── domain/                          ← Domain layer (pure Kotlin)
│   │   │   │   │   ├── model/                       ← Domain models (no Android)
│   │   │   │   │   ├── repository/                  ← Repository interfaces
│   │   │   │   │   └── usecase/                     ← Use cases
│   │   │   │   │       ├── RunAgentTaskUseCase.kt
│   │   │   │   │       ├── SendChatMessageUseCase.kt
│   │   │   │   │       ├── InstallPackageUseCase.kt
│   │   │   │   │       ├── ExecuteTerminalCommandUseCase.kt
│   │   │   │   │       ├── RegisterMiniAppUseCase.kt
│   │   │   │   │       └── SearchMemoryUseCase.kt
│   │   │   │   │
│   │   │   │   ├── agent/                           ← Agent loop system
│   │   │   │   │   ├── AgentLoopManager.kt
│   │   │   │   │   ├── AgentContextBuilder.kt
│   │   │   │   │   ├── ToolDispatcher.kt
│   │   │   │   │   ├── ToolRegistry.kt
│   │   │   │   │   ├── HookMiddleware.kt
│   │   │   │   │   ├── hooks/
│   │   │   │   │   │   ├── DangerousCommandHook.kt
│   │   │   │   │   │   ├── NetworkConsentHook.kt
│   │   │   │   │   │   ├── PathSandboxHook.kt
│   │   │   │   │   │   ├── StorageQuotaHook.kt
│   │   │   │   │   │   └── ApiKeyRedactHook.kt
│   │   │   │   │   ├── SubAgentManager.kt
│   │   │   │   │   ├── MemoryCompactor.kt
│   │   │   │   │   └── ResponseParser.kt           ← XML tool call parser
│   │   │   │   │
│   │   │   │   ├── llm/                             ← LLM engine implementations
│   │   │   │   │   ├── LLMEngine.kt                 ← Interface
│   │   │   │   │   ├── LLMEngineFactory.kt
│   │   │   │   │   ├── LocalGemmaEngine.kt
│   │   │   │   │   ├── OpenRouterEngine.kt
│   │   │   │   │   ├── GeminiEngine.kt
│   │   │   │   │   ├── OllamaEngine.kt
│   │   │   │   │   └── LMStudioEngine.kt
│   │   │   │   │
│   │   │   │   ├── terminal/                        ← Terminal system
│   │   │   │   │   ├── TerminalSession.kt
│   │   │   │   │   ├── TerminalSessionManager.kt
│   │   │   │   │   ├── PkgManager.kt
│   │   │   │   │   ├── RepoManager.kt
│   │   │   │   │   ├── BootstrapExtractor.kt       ← First-run binary extraction
│   │   │   │   │   └── AsciiArtRenderer.kt
│   │   │   │   │
│   │   │   │   ├── memory/                          ← Memory & RAG system
│   │   │   │   │   ├── MarkdownMemory.kt
│   │   │   │   │   ├── VectorIndex.kt
│   │   │   │   │   ├── RAGRetriever.kt
│   │   │   │   │   └── EmbeddingEngine.kt          ← MiniLM-L6 via ONNX Runtime
│   │   │   │   │
│   │   │   │   ├── screens/                         ← Dynamic screen system
│   │   │   │   │   ├── ScreenWatcher.kt
│   │   │   │   │   ├── ScreenManifest.kt
│   │   │   │   │   └── DSLRenderer.kt
│   │   │   │   │
│   │   │   │   ├── chat/                            ← Chat persistence
│   │   │   │   │   ├── ChatRepository.kt
│   │   │   │   │   ├── ChatCompressor.kt
│   │   │   │   │   └── ChatExporter.kt
│   │   │   │   │
│   │   │   │   ├── service/
│   │   │   │   │   ├── AgentShellService.kt        ← Foreground Service
│   │   │   │   │   └── BootReceiver.kt             ← RECEIVE_BOOT_COMPLETED
│   │   │   │   │
│   │   │   │   ├── data/                            ← Data implementations
│   │   │   │   │   ├── db/
│   │   │   │   │   │   ├── AppDatabase.kt
│   │   │   │   │   │   ├── migrations/             ← One file per migration
│   │   │   │   │   │   │   └── Migration_1_2.kt
│   │   │   │   │   │   └── dao/
│   │   │   │   │   │       ├── ChatDao.kt
│   │   │   │   │   │       ├── VectorDao.kt
│   │   │   │   │   │       ├── AppsDao.kt
│   │   │   │   │   │       └── SchedulerDao.kt
│   │   │   │   │   └── datastore/
│   │   │   │   │       └── SettingsDataStore.kt
│   │   │   │   │
│   │   │   │   └── di/                              ← Hilt modules
│   │   │   │       ├── LLMModule.kt
│   │   │   │       ├── DatabaseModule.kt
│   │   │   │       ├── RepositoryModule.kt
│   │   │   │       └── UseCaseModule.kt
│   │   │   │
│   │   │   └── res/
│   │   │       ├── assets/
│   │   │       │   ├── splash/agentshell_logo.ascii
│   │   │       │   ├── figlet_fonts/               ← .flf font files
│   │   │       │   ├── system_prompt.md            ← Agent system prompt
│   │   │       │   └── bootstrap/                  ← ARM64 binaries + packages
│   │   │       └── font/
│   │   │           └── jetbrains_mono.ttf
│   │   └── test/
│   │       └── kotlin/dev/agentshell/
│   │           ├── agent/AgentLoopManagerTest.kt
│   │           ├── terminal/TerminalSessionTest.kt
│   │           ├── memory/RAGRetrieverTest.kt
│   │           ├── screens/DSLRendererTest.kt
│   │           └── llm/ModelValidatorTest.kt
│   │
│   └── build.gradle.kts
│
├── gradle/
│   └── libs.versions.toml                          ← Version catalog (all deps here)
├── .github/
│   └── workflows/
│       ├── ci.yml                                  ← Build + test on every PR
│       ├── cd.yml                                  ← Release builds
│       └── schema-check.yml                        ← Room migration validation
└── settings.gradle.kts
```

---

## 16. REUSABLE COMPONENT LIBRARY

> These components are the building blocks. No screen should re-implement what's here.
> Designed for easy composition and zero conflict.

```kotlin
// ShellPanel.kt — the core container component
// Used everywhere: terminal sections, chat messages, mini-app cards

@Composable
fun ShellPanel(
    header: String? = null,           // "SECTION // detail" format
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .border(Borders.standard, AgentShellColors.Shell3)  // 1dp
            .background(AgentShellColors.Shell1)
    ) {
        header?.let {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .padding(horizontal = Spacing.sm)
                    .border(bottom = Borders.thin, color = AgentShellColors.Shell3),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = it, style = body12, color = AgentShellColors.Amber)
            }
        }
        Column(modifier = Modifier.padding(Spacing.sm), content = content)
    }
}

// ShellButton.kt
@Composable
fun ShellButton(
    label: String,
    style: ButtonStyle = ButtonStyle.PRIMARY,
    enabled: Boolean = true,
    onClick: () -> Unit
) // border only, no fill, no radius, amber/shell3/error border by style

// ShellInput.kt — the "> " terminal input
@Composable
fun ShellInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    placeholder: String = ""
) // amber "> " prefix, blinking block cursor, monospace

// ShellProgressBar.kt — text-based progress
@Composable
fun ShellProgressBar(
    progress: Float,           // 0.0 to 1.0
    width: Int = 24,           // number of segments
    style: ProgressStyle = ProgressStyle.BLOCK   // or EQUALS
) // [████████░░░░░░░░] or [═══════○·········]

// ShellRadar.kt — ASCII 8-axis radar
@Composable
fun ShellRadar(
    values: Map<String, Float>,   // axis name → 0.0..1.0
    updateIntervalMs: Long = 250,
    modifier: Modifier = Modifier
) // Renders and animates the radar using Text composable with mono font

// AgentStepCard.kt — collapsible tool execution card
@Composable
fun AgentStepCard(
    step: AgentStep,
    initiallyExpanded: Boolean = false
) // "step 2/4: write_file → ✓ 45ms" — tappable to expand details

// TierSelectorBar.kt — LOCAL | SELFHOST | API 3-segment control
@Composable
fun TierSelectorBar(
    selected: AgentTier,
    onSelect: (AgentTier) -> Unit
) // 1dp border segments, amber fill on selected, no radius

// ALL components:
// - Use only AgentShellColors values (no hardcoded hex)
// - Use only AgentShellTypography styles (no hardcoded sp)
// - 0dp corner radius everywhere
// - Accept a Modifier parameter for composability
// - Are documented with KDoc
// - Have Preview functions with dark background
```

---

## 17. FUTURE: SSH CLOUD PROCESSING

> This section defines architectural requirements NOW so the app is built
> to support SSH cloud processing WITHOUT a major rewrite.

### 17.1 Design Constraints (Required Now)

```kotlin
// These abstractions must exist from day 1 even if SSH isn't implemented yet:

// 1. ExecutionTarget — where a command runs
sealed class ExecutionTarget {
    object Local : ExecutionTarget()        // Phone's own terminal (v1.0)
    data class SSH(                         // Remote server (future)
        val host: String,
        val port: Int = 22,
        val username: String,
        val authMethod: SshAuth
    ) : ExecutionTarget()
}

// 2. ToolDispatcher already accepts ExecutionTarget
// When SSH is implemented: route run_shell, run_python, write_file
// to the SSH session instead of local ProcessBuilder

// 3. AgentSession can have an ExecutionTarget
data class AgentSession(
    val sessionId: String,
    val chatSessionId: String,
    val executionTarget: ExecutionTarget = ExecutionTarget.Local,
    val llmProvider: ProviderType
)

// 4. ResultStreaming: already uses Flow<String>
// SSH output streams into the same Flow — UI doesn't change at all
// This is the key design win: UI is execution-target-agnostic
```

### 17.2 SSH Architecture (Future Implementation)

```
SSH FEATURE ROADMAP (future phase — architecture ready now):

On-phone → SSH → Cloud Server (RunPod, AWS, GCloud, any VPS):

  Phone                         Server (cloud)
  ────────────────────────────────────────────
  TerminalSession (SSH)  ←────→  sshd (port 22)
  AgentLoopManager       ────→  runs commands via SSH
  FileWatcher            ←────  scp/sftp for file sync
  ResultStream           ←────  stdout/stderr streamed to phone UI

SSH Libraries (to add in future phase):
  JSch (pure Java SSH client) — zero native code, safe for Android
  OR: SSHJ (more maintained, modern API)

SSH Key Management:
  Keys stored in EncryptedSharedPreferences
  Key generation: Android KeyStore (RSA 4096 or Ed25519)
  Known hosts: stored in /files/home/.ssh/known_hosts

Cloud provider configs (future Settings screen):
  [RunPod] [AWS EC2] [Google Cloud] [Azure] [Custom VPS]
  Each: host, port, username, key or password
  Connection test: [PING] → shows latency

File sync (future):
  Agent-created files on server synced back to phone via SFTP
  Phone pushes script files to server before executing
  Uses incremental rsync-like sync (modified files only)
```

---

## 18. CI/CD ARCHITECTURE

### 18.1 GitHub Actions Workflows

```yaml
# .github/workflows/ci.yml — runs on every PR

name: CI
on:
  pull_request:
    branches: [main, develop]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      # Build debug APK
      - name: Build
        run: ./gradlew assembleDebug
      
      # Unit tests (no emulator needed)
      - name: Unit Tests
        run: ./gradlew test
      
      # Room schema migration check
      - name: Schema validation
        run: ./gradlew app:generateRoomSchemas && git diff --exit-code
      
      # Lint
      - name: Lint
        run: ./gradlew lint
      
      # Dependency audit (check for vulnerable deps)
      - name: Dependency audit
        run: ./gradlew dependencyCheckAnalyze

# .github/workflows/cd.yml — runs on main merge (release)
name: CD
on:
  push:
    branches: [main]
    tags: ['v*']

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Build release APK
        run: ./gradlew assembleRelease
        env:
          KEYSTORE_PATH: ${{ secrets.KEYSTORE_PATH }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
      
      # Upload to GitHub Releases
      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          files: app/build/outputs/apk/release/app-release.apk
```

### 18.2 Code Quality Rules (Enforced in CI)

```kotlin
// Detekt configuration (detekt.yml):
// - No functions longer than 40 lines
// - No classes longer than 300 lines
// - Cyclomatic complexity ≤ 10 per function
// - No hardcoded strings (use string resources or constants)
// - No hardcoded colors (use AgentShellColors only)
// - No Thread.sleep() (use delay() instead)
// - All coroutines use structured concurrency (no GlobalScope)
// - All Flows collected in lifecycle-aware scope

// Required coverage (JaCoCo):
// - agent/ package: ≥ 80% line coverage
// - llm/ package: ≥ 70% (engine impls hard to test without hardware)
// - terminal/ package: ≥ 70%
// - components/ package: ≥ 50% (Compose previews count)

// Build variants:
// debug   → logging ON, strict mode ON, test API keys
// release → logging OFF, ProGuard/R8 ON, production keys
// staging → logging ON, ProGuard OFF, staging API keys
```

---

## 19. PERFORMANCE BUDGET

### Pixel 6a (6 GB RAM, Tensor G2, 60Hz)

```
RAM ALLOCATION:
  Android OS + services              ~1.70 GB
  Gemma 2B INT4 (GPU delegate)       ~1.62 GB
  agentShell app process             ~200 MB
  Terminal sessions (×4 max)         ~120 MB  (30 MB each)
  ONNX Runtime + MiniLM-L6           ~80 MB
  SQLite + Room cache                ~60 MB
  OkHttp connection pool             ~30 MB
  Compose rendering                  ~40 MB
  ─────────────────────────────────────────
  USED (typical, all loaded):        ~3.92 GB
  FREE HEADROOM:                     ~2.08 GB
  ─────────────────────────────────────────
  SAFETY RULE: Unload Gemma if freeRAM < 700 MB
               Trim session count if freeRAM < 400 MB

STARTUP TIMES:
  Cold start → terminal ready (model warm):   < 4s
  Cold start → terminal ready (model cold):   < 10s
  New terminal session:                       < 500ms
  Mini-app DSL render:                        < 150ms
  Package mirror ping + select:               < 3s

FRAME RATE (60Hz Pixel 6a):
  All UI: 60fps during idle/navigation
  Terminal scrolling: SurfaceView, 60fps
  During LLM inference: UI stays at 60fps (inference on Default dispatcher)
  NO Compose animations during model loading (causes jank)

BATTERY:
  Foreground Service idle:      < 2%/hour drain
  Active LLM inference:         < 10%/hour drain
  Terminal running scripts:     < 3%/hour drain
  Strategy: yield() between inference batches, use WorkManager for non-urgent tasks

STORAGE:
  APK size:                     < 50 MB (bootstrap extracted separately)
  Bootstrap (first run):        ~180 MB extracted
  Gemma model:                  ~1.62 GB (user-controlled download)
  MiniLM model:                 ~22 MB
  Packages (typical install):   ~300 MB
  Chat sessions (1 year):       < 20 MB (compressed)
```

---

## 20. TDD SPECIFICATION

### 20.1 Unit Tests

```kotlin
// AgentLoopManagerTest.kt

@Test fun `loop stops at max steps for LOCAL tier (4 steps)`() {
    val fakeLLM = FakeLLM(alwaysEmitsToolCall = "read_file")
    val agent = AgentLoopManager(fakeLLM, fakeTools, fakeHooks, fakeCompactor, fakeContext)
    val result = runTest { agent.run("infinite task", "session-1", AgentTier.LOCAL) }
    assertEquals(4, result.stepsExecuted)
    assertIs<AgentResult.MaxDepthReached>(result)
}

@Test fun `LOCAL tier cannot use fetch_url tool`() {
    val hooks = HookMiddleware(fakeConsent, fakeStorage)
    val toolCall = ToolCall(AgentTool.FetchURL, mapOf("url" to "https://example.com"))
    assertThrows<HookException> {
        runTest { hooks.executeWithHooks(toolCall, AgentTier.LOCAL) }
    }
}

@Test fun `DangerousCommandHook blocks rm -rf slash`() {
    val hook = DangerousCommandHook()
    val call = ToolCall(AgentTool.RunShell, mapOf("command" to "rm -rf /"))
    assertThrows<HookException> { runTest { hook.check(call, AgentTier.API) } }
}

@Test fun `memory compaction triggers at 2000 tokens`() {
    val session = buildSession(tokenCount = 2001)
    val compacted = runTest { MemoryCompactor(FakeLLM()).compact(session) }
    assertTrue(compacted.isCompacted)
    assertTrue(compacted.turns.size <= 4)
}

@Test fun `context builder never exceeds 1024 tokens`() {
    val context = runTest { AgentContextBuilder(...).build("build me 20 apps", "session-1") }
    assertTrue(context.totalTokens <= 1024)
}

@Test fun `chat session compresses to under 25% of original size`() {
    val session = buildLargeSession(turns = 100)
    val compressed = ChatCompressor.compress(session)
    assertTrue(compressed.size.toFloat() / session.rawJsonSize < 0.25f)
}

@Test fun `Gemma model validator rejects files under 1.4 GB`() {
    val smallFile = createTempFile(size = 100_000_000)
    val result = runTest { ModelValidator().validate(smallFile) }
    assertIs<ValidationResult.WrongSize>(result)
}

@Test fun `Ollama ping returns failure for unreachable IP`() {
    val engine = OllamaEngine(OllamaConfig(baseUrl = "http://192.0.2.0:11434"))
    val result = runTest { engine.ping() }
    assertIs<PingResult.Failure>(result)
}

@Test fun `DSL renderer produces correct block-char progress bar`() {
    val bar = ShellProgressBarRenderer.render(progress = 0.5f, width = 10, style = BLOCK)
    assertEquals("[█████░░░░░]", bar)
}

@Test fun `ScreenWatcher emits new app event when JSON file appears`() {
    val watcher = ScreenWatcher(tempDir, fakeMemory, fakeRepo, fakeNotif)
    watcher.start()
    val testFile = File(tempDir, "test_app.json")
    testFile.writeText(validScreenDsl)
    runTest { delay(500) }
    verify { fakeRepo.onScreenRegistered(any()) }
}
```

### 20.2 Integration Tests (Instrumented, requires device/emulator)

```kotlin
// Runs on Android 12+ emulator in CI

@Test fun `full agent task creates hydration screen end-to-end`()
@Test fun `pkg install nano succeeds and nano binary is executable`()
@Test fun `terminal Python 3 executes print statement correctly`()
@Test fun `onboarding flow completes to terminal with API provider`()
@Test fun `Gemma model loads under 10 seconds on Pixel 6a`()       // device-only test
```

---

## 21. SECURITY MODEL

### 21.1 Data Storage Security

```
API KEYS:
  Storage: EncryptedSharedPreferences with AES-256-GCM
  Key: Android KeyStore (hardware-backed on Pixel 6a)
  Never stored in: Room DB, .md files, logs, SharedPreferences
  In transit: HTTPS only (certificate pinning for API endpoints in v1.1)

CHAT SESSIONS:
  Location: /data/data/dev.agentshell.app/files/chat_sessions/
  Access: App private (no world-readable)
  Encryption: NOT encrypted by default in v1 (app sandbox is the boundary)
  Future v1.1: per-session encryption with user-supplied PIN

MEMORY FILES (.md):
  Same location, same access rules
  AtomicFile for all writes (no partial write corruption on crash)

TERMINAL:
  All processes: run as app UID (no root, no setuid)
  File writes: PathSandboxHook enforces /data/data/dev.agentshell.app/ boundary
  Network: outbound only, no binding to ports < 1024
```

### 21.2 Permissions (AndroidManifest.xml)

```xml
<!-- Foreground Service — REQUIRED -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC"/>

<!-- Notifications — REQUIRED -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>

<!-- Boot persistence -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>

<!-- Scheduling -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>
<uses-permission android:name="android.permission.USE_EXACT_ALARM"/>
<uses-permission android:name="android.permission.WAKE_LOCK"/>

<!-- Network -->
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>

<!-- Storage (for external model file selection) -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>

<!-- Sensors (for mini-apps) -->
<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION"/>

<!-- Audio (voice input, future) -->
<uses-permission android:name="android.permission.RECORD_AUDIO"/>

<!-- Vibration -->
<uses-permission android:name="android.permission.VIBRATE"/>

<!-- Package install (future APK sideload) -->
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
```

---

## APPENDIX A — Gradle Dependencies (libs.versions.toml)

```toml
[versions]
kotlin = "2.0.0"
agp = "8.5.0"
compose-bom = "2024.06.00"
hilt = "2.51.1"
room = "2.6.1"
coroutines = "1.8.1"
mediapipe = "0.10.14"
onnxruntime = "1.18.0"
okhttp = "4.12.0"
pty4j = "0.12.30"
datastore = "1.1.1"
work = "2.9.0"

[libraries]
# Compose
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-viewmodel = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version = "2.8.3" }

# Hilt DI
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }

# Room
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# LLM
mediapipe-llm = { group = "com.google.mediapipe", name = "tasks-genai", version.ref = "mediapipe" }
onnxruntime = { group = "com.microsoft.onnxruntime", name = "onnxruntime-android", version.ref = "onnxruntime" }

# Networking
okhttp = { group = "com.squareup.okhttp3", name = "okhttp", version.ref = "okhttp" }
okhttp-sse = { group = "com.squareup.okhttp3", name = "okhttp-sse", version.ref = "okhttp" }

# Terminal
pty4j = { group = "org.jetbrains.pty4j", name = "pty4j", version.ref = "pty4j" }

# Storage
datastore = { group = "androidx.datastore", name = "datastore", version.ref = "datastore" }
security-crypto = { group = "androidx.security", name = "security-crypto", version = "1.1.0-alpha06" }

# Background
workmanager = { group = "androidx.work", name = "work-runtime-ktx", version.ref = "work" }

# JSON
kotlinx-serialization = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version = "1.7.1" }

# Testing
junit = { group = "junit", name = "junit", version = "4.13.2" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
mockk = { group = "io.mockk", name = "mockk", version = "1.13.11" }
turbine = { group = "app.cash.turbine", name = "turbine", version = "1.1.0" }
```

---

## APPENDIX B — File System Quick Reference

```
/data/data/dev.agentshell.app/files/
├── home/                    ← Terminal home directory ($HOME)
│   └── projects/            ← User project files
├── memory/
│   ├── context.md           ← User profile + session summaries
│   ├── mistakes.md          ← Agent error log
│   ├── apps.md              ← Mini-app manifest
│   └── calendar.md          ← Scheduled tasks
├── screens/                 ← DSL JSON files (watched by FileObserver)
├── scripts/                 ← LLM-generated Python/JS/bash scripts
├── models/
│   ├── gemma-2b-it-gpu-int4.task
│   └── minilm-l6-v2.onnx
├── chat_sessions/
│   ├── {uuid}.agchat        ← GZIP JSON chat sessions
│   └── archive/
├── packages/
│   └── usr/                 ← Termux-compatible package root
│       ├── bin/             ← Installed binaries (python3, bash, git...)
│       ├── lib/             ← Shared libraries
│       └── etc/apt/         ← APT configuration + sources.list
└── logs/
    ├── agent.log            ← Agent loop execution log (ring, 10 MB max)
    └── terminal.log         ← Terminal session log (ring, 5 MB max)
```

---

*agentShell PRD v2.0 — Single source of truth*
*All implementation must reference this document.*
*Conflicts between this document and any other spec: this document wins.*