# agentShell Processing & Agent Architecture

## 1. Agent Loop (The Claude Code Loop)

The Agent relies on a strict Reasoning Loop managed by `AgentLoopManager`, acting as a ReAct-style agent. 
A Mutex ensures only one loop runs at a time.

**Steps per Iteration:**
1.  **PLAN (Context Building):** 
    `AgentContextBuilder` constructs a prompt (< 1024 tokens) containing:
    *   System Prompt (~200 t)
    *   RAG Embeddings (top 3 relevant chunks, ~300 t)
    *   Recent session history (~300 t)
    *   Available tools XML schema (~100 t)
    *   Task context (~150 t)
2.  **THINK (Inference):** 
    The LLM streams output. Parsed dynamically.
3.  **ACT (Tool Execution):** 
    If the LLM outputs an XML tool call, `ToolDispatcher` executes it.
    *Crucial Step:* Tool execution is intercepted by `HookMiddleware` for safety and constraints.
4.  **OBSERVE:** 
    The result is appended to step history.
5.  **REFLECT:** 
    Upon completion or reaching max steps, the agent summarizes success in `context.md` or records failure in `mistakes.md`.

## 2. LLM Provider System

Abstracted behind `LLMEngine`. All engines implement `generate(streaming)`, `complete(single-shot)`, `countTokens`, and `ping`.

**Supported Providers:**
*   **LOCAL_GEMMA:** Runs via MediaPipe (`.task` models). 
*   **OPENROUTER / GOOGLE_GEMINI:** Standard API interfaces.
*   **SELF_HOSTED (Ollama / LMStudio):** Reaches out to local network IP addresses.

## 3. Tier System

Agent behavior is limited based on the compute tier selected by the user.

*   **Tier 1: LOCAL (Gemma)**
    *   Max Steps: 4
    *   Tools: All file/shell/memory tools. NO network, NO sub-agents.
    *   Best for: Offline tasks, reminders, basic scripts.
*   **Tier 2: SELF-HOSTED**
    *   Max Steps: 8
    *   Tools: Includes network tools (`fetch_url`). NO sub-agents.
*   **Tier 3: API (Cloud/Gemini)**
    *   Max Steps: 12
    *   Tools: All 22 tools, including `spawn_agent`.
    *   Best for: Building complex mini-apps.

## 4. Hook Middleware

A safety layer wrapping all tool execution.
*   **PreHooks:** Check conditions before running (e.g., `PathSandboxHook` prevents breaking out of app dir, `NetworkConsentHook` prompts UI for user approval, `DangerousCommandHook` blocks `rm -rf /`).
*   **PostHooks:** Clean up data (e.g., `ApiKeyRedactHook`, `OutputTruncationHook`).

## 5. Sub-Agent Orchestration

(API Tier Only)
`SubAgentManager` allows spawning up to 3 concurrent worker loops to handle complex tasks (e.g., "build a full news app" could dispatch an agent to write logic, another to write UI, another to test).
