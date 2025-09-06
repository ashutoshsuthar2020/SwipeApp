package com.example.swipeapp

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.swipeapp.ui.MainScreen
import com.example.swipeapp.ui.theme.SwipeAppTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize

private const val ACTION_TOGGLE_SWIPE_UI_UPDATE = "com.example.swipeapp.TOGGLE_SWIPE_UI_UPDATE"
private const val EXTRA_IS_SWIPING_ACTIVE = "EXTRA_IS_SWIPING_ACTIVE"

class MainActivity : ComponentActivity() {

    private val tag = "MainActivity"

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            checkAndStartFloatingWidgetService()
        } else {
            Toast.makeText(
                this,
                getString(R.string.overlay_permission_denied_toast),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private var isServiceActive by mutableStateOf(false)

    private val toggleReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_TOGGLE_SWIPE_UI_UPDATE) {
                val isActive = intent.getBooleanExtra(EXTRA_IS_SWIPING_ACTIVE, false)
                isServiceActive = isActive
                Log.d(tag, "Received toggle broadcast: Service active = $isActive")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var maxInterval by mutableStateOf(getSharedPreferences("swipe_prefs", Context.MODE_PRIVATE).getLong("max_interval", 5000L).toString())
        setContent {
            SwipeAppTheme {
                Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onEnableFeatureClicked = {
                            Log.d(tag, "Enable Feature clicked")
                            requestPermissionsAndStartServices()
                        },
                        isServiceActive = isServiceActive,
                        maxInterval = maxInterval,
                        onMaxIntervalChange = { newValue ->
                            maxInterval = newValue
                            newValue.toLongOrNull()?.let {
                                getSharedPreferences("swipe_prefs", Context.MODE_PRIVATE)
                                    .edit().putLong("max_interval", it).apply()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val filter = IntentFilter(ACTION_TOGGLE_SWIPE_UI_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.registerReceiver(this, toggleReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
           registerReceiver(toggleReceiver, filter)
        }
    }



    override fun onPause() {
        super.onPause()
        unregisterReceiver(toggleReceiver)
    }

    private fun requestPermissionsAndStartServices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
            overlayPermissionLauncher.launch(intent)
        } else {
            checkAndStartFloatingWidgetService()
        }
    }

    private fun checkAndStartFloatingWidgetService() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(
                this,
                getString(R.string.accessibility_permission_required_toast),
                Toast.LENGTH_LONG
            ).show()
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
                })
            } catch (e: Exception) {
                Log.e(tag, "Cannot open Accessibility Settings", e)
            }
        } else {
            try {
                val serviceIntent = Intent(this, FloatingWidgetService::class.java)
                ContextCompat.startForegroundService(this, serviceIntent)
                isServiceActive = true
            } catch (e: Exception) {
                Log.e(tag, "Failed to start FloatingWidgetService", e)
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val expected = "$packageName/${MyAccessibilityService::class.java.name}"
        val result = enabledServices?.split(':')?.any { it.equals(expected, ignoreCase = true) } == true
        Log.d(tag, "isAccessibilityServiceEnabled: enabledServices=$enabledServices, expected=$expected, result=$result")
        return result
    }
}
