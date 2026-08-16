package com.marshall.motif

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.marshall.motif.ble.BleManager
import com.marshall.motif.ui.MarshallApp

class MainActivity : ComponentActivity() {

    private lateinit var ble: BleManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var settings: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ble = BleManager.get(this)
        ble.attachActivity(this)
        settings = SettingsStore(this)
        setContent {
            MarshallApp(ble, settings)
        }
        ble.restoreSavedDevice()
        handleWidgetIntent(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: android.content.Intent?) {
        if (intent?.action != MarshallWidgetProvider.ACTION_TOGGLE_ANC) return
        val attempt = object : Runnable {
            var remaining = 20
            override fun run() {
                if (ble.state.connected) {
                    ble.setAncMode(
                        com.marshall.motif.ble.Protocol.nextAncMode(ble.state.ancMode),
                    )
                } else if (remaining-- > 0) {
                    mainHandler.postDelayed(this, 500L)
                }
            }
        }
        mainHandler.post(attempt)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        ble.onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onDestroy() {
        ble.attachActivity(null)
        super.onDestroy()
    }
}
