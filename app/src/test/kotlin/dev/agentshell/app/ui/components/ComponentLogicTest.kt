package dev.agentshell.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 * This establishes our programmatic testing framework.
 * Future tests will mock components and verify logic (e.g., AgentLoopManager).
 */
class ComponentLogicTest {

    @Test
    fun `button styles map to correct identifiers`() {
        assertEquals("PRIMARY", ButtonStyle.PRIMARY.name)
        assertEquals("SECONDARY", ButtonStyle.SECONDARY.name)
        assertEquals("DANGER", ButtonStyle.DANGER.name)
    }
    
    @Test
    fun `nav routes match PRD`() {
        assertEquals("SHELL", dev.agentshell.app.ui.nav.NavRoute.SHELL.title)
        assertEquals("CHAT", dev.agentshell.app.ui.nav.NavRoute.CHAT.title)
        assertEquals("APPS", dev.agentshell.app.ui.nav.NavRoute.APPS.title)
    }
}
