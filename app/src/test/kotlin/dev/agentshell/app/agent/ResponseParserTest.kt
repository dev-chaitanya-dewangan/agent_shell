package dev.agentshell.app.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseParserTest {

    @Test
    fun `parse run_shell tool call`() {
        val xml = """
            Here is what I will do.
            <tool_call>
              <name>run_shell</name>
              <command>echo "hello world"</command>
            </tool_call>
        """.trimIndent()

        val parsed = ResponseParser.parse(xml)

        assertNotNull(parsed.toolCall)
        assertEquals("run_shell", parsed.toolCall?.name)
        assertEquals("echo \"hello world\"", parsed.toolCall?.params?.get("command"))
    }

    @Test
    fun `parse write_file tool call`() {
        val xml = """
            <tool_call>
              <name>write_file</name>
              <path>/test.txt</path>
              <content>
                  line 1
                  line 2
              </content>
            </tool_call>
        """.trimIndent()

        val parsed = ResponseParser.parse(xml)

        assertNotNull(parsed.toolCall)
        assertEquals("write_file", parsed.toolCall?.name)
        assertEquals("/test.txt", parsed.toolCall?.params?.get("path"))
        assertEquals("\n                  line 1\n                  line 2\n              ", parsed.toolCall?.params?.get("content"))
    }

    @Test
    fun `parse final message without tool call`() {
        val xml = "I have successfully executed the task."

        val parsed = ResponseParser.parse(xml)

        assertNull(parsed.toolCall)
        assertTrue(parsed.isDone)
        assertEquals(xml, parsed.finalMessage)
    }
}
