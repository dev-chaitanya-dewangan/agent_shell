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
        // Scan the response for the first valid JSON object that has a "tool" key.
        // We do NOT use DOT_MATCHES_ALL on the full string because a greedy .* between { and }
        // will swallow multiple tool objects and surrounding prose.
        val toolCall = findJsonToolCall(response)
        if (toolCall != null) return ParsedResponse(toolCall = toolCall)

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

    /**
     * Walks [text] char-by-char to find the first balanced JSON object that has
     * a top-level "tool" key. Returns null if none found.
     *
     * This avoids the greedy-regex problem where `\{.*\}` with DOT_MATCHES_ALL
     * collapses multiple tool calls and surrounding prose into a single match.
     */
    private fun findJsonToolCall(text: String): ToolCall? {
        var i = 0
        while (i < text.length) {
            val start = text.indexOf('{', i)
            if (start == -1) break

            // Walk to find the matching closing brace (handling nesting)
            var depth = 0
            var j = start
            while (j < text.length) {
                when (text[j]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) break
                    }
                    '"' -> {
                        // Skip over string literals so braces inside strings don't count
                        j++
                        while (j < text.length && text[j] != '"') {
                            if (text[j] == '\\') j++ // skip escape
                            j++
                        }
                    }
                }
                j++
            }

            val candidate = text.substring(start, minOf(j + 1, text.length))
            try {
                val jsonObj = JSONObject(candidate)
                val name = jsonObj.optString("tool").takeIf { it.isNotBlank() }
                if (name != null) {
                    val paramsObj = jsonObj.optJSONObject("params")
                    val params = mutableMapOf<String, String>()
                    if (paramsObj != null) {
                        paramsObj.keys().forEach { key ->
                            // Use opt to avoid throw on nested objects; convert to string
                            params[key] = paramsObj.opt(key)?.toString() ?: ""
                        }
                    }
                    return ToolCall(name, params)
                }
            } catch (_: Exception) {
                // Not valid JSON, keep scanning
            }
            i = start + 1
        }
        return null
    }
}

