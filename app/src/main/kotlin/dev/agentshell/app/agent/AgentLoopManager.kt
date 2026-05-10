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
    data class Acting(val tool: String, val step: Int) : AgentState()
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
            
            You have access to the following tools:
            run_shell, run_termux, write_file, read_file, list_dir, ui_tap, ui_type, ui_find_and_tap, ui_get_screen, open_app, create_mini_app, take_screenshot.
            
            You can use tools by returning JSON format:
            {"tool": "run_shell", "params": {"command": "ls -la"}}
            OR XML format:
            <tool_call>
              <name>run_shell</name>
              <command>ls -la</command>
            </tool_call>
            
            When finished, reply without JSON/XML blocks to return to user.
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
            _agentState.value = AgentState.Acting(toolCall.name, stepCount)
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
