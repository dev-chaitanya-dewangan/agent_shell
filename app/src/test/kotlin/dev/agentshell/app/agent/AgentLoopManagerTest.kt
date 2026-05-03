package dev.agentshell.app.agent

import dev.agentshell.app.llm.EngineStatus
import dev.agentshell.app.llm.LLMEngine
import dev.agentshell.app.llm.PingResult
import dev.agentshell.app.llm.ProviderType
import dev.agentshell.app.terminal.TerminalSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class FakeLLMEngine(private val responses: List<String>) : LLMEngine {
    override val providerType = ProviderType.OPENROUTER
    override val isReady = true
    override val statusFlow = MutableStateFlow(EngineStatus.Ready)
    
    private var callCount = 0

    override fun generate(prompt: String, systemPrompt: String, maxTokens: Int, temperature: Float): Flow<String> {
        val response = if (callCount < responses.size) responses[callCount++] else "I am done."
        return flowOf(response)
    }

    override suspend fun complete(prompt: String, maxTokens: Int): String = ""
    override fun countTokens(text: String): Int = text.length
    override suspend fun ping(): PingResult = PingResult.Success(10L)
}

class AgentLoopManagerTest {

    @Test
    fun `loop stops at max steps when not completing`() = runTest {
        // Given an LLM that always returns a tool call
        val fakeLLM = FakeLLMEngine(listOf(
            "<tool_call><name>run_shell</name><command>ls</command></tool_call>",
            "<tool_call><name>run_shell</name><command>ls</command></tool_call>",
            "<tool_call><name>run_shell</name><command>ls</command></tool_call>",
            "<tool_call><name>run_shell</name><command>ls</command></tool_call>"
        ))
        
        val mockSession = mock(TerminalSession::class.java)
        val dispatcher = ToolDispatcher(mockSession)
        val agent = AgentLoopManager(fakeLLM, dispatcher)
        
        // Run with maxSteps = 3
        val result = agent.run("Do something infinite", maxSteps = 3)
        
        // Assert
        assertTrue(result is AgentResult.MaxDepthReached)
        assertEquals(3, (result as AgentResult.MaxDepthReached).stepsExecuted)
    }
    
    @Test
    fun `loop completes successfully when LLM returns no tool call`() = runTest {
        // Given an LLM that runs a tool, then returns final message
        val fakeLLM = FakeLLMEngine(listOf(
            "<tool_call><name>run_shell</name><command>ls</command></tool_call>",
            "I have finished the task."
        ))
        
        val mockSession = mock(TerminalSession::class.java)
        val dispatcher = ToolDispatcher(mockSession)
        val agent = AgentLoopManager(fakeLLM, dispatcher)
        
        val result = agent.run("Do something short", maxSteps = 4)
        
        assertTrue(result is AgentResult.Success)
        assertEquals(2, (result as AgentResult.Success).stepsExecuted)
        assertEquals("I have finished the task.", result.message)
    }
}
