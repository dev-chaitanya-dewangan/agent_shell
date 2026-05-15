# How agentShell Communicates with Termux

## Architecture Overview

The agentShell app does not actually require a standalone Termux app to be installed. Instead, it **bundles a fully compatible Termux environment directly within the app itself**. This allows the app to act as an AI wrapper with deep integration into a local Linux environment.

Here is how the communication and execution pipeline works:

1. **The Sandboxed Environment (proot + bash):**
   The app sets up a sandboxed `proot` environment with its own file system (`/files/usr`). It ships with statically linked ARM64 binaries extracted on first launch, including `bash`, `Python 3.11`, `Node.js 20 LTS`, and the `pkg`/`apt` package managers.

2. **The Terminal Bridge (pty4j):**
   The app uses a pseudo-terminal (PTY) bridge via the `pty4j` library. This bridge creates master/slave file descriptors. The UI reads stdout/stderr from the master file descriptor, and standard input is written to it.

3. **The Agent Tool System:**
   The Local LLM (e.g., Gemma 2B) running inside the app interacts with this environment via its structured Tool System (defined in `AgentTool` XML schemas). The LLM can emit tool calls like `<tool_call><name>run_shell</name>...</tool_call>`.

   The available Termux-related tools are:
   * `run_shell`: Executes a bash command in the PTY.
   * `run_python`: Executes a Python script.
   * `run_node`: Executes a Node.js script.
   * `pkg_install`: Uses the Termux package manager (`pkg` / `apt-get`) to install new packages.

4. **Execution and Observation:**
   When the LLM decides to run a command, the app's `ToolDispatcher` executes the command via the PTY bridge. The agent writes its commands to a named pipe connected to the PTY, and the output is streamed back to the agent as an observation. This loop continues until the agent has completed its task or reached its maximum allowed steps.

## Example User Prompts and Agent Capabilities

Because the LLM has direct access to the `pkg_install` and `run_shell` tools within this Termux-compatible environment, it can perform highly complex operations autonomously.

Here is a list of example prompts a user can ask, and what the agent will do under the hood:

| User Prompt | What the Agent Will Do (Under the hood) |
| :--- | :--- |
| **"Write a Python script to calculate the first 100 Fibonacci numbers and run it."** | 1. Uses `write_file` to create `fib.py`. <br> 2. Uses `run_python` to execute the script and observe the output. |
| **"Install the `htop` package and tell me how much RAM is free."** | 1. Uses `pkg_install` tool with parameter `htop`. <br> 2. Uses `run_shell` to execute `htop -b -n 1` or parses `/proc/meminfo`. |
| **"Download the latest Bitcoin price using curl and save it to a file."** | 1. Uses `run_shell` to run `curl -s https://api.coindesk.com/... > price.json`. <br> 2. Uses `read_file` to read the price and inform the user. |
| **"Create a Node.js server that listens on port 3000 and returns 'Hello World'."** | 1. Uses `write_file` to write `server.js`. <br> 2. Uses `run_node` (or `run_shell` with `node server.js &`) to start the server in the background. |
| **"Find all Markdown files in my projects folder that mention 'API'."** | 1. Uses `pkg_install` to install `ripgrep` (if not already installed). <br> 2. Uses `run_shell` to execute `rg "API" /data/data/.../files/home/projects/*.md`. |
| **"Set up a git repository in `~/projects/my_app` and commit the current files."** | 1. Uses `run_shell` to run `git init`, `git add .`, and `git commit -m "Initial commit"` in the specified directory. |
| **"Write a bash script that checks if my website is up every 5 minutes."** | 1. Uses `write_file` to write a bash script. <br> 2. Uses `run_shell` or the agent's scheduler tools (`schedule_work`) to execute it periodically. |
| **"Install rust and compile this rust program for me."** | 1. Uses `pkg_install` to install `rust`. <br> 2. Uses `write_file` to create `main.rs`. <br> 3. Uses `run_shell` to run `rustc main.rs` and `./main`. |
