package dev.agentshell.app.agent

import dev.agentshell.app.brain.BrainLogger
import dev.agentshell.app.brain.HermesContextBuilder
import dev.agentshell.app.brain.LogType
import dev.agentshell.app.llm.LLMEngine
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed class AgentState {
    object Idle : AgentState()
    data class Planning(val task: String) : AgentState()
    data class Thinking(val step: Int, val maxSteps: Int) : AgentState()
    data class Streaming(val partial: String) : AgentState()
    data class Acting(val tool: String, val params: Map<String, String>, val step: Int) : AgentState()
    object Reflecting : AgentState()
}

sealed class AgentResult {
    data class Success(val message: String, val stepsExecuted: Int) : AgentResult()
    data class MaxDepthReached(val stepsExecuted: Int) : AgentResult()
    data class Failure(val reason: String) : AgentResult()
}

@Singleton
class AgentLoopManager @Inject constructor(
    private val llmEngine: LLMEngine,
    private val toolDispatcher: ToolDispatcher,
    private val hermesContextBuilder: HermesContextBuilder,
    private val brainLogger: BrainLogger
) {
    private val loopMutex = Mutex()
    
    private val _agentState = MutableStateFlow<AgentState>(AgentState.Idle)
    val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

    suspend fun run(task: String, maxSteps: Int = 12): AgentResult {
        return loopMutex.withLock {
            runLoop(task, maxSteps)
        }
    }

    private suspend fun runLoop(task: String, maxSteps: Int): AgentResult = withContext(Dispatchers.Default) {
        var stepCount = 0
        var contextHistory = "Task: $task\n\n"

        brainLogger.log(LogType.TASK_START, "NEW TASK", task)

        val systemPrompt = hermesContextBuilder.buildSystemPrompt() + """
            
            You have access to the following tools. Call them using JSON format only.

            TOOL SCHEMAS:
            {"tool": "run_shell",          "params": {"command": "<bash command>"}}
            {"tool": "run_termux",         "params": {"command": "<bash command>"}}
            {"tool": "write_file",         "params": {"path": "<abs path>", "content": "<text>"}}
            {"tool": "read_file",          "params": {"path": "<abs path>"}}
            {"tool": "list_dir",           "params": {"path": "<abs path>"}}
            {"tool": "ui_tap",             "params": {"x": "100", "y": "200"}}
            {"tool": "ui_type",            "params": {"text": "<text to type>"}}
            {"tool": "ui_find_and_tap",    "params": {"text": "<visible button or element label>"}}
            {"tool": "ui_get_screen",      "params": {}}
            {"tool": "ui_scroll",          "params": {"direction": "down"}}
            {"tool": "open_app",           "params": {"package": "<android package name>"}}
            {"tool": "wait_ms",            "params": {"ms": "1500"}}
            {"tool": "read_screen_text",   "params": {}}
            {"tool": "speak",              "params": {"text": "<text to say aloud to the user>"}}
            {"tool": "take_screenshot",    "params": {}}
            {"tool": "whatsapp_message",   "params": {"contact": "<contact name>", "message": "<message text>"}}
            {"tool": "create_mini_app",    "params": {
                "name": "<display name>",
                "description": "<one line description>",
                "html": "<full self-contained HTML string for the mini-app>"
            }}

            TOOL USAGE RULES:
            - Use open_app to launch apps. Common packages:
              WhatsApp="com.whatsapp", Chrome="com.android.chrome",
              YouTube="com.google.android.youtube", Photos="com.google.android.apps.photos",
              Spotify="com.spotify.music", Gmail="com.google.android.gm",
              Ola="com.olacabs.customer", Uber="com.ubercabs.rider"
            - ALWAYS call wait_ms(1500) immediately after open_app to let the app load
            - Use whatsapp_message for any "send message on WhatsApp" task — it handles the full chain
            - Use read_screen_text to extract all visible text for summarization or searching
            - Use speak to read results aloud to the user after completing a task
            - Use ui_find_and_tap for labeled buttons; use ui_tap(x,y) only when no label exists
            - create_mini_app saves the app to the APPS tab permanently — provide full self-contained HTML

            When finished with all steps and no more tool calls are needed,
            respond with plain text (no JSON) to complete the task.
        """.trimIndent()

        _agentState.value = AgentState.Planning(task)

        while (stepCount < maxSteps) {
            stepCount++
            _agentState.value = AgentState.Thinking(stepCount, maxSteps)

            val llmResponse = StringBuilder()
            
            try {
                llmEngine.generate(
                    prompt = contextHistory,
                    systemPrompt = systemPrompt
                ).collect { token ->
                    llmResponse.append(token)
                    _agentState.value = AgentState.Streaming(llmResponse.toString())
                }
            } catch (e: Exception) {
                _agentState.value = AgentState.Idle
                return@withContext AgentResult.Failure("LLM Error: ${e.message}")
            }

            val parsed = ResponseParser.parse(llmResponse.toString())

            contextHistory += "Agent:\n$llmResponse\n\n"
            brainLogger.log(LogType.AGENT_THOUGHT, "STEP $stepCount", llmResponse.toString())

            if (parsed.isDone || parsed.toolCall == null) {
                _agentState.value = AgentState.Reflecting
                _agentState.value = AgentState.Idle
                return@withContext AgentResult.Success(parsed.finalMessage, stepCount)
            }

            val toolCall = parsed.toolCall
            _agentState.value = AgentState.Acting(toolCall.name, toolCall.params, stepCount)
            brainLogger.log(LogType.TOOL_CALL, toolCall.name, toolCall.params.toString())
            
            val toolOutput = StringBuilder()
            try {
                toolDispatcher.dispatch(toolCall.name, toolCall.params).collect { output ->
                    toolOutput.append(output).append("\n")
                }
            } catch (e: Exception) {
                val err = "[Tool Execution Error: ${e.message}]\n"
                toolOutput.append(err)
                brainLogger.log(LogType.ERROR, "TOOL_ERROR", err)
            }

            contextHistory += "Tool Output:\n$toolOutput\n\n"
            brainLogger.log(LogType.TOOL_RESULT, toolCall.name, toolOutput.toString())
        }

        _agentState.value = AgentState.Reflecting
        _agentState.value = AgentState.Idle
        AgentResult.MaxDepthReached(maxSteps)
    }
}
