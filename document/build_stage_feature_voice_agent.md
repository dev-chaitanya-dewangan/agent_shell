# AgentShell — Build Stage: Voice Agent Features
> **Feature:** Voice-Controlled Autonomous Agent (Demo-Ready)
> **Created:** 2026-05-11 | **Author:** Chaitanya + Antigravity
> **Target:** 2 working viral demos + voice pipeline foundation

---

## 📦 Codebase Audit (First Principles)

### What We Have — Strengths to Build On

| Component | File | State |
|---|---|---|
| Agent Loop (PLAN→ACT→OBSERVE) | `agent/AgentLoopManager.kt` | ✅ Working |
| Tool Dispatcher | `agent/ToolDispatcher.kt` | ✅ Working, needs new tools |
| Accessibility Service | `accessibility/AgentAccessibilityService.kt` | ✅ Registered, has tapAt / typeText / findAndTap / getScreenTree |
| `open_app` tool | `ToolDispatcher.kt` line 73 | ❌ TODO stub — critical blocker |
| Chat UI → Agent wiring | `chat/ChatViewModel.kt` | ✅ Works, no voice input yet |
| LLM Engine (Gemini/OpenRouter) | `llm/DynamicLLMEngine.kt` | ✅ Streaming works |
| Hilt DI | `di/AppModule.kt` | ✅ Clean, easy to extend |

### Critical Gaps (20% blocking 80% of demos)

1. **`open_app` is a stub** — every demo starts with opening an app
2. **No voice input** — demos need hands-free trigger
3. **No `speak` tool** — agent can't read results aloud
4. **No `ui_scroll` tool** — needed to navigate lists (WhatsApp contacts)
5. **No `wait_ms` tool** — apps need settle time after opening
6. **No `read_screen_text` tool** — for Gallery search result extraction

---

## 🎯 The Two Demos

### Demo A — "Messy Cooking Hands"
> Say: *"Send Priya on WhatsApp — running 15 minutes late"*

**Tool chain:**
```
open_app(package="com.whatsapp")
→ wait_ms(1500)
→ ui_find_and_tap("Search")
→ ui_type(text="Priya")
→ ui_find_and_tap("Priya")
→ ui_type(text="Running 15 minutes late")
→ ui_find_and_tap("Send")
→ speak(text="Done! Message sent to Priya.")
```

### Demo B — "Where Is It? Search"
> Say: *"Find my WiFi password screenshot"*

**Tool chain:**
```
open_app(package="com.google.android.apps.photos")
→ wait_ms(1500)
→ ui_find_and_tap("Search")
→ ui_type(text="wifi password")
→ wait_ms(2000)
→ ui_get_screen
→ read_screen_text  ← flattens accessibility tree to plain text
→ speak(text=<extracted text>)
```

---

## 🔮 Demo C — Cross-App Sharing (FUTURE, Do Not Build Now)
> Say: *"Send this Instagram reel to Rahul on WhatsApp"*

**Planned tool chain:**
```
ui_get_screen → find share button in Instagram
→ ui_find_and_tap("Share") → ui_find_and_tap("WhatsApp")
→ ui_type("Rahul") → ui_find_and_tap("Rahul") → ui_find_and_tap("Send")
```
> ⚠️ Deferred — Instagram uses a non-standard share sheet. Build only after demos A + B are shipped and recorded.

---

## ✅ Master Checklist (Ordered by Dependency)

### PHASE 0 — Foundation Fixes (Unblocks Everything)
- [ ] **0.1** Fix `open_app` stub in `ToolDispatcher.kt` → use `packageManager.getLaunchIntentForPackage(pkg)`
- [ ] **0.2** Add `wait_ms` tool to `ToolDispatcher` → `delay(params["ms"].toLong())`
- [ ] **0.3** Add `ui_scroll` tool → `AccessibilityService.performGlobalAction(GLOBAL_ACTION_SCROLL_DOWN)`
- [ ] **0.4** Add `read_screen_text` tool → recursive flatten of accessibility tree to plain text
- [ ] **0.5** Update `accessibility_service_config.xml` → add `flagRequestFilterKeyEvents`
- [ ] **0.6** Add new tool schemas to system prompt in `AgentLoopManager.kt`

### PHASE 1 — Voice Input Pipeline
- [ ] **1.1** Create `voice/VoiceInputManager.kt` → wraps Android `SpeechRecognizer`, emits `Flow<String>`
- [ ] **1.2** Add `RECORD_AUDIO` permission to `AndroidManifest.xml`
- [ ] **1.3** Add mic FAB to `ChatScreen.kt` → hold-to-speak, releases to submit
- [ ] **1.4** Wire `VoiceInputManager` → `ChatViewModel.onIntent(InputChanged + SubmitTask)`
- [ ] **1.5** Create `voice/TextToSpeechManager.kt` → singleton TTS with `speak(text)` suspend fun
- [ ] **1.6** Add `speak` tool to `ToolDispatcher` → calls `TextToSpeechManager.speak(text)`
- [ ] **1.7** Provide both in `AppModule.kt` as `@Singleton`

