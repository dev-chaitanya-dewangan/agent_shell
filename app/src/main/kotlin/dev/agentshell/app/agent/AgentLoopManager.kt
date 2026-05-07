package dev.agentshell.app.agent

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
    private val toolDispatcher: ToolDispatcher
) {
    private val loopMutex = Mutex()
    
    private val _agentState = MutableStateFlow<AgentState>(AgentState.Idle)
    val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

    suspend fun run(task: String, maxSteps: Int = 4): AgentResult {
        return loopMutex.withLock {
            runLoop(task, maxSteps)
        }
    }

    private suspend fun runLoop(task: String, maxSteps: Int): AgentResult = withContext(Dispatchers.Default) {
        var stepCount = 0
        var contextHistory = "Task: $task\n\n"

        val systemPrompt = """
            You are agentShell, a local on-device AI agent.
            You can use tools by returning XML format:
            <tool_call>
              <name>run_shell</name>
              <command>ls -la</command>
            </tool_call>
            OR
            <tool_call>
              <name>write_file</name>
              <path>test.txt</path>
              <content>hello</content>
            </tool_call>
            
            When finished, reply without XML blocks to return to user.
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

            if (parsed.isDone || parsed.toolCall == null) {
                _agentState.value = AgentState.Reflecting
                _agentState.value = AgentState.Idle
                return@withContext AgentResult.Success(parsed.finalMessage, stepCount)
            }

            val toolCall = parsed.toolCall
            _agentState.value = AgentState.Acting(toolCall.name, stepCount)
            
            val toolOutput = StringBuilder()
            try {
                toolDispatcher.dispatch(toolCall.name, toolCall.params).collect { output ->
                    toolOutput.append(output).append("\n")
                }
            } catch (e: Exception) {
                toolOutput.append("[Tool Execution Error: ${e.message}]\n")
            }

            contextHistory += "Tool Output:\n$toolOutput\n\n"
        }

        _agentState.value = AgentState.Reflecting
        _agentState.value = AgentState.Idle
        AgentResult.MaxDepthReached(maxSteps)
    }
}
