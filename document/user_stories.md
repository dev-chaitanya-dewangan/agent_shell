# agentShell User Stories

## P0 — MUST HAVE FOR v1.0
These are critical to the core experience of the app as a functioning on-device agent.

*   **US-001:** First launch shows ASCII splash → onboarding (no data downloaded yet).
*   **US-002:** User warned with exact size before ANY download starts.
*   **US-003:** User can download Gemma internally OR copy link and download externally.
*   **US-004:** User can select an already-downloaded Gemma .task or .bin file from storage.
*   **US-005:** User can choose Ollama/LM Studio and enter IP address with instant ping test.
*   **US-006:** API keys validated instantly in onboarding (green/red feedback).
*   **US-007:** Terminal works with Python, bash, git, curl on first launch (no extra setup).
*   **US-008:** `pkg install` works and uses fastest available mirror auto-selected.
*   **US-009:** User can type "build me a hydration reminder" and get a working mini-app.
*   **US-010:** Agent shows live steps (step 1/4, tool name, result) in chat.
*   **US-011:** New mini-app appears in Apps without restart.
*   **US-012:** Chat sessions auto-save, compress, and resume correctly.
*   **US-013:** Local agent (Gemma) limited to 4 steps — complex tasks suggest API upgrade.

## P1 — SHOULD HAVE FOR v1.0
Important features that complete the intended design and workflow.

*   **US-020:** Hamburger nav shows all sections, 2dp borders, amber active state.
*   **US-021:** Bottom nav has 2dp top border, amber active, outlined 2dp icons.
*   **US-022:** ASCII art renders in terminal with `ascii-art` command (FIGlet fonts).
*   **US-023:** Terminal has multi-session tabs (up to 4 sessions).
*   **US-024:** Memory `.md` files viewable in terminal with `mem show` command.
*   **US-025:** `mistakes.md` auto-updated on agent errors, used in next task context.
*   **US-026:** Settings → AI Engine allows switching provider + configuring all options.
*   **US-027:** Gemma model can be re-downloaded or replaced (path re-selectable in settings).

## P2 — NICE TO HAVE FOR v1.0
Enhancements that can be deferred if timelines are tight.

*   **US-030:** Sub-agents for complex tasks (API tier only).
*   **US-031:** Sensor integration (steps, battery) for mini-apps.
*   **US-032:** Chat session export as `.md` file.
*   **US-033:** ASCII radar display for sensor data (Screen C style).
*   **US-034:** Custom repo URLs added in settings.
