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
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
    }

    fun tapAt(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 50L))
            .build()
        dispatchGesture(gesture, null, null)
    }

    fun typeText(text: String) {
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        val node = findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        node?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun findAndTap(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        return nodes?.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

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
            val child = node.getChild(i)
            if (child != null) {
                children.put(buildJsonTree(child))
            }
        }
        if (children.length() > 0) {
            obj.put("children", children)
        }
        return obj
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    
    override fun onInterrupt() {}
    
    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
