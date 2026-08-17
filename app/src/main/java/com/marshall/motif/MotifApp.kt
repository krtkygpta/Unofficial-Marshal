package com.marshall.motif

import android.app.Application
import android.content.ComponentCallbacks
import android.content.res.Configuration

class MotifApp : Application() {
    override fun onCreate() {
        super.onCreate()
        registerComponentCallbacks(object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                WidgetStateStore.update(this@MotifApp)
            }

            override fun onLowMemory() = Unit
        })
    }
}
