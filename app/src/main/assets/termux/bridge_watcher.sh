#!/data/data/com.termux/files/usr/bin/bash
# AgentShell Bridge Watcher v2
# Bridge dir MUST match TermuxBridgeRepository.bridgeDir in the Android app
BRIDGE_DIR="/sdcard/Download/agentshell/bridge"

mkdir -p "$BRIDGE_DIR"
echo "[AgentShell Bridge] Watcher started. Watching: $BRIDGE_DIR"
echo "[AgentShell Bridge] PID: $$"

while true; do
  for cmd_file in "$BRIDGE_DIR"/cmd_*.json; do
    [ -f "$cmd_file" ] || continue

    # Parse JSON using python3 (available in Termux by default)
    CMD_ID=$(python3 -c "import json; d=json.load(open('$cmd_file')); print(d['id'])" 2>/dev/null)
    COMMAND=$(python3 -c "import json; d=json.load(open('$cmd_file')); print(d['command'])" 2>/dev/null)

    if [ -z "$CMD_ID" ]; then
      echo "[Bridge] Failed to parse $cmd_file — skipping"
      rm -f "$cmd_file"
      continue
    fi

    rm -f "$cmd_file"
    echo "[Bridge] Executing [$CMD_ID]: $COMMAND"

    # Run the command, capture stdout+stderr
    OUTPUT=$(eval "$COMMAND" 2>&1)

    # Write result — use python3 with a variable, NOT stdin/heredoc (fragile in Termux)
    python3 - "$CMD_ID" "$OUTPUT" "$BRIDGE_DIR" << 'PYEOF'
import sys, json, os
cmd_id = sys.argv[1]
output = sys.argv[2]
bridge_dir = sys.argv[3]
result_path = os.path.join(bridge_dir, f"result_{cmd_id}.json")
with open(result_path, 'w') as f:
    json.dump({"id": cmd_id, "output": output}, f)
PYEOF

    echo "[Bridge] Result written for [$CMD_ID]"
  done
  sleep 0.2
done
