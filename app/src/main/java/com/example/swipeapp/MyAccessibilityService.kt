package com.example.swipeapp


import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import kotlin.random.Random

class MyAccessibilityService : AccessibilityService() {
    private var lastToggleFlag = false
    private val pollHandler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            val prefs = getSharedPreferences("swipe_prefs", Context.MODE_PRIVATE)
            val flag = prefs.getBoolean("toggle", false)
            if (flag != lastToggleFlag) {
                lastToggleFlag = flag
                if (isSwiping) stopSwiping() else startSwiping()
                android.util.Log.d("MyAccessibilityService", "Polled toggle flag changed, toggled swiping. isSwiping=$isSwiping")
            }
            pollHandler.postDelayed(this, 1000)
        }
    }
    companion object {
        @Volatile
        private var instance: MyAccessibilityService? = null

        fun toggleSwipe() {
            instance?.let {
                    Handler(Looper.getMainLooper()).post {
                    if (it.isSwiping) it.stopSwiping() else it.startSwiping()
                    android.util.Log.d("MyAccessibilityService", "toggleSwipe() called from static receiver. isSwiping=${it.isSwiping}")
                }
            } ?: android.util.Log.d("MyAccessibilityService", "toggleSwipe() called but instance is null!")
        }
    }
    override fun onCreate() {
        super.onCreate()
        instance = this
        lastToggleFlag = getSharedPreferences("swipe_prefs", Context.MODE_PRIVATE).getBoolean("toggle", false)
        pollHandler.post(pollRunnable)
    }

    // ...existing code...
    private var isSwiping = false
    private val handler = Handler(Looper.getMainLooper())
    private val swipeRunnable = object : Runnable {
        override fun run() {
            if (isSwiping) {
                performSwipe()
                // Get max interval from SharedPreferences (default 5000ms)
                val prefs = getSharedPreferences("swipe_prefs", Context.MODE_PRIVATE)
                val maxInterval = prefs.getLong("max_interval", 5000L)
                val delay = Random.nextLong(1000, maxInterval.coerceAtLeast(1000L))
                handler.postDelayed(this, delay)
            }
        }
    }


    override fun onServiceConnected() {
        super.onServiceConnected()
        android.util.Log.d("MyAccessibilityService", "onServiceConnected: Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used for this automation
    }

    override fun onInterrupt() {
        stopSwiping()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSwiping()
        pollHandler.removeCallbacks(pollRunnable)
    }

    private fun startSwiping() {
        android.util.Log.d("MyAccessibilityService", "startSwiping called. isSwiping=$isSwiping")
        if (!isSwiping) {
            isSwiping = true
            android.util.Log.d("MyAccessibilityService", "startSwiping: Swiping started")
            handler.post(swipeRunnable)
        } else {
            android.util.Log.d("MyAccessibilityService", "startSwiping: Already swiping, ignoring.")
        }
    }

    private fun stopSwiping() {
        android.util.Log.d("MyAccessibilityService", "stopSwiping called. isSwiping=$isSwiping")
        isSwiping = false
        android.util.Log.d("MyAccessibilityService", "stopSwiping: Swiping stopped")
        handler.removeCallbacks(swipeRunnable)
    }

    private fun performSwipe() {
        android.util.Log.d("MyAccessibilityService", "performSwipe called. isSwiping=$isSwiping")
        // Swipe from center to right
        val display = resources.displayMetrics
        val centerX = display.widthPixels / 2f
        val centerY = display.heightPixels / 2f
        val endX = display.widthPixels * 0.9f
        val path = Path().apply {
            moveTo(centerX, centerY)
            lineTo(endX, centerY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                android.util.Log.d("MyAccessibilityService", "performSwipe: Gesture completed")
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                android.util.Log.d("MyAccessibilityService", "performSwipe: Gesture cancelled")
            }
        }, null)
        android.util.Log.d("MyAccessibilityService", "performSwipe: dispatchGesture returned $dispatched")
    }
}
