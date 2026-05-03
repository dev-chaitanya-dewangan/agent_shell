# agentShell Implementation Plan (80/20 Approach)

## The 80/20 Philosophy
To build `agentShell` rapidly, we focus on the 20% of effort that yields 80% of the core functionality: an on-device agent capable of executing local commands to fulfill user intents.

We will bypass complex UI animations, elaborate settings screens, and cloud-syncing initially, to focus purely on the **Terminal Bridge**, **LLM Integration**, and the **Agent Loop**.

## Phase 1: The Core Foundation (The 20%)

### Step 1: Project Setup & Basic UI Framework
*   **Goal:** Initialize the Native Kotlin project, setup Hilt, and define the `AgentShellColors` and Typography.
*   **Tasks:**
    *   Initialize Gradle with Compose and Hilt dependencies.
    *   Create `Theme.kt` with the Brutalist design tokens (0dp radius, #1C0F09 background).
    *   Create a basic MainActivity with bottom navigation (Shell, Chat, Apps).

### Step 2: The Terminal Bridge
*   **Goal:** Allow the app to execute Linux commands locally and view the output.
*   **Tasks:**
    *   Integrate `pty4j` or a simplified `ProcessBuilder` execution layer for Android.
    *   Build a minimal Compose `TerminalScreen` that maps stdout/stderr to a scrollable text view.
    *   Test basic commands (`ls`, `echo`).
    *   *(Deferred)*: Full VT100 ANSI parsing, multi-session tabs.

### Step 3: LLM Engine Integration
*   **Goal:** Get a working AI endpoint to process prompts.
*   **Tasks:**
    *   Implement the `LLMEngine` interface.
    *   First target: OpenRouter API (easiest to implement, no device constraints) OR Local MediaPipe (if Gemma binary is available).
    *   Build the `AgentContextBuilder` to format the prompt.

### Step 4: The Agent Loop & Tools
*   **Goal:** The LLM can execute terminal commands autonomously.
*   **Tasks:**
    *   Implement `AgentLoopManager` (The `PLAN -> ACT -> OBSERVE` loop).
    *   Implement `ToolDispatcher` with only two tools: `run_shell` and `write_file`.
    *   Connect the loop to the Terminal UI so the user can watch the agent type commands.

## Phase 2: Refinement & Extension (The Next 80%)

Once Phase 1 is proven (The agent can write a file and run a script on the phone), we expand:

### Step 5: Memory & Persistence
*   Implement Room database for chat history.
*   Implement `MarkdownMemory` for appending to `context.md` and `mistakes.md`.
*   Integrate RAG (MiniLM-L6) for semantic search over memory.

### Step 6: Dynamic Screen Rendering
*   Build the `ScreenWatcher` to observe `/files/screens/`.
*   Build the `DSLRenderer` to parse JSON and render Jetpack Compose components dynamically.

### Step 7: Onboarding & Production Polish
*   Implement the 7-screen Onboarding Flow.
*   Build out the remaining tools (Scheduler, Network Fetch with Hooks).
*   Add ASCII art and brutalist polish.
