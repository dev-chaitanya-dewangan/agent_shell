# agentShell — Product Requirements Document (PRD)

## 1. App Identity & Core Goal
**agentShell** is an autonomous, on-device AI agent for Android. It operates as a bridge between a Large Language Model (LLM) and the Android operating system, allowing the user to control their phone entirely via natural language (voice or text). It does not rely on cloud processing by default, ensuring absolute privacy.

**Tagline:** "Your phone. Your agent. No cloud required."

## 2. Core Features & Capabilities
- **Universal App Control:** The agent can interact with installed apps (e.g., WhatsApp, Chrome) via Accessibility Services (tap, scroll, read, type).
- **Terminal/Shell Access:** Direct execution of Linux commands on the device via Termux bridge integration.
- **Dynamic Voice Interface:** Minimalist voice UI dock allowing hands-free command execution with real-time feedback of the agent's thought process.
- **Mini-App Generation:** The agent can dynamically generate standalone "Mini Apps" (HTML/JS) and save them to the App Hub for immediate user interaction.
- **On-Device LLM Support:** Full integration with local models (e.g., Gemma via MediaPipe) and self-hosted models (Ollama, LM Studio) to maintain privacy.

## 3. Long-Term Vision & Ecosystem (Future Features)
agentShell aims to evolve from a simple mobile app into a **fully automated, cross-device ecosystem**.

### 3.1 The "Everywhere" Agent Ecosystem
- **Multi-Device Sync:** Install agentShell on phone, PC, tablet, and smart home hubs. 
- **Remote Automation:** The user can speak to their phone to control their PC. For example, the agent can connect to the PC over the internet, access files, execute scripts, and perform tasks without the user ever opening the laptop.

### 3.2 Plugin & Add-on Marketplace
- **Developer Plugins:** Third-party developers can build and list "Mini Features" or APIs that agentShell can integrate with.
- **Hardware Integration:** With the right plugins, the agent can interface with IoT devices (Home Automation), initiate 3D prints, or orchestrate complex multi-step workflows across different services.
- **Ethical Monetization:** Core features remain free and private. Advanced integrations, premium cloud-fallback credits, and specialized marketplace plugins will be available via subscription or one-time ethical charges.

## 4. Developer Needs & Architecture
- **Tech Stack:** 100% Kotlin, Jetpack Compose, Room DB, Hilt DI.
- **Architecture:** MVI + Clean Architecture for maximum maintainability.
- **Agent Loop:** Employs a robust PLAN → ACT → OBSERVE loop. Developers must adhere strictly to the established `AgentLoopManager` and `ToolDispatcher` patterns when adding new capabilities.
## 3. DESIGN SYSTEM

### 3.1 Color Palette
## 3. DESIGN SYSTEM

### 3.1 Color Palette

```kotlin
// Theme.kt ΓÇö single source of truth for all colors

object AgentShellColors {

    // BACKGROUND SCALE (darkest ΓåÆ lightest surface)
    val Shell0 = Color(0xFF1C0F09)   // Espresso Black ΓÇö app background
    val Shell1 = Color(0xFF2D1810)   // Dark Brown ΓÇö card/panel backgrounds
    val Shell2 = Color(0xFF4A2C1E)   // Mid Brown ΓÇö elevated surfaces, input BG
    val Shell3 = Color(0xFF6B4030)   // Warm Brown ΓÇö borders, dividers
    val Shell4 = Color(0xFF8C5A3C)   // Tan Brown ΓÇö secondary borders, inactive

    // TEXT SCALE (brightest ΓåÆ most muted)
    val Text0 = Color(0xFFF2E6CC)   // Cream White ΓÇö primary, headings
    val Text1 = Color(0xFFE8D4B0)   // Warm Cream ΓÇö body text
    val Text2 = Color(0xFFC4A882)   // Sand ΓÇö secondary, metadata
    val Text3 = Color(0xFF9A7A5A)   // Muted Tan ΓÇö placeholder, hints
    val Text4 = Color(0xFF6B5540)   // Deep Tan ΓÇö very muted background text

    // ACCENT
    val Amber    = Color(0xFFB89450)  // Primary accent ΓÇö active, fills, highlights
    val AmberLow = Color(0xFF7A5C28)  // Dark amber ΓÇö pressed state

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
    // 9sp is the MINIMUM ΓÇö never go below this
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
    small  = RectangleShape,   // 0dp ΓÇö all buttons, chips
    medium = RectangleShape,   // 0dp ΓÇö all cards, panels
    large  = RectangleShape    // 0dp ΓÇö all bottom sheets, dialogs
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

// Trigger icon: 3 lines, each 18dp wide ├ù 2dp height, 4dp gap between
// Touch target: 48├ù48dp
// Animation: lines 1+3 rotate to X in 250ms, line 2 fades 150ms

// Drawer:
//   Width: 280dp
//   Background: Shell1
//   Border-right: 2dp Shell3  ΓåÉ exact 2dp as required
//   Slide animation: 300ms ease-in-out from left
//   Scrim behind: Shell0 at 60% alpha

// Drawer header (100dp tall):
//   Logo 'a' (40├ù40dp) + "agentShell" (14sp) + version (10sp Text3)
//   Padding: 20dp left, 16dp top

// Nav items (52dp each):
//   Icon: 20├ù20dp, Phosphor outlined style, 2dp stroke  ΓåÉ 2dp as required
//   Label: 13sp JetBrains Mono
//   Active: 3dp left accent bar Amber + Text0 label
//   Inactive: Text2 label, no bar
//   Dividers: 0.5dp Shell3

// BOTTOM NAV BAR:
//   Height: 56dp
//   Background: Shell1
//   Border-top: 2dp Shell3  ΓåÉ 2dp as required
//   Icons: 22├ù22dp outlined, 2dp stroke
//   Labels: 9sp mono
//   Active: Amber icon + label
//   Inactive: Text3
//   Items: Shell | Chat | Apps | Settings
//   New app badge: amber dot on Apps icon
```

---
