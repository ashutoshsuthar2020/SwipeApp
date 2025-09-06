package com.example.swipeapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ToggleSwipeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("ToggleSwipeReceiver", "onReceive CALLED! intent=$intent, context=$context, thread=${Thread.currentThread().name}")
        if (intent?.action == "com.example.swipeapp.TOGGLE_SWIPE") {
            Log.d("ToggleSwipeReceiver", "TOGGLE_SWIPE action matched. Calling MyAccessibilityService.toggleSwipe()")
            MyAccessibilityService.toggleSwipe()
        } else {
            Log.d("ToggleSwipeReceiver", "Unknown action: ${intent?.action}")
        }
    }
}