### PHASE 2 — Demo A (WhatsApp Hands-Free Message)
- [ ] **2.1** Test `open_app("com.whatsapp")` → confirm WhatsApp opens on device
- [ ] **2.2** Test `ui_find_and_tap("Search")` in WhatsApp → confirm search bar activates
- [ ] **2.3** Test `ui_type("Priya")` in search → confirm contacts filter
- [ ] **2.4** Test `ui_find_and_tap("Priya")` → confirm chat opens
- [ ] **2.5** Test `ui_type` in message box + `ui_find_and_tap("Send")` → confirm send
- [ ] **2.6** Tune system prompt: add WhatsApp flow as a worked example (few-shot)
- [ ] **2.7** Add `whatsapp_message` convenience tool (single-call wrapper for whole chain)
- [ ] **2.8** End-to-end voice test: speak command → message sent ✅

### PHASE 3 — Demo B (Gallery / Screenshot Search)
- [ ] **3.1** Test `open_app("com.google.android.apps.photos")` → confirm Photos opens
- [ ] **3.2** Test search + type flow in Google Photos
- [ ] **3.3** Test `read_screen_text` output — confirm password/text is visible in result
- [ ] **3.4** Wire `speak` tool to read the found text aloud
- [ ] **3.5** End-to-end voice test: speak command → agent reads text aloud ✅

### PHASE 4 — Polish for Recording
- [ ] **4.1** Add real-time step overlay: `"Step 2/5: Opening WhatsApp..."` in chat
- [ ] **4.2** Tune `wait_ms` delays for smooth visual pacing in screen recording
- [ ] **4.3** Add `be_concise=true` hint in demo system prompt (less LLM chatter)
- [ ] **4.4** Physical device test on Pixel 6a — confirm 15-second end-to-end timing
- [ ] **4.5** Record Demo A and Demo B back-to-back in single session

---

## 🏗️ Architecture After This Feature

```
VOICE INPUT
    ↓
VoiceInputManager (SpeechRecognizer)
    ↓
ChatViewModel.onIntent(InputChanged + SubmitTask)
    ↓
AgentLoopManager.run(task)         [Mutex — one loop at a time]
    ↓  [LLM reasons, emits tool JSON]
ToolDispatcher.dispatch(tool, params)
    ├── open_app         → startActivity(getLaunchIntentForPackage)
    ├── wait_ms          → delay(ms)
    ├── ui_find_and_tap  → AccessibilityService.findAndTap()
    ├── ui_type          → AccessibilityService.typeText()
    ├── ui_scroll        → AccessibilityService.scrollNode()
    ├── ui_get_screen    → AccessibilityService.getScreenTree() [JSON]
    ├── read_screen_text → flatten tree → plain text
    ├── speak            → TextToSpeechManager.speak(text)
    └── whatsapp_message → [convenience: wraps full WA chain]

VOICE OUTPUT
    ↑
TextToSpeechManager.speak(result)
```

---

## 📁 Files to Create

| File | Purpose |
|---|---|
| `voice/VoiceInputManager.kt` | SpeechRecognizer wrapper, Flow<String> |
| `voice/TextToSpeechManager.kt` | TTS singleton, suspend speak() |

## 📝 Files to Modify

| File | What Changes |
|---|---|
| `agent/ToolDispatcher.kt` | Add: open_app, wait_ms, ui_scroll, read_screen_text, speak, whatsapp_message |
| `agent/AgentLoopManager.kt` | Add new tool schemas to system prompt |
| `accessibility/AgentAccessibilityService.kt` | Add scrollNode(), findByContentDesc() |
| `res/xml/accessibility_service_config.xml` | Add flagRequestFilterKeyEvents |
| `AndroidManifest.xml` | Add RECORD_AUDIO permission |
| `chat/ChatScreen.kt` | Add mic FAB button (hold-to-speak) |
| `chat/ChatViewModel.kt` | Add StartVoiceInput intent handler |
| `di/AppModule.kt` | Provide VoiceInputManager, TextToSpeechManager |

---

## 80/20 Prioritization

**Do these first (20% effort, 80% demo value):**
1. Fix `open_app` → unblocks ALL demos immediately
2. Add `wait_ms` → makes agent reliable (apps need time to settle)
3. Add mic FAB → makes it visually hands-free
4. Add `speak` → closes the feedback loop (agent talks back)
5. Add `whatsapp_message` convenience tool → Demo A in 1 LLM call

**Skip for now:**
- OCR via MediaProjection (accessibility tree text is enough)
- Instagram cross-app sharing (non-standard API)
- Always-on background voice (needs Foreground Service, v2 scope)
- RAG memory (not needed for demos)

---

## ⚡ Build Timeline

```
Day 1:  Phase 0 (all 6 foundation fixes) → build + verify
Day 2:  Phase 1.1–1.4 (voice input → chat auto-submit)
Day 3:  Phase 1.5–1.7 (TTS speak) + Phase 2.1–2.5 (WhatsApp chain)
Day 4:  Phase 2.6–2.8 (Demo A end-to-end) → RECORD Demo A
Day 5:  Phase 3 (Gallery search, Demo B) → RECORD Demo B
Day 6:  Phase 4 (Polish) → Final recording session
```

---

## 🚀 Start Here Right Now — Task 0.1

```kotlin
// ToolDispatcher.kt — replace open_app TODO stub:
"open_app" -> {
    val pkg = params["package"] ?: return@flow emit("[Error: No package]")
    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        ?: return@flow emit("[Error: App not installed: $pkg]")
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    emit("[App opened: $pkg]")
}
```

This single 8-line fix unblocks Demo A completely. Build and test this first.
