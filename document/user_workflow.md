# agentShell User Workflow

## 1. Onboarding Journey

1.  **Welcome & Intro:** ASCII splash screen leading to app explanation.
2.  **Engine Selection:** User picks an AI engine (Local, API, or Self-hosted).
3.  **Download Flow (If Local chosen):**
    *   Shows 1.62GB size warning.
    *   User can download via app (Foreground Service) or externally (browser).
    *   Provides file validation.
4.  **API Config (If API chosen):** Real-time key validation or IP Ping for self-hosted.
5.  **Permissions:** Batched request (Notifications, FS, Storage, Alarms) with explanations.
6.  **Initialization System Check:** Terminal text UI showing "Mounting FS", "Extracting Bootstraps", "Loading LLM".

## 2. Daily App Usage (Primary Workflows)

### 2.1 Direct Terminal Use
*   User opens the "Shell" tab.
*   Has access to standard Linux commands (`bash`, `python3`, `git`).
*   Can install packages via `pkg install`.
*   Can manage up to 4 concurrent terminal tabs.

### 2.2 Chat / Task Delegation
*   User opens the "Chat" tab.
*   Selects the Tier (`[LOCAL | SELFHOST | API]`).
*   Types a command: "Build me a hydration reminder app".
*   Observes the agent's thought process stream live.
*   Approves any network requests via popup dialogs.
*   The final output is generated and the user is notified.

### 2.3 Mini-App Consumption
*   Agent writes a `hydration.json` to the `/screens/` directory.
*   The `ScreenWatcher` detects the file, registers it, and shows a notification.
*   User opens "Apps" tab, taps the new icon.
*   The DSL renderer dynamically constructs the screen from JSON.
*   User interacts with buttons which write to `local_kv` or execute local scripts.

## 3. Recovery Workflows

*   **Agent Errors:** If a task fails, the agent writes the failure to `mistakes.md`. On the next attempt, RAG includes this mistake so the agent avoids doing the same thing.
*   **App Reboot:** The Foreground Service auto-starts on boot. Terminal buffers are loaded from disk, and running alarms/workers re-attach seamlessly.
