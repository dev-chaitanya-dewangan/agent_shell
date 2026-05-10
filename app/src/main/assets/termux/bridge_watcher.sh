#!/data/data/com.termux/files/usr/bin/bash
BRIDGE_DIR="/sdcard/Download/agentshell/bridge"
mkdir -p "$BRIDGE_DIR"
echo "[AgentShell Bridge] Watcher started at $BRIDGE_DIR"
while true; do
  for cmd_file in "$BRIDGE_DIR"/cmd_*.json; do
    [ -f "$cmd_file" ] || continue
    CMD_ID=$(python3 -c "import json,sys; d=json.load(open('$cmd_file')); print(d['id'])")
    COMMAND=$(python3 -c "import json,sys; d=json.load(open('$cmd_file')); print(d['command'])")
    rm "$cmd_file"
    OUTPUT=$(eval "$COMMAND" 2>&1)
    python3 -c "import json; json.dump({'id':'${CMD_ID}','output':open('/dev/stdin').read()},open('${BRIDGE_DIR}/result_${CMD_ID}.json','w'))" <<< "$OUTPUT"
  done
  sleep 0.3
done
