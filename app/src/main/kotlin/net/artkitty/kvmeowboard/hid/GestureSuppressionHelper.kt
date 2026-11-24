package net.artkitty.kvmeowboard.hid

import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.util.Log

/**
 * 🛡️ GestureSuppressionHelper 🛡️
 * Prevents Android / OxygenOS system gestures (such as 3-finger screenshot or edge back)
 * from hijacking touchpad surface interactions.
 */
object GestureSuppressionHelper {

    fun applyTouchpadExclusionRect(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.window?.decorView?.post {
                try {
                    val decor = activity.window?.decorView ?: return@post
                    val rect = Rect(0, 0, decor.width, decor.height)
                    decor.systemGestureExclusionRects = listOf(rect)
                } catch (t: Throwable) {
                    Log.w("GestureSuppression", "Could not apply gesture exclusion: ${t.message}")
                }
            }
        }
    }

    fun suppressThreeFingerScreenshotIfRooted() {
        Thread {
            try {
                // OxygenOS/ColorOS key: oplus_customize_gesture_screen_shot or three_finger_screenshot
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "settings put system oplus_customize_gesture_screen_shot 0; settings put system three_finger_screenshot 0"))
                p.waitFor()
                Log.d("GestureSuppression", "Suppressed 3-finger screenshot via root")
            } catch (_: Throwable) {}
        }.start()
    }

    fun restoreSystemGestures() {
        Thread {
            try {
                val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "settings put system oplus_customize_gesture_screen_shot 1; settings put system three_finger_screenshot 1"))
                p.waitFor()
                Log.d("GestureSuppression", "Restored system gestures via root")
            } catch (_: Throwable) {}
        }.start()
    }
}
