package dev.agentshell.app.agent

data class ToolCall(val name: String, val params: Map<String, String>)

data class ParsedResponse(
    val finalMessage: String = "",
    val toolCall: ToolCall? = null,
    val isDone: Boolean = false
)

object ResponseParser {
    fun parse(response: String): ParsedResponse {
        if (response.contains("<tool_call>")) {
            val nameMatch = "<name>(.*?)</name>".toRegex().find(response)
            val name = nameMatch?.groupValues?.get(1) ?: return ParsedResponse(isDone = true)
            
            val params = mutableMapOf<String, String>()
            if (name == "run_shell") {
                val cmdMatch = "<command>(.*?)</command>".toRegex(RegexOption.DOT_MATCHES_ALL).find(response)
                params["command"] = cmdMatch?.groupValues?.get(1)?.trim() ?: ""
            } else if (name == "write_file") {
                val pathMatch = "<path>(.*?)</path>".toRegex().find(response)
                val contentMatch = "<content>(.*?)</content>".toRegex(RegexOption.DOT_MATCHES_ALL).find(response)
                params["path"] = pathMatch?.groupValues?.get(1)?.trim() ?: ""
                params["content"] = contentMatch?.groupValues?.get(1)?.trim() ?: ""
            }
            
            return ParsedResponse(toolCall = ToolCall(name, params))
        }
        
        return ParsedResponse(finalMessage = response, isDone = true)
    }
}
