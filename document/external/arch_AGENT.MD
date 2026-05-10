Based on the provided document, here is a detailed breakdown of the architecture and working of Claude Code (and similar autonomous agents like Hermes).

### System Architecture

The architecture is fundamentally built on a **Middleware-Driven Asynchronous Event Loop**. This structure transforms the LLM from a simple generation engine into a stateful compute orchestrator.

Key architectural principles include:

* 
**Separation of Concerns:** The architecture strictly separates the LLM's reasoning faculties from the execution of side effects.


* 
**Web Framework Pattern:** By structuring the agent like a modern web framework (Router → Middleware → Controller/Tool → Response), the system achieves extreme fault tolerance. This prevents the monolithic failures that normally occur when context windows degrade or tools crash.


* 
**Layered Memory System:** Memory is not a flat text file; it relies on hierarchical retrieval. It uses Markdown files (like `MEMORY.md` and `USER.md`) for human-readable, token-efficient long-term state persistence.



### How It Works: The Execution Loop

When a user provides a task, the agent enters a recursive lifecycle rather than returning a single, static answer.

1. 
**Context Hydration:** The orchestrator reads the user's filesystem to pull global personality rules (`SOUL.md`), user preferences (`USER.md`), past state (`MEMORY.md`), and project conventions (`CLAUDE.md`). It also fetches a compressed index of available skills.


2. 
**Ingestion & Planning:** The LLM receives the prompt and generates a structured thought block (Chain-of-Thought) outlining a multi-step plan instead of answering immediately.


3. 
**Tool Selection:** The LLM emits a standardized JSON schema to invoke a specific tool, such as a bash command or file reader.


4. 
**Pre-Execution Hooks (Middleware):** The system intercepts the tool call to run safety scanners (looking for malicious commands) or uses a secondary LLM to risk-rate the action.


5. 
**Tool Execution & Error Wrapping:** The tool runs in an isolated backend. If the tool fails (e.g., a syntax error), the system actively prevents a crash. Instead, the exception is caught, wrapped in a JSON payload, and returned to the loop.


6. 
**Reflection & Retry Loop:** Operating on a closed-loop feedback system, the LLM observes the actual state versus its predicted state. If it sees an error payload, the agent shifts into "diagnostic mode," reading the stack trace to identify the mistake, modify the parameters, and re-trigger the tool.


7. 
**Loop Termination:** Once the task is successfully completed, the agent returns the final result to the user.