package com.lia.ai

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Stub service: screen automation ke liye base. Yahan se tap, back, home aur
 * text-based click available hain; baaki logic apne hisaab se badhao.
 */
class LiaAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: LiaAccessibilityService? = null
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // TODO: screen events yahan handle karo
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun goBack() = performGlobalAction(GLOBAL_ACTION_BACK)
    fun goHome() = performGlobalAction(GLOBAL_ACTION_HOME)

    fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    fun clickByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        for (node in nodes) {
            var n: AccessibilityNodeInfo? = node
            while (n != null) {
                if (n.isClickable) return n.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                n = n.parent
            }
        }
        return false
    }
}
