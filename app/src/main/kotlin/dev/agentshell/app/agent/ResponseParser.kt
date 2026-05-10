package dev.agentshell.app.agent

import org.json.JSONObject

data class ToolCall(val name: String, val params: Map<String, String>)

data class ParsedResponse(
    val finalMessage: String = "",
    val toolCall: ToolCall? = null,
    val isDone: Boolean = false
)

object ResponseParser {
    fun parse(response: String): ParsedResponse {
        // Try JSON parsing first (look for {"tool": "name", ...})
        val jsonMatch = Regex("\\{.*\"tool\"\\s*:\\s*\"([^\"]+)\".*\\}", RegexOption.DOT_MATCHES_ALL).find(response)
        if (jsonMatch != null) {
            try {
                val jsonString = jsonMatch.value
                val jsonObj = JSONObject(jsonString)
                val name = jsonObj.getString("tool")
                val paramsObj = jsonObj.optJSONObject("params")
                val params = mutableMapOf<String, String>()
                if (paramsObj != null) {
                    paramsObj.keys().forEach { key ->
                        params[key] = paramsObj.getString(key)
                    }
                }
                return ParsedResponse(toolCall = ToolCall(name, params))
            } catch (e: Exception) {
                // fallback to XML or text
            }
        }

        // Fallback to XML
        if (response.contains("<tool_call>")) {
            val nameMatch = "<name>(.*?)</name>".toRegex().find(response)
            val name = nameMatch?.groupValues?.get(1) ?: return ParsedResponse(isDone = true)
            
            val params = mutableMapOf<String, String>()
            if (name == "run_shell" || name == "run_termux") {
                val cmdMatch = "<command>(.*?)</command>".toRegex(RegexOption.DOT_MATCHES_ALL).find(response)
                params["command"] = cmdMatch?.groupValues?.get(1)?.trim() ?: ""
            } else if (name == "write_file") {
                val pathMatch = "<path>(.*?)</path>".toRegex().find(response)
                val contentMatch = "<content>(.*?)</content>".toRegex(RegexOption.DOT_MATCHES_ALL).find(response)
                params["path"] = pathMatch?.groupValues?.get(1)?.trim() ?: ""
                params["content"] = contentMatch?.groupValues?.get(1)?.trim() ?: ""
            } else {
                // generic fallback
                val paramMatches = "<([a-zA-Z0-9_]+)>(.*?)</\\1>".toRegex(RegexOption.DOT_MATCHES_ALL).findAll(response)
                for (match in paramMatches) {
                    if (match.groupValues[1] != "name") {
                        params[match.groupValues[1]] = match.groupValues[2].trim()
                    }
                }
            }
            
            return ParsedResponse(toolCall = ToolCall(name, params))
        }
        
        return ParsedResponse(finalMessage = response, isDone = true)
    }
}
