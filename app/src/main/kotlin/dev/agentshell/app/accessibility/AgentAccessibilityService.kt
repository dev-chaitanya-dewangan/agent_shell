package dev.agentshell.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONArray
import org.json.JSONObject

@AndroidEntryPoint
class AgentAccessibilityService : AccessibilityService() {

    companion object {
        var instance: AgentAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        instance = this
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes   = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags        = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                           AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                           AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
    }

    // ── Gesture: tap at absolute screen coords ───────────────────────────────
    fun tapAt(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 50L))
            .build()
        dispatchGesture(gesture, null, null)
    }

    // ── Text input: set text on the focused input node ───────────────────────
    fun typeText(text: String) {
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val node = findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        node?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    // ── Find visible element by text and click it ────────────────────────────
    fun findAndTap(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return nodes?.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    // ── Find element by content description (for icon-only buttons) ──────────
    fun findByContentDesc(desc: String): Boolean {
        val root = rootInActiveWindow ?: return false
        return findNodeByContentDesc(root, desc)?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    private fun findNodeByContentDesc(node: AccessibilityNodeInfo, desc: String): AccessibilityNodeInfo? {
        if (node.contentDescription?.toString()?.contains(desc, ignoreCase = true) == true) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByContentDesc(child, desc)
            if (result != null) return result
        }
        return null
    }

    // ── Scroll a scrollable node in the tree ─────────────────────────────────
    fun scrollNode(direction: String = "down"): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (direction == "up")
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        else
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        return findFirstScrollable(root)?.performAction(action) ?: false
    }

    private fun findFirstScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val result = findFirstScrollable(node.getChild(i) ?: continue)
            if (result != null) return result
        }
        return null
    }

    // ── Return full UI tree as compact JSON (for LLM reasoning) ─────────────
    fun getScreenTree(): String {
        val root = rootInActiveWindow ?: return "{}"
        return buildJsonTree(root).toString()
    }

    private fun buildJsonTree(node: AccessibilityNodeInfo): JSONObject {
        val obj = JSONObject()
        obj.put("class", node.className)
        obj.put("text", node.text)
        obj.put("contentDescription", node.contentDescription)
        obj.put("isClickable", node.isClickable)
        obj.put("isFocused", node.isFocused)
        val children = JSONArray()
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            children.put(buildJsonTree(child))
        }
        if (children.length() > 0) obj.put("children", children)
        return obj
    }

    // ── Return all visible text as a flat string (for screen reading) ─────────
    // Used by the read_screen_text tool — extracts all text/contentDesc from tree.
    fun getScreenText(): String {
        val root = rootInActiveWindow ?: return ""
        val sb = StringBuilder()
        flattenText(root, sb)
        return sb.toString().trim()
    }

    private fun flattenText(node: AccessibilityNodeInfo, sb: StringBuilder) {
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        if (!text.isNullOrEmpty()) sb.appendLine(text)
        else if (!desc.isNullOrEmpty()) sb.appendLine(desc)
        for (i in 0 until node.childCount) {
            flattenText(node.getChild(i) ?: continue, sb)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
