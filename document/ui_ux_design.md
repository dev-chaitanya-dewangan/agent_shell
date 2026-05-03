# agentShell UI/UX Design

## 1. Design Philosophy: Brutalist & Terminal-First

The UI avoids soft, rounded designs in favor of sharp, dense, terminal-inspired aesthetics.
*   **Strict Rule:** `0dp` corner radius everywhere except the app logo (which has 22%).
*   **Color Theme:** Extremely specific dark tones with an amber accent. No system default light mode.
*   **Data Density:** Screens should show maximum information (e.g., ASCII radars, level meters, dense text logs).

## 2. Color Palette (AgentShellColors)

**Background Scale:**
*   `Shell0` (#1C0F09): Espresso Black — Base app background.
*   `Shell1` (#2D1810): Dark Brown — Panels, cards.
*   `Shell2` (#4A2C1E): Mid Brown — Elevated surfaces.
*   `Shell3` (#6B4030): Warm Brown — Borders.

**Text Scale:**
*   `Text0` (#F2E6CC): Cream White — Primary text.
*   `Text1` (#E8D4B0): Warm Cream — Body text.
*   `Text2` (#C4A882): Sand — Metadata.

**Accents & Semantics:**
*   `Amber` (#B89450): Primary action color, active states.
*   `Success` (#6A9A6A): Muted green (Agent messages).
*   `Error` (#C45040): Muted red.

## 3. Typography (AgentShellTypography)

*   **Primary Font:** `JetBrains Mono` for almost everything (Terminal, UI labels, buttons, metrics).
*   **Fallback/Chat Text:** `Roboto` (system default) can be used for chat message bodies only.
*   **Minimum Size:** `9sp` is the absolute minimum, ensuring readability without wasting space.

## 4. Spacing & Borders

*   **Spacing Units:** `xs` (4dp) to `xxl` (32dp).
*   **Borders:** 
    *   `thin` (0.5dp) for dividers.
    *   `standard` (1dp) for panels and cards.
    *   `accent` (2dp) for navigation items.
    *   `thick` (3dp) for active item left bars.

## 5. UI Layout Elements

### 5.1 Screens Overview

*   **Screen A (Splash):** Full-screen terminal loading. Typewriter ASCII art. Progress bar fills left-to-right.
*   **Screen B (Terminal Dashboard):** Multi-pane brutalist panels. Includes scrolling logs, horizontal `═══════○··` meters, and an amber `>_` input pinned to the bottom.
*   **Screen C (Data Detail/Mini-Apps):** Rendered dynamically from JSON. Features an ASCII 8-axis radar, 1dp bordered action buttons, and monospaced text.

### 5.2 Component Library

*   **ShellPanel:** Container with a 1dp border and optional uppercase header.
*   **ShellButton:** Transparent background with an Amber 1dp border. Text is bold mono, uppercase. Press state briefly flashes `Shell1`.
*   **ShellInput:** Input field with a permanent `> ` amber prefix and a blinking cursor.
*   **TierSelectorBar:** Compact 3-segment toggle for switching between LOCAL, SELF-HOSTED, and API agent tiers.
