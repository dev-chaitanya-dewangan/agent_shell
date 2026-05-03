package dev.agentshell.app.terminal

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

/**
 * Programmatic test for TerminalViewModel logic.
 * Mocks the Application context to avoid relying on the Android framework.
 */
class TerminalViewModelTest {

    private lateinit var viewModel: TerminalViewModel
    private lateinit var mockApp: Application

    @Before
    fun setup() {
        mockApp = mock(Application::class.java)
        val tempDir = System.getProperty("java.io.tmpdir")
        `when`(mockApp.filesDir).thenReturn(File(tempDir))
        
        viewModel = TerminalViewModel(mockApp)
    }

    @Test
    fun `onInputChanged updates the current input state`() {
        viewModel.onInputChanged("echo hello")
        assertEquals("echo hello", viewModel.state.value.currentInput)
    }

    @Test
    fun `clearTerminal empties the output log`() {
        viewModel.clearTerminal()
        assertEquals(0, viewModel.state.value.outputLog.size)
    }
}
