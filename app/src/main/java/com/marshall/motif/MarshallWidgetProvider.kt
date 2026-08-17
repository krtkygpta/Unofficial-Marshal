package com.marshall.motif

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.marshall.motif.ble.BleManager
import com.marshall.motif.ble.Protocol
import com.marshall.motif.ui.components.ancModeDrawable

object WidgetStateStore {
    const val PREFS = "marshall_widget"

    fun save(
        context: Context,
        left: Int?,
        right: Int?,
        case: Int?,
        ancMode: Int,
        name: String,
        connected: Boolean = false,
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putIntOrNull("left", left)
            .putIntOrNull("right", right)
            .putIntOrNull("case", case)
            .putInt("anc", ancMode)
            .putString("name", name)
            .putBoolean("connected", connected)
            .apply()
        update(context)
    }

    private fun android.content.SharedPreferences.Editor.putIntOrNull(key: String, value: Int?) = apply {
        if (value == null) remove(key) else putInt(key, value)
    }

    fun update(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, MarshallWidgetProvider::class.java)
        manager.getAppWidgetIds(component).forEach { id ->
            manager.updateAppWidget(id, MarshallWidgetProvider.views(context, id))
            manager.notifyAppWidgetViewDataChanged(id, R.id.widget_list)
        }
    }
}

class MarshallWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            manager.updateAppWidget(id, views(context, id))
            manager.notifyAppWidgetViewDataChanged(id, R.id.widget_list)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        appWidgetManager.updateAppWidget(appWidgetId, views(context, appWidgetId))
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_FLIP_PAGE -> {
                val prefs = context.getSharedPreferences(WidgetStateStore.PREFS, Context.MODE_PRIVATE)
                prefs.edit().putInt("page", if (prefs.getInt("page", 0) == 0) 1 else 0).apply()
                WidgetStateStore.update(context)
            }
            ACTION_TOGGLE_ANC -> toggleAnc(context)
            ACTION_OPEN_APP -> openApp(context)
            ACTION_WIDGET -> when (intent.getStringExtra(EXTRA_WIDGET_ACTION)) {
                ACTION_TOGGLE_ANC -> toggleAnc(context)
                ACTION_OPEN_APP -> openApp(context)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_ANC = "com.marshall.motif.action.TOGGLE_ANC"
        const val ACTION_FLIP_PAGE = "com.marshall.motif.action.FLIP_PAGE"
        const val ACTION_OPEN_APP = "com.marshall.motif.action.OPEN_APP"
        const val ACTION_WIDGET = "com.marshall.motif.action.WIDGET"
        const val EXTRA_WIDGET_ACTION = "widget_action"

        fun views(context: Context, appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID): RemoteViews {
            return try {
                if (!WidgetArt.ready) WidgetArt.warmAsync(context)
                val views = RemoteViews(context.packageName, R.layout.widget_scroll)
                val service = Intent(context, MarshallWidgetService::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = android.net.Uri.parse("marshall://widget/$appWidgetId")
                }
                views.setRemoteAdapter(R.id.widget_list, service)
                views.setPendingIntentTemplate(
                    R.id.widget_list,
                    pending(context, ACTION_WIDGET, appWidgetId, 2, mutable = true),
                )
                views
            } catch (_: Exception) {
                bind(context, appWidgetId)
            }
        }

        private fun bind(context: Context, appWidgetId: Int): RemoteViews {
            val prefs = context.getSharedPreferences(WidgetStateStore.PREFS, Context.MODE_PRIVATE)
            val connected = prefs.getBoolean("connected", false)
            val anc = prefs.getInt("anc", Protocol.ANC_OFF)
            val page = prefs.getInt("page", 0)
            val views = RemoteViews(context.packageName, R.layout.widget)

            views.setViewVisibility(R.id.widget_page_battery, if (page == 0) View.VISIBLE else View.GONE)
            views.setViewVisibility(R.id.widget_page_anc, if (page == 0) View.GONE else View.VISIBLE)

            WidgetArt.left(context)?.let { views.setImageViewBitmap(R.id.widget_bud_left, it) }
            WidgetArt.right(context)?.let { views.setImageViewBitmap(R.id.widget_bud_right, it) }

            if (connected) {
                views.setViewVisibility(R.id.widget_led, View.VISIBLE)
                views.setViewVisibility(R.id.widget_disconnected, View.GONE)
                views.setImageViewBitmap(
                    R.id.widget_led,
                    WidgetLed.drawPercents(
                        prefs.getInt("left", -1),
                        prefs.getInt("right", -1),
                        prefs.getInt("case", -1),
                        context.resources.displayMetrics.density,
                        ContextCompat.getColor(context, R.color.widget_on_surface),
                    ),
                )
                views.setTextViewText(R.id.widget_anc_label, ancLabel(anc))
                views.setImageViewResource(R.id.widget_anc_icon, ancModeDrawable(anc))
            } else {
                views.setViewVisibility(R.id.widget_led, View.GONE)
                views.setViewVisibility(R.id.widget_disconnected, View.VISIBLE)
                views.setTextViewText(R.id.widget_anc_label, "Not connected")
                views.setImageViewResource(R.id.widget_anc_icon, R.drawable.ic_noise_control_off)
            }
            views.setInt(
                R.id.widget_anc_icon,
                "setColorFilter",
                ContextCompat.getColor(context, R.color.widget_on_surface),
            )

            val open = pendingActivity(context, appWidgetId)
            val flip = pending(context, ACTION_FLIP_PAGE, appWidgetId, 1)
            val toggle = pending(context, ACTION_TOGGLE_ANC, appWidgetId, 2)
            views.setOnClickPendingIntent(R.id.widget_page_battery, open)
            views.setOnClickPendingIntent(R.id.widget_anc_label, flip)
            views.setOnClickPendingIntent(R.id.widget_anc_icon, toggle)
            views.setOnClickPendingIntent(R.id.widget_anc_hit, toggle)
            return views
        }

        internal fun ancLabel(anc: Int): String = when (anc) {
            Protocol.ANC_ON -> "Noise cancellation"
            Protocol.ANC_TRANSPARENCY -> "Transparency"
            else -> "Off"
        }

        private fun openApp(context: Context) {
            context.startActivity(launchIntent(context))
        }

        private fun launchIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

        private fun pendingActivity(context: Context, appWidgetId: Int): PendingIntent {
            return PendingIntent.getActivity(
                context,
                appWidgetId * 10 + 3,
                launchIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun pending(
            context: Context,
            action: String,
            appWidgetId: Int,
            slot: Int,
            mutable: Boolean = false,
        ): PendingIntent {
            val intent = Intent(context, MarshallWidgetProvider::class.java)
                .setAction(action)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.getBroadcast(context, appWidgetId * 10 + slot, intent, flags)
        }

        private fun toggleAnc(context: Context) {
            val appContext = context.applicationContext
            val prefs = appContext.getSharedPreferences(WidgetStateStore.PREFS, Context.MODE_PRIVATE)
            val requestedMode = Protocol.nextAncMode(prefs.getInt("anc", Protocol.ANC_OFF))
            WidgetStateStore.save(
                appContext,
                prefs.getInt("left", -1).takeIf { it in 0..100 },
                prefs.getInt("right", -1).takeIf { it in 0..100 },
                prefs.getInt("case", -1).takeIf { it in 0..100 },
                requestedMode,
                prefs.getString("name", "") ?: "",
                prefs.getBoolean("connected", false),
            )
            val manager = BleManager.get(appContext)
            if (!manager.state.connected && !manager.state.connecting) {
                manager.restoreSavedDevice()
            }
            val handler = Handler(Looper.getMainLooper())
            val attempt = object : Runnable {
                var remaining = 40
                override fun run() {
                    if (manager.state.connected) {
                        manager.setAncMode(requestedMode)
                        WidgetStateStore.save(
                            appContext,
                            manager.state.leftBattery,
                            manager.state.rightBattery,
                            manager.state.caseBattery,
                            requestedMode,
                            manager.state.deviceName,
                            manager.state.connected,
                        )
                    } else if (remaining-- > 0) {
                        handler.postDelayed(this, 500L)
                    }
                }
            }
            handler.post(attempt)
        }
    }
}

internal object WidgetArt {
    @Volatile private var leftBmp: Bitmap? = null
    @Volatile private var rightBmp: Bitmap? = null
    @Volatile private var warming = false

    val ready: Boolean get() = leftBmp != null && rightBmp != null

    fun left(context: Context): Bitmap? = leftBmp
    fun right(context: Context): Bitmap? = rightBmp

    fun warmAsync(context: Context) {
        if (ready || warming) return
        warming = true
        val app = context.applicationContext
        Thread {
            try {
                leftBmp = raster(app, R.drawable.motif_earbud_left)
                rightBmp = raster(app, R.drawable.motif_earbud_right)
                Handler(Looper.getMainLooper()).post { WidgetStateStore.update(app) }
            } catch (_: Exception) {
            } finally {
                warming = false
            }
        }.start()
    }

    private fun raster(context: Context, resId: Int): Bitmap? {
        return try {
            val density = context.resources.displayMetrics.density
            val height = (56 * density).toInt().coerceIn(72, 160)
            val width = (height * 72 / 98f).toInt().coerceAtLeast(64)
            val drawable = ContextCompat.getDrawable(context, resId) ?: return null
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                drawable.setBounds(0, 0, width, height)
                drawable.draw(Canvas(bitmap))
            }
        } catch (_: Exception) {
            null
        }
    }
}
