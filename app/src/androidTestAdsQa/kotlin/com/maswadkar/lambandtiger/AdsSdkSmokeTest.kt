package com.maswadkar.lambandtiger

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/** Real Google test ads; completed-match key is a harness fixture, not a game/save. */
class AdsSdkSmokeTest {
    @Test fun realTestAdDismissalContinuesOnce() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val calls = AtomicInteger()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var activity: MainActivity? = null
            scenario.onActivity {
                activity = it
                it.ads.updateAudience(false); it.audience.changePlayer()
                assertFalse(it.ads.eligible); assertFalse(it.ads.bannerAllowed); assertFalse(it.ads.interstitialReady)
                it.audience.choose(com.maswadkar.lambandtiger.privacy.AgeGroup.UNDER_13)
            }
            fun waitFor(timeout: Long, check: () -> Boolean) {
                val end = android.os.SystemClock.elapsedRealtime() + timeout
                while (android.os.SystemClock.elapsedRealtime() < end) {
                    var ok = false
                    instrumentation.runOnMainSync { ok = check() }
                    if (ok) return
                    android.os.SystemClock.sleep(200)
                }
                fail("Timed out waiting for test ad state; inspect SDK/network evidence")
            }
            waitFor(90_000) { activity!!.ads.interstitialReady }
            instrumentation.runOnMainSync {
                val ads = activity!!.ads
                // Swap to a static completed-session harness so normal Home recomposition
                // cannot overwrite its key. This never mutates the saved game.
                activity!!.setContentView(android.widget.TextView(activity).apply { text = "Completed game ad SDK test" })
                ads.updateLocation(false, false, "sdk-completed-fixture")
                ads.betweenGames("sdk-completed-fixture") { calls.incrementAndGet() }
            }
            waitFor(10_000) { activity!!.ads.showing }
            // Wait for the SDK creative and its dismissal control, not just show().
            android.os.SystemClock.sleep(10_000)
            val bitmap = instrumentation.uiAutomation.takeScreenshot()
            val file = java.io.File(activity!!.getExternalFilesDir(null), "interstitial-test.png")
            file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            // Use the SDK's accessible Close control, never ad content.
            fun closeNode(node: android.view.accessibility.AccessibilityNodeInfo?): android.view.accessibility.AccessibilityNodeInfo? {
                if (node == null) return null
                if (node.isVisibleToUser && node.isEnabled && node.isClickable &&
                    (node.contentDescription?.contains("close", ignoreCase = true) == true ||
                     node.text?.toString()?.equals("Close", ignoreCase = true) == true)) return node
                for (i in 0 until node.childCount) closeNode(node.getChild(i))?.let { return it }
                return null
            }
            var close: android.view.accessibility.AccessibilityNodeInfo? = null
            val closeDeadline = android.os.SystemClock.elapsedRealtime() + 60_000
            while (close == null && android.os.SystemClock.elapsedRealtime() < closeDeadline) {
                close = closeNode(instrumentation.uiAutomation.rootInActiveWindow)
                if (close == null) android.os.SystemClock.sleep(250)
            }
            assertNotNull("The test creative must expose an accessible Close control", close)
            val bounds = android.graphics.Rect()
            close!!.getBoundsInScreen(bounds)
            android.util.Log.i("AdsSmoke", "Closing ${close!!.contentDescription} at $bounds")
            // Inject the same touch a player uses, using coordinates from the live UI tree.
            val time = android.os.SystemClock.uptimeMillis()
            for (action in listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP)) {
                val event = android.view.MotionEvent.obtain(time, android.os.SystemClock.uptimeMillis(), action,
                    bounds.exactCenterX(), bounds.exactCenterY(), 0)
                event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
                assertTrue(instrumentation.uiAutomation.injectInputEvent(event, true))
                event.recycle()
            }
            waitFor(15_000) { calls.get() == 1 }
            assertEquals(1, calls.get())
            assertFalse(activity!!.ads.showing)
        }
    }
}
