---
trigger: always_on
---

fixations and bugged things should be mentioned in the file .agents\rules\fixations.md so the future agents dont repeat that same mistakes.

## Known Bugs & Fixes

### [BRIDGE] Termux bridge timeout despite watcher running
**Root cause (3 issues):**
1. **Path mismatch** — App writes to `/sdcard/Download/agentshell/bridge` but old watcher script used `/sdcard/agentshell/bridge`. These MUST be identical.
2. **Python stdin heredoc** — `python3 -c "..." <<< "$OUTPUT"` fails silently in Termux. Use `python3 - arg1 arg2 << 'PYEOF'` inline script pattern instead.
3. **Short timeout** — `executeInTermux` had 3s default. Changed to 30s.

**Rule:** `TermuxBridgeRepository.bridgeDir` path and `BRIDGE_DIR` in `bridge_watcher.sh` MUST always be the same string: `/sdcard/Download/agentshell/bridge`.

**Files:** `agent/TermuxBridgeRepository.kt`, `assets/termux/bridge_watcher.sh`

### [EACCES] "open failed: EACCES (Permission denied)" on /sdcard bridge dir
**Two independent causes:**
1. **Android app** — `MANAGE_EXTERNAL_STORAGE` declared in manifest is NOT auto-granted on Android 11+. Must call `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` intent at runtime and have the user grant it. Use `Environment.isExternalStorageManager()` to check.
2. **Termux** — Termux cannot access `/sdcard` until the user has run `termux-setup-storage` once inside Termux. This is a one-time setup step.

**Fix pattern:** `TerminalViewModel.runBridgeSelfTest()` — checks `isExternalStorageManager()`, tries a write to bridge dir, shows coloured error lines + permission banner in UI. User taps banner → `ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` intent opens system settings.

**Files:** `terminal/TerminalViewModel.kt`, `terminal/TerminalScreen.kt`
