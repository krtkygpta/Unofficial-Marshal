package com.marshall.motif

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.marshall.motif.ble.Protocol
import com.marshall.motif.ui.components.ancModeDrawable

class MarshallWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        Factory(
            applicationContext,
            intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID),
        )

    private class Factory(
        private val context: Context,
        private val appWidgetId: Int,
    ) : RemoteViewsFactory {
        private var left = -1
        private var right = -1
        private var case = -1
        private var anc = Protocol.ANC_OFF
        private var connected = false
        private var pageHeightPx = 0

        override fun onCreate() = readState()
        override fun onDataSetChanged() = readState()
        override fun onDestroy() = Unit
        override fun getCount(): Int = 2
        override fun getViewTypeCount(): Int = 2
        override fun getItemId(position: Int): Long = position.toLong()
        override fun hasStableIds(): Boolean = true
        override fun getLoadingView(): RemoteViews =
            RemoteViews(context.packageName, R.layout.widget_host)

        override fun getViewAt(position: Int): RemoteViews {
            return try {
                if (position == 0) batteryPage() else ancPage()
            } catch (_: Exception) {
                RemoteViews(context.packageName, R.layout.widget_host)
            }
        }

        private fun batteryPage(): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_page_battery)
            WidgetArt.left(context)?.let { views.setImageViewBitmap(R.id.widget_bud_left, it) }
            WidgetArt.right(context)?.let { views.setImageViewBitmap(R.id.widget_bud_right, it) }
            if (connected) {
                views.setViewVisibility(R.id.widget_led, View.VISIBLE)
                views.setViewVisibility(R.id.widget_disconnected, View.GONE)
                views.setImageViewBitmap(
                    R.id.widget_led,
                    WidgetLed.drawPercents(left, right, case, context.resources.displayMetrics.density),
                )
            } else {
                views.setViewVisibility(R.id.widget_led, View.GONE)
                views.setViewVisibility(R.id.widget_disconnected, View.VISIBLE)
            }
            applyPageHeight(views, R.id.widget_page_battery)
            return views
        }

        private fun ancPage(): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_page_anc)
            if (connected) {
                views.setTextViewText(R.id.widget_anc_label, MarshallWidgetProvider.ancLabel(anc))
                views.setImageViewResource(R.id.widget_anc_icon, ancModeDrawable(anc))
            } else {
                views.setTextViewText(R.id.widget_anc_label, "Not connected")
                views.setImageViewResource(R.id.widget_anc_icon, R.drawable.ic_noise_control_off)
            }
            views.setInt(R.id.widget_anc_icon, "setColorFilter", 0xFFF4F4F4.toInt())
            applyPageHeight(views, R.id.widget_anc)
            val click = Intent().setAction(MarshallWidgetProvider.ACTION_TOGGLE_ANC)
            views.setOnClickFillInIntent(R.id.widget_anc_hit, click)
            views.setOnClickFillInIntent(R.id.widget_anc_icon, click)
            return views
        }

        private fun applyPageHeight(views: RemoteViews, rootId: Int) {
            if (pageHeightPx <= 0) return
            views.setInt(rootId, "setMinimumHeight", pageHeightPx)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                views.setViewLayoutHeight(rootId, pageHeightPx.toFloat(), TypedValue.COMPLEX_UNIT_PX)
            }
        }

        private fun readState() {
            val prefs = context.getSharedPreferences(WidgetStateStore.PREFS, Context.MODE_PRIVATE)
            left = prefs.getInt("left", -1)
            right = prefs.getInt("right", -1)
            case = prefs.getInt("case", -1)
            anc = prefs.getInt("anc", Protocol.ANC_OFF)
            connected = prefs.getBoolean("connected", false)
            if (!WidgetArt.ready) WidgetArt.warmAsync(context)
            pageHeightPx = try {
                val options = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId)
                val minHdp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
                (minHdp * context.resources.displayMetrics.density).toInt().coerceIn(140, 480)
            } catch (_: Exception) {
                (110 * context.resources.displayMetrics.density).toInt()
            }
        }
    }
}
